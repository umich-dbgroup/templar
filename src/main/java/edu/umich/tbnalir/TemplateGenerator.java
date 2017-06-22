package edu.umich.tbnalir;

import com.esotericsoftware.minlog.Log;
import com.opencsv.CSVReader;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;

import java.io.FileReader;
import java.util.*;
import java.util.function.Function;

/**
 * Created by cjbaik on 6/20/17.
 */
public class TemplateGenerator {
    public String noConstantTemplate(Statement stmt) {
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            StringBuilder buffer = new StringBuilder();
            ConstantRemovalExprDeParser constantRemover = new ConstantRemovalExprDeParser();
            TableFunctionRemovalDeParser deParser = new TableFunctionRemovalDeParser(constantRemover, buffer);
            constantRemover.setSelectVisitor(deParser);
            constantRemover.setBuffer(buffer);
            select.getSelectBody().accept(deParser);
            return buffer.toString().intern();
        }
        return null;
    }

    public String noConstantProjectionTemplate(Statement stmt) {
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            StringBuilder buffer = new StringBuilder();
            ConstantRemovalExprDeParser constantRemover = new ConstantRemovalExprDeParser();
            ProjectionRemovalDeParser deParser = new ProjectionRemovalDeParser(constantRemover, buffer);
            constantRemover.setSelectVisitor(deParser);
            constantRemover.setBuffer(buffer);
            select.getSelectBody().accept(deParser);
            return buffer.toString().intern();
        }
        return null;
    }

    public String noComparisonTemplate(Statement stmt) {
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            StringBuilder buffer = new StringBuilder();
            ComparisonRemovalExprDeParser comparisonRemover = new ComparisonRemovalExprDeParser();
            TableFunctionRemovalDeParser deParser = new TableFunctionRemovalDeParser(comparisonRemover, buffer);
            comparisonRemover.setSelectVisitor(deParser);
            comparisonRemover.setBuffer(buffer);
            select.getSelectBody().accept(deParser);
            return buffer.toString().intern();
        }
        return null;
    }

    public String noComparisonProjectionTemplate(Statement stmt) {
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            StringBuilder buffer = new StringBuilder();
            ComparisonRemovalExprDeParser comparisonRemover = new ComparisonRemovalExprDeParser();
            ProjectionRemovalDeParser deParser = new ProjectionRemovalDeParser(comparisonRemover, buffer);
            comparisonRemover.setSelectVisitor(deParser);
            comparisonRemover.setBuffer(buffer);
            select.getSelectBody().accept(deParser);
            return buffer.toString().intern();
        }
        return null;
    }

    public String noPredicateTemplate(Statement stmt) {
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            StringBuilder buffer = new StringBuilder();
            PredicateRemovalExprDeParser predicateRemover = new PredicateRemovalExprDeParser();
            TableFunctionRemovalDeParser deParser = new TableFunctionRemovalDeParser(predicateRemover, buffer);
            predicateRemover.setSelectVisitor(deParser);
            predicateRemover.setBuffer(buffer);
            select.getSelectBody().accept(deParser);
            return buffer.toString().intern();
        }
        return null;
    }

    public String noPredicateProjectionTemplate(Statement stmt) {
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            StringBuilder buffer = new StringBuilder();
            PredicateRemovalExprDeParser predicateRemover = new PredicateRemovalExprDeParser();
            ProjectionRemovalDeParser deParser = new ProjectionRemovalDeParser(predicateRemover, buffer);
            predicateRemover.setSelectVisitor(deParser);
            predicateRemover.setBuffer(buffer);
            select.getSelectBody().accept(deParser);
            return buffer.toString().intern();
        }
        return null;
    }

    public Set<String> generateTemplates(List<Statement> stmts, Function<Statement, String> templateFn) {
        Set<String> templates = new HashSet<String>();
        for (Statement stmt : stmts) {
            templates.add(templateFn.apply(stmt));
        }
        return templates;
    }

    public List<String> generateTestTemplates(List<Statement> stmts, Function<Statement, String> templateFn) {
        List<String> templates = new ArrayList<String>();
        for (Statement stmt : stmts) {
            templates.add(templateFn.apply(stmt));
        }
        return templates;
    }

    public int testCoverage(Set<String> templates, List<String> test) {
        int covered = 0;
        for (String t : test) {
            if (templates.contains(t)) {
                covered++;
            }
        }
        return covered;
    }

    public static void main (String[] args) {
        if (args.length < 1) {
            Log.error("Missing args, need at least CSV file arg.");
            System.exit(1);
        }

        String filename = args[0];
        if (args.length > 1 && args[1].equals("true")) {
            Log.DEBUG();
        }

        TemplateGenerator tg = new TemplateGenerator();

        // Statistics
        int totalSQL = 0;
        int parsedSQL = 0;
        int lastUpdate = 0;

        /*
        Map<String, Integer> constantCounts = new HashMap<String, Integer>();
        Map<String, Integer> constantProjCounts = new HashMap<String, Integer>();
        Map<String, Integer> comparisonCounts = new HashMap<String, Integer>();
        Map<String, Integer> comparisonProjCounts = new HashMap<String, Integer>();
        Map<String, Integer> predicateCounts = new HashMap<String, Integer>();
        Map<String, Integer> predicateProjCounts = new HashMap<String, Integer>();
        */

        // Tokens to replace from error
        char[] tokensToReplace = {'#'};

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
                } catch (Throwable t) {
                    if (Log.DEBUG) t.printStackTrace();
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

        // Split into n partitions for cross validation
        int cvSplits = 4;
        int partitionSize = stmts.size() / cvSplits;
        int remainder = stmts.size() % cvSplits;

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
        Log.info("a <Don't Abs. Projections> / b <Abs. Projections>\n");
        Log.info("Fold #\t1a\t1b\t2a\t2b\t3a\t3b\tTotal");

        for (int i = 0; i < cvPartitions.size(); i++) {
            List<Statement> templateGenSet = new ArrayList<Statement>();
            for (int j = 0; j < cvPartitions.size(); j++) {
                if (j != i) {
                    templateGenSet.addAll(cvPartitions.get(j));
                }
            }
            List<Statement> coverageTestSet = cvPartitions.get(i);

            Set<String> constTmpl = tg.generateTemplates(templateGenSet, tg::noConstantTemplate);
            List<String> constTest = tg.generateTestTemplates(coverageTestSet, tg::noConstantTemplate);
            int constCoverage = tg.testCoverage(constTmpl, constTest);

            Set<String> constProjTmpl = tg.generateTemplates(templateGenSet, tg::noConstantProjectionTemplate);
            List<String> constProjTest = tg.generateTestTemplates(coverageTestSet, tg::noConstantProjectionTemplate);
            int constProjCoverage = tg.testCoverage(constProjTmpl, constProjTest);

            Set<String> compTmpl = tg.generateTemplates(templateGenSet, tg::noComparisonTemplate);
            List<String> compTest = tg.generateTestTemplates(coverageTestSet, tg::noComparisonTemplate);
            int compCoverage = tg.testCoverage(compTmpl, compTest);

            Set<String> compProjTmpl = tg.generateTemplates(templateGenSet, tg::noComparisonProjectionTemplate);
            List<String> compProjTest = tg.generateTestTemplates(coverageTestSet, tg::noComparisonProjectionTemplate);
            int compProjCoverage = tg.testCoverage(compProjTmpl, compProjTest);

            Set<String> predTmpl = tg.generateTemplates(templateGenSet, tg::noPredicateTemplate);
            List<String> predTest = tg.generateTestTemplates(coverageTestSet, tg::noPredicateTemplate);
            int predCoverage = tg.testCoverage(predTmpl, predTest);

            Set<String> predProjTmpl = tg.generateTemplates(templateGenSet, tg::noPredicateProjectionTemplate);
            List<String> predProjTest = tg.generateTestTemplates(coverageTestSet, tg::noPredicateProjectionTemplate);
            tg.testCoverage(predProjTmpl, predProjTest);
            int predProjCoverage = tg.testCoverage(predProjTmpl, predProjTest);

            Log.info(i + ":\t\t" + constCoverage + "\t" + constProjCoverage + "\t" + compCoverage + "\t"
                    + compProjCoverage + "\t" + predCoverage + "\t" + predProjCoverage + "\t" + coverageTestSet.size());
        }

        // Write to file
        /*jj
        String basename = FilenameUtils.getBaseName(filename);
        try {
            String constFileName = basename + "_const.csv";
            Log.info("Saving " + constFileName + "...");
            PrintWriter writer = new PrintWriter(constFileName, "UTF-8");
            Map<String, Integer> sortedConstantCounts = Utils.sortByValueDesc(constantCounts);
            for (Map.Entry<String,Integer> e : sortedConstantCounts.entrySet()) {
                writer.println(e.getValue() + "\t" + e.getKey());
            }
            writer.close();

            String constProjFileName = basename + "_const_proj.csv";
            Log.info("Saving " + constProjFileName + "...");
            PrintWriter constProjWriter = new PrintWriter(constProjFileName, "UTF-8");
            Map<String, Integer> sortedConstantProjCounts = Utils.sortByValueDesc(constantProjCounts);
            for (Map.Entry<String,Integer> e : sortedConstantProjCounts.entrySet()) {
                constProjWriter.println(e.getValue() + "\t" + e.getKey());
            }
            constProjWriter.close();

            String compFileName = basename + "_comp.csv";
            Log.info("Saving " + compFileName + "...");
            PrintWriter compWriter = new PrintWriter(compFileName, "UTF-8");
            Map<String, Integer> sortedComparisonCounts = Utils.sortByValueDesc(comparisonCounts);
            for (Map.Entry<String,Integer> e : sortedComparisonCounts.entrySet()) {
                compWriter.println(e.getValue() + "\t" + e.getKey());
            }
            compWriter.close();

            String compProjFileName = basename + "_comp_proj.csv";
            Log.info("Saving " + compProjFileName + "...");
            PrintWriter compProjWriter = new PrintWriter(compProjFileName, "UTF-8");
            Map<String, Integer> sortedCompProjCounts = Utils.sortByValueDesc(comparisonProjCounts);
            for (Map.Entry<String,Integer> e : sortedCompProjCounts.entrySet()) {
                compProjWriter.println(e.getValue() + "\t" + e.getKey());
            }
            compProjWriter.close();

            String predFileName = basename + "_pred.csv";
            Log.info("Saving " + predFileName + "...");
            PrintWriter predWriter = new PrintWriter(predFileName, "UTF-8");
            Map<String, Integer> sortedPredCounts = Utils.sortByValueDesc(predicateCounts);
            for (Map.Entry<String,Integer> e : sortedPredCounts.entrySet()) {
                predWriter.println(e.getValue() + "\t" + e.getKey());
            }
            predWriter.close();

            String predProjFileName = basename + "_pred_proj.csv";
            Log.info("Saving " + predProjFileName + "...");
            PrintWriter predProjWriter = new PrintWriter(predProjFileName, "UTF-8");
            Map<String, Integer> sortedPredProjCounts = Utils.sortByValueDesc(predicateProjCounts);
            for (Map.Entry<String,Integer> e : sortedPredProjCounts.entrySet()) {
                predProjWriter.println(e.getValue() + "\t" + e.getKey());
            }
            predProjWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        */
    }
}
