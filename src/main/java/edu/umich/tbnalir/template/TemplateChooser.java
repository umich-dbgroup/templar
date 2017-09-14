package edu.umich.tbnalir.template;

import com.esotericsoftware.minlog.Log;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.*;
import edu.umich.tbnalir.components.EntityResolution;
import edu.umich.tbnalir.components.NodeMapper;
import edu.umich.tbnalir.components.StanfordNLParser;
import edu.umich.tbnalir.dataStructure.EntityPair;
import edu.umich.tbnalir.dataStructure.ParseTree;
import edu.umich.tbnalir.dataStructure.ParseTreeNode;
import edu.umich.tbnalir.dataStructure.Query;
import edu.umich.tbnalir.parse.DependencyInfo;
import edu.umich.tbnalir.parse.PossibleTranslation;
import edu.umich.tbnalir.parse.Word;
import edu.umich.tbnalir.rdbms.*;
import edu.umich.tbnalir.sql.Operator;
import edu.umich.tbnalir.sql.Predicate;
import edu.umich.tbnalir.tools.PrintForCheck;
import edu.umich.tbnalir.util.Constants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.CharSet;
import org.apache.commons.lang3.StringUtils;
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
                if (rel == null) throw new RuntimeException("Relation " + schemaEl.schemaElement.relation.name + " not found.");

                // If we're dealing with a relation, move ahead
                if (schemaEl.schemaElement.type.equals("relation")) {
                    Set<Relation> newAccumRel = new HashSet<>(accumRel);
                    newAccumRel.add(rel);

                    result.addAll(this.generatePossibleTranslationsRecursive(parseTree, new ArrayList<>(remainingNodes),
                            newAccumRel, new ArrayList<>(accumProj), new ArrayList<>(accumPred),
                            accumScore + schemaEl.similarity, accumNodes + 1));
                    continue;
                }

                Attribute attr = rel.getAttributes().get(schemaEl.schemaElement.name);
                if (attr == null) throw new RuntimeException("Attribute " + schemaEl.schemaElement.name + " not found.");

                String alias = rel.getName() + "_0";
                Projection proj = new Projection(alias, attr);

                Operator op = null;
                String value = null;

                // Treat as projection, if no mapped values
                if (schemaEl.mappedValues.isEmpty() || schemaEl.choice == -1) {
                    // Copy relevant structures so recursive operations don't interfere with it
                    Set<Relation> newAccumRel = new HashSet<>(accumRel);
                    List<Projection> newAccumProj = new ArrayList<>(accumProj);
                    List<Predicate> newAccumPred = new ArrayList<>(accumPred);

                    // If you are creating a projection and there already exists a predicate with the same attribute,
                    // then create an additional path of eliminating this projection.
                    boolean selfJoinFlag = false;
                    int aliasInt = 0;
                    for (Predicate pred : accumPred) {
                        if (pred.getAttr().equals(attr)) {
                            if (!selfJoinFlag) {
                                result.addAll(this.generatePossibleTranslationsRecursive(parseTree, new ArrayList<>(remainingNodes),
                                        new HashSet<>(accumRel), new ArrayList<>(accumProj), new ArrayList<>(accumPred),
                                        accumScore + 0.5, accumNodes + 1));
                            }
                            selfJoinFlag = true;
                            aliasInt++;
                        }
                    }

                    // Increment aliasInt for every shared projection
                    for (Projection curProj : accumProj) {
                        if (curProj.getAttribute().equals(proj.getAttribute())) {
                            aliasInt++;
                        }
                    }
                    proj.setAlias(rel.getName() + "_" + aliasInt);

                    //if (!newAccumProj.contains(proj)) newAccumProj.add(proj);
                    newAccumProj.add(proj);
                    newAccumRel.add(rel);

                    // Penalize the similarity if this projection is not the child of a CMT (Command Token) node
                    double similarityAdded = schemaEl.similarity;
                    if (!curNode.parent.tokenType.equals("CMT")) {
                       similarityAdded *= 0.8;
                    }

                    result.addAll(this.generatePossibleTranslationsRecursive(parseTree, new ArrayList<>(remainingNodes),
                            newAccumRel, newAccumProj, newAccumPred, accumScore + similarityAdded, accumNodes + 1));
                } else {
                    // If you are creating a predicate and there already exists a projection with the same attribute,
                    // then create an additional path of eliminating this predicate.

                    int aliasInt = 0;

                    if (accumProj.contains(proj)) {
                        result.addAll(this.generatePossibleTranslationsRecursive(parseTree, new ArrayList<>(remainingNodes),
                                new HashSet<>(accumRel), new ArrayList<>(accumProj), new ArrayList<>(accumPred),
                                accumScore + 0.5, accumNodes + 1));

                        aliasInt++;
                    }

                    // Try to find nearby node with operator token if number
                    // TODO: possible that we might just want to check node's parents instead...
                    if (curNode.tokenType.equals("VTNUM")) {
                        int opDistanceLimit = 3;
                        for (int i = curNode.wordOrder - 1; i >= curNode.wordOrder - opDistanceLimit; i--) {
                            ParseTreeNode opNode = parseTree.searchNodeByOrder(i);
                            if (opNode != null && opNode.tokenType.equals("OT")) {
                                switch (opNode.function) {
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
                                if (op != null) break;
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
                    for (Predicate curPred : accumPred) {
                        if (curPred.getAttr().equals(pred.getAttr())) {
                            aliasInt++;
                        }
                    }

                    String predAlias = rel.getName() + "_" + aliasInt;
                    pred.setAlias(predAlias);

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
        int joinLevel = 5;

        String nlqFile = "data/mas/easy_queries_nlq.txt";

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

        /*
        List<String> queryStrs;
        try {
            queryStrs = FileUtils.readLines(new File(nlqFile), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }*/

        List<String> queryStrs = new ArrayList<>();
        queryStrs.add("return me the papers written by \"H. V. Jagadish\" and \"Divesh Srivastava\"");
        // queryStrs.add("return me the papers on VLDB conference after 2000."); // query 26
        // queryStrs.add("return me the papers by \"H. V. Jagadish\" on PVLDB after 2000."); // query 30
        // queryStrs.add("return me the papers by \"H. V. Jagadish\" on VLDB conference after 2000."); // query 31
        // queryStrs.add("return me all the keywords."); // query 35
        // queryStrs.add("return me the papers in PVLDB containing keyword \"Keyword search\""); // query 44
        // queryStrs.add("return me all the organizations in \"North America\""); // query 48
        // queryStrs.add("return me the keywords in the papers of \"University of Michigan\""); // query 42
        // queryStrs.add("return me the papers in VLDB conference containing keyword \"Information Retrieval\""); // query 45
        // queryStrs.add("return me the authors who have papers containing keyword \"Relational Database\""); // query 46
        // queryStrs.add("return me all the organizations."); // query 47
        // queryStrs.add("return me all the organizations in the databases area located in \"North America\""); // query 50
        // queryStrs.add("return me all the papers in \"University of Michigan\""); // query 54
        // queryStrs.add("return me all the papers after 2000 in \"University of Michigan\""); // query 55
        // queryStrs.add("return me all the papers in VLDB after 2000 in \"University of Michigan\""); // query 58
        // queryStrs.add("return me all the papers in PVLDB after 2000 in \"University of Michigan\""); // query 59

        int i = 0;
        for (String queryStr : queryStrs) {
            Log.info("================");
            Log.info("QUERY " + i++ + ": " + queryStr);
            Log.info("================");
            Query query = new Query(queryStr, db.schemaGraph);

            Log.info("Parsing query with NL parser...");
            StanfordNLParser.parse(query, lexiParser);

            /*
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
            }*/

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

                if (isNameToken || isValueToken) mappedNodes.add(node);

                // In the case we have a VT related to an NT, and they share an "amod" (adjective modifier)
                // relationship somehow...
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
                                    // Somewhat arbitrarily combine their similarity
                                    double combinedScore = nodeSimilarity + relatedMappedEl.similarity;

                                    if (combinedScore > maxSimilarity) {
                                        chosenMappedSchemaEl = nodeMappedEl;
                                        maxSimilarity = combinedScore;
                                        choice = j;
                                    }
                                }
                            }
                        }
                        mappedNodesToRemove.add(relatedNode);

                        if (chosenMappedSchemaEl != null) {
                            chosenMappedSchemaEl.similarity = maxSimilarity;
                            node.choice = choice;
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
                    InstantiatedTemplate inst = tmpl.instantiate(trans);
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
