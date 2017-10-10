package edu.umich.templar;

import com.esotericsoftware.minlog.Log;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.SentenceUtils;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.*;
import edu.umich.templar.components.NodeMapper;
import edu.umich.templar.components.StanfordNLParser;
import edu.umich.templar.dataStructure.ParseTreeNode;
import edu.umich.templar.dataStructure.Query;
import edu.umich.templar.parse.*;
import edu.umich.templar.rdbms.*;
import edu.umich.templar.sql.Operator;
import edu.umich.templar.template.*;
import edu.umich.templar.tools.PrintForCheck;
import edu.umich.templar.tools.SimFunctions;
import edu.umich.templar.util.Constants;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

/**
 * Created by cjbaik on 9/6/17.
 */
public class Templar {
    Map<String, Relation> relations;
    NLSQLLogCounter logCounter;

    public Templar(Map<String, Relation> relations, NLSQLLogCounter logCounter) {
        this.relations = relations;
        this.logCounter = logCounter;
    }

    public double getWeightedSimilarity(double similarity, String token, Object qfOrRel) {
        double universalScore = 0;
        double tokenScore = 0;
        if (qfOrRel instanceof QueryFragment) {
            QueryFragment qf = (QueryFragment) qfOrRel;
            universalScore = this.logCounter.getUniversalFreqScore(qf);
            if (token != null) {
                QueryFragmentCounter tkCounter = this.logCounter.getTokenCounters().get(token.toLowerCase());
                if (tkCounter != null) {
                    tokenScore = tkCounter.getFreqScore(qf);
                }
            }
        } else if (qfOrRel instanceof Relation) {
            Relation rel = (Relation) qfOrRel;
            universalScore = this.logCounter.getUniversalFreqScore(rel);
            if (token != null) {
                QueryFragmentCounter tkCounter = this.logCounter.getTokenCounters().get(token.toLowerCase());
                if (tkCounter != null) {
                    tokenScore = tkCounter.getFreqScore(rel);
                }
            }
        }
        return 0.5 * similarity + 0.1 * Math.sqrt(universalScore) + 0.4 * Math.sqrt(tokenScore);
    }

