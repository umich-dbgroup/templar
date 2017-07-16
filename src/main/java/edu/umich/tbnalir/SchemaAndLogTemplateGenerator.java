package edu.umich.tbnalir;

import com.esotericsoftware.minlog.Log;
import com.opencsv.CSVWriter;
import edu.umich.tbnalir.rdbms.Attribute;
import edu.umich.tbnalir.rdbms.Relation;
import edu.umich.tbnalir.util.Utils;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by cjbaik on 7/5/17.
 */
public class SchemaAndLogTemplateGenerator {
    static Map<String, Integer> errorTemplates = new HashMap<>();

    public static boolean isCovered(LogTemplateGenerator ltg, Statement testStatement, Set<String> templates) {
        List<String> levels = new ArrayList<>(ltg.getLevels());

        // Add pred_proj by default because this is where schema templates come from
        levels.add("pred_proj");

        String errorCheckingTemplate = null;

        for (String level : levels) {
            String template = null;
            switch (level) {
                case "const":
                    template = ltg.noConstantTemplate(testStatement);
                    break;
                case "const_proj":
                    template = ltg.noConstantProjectionTemplate(testStatement);
                    break;
                case "comp":
                    template = ltg.noComparisonTemplate(testStatement);
                    break;
                case "comp_proj":
                    template = ltg.noComparisonProjectionTemplate(testStatement);
                    break;
                case "pred":
                    template = ltg.noPredicateTemplate(testStatement);
                    break;
                case "pred_proj":
                    template = ltg.noPredicateProjectionTemplate(testStatement);
                    errorCheckingTemplate = template;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid level specified: <" + level + ">.");
            }

            if (templates.contains(template)) {
                return true;
            }
        }
        Integer errorCount = errorTemplates.get(errorCheckingTemplate);
        if (errorCount == null) {
            errorTemplates.put(errorCheckingTemplate, 1);
        } else {
            errorTemplates.put(errorCheckingTemplate, errorCount + 1);
        }
        return false;
    }

