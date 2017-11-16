package edu.umich.templar;

import com.esotericsoftware.minlog.Log;
import edu.umich.templar.rdbms.RDBMS;
import edu.umich.templar.rdbms.SchemaGraph;
import edu.umich.templar.template.SchemaDataTemplateGenerator;
import edu.umich.templar.template.Template;
import edu.umich.templar.template.TemplateRoot;
import edu.umich.templar.util.CoverageHelper;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.statement.select.Select;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by cjbaik on 8/31/17.
 */
public class CoverageSDSS extends CoverageHelper {
    public CoverageSDSS(String outFileName, String errFileName) {
        super(outFileName, errFileName);
    }

    public void performFixedTestSet(LogTemplateGenerator logGen, List<Select> stmts) {
        double halfLog = Math.floor(0.5 * stmts.size());
        List<Select> trainQueries = stmts.subList(0, (int) halfLog);
        List<Select> testQueries = stmts.subList((int) halfLog, stmts.size() - 1);

        Set<Template> logPredProjTmpl = logGen.generate(trainQueries, TemplateRoot::noPredicateProjectionTemplate);
        float logPredProjCoverage = this.calculateCoveragePercent(logPredProjTmpl, testQueries, TemplateRoot::noPredicateProjectionTemplate, false);

        Set<Template> logPredTmpl = logGen.generate(trainQueries, TemplateRoot::noPredicateTemplate);
        float logPredCoverage = this.calculateCoveragePercent(logPredTmpl, testQueries, TemplateRoot::noPredicateTemplate, false);

        Set<Template> logAttrConstTmpl = logGen.generate(trainQueries, TemplateRoot::noAttributeConstantTemplate);
        float logAttrConstCoverage = this.calculateCoveragePercent(logAttrConstTmpl, testQueries, TemplateRoot::noAttributeConstantTemplate, false);

        Set<Template> logCompProjTmpl = logGen.generate(trainQueries, TemplateRoot::noComparisonProjectionTemplate);
        float logCompProjCoverage = this.calculateCoveragePercent(logCompProjTmpl, testQueries, TemplateRoot::noComparisonProjectionTemplate, false);

        Set<Template> logCompTmpl = logGen.generate(trainQueries, TemplateRoot::noComparisonTemplate);
        float logCompCoverage = this.calculateCoveragePercent(logCompTmpl, testQueries, TemplateRoot::noComparisonTemplate, false);

        Set<Template> logConstProjTmpl = logGen.generate(trainQueries, TemplateRoot::noConstantProjectionTemplate);
        float logConstProjCoverage = this.calculateCoveragePercent(logConstProjTmpl, testQueries, TemplateRoot::noConstantProjectionTemplate, false);

        Set<Template> logConstTmpl = logGen.generate(trainQueries, TemplateRoot::noConstantTemplate);
        float logConstCoverage = this.calculateCoveragePercent(logConstTmpl, testQueries, TemplateRoot::noConstantTemplate, false);

        Set<Template> logFullTmpl = logGen.generate(trainQueries, TemplateRoot::fullQueryTemplate);
        float logFullCoverage = this.calculateCoveragePercent(logFullTmpl, testQueries, TemplateRoot::fullQueryTemplate, false);

        Log.info("Training Set Size: " + trainQueries.size());
        Log.info("Coverage Test Set Size: " + testQueries.size());
        Log.info("           \tc\tc_p\tcm\tcm_p\ta\tpd\tpd_p\tfull");
        Log.info("Log Coverage %:\t"
                + String.format("%.1f", logConstCoverage) + "%\t"
                + String.format("%.1f", logConstProjCoverage) + "%\t"
                + String.format("%.1f", logCompCoverage) + "%\t"
                + String.format("%.1f", logCompProjCoverage) + "%\t"
                + String.format("%.1f", logAttrConstCoverage) + "%\t"
                + String.format("%.1f", logPredCoverage) + "%\t"
                + String.format("%.1f", logPredProjCoverage) + "%\t"
                + String.format("%.1f", logFullCoverage) + "%\t");
        Log.info("Log Tmpl. Count:\t"
                + logConstTmpl.size() + "\t"
                + logConstProjTmpl.size() + "\t"
                + logCompTmpl.size() + "\t"
                + logCompProjTmpl.size() + "\t"
                + logAttrConstTmpl.size() + "\t"
                + logPredTmpl.size() + "\t"
                + logPredProjTmpl.size() + "\t"
                + logFullTmpl.size() + "\t\n");
    }


    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: CoverageSDSS <query-log-filename>");
            System.err.println("Example: CoverageSDSS data/sdss/final/bestdr7_0.05.parsed");
            System.exit(1);
        }

        String queryLogFilename = args[0];

        SchemaGraph schemaGraph = null;
        try {
            schemaGraph = new SchemaGraph("data/sdss/schema/bestdr7");
            TemplateRoot.relations = schemaGraph.relations;
        } catch (Exception e) {
            e.printStackTrace();
        }

        LogTemplateGenerator logGen = new LogTemplateGenerator();

        CoverageSDSS cov = new CoverageSDSS("templates.out", "errors.out");
        Log.info("Parsing statements...");
        List<Select> stmts = Utils.parseStatements(queryLogFilename);
        Log.info("Done parsing statements.");

        cov.performFixedTestSet(logGen, stmts);
        cov.finish();
    }
}
