package edu.umich.templar;

import com.esotericsoftware.minlog.Log;
import edu.umich.templar.rdbms.RDBMS;
import edu.umich.templar.template.JoinPathGenerator;
import edu.umich.templar.template.Template;
import edu.umich.templar.template.TemplateRoot;
import edu.umich.templar.util.CoverageHelper;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.statement.select.Select;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by cjbaik on 9/8/17.
 */
public class CoverageTrainTest extends CoverageHelper {
    public CoverageTrainTest(String outFileName, String errFileName) {
        super(outFileName, errFileName);
    }

    public void performTrainTestCoverage(JoinPathGenerator schemaDataGen,
                                    LogTemplateGenerator logGen,
                                    List<Select> trainSet, List<Select> testSet) {
        // Generate templates from SchemaDataTemplateGenerator
        Set<Template> schemaDataTemplates = schemaDataGen.generate();
        List<Template> schemaDataNoPredProj = new ArrayList<>();
        List<Template> schemaDataNoPred = new ArrayList<>();
        List<Template> schemaDataNoAttrConst = new ArrayList<>();
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
                case NO_ATTR_CONST:
                    schemaDataNoAttrConst.add(t);
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

        Log.info("===== Legend =====");
        Log.info("c <Abs. Constants> / cm <Abs. Constants/Comparison Ops> / a <Abs. Attributes/Constants> / pd <Abs. Full Predicates> / p <Abs. Projections>");

        Set<Template> logPredProjTmpl = logGen.generate(trainSet, TemplateRoot::noPredicateProjectionTemplate);
        float logPredProjCoverage = this.calculateCoveragePercent(logPredProjTmpl, testSet, TemplateRoot::noPredicateProjectionTemplate, false);
        float schemaDataPredProjCoverage = this.calculateCoveragePercent(schemaDataNoPredProj, testSet, TemplateRoot::noPredicateProjectionTemplate, false);
        Set<Template> bothPredProjTmpl = new HashSet<>(logPredProjTmpl);
        bothPredProjTmpl.addAll(schemaDataNoPredProj);
        float bothPredProjCoverage = this.calculateCoveragePercent(bothPredProjTmpl, testSet, TemplateRoot::noPredicateProjectionTemplate, false);

        Set<Template> logPredTmpl = logGen.generate(trainSet, TemplateRoot::noPredicateTemplate);
        float logPredCoverage = this.calculateCoveragePercent(logPredTmpl, testSet, TemplateRoot::noPredicateTemplate, false);
        float schemaDataPredCoverage = this.calculateCoveragePercent(schemaDataNoPred, testSet, TemplateRoot::noPredicateTemplate, false);
        Set<Template> bothPredTmpl = new HashSet<>(logPredTmpl);
        bothPredTmpl.addAll(schemaDataNoPred);
        float bothPredCoverage = this.calculateCoveragePercent(bothPredTmpl, testSet, TemplateRoot::noPredicateTemplate, false);

        Set<Template> logAttrConstTmpl = logGen.generate(trainSet, TemplateRoot::noAttributeConstantTemplate);
        float logAttrConstCoverage = this.calculateCoveragePercent(logAttrConstTmpl, testSet, TemplateRoot::noAttributeConstantTemplate, false);
        float schemaDataAttrConstCoverage = this.calculateCoveragePercent(schemaDataNoAttrConst, testSet, TemplateRoot::noAttributeConstantTemplate, true);
        Set<Template> bothAttrConstTmpl = new HashSet<>(logAttrConstTmpl);
        bothAttrConstTmpl.addAll(schemaDataNoAttrConst);
        float bothAttrConstCoverage = this.calculateCoveragePercent(bothAttrConstTmpl, testSet, TemplateRoot::noAttributeConstantTemplate, false);

        Set<Template> logCompProjTmpl = logGen.generate(trainSet, TemplateRoot::noComparisonProjectionTemplate);
        float logCompProjCoverage = this.calculateCoveragePercent(logCompProjTmpl, testSet, TemplateRoot::noComparisonProjectionTemplate, false);
        float schemaDataCompProjCoverage = this.calculateCoveragePercent(schemaDataNoCompProj, testSet, TemplateRoot::noComparisonProjectionTemplate, false);
        Set<Template> bothCompProjTmpl = new HashSet<>(logCompProjTmpl);
        bothCompProjTmpl.addAll(schemaDataNoCompProj);
        float bothCompProjCoverage = this.calculateCoveragePercent(bothCompProjTmpl, testSet, TemplateRoot::noComparisonProjectionTemplate, false);

        Set<Template> logCompTmpl = logGen.generate(trainSet, TemplateRoot::noComparisonTemplate);
        float logCompCoverage = this.calculateCoveragePercent(logCompTmpl, testSet, TemplateRoot::noComparisonTemplate, false);
        float schemaDataCompCoverage = this.calculateCoveragePercent(schemaDataNoComp, testSet, TemplateRoot::noComparisonTemplate, false);
        Set<Template> bothCompTmpl = new HashSet<>(logCompTmpl);
        bothCompTmpl.addAll(schemaDataNoComp);
        float bothCompCoverage = this.calculateCoveragePercent(bothCompTmpl, testSet, TemplateRoot::noComparisonTemplate, false);

        Set<Template> logConstProjTmpl = logGen.generate(trainSet, TemplateRoot::noConstantProjectionTemplate);
        float logConstProjCoverage = this.calculateCoveragePercent(logConstProjTmpl, testSet, TemplateRoot::noConstantProjectionTemplate, false);
        float schemaDataConstProjCoverage = this.calculateCoveragePercent(schemaDataNoConstProj, testSet, TemplateRoot::noConstantProjectionTemplate, false);
        Set<Template> bothConstProjTmpl = new HashSet<>(logConstProjTmpl);
        bothConstProjTmpl.addAll(schemaDataNoConstProj);
        float bothConstProjCoverage = this.calculateCoveragePercent(bothConstProjTmpl, testSet, TemplateRoot::noConstantProjectionTemplate, false);

        Set<Template> logConstTmpl = logGen.generate(trainSet, TemplateRoot::noConstantTemplate);
        float logConstCoverage = this.calculateCoveragePercent(logConstTmpl, testSet, TemplateRoot::noConstantTemplate, false);
        float schemaDataConstCoverage = this.calculateCoveragePercent(schemaDataNoConst, testSet, TemplateRoot::noConstantTemplate, false);
        Set<Template> bothConstTmpl = new HashSet<>(logConstTmpl);
        bothConstTmpl.addAll(schemaDataNoConst);
        float bothConstCoverage = this.calculateCoveragePercent(bothConstTmpl, testSet, TemplateRoot::noConstantTemplate, false);

        Set<Template> logFullTmpl = logGen.generate(trainSet, TemplateRoot::fullQueryTemplate);
        float logFullCoverage = this.calculateCoveragePercent(logFullTmpl, testSet, TemplateRoot::fullQueryTemplate, false);
        float schemaDataFullCoverage = this.calculateCoveragePercent(schemaDataFull, testSet, TemplateRoot::fullQueryTemplate, false);
        Set<Template> bothFullTmpl = new HashSet<>(logFullTmpl);
        bothFullTmpl.addAll(schemaDataFull);
        float bothFullCoverage = this.calculateCoveragePercent(bothFullTmpl, testSet, TemplateRoot::fullQueryTemplate, false);

        Log.info("Template Gen. Set Size: " + trainSet.size());
        Log.info("Coverage Test Set Size: " + testSet.size());
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
        Log.info("Schema/Data Coverage %:\t"
                + String.format("%.1f", schemaDataConstCoverage) + "%\t"
                + String.format("%.1f", schemaDataConstProjCoverage) + "%\t"
                + String.format("%.1f", schemaDataCompCoverage) + "%\t"
                + String.format("%.1f", schemaDataCompProjCoverage) + "%\t"
                + String.format("%.1f", schemaDataAttrConstCoverage) + "%\t"
                + String.format("%.1f", schemaDataPredCoverage) + "%\t"
                + String.format("%.1f", schemaDataPredProjCoverage) + "%\t"
                + String.format("%.1f", schemaDataFullCoverage) + "%\t");
        Log.info("Schema/Data Tmpl. Count:\t"
                + schemaDataNoConst.size() + "\t"
                + schemaDataNoConstProj.size() + "\t"
                + schemaDataNoComp.size() + "\t"
                + schemaDataNoCompProj.size() + "\t"
                + schemaDataNoAttrConst.size() + "\t"
                + schemaDataNoPred.size() + "\t"
                + schemaDataNoPredProj.size() + "\t"
                + schemaDataFull.size() + "\t\n");
        Log.info("Combined Coverage %:\t"
                + String.format("%.1f", bothConstCoverage) + "%\t"
                + String.format("%.1f", bothConstProjCoverage) + "%\t"
                + String.format("%.1f", bothCompCoverage) + "%\t"
                + String.format("%.1f", bothCompProjCoverage) + "%\t"
                + String.format("%.1f", bothAttrConstCoverage) + "%\t"
                + String.format("%.1f", bothPredCoverage) + "%\t"
                + String.format("%.1f", bothPredProjCoverage) + "%\t"
                + String.format("%.1f", bothFullCoverage) + "%\t");
        Log.info("Combined Tmpl. Count:\t"
                + bothConstTmpl.size() + "\t"
                + bothConstProjTmpl.size() + "\t"
                + bothCompTmpl.size() + "\t"
                + bothCompProjTmpl.size() + "\t"
                + bothAttrConstTmpl.size() + "\t"
                + bothPredTmpl.size() + "\t"
                + bothPredProjTmpl.size() + "\t"
                + bothFullTmpl.size() + "\t\n");
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            System.err.println("Usage: CoverageTrainTest <db-name> <schema-filename-prefix> <max-join-level> <train-queries> <test-queries>");
            System.err.println("Example: CoverageTrainTest senlidb data/senlidb/senlidb 1 data/senlidb/train_queries.txt data/senlidb/test_queries.txt");
            System.exit(1);
        }

        String dbName = args[0];
        String prefix = args[1];

        Integer joinLevel = Integer.valueOf(args[2]);
        String trainFilename = args[3];
        String testFilename = args[4];

        RDBMS db = null;
        try {
            db = new RDBMS(dbName, prefix);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JoinPathGenerator schemaDataGen = new JoinPathGenerator(db, joinLevel);
        LogTemplateGenerator logGen = new LogTemplateGenerator();

        CoverageTrainTest trainTest = new CoverageTrainTest("templates.out", "errors.out");
        List<Select> train = Utils.parseStatements(trainFilename);
        List<Select> test = Utils.parseStatements(testFilename);
        trainTest.performTrainTestCoverage(schemaDataGen, logGen, train, test);
        trainTest.finish();
    }
}
