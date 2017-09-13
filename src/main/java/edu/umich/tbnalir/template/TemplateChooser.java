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
                                                                           double accumScore) {
        List<PossibleTranslation> result = new ArrayList<>();

        if (accumRel == null) accumRel = new HashSet<>();
        if (accumProj == null) accumProj = new ArrayList<>();
        if (accumPred == null) accumPred = new ArrayList<>();

        // Base case: generate current possible translations now
        if (remainingNodes.size() == 0) {
            PossibleTranslation translation = new PossibleTranslation();
            translation.setRelations(new HashSet<>(accumRel));
            translation.setProjections(new ArrayList<>(accumProj));
            translation.setPredicates(new ArrayList<>(accumPred));
            translation.setTranslationScore(accumScore);
            result.add(translation);
            return result;
        }

        ParseTreeNode curNode = remainingNodes.remove(0);

        if (curNode.tokenType.equals("NT")) {
            // Add to accumulated projections if NT (name type)
            for (MappedSchemaElement schemaEl : curNode.mappedElements) {
                Relation rel = this.relations.get(schemaEl.schemaElement.relation.name);
                if (rel == null) throw new RuntimeException("Relation " + schemaEl.schemaElement.relation.name + " not found.");

                Attribute attr = rel.getAttributes().get(schemaEl.schemaElement.name);
                if (attr == null) throw new RuntimeException("Attribute " + schemaEl.schemaElement.name + " not found.");

                // Copy relevant structures so recursive operations don't interfere with it
                Set<Relation> newAccumRel = new HashSet<>(accumRel);
                List<Projection> newAccumProj = new ArrayList<>(accumProj);
                List<Predicate> newAccumPred = new ArrayList<>(accumPred);

                String alias = rel.getName();
                Projection proj = new Projection(alias, attr);
                if (!newAccumProj.contains(proj)) newAccumProj.add(proj);
                newAccumRel.add(rel);

                result.addAll(this.generatePossibleTranslationsRecursive(parseTree, new ArrayList<>(remainingNodes),
                        newAccumRel, newAccumProj, newAccumPred, accumScore * schemaEl.similarity));
            }
        } else if (curNode.tokenType.startsWith("VT")) {
            // Add to accumulated projections if VTTEXT or VTNUM (value type)
            for (MappedSchemaElement schemaEl : curNode.mappedElements) {
                Relation rel = this.relations.get(schemaEl.schemaElement.relation.name);
                if (rel == null) throw new RuntimeException("Relation " + schemaEl.schemaElement.relation.name + " not found.");

                Attribute attr = rel.getAttributes().get(schemaEl.schemaElement.name);
                if (attr == null) throw new RuntimeException("Attribute " + schemaEl.schemaElement.name + " not found.");

                Operator op = null;
                String value = null;

                // Try to find nearby node with operator token
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

                if (op == null) op = Operator.EQ;

                if (schemaEl.choice != -1) {
                    // TODO: open up the possibility that you have more than 1 mapped value?
                    if (value == null) {
                        value = schemaEl.mappedValues.get(0);
                    }
                }

                Predicate pred = new Predicate(attr, op, value);

                // Copy relevant structures so recursive operations don't interfere with it
                Set<Relation> newAccumRel = new HashSet<>(accumRel);
                List<Projection> newAccumProj = new ArrayList<>(accumProj);
                List<Predicate> newAccumPred = new ArrayList<>(accumPred);

                if (!newAccumPred.contains(pred)) newAccumPred.add(pred);
                newAccumRel.add(rel);

                result.addAll(this.generatePossibleTranslationsRecursive(parseTree, new ArrayList<>(remainingNodes),
                        newAccumRel, newAccumProj, newAccumPred, accumScore * schemaEl.similarity));
            }
        }
        return result;
    }

    public static void main(String[] args) {
        String dbName = "mas";
        String prefix = "data/mas/mas";
        int joinLevel = 4;

        RDBMS db;
        try {
            db = new RDBMS(dbName, prefix);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        SchemaDataTemplateGenerator tg = new SchemaDataTemplateGenerator(db, joinLevel);
        Set<Template> templates = tg.generate();

        // String queryStr = "return me the publications written by \"H. V. Jagadish\" and \"Yunyao Li\" on PVLDB after 2005.";
        String queryStr = "return me the homepage of PVLDB.";
        // String queryStr = "return the homepage of the journal which has name PVLDB";
        // String queryStr = "can undergrads take 595?";
        Log.info("QUERY: " + queryStr);
        Query query = new Query(queryStr, db.schemaGraph);

        Log.info("Parsing query with NL parser...");
        LexicalizedParser lexiParser = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
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

        PrintForCheck.allParseTreeNodePrintForCheck(query.parseTree);
        System.out.println();

        List<ParseTreeNode> mappedNodes = new ArrayList<>();

        for (ParseTreeNode node : query.parseTree.allNodes) {
            if (node.tokenType.equals("NT") || node.tokenType.startsWith("VT")) mappedNodes.add(node);
        }

        TemplateChooser tc = new TemplateChooser(db.schemaGraph.relations);
        List<PossibleTranslation> translations = tc.generatePossibleTranslationsRecursive(query.parseTree, mappedNodes, null, null, null, 1);

        int n = 10;

        Log.info("============");
        Log.info("TRANSLATIONS");
        Log.info("============");
        translations.sort((a,b) -> b.getTotalScore().compareTo(a.getTotalScore()));

        List<PossibleTranslation> topNTranslations = translations.subList(0, Math.min(translations.size(), n));
        topNTranslations.stream().map(PossibleTranslation::toStringDebug).forEach(System.out::println);

        List<InstantiatedTemplate> results = new ArrayList<>();

        for (Template tmpl : templates) {
            for (PossibleTranslation trans : topNTranslations) {
                InstantiatedTemplate instance = tmpl.instantiate(trans);
                if (instance != null) {
                    results.add(instance);
                }
            }
        }

        results.sort((a, b) -> b.getTotalScore().compareTo(a.getTotalScore()));

        System.out.println();
        Log.info("============");
        Log.info("FINAL RESULTS");
        Log.info("============");
        results.subList(0, Math.min(20, results.size())).forEach(System.out::println);

        /*
        int nounNodeCount = 0;
        int numNodeCount = 0;
        int textNodeCount = 0;

        // Contains all possible combinations of relations from parse tree nodes
        List<Set<Relation>> relationSets = new ArrayList<>();

        for (ParseTreeNode node : query.parseTree.allNodes) {
            if (node.tokenType.equals("NT")) {
                nounNodeCount++;

                if (relationSets.isEmpty()) {
                    for (MappedSchemaElement mse : node.mappedElements) {
                        Relation rel = db.schemaGraph.relations.get(mse.schemaElement.relation.name);
                        if (rel == null)
                            throw new RuntimeException("Relation <" + mse.schemaElement.relation.name + "> not found!");

                        Set<Relation> relSet = new HashSet<>();
                        relSet.add(rel);
                        relationSets.add(relSet);
                    }
                } else {
                    for (Set<Relation> relSet : relationSets) {
                        for (MappedSchemaElement mse : node.mappedElements) {
                            Relation rel = db.schemaGraph.relations.get(mse.schemaElement.relation.name);
                            if (rel == null)
                                throw new RuntimeException("Relation <" + mse.schemaElement.relation.name + "> not found!");
                            relSet.add(rel);
                        }
                    }
                }

            } else if (node.tokenType.equals("VTTEXT")) {
                textNodeCount++;
            } else if (node.tokenType.equals("VTNUM")) {
                numNodeCount++;
            }
        }

        // TODO: static (regardless of NLQ) template score
        // Determined by:
        // 1) "importance"/centrality of tables (a la relative cardinality in Schema Summarization)
        // 2) combined popularity of tables in query log
        // 3) entropy of attributes in projection, if included
        // 4) entropy of attributes in predicates, if included

        // SLOT COUNTING
        // Find the template with the minimum amount of slots which can cover parse tree tokens
        /*
        List<Template> candidateTemplates = new ArrayList<>();
        for (Template template : templates) {
            // First try with attr/const templates
            if (template.getType().equals(TemplateType.NO_ATTR_CONST)) {
                int attrSlots = StringUtils.countMatches(template.toString(), Constants.COLUMN);
                int numSlots = StringUtils.countMatches(template.toString(), Constants.NUM);
                int strSlots = StringUtils.countMatches(template.toString(), Constants.STR);

                // TODO: counting commas might be wrong for number of tables but this is cheap
                int countTables = StringUtils.countMatches(template.toString(), ',') + 1;

                template.setCountTables(countTables);
                template.setCountAttrSlots(attrSlots);
                template.setCountNumberSlots(numSlots);
                template.setCountStringSlots(strSlots);
                template.setTotalSlots(attrSlots + countTables + numSlots + strSlots);

                // Check that there are enough slots for each object in the NLQ
                boolean coversNumSlots = (numSlots >= numNodeCount);
                boolean coversStrSlots = (strSlots >= textNodeCount);
                boolean coversNounSlots = ((countTables + attrSlots) >= nounNodeCount);

                // Include only ones for which we have attributes/relations we mapped to
                boolean coversRelations = false;
                for (Set<Relation> relationSet : relationSets) {
                    coversRelations = template.getRelations().containsAll(relationSet);
                    if (coversRelations) break;
                }

                if (coversNumSlots && coversStrSlots && coversNounSlots && coversRelations) {
                    candidateTemplates.add(template);
                }
            }
        }

        // Order candidate templates by total num slots ascending (minimize slots)
        candidateTemplates.sort((a, b) -> a.getTotalSlots().compareTo(b.getTotalSlots()));

        Log.info("===================");
        Log.info("CANDIDATE TEMPLATES (Count: " + candidateTemplates.size() + ")");
        Log.info("===================");
        for (Template t : candidateTemplates.subList(0, 20)) {
            System.out.println(t);
        }*/

        // TODO: similarity of query parse tree structure to template parse tree structure
    }
}
