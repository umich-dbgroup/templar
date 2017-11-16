package edu.umich.templar;

import com.esotericsoftware.minlog.Log;
import edu.umich.templar.rdbms.RDBMS;
import edu.umich.templar.template.SchemaDataTemplateGenerator;
import edu.umich.templar.template.Template;
import edu.umich.templar.template.TemplateRoot;
import edu.umich.templar.util.CoverageHelper;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.statement.select.Select;

import java.util.*;

/**
 * Created by cjbaik on 8/31/17.
 */
public class CoverageSDSS extends CoverageHelper {
    public CoverageSDSS(String outFileName, String errFileName) {
        super(outFileName, errFileName);
    }

    public void performCrossValidation(LogTemplateGenerator logGen, List<Select> stmts, int randomSeed, int cvSplits) {
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
        Log.info("c <Abs. Constants> / cm <Abs. Constants/Comparison Ops> / a <Abs. Attributes/Constants> / pd <Abs. Full Predicates> / p <Abs. Projections>");

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

            Set<Template> logPredTmpl = logGen.generate(templateGenSet, TemplateRoot::noPredicateTemplate);
            float logPredCoverage = this.calculateCoveragePercent(logPredTmpl, coverageTestSet, TemplateRoot::noPredicateTemplate, false);

            Set<Template> logAttrConstTmpl = logGen.generate(templateGenSet, TemplateRoot::noAttributeConstantTemplate);
            float logAttrConstCoverage = this.calculateCoveragePercent(logAttrConstTmpl, coverageTestSet, TemplateRoot::noAttributeConstantTemplate, false);

            Set<Template> logCompProjTmpl = logGen.generate(templateGenSet, TemplateRoot::noComparisonProjectionTemplate);
            float logCompProjCoverage = this.calculateCoveragePercent(logCompProjTmpl, coverageTestSet, TemplateRoot::noComparisonProjectionTemplate, false);

            Set<Template> logCompTmpl = logGen.generate(templateGenSet, TemplateRoot::noComparisonTemplate);
            float logCompCoverage = this.calculateCoveragePercent(logCompTmpl, coverageTestSet, TemplateRoot::noComparisonTemplate, false);

            Set<Template> logConstProjTmpl = logGen.generate(templateGenSet, TemplateRoot::noConstantProjectionTemplate);
            float logConstProjCoverage = this.calculateCoveragePercent(logConstProjTmpl, coverageTestSet, TemplateRoot::noConstantProjectionTemplate, false);

            Set<Template> logConstTmpl = logGen.generate(templateGenSet, TemplateRoot::noConstantTemplate);
            float logConstCoverage = this.calculateCoveragePercent(logConstTmpl, coverageTestSet, TemplateRoot::noConstantTemplate, false);

            Set<Template> logFullTmpl = logGen.generate(templateGenSet, TemplateRoot::fullQueryTemplate);
            float logFullCoverage = this.calculateCoveragePercent(logFullTmpl, coverageTestSet, TemplateRoot::fullQueryTemplate, false);

            Log.info("--- Fold " + i + " ---");
            Log.info("Template Gen. Set Size: " + templateGenSet.size());
            Log.info("Coverage Test Set Size: " + coverageTestSet.size());
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
    }


    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: CoverageSDSS <query-log-filename> <random seed>");
            System.err.println("Example: CoverageSDSS data/sdss/final/bestdr7_0.05.parsed 1234");
            System.exit(1);
        }

        String queryLogFilename = args[0];
        Integer randomSeed = Integer.valueOf(args[1]);

        LogTemplateGenerator logGen = new LogTemplateGenerator();

        int cvSplits = 4;
        CoverageSDSS cv = new CoverageSDSS("templates.out", "errors.out");
        List<Select> stmts = Utils.parseStatements(queryLogFilename);
        cv.performCrossValidation(logGen, stmts, randomSeed, cvSplits);
        cv.finish();
    }
}