    // If we have a statement with multiple joins, then generate variants from each level of join
    public static Set<String> generateSubsetExpansionJoins(Select select, SchemaTemplateGenerator stg) {
        Set<String> templates = new HashSet<>();

        PlainSelect ps = (PlainSelect) select.getSelectBody();

        List<Join> joins = ps.getJoins();
        if (joins != null && joins.size() > 0) {
            System.out.println("There's at least 1 join.");
            for (int i = 1; i <= joins.size(); i++) {
                System.out.println("For join subset 0 to " + (i-1) + "...");
                // For each subset of join tables (from the first join to 1 less than the full amount of tables)
                List<Join> subsetJoins = joins.subList(0, i);

                // For each table in the join, add its FK/PK and PK/FK relationships
                for (Join curJoin : subsetJoins) {
                    Relation rel = stg.getRelations().get(curJoin.getRightItem().toString().toLowerCase());
                    if (rel != null) {
                        for (Map.Entry<String, Attribute> attrEntry : rel.getAttributes().entrySet()) {
                            Set<Attribute> fks = stg.getPkfkEdges().get(attrEntry.getValue());
                            if (fks != null) {
                                for (Attribute fk : fks) {
                                    if (fk != null) {
                                        Join newFKJoin = stg.createSimpleJoinFromAttribute(fk);
                                        List<Join> newJoins = new ArrayList<>(subsetJoins);
                                        newJoins.add(newFKJoin);

                                        ps.setJoins(newJoins);
                                        templates.addAll(stg.generateTemplateVariants(stg::noPredicateProjectionTemplate, select));
                                    }
                                }
                            }
                        }

                        for (Map.Entry<String, Attribute> attrEntry : rel.getAttributes().entrySet()) {
                            Set<Attribute> pks = stg.getFkpkEdges().get(attrEntry.getValue());
                            if (pks != null) {
                                for (Attribute pk : pks) {
                                    if (pk != null) {
                                        Join newPKJoin = stg.createSimpleJoinFromAttribute(pk);
                                        List<Join> newJoins = new ArrayList<>(subsetJoins);
                                        newJoins.add(newPKJoin);

                                        templates.addAll(stg.generateTemplateVariants(stg::noPredicateProjectionTemplate, select));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Reset state for joins
        ps.setJoins(joins);

        return templates;
    }

    public static Set<String> generateSubsetExpansionJoinsAll(List<Statement> stmts, SchemaTemplateGenerator stg) {
        Set<String> templates = new HashSet<>();
        for (Statement stmt : stmts) {
            templates.addAll(generateSubsetExpansionJoins((Select) stmt, stg));
        }
        return templates;
    }

    public static void main(String[] args) {
        if (args.length < 8) {
            System.err.println("Usage: <schema-prefix> <query-log-parsed> <join-level> <log-template-levels> <% of log used (50p)|number of queries> <random|temporal> <template-file-name> <error-file-name>");
            System.err.println("Example: SchemaAndLogTemplateGenerator data/sdss/schema/bestdr7" +
                    " data/sdss/final/bestdr7_0.05.parsed 0 pred_proj 50p random" +
                    " results/sdss_randomized/bestdr7_0.05.join0.pred_proj.50p.tmpl" +
                    " results/sdss_randomized/bestdr7_0.05.join0.pred_proj.50p.err");
            System.exit(1);
        }

        String prefix = args[0];
        String relationsFile = prefix + ".relations.json";
        String edgesFile = prefix + ".edges.json";

        String queryLogFilename = args[1];

        Integer joinLevel = Integer.valueOf(args[2]);
        String[] logTemplateLevels = args[3].split(",");

        Float logPercent = null;
        Integer numLogQueries = null;
        if (args[4].contains("p")) {
            logPercent = (float) Integer.valueOf(args[4].replaceAll("p", "")) / 100;
        } else {
            numLogQueries = Integer.valueOf(args[4]);
        }

        String randomArg = args[5];
        boolean randomizeLogOrder = randomArg.equals("random");

        String templateFileName = args[6];
        String errorFileName = args[7];

        Log.info("==============================");
        Log.info("Creating templates from schema...");
        SchemaTemplateGenerator stg = new SchemaTemplateGenerator(relationsFile, edgesFile, joinLevel);
        Set<String> templates = stg.generate();
        Log.info("==============================\n");

        try {
            LogTemplateGenerator ltg = new LogTemplateGenerator(logTemplateLevels);

            Log.info("==============================");
            Log.info("Loading log into memory...");
            List<String> queryLogSql = ltg.readQueryLogParsed(queryLogFilename);
            Log.info("Parsing statements...");
            List<Statement> queryLogStmts = ltg.parseStatements(queryLogSql);
            Log.info("==============================\n");

            if (randomizeLogOrder) Collections.shuffle(queryLogStmts);    // Randomize order

            // Used for testing generateSubsetExpansionJoinsAll
            /*List<String> queryLogSql = new ArrayList<>();
            queryLogSql.add("SELECT * FROM PhotoPrimary LEFT JOIN SpecObjAll LEFT JOIN Galaxy".toLowerCase());
            List<Statement> queryLogStmts = ltg.parseStatements(queryLogSql);
            Set<String> tmpl = generateSubsetExpansionJoinsAll(queryLogStmts, stg);
            for (String t : tmpl) {
                System.out.println(t);
            }*/

            // Separate generation segment of log from test segment of log
            List<Statement> generationQueryLog = null;
            if (logPercent != null) {
                double generationSize = Math.floor(logPercent * queryLogStmts.size());
                generationQueryLog = queryLogStmts.subList(0, (int) generationSize);
            } else if (numLogQueries != null) {
                generationQueryLog = queryLogStmts.subList(0, numLogQueries);
            } else {
                Log.error("Need to specify either log % number or number of queries to use from log.");
                System.exit(1);
            }

            // Fix test query log as last 50% no matter what
            double halfLog = Math.floor(0.5 * queryLogStmts.size());
            List<Statement> testQueryLog = queryLogStmts.subList((int) halfLog, queryLogStmts.size() - 1);

            Log.info("==============================");
            Log.info("Creating templates from log...");
            templates.addAll(ltg.generate(generationQueryLog));
            Log.info("==============================\n");

            Log.info("==============================");
            Log.info("Creating subset expansion join templates from logs...");
            templates.addAll(generateSubsetExpansionJoinsAll(generationQueryLog, stg));
            Log.info("==============================\n");

            Log.info("==============================");
            Log.info("Measuring coverage...");
            int covered = 0;
            for (Statement stmt : testQueryLog) {
                if (isCovered(ltg, stmt, templates)) covered++;
            }

            float coverage = (float) covered / testQueryLog.size() * 100;
            Log.info("Number of templates: " + templates.size());
            Log.info(String.format("Coverage: %d / %d (%.1f)", covered, testQueryLog.size(), coverage) + "%");
            Log.info("==============================\n");

            PrintWriter templateWriter = new PrintWriter(templateFileName, "UTF-8");

            List<String> templateList = new ArrayList<>(templates);
            Collections.sort(templateList);

            for (String template : templateList) {
                templateWriter.println(template);
            }
            templateWriter.close();

            CSVWriter errorWriter = new CSVWriter(new FileWriter(errorFileName), '\t', CSVWriter.NO_QUOTE_CHARACTER);

            for (Map.Entry<String, Integer> e : Utils.sortByValueDesc(errorTemplates).entrySet()) {
                String[] row = new String[2];
                row[0] = e.getValue().toString();
                row[1] = e.getKey();
                errorWriter.writeNext(row);
            }
            errorWriter.close();

            Log.info("==============================");
            Log.info("Templates generated were printed to <" + templateFileName + ">.");
            Log.info("Templates not covered were printed to <" + errorFileName + ">.");
            Log.info("==============================");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
