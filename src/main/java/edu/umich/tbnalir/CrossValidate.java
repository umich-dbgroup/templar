package edu.umich.tbnalir;

import com.esotericsoftware.minlog.Log;
import edu.umich.tbnalir.rdbms.RDBMS;
import edu.umich.tbnalir.template.SchemaDataTemplateGenerator;
import edu.umich.tbnalir.template.Template;
import edu.umich.tbnalir.template.TemplateRoot;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Created by cjbaik on 8/31/17.
 */
public class CrossValidate {
    PrintWriter outWriter;
    PrintWriter errWriter;

    public CrossValidate(String outFileName, String errFileName) {
        try {
            this.outWriter = new PrintWriter(new FileWriter(outFileName));
            this.errWriter = new PrintWriter(new FileWriter(errFileName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void finish() {
        this.outWriter.close();
        this.errWriter.close();
    }

    public float calculateCoveragePercent(Collection<Template> generatedTemplates, Collection<Select> testStatements,
                                   Function<Select, Template> templateFn, boolean writeResults) {
        float covered = 0;

        for (Select select : testStatements) {
            Template testTemplate = templateFn.apply(select);
            if (generatedTemplates.contains(testTemplate)) {
                covered++;
            } else {
                if (writeResults) {
                    this.errWriter.println(testTemplate);
                }
            }
        }

        if (writeResults) generatedTemplates.stream().forEach(this.outWriter::println);

        return covered / testStatements.size() * 100;
    }

    public void performCrossValidation(SchemaDataTemplateGenerator schemaDataGen, LogTemplateGenerator logGen,
                                              List<Select> stmts, int randomSeed, int cvSplits) {
        // Generate templates from SchemaDataTemplateGenerator
        Set<Template> schemaDataTemplates = schemaDataGen.generate();
        List<Template> schemaDataNoPredProj = new ArrayList<>();
        List<Template> schemaDataNoPred = new ArrayList<>();
        List<Template> schemaDataNoCompProj = new ArrayList<>();
        List<Template> schemaDataNoComp = new ArrayList<>();
        List<Template> schemaDataNoConstProj = new ArrayList<>();
        List<Template> schemaDataNoConst = new ArrayList<>();
        List<Template> schemaDataFull = new ArrayList<>();
        for (Template t : schemaDataTemplates) {
            switch (t.getType()) {
                case NO_PRED_PROJ:
                    schemaDataNoPredProj.add(t);
                    break;
                case NO_PRED:
                    schemaDataNoPred.add(t);
                    break;
                case NO_CONST_OP_PROJ:
                    schemaDataNoCompProj.add(t);
                    break;
                case NO_CONST_OP:
                    schemaDataNoComp.add(t);
                    break;
                case NO_CONST_PROJ:
                    schemaDataNoConstProj.add(t);
                    break;
                case NO_CONST:
                    schemaDataNoConst.add(t);
                    break;
                case FULL_QUERY:
                    schemaDataFull.add(t);
                    break;
            }
        }

        // Split into partitions for cross validation
        int partitionSize = stmts.size() / cvSplits;
        int remainder = stmts.size() % cvSplits;

        // Shuffle partitions (deterministically) so cross-validation is randomized
        Collections.sort(stmts, (o1, o2) -> o1.toString().compareTo(o2.toString()));
        Collections.shuffle(stmts, new Random(randomSeed));

        List<List<Select>> cvPartitions = new ArrayList<>();
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
        Log.info("c <Abs. Constants> / cm <Abs. Constants/Comparison Ops> / pd <Abs. Full Predicates> / p <Abs. Projections>");

        for (int i = 0; i < cvPartitions.size(); i++) {
            List<Select> templateGenSet = new ArrayList<>();
            for (int j = 0; j < cvPartitions.size(); j++) {
                if (j != i) {
                    templateGenSet.addAll(cvPartitions.get(j));
                }
            }
            List<Select> coverageTestSet = cvPartitions.get(i);

            Set<Template> logPredProjTmpl = logGen.generate(templateGenSet, TemplateRoot::noPredicateProjectionTemplate);
            float logPredProjCoverage = this.calculateCoveragePercent(logPredProjTmpl, coverageTestSet, TemplateRoot::noPredicateProjectionTemplate, false);
            float schemaDataPredProjCoverage = this.calculateCoveragePercent(schemaDataNoPredProj, coverageTestSet, TemplateRoot::noPredicateProjectionTemplate, false);
            Set<Template> bothPredProjTmpl = new HashSet<>(logPredProjTmpl);
            bothPredProjTmpl.addAll(schemaDataNoPredProj);
            float bothPredProjCoverage = this.calculateCoveragePercent(bothPredProjTmpl, coverageTestSet, TemplateRoot::noPredicateProjectionTemplate, false);

            Set<Template> logPredTmpl = logGen.generate(templateGenSet, TemplateRoot::noPredicateTemplate);
            float logPredCoverage = this.calculateCoveragePercent(logPredTmpl, coverageTestSet, TemplateRoot::noPredicateTemplate, false);
            float schemaDataPredCoverage = this.calculateCoveragePercent(schemaDataNoPred, coverageTestSet, TemplateRoot::noPredicateTemplate, false);
            Set<Template> bothPredTmpl = new HashSet<>(logPredTmpl);
            bothPredTmpl.addAll(schemaDataNoPred);
            float bothPredCoverage = this.calculateCoveragePercent(bothPredTmpl, coverageTestSet, TemplateRoot::noPredicateTemplate, true);

            Set<Template> logCompProjTmpl = logGen.generate(templateGenSet, TemplateRoot::noComparisonProjectionTemplate);
            float logCompProjCoverage = this.calculateCoveragePercent(logCompProjTmpl, coverageTestSet, TemplateRoot::noComparisonProjectionTemplate, false);
            float schemaDataCompProjCoverage = this.calculateCoveragePercent(schemaDataNoCompProj, coverageTestSet, TemplateRoot::noComparisonProjectionTemplate, false);
            Set<Template> bothCompProjTmpl = new HashSet<>(logCompProjTmpl);
            bothCompProjTmpl.addAll(schemaDataNoCompProj);
            float bothCompProjCoverage = this.calculateCoveragePercent(bothCompProjTmpl, coverageTestSet, TemplateRoot::noComparisonProjectionTemplate, false);

            Set<Template> logCompTmpl = logGen.generate(templateGenSet, TemplateRoot::noComparisonTemplate);
            float logCompCoverage = this.calculateCoveragePercent(logCompTmpl, coverageTestSet, TemplateRoot::noComparisonTemplate, false);
            float schemaDataCompCoverage = this.calculateCoveragePercent(schemaDataNoComp, coverageTestSet, TemplateRoot::noComparisonTemplate, false);
            Set<Template> bothCompTmpl = new HashSet<>(logCompTmpl);
            bothCompTmpl.addAll(schemaDataNoComp);
            float bothCompCoverage = this.calculateCoveragePercent(bothCompTmpl, coverageTestSet, TemplateRoot::noComparisonTemplate, false);

            Set<Template> logConstProjTmpl = logGen.generate(templateGenSet, TemplateRoot::noConstantProjectionTemplate);
            float logConstProjCoverage = this.calculateCoveragePercent(logConstProjTmpl, coverageTestSet, TemplateRoot::noConstantProjectionTemplate, false);
            float schemaDataConstProjCoverage = this.calculateCoveragePercent(schemaDataNoConstProj, coverageTestSet, TemplateRoot::noConstantProjectionTemplate, false);
            Set<Template> bothConstProjTmpl = new HashSet<>(logConstProjTmpl);
            bothConstProjTmpl.addAll(schemaDataNoConstProj);
            float bothConstProjCoverage = this.calculateCoveragePercent(bothConstProjTmpl, coverageTestSet, TemplateRoot::noConstantProjectionTemplate, false);

            Set<Template> logConstTmpl = logGen.generate(templateGenSet, TemplateRoot::noConstantTemplate);
            float logConstCoverage = this.calculateCoveragePercent(logConstTmpl, coverageTestSet, TemplateRoot::noConstantTemplate, false);
            float schemaDataConstCoverage = this.calculateCoveragePercent(schemaDataNoConst, coverageTestSet, TemplateRoot::noConstantTemplate, false);
            Set<Template> bothConstTmpl = new HashSet<>(logConstTmpl);
            bothConstTmpl.addAll(schemaDataNoConst);
            float bothConstCoverage = this.calculateCoveragePercent(bothConstTmpl, coverageTestSet, TemplateRoot::noConstantTemplate, false);

            Set<Template> logFullTmpl = logGen.generate(templateGenSet, TemplateRoot::fullQueryTemplate);
            float logFullCoverage = this.calculateCoveragePercent(logFullTmpl, coverageTestSet, TemplateRoot::fullQueryTemplate, false);
            float schemaDataFullCoverage = this.calculateCoveragePercent(schemaDataFull, coverageTestSet, TemplateRoot::fullQueryTemplate, false);
            Set<Template> bothFullTmpl = new HashSet<>(logFullTmpl);
            bothFullTmpl.addAll(schemaDataFull);
            float bothFullCoverage = this.calculateCoveragePercent(bothFullTmpl, coverageTestSet, TemplateRoot::fullQueryTemplate, false);

            Log.info("--- Fold " + i + " ---");
            Log.info("Template Gen. Set Size: " + templateGenSet.size());
            Log.info("Coverage Test Set Size: " + coverageTestSet.size());
            Log.info("           \tc\tc_p\tcm\tcm_p\tpd\tpd_p\tfull");
            Log.info("Log Coverage %:\t"
                    + String.format("%.1f", logConstCoverage) + "%\t"
                    + String.format("%.1f", logConstProjCoverage) + "%\t"
                    + String.format("%.1f", logCompCoverage) + "%\t"
                    + String.format("%.1f", logCompProjCoverage) + "%\t"
                    + String.format("%.1f", logPredCoverage) + "%\t"
                    + String.format("%.1f", logPredProjCoverage) + "%\t"
                    + String.format("%.1f", logFullCoverage) + "%\t");
            Log.info("Log Tmpl. Count:\t"
                    + logConstTmpl.size() + "\t"
                    + logConstProjTmpl.size() + "\t"
                    + logCompTmpl.size() + "\t"
                    + logCompProjTmpl.size() + "\t"
                    + logPredTmpl.size() + "\t"
                    + logPredProjTmpl.size() + "\t"
                    + logFullTmpl.size() + "\t\n");
            Log.info("Schema/Data Coverage %:\t"
                    + String.format("%.1f", schemaDataConstCoverage) + "%\t"
                    + String.format("%.1f", schemaDataConstProjCoverage) + "%\t"
                    + String.format("%.1f", schemaDataCompCoverage) + "%\t"
                    + String.format("%.1f", schemaDataCompProjCoverage) + "%\t"
                    + String.format("%.1f", schemaDataPredCoverage) + "%\t"
                    + String.format("%.1f", schemaDataPredProjCoverage) + "%\t"
                    + String.format("%.1f", schemaDataFullCoverage) + "%\t");
            Log.info("Schema/Data Tmpl. Count:\t"
                    + schemaDataNoConst.size() + "\t"
                    + schemaDataNoConstProj.size() + "\t"
                    + schemaDataNoComp.size() + "\t"
                    + schemaDataNoCompProj.size() + "\t"
                    + schemaDataNoPred.size() + "\t"
                    + schemaDataNoPredProj.size() + "\t"
                    + schemaDataFull.size() + "\t\n");
            Log.info("Combined Coverage %:\t"
                    + String.format("%.1f", bothConstCoverage) + "%\t"
                    + String.format("%.1f", bothConstProjCoverage) + "%\t"
                    + String.format("%.1f", bothCompCoverage) + "%\t"
                    + String.format("%.1f", bothCompProjCoverage) + "%\t"
                    + String.format("%.1f", bothPredCoverage) + "%\t"
                    + String.format("%.1f", bothPredProjCoverage) + "%\t"
                    + String.format("%.1f", bothFullCoverage) + "%\t");
            Log.info("Combined Tmpl. Count:\t"
                    + bothConstTmpl.size() + "\t"
                    + bothConstProjTmpl.size() + "\t"
                    + bothCompTmpl.size() + "\t"
                    + bothCompProjTmpl.size() + "\t"
                    + bothPredTmpl.size() + "\t"
                    + bothPredProjTmpl.size() + "\t"
                    + bothFullTmpl.size() + "\t\n");
        }
    }


    private Runnable getParserRunnable(ConcurrentLinkedQueue<Select> stmts, String sql) {
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
            if (stmt == null || !(stmt instanceof Select)) return; // Case that it's not a select statement
            stmts.add((Select) stmt);
        };
    }

    public List<Select> parseStatements(String queryLogFilename) {
        try {
            List<String> sqls = Files.readAllLines(Paths.get(queryLogFilename));
            ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            ConcurrentLinkedQueue<Select> stmts = new ConcurrentLinkedQueue<>();
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            System.err.println("Usage: CrossValidate <db-name> <schema-filename-prefix> <max-join-level> <query-log-filename> <random seed>");
            System.err.println("Example: CrossValidate mas data/mas/mas 1 data/mas/easy_queries_sql.txt 1234");
            System.exit(1);
        }

        String dbName = args[0];
        String prefix = args[1];
        String relationsFile = prefix + ".relations.json";
        String edgesFile = prefix + ".edges.json";

        Integer joinLevel = Integer.valueOf(args[2]);
        String queryLogFilename = args[3];
        Integer randomSeed = Integer.valueOf(args[4]);

        RDBMS db = null;
        try {
            db = new RDBMS(dbName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        SchemaDataTemplateGenerator schemaDataGen = new SchemaDataTemplateGenerator(relationsFile, edgesFile, joinLevel, db);
        LogTemplateGenerator logGen = new LogTemplateGenerator();

        int cvSplits = 4;
        CrossValidate cv = new CrossValidate("templates.out", "errors.out");
        List<Select> stmts = cv.parseStatements(queryLogFilename);
        cv.performCrossValidation(schemaDataGen, logGen, stmts, randomSeed, cvSplits);
        cv.finish();
    }
}
