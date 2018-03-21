package edu.umich.templar._old;

import com.esotericsoftware.minlog.Log;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.SentenceUtils;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.*;
import edu.umich.templar._old.components.NodeMapper;
import edu.umich.templar._old.components.StanfordNLParser;
import edu.umich.templar._old.dataStructure.ParseTreeNode;
import edu.umich.templar._old.dataStructure.Query;
import edu.umich.templar._old.qf.*;
import edu.umich.templar._old.qf.agnostic.AgnosticGraph;
import edu.umich.templar._old.qf.pieces.AttributeType;
import edu.umich.templar._old.rdbms.*;
import edu.umich.templar._old.qf.pieces.Operator;
import edu.umich.templar._old.template.*;
import edu.umich.templar._old.tools.PrintForCheck;
import edu.umich.templar._old.tools.SimFunctions;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

/**
 * Created by cjbaik on 9/6/17.
 */
public class TemplarTest {
    Map<String, Relation> relations;
    AgnosticGraph agnosticGraph;  // schema-agnostic co-occurrence graph
    QFGraph qfGraph;              // schema-aware co-occurrence graph

    public TemplarTest(Map<String, Relation> relations, AgnosticGraph agnosticGraph, QFGraph qfGraph) {
        this.relations = relations;
        this.agnosticGraph = agnosticGraph;
        this.qfGraph = qfGraph;
    }

    public Translation newTranslationWithSuperlative(Translation trans, ParseTreeNode node, MappedSchemaElement mse,
                                                     Attribute attr, Double similarity) {
        String funcStr;
        String superlativeStr;
        if (node.attachedSuperlative != null) {
            superlativeStr = node.attachedSuperlative;
            funcStr = mse.attachedFT;
        } else {
            superlativeStr = mse.attachedFT;
            funcStr = null;
        }
        boolean desc = superlativeStr.equals("max");

        Superlative newSuper = new Superlative(node, attr, funcStr, desc);

        Translation newTrans = new Translation(trans);
        newTrans.addQueryFragment(newSuper, similarity);
        return newTrans;
    }

    public List<Translation> generateNewTranslationWithProjectionOrSuperlative(List<ParseTreeNode> remainingNodes,
                                                                               Translation trans, ParseTreeNode node,
                                                                               MappedSchemaElement mse, Attribute attr,
                                                                               Double similarity) {
        return this.generateNewTranslationWithProjectionOrSuperlative(remainingNodes, trans, node, mse, attr, similarity, false);
    }

