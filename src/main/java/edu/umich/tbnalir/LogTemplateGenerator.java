package edu.umich.tbnalir;

import com.esotericsoftware.minlog.Log;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by cjbaik on 6/20/17.
 */
public class LogTemplateGenerator extends TemplateGenerator {
    List<String> levels;

    public LogTemplateGenerator(String[] levels) {
        this.levels = new ArrayList<>();
        for (String level : levels) {
            this.levels.add(level);
        }
    }

    public List<String> getLevels() {
        return levels;
    }

    public void performCrossValidation(List<Statement> stmts, String basename) {
        // Split into n partitions for cross validation
        int cvSplits = 4;
        int partitionSize = stmts.size() / cvSplits;
        int remainder = stmts.size() % cvSplits;

        // Shuffle partitions so cross-validation is randomized
        Collections.shuffle(stmts);

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
            Set<String> constTmpl = this.generateTemplates(templateGenSet, this::noConstantTemplate, constFileName);
            List<String> constTest = this.generateTestTemplates(coverageTestSet, this::noConstantTemplate);
            float constCoverage = (float) this.testCoverage(constTmpl, constTest) / coverageTestSet.size() * 100;

            Set<String> constProjTmpl = this.generateTemplates(templateGenSet, this::noConstantProjectionTemplate,
                    constProjFileName);
            List<String> constProjTest = this.generateTestTemplates(coverageTestSet, this::noConstantProjectionTemplate);
            float constProjCoverage = (float) this.testCoverage(constProjTmpl, constProjTest) / coverageTestSet.size() * 100;

            Set<String> compTmpl = this.generateTemplates(templateGenSet, this::noComparisonTemplate, compFileName);
            List<String> compTest = this.generateTestTemplates(coverageTestSet, this::noComparisonTemplate);
            float compCoverage = (float) this.testCoverage(compTmpl, compTest) / coverageTestSet.size() * 100;

            Set<String> compProjTmpl = this.generateTemplates(templateGenSet, this::noComparisonProjectionTemplate, compProjFileName);
            List<String> compProjTest = this.generateTestTemplates(coverageTestSet, this::noComparisonProjectionTemplate);
            float compProjCoverage = (float) this.testCoverage(compProjTmpl, compProjTest) / coverageTestSet.size() * 100;

            Set<String> predTmpl = this.generateTemplates(templateGenSet, this::noPredicateTemplate, predFileName);
            List<String> predTest = this.generateTestTemplates(coverageTestSet, this::noPredicateTemplate);
            float predCoverage = (float) this.testCoverage(predTmpl, predTest) / coverageTestSet.size() * 100;

            Set<String> predProjTmpl = this.generateTemplates(templateGenSet, this::noPredicateProjectionTemplate, predProjFileName);
            List<String> predProjTest = this.generateTestTemplates(coverageTestSet, this::noPredicateProjectionTemplate);
            float predProjCoverage = (float) this.testCoverage(predProjTmpl, predProjTest) / coverageTestSet.size() * 100;

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

    private Runnable getParserRunnable(ConcurrentLinkedQueue<Statement> stmts, String sql) {
        // Tokens to replace from JSqlParser TokenMgrError, so the whole process doesn't crash
        char[] tokensToReplace = {'#', '\u0018', '\u00a0', '\u2018', '\u201d', '\u00ac'};
        for (char token : tokensToReplace) {
            sql = sql.replace(token, '_');
        }
        final String finalSql = sql.toLowerCase();

        return () -> {
            Statement stmt;
            try {
                stmt = CCJSqlParserUtil.parse(finalSql);
            } catch (JSQLParserException e) {
                if (Log.DEBUG) e.printStackTrace();
                return;
            } catch (Throwable t) {
                t.printStackTrace();
                return;
            }
            if (stmt == null) return; // Case that it's not a select statement
            stmts.add(stmt);
        };
    }

    public List<Statement> parseStatements(List<String> sqls) {
        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        ConcurrentLinkedQueue<Statement> stmts = new ConcurrentLinkedQueue<>();
        for (String sql : sqls) {
            pool.submit(this.getParserRunnable(stmts, sql));
        }

        pool.shutdown();

        try {
            if (!pool.awaitTermination(10, TimeUnit.MINUTES)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    Log.error("Pool did not terminate");
                }
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        Log.info("===== Parsing Results =====");
        Log.info("Total Queries: " + sqls.size());
        Log.info("Correctly Parsed: " + stmts.size() + "/" + sqls.size() + "\n");

        return new ArrayList<>(stmts);
    }

    public void generateAndSave(List<Statement> stmts, String outBasename) {
        for (String level : this.levels) {
            String outfileName = outBasename + "_" + level + ".csv";
            switch (level) {
                case "const":
                    this.generateTemplates(stmts, this::noConstantTemplate, outfileName);
                    break;
                case "const_proj":
                    this.generateTemplates(stmts, this::noConstantProjectionTemplate, outfileName);
                    break;
                case "comp":
                    this.generateTemplates(stmts, this::noComparisonTemplate, outfileName);
                    break;
                case "comp_proj":
                    this.generateTemplates(stmts, this::noComparisonProjectionTemplate, outfileName);
                    break;
                case "pred":
                    this.generateTemplates(stmts, this::noPredicateTemplate, outfileName);
                    break;
                case "pred_proj":
                    this.generateTemplates(stmts, this::noPredicateProjectionTemplate, outfileName);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid level specified: <" + level + ">.");
            }
        }
    }

    public Set<String> generate(List<Statement> stmts) {
        Set<String> templates = null;
        for (String level : this.levels) {
            switch (level) {
                case "const":
                    templates = this.generateTemplates(stmts, this::noConstantTemplate, null);
                    break;
                case "const_proj":
                    templates = this.generateTemplates(stmts, this::noConstantProjectionTemplate, null);
                    break;
                case "comp":
                    templates = this.generateTemplates(stmts, this::noComparisonTemplate, null);
                    break;
                case "comp_proj":
                    templates = this.generateTemplates(stmts, this::noComparisonProjectionTemplate, null);
                    break;
                case "pred":
                    templates = this.generateTemplates(stmts, this::noPredicateTemplate, null);
                    break;
                case "pred_proj":
                    templates = this.generateTemplates(stmts, this::noPredicateProjectionTemplate, null);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid level specified: <" + level + ">.");
            }
        }
        return templates;
    }

    public List<String> readQueryLogParsed(String filename) {
        try {
            return FileUtils.readLines(new File(filename), Charset.forName("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public static void main (String[] args) {
        if (args.length < 2) {
            Log.error("Usage: <querylog.parsed> <levels (comma-sep)>");
            Log.error("Levels: 'const', 'const_proj', 'comp', 'comp_proj', 'pred', 'pred_proj'");
            System.exit(1);
        }

        String filename = args[0];
        String[] levels = args[1].split(",");
        if (args.length > 2 && args[2].equals("true")) {
            Log.DEBUG();
        }

        LogTemplateGenerator tg = new LogTemplateGenerator(levels);

        List<String> sqls = tg.readQueryLogParsed(filename);
        List<Statement> stmts = tg.parseStatements(sqls);

        // TODO: For testing, perform cross-validation:
        // tg.performCrossValidation(stmts, FilenameUtils.getBaseName(filename));

        // Generate
        String basename = FilenameUtils.getBaseName(filename);
        tg.generateAndSave(stmts, basename);
    }
}
