package edu.umich.tbnalir;

import com.esotericsoftware.minlog.Log;
import com.opencsv.CSVReader;
import edu.umich.tbnalir.util.Utils;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.apache.commons.io.FilenameUtils;

import java.io.FileReader;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Function;

/**
 * Created by cjbaik on 6/20/17.
 */
public class LogTemplateGenerator extends TemplateGenerator {
    public static void performCrossValidation(List<Statement> stmts, String basename) {
        TemplateGenerator tg = new LogTemplateGenerator();

        // Split into n partitions for cross validation
        int cvSplits = 4;
        int partitionSize = stmts.size() / cvSplits;
        int remainder = stmts.size() % cvSplits;

        // Shuffle partitions so cross-validation is randomized
        Collections.shuffle(stmts);

        List<List<Statement>> cvPartitions = new ArrayList<List<Statement>>();
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
            List<Statement> templateGenSet = new ArrayList<Statement>();
            for (int j = 0; j < cvPartitions.size(); j++) {
                if (j != i) {
                    templateGenSet.addAll(cvPartitions.get(j));
                }
            }
            List<Statement> coverageTestSet = cvPartitions.get(i);

            // Only print out files for templates for first fold
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
            Set<String> constTmpl = tg.generateTemplates(templateGenSet, tg::noConstantTemplate, constFileName);
            List<String> constTest = tg.generateTestTemplates(coverageTestSet, tg::noConstantTemplate);
            float constCoverage = (float) tg.testCoverage(constTmpl, constTest) / coverageTestSet.size() * 100;

            Set<String> constProjTmpl = tg.generateTemplates(templateGenSet, tg::noConstantProjectionTemplate,
                    constProjFileName);
            List<String> constProjTest = tg.generateTestTemplates(coverageTestSet, tg::noConstantProjectionTemplate);
            float constProjCoverage = (float) tg.testCoverage(constProjTmpl, constProjTest) / coverageTestSet.size() * 100;

            Set<String> compTmpl = tg.generateTemplates(templateGenSet, tg::noComparisonTemplate, compFileName);
            List<String> compTest = tg.generateTestTemplates(coverageTestSet, tg::noComparisonTemplate);
            float compCoverage = (float) tg.testCoverage(compTmpl, compTest) / coverageTestSet.size() * 100;

            Set<String> compProjTmpl = tg.generateTemplates(templateGenSet, tg::noComparisonProjectionTemplate, compProjFileName);
            List<String> compProjTest = tg.generateTestTemplates(coverageTestSet, tg::noComparisonProjectionTemplate);
            float compProjCoverage = (float) tg.testCoverage(compProjTmpl, compProjTest) / coverageTestSet.size() * 100;

            Set<String> predTmpl = tg.generateTemplates(templateGenSet, tg::noPredicateTemplate, predFileName);
            List<String> predTest = tg.generateTestTemplates(coverageTestSet, tg::noPredicateTemplate);
            float predCoverage = (float) tg.testCoverage(predTmpl, predTest) / coverageTestSet.size() * 100;

            Set<String> predProjTmpl = tg.generateTemplates(templateGenSet, tg::noPredicateProjectionTemplate, predProjFileName);
            List<String> predProjTest = tg.generateTestTemplates(coverageTestSet, tg::noPredicateProjectionTemplate);
            float predProjCoverage = (float) tg.testCoverage(predProjTmpl, predProjTest) / coverageTestSet.size() * 100;

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

    public static List<Statement> parseStatements(String filename) {
        // Statistics
        int totalSQL = 0;
        int parsedSQL = 0;
        int lastUpdate = 0;

        // Tokens to replace from JSqlParser TokenMgrError, so the whole process doesn't crash
        char[] tokensToReplace = {'#', '\u0018', '\u00a0'};

        // Read in all statements first
        List<Statement> stmts = new ArrayList<Statement>();
        Log.info("Reading file <" + filename + ">...");
        try {
            CSVReader csvr = new CSVReader(new FileReader(filename));
            String [] nextLine;
            while ((nextLine = csvr.readNext()) != null) {
                totalSQL++;
                if (totalSQL >= (lastUpdate + 20000)) {
                    Log.info("Parsed " + totalSQL + " statements...");
                    lastUpdate = totalSQL;
                }

                if (nextLine.length < 4) continue;

                String sql = nextLine[3];

                for (char token : tokensToReplace) {
                    sql = sql.replace(token, '_');
                }

                Log.debug("ORIGINAL: " + sql.replace("\n", " "));
                Statement stmt;
                try {
                    stmt = CCJSqlParserUtil.parse(sql);
                } catch (JSQLParserException e) {
                    if (Log.DEBUG) e.printStackTrace();
                    continue;
                } catch (Throwable t) {
                    t.printStackTrace();
                    continue;
                }
                if (stmt == null) continue; // Case that it's not a select statement
                stmts.add(stmt);
                parsedSQL++;
            }
            csvr.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.info("===== Parsing Results =====");
        Log.info("Total Queries: " + totalSQL);
        Log.info("Correctly Parsed: " + parsedSQL + "/" + totalSQL + "\n");

        return stmts;
    }

    public static void main (String[] args) {
        if (args.length < 2) {
            Log.error("Usage: <querylog-csv> <levels (comma-sep)>");
            Log.error("Levels: 'const', 'const_proj', 'comp', 'comp_proj', 'pred', 'pred_proj'");
            System.exit(1);
        }

        String filename = args[0];
        String[] levels = args[1].split(",");
        if (args.length > 2 && args[2].equals("true")) {
            Log.DEBUG();
        }

        List<Statement> stmts = parseStatements(filename);

        // TODO: For testing, perform cross-validation:
        // performCrossValidation(stmts, FilenameUtils.getBaseName(filename));

        // Generate
        LogTemplateGenerator tg = new LogTemplateGenerator();
        for (String level : levels) {
            String basename = FilenameUtils.getBaseName(filename);
            String outfileName = basename + "_" + level + ".csv";

            switch (level) {
                case "const":
                    tg.generateTemplates(stmts, tg::noConstantTemplate, outfileName);
                    break;
                case "const_proj":
                    tg.generateTemplates(stmts, tg::noConstantProjectionTemplate, outfileName);
                    break;
                case "comp":
                    tg.generateTemplates(stmts, tg::noComparisonTemplate, outfileName);
                    break;
                case "comp_proj":
                    tg.generateTemplates(stmts, tg::noComparisonProjectionTemplate, outfileName);
                    break;
                case "pred":
                    tg.generateTemplates(stmts, tg::noPredicateTemplate, outfileName);
                    break;
                case "pred_proj":
                    tg.generateTemplates(stmts, tg::noPredicateProjectionTemplate, outfileName);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid level specified: <" + level + ">.");
            }
        }


    }
}
