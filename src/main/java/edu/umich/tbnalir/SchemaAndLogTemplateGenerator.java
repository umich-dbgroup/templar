package edu.umich.tbnalir;

import com.esotericsoftware.minlog.Log;
import com.opencsv.CSVWriter;
import edu.umich.tbnalir.rdbms.Attribute;
import edu.umich.tbnalir.rdbms.Relation;
import edu.umich.tbnalir.util.Utils;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.apache.commons.io.FilenameUtils;

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

        SelectBody sb = select.getSelectBody();

        if (!(sb instanceof PlainSelect))  return templates;

        PlainSelect ps = (PlainSelect) select.getSelectBody();

        List<Join> joins = ps.getJoins();
        if (joins != null && joins.size() > 0) {
            for (int i = 1; i <= joins.size(); i++) {
                // For each subset of join tables (from the first join to 1 less than the full amount of tables)
                List<Join> subsetJoins = joins.subList(0, i);
                List<FromItem> tableItems = new ArrayList<>();
                tableItems.add(ps.getFromItem());

                for (Join join : subsetJoins) {
                    tableItems.add(join.getRightItem());
                }

                // For each table, add its FK/PK and PK/FK relationships
                for (FromItem curItem : tableItems) {
                    // Clear out aliases
                    curItem.setAlias(null);

                    // Clean up to simplified schema names
                    String relationName;
                    if (curItem instanceof TableFunction) {
                        relationName = ((TableFunction) curItem).getFunction().getName();
                    } else {
                        relationName = curItem.toString();
                    }

                    String[] relationSplitName = relationName.split("\\.");
                    relationName = relationSplitName[relationSplitName.length - 1].toLowerCase();

                    Relation rel = stg.getRelations().get(relationName);
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
            if (stmt instanceof Select) {
                templates.addAll(generateSubsetExpansionJoins((Select) stmt, stg));
            }
        }
        return templates;
    }

    public static void performCrossValidation(LogTemplateGenerator ltg, List<Statement> stmts, Set<String> templates,
                                              String basename, Integer randomSeed) {
        // Split into n partitions for cross validation
        int cvSplits = 4;
        int partitionSize = stmts.size() / cvSplits;
        int remainder = stmts.size() % cvSplits;

        // Shuffle partitions so cross-validation is randomized
        Collections.sort(stmts, (o1, o2) -> o1.toString().compareTo(o2.toString()));
        Collections.shuffle(stmts, new Random(randomSeed));

        List<List<Statement>> cvPartitions = new ArrayList<>();
        int curIndex = 0;
        for (int i = 0; i < cvSplits; i++) {
            int fromIndex = curIndex;
            int toIndex = fromIndex + partitionSize;
            if (remainder > i) {
                toIndex += 1;
            }
            cvPartitions.add(stmts.subList(fromIndex, toIndex));
            curIndex = toIndex;
        }

        // Cross-validate for each set
        Log.info("Performing " + cvSplits + "-fold cross-validation...");

        Log.info("===== Legend =====");
        Log.info("1 <Abs. Constants> / 2 <Abs. Constants/Comparison Ops> / 3 <Abs. Full Predicates>");
        Log.info("a <Don't Abs. Projections> / b <Abs. Projections>");

        for (int i = 0; i < cvPartitions.size(); i++) {
            List<Statement> templateGenSet = new ArrayList<>();
            for (int j = 0; j < cvPartitions.size(); j++) {
                if (j != i) {
                    templateGenSet.addAll(cvPartitions.get(j));
                }
            }
            List<Statement> coverageTestSet = cvPartitions.get(i);

            String constFileName = null;
            String constProjFileName = null;
            String compFileName = null;
            String compProjFileName = null;
            String predFileName = null;
            String predProjFileName = null;
            if (i == 0) {
                constFileName = basename + "_const.csv";
                constProjFileName = basename + "_const_proj.csv";
                compFileName = basename + "_comp.csv";
                compProjFileName = basename + "_comp_proj.csv";
                predFileName = basename + "_pred.csv";
                predProjFileName = basename + "_pred_proj.csv";
            }
            // Only print out files for templates for first fold
            Set<String> constTmpl = ltg.generateTemplates(templateGenSet, ltg::noConstantTemplate, constFileName);
            constTmpl.addAll(templates);
            List<String> constTest = ltg.generateTestTemplates(coverageTestSet, ltg::noConstantTemplate);
            float constCoverage = (float) ltg.testCoverage(constTmpl, constTest) / coverageTestSet.size() * 100;

            Set<String> constProjTmpl = ltg.generateTemplates(templateGenSet, ltg::noConstantProjectionTemplate,
                    constProjFileName);
            constProjTmpl.addAll(templates);
            List<String> constProjTest = ltg.generateTestTemplates(coverageTestSet, ltg::noConstantProjectionTemplate);
            float constProjCoverage = (float) ltg.testCoverage(constProjTmpl, constProjTest) / coverageTestSet.size() * 100;

            Set<String> compTmpl = ltg.generateTemplates(templateGenSet, ltg::noComparisonTemplate, compFileName);
            compTmpl.addAll(templates);
            List<String> compTest = ltg.generateTestTemplates(coverageTestSet, ltg::noComparisonTemplate);
            float compCoverage = (float) ltg.testCoverage(compTmpl, compTest) / coverageTestSet.size() * 100;

            Set<String> compProjTmpl = ltg.generateTemplates(templateGenSet, ltg::noComparisonProjectionTemplate,
                    compProjFileName);
            compProjTmpl.addAll(templates);
            List<String> compProjTest = ltg.generateTestTemplates(coverageTestSet, ltg::noComparisonProjectionTemplate);
            float compProjCoverage = (float) ltg.testCoverage(compProjTmpl, compProjTest) / coverageTestSet.size() * 100;

            Set<String> predTmpl = ltg.generateTemplates(templateGenSet, ltg::noPredicateTemplate, predFileName);
            predTmpl.addAll(templates);
            List<String> predTest = ltg.generateTestTemplates(coverageTestSet, ltg::noPredicateTemplate);
            float predCoverage = (float) ltg.testCoverage(predTmpl, predTest) / coverageTestSet.size() * 100;

            Set<String> predProjTmpl = ltg.generateTemplates(templateGenSet, ltg::noPredicateProjectionTemplate,
                    predProjFileName);
            predProjTmpl.addAll(templates);
            List<String> predProjTest = ltg.generateTestTemplates(coverageTestSet, ltg::noPredicateProjectionTemplate);
            float predProjCoverage = (float) ltg.testCoverage(predProjTmpl, predProjTest) / coverageTestSet.size() * 100;

            Log.info("--- Fold " + i + " ---");
            Log.info("Template Gen. Set Size: " + templateGenSet.size());
            Log.info("Coverage Test Set Size: " + coverageTestSet.size());
            Log.info("           \t1a\t1b\t2a\t2b\t3a\t3b");
            Log.info("Coverage %:\t"
                    + String.format("%.1f", constCoverage) + "%\t"
                    + String.format("%.1f", constProjCoverage) + "%\t"
                    + String.format("%.1f", compCoverage) + "%\t"
                    + String.format("%.1f", compProjCoverage) + "%\t"
                    + String.format("%.1f", predCoverage) + "%\t"
                    + String.format("%.1f", predProjCoverage) + "%\t");
            Log.info("Tmpl. Count:\t"
                    + constTmpl.size() + "\t"
                    + constProjTmpl.size() + "\t"
                    + compTmpl.size() + "\t"
                    + compProjTmpl.size() + "\t"
                    + predTmpl.size() + "\t"
                    + predProjTmpl.size() + "\t\n");
        }
    }

    public static void performFixedTestSet(LogTemplateGenerator ltg, List<Statement> stmts, Set<String> templates,
                             Float logPercent, Integer numLogQueries, boolean randomizeLogOrder,
                                           String templateFileName, String errorFileName) {
        if (randomizeLogOrder) Collections.shuffle(stmts);    // Randomize order

        // Separate generation segment of log from test segment of log
        List<Statement> generationQueryLog = null;
        if (logPercent != null) {
            double generationSize = Math.floor(logPercent * stmts.size());
            generationQueryLog = stmts.subList(0, (int) generationSize);
        } else if (numLogQueries != null) {
            generationQueryLog = stmts.subList(0, numLogQueries);
        } else {
            Log.error("Need to specify either log % number or number of queries to use from log.");
            System.exit(1);
        }

        // Fix test query log as last 50% no matter what
        double halfLog = Math.floor(0.5 * stmts.size());
        List<Statement> testQueryLog = stmts.subList((int) halfLog, stmts.size() - 1);

        Log.info("==============================");
        Log.info("Creating templates from log...");
        templates.addAll(ltg.generate(generationQueryLog));
        Log.info("==============================\n");

            /*
            Log.info("==============================");
            Log.info("Creating subset expansion join templates from logs...");
            templates.addAll(generateSubsetExpansionJoinsAll(generationQueryLog, stg));
            Log.info("==============================\n");
            */

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

        try {
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

    public static void main(String[] args) {
        if (args.length < 8 && !args[3].equals("cv")) {
            System.err.println("Usage: <schema-prefix> <query-log-parsed> <join-level> <log-template-levels> <% of log used (50p)|number of queries> <random|temporal> <template-file-name> <error-file-name>");
            System.err.println("Example: SchemaAndLogTemplateGenerator data/sdss/schema/bestdr7" +
                    " data/sdss/final/bestdr7_0.05.parsed 0 pred_proj 50p random" +
                    " results/sdss_randomized/bestdr7_0.05.join0.pred_proj.50p.tmpl" +
                    " results/sdss_randomized/bestdr7_0.05.join0.pred_proj.50p.err");
            System.err.println("--- OR ---");
            System.err.println("Usage (for cross-validation): <schema-prefix> <query-log-parsed> <join-level> cv <random-seed>");
            System.exit(1);
        }

        String prefix = args[0];
        String relationsFile = prefix + ".relations.json";
        String edgesFile = prefix + ".edges.json";

        String queryLogFilename = args[1];

        Integer joinLevel = Integer.valueOf(args[2]);

        String[] logTemplateLevels;
        Float logPercent = null;
        Integer numLogQueries = null;
        Boolean randomizeLogOrder = false;
        String templateFileName = null;
        String errorFileName = null;
        Integer randomSeed = null;

        if (!args[3].equals("cv")) {
            logTemplateLevels = args[3].split(",");

            if (args[4].contains("p")) {
                logPercent = (float) Integer.valueOf(args[4].replaceAll("p", "")) / 100;
            } else {
                numLogQueries = Integer.valueOf(args[4]);
            }

            String randomArg = args[5];
            randomizeLogOrder = randomArg.equals("random");

            templateFileName = args[6];
            errorFileName = args[7];
        } else {
            logTemplateLevels = "const,const_proj,comp,comp_proj,pred,pred_proj".split(",");
            randomSeed = Integer.valueOf(args[4]);
        }


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

            if (args[3].equals("cv")) {
                performCrossValidation(ltg, queryLogStmts, templates, FilenameUtils.getBaseName(prefix), randomSeed);
            } else {
                performFixedTestSet(ltg, queryLogStmts, templates, logPercent, numLogQueries,
                        randomizeLogOrder, templateFileName, errorFileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
