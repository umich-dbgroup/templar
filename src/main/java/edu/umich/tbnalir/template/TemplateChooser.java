package edu.umich.tbnalir.template;

import com.esotericsoftware.minlog.Log;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.*;
import edu.umich.tbnalir.components.NodeMapper;
import edu.umich.tbnalir.components.StanfordNLParser;
import edu.umich.tbnalir.dataStructure.ParseTree;
import edu.umich.tbnalir.dataStructure.ParseTreeNode;
import edu.umich.tbnalir.dataStructure.Query;
import edu.umich.tbnalir.parse.PossibleTranslation;
import edu.umich.tbnalir.parse.Projection;
import edu.umich.tbnalir.rdbms.*;
import edu.umich.tbnalir.sql.Operator;
import edu.umich.tbnalir.parse.Predicate;
import edu.umich.tbnalir.tools.PrintForCheck;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

/**
 * Created by cjbaik on 9/6/17.
 */
public class TemplateChooser {
    Map<String, Relation> relations;

    public TemplateChooser(Map<String, Relation> relations) {
        this.relations = relations;
    }

    public List<PossibleTranslation> generatePossibleTranslationsRecursive(ParseTree parseTree,
                                                                           List<ParseTreeNode> remainingNodes,
                                                                           Set<Relation> accumRel,
                                                                           List<Projection> accumProj,
                                                                           List<Predicate> accumPred,
                                                                           double accumScore, double accumNodes) {
        List<PossibleTranslation> result = new ArrayList<>();

        if (accumRel == null) accumRel = new HashSet<>();
        if (accumProj == null) accumProj = new ArrayList<>();
        if (accumPred == null) accumPred = new ArrayList<>();

        // Base case: generate current possible translations now
        if (remainingNodes.size() == 0) {
            // Invalid translation if projections are empty
            if (accumProj.isEmpty()) {
                return result;
            }

            PossibleTranslation translation = new PossibleTranslation();
            translation.setRelations(new HashSet<>(accumRel));
            translation.setProjections(new ArrayList<>(accumProj));
            translation.setPredicates(new ArrayList<>(accumPred));
            translation.setTranslationScore(accumScore / accumNodes);
            result.add(translation);
            return result;
        }

        ParseTreeNode curNode = remainingNodes.remove(0);

        if (curNode.tokenType.equals("NT") || curNode.tokenType.startsWith("VT")) {
            for (MappedSchemaElement schemaEl : curNode.mappedElements) {
                Relation rel = this.relations.get(schemaEl.schemaElement.relation.name);
                if (rel == null)
                    throw new RuntimeException("Relation " + schemaEl.schemaElement.relation.name + " not found.");

                // If we're dealing with a relation, generate a version without it, and also move ahead
                if (schemaEl.schemaElement.type.equals("relation")) {
                    // TODO: arbitrary 0.8 added here - what to do?
                    result.addAll(this.generatePossibleTranslationsRecursive(parseTree, new ArrayList<>(remainingNodes),
                            new HashSet<>(accumRel), new ArrayList<>(accumProj), new ArrayList<>(accumPred),
                            accumScore + 0.8, accumNodes + 1));

                    Set<Relation> newAccumRel = new HashSet<>(accumRel);
                    newAccumRel.add(rel);

                    result.addAll(this.generatePossibleTranslationsRecursive(parseTree, new ArrayList<>(remainingNodes),
                            newAccumRel, new ArrayList<>(accumProj), new ArrayList<>(accumPred),
                            accumScore + schemaEl.similarity, accumNodes + 1));
                    continue;
                }

                Attribute attr = rel.getAttributes().get(schemaEl.schemaElement.name);
                if (attr == null)
                    throw new RuntimeException("Attribute " + schemaEl.schemaElement.name + " not found.");

                Projection proj = new Projection(attr, curNode.attachedFT);

                Operator op = null;
                String value = null;

                // Treat as projection, if no mapped values
                if (schemaEl.mappedValues.isEmpty() || schemaEl.choice == -1) {
                    // Copy relevant structures so recursive operations don't interfere with it
                    Set<Relation> newAccumRel = new HashSet<>(accumRel);
                    List<Projection> newAccumProj = new ArrayList<>(accumProj);
                    List<Predicate> newAccumPred = new ArrayList<>(accumPred);

                    // If you are creating a projection and there already exists a predicate with the same attribute,
                    // then create an additional path without this projection.
                    boolean selfJoinFlag = false;
                    int aliasInt = 0;
                    for (Predicate pred : accumPred) {
                        if (pred.getAttribute().hasSameRelationNameAndNameAs(attr)) {
                            if (!selfJoinFlag) {
                                result.addAll(this.generatePossibleTranslationsRecursive(parseTree, new ArrayList<>(remainingNodes),
                                        new HashSet<>(accumRel), new ArrayList<>(accumProj), new ArrayList<>(accumPred),
                                        accumScore + 0.5, accumNodes + 1));
                            }
                            selfJoinFlag = true;
                            aliasInt++;
                        }
                    }

                    // If we have a quantifier, set GROUP BY
                    if (curNode.QT != null && curNode.QT.equals("each")) {
                        proj.setGroupBy(true);
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

                    //if (!newAccumProj.contains(proj)) newAccumProj.add(proj);
                    newAccumProj.add(proj);
                    newAccumRel.add(rel);

                    // Penalize the similarity if this projection is not the child of a CMT (Command Token) node
                    // but only if it's not a group by token
                    boolean likelyProjection = curNode.parent.tokenType.equals("CMT") || proj.isGroupBy();
                    double similarityAdded = schemaEl.similarity;
                    if (!likelyProjection) {
                        similarityAdded *= 0.8;
                    }

                    result.addAll(this.generatePossibleTranslationsRecursive(parseTree, new ArrayList<>(remainingNodes),
                            newAccumRel, newAccumProj, newAccumPred, accumScore + similarityAdded, accumNodes + 1));
                } else {
                    // If you are creating a predicate and there already exists a projection with the same attribute,
                    // then create an additional path of eliminating this predicate.
                    int aliasInt = 0;

                    for (Projection p : accumProj) {
                        if (p.getAttribute().hasSameRelationNameAndNameAs(proj.getAttribute())) {
                            result.addAll(this.generatePossibleTranslationsRecursive(parseTree, new ArrayList<>(remainingNodes),
                                    new HashSet<>(accumRel), new ArrayList<>(accumProj), new ArrayList<>(accumPred),
                                    accumScore + 0.5, accumNodes + 1));

                            aliasInt++;
                        }
                    }

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

                    // Copy relevant structures so recursive operations don't interfere with it
                    Set<Relation> newAccumRel = new HashSet<>(accumRel);
                    List<Projection> newAccumProj = new ArrayList<>(accumProj);
                    List<Predicate> newAccumPred = new ArrayList<>(accumPred);

                    if (!newAccumPred.contains(pred)) newAccumPred.add(pred);
                    newAccumRel.add(rel);

                    result.addAll(this.generatePossibleTranslationsRecursive(parseTree, new ArrayList<>(remainingNodes),
                            newAccumRel, newAccumProj, newAccumPred, accumScore + schemaEl.similarity, accumNodes + 1));
                }
            }
        }
        return result;
    }

    public static void main(String[] args) {
        String dbName = "mas";
        String prefix = "data/mas/mas";
        int joinLevel = 6;

        String nlqFile = "data/mas/mas_c4_nlq.txt";

        RDBMS db;
        try {
            db = new RDBMS(dbName, prefix);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        SchemaDataTemplateGenerator tg = new SchemaDataTemplateGenerator(db, joinLevel);
        Set<Template> templates = tg.generate();

        LexicalizedParser lexiParser = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");

        List<String> queryStrs;
        try {
            queryStrs = FileUtils.readLines(new File(nlqFile), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        // List<String> queryStrs = new ArrayList<>();
        // queryStrs.add("return me papers with more than 200 citations.");  // C1.12
        // queryStrs.add("return me the papers written by \"H. V. Jagadish\" and \"Divesh Srivastava.\""); // C4.06

        int i = 0;
        for (String queryStr : queryStrs) {
            Log.info("================");
            Log.info("QUERY " + i++ + ": " + queryStr);
            Log.info("================");
            Query query = new Query(queryStr, db.schemaGraph);

            Log.info("Parsing query with NL parser...");
            StanfordNLParser.parse(query, lexiParser);

            List<CoreLabel> rawWords = Sentence.toCoreLabelList(query.sentence.outputWords); // use Stanford parser to parse a sentence;
            Tree parse = lexiParser.apply(rawWords);
            TreebankLanguagePack tlp = new PennTreebankLanguagePack();
            GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
            GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
            List<TypedDependency> dependencyList = gs.typedDependencies(false);

            for (TaggedWord tagged : parse.taggedYield()) {
                System.out.println(tagged.word() + ", " + tagged.tag());
            }

            for (TypedDependency dep : dependencyList) {
                System.out.println(dep);
            }

            Log.info("Mapping nodes to token types...");
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document tokens = builder.parse(new File("libs/tokens.xml"));
                NodeMapper.phraseProcess(query, db, tokens);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            System.out.println("PARSE TREE:");
            System.out.println(query.parseTree);
            System.out.println();

            List<ParseTreeNode> mappedNodes = new ArrayList<>();
            List<ParseTreeNode> mappedNodesToRemove = new ArrayList<>();

            for (ParseTreeNode node : query.parseTree.allNodes) {
                boolean isNameToken = node.tokenType.equals("NT");
                boolean isValueToken = node.tokenType.startsWith("VT");

                // TODO: move all this logic to Fei's components
                if (isNameToken || isValueToken) {
                    mappedNodes.add(node);

                    // Check for related nodes that are auxiliary and delete
                    for (ParseTreeNode[] auxEntry : query.auxTable) {
                        ParseTreeNode relatedNode;
                        // If governing node
                        if (auxEntry[1].equals(node)) {
                            mappedNodesToRemove.add(auxEntry[0]);
                        }
                    }

                    // In the case that we have a function as a parent, add accordingly and "ignore" function
                    if (node.parent.tokenType.equals("FT")) {
                        node.attachedFT = node.parent.function;
                        ParseTreeNode functionNode = node.parent;
                        for (ParseTreeNode funcChild : functionNode.children) {
                            funcChild.parent = node;
                            node.children.add(funcChild);
                        }
                        node.parent = functionNode.parent;
                        functionNode.parent.children.remove(functionNode);
                    }

                    // Do similar operation if function is child
                    List<ParseTreeNode> funcToRemove = new ArrayList<>();
                    List<ParseTreeNode> childrenToAdd = new ArrayList<>();
                    for (ParseTreeNode child : node.children) {
                        if (child.tokenType.equals("FT")) {
                            node.attachedFT = child.function;
                            childrenToAdd.addAll(child.children);
                            funcToRemove.add(child);
                        }
                    }
                    node.children.removeAll(funcToRemove);
                    node.children.addAll(childrenToAdd);
                }

                // In the case we have a VT related to an NT, and they share an "amod" (adjective modifier)
                // or "num" (number modifier) or "nn" (noun compound modifier) relationship
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

                        MappedSchemaElement chosenMappedSchemaEl = null;
                        int choice = node.choice;
                        double nodeSimilarity = node.getChoiceMap().similarity;
                        double maxSimilarity = node.getChoiceMap().similarity;

                        for (int j = 0; j < node.mappedElements.size(); j++) {
                            MappedSchemaElement nodeMappedEl = node.mappedElements.get(j);

                            for (int k = 0; k < relatedNode.mappedElements.size(); k++) {
                                MappedSchemaElement relatedMappedEl = relatedNode.mappedElements.get(k);

                                if (nodeMappedEl.schemaElement.equals(relatedMappedEl.schemaElement)) {
                                    // Somewhat arbitrarily combine their similarity by averaging
                                    double combinedScore = (nodeSimilarity + relatedMappedEl.similarity) / 2;

                                    if (combinedScore > maxSimilarity) {
                                        chosenMappedSchemaEl = nodeMappedEl;
                                        maxSimilarity = combinedScore;
                                        choice = j;
                                    }
                                }
                            }
                        }

                        if (chosenMappedSchemaEl != null) {
                            chosenMappedSchemaEl.similarity = maxSimilarity;
                            node.choice = choice;
                            mappedNodesToRemove.add(relatedNode);
                        }
                    }
                }
            }
            mappedNodes.removeAll(mappedNodesToRemove);

            PrintForCheck.allParseTreeNodePrintForCheck(query.parseTree);
            System.out.println();

            TemplateChooser tc = new TemplateChooser(db.schemaGraph.relations);
            List<PossibleTranslation> translations = tc.generatePossibleTranslationsRecursive(query.parseTree, mappedNodes, null, null, null, 1, 0);

            int n = 10;

            Log.info("============");
            Log.info("TRANSLATIONS");
            Log.info("============");
            translations.sort((a, b) -> b.getTotalScore().compareTo(a.getTotalScore()));

            List<PossibleTranslation> topNTranslations = translations.subList(0, Math.min(translations.size(), n));
            topNTranslations.stream().map(PossibleTranslation::toStringDebug).forEach(System.out::println);

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

            System.out.println();
            Log.info("============");
            Log.info("FINAL RESULTS");
            Log.info("============");
            results.subList(0, Math.min(10, results.size())).forEach(System.out::println);

            System.out.println();
        }

    }
}
