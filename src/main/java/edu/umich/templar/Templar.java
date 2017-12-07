package edu.umich.templar;

import com.esotericsoftware.minlog.Log;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.umich.templar.components.NodeMapper;
import edu.umich.templar.components.StanfordNLParser;
import edu.umich.templar.dataStructure.ParseTreeNode;
import edu.umich.templar.dataStructure.Query;
import edu.umich.templar.qf.*;
import edu.umich.templar.qf.pieces.AttributeType;
import edu.umich.templar.qf.pieces.Operator;
import edu.umich.templar.rdbms.Attribute;
import edu.umich.templar.rdbms.MappedSchemaElement;
import edu.umich.templar.rdbms.RDBMS;
import edu.umich.templar.rdbms.Relation;
import edu.umich.templar.template.InstantiatedTemplate;
import edu.umich.templar.template.JoinPathGenerator;
import edu.umich.templar.template.Template;
import edu.umich.templar.template.Translation;
import edu.umich.templar.tools.SimFunctions;
import edu.umich.templar.util.Constants;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by cjbaik on 12/7/17.
 */
public class Templar {
    RDBMS db;

    Document tokens;
    LexicalizedParser lexiParser;

    QFGraph qfGraph;
    Set<Template> templates;

    /*
     * EXAMPLE EXECUTION
     */
    public static void main(String[] args) {
        try {
            Templar templar = new Templar("127.0.0.1", 3306, "root", null, "advising", null);

            // Load a SQL Log
            /*
            List<String> sqlLogFile = FileUtils.readLines(new File("data/mas/mas_all.ans"), "UTF-8");
            List<String> sqlLog = new ArrayList<>();
            for (String line : sqlLogFile) {
                sqlLog.add(line.split("\t")[0]);
            }
            templar.loadSQLLog(sqlLog);*/

            // Translate NLQ
            List<String> output = templar.translate("return the department of \"Humanities Topics in Judaism\"");
            output.forEach(System.out::println);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * FUNCTIONS TO EXPOSE
     */
    public Templar(String dbHost, Integer dbPort, String dbUser,
                   String dbPassword, String datasetName, String dbName) throws Exception {
        String schemaPrefix = "data/" + datasetName + "/" + datasetName;

        if (dbName == null) {
            dbName = datasetName;
        }

        Log.info("Loading database...");
        this.db = new RDBMS(dbHost, dbPort, dbUser, dbPassword, dbName, schemaPrefix);
        Log.info("Database loaded.");

        // Read in Stanford Parser Model
        this.lexiParser = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");

        // Load token types
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        this.tokens = builder.parse(new File("libs/tokens.xml"));

        // Generate join paths
        int joinLevel = 6;
        JoinPathGenerator tg = new JoinPathGenerator(db, joinLevel);
        this.templates = tg.generate();

        this.qfGraph = new QFGraph(this.db.schemaGraph.relations);
    }

    public void loadSQLLog(List<String> queries) throws Exception {
        Log.info("Loading SQL log...");
        List<Select> selects = Utils.parseStatements(queries);
        for (Select select : selects) {
            this.qfGraph.analyzeSelect((PlainSelect) select.getSelectBody());
        }
        Log.info("Done loading SQL log.");
    }

    public List<String> translate(String nlq) throws Exception {
        Query query = new Query(nlq, this.db.schemaGraph);

        // Parse query with NL Parser
        StanfordNLParser.parse(query, lexiParser);

        // Map nodes to token types
        NodeMapper.phraseProcess(query, db, tokens);

        // Remove stopwords from parse tree
        query.parseTree.removeStopwords(Utils.stopwords);

        List<ParseTreeNode> mappedNodes = this.getMappedNodes(query);
        List<Translation> translations = this.generatePossibleTranslationsRecursive(mappedNodes,
                new Translation(null, this.qfGraph));

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

        for (Template tmpl : this.templates) {
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

        int maxResults = 10;
        return results.stream()
                .sorted((a, b) -> b.getScore().compareTo(a.getScore()))
                .map(InstantiatedTemplate::getValue)
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    /*
     * PRIVATE FUNCTIONS
     */
    private List<ParseTreeNode> getMappedNodes(Query query) {
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

    private Translation newTranslationWithSuperlative(Translation trans, ParseTreeNode node, MappedSchemaElement mse,
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

    private List<Translation> generateNewTranslationWithProjectionOrSuperlative(List<ParseTreeNode> remainingNodes,
                                                                               Translation trans, ParseTreeNode node,
                                                                               MappedSchemaElement mse, Attribute attr,
                                                                               Double similarity) {
        return this.generateNewTranslationWithProjectionOrSuperlative(remainingNodes, trans, node, mse, attr, similarity, false);
    }

    private List<Translation> generateNewTranslationWithProjectionOrSuperlative(List<ParseTreeNode> remainingNodes,
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
    private List<Translation> generatePossibleTranslationsRecursive(List<ParseTreeNode> remainingNodes,
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

            Relation rel = this.db.schemaGraph.relations.get(schemaEl.schemaElement.relation.name);
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
            if (attr == null) {
                throw new RuntimeException("Attribute " + schemaEl.schemaElement.name + " not found.");
            }

            double attrSim;
            try {
                attrSim = SimFunctions.similarity(attr.getName(), "NN", curNode.label, curNode.pos);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // If it has a child that has a "nummod" relationship, generate a HAVING
            boolean havingPrimaryAttr = rel.getPrimaryAttr() != null &&
                    rel.getPrimaryAttr().hasSameRelationNameAndNameAs(attr) && relSim >= Constants.MIN_SIM;
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

                            if (rel.getPrimaryAttr() != null) {
                                result.addAll(this.generateNewTranslationWithProjectionOrSuperlative(remainingNodes, trans,
                                        curNode, schemaEl, rel.getPrimaryAttr(), relSim));
                            }
                        }

                        if (rel.isWeak() &&
                                (curNode.relationship.equals("dobj") || curNode.relationship.equals("nsubj")
                                        || curNode.relationship.equals("nsubjpass")
                                        || curNode.parent.tokenType.equals("FT"))) {
                            // CASE 3: If it's a weak entity like "categories", project parent relation default attribute as well as
                            // predicate
                            // e.g. "How many restaurants..."

                            Relation parent = this.db.schemaGraph.relations.get(rel.getParent());

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
}