    public List<Translation> generateNewTranslationWithProjectionOrSuperlative(List<ParseTreeNode> remainingNodes,
                                                                               Translation trans, ParseTreeNode node,
                                                                               MappedSchemaElement mse, Attribute attr,
                                                                               Double similarity, boolean specialPkCount) {
        List<Translation> result = new ArrayList<>();

        if (mse.isSuperlative(node)) {
            Translation newTrans = this.newTranslationWithSuperlative(trans, node, mse, attr, similarity);
            result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes), newTrans));
        } else {
            // Eliminate unlikely function/attr type combos
            String funcName = mse.attachedFT;
            if (funcName != null) {
                boolean textNotCount = !funcName.equals("count") && attr.getAttributeType().equals(AttributeType.TEXT);
                if (textNotCount) {
                    return result;
                }

                boolean pkFkCount = !specialPkCount && funcName.equals("count") && (attr.isPk() || attr.isFk());
                if (pkFkCount) {
                    return result;
                }

                boolean numCount = funcName.equals("count") && attr.getAttributeType().equals(AttributeType.NUMBER);
                if (numCount) {
                    funcName = null;
                }
            }

            if (!node.QT.isEmpty()) {
                boolean returnAllAndNumberOrKey = node.QT.equals("all")
                        && (attr.getAttributeType().equals(AttributeType.NUMBER) || attr.isFk() || attr.isPk());
                if (returnAllAndNumberOrKey) {
                    return result;
                }
            }


            Projection proj = new Projection(node, attr, funcName, node.QT);

            // Check two things (either/or) with predicates if they share the same attribute:
            // Merge the projection into the predicate, if related by adjective
            for (Predicate pred : trans.getPredicates()) {
                if (pred.getAttribute().hasSameRelationNameAndNameAs(attr)) {
                    if (pred.getNode().isRelatedByAdjective(node)) {
                        Translation newTrans = new Translation(trans);
                        double oldSim = newTrans.getSimilarity(pred);
                        newTrans.removeQueryFragment(pred);
                        double sim = Math.max(oldSim, similarity);

                        // Add a HAVING instead if there's a function attached to this
                        if (mse.attachedFT != null) {
                            Having having = new Having(node, attr, pred.getOp(), pred.getValue(), mse.attachedFT);
                            newTrans.addQueryFragment(having, sim);
                        } else {
                            // merge the projection into the predicate
                            Predicate newPred = new Predicate(pred);
                            newTrans.addQueryFragment(newPred, sim);
                        }
                        result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes), newTrans));
                    }
                }
            }
            Projection aggregateExistingProj = null;
            boolean aggregateNewProj = false;

            for (Projection curProj : trans.getProjections()) {
                // If this projection is GROUP BY, aggregate other projections
                if (proj.isGroupBy()) {
                    aggregateExistingProj = curProj;
                }

                // If other projection contains GROUP BY, set aggregate flag to aggregate this one
                if (curProj.isGroupBy()) {
                    aggregateNewProj = true;
                }

                // Don't make duplicate projections
                if (curProj.getAttribute().hasSameRelationNameAndNameAs(proj.getAttribute())) {
                    // The one exists dominates if it has CMT parent
                    if (curProj.getNode().isFirstLikelyProjection()) {
                        Translation newTrans = new Translation(trans);
                        newTrans.addQueryFragment(new BlankQueryFragment(node), similarity);
                        result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes), newTrans));
                        return result;
                    } else {
                        Translation newTrans = new Translation(trans);
                        double oldSim = newTrans.getSimilarity(curProj);
                        newTrans.removeQueryFragment(curProj);
                        double sim = Math.max(oldSim, similarity);
                        newTrans.addQueryFragment(proj, sim);
                        result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes), newTrans));
                        return result;
                    }
                }
            }

            Translation newTrans = new Translation(trans);

            if (aggregateNewProj) {
                proj.applyAggregateFunction();
            }

            newTrans.addQueryFragment(proj, similarity);

            if (aggregateExistingProj != null) {
                Projection newProj = new Projection(aggregateExistingProj);
                double existingSim = newTrans.getSimilarity(aggregateExistingProj);
                newTrans.removeQueryFragment(aggregateExistingProj);
                newProj.applyAggregateFunction();
                newTrans.addQueryFragment(newProj, existingSim);
            }

            result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes), newTrans));
        }
        return result;
    }

    public List<Translation> generatePossibleTranslationsRecursive(List<ParseTreeNode> remainingNodes,
                                                                           Translation trans) {
        List<Translation> result = new ArrayList<>();

        // Base case: generate current possible translations now
        if (remainingNodes.size() == 0) {
            // Check if there is exactly one valid projection, and an optional Group By (assumption for our workload)
            int validProjections = 0;
            for (Projection proj : trans.getProjections()) {
                if (!proj.isGroupBy()) {
                    validProjections++;

                    // If not child of CMT or groupby, unlikely projection
                    if (!proj.getNode().isFirstLikelyProjection()) {
                        double oldSim = trans.getSimilarity(proj);
                        oldSim *= Constants.PENALTY_UNLIKELY_PROJECTION;
                        trans.setSimilarity(proj, oldSim);
                    }

                }
            }

            // Penalize any relations if they have an adjective relationship with any projection/predicate
            for (RelationFragment rel : trans.getRelations()) {
                double similarity = trans.getSimilarity(rel);
                for (Projection proj : trans.getProjections()) {
                    if (rel.getNode().isRelatedByAdjective(proj.getNode())) {
                        similarity *= Constants.PENALTY_RELATION_WITH_ADJECTIVE;
                    }
                }
                for (Predicate pred : trans.getPredicates()) {
                    if (rel.getNode().isRelatedByAdjective(pred.getNode())) {
                        similarity *= Constants.PENALTY_RELATION_WITH_ADJECTIVE;
                    }
                }
                trans.setSimilarity(rel, similarity);
            }

            if (validProjections == 1) {
                result.add(trans);
            }
            return result;
        }

        ParseTreeNode curNode = remainingNodes.remove(0);

        // Pass on this node if it's already in the translation (e.g. by means of a forward-looking HAVING that
        // already used it)
        if (trans.containsNode(curNode)) {
            return this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes), new Translation(trans));
        }

        curNode.mappedElements.sort((a, b) -> Double.valueOf(b.similarity).compareTo(a.similarity));
        curNode.choice = 0;

        List<MappedSchemaElement> mappedList = curNode.mappedElements.subList(0, Math.min(Constants.MAX_MAPPED_EL, curNode.mappedElements.size()));

        // Add a blank query fragment with minimum similarity, as long as it's not superlative, function, group by
        if (curNode.attachedSuperlative == null && curNode.QT.isEmpty()) {
            Translation blankTrans = new Translation(trans);
            blankTrans.addQueryFragment(new BlankQueryFragment(curNode), Constants.MIN_SIM);
            result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes), blankTrans));
        }

        for (MappedSchemaElement schemaEl : mappedList) {
            // Min threshold to even try...
            if (schemaEl.similarity < Constants.MIN_SIM) {
                break;
            }

            Relation rel = this.relations.get(schemaEl.schemaElement.relation.name);
            if (rel == null)
                throw new RuntimeException("Relation " + schemaEl.schemaElement.relation.name + " not found.");

            double relSim;
            try {
                String relPos = rel.isJoinTable()? "VB" : "NN";
                relSim = SimFunctions.similarity(rel.getName(), relPos, curNode.label, curNode.pos);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // If we're dealing with a relation, generate a version without it (only if it's not a likely projection or superlative),
            // and also move ahead
            if (schemaEl.schemaElement.type.equals("relation")) {
                if (!curNode.isFirstLikelyProjection() &&
                        curNode.attachedSuperlative == null) {
                    double similarity = schemaEl.similarity;
                    Translation newTrans = new Translation(trans);

                    // penalize verbs
                    /*
                    if (curNode.pos.startsWith("VB")) {
                        similarity *= Constants.PENALTY_VERB_TO_RELATION;
                    }*/
                    newTrans.addQueryFragment(new RelationFragment(curNode, rel), similarity);

                    result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes), newTrans));
                }

                continue;
            }

            Attribute attr = rel.getAttributes().get(schemaEl.schemaElement.name);
            if (attr == null)
                throw new RuntimeException("Attribute " + schemaEl.schemaElement.name + " not found.");

            double attrSim;
            try {
                attrSim = SimFunctions.similarity(attr.getName(), "NN", curNode.label, curNode.pos);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // If it has a child that has a "nummod" relationship, generate a HAVING
            boolean havingPrimaryAttr = rel.getPrimaryAttr().hasSameRelationNameAndNameAs(attr) && relSim >= Constants.MIN_SIM;
            ParseTreeNode nummodChild = curNode.getNummodChild();
            if (havingPrimaryAttr && nummodChild != null) {
                Translation newTrans = new Translation(trans);

                // Remove any existing predicates with the number if they exist
                for (Predicate pred : newTrans.getPredicates()) {
                    if (pred.getNode().equals(nummodChild)) {
                        newTrans.removeQueryFragment(pred);
                        break;
                    }
                }

                Having having = new Having(nummodChild, rel.getPrimaryAttr(),
                        Utils.getOperatorFromString(nummodChild.attachedOT), nummodChild.label, "count");
                newTrans.addQueryFragment(having, schemaEl.similarity);
                result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes), newTrans));
            }

            // Treat as projection, if no mapped values
            boolean isProjectionOrSuperlative = schemaEl.mappedValues.isEmpty() || schemaEl.choice == -1;
            if (isProjectionOrSuperlative) {
                result.addAll(this.generateNewTranslationWithProjectionOrSuperlative(remainingNodes, trans, curNode, schemaEl,
                        attr, schemaEl.similarity));
            } else {
                // For predicates and havings.
                Operator op = null;
                String value = null;

                // Try to find nearby node with operator token if number
                if (curNode.tokenType.equals("VTNUM")) {
                    if (curNode.attachedOT != null) {
                        switch (curNode.attachedOT) {
                            case ">":
                                op = Operator.GT;
                                value = curNode.label;
                                break;
                            case ">=":
                                op = Operator.GTE;
                                value = curNode.label;
                                break;
                            case "<":
                                op = Operator.LT;
                                value = curNode.label;
                                break;
                            case "<=":
                                op = Operator.LTE;
                                value = curNode.label;
                                break;
                            case "!=":
                                op = Operator.NE;
                                value = curNode.label;
                                break;
                            case "=":
                                op = Operator.EQ;
                                break;
                        }
                    }
                }

                if (op == null) op = Operator.EQ;

                if (value == null) {
                    value = schemaEl.mappedValues.get(0);
                }

                // Criteria for HAVING:
                // (1) node should not be first descendant of CMT (it should be projection)
                // (2) mapped schema element should have a valid attached function
                if (!curNode.isFirstLikelyProjection() && schemaEl.isValidHavingCandidate()) {
                    Having having = new Having(curNode, attr, op, value, schemaEl.attachedFT);

                    // Should not do the same attribute for having as a projection
                    boolean projExists = false;
                    for (Projection proj : trans.getProjections()) {
                        if (having.getAttribute().hasSameRelationNameAndNameAs(proj.getAttribute())) {
                            projExists = true;
                        }
                    }
                    if (projExists) continue;

                    Translation newTrans = new Translation(trans);
                    newTrans.addQueryFragment(having, schemaEl.similarity);

                    result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes), newTrans));
                } else {
                    Predicate pred = new Predicate(curNode, attr, op, value);

                    // Merge the projection into the predicate if they are related by adjective
                    for (Projection p : trans.getProjections()) {
                        if (p.getAttribute().hasSameRelationNameAndNameAs(attr)) {
                            // Merge the projection into the predicate, if related by adjective
                            if (p.getNode().isRelatedByAdjective(curNode)) {
                                Translation newTrans = new Translation(trans);
                                double oldSim = newTrans.getSimilarity(p);
                                newTrans.removeQueryFragment(p);
                                double sim = Math.max(oldSim, schemaEl.similarity);

                                // Add a HAVING instead if there's a function attached to the projection
                                if (p.getFunction() != null) {
                                    Having having = new Having(curNode, attr, pred.getOp(), pred.getValue(), p.getFunction());
                                    newTrans.addQueryFragment(having, sim);
                                } else {
                                    Predicate newPred = new Predicate(pred);
                                    newTrans.addQueryFragment(newPred, sim);
                                }
                                result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes), newTrans));
                            }
                        }
                    }

                    // Consider possibilities for projection if it's a descendant of CMT OR group by OR superlative
                    if (curNode.isFirstLikelyProjection() || curNode.QT.equals("each")
                            || curNode.attachedSuperlative != null) {
                        if (attrSim >= Constants.MIN_SIM) {
                            // CASE 1: If attribute is similar, project attribute accordingly

                            // Get the maximum of the attribute or relation similarity
                            double maxSim = Math.max(relSim, attrSim);

                            result.addAll(this.generateNewTranslationWithProjectionOrSuperlative(remainingNodes, trans,
                                    curNode, schemaEl, attr, maxSim));
                        }
                        if (relSim >= Constants.MIN_SIM) {
                            // CASE 2: If relation is similar, project relation default attribute
                            // e.g. "How many papers..."

                            result.addAll(this.generateNewTranslationWithProjectionOrSuperlative(remainingNodes, trans,
                                    curNode, schemaEl, rel.getPrimaryAttr(), relSim));
                        }

                        if (rel.isWeak() &&
                                (curNode.relationship.equals("dobj") || curNode.relationship.equals("nsubj")
                                        || curNode.relationship.equals("nsubjpass")
                                        || curNode.parent.tokenType.equals("FT"))) {
                            // CASE 3: If it's a weak entity like "categories", project parent relation default attribute as well as
                            // predicate
                            // e.g. "How many restaurants..."

                            Relation parent = this.relations.get(rel.getParent());

                            Translation newTrans = new Translation(trans);
                            newTrans.addQueryFragment(pred, schemaEl.similarity);
                            result.addAll(this.generateNewTranslationWithProjectionOrSuperlative(remainingNodes, newTrans,
                                    curNode, schemaEl, parent.getPrimaryAttr(), schemaEl.similarity));
                        } else {
                            // CASE 4: Project relation default attribute in addition to predicate
                            // e.g. "How many Starbucks..."

                            double similarity = schemaEl.similarity;

                            // penalize predicates that deal with common nouns
                            if (curNode.pos.equals("NNS") || curNode.pos.equals("NN")) {
                                similarity *= Constants.PENALTY_PREDICATE_COMMON_NOUN;
                            }

                            if (rel.getPk() != null) {
                                Translation newTrans = new Translation(trans);
                                newTrans.addQueryFragment(pred, similarity);
                                result.addAll(this.generateNewTranslationWithProjectionOrSuperlative(remainingNodes, newTrans,
                                        curNode, schemaEl, rel.getPk(), schemaEl.similarity, true));
                            }

                            // CASE 5: Maybe it's just a predicate, even though the parent is a CMT
                            // penalize if a superlative is attached to this node
                            if (curNode.attachedSuperlative != null) similarity *= Constants.PENALTY_PREDICATE_WITH_SUPERLATIVE;

                            Translation newTrans2 = new Translation(trans);
                            newTrans2.addQueryFragment(pred, similarity);

                            result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes), newTrans2));
                        }
                    } else {
                        // In situations where it's probably not a projection because parent is not CMT

                        if (relSim >= Constants.MIN_SIM) {
                            // CASE 1: it's a simple relation reference

                            // Enforce a penalty if there's a superlative or function associated with relation
                            double similarity = relSim;
                            if (curNode.attachedSuperlative != null || schemaEl.attachedFT != null) {
                                similarity *= Constants.PENALTY_RELATION_WITH_SUPERLATIVE;
                            }

                            // penalize verbs
                            /*
                            if (curNode.pos.startsWith("VB")) {
                                similarity *= Constants.PENALTY_VERB_TO_RELATION;
                            }*/

                            Translation newTrans = new Translation(trans);
                            newTrans.addQueryFragment(new RelationFragment(curNode, rel), similarity);

                            result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes), newTrans));
                        }

                        // penalize predicates that deal with common nouns, but only if it's not a weak entity
                        double similarity = schemaEl.similarity;
                        if ((curNode.pos.equals("NNS") || curNode.pos.equals("NN")) && !rel.isWeak()) {
                            similarity *= Constants.PENALTY_PREDICATE_COMMON_NOUN;
                        }

                        Translation newTrans = new Translation(trans);
                        newTrans.addQueryFragment(pred, similarity);

                        result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes), newTrans));
                    }
                }
            }
        }
        return result;
    }

    public static void removeStopwords(List<String> stopwords, Query query) {
        List<ParseTreeNode> nodesToRemove = new ArrayList<>();
        for (ParseTreeNode node : query.parseTree.allNodes) {
            if (stopwords.contains(node.label.toLowerCase())) {
                nodesToRemove.add(node);
            }
        }
        for (ParseTreeNode node : nodesToRemove) {
            query.parseTree.deleteNode(node);
        }
    }

    public static List<ParseTreeNode> getMappedNodes(Query query) {
        List<ParseTreeNode> mappedNodes = new ArrayList<>();
        List<ParseTreeNode> mappedNodesToRemove = new ArrayList<>();

        for (ParseTreeNode node : query.parseTree.allNodes) {
            boolean isNameToken = node.tokenType.equals("NT");
            boolean isValueToken = node.tokenType.startsWith("VT");

            if (isNameToken || isValueToken) {
                // Check for related nodes that are auxiliary and delete
                for (ParseTreeNode[] auxEntry : query.auxTable) {
                    // If governing node
                    if (auxEntry[1].equals(node)) {
                        mappedNodesToRemove.add(auxEntry[0]);
                    }
                }

                // In the case that we have a function as a parent, add accordingly and "ignore" function
                if (!node.parent.function.equals("NA")) {
                    ParseTreeNode functionNode = node.parent;

                    String primaryFT = node.parent.function;
                    String secondaryFT = null;
                    if (!functionNode.parent.function.equals("NA")) {
                        if (functionNode.parent.function.equals("max") || functionNode.parent.function.equals("min")) {
                            node.attachedSuperlative = functionNode.parent.function;
                        } else {
                            secondaryFT = functionNode.parent.function;
                        }
                    }
                    if (secondaryFT == null || node.attachedSuperlative == null) {
                        for (ParseTreeNode funcChild : functionNode.children) {
                            if (!funcChild.function.equals("NA")) {
                                if (funcChild.function.equals("max") || funcChild.function.equals("min")) {
                                    node.attachedSuperlative = funcChild.function;
                                } else {
                                    secondaryFT = funcChild.function;
                                }
                                break;
                            }
                        }
                    }

                    for (MappedSchemaElement mse : node.mappedElements) {
                        if (mse.schemaElement.type.equals("int") || mse.schemaElement.type.equals("double")) {
                            if (!primaryFT.equals("count")) {
                                mse.attachedFT = primaryFT;
                            } else if (secondaryFT != null && !secondaryFT.equals("count")) {
                                mse.attachedFT = secondaryFT;
                            }
                        } else {
                            // Only allow "count" for text nodes
                            if (primaryFT.equals("count") || (secondaryFT != null && secondaryFT.equals("count"))) {
                                mse.attachedFT = "count";
                            }
                        }
                    }

                    // Only move around children if the function isn't actually a CMT (like "how many"), or its parent
                    // is a CMT
                    if (!functionNode.tokenType.equals("CMT") && !functionNode.parent.tokenType.equals("CMT")) {
                        for (ParseTreeNode funcChild : functionNode.children) {
                            if (!funcChild.equals(node)) {
                                funcChild.parent = node;
                                node.children.add(funcChild);
                            }
                        }

                        node.parent = functionNode.parent;
                        node.relationship = functionNode.relationship;
                        functionNode.parent.children.remove(functionNode);
                        functionNode.parent.children.add(node);
                    }
                }

                // Do similar operation if function is child and has no children
                // for operations such as: "return me the author who has the most number of papers..."
                List<ParseTreeNode> funcToRemove = new ArrayList<>();
                List<ParseTreeNode> childrenToAdd = new ArrayList<>();
                for (ParseTreeNode child : node.children) {
                    if (child.tokenType.equals("FT") && child.children.isEmpty()) {
                        if (child.function.equals("max") || child.function.equals("min")) {
                            node.attachedSuperlative = child.function;
                        } else {
                            for (MappedSchemaElement mse : node.mappedElements) {
                                mse.attachedFT = child.function;
                            }
                        }
                        childrenToAdd.addAll(child.children);
                        funcToRemove.add(child);
                    }
                }
                node.children.removeAll(funcToRemove);
                node.children.addAll(childrenToAdd);
            }

            if (node.mappedElements.size() > 0 ) mappedNodes.add(node);
        }
        mappedNodes.removeAll(mappedNodesToRemove);
        return mappedNodes;
    }

    public static String getDebugOutput(LexicalizedParser lexiParser, Query query, List<Translation> translations,
                                 List<InstantiatedTemplate> results) {
        StringBuilder sb = new StringBuilder();

        List<CoreLabel> rawWords = SentenceUtils.toCoreLabelList(query.sentence.outputWords); // use Stanford parser to parse a sentence;
        Tree parse = lexiParser.apply(rawWords);
        TreebankLanguagePack tlp = new PennTreebankLanguagePack();
        GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
        GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
        List<TypedDependency> dependencyList = gs.typedDependencies(false);

        for (TaggedWord tagged : parse.taggedYield()) {
            sb.append(tagged.word());
            sb.append(", ");
            sb.append(tagged.tag());
            sb.append("\n");
        }

        for (TypedDependency dep : dependencyList) {
            sb.append(dep);
            sb.append("\n");
        }

        sb.append("\n");
        sb.append("PARSE TREE:");
        sb.append(query.parseTree);
        sb.append("\n");

        PrintForCheck.allParseTreeNodePrintForCheck(query.parseTree);
        sb.append("\n");

        sb.append("===========\n");
        sb.append("TRANSLATIONS\n");
        sb.append("===========\n");
        for (Translation trans : translations) {
            sb.append(trans.toString());
            sb.append("\n");
        }
        sb.append("\n");

        sb.append("===========\n");
        sb.append("RESULTS\n");
        sb.append("===========\n");
        for (InstantiatedTemplate result : results) {
            sb.append(result);
            sb.append("\n");
        }
        sb.append("\n");

        return sb.toString();
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: TemplarTest <testset> <join-level> <cat-level>");
            System.out.println("Example: TemplarTest mas 6 all");
            System.out.println("Example: TemplarTest mas 6 c1");
            System.exit(1);
        }
        String dbName = args[0];
        String prefix = "data/" + dbName + "/" + dbName;
        int joinLevel = Integer.valueOf(args[1]);
        String catLevel = args[2];

        String nlqFile = prefix + "_" + catLevel + ".txt";
        String ansFile = prefix + "_" + catLevel + ".ans";

        RDBMS db;
        try {
            db = new RDBMS(dbName, prefix);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        // Load in everything for log counter
        // NLSQLLogCounter nlsqlLogCounter = new NLSQLLogCounter(db.schemaGraph.relations);

        List<List<String>> queryAnswers = null;
        try {
            List<String> answerFileLines = FileUtils.readLines(new File(ansFile), "UTF-8");
            queryAnswers = new ArrayList<>();
            for (String line : answerFileLines) {
                List<String> answerList = new ArrayList<>();
                for (String ans : line.trim().split("\t")) {
                    answerList.add(ans.trim());
                }
                queryAnswers.add(answerList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Add NLQ/SQL log pairs
        /*
        for (int i = 0; i < logNLQ.size(); i++) {
            if (logSQL.get(i) != null) {
                Query query = new Query(logNLQ.get(i), db.schemaGraph);
                List<String> tokens = Arrays.asList(query.sentence.outputWords);
                nlsqlLogCounter.addNLQSQLPair(tokens, logSQL.get(i));
            }
        }*/

        // Load in agnostic graph
        AgnosticGraph agnosticGraph = new AgnosticGraph(db.schemaGraph.relations);
        QFGraph qfGraph = new QFGraph(db.schemaGraph.relations);

        List<String> logSQLStr = new ArrayList<>();
        queryAnswers.stream().map((list) -> list.get(0)).forEach(logSQLStr::add);
        List<Select> selects = Utils.parseStatements(logSQLStr);
        for (Select select : selects) {
            agnosticGraph.analyzeSelect((PlainSelect) select.getSelectBody());
            qfGraph.analyzeSelect((PlainSelect) select.getSelectBody());
        }

        JoinPathGenerator tg = new JoinPathGenerator(db, joinLevel);
        Set<Template> templates = tg.generate();

        // Read in Stanford Parser Model
        LexicalizedParser lexiParser = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");

        List<String> testNLQ = new ArrayList<>();
        List<List<String>> testSQL = new ArrayList<>();
        try {
            // TODO: set this MODE to test different types of graphs
            Translation.MODE = 2;
            testNLQ.addAll(FileUtils.readLines(new File(nlqFile), "UTF-8"));

            /*
            // This is hard...
            testNLQ.add("What is the maximum number of movies in which \"Brad Pitt\" act in a given year?");

            // Can't handle splitting into two predicates right now
            testNLQ.add("Find all movies written and produced by \"Woody Allen\"");

            // nmod falsely collected...
            // testNLQ.add("How many movies did \"Quentin Tarantino\" direct before 2002 and after 2010?");

            // "latest" requires some parsing tricks for "when" attributes
            // testNLQ.add("What is the latest movie by \"Jim Jarmusch\"");
            // testNLQ.add("Who directed the latest movie by \"NBCUniversal\"");
            // testNLQ.add("Find the latest movie which "Gabriele Ferzetti" acted in");

            // 2 projections..
            testNLQ.add("Find the name and budget of the latest movie by \"Quentin Tarantino\"");

            // nouns are mischaracterized as verbs
            testNLQ.add("Who was the director of the movie Joy from 2015?");
            testNLQ.add("How many movies are there that are directed by \"Steven Spielberg\" and featuring \"Matt Damon\"?");

            // Ranks aren't as good as hoped for certain keyword queries
            testNLQ.add("Find all movies about Iraq war");
            testNLQ.add("What are the movies related to nuclear weapons");
            testNLQ.add("List all the directors of movies about nuclear weapons");

            // "Role" is not selected
            testNLQ.add("What are the major roles in the movie \"Daddy Long Legs\"");
            */

            if (ansFile != null) {
                List<String> answerFileLines = FileUtils.readLines(new File(ansFile), "UTF-8");
                testSQL = new ArrayList<>();
                for (String line : answerFileLines) {
                    List<String> answerList = new ArrayList<>();
                    for (String ans : line.trim().split("\t")) {
                        answerList.add(ans.trim());
                    }
                    testSQL.add(answerList);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        int i = 0;
        int top1 = 0;
        int top3 = 0;
        int top5 = 0;
        for (String queryStr : testNLQ) {
            Log.info("================");
            Log.info("QUERY " + i + ": " + queryStr);
            Log.info("================");

            // TODO: hack, to convert TX to Texas, probably don't need later
            queryStr = queryStr.replace("TX", "Texas");
            queryStr = queryStr.replace("PA", "Pennsylvania");

            Query query = new Query(queryStr, db.schemaGraph);

            // Parse query with NL Parser
            StanfordNLParser.parse(query, lexiParser);

            // Map nodes to token types
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document tokens = builder.parse(new File("libs/tokens.xml"));
                NodeMapper.phraseProcess(query, db, tokens);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            removeStopwords(Utils.stopwords, query);

            List<ParseTreeNode> mappedNodes = getMappedNodes(query);

            TemplarTest tc = new TemplarTest(db.schemaGraph.relations, agnosticGraph, qfGraph);
            List<Translation> translations = tc.generatePossibleTranslationsRecursive(mappedNodes,
                    new Translation(agnosticGraph, qfGraph));

            translations.sort((a, b) -> b.getScore().compareTo(a.getScore()));

            int n = 10;
            List<Translation> topNTranslations = new ArrayList<>();

            double lastScore = 0.0;
            for (Translation t : translations) {
                if (t.getScore() < lastScore && topNTranslations.size() > n) {
                    break;
                }

                topNTranslations.add(t);
                lastScore = t.getScore();
            }

            List<InstantiatedTemplate> results = new ArrayList<>();
            Map<String, Integer> resultIndexMap = new HashMap<>();

            for (Template tmpl : templates) {
                for (Translation trans : topNTranslations) {
                    Set<Translation> perms = trans.getAliasPermutations(tmpl.getJoinPath());

                    for (Translation perm : perms) {
                        InstantiatedTemplate inst = new InstantiatedTemplate(tmpl, perm);
                        if (inst.getValue() == null) continue;

                        Integer existingIndex = resultIndexMap.get(inst.getValue());

                        if (existingIndex != null) {
                            InstantiatedTemplate existingTmpl = results.get(existingIndex);
                            if (inst.getScore() > existingTmpl.getScore()) {
                                results.set(existingIndex, inst);
                            }
                        } else {
                            resultIndexMap.put(inst.getValue(), results.size());
                            results.add(inst);
                        }
                    }
                }
            }

            results.sort((a, b) -> b.getScore().compareTo(a.getScore()));

            Integer rank = null;
            if (testSQL != null) {
                Double correctResultScore = null;
                for (int j = 0; j < Math.min(results.size(), 10); j++) {
                    if (correctResultScore != null) {
                        // If this score is less, then we have no ties and we just break
                        if (results.get(j).getScore() < correctResultScore) {
                            break;
                        }
                    }

                    if (testSQL.get(i).contains(results.get(j).getValue())) {
                        if (correctResultScore == null) {
                            correctResultScore = results.get(j).getScore();

                            rank = j + 1;
                            if (rank <= 5) top5++;
                            if (rank <= 3) top3++;
                            if (rank == 1) top1++;
                        }
                    }
                }
            }

            if (rank == null || rank > 1) {
                System.out.println(getDebugOutput(lexiParser, query, topNTranslations, results.subList(0, Math.min(10, results.size()))));
            }
            i++;
        }

        Log.info("==============");
        Log.info("SUMMARY");
        Log.info("==============");
        Log.info("Total queries: " + testNLQ.size());
        Log.info("Top 1: " + top1);
        Log.info("Top 3: " + top3);
        Log.info("Top 5: " + top5);
    }
}
