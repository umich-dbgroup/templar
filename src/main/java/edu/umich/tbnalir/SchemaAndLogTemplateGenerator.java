package edu.umich.tbnalir;

import com.esotericsoftware.minlog.Log;
import net.sf.jsqlparser.statement.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by cjbaik on 7/5/17.
 */
public class SchemaAndLogTemplateGenerator {
    public static boolean isCovered(LogTemplateGenerator ltg, Statement testStatement, Set<String> templates) {
        List<String> levels = new ArrayList<>(ltg.getLevels());

        // Add pred_proj by default because this is where schema templates come from
        levels.add("pred_proj");

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
                    break;
                default:
                    throw new IllegalArgumentException("Invalid level specified: <" + level + ">.");
            }

            if (templates.contains(template)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            System.err.println("Usage: <schema-prefix> <query-log-parsed> <join-level> <log-template-levels> <% of log used>");
            System.err.println("Example: SchemaAndLogTemplateGenerator data/sdss/schema/bestdr7 data/sdss/final/bestdr7_0.05.csv 0 pred_proj 50");
            System.exit(1);
        }

        String prefix = args[0];
        String relationsFile = prefix + ".relations.json";
        String edgesFile = prefix + ".edges.json";

        String queryLogFilename = args[1];

        Integer joinLevel = Integer.valueOf(args[2]);
        String[] logTemplateLevels = args[3].split(",");
        Float logPercent = (float) Integer.valueOf(args[4]) / 100;

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

            // Separate generation segment of log from test segment of log
            Collections.shuffle(queryLogStmts);    // Randomize order
            double generationSize = Math.floor(logPercent * queryLogStmts.size());
            List<Statement> generationQueryLog = queryLogStmts.subList(0, (int) generationSize);
            List<Statement> testQueryLog = queryLogStmts.subList((int) generationSize, queryLogStmts.size() - 1);

            Log.info("==============================");
            Log.info("Creating templates from log...");
            templates.addAll(ltg.generate(generationQueryLog));
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}