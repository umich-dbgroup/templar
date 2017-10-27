package edu.umich.templar;

import com.esotericsoftware.minlog.Log;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.umich.templar.components.NodeMapper;
import edu.umich.templar.components.StanfordNLParser;
import edu.umich.templar.dataStructure.ParseTreeNode;
import edu.umich.templar.dataStructure.Query;
import edu.umich.templar.qf.QFGraph;
import edu.umich.templar.qf.agnostic.AgnosticGraph;
import edu.umich.templar.rdbms.RDBMS;
import edu.umich.templar.rdbms.Relation;
import edu.umich.templar.template.InstantiatedTemplate;
import edu.umich.templar.template.SchemaDataTemplateGenerator;
import edu.umich.templar.template.Template;
import edu.umich.templar.template.Translation;
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
 * Created by cjbaik on 10/23/17.
 */
public class TemplarCV {
    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Usage: Templar <testset> <join-level> <agnostic-sql-train> <trans-mode> <log %>");
            System.out.println("Example: TemplarCV mas 6 yelp,imdb 2 10");
            System.exit(1);
        }
        String dbName = args[0];
        String prefix = "data/" + dbName + "/" + dbName;
        int joinLevel = Integer.valueOf(args[1]);
        String nlqFile = prefix + "_all.txt";
        String ansFile = prefix + "_all.ans";

        String[] agnosticSets = args[2].split(",");

        // Set translation mode (0 for NL only, 1 for NL + agnostic, 2 for NL + QF)
        Translation.MODE = Integer.valueOf(args[3]);

        // Log percent (10 means use 10% of training set, 25 => 25%, etc.)
        Double logPercent = Double.valueOf(args[4]) / 100;

        RDBMS db;
        try {
            db = new RDBMS(dbName, prefix);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        // Load all databases for each agnostic graph
        Map<String, Relation> agnosticRelations = new HashMap<>();
        for (String agnosticSet : agnosticSets) {
            String agnosticPrefix = "data/" + agnosticSet + "/" + agnosticSet;
            RDBMS agnosticDB;
            try {
                agnosticDB = new RDBMS(agnosticSet, agnosticPrefix);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            agnosticRelations.putAll(agnosticDB.schemaGraph.relations);
        }

        AgnosticGraph agnosticGraph = new AgnosticGraph(agnosticRelations);
        for (String agnosticSet: agnosticSets) {
            try {
                String agnosticSQLFile = "data/" + agnosticSet + "/" + agnosticSet + "_all.ans";
                List<String> otherSQLStr = FileUtils.readLines(new File(agnosticSQLFile), "UTF-8");
                List<Select> otherSQL = Utils.parseStatements(otherSQLStr);

                for (Select select : otherSQL) {
                    agnosticGraph.analyzeSelect((PlainSelect) select.getSelectBody());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<String> nlq = new ArrayList<>();
        List<List<String>> queryAnswers = new ArrayList<>();
        try {
            nlq = FileUtils.readLines(new File(nlqFile), "UTF-8");
            List<String> answerFileLines = FileUtils.readLines(new File(ansFile), "UTF-8");
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

        List<Integer> shuffleIndexes = new ArrayList<>();
        for (int i = 0; i < nlq.size(); i++) {
            shuffleIndexes.add(i);
        }
        Collections.shuffle(shuffleIndexes, new Random(1234));

        int numFolds = 4;
        List<List<String>> nlqFolds = new ArrayList<>();
        List<List<List<String>>> sqlFolds = new ArrayList<>();
        List<List<Integer>> shuffleIndexFolds = new ArrayList<>();
        for (int i = 0; i < numFolds; i++) {
            nlqFolds.add(new ArrayList<>());
            sqlFolds.add(new ArrayList<>());
            shuffleIndexFolds.add(new ArrayList<>());
        }

        // pass in nlq/sql pairs to each fold
        for (int i = 0; i < shuffleIndexes.size(); i++) {
            int foldIndex = i % numFolds;
            nlqFolds.get(foldIndex).add(nlq.get(shuffleIndexes.get(i)));
            sqlFolds.get(foldIndex).add(queryAnswers.get(shuffleIndexes.get(i)));
            shuffleIndexFolds.get(foldIndex).add(shuffleIndexes.get(i));
        }

        // Generate join paths
        SchemaDataTemplateGenerator tg = new SchemaDataTemplateGenerator(db, joinLevel);
        Set<Template> templates = tg.generate();

        // Read in Stanford Parser Model
        LexicalizedParser lexiParser = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");

        // MAIN LOOP
        int top1 = 0;
        int top3 = 0;
        int top5 = 0;
        Integer[] finalRanks = new Integer[nlq.size()];
        for (int i = 0; i < numFolds; i++) {
            Log.info("===== FOLD " + i + " =====");
            List<String> curFoldNLQ = nlqFolds.get(i);
            List<List<String>> curFoldAnswers = sqlFolds.get(i);
            List<Integer> curFoldShuffleIndexes = shuffleIndexFolds.get(i);

            List<String> logSQLStr = new ArrayList<>();
            for (int j = 0; j < numFolds; j++) {
                if (i != j) {
                    for (List<String> ans : sqlFolds.get(j)) {
                        // Ignore "NO ANSWER YET" answers
                        if (!ans.get(0).startsWith("NO")) {
                            logSQLStr.add(ans.get(0));
                        }
                    }
                }
            }

            int maxLogSize = ((Double) (logPercent * logSQLStr.size())).intValue();
            Collections.shuffle(logSQLStr);
            logSQLStr = logSQLStr.subList(0, maxLogSize);

            List<Select> logSQL = Utils.parseStatements(logSQLStr);
            QFGraph qfGraph = new QFGraph(db.schemaGraph.relations);

            for (Select select : logSQL) {
                qfGraph.analyzeSelect((PlainSelect) select.getSelectBody());
            }

            for (int j = 0; j < curFoldNLQ.size(); j++) {
                String queryStr = curFoldNLQ.get(j);

                Integer queryID = curFoldShuffleIndexes.get(j);

                Log.info("================");
                Log.info("QUERY " + queryID + ": " + queryStr);
                Log.info("================");

                // convert some state values used
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

                Templar.removeStopwords(Utils.stopwords, query);

                List<ParseTreeNode> mappedNodes = Templar.getMappedNodes(query);

                // qfGraph = null;
                // agnosticGraph = null;
                Templar tc = new Templar(db.schemaGraph.relations, agnosticGraph, qfGraph);
                List<Translation> translations = tc.generatePossibleTranslationsRecursive(mappedNodes,
                        new Translation(agnosticGraph, qfGraph));

                translations.sort((a, b) -> b.getScore().compareTo(a.getScore()));

                int n = 10;
                List<Translation> topNTranslations = translations.subList(0, Math.min(translations.size(), n));

                List<InstantiatedTemplate> results = new ArrayList<>();
                Map<String, Integer> resultIndexMap = new HashMap<>();

                for (Template tmpl : templates) {
                    for (Translation trans : topNTranslations) {
                        Set<Translation> perms = trans.getAliasPermutations();
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
                if (curFoldAnswers != null) {
                    Double correctResultScore = null;
                    for (int k = 0; k < Math.min(results.size(), 10); k++) {
                        if (correctResultScore != null) {
                            // If this score is less, then we have no ties and we just break
                            if (results.get(k).getScore() < correctResultScore) {
                                break;
                            }
                        }

                        if (curFoldAnswers.get(j).contains(results.get(k).getValue())) {
                            if (correctResultScore == null) {
                                correctResultScore = results.get(k).getScore();

                                rank = k + 1;
                                finalRanks[queryID] = rank;
                                if (rank <= 5) top5++;
                                if (rank <= 3) top3++;
                                if (rank == 1) top1++;
                            }
                        }
                    }
                }
                if (rank == null || rank > 1) {
                    System.out.println(Templar.getDebugOutput(lexiParser, query, topNTranslations, results.subList(0, Math.min(10, results.size()))));
                }

                // Answer not found
                if (finalRanks[queryID] == null) {
                    finalRanks[queryID] = -1;
                }

            }
        }

        Log.info("====== RANKS ======");
        for (int i = 0; i < finalRanks.length; i++) {
            Log.info("QUERY " + i + ": " + finalRanks[i]);
        }

        Log.info("==============");
        Log.info("SUMMARY");
        Log.info("==============");
        Log.info("Total queries: " + nlq.size());
        Log.info("Top 1: " + top1);
        Log.info("Top 3: " + top3);
        Log.info("Top 5: " + top5);
    }
}
