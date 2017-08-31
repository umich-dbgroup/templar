package edu.umich.tbnalir;

import com.esotericsoftware.minlog.Log;
import edu.umich.tbnalir.template.Template;
import edu.umich.tbnalir.template.TemplateRoot;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by cjbaik on 6/20/17.
 */
public class LogTemplateGenerator {
    public LogTemplateGenerator() {
    }

    public Set<Template> generate(List<Select> stmts, Function<Select, Template> templateFn) {
        return stmts.stream().map(templateFn::apply).collect(Collectors.toSet());
    }

    public void performCrossValidation(List<Select> stmts, String basename) {
        /*
        // Split into n partitions for cross validation
        int cvSplits = 4;
        int partitionSize = stmts.size() / cvSplits;
        int remainder = stmts.size() % cvSplits;

        // Shuffle partitions so cross-validation is randomized
        Collections.shuffle(stmts);

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
        Log.info("1 <Abs. Constants> / 2 <Abs. Constants/Comparison Ops> / 3 <Abs. Full Predicates>");
        Log.info("a <Don't Abs. Projections> / b <Abs. Projections>");

        for (int i = 0; i < cvPartitions.size(); i++) {
            List<Select> templateGenSet = new ArrayList<>();
            for (int j = 0; j < cvPartitions.size(); j++) {
                if (j != i) {
                    templateGenSet.addAll(cvPartitions.get(j));
                }
            }
            List<Select> coverageTestSet = cvPartitions.get(i);

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
            Set<String> constTmpl = TemplateRoot.generateTemplates(templateGenSet, TemplateRoot::noConstantTemplate, constFileName);
            List<String> constTest = TemplateRoot.generateTestTemplates(coverageTestSet, TemplateRoot::noConstantTemplate);
            float constCoverage = (float) TemplateRoot.testCoverage(constTmpl, constTest) / coverageTestSet.size() * 100;

            Set<String> constProjTmpl = TemplateRoot.generateTemplates(templateGenSet, TemplateRoot::noConstantProjectionTemplate,
                    constProjFileName);
            List<String> constProjTest = TemplateRoot.generateTestTemplates(coverageTestSet, TemplateRoot::noConstantProjectionTemplate);
            float constProjCoverage = (float) TemplateRoot.testCoverage(constProjTmpl, constProjTest) / coverageTestSet.size() * 100;

            Set<String> compTmpl = TemplateRoot.generateTemplates(templateGenSet, TemplateRoot::noComparisonTemplate, compFileName);
            List<String> compTest = TemplateRoot.generateTestTemplates(coverageTestSet, TemplateRoot::noComparisonTemplate);
            float compCoverage = (float) TemplateRoot.testCoverage(compTmpl, compTest) / coverageTestSet.size() * 100;

            Set<String> compProjTmpl = TemplateRoot.generateTemplates(templateGenSet, TemplateRoot::noComparisonProjectionTemplate, compProjFileName);
            List<String> compProjTest = TemplateRoot.generateTestTemplates(coverageTestSet, TemplateRoot::noComparisonProjectionTemplate);
            float compProjCoverage = (float) TemplateRoot.testCoverage(compProjTmpl, compProjTest) / coverageTestSet.size() * 100;

            Set<String> predTmpl = TemplateRoot.generateTemplates(templateGenSet, TemplateRoot::noPredicateTemplate, predFileName);
            List<String> predTest = TemplateRoot.generateTestTemplates(coverageTestSet, TemplateRoot::noPredicateTemplate);
            float predCoverage = (float) TemplateRoot.testCoverage(predTmpl, predTest) / coverageTestSet.size() * 100;

            Set<String> predProjTmpl = TemplateRoot.generateTemplates(templateGenSet, TemplateRoot::noPredicateProjectionTemplate, predProjFileName);
            List<String> predProjTest = TemplateRoot.generateTestTemplates(coverageTestSet, TemplateRoot::noPredicateProjectionTemplate);
            float predProjCoverage = (float) TemplateRoot.testCoverage(predProjTmpl, predProjTest) / coverageTestSet.size() * 100;

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
        }*/
    }

    public void generateAndSave(List<Select> stmts, String outBasename) {
        /*
        for (String level : this.levels) {
            String outfileName = outBasename + "_" + level + ".csv";
            switch (level) {
                case "const":
                    TemplateRoot.generateTemplates(stmts, TemplateRoot::noConstantTemplate, outfileName);
                    break;
                case "const_proj":
                    TemplateRoot.generateTemplates(stmts, TemplateRoot::noConstantProjectionTemplate, outfileName);
                    break;
                case "comp":
                    TemplateRoot.generateTemplates(stmts, TemplateRoot::noComparisonTemplate, outfileName);
                    break;
                case "comp_proj":
                    TemplateRoot.generateTemplates(stmts, TemplateRoot::noComparisonProjectionTemplate, outfileName);
                    break;
                case "pred":
                    TemplateRoot.generateTemplates(stmts, TemplateRoot::noPredicateTemplate, outfileName);
                    break;
                case "pred_proj":
                    TemplateRoot.generateTemplates(stmts, TemplateRoot::noPredicateProjectionTemplate, outfileName);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid level specified: <" + level + ">.");
            }
        }*/
    }

        /*
    public Set<String> generate(List<Select> stmts) {
        Set<String> templates = null;
        for (String level : this.levels) {
            switch (level) {
                case "const":
                    templates = TemplateRoot.generateTemplates(stmts, TemplateRoot::noConstantTemplate, null);
                    break;
                case "const_proj":
                    templates = TemplateRoot.generateTemplates(stmts, TemplateRoot::noConstantProjectionTemplate, null);
                    break;
                case "comp":
                    templates = TemplateRoot.generateTemplates(stmts, TemplateRoot::noComparisonTemplate, null);
                    break;
                case "comp_proj":
                    templates = TemplateRoot.generateTemplates(stmts, TemplateRoot::noComparisonProjectionTemplate, null);
                    break;
                case "pred":
                    templates = TemplateRoot.generateTemplates(stmts, TemplateRoot::noPredicateTemplate, null);
                    break;
                case "pred_proj":
                    templates = TemplateRoot.generateTemplates(stmts, TemplateRoot::noPredicateProjectionTemplate, null);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid level specified: <" + level + ">.");
            }
        }
        return templates;
    }*/

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

        LogTemplateGenerator tg = new LogTemplateGenerator();

        /*
        List<String> sqls = tg.readQueryLogParsed(filename);
        List<Select> stmts = tg.parseStatements(sqls);

        // For testing, perform cross-validation:
        tg.performCrossValidation(stmts, FilenameUtils.getBaseName(filename));

        // Generate
        /*
        String basename = FilenameUtils.getBaseName(filename);
        tg.generateAndSave(stmts, basename);*/
    }
}