    public List<PossibleTranslation> generatePossibleTranslationsRecursive(List<ParseTreeNode> remainingNodes,
                                                                           Set<Relation> accumRel,
                                                                           List<Projection> accumProj,
                                                                           List<Predicate> accumPred,
                                                                           List<Having> accumHaving,
                                                                           Superlative superlative,
                                                                           double accumScore, double accumNodes) {
        List<PossibleTranslation> result = new ArrayList<>();

        if (accumRel == null) accumRel = new HashSet<>();
        if (accumProj == null) accumProj = new ArrayList<>();
        if (accumPred == null) accumPred = new ArrayList<>();
        if (accumHaving == null) accumHaving = new ArrayList<>();

        // Base case: generate current possible translations now
        if (remainingNodes.size() == 0) {
            // Check if there is at least one valid projection
            boolean validProjections = false;
            for (Projection proj : accumProj) {
                if (!proj.isGroupBy()) {
                    validProjections = true;
                }
            }

            if (validProjections) {
                PossibleTranslation translation = new PossibleTranslation();
                translation.setRelations(new HashSet<>(accumRel));
                translation.setProjections(new ArrayList<>(accumProj));
                translation.setPredicates(new ArrayList<>(accumPred));
                translation.setHavings(new ArrayList<>(accumHaving));
                translation.setSuperlative(superlative);
                translation.setTranslationScore(accumScore / accumNodes);
                result.add(translation);
            }
            return result;
        }

        ParseTreeNode curNode = remainingNodes.remove(0);

        if (curNode.tokenType.equals("NT") || curNode.tokenType.startsWith("VT")) {
            curNode.mappedElements.sort((a, b) -> Double.valueOf(b.similarity).compareTo(a.similarity));
            curNode.choice = 0;

            List<MappedSchemaElement> mappedList = curNode.mappedElements.subList(0, Math.min(5, curNode.mappedElements.size()));

            // If the highest similarity is still less than the minimum similarity, we just pass on this node
            if (mappedList.get(0).similarity < Constants.MIN_SIM) {
                result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes),
                        new HashSet<>(accumRel), new ArrayList<>(accumProj), new ArrayList<>(accumPred),
                        new ArrayList<>(accumHaving), superlative, accumScore, accumNodes));
            }

            for (MappedSchemaElement schemaEl : mappedList) {
                // Min threshold to even try...
                if (schemaEl.similarity < Constants.MIN_SIM) {
                    continue;
                }

                // Add a version without any changes, and MIN_SIM
                double scoreToAdd = this.getWeightedSimilarity(Constants.MIN_SIM, null, null);
                result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes),
                        new HashSet<>(accumRel), new ArrayList<>(accumProj), new ArrayList<>(accumPred),
                        new ArrayList<>(accumHaving), superlative, accumScore + scoreToAdd, accumNodes + 1));

                Relation rel = this.relations.get(schemaEl.schemaElement.relation.name);
                if (rel == null)
                    throw new RuntimeException("Relation " + schemaEl.schemaElement.relation.name + " not found.");

                // If we're dealing with a relation, generate a version without it (only if it's not a likely projection),
                // and also move ahead
                if (schemaEl.schemaElement.type.equals("relation")) {
                    if (!curNode.isFirstMappedDescendantOfCMT()) {
                        Set<Relation> newAccumRel = new HashSet<>(accumRel);
                        newAccumRel.add(rel);

                        // Enforce a penalty if there's a superlative or function associated with relation
                        double similarity = schemaEl.similarity;
                        if (curNode.attachedSuperlative != null || schemaEl.attachedFT != null) {
                            similarity *= Constants.PENALTY_RELATION_WITH_SUPERLATIVE;
                        }

                        similarity = this.getWeightedSimilarity(similarity, curNode.label, rel);

                        result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes),
                                newAccumRel, new ArrayList<>(accumProj), new ArrayList<>(accumPred),
                                new ArrayList<>(accumHaving), superlative, accumScore + similarity, accumNodes + 1));
                    }
                    continue;
                }

                Attribute attr = rel.getAttributes().get(schemaEl.schemaElement.name);
                if (attr == null)
                    throw new RuntimeException("Attribute " + schemaEl.schemaElement.name + " not found.");

                // Copy relevant structures so recursive operations don't interfere with it
                Set<Relation> newAccumRel = new HashSet<>(accumRel);
                List<Projection> newAccumProj = new ArrayList<>(accumProj);
                List<Predicate> newAccumPred = new ArrayList<>(accumPred);
                List<Having> newAccumHaving = new ArrayList<>(accumHaving);

                // Treat as projection, if no mapped values
                if (schemaEl.mappedValues.isEmpty() || schemaEl.choice == -1) {
                    // Treat as superlative if has both superlative and function
                    if (curNode.attachedSuperlative != null ||
                            (schemaEl.attachedFT != null &&
                                    (schemaEl.attachedFT.equals("max") || schemaEl.attachedFT.equals("min")))) {
                        String funcStr;
                        String superlativeStr;
                        if (curNode.attachedSuperlative != null) {
                            superlativeStr = curNode.attachedSuperlative;
                            funcStr = schemaEl.attachedFT;
                        } else {
                            superlativeStr = schemaEl.attachedFT;
                            funcStr = null;
                        }
                        boolean desc = superlativeStr.equals("max");

                        Superlative newSuper = new Superlative(attr, funcStr, desc);
                        if (!newAccumRel.contains(attr.getRelation())) {
                            newAccumRel.add(attr.getRelation());
                        }

                        double similarity = schemaEl.similarity;
                        similarity = this.getWeightedSimilarity(similarity, curNode.label, newSuper);

                        result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes),
                                newAccumRel, newAccumProj, newAccumPred, newAccumHaving, newSuper,
                                accumScore + similarity, accumNodes + 1));
                    } else {
                        Projection proj = new Projection(attr, schemaEl.attachedFT, curNode.QT);

                        // If you are creating a projection and there already exists a predicate with the same attribute,
                        // then create an additional path without this projection.
                        boolean selfJoinFlag = false;
                        int aliasInt = 0;
                        for (Predicate pred : accumPred) {
                            if (pred.getAttribute().hasSameRelationNameAndNameAs(attr)) {
                                selfJoinFlag = true;
                                aliasInt++;
                            }
                        }

                        // Increment aliasInt for every shared projection
                        boolean aggregateNewProj = false;
                        for (Projection curProj : accumProj) {
                            if (curProj.getAttribute().hasSameRelationNameAndNameAs(proj.getAttribute())) {
                                aliasInt++;
                            }
                            // If this projection is GROUP BY, aggregate other projections
                            if (proj.isGroupBy()) {
                                curProj.applyAggregateFunction();
                            }

                            // If other projection contains GROUP BY, set aggregate flag to aggregate this one
                            if (curProj.isGroupBy()) {
                                aggregateNewProj = true;
                            }
                        }

                        rel = new Relation(rel);
                        rel.setAliasInt(aliasInt);
                        Attribute newAttr = new Attribute(proj.getAttribute());
                        newAttr.setRelation(rel);
                        proj.setAttribute(newAttr);

                        if (aggregateNewProj) proj.applyAggregateFunction();

                        newAccumProj.add(proj);
                        newAccumRel.add(rel);

                        // Penalize the similarity if this projection is not the child of a CMT (Command Token) node
                        // but only if it's not a group by token
                        boolean likelyProjection = curNode.isFirstMappedDescendantOfCMT() || proj.isGroupBy();
                        double similarityAdded = schemaEl.similarity;
                        if (!likelyProjection) {
                            similarityAdded *= 0.8;
                        }
                        similarityAdded = this.getWeightedSimilarity(similarityAdded, curNode.label, proj);

                        result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes),
                                newAccumRel, newAccumProj, newAccumPred, newAccumHaving, superlative,
                                accumScore + similarityAdded, accumNodes + 1));
                    }
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

                    if (schemaEl.choice != -1) {
                        // TODO: open up the possibility that you have more than 1 mapped value?
                        if (value == null) {
                            value = schemaEl.mappedValues.get(0);
                        }
                    }

                    // Criteria for HAVING:
                    // (1) node should not be first descendant of CMT (it should be projection)
                    // (2) mapped schema element should have a valid attached function
                    if (!curNode.isFirstMappedDescendantOfCMT() && schemaEl.isValidHavingCandidate()) {
                        // TODO: do we need aliasInt for having?
                        Having having = new Having(attr, op, value, schemaEl.attachedFT);

                        // Should not do the same attribute for having as a projection
                        boolean projExists = false;
                        for (Projection proj : accumProj) {
                            if (having.getAttribute().hasSameRelationNameAndNameAs(proj.getAttribute())) {
                                projExists = true;
                            }
                        }
                        if (projExists) continue;

                        if (!newAccumHaving.contains(having)) newAccumHaving.add(having);
                        newAccumRel.add(rel);

                        double similarity = schemaEl.similarity;
                        similarity = this.getWeightedSimilarity(similarity, curNode.label, having);

                        result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes),
                                newAccumRel, newAccumProj, newAccumPred, newAccumHaving, superlative,
                                accumScore + similarity, accumNodes + 1));
                    } else {
                        // If you are creating a predicate and there already exists a projection with the same attribute,
                        // then create an additional path of eliminating this predicate.
                        int aliasInt = 0;

                        for (Projection p : accumProj) {
                            if (p.getAttribute().hasSameRelationNameAndNameAs(attr)) {
                                /*
                                result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes),
                                        new HashSet<>(accumRel), new ArrayList<>(accumProj), new ArrayList<>(accumPred),
                                        new ArrayList<>(accumHaving), superlative, accumScore + 0.5, accumNodes + 1));
                                        */

                                aliasInt++;
                            }
                        }

                        Predicate pred = new Predicate(attr, op, value);

                        // Check previous predicates, if same attr exists, increment aliasInt
                        boolean notNumberWithNonEquality = !(curNode.tokenType.equals("VTNUM") && curNode.attachedOT != null
                                && !curNode.attachedOT.equals("="));
                        if (notNumberWithNonEquality) {
                            for (Predicate curPred : accumPred) {
                                if (curPred.getAttribute().hasSameRelationNameAndNameAs(pred.getAttribute())) {
                                    aliasInt++;
                                }
                            }
                        }

                        rel = new Relation(rel);
                        rel.setAliasInt(aliasInt);
                        Attribute newAttr = new Attribute(pred.getAttribute());
                        newAttr.setRelation(rel);
                        pred.setAttribute(newAttr);

                        double relSim;
                        double attrSim;
                        try {
                            relSim = SimFunctions.similarity(rel.getName(), null, curNode.label, curNode.pos);
                            attrSim = SimFunctions.similarity(attr.getName(), null, curNode.label, curNode.pos);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        // Consider possibilities for projection if it's a descendant of CMT or each QT
                        if (curNode.isFirstMappedDescendantOfCMT() || curNode.QT.equals("each")) {
                            if (attrSim >= Constants.MIN_SIM) {
                                // CASE 1: If attribute is similar, project attribute accordingly

                                // TODO: do I need to increment aliasInt here for the parent? how?
                                Projection proj = new Projection(attr, curNode.getChoiceMap().attachedFT, curNode.QT);
                                newAccumProj.add(proj);
                                newAccumRel.add(rel);

                                // Get the maximum of the attribute or relation similarity
                                double maxSim = Math.max(relSim, attrSim);
                                maxSim = this.getWeightedSimilarity(maxSim, curNode.label, proj);

                                result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes),
                                        newAccumRel, newAccumProj, newAccumPred, newAccumHaving, superlative,
                                        accumScore + maxSim, accumNodes + 1));
                            } else if (relSim >= Constants.MIN_SIM) {
                                // CASE 2: If relation is similar, project relation default attribute
                                // e.g. "How many papers..."
                                Projection proj = new Projection(rel.getPrimaryAttr(), curNode.getChoiceMap().attachedFT,
                                        curNode.QT);
                                newAccumProj.add(proj);
                                newAccumRel.add(rel);

                                double similarity = relSim;
                                similarity = this.getWeightedSimilarity(similarity, curNode.label, proj);

                                result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes),
                                        newAccumRel, newAccumProj, newAccumPred, newAccumHaving, superlative,
                                        accumScore + similarity, accumNodes + 1));
                            } else if (rel.isWeak() &&
                                    (curNode.relationship.equals("dobj") || curNode.relationship.equals("nsubj")
                                            || curNode.relationship.equals("nsubjpass")
                                            || curNode.parent.tokenType.equals("FT"))) {
                                // CASE 3: If it's a weak entity, project parent relation default attribute as well as
                                // predicate
                                // e.g. "How many restaurants..."

                                Relation parent = this.relations.get(rel.getParent());
                                // TODO: do I need to increment aliasInt here for the parent? how?
                                Projection proj = new Projection(parent.getPrimaryAttr(), curNode.getChoiceMap().attachedFT,
                                        curNode.QT);
                                newAccumProj.add(proj);
                                newAccumRel.add(parent);

                                newAccumPred.add(pred);
                                newAccumRel.add(rel);

                                double similarity = schemaEl.similarity;
                                similarity = this.getWeightedSimilarity(similarity, curNode.label, proj);
                                // TODO: do I need to average the predicate weight too?

                                result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes),
                                        newAccumRel, newAccumProj, newAccumPred, newAccumHaving, superlative,
                                        accumScore + similarity, accumNodes + 1));
                            } else {
                                // CASE 4: Project relation default attribute in addition to predicate
                                // e.g. "How many Starbucks..."
                                Projection proj = new Projection(rel.getPk(), curNode.getChoiceMap().attachedFT,
                                        curNode.QT);
                                if (!newAccumPred.contains(pred)) newAccumPred.add(pred);
                                newAccumProj.add(proj);
                                newAccumRel.add(rel);

                                double similarity = schemaEl.similarity;
                                similarity = this.getWeightedSimilarity(similarity, curNode.label, proj);

                                result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes),
                                        newAccumRel, newAccumProj, newAccumPred, newAccumHaving, superlative,
                                        accumScore + similarity, accumNodes + 1));
                            }
                        } else {
                            // In situations where it's probably not a projection because parent is not CMT

                            if (relSim >= Constants.MIN_SIM) {
                                // CASE 1: it's a simple relation reference
                                newAccumRel.add(rel);

                                // Enforce a penalty if there's a superlative or function associated with relation
                                double similarity = schemaEl.similarity;
                                if (curNode.attachedSuperlative != null || schemaEl.attachedFT != null) {
                                    similarity *= Constants.PENALTY_RELATION_WITH_SUPERLATIVE;
                                }

                                similarity = this.getWeightedSimilarity(similarity, curNode.label, rel);

                                result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes),
                                        newAccumRel, newAccumProj, newAccumPred, newAccumHaving, superlative,
                                        accumScore + similarity, accumNodes + 1));
                            }

                            // CASE 2: it's actually supposed to be a predicate
                            if (!newAccumPred.contains(pred)) newAccumPred.add(pred);
                            newAccumRel.add(rel);

                            // penalize predicates that deal with common nouns
                            double similarity = schemaEl.similarity;
                            if (curNode.pos.equals("NNS")) similarity *= Constants.PENALTY_PREDICATE_COMMON_NOUN;

                            similarity = this.getWeightedSimilarity(similarity, curNode.label, pred);

                            result.addAll(this.generatePossibleTranslationsRecursive(new ArrayList<>(remainingNodes),
                                    newAccumRel, newAccumProj, newAccumPred, newAccumHaving, superlative,
                                    accumScore + similarity, accumNodes + 1));
                        }
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
                mappedNodes.add(node);

                // Check for related nodes that are auxiliary and delete
                for (ParseTreeNode[] auxEntry : query.auxTable) {
                    // If governing node
                    if (auxEntry[1].equals(node)) {
                        mappedNodesToRemove.add(auxEntry[0]);
                    }
                }

                // In the case that we have a function as a parent, add accordingly and "ignore" function
                if (!node.parent.function.equals("NA")) {
                    String superlative = null;
                    for (MappedSchemaElement mse : node.mappedElements) {
                        mse.attachedFT = node.parent.function;
                    }
                    ParseTreeNode functionNode = node.parent;

                    if (functionNode.parent.function.equals("max") || functionNode.parent.function.equals("min")) {
                        superlative = node.parent.parent.function;
                    }

                    // Only move around children if the function isn't actually a CMT (like "how many"), or its parent
                    // is a CMT
                    if (!functionNode.tokenType.equals("CMT") && !functionNode.parent.tokenType.equals("CMT")) {
                        for (ParseTreeNode funcChild : functionNode.children) {
                            if (!funcChild.equals(node)) {
                                funcChild.parent = node;
                                node.children.add(funcChild);
                            }

                            if (funcChild.function.equals("max") || funcChild.function.equals("min")) {
                                superlative = funcChild.function;
                            }
                        }

                        if (superlative != null) {
                            node.attachedSuperlative = superlative;
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

            // In the case we have a VT related to an NT, and they share an "amod" (adjective modifier)
            // or "num" (number modifier) or "nn" (noun compound modifier) relationship, merge the two.
            if (isValueToken) {
                for (ParseTreeNode[] adjEntry : query.adjTable) {
                    ParseTreeNode relatedNode;
                    if (adjEntry[0].equals(node)) {
                        relatedNode = adjEntry[1];
                    } else if (adjEntry[1].equals(node)) {
                        relatedNode = adjEntry[0];
                    } else {
                        continue;
                    }

                    // If it's been removed, don't consider it
                    if (!query.parseTree.allNodes.contains(relatedNode)) continue;

                    // Only do NTs for the remainder
                    // if (!relatedNode.tokenType.equals("NT")) continue;

                    // Leave direct objects of CMTs alone, because they're unlikely to be modified
                    if (relatedNode.parent.tokenType.equals("CMT") && relatedNode.relationship.equals("dobj")) continue;

                    MappedSchemaElement chosenMappedSchemaEl = null;
                    int choice = node.choice;
                    // double nodeSimilarity = node.getChoiceMap().similarity;
                    // double maxSimilarity = node.getChoiceMap().similarity;
                    double maxSimilarity = 0;
                    // List<Integer> matchedNodes = new ArrayList<>();
                    String attachedFT = null;

                    boolean addNewForPrimaryAttribute = false;

                    for (int j = 0; j < relatedNode.mappedElements.size(); j++) {
                        MappedSchemaElement relatedMappedEl = relatedNode.mappedElements.get(j);
                        SchemaElement relatedEl = relatedMappedEl.schemaElement;

                        // Only consider if NT
                        boolean relatedIsNT = relatedMappedEl.mappedValues.size() == 0 || relatedMappedEl.choice == -1;
                        if (!relatedIsNT) continue;

                        boolean matchedNodeEl = false;

                        for (int k = 0; k < node.mappedElements.size(); k++) {
                            MappedSchemaElement nodeMappedEl = node.mappedElements.get(k);

                            boolean relatedIsRelation = relatedMappedEl.schemaElement.type.equals("relation");
                            boolean relationMatchesAndThisIsPrimary = relatedIsRelation &&
                                    nodeMappedEl.schemaElement.relation.name.equals(relatedMappedEl.schemaElement.relation.name)
                                    && nodeMappedEl.schemaElement.relation.defaultAttribute.equals(nodeMappedEl.schemaElement);
                            boolean attributeMatchesIfBothAttributes = !relatedIsRelation &&
                                    nodeMappedEl.schemaElement.relation.name.equals(relatedMappedEl.schemaElement.relation.name) &&
                                    nodeMappedEl.schemaElement.name.equals(relatedMappedEl.schemaElement.name);
                            if (relationMatchesAndThisIsPrimary
                                    || attributeMatchesIfBothAttributes) {
                                matchedNodeEl = true;
                                // matchedNodes.add(k);

                                // Somewhat arbitrarily combine their similarity by averaging and giving a boost
                                // double combinedScore = (nodeSimilarity + relatedMappedEl.similarity) / 2;

                                double relatedScore = relatedMappedEl.similarity;
                                double nodeScore = nodeMappedEl.similarity;

                                if (nodeScore >= Constants.MIN_SIM && relatedScore >= Constants.MIN_SIM) {
                                    // Weigh each disproportionately and compare
                                    double firstScore = 0.8 * relatedScore + 0.2 * nodeScore;
                                    double secondScore = 0.8 * nodeScore + 0.2 * relatedScore;

                                    double combinedScore = Math.max(firstScore, secondScore);
                                    nodeMappedEl.similarity = combinedScore;

                                    if (combinedScore > maxSimilarity) {
                                        chosenMappedSchemaEl = nodeMappedEl;
                                        maxSimilarity = combinedScore;
                                        choice = k;
                                        addNewForPrimaryAttribute = false;
                                        attachedFT = relatedMappedEl.attachedFT;
                                    }
                                } else if (nodeScore >= Constants.MIN_SIM) {
                                    // Add 0.12 to be minimum related score, 0.6, multiplied by weight 0.2
                                    double nodeSim = nodeScore * 0.8 + 0.12;
                                    if (nodeSim > maxSimilarity) {
                                        // In the case that the node is fine as is
                                        chosenMappedSchemaEl = null;
                                        maxSimilarity = nodeSim;
                                        choice = k;
                                        addNewForPrimaryAttribute = false;
                                        attachedFT = null;
                                    }
                                } else if (relatedScore >= Constants.MIN_SIM) {
                                    double relatedSim = relatedScore * 0.8 + 0.12;
                                    if (relatedSim > maxSimilarity) {
                                        // In the case that the related is fine as is
                                        chosenMappedSchemaEl = null;
                                        maxSimilarity = relatedSim;
                                        choice = k;
                                        addNewForPrimaryAttribute = false;
                                        attachedFT = null;
                                    }
                                }
                            }
                        }

                        // In the case that the NT is the primary attribute, perhaps we're doing a COUNT?
                        if (!matchedNodeEl && relatedEl.equals(relatedEl.relation.defaultAttribute)) {
                            // Use only the related element's similarity
                            double relatedScore = relatedMappedEl.similarity;

                            // Make a new mapped element to add
                            MappedSchemaElement newMappedEl = new MappedSchemaElement(relatedMappedEl.schemaElement);
                            newMappedEl.mappedValues.add(node.label);
                            newMappedEl.attachedFT = "count";
                            newMappedEl.similarity = relatedScore;
                            node.mappedElements.add(newMappedEl);

                            if (relatedScore > maxSimilarity) {
                                chosenMappedSchemaEl = relatedMappedEl;
                                maxSimilarity = relatedScore;
                                addNewForPrimaryAttribute = true;
                            }
                        }
                    }

                    // Penalize all unmatched node elements
                        /*for (int m = 0; m < node.mappedElements.size(); m++) {
                            if (!matchedNodes.contains(m)) {
                                node.mappedElements.get(m).similarity *= 0.8;
                            }
                        }*/

                    if (chosenMappedSchemaEl != null) {
                        if (addNewForPrimaryAttribute) {
                            /*
                            chosenMappedSchemaEl.mappedValues.add(node.label);
                            chosenMappedSchemaEl.choice = chosenMappedSchemaEl.mappedValues.size() - 1;

                            node.mappedElements.add(chosenMappedSchemaEl);
                            node.choice = node.mappedElements.size() - 1;
                            chosenMappedSchemaEl.attachedFT = "count";*/
                        } else {
                            chosenMappedSchemaEl.similarity = maxSimilarity;
                            node.choice = choice;
                            if (attachedFT != null && node.getChoiceMap().attachedFT == null) {
                                node.getChoiceMap().attachedFT = attachedFT;
                            }
                        }
                        mappedNodesToRemove.add(relatedNode);
                    }
                }
            }
        }
        mappedNodes.removeAll(mappedNodesToRemove);
        return mappedNodes;
    }

    public static String getDebugOutput(LexicalizedParser lexiParser, Query query, List<PossibleTranslation> translations,
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
        for (PossibleTranslation trans : translations) {
            sb.append(trans.toStringDebug());
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
        if (args.length < 4) {
            System.out.println("Usage: Templar <db> <schema-prefix> <join-level> <nlq-file> <ans-file (optional)>");
            System.out.println("Example: Templar mas data/mas/mas 6 data/mas/mas_c1.txt data/mas/mas_c1.ans");
            System.exit(1);
        }
        String dbName = args[0];
        String prefix = args[1];
        int joinLevel = Integer.valueOf(args[2]);
        String nlqFile = args[3];

        String ansFile = null;
        if (args.length > 4) {
            ansFile = args[4];
        }

        RDBMS db;
        try {
            db = new RDBMS(dbName, prefix);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        // Load in everything for log counter
        NLSQLLogCounter nlsqlLogCounter = new NLSQLLogCounter(db.schemaGraph.relations);
        List<String> nlq = new ArrayList<>();
        try {
            nlq = FileUtils.readLines(new File(prefix + "_all.txt"), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<Select> selects = Utils.parseStatements(prefix + "_all.ans");
        for (int i = 0; i < selects.size(); i++) {
            Query query = new Query(nlq.get(i), db.schemaGraph);
            List<String> tokens = Arrays.asList(query.sentence.outputWords);
            nlsqlLogCounter.addNLQSQLPair(tokens, selects.get(i));
        }

        /*
        Templar templar = new Templar(db.schemaGraph.relations, nlsqlLogCounter);
        Relation user = db.schemaGraph.relations.get("user");
        Attribute username = user.getAttributes().get("name");
        System.out.println("username: " + templar.getWeightedSimilarity(0.5, "people", new Projection(username, "count", null)));
        Relation tip = db.schemaGraph.relations.get("tip");
        Attribute tipid = tip.getAttributes().get("tip_id");
        System.out.println("tip_text: " + templar.getWeightedSimilarity(0.5, "people", new Projection(tipid, "count", null)));
        System.exit(1);*/

        SchemaDataTemplateGenerator tg = new SchemaDataTemplateGenerator(db, joinLevel);
        Set<Template> templates = tg.generate();

        // templates.stream().map(Template::toString).forEach(System.out::println);

        // Read in Stanford Parser Model
        LexicalizedParser lexiParser = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");

        // Read in stop words list
        List<String> queryStrs = new ArrayList<>();
        List<List<String>> queryAnswers = null;
        try {
            // queryStrs.addAll(FileUtils.readLines(new File(nlqFile), "UTF-8"));

            if (ansFile != null) {
                List<String> answerFileLines = FileUtils.readLines(new File(ansFile), "UTF-8");
                queryAnswers = new ArrayList<>();
                for (String line : answerFileLines) {
                    queryAnswers.add(Arrays.asList(line.trim().split("\t")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        // queryStrs.add("what is the average rating given in Michelle reviews");
        // queryStrs.add("What is the number of businesses user Michelle reviews per month?");
        // queryStrs.add("How many reviews has Michelle written in March 2014?");
        // queryStrs.add("How many people reviewed the restaurant \"Texas De Brazil\" in Dallas TX?");
        // queryStrs.add("How many people reviewed \"Bistro Di Napoli\" in 2015?");
        // queryStrs.add("Find the average number of checkins in restaurant \"Barrio Café\" per day");
        // queryStrs.add("How many bars in Dallas have a rating above 3.5?");
        // queryStrs.add("What is the number of businesses user Michelle reviews per month?");
        // queryStrs.add("How many businesses in \"San Diego\" has Christine reviewed in 2010?");
        queryStrs.add("Find the number of reviews on businesses located in \"South Summerlin\" neighborhood");

        int i = 0;
        int top1 = 0;
        int top3 = 0;
        int top5 = 0;
        for (String queryStr : queryStrs) {
            Log.info("================");
            Log.info("QUERY " + i + ": " + queryStr);
            Log.info("================");

            // TODO: hack, to convert TX to Texas, probably don't need with something like word2vec
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

            Templar tc = new Templar(db.schemaGraph.relations, nlsqlLogCounter);
            List<PossibleTranslation> translations = tc.generatePossibleTranslationsRecursive(mappedNodes, null, null,
                    null, null, null, 0, 0);

            translations.sort((a, b) -> b.getTotalScore().compareTo(a.getTotalScore()));

            int n = 10;
            List<PossibleTranslation> topNTranslations = translations.subList(0, Math.min(translations.size(), n));

            List<InstantiatedTemplate> results = new ArrayList<>();
            Map<String, Integer> resultIndexMap = new HashMap<>();

            for (Template tmpl : templates) {
                for (PossibleTranslation trans : topNTranslations) {
                    Set<PossibleTranslation> perms = trans.getAliasPermutations();
                    for (PossibleTranslation perm : perms) {
                        InstantiatedTemplate inst = tmpl.instantiate(perm);
                        if (inst == null) continue;

                        Integer existingIndex = resultIndexMap.get(inst.getValue());

                        if (existingIndex != null) {
                            InstantiatedTemplate existingTmpl = results.get(existingIndex);
                            if (inst.getTotalScore() > existingTmpl.getTotalScore()) {
                                results.set(existingIndex, inst);
                            }
                        } else {
                            resultIndexMap.put(inst.getValue(), results.size());
                            results.add(inst);
                        }
                    }
                }
            }

            results.sort((a, b) -> b.getTotalScore().compareTo(a.getTotalScore()));

            Integer rank = null;
            if (queryAnswers != null) {
                Double correctResultScore = null;
                for (int j = 0; j < Math.min(results.size(), 10); j++) {
                    if (correctResultScore != null) {
                        // If this score is less, then we have no ties and we just break
                        if (results.get(j).getTotalScore() < correctResultScore) {
                            break;
                        }
                    }

                    if (queryAnswers.get(i).contains(results.get(j).getValue())) {
                        if (correctResultScore == null) {
                            correctResultScore = results.get(j).getTotalScore();

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
        Log.info("Total queries: " + queryStrs.size());
        Log.info("Top 1: " + top1);
        Log.info("Top 3: " + top3);
        Log.info("Top 5: " + top5);
    }
}
