package edu.umich.tbnalir;

import com.esotericsoftware.minlog.Log;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.apache.commons.io.FilenameUtils;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by cjbaik on 6/20/17.
 */
public class TemplateGenerator {
    public String noConstantTemplate(Statement stmt) {
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            StringBuilder buffer = new StringBuilder();
            ConstantRemovalExprDeParser constantRemover = new ConstantRemovalExprDeParser();
            UDFRemovalDeParser deParser = new UDFRemovalDeParser(constantRemover, buffer);
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

            UDFRemovalDeParser deParser = new UDFRemovalDeParser(comparisonRemover, buffer);
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
            PredicateRemovalExprDeParser comparisonRemover = new PredicateRemovalExprDeParser();
            UDFRemovalDeParser deParser = new UDFRemovalDeParser(comparisonRemover, buffer);
            comparisonRemover.setSelectVisitor(deParser);
            comparisonRemover.setBuffer(buffer);
            select.getSelectBody().accept(deParser);
            return buffer.toString().intern();
        }
        return null;
    }

    public String noPredicateProjectionTemplate(Statement stmt) {
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            StringBuilder buffer = new StringBuilder();
            PredicateRemovalExprDeParser comparisonRemover = new PredicateRemovalExprDeParser();
            ProjectionRemovalDeParser deParser = new ProjectionRemovalDeParser(comparisonRemover, buffer);
            comparisonRemover.setSelectVisitor(deParser);
            comparisonRemover.setBuffer(buffer);
            select.getSelectBody().accept(deParser);
            return buffer.toString().intern();
        }
        return null;
    }

    public static void main (String[] args) {
        if (args.length < 1) {
            Log.error("Missing args, need at least CSV file arg.");
            System.exit(1);
        }


        String filename = args[0];

        TemplateGenerator tg = new TemplateGenerator();

        // Statistics
        int totalSQL = 0;
        int parsedSQL = 0;
        Map<String, Integer> constantCounts = new HashMap<String, Integer>();
        Map<String, Integer> constantProjCounts = new HashMap<String, Integer>();
        Map<String, Integer> comparisonCounts = new HashMap<String, Integer>();
        Map<String, Integer> comparisonProjCounts = new HashMap<String, Integer>();
        Map<String, Integer> predicateCounts = new HashMap<String, Integer>();
        Map<String, Integer> predicateProjCounts = new HashMap<String, Integer>();

        int lastUpdate = 0;

        try {
            CSVReader csvr = new CSVReader(new FileReader(filename));
            String [] nextLine;
            while ((nextLine = csvr.readNext()) != null) {
                if (nextLine.length < 4) continue;

                String sql = nextLine[3];
                totalSQL++;
                if (totalSQL > (lastUpdate + 100000)) {
                    System.out.println("\rProcessed " + totalSQL + " statements...");
                    lastUpdate = totalSQL;
                }

                // Clean up strange syntax in some queries which caused crashing error
                sql = sql.replace("#", "_");
                sql = sql.replace("\u0018", "_");

                Log.debug("ORIGINAL: " + sql.replace("\n", " "));
                Statement stmt;
                try {
                    stmt = CCJSqlParserUtil.parse(sql);
                } catch (Throwable t) {
                    if (Log.DEBUG) t.printStackTrace();
                    continue;
                }

                String cnstTmpl = tg.noConstantTemplate(stmt);
                if (cnstTmpl == null) {
                    Log.debug("Only SELECT statements are currently supported.");
                    continue;
                }
                Integer cnstCount = constantCounts.get(cnstTmpl);
                if (cnstCount == null) {
                    constantCounts.put(cnstTmpl, 1);
                } else {
                    constantCounts.put(cnstTmpl, cnstCount + 1);
                }
                Log.debug("NO CONSTANTS: " + cnstTmpl);

                String cnstProjTmpl = tg.noConstantProjectionTemplate(stmt);
                Integer cnstProjCount = constantProjCounts.get(cnstProjTmpl);
                if (cnstProjCount == null) {
                    constantProjCounts.put(cnstProjTmpl, 1);
                } else {
                    constantProjCounts.put(cnstProjTmpl, cnstProjCount + 1);
                }

                Log.debug("NO CONSTANTS/PROJECTIONS: " + cnstTmpl);

                String cmpTmpl = tg.noComparisonTemplate(stmt);
                Integer cmpCount = comparisonCounts.get(cmpTmpl);
                if (cmpCount == null) {
                    comparisonCounts.put(cmpTmpl, 1);
                } else {
                    comparisonCounts.put(cmpTmpl, cmpCount + 1);
                }
                Log.debug("NO COMPARISONS: " + cmpTmpl);

                String cmpProjTmpl = tg.noComparisonProjectionTemplate(stmt);
                Integer cmpProjCount = comparisonProjCounts.get(cmpProjTmpl);
                if (cmpProjCount == null) {
                    comparisonProjCounts.put(cmpProjTmpl, 1);
                } else {
                    comparisonProjCounts.put(cmpProjTmpl, cmpProjCount + 1);
                }
                Log.debug("NO COMPARISONS/PROJECTIONS: " + cmpProjTmpl);

                String predTmpl = tg.noPredicateTemplate(stmt);
                Integer predCount = predicateCounts.get(predTmpl);
                if (predCount == null) {
                    predicateCounts.put(predTmpl, 1);
                } else {
                    predicateCounts.put(predTmpl, predCount + 1);
                }
                Log.debug("NO PREDICATES: " + predTmpl);

                String predProjTmpl = tg.noPredicateProjectionTemplate(stmt);
                Integer predProjCount = predicateProjCounts.get(predProjTmpl);
                if (predProjCount == null) {
                    predicateProjCounts.put(predProjTmpl, 1);
                } else {
                    predicateProjCounts.put(predProjTmpl, predProjCount + 1);
                }
                Log.debug("NO PREDICATES/PROJECTIONS: " + predProjTmpl);

                Log.debug("---");
                parsedSQL++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.info("Total Queries: " + totalSQL);
        Log.info("Correctly Parsed: " + parsedSQL + "/" + totalSQL);
        Log.info("1 / Constant Templates: " + constantCounts.size());
        Log.info("1a / Constant/Projection Templates: " + constantProjCounts.size());
        Log.info("2 / Comparison Templates: " + comparisonCounts.size());
        Log.info("2a / Comparison/Projection Templates: " + comparisonProjCounts.size());
        Log.info("3 / Predicate Templates: " + predicateCounts.size());
        Log.info("3a / Predicate/Projection Templates: " + predicateProjCounts.size());

        // Write to file
        String basename = FilenameUtils.getBaseName(filename);
        try {
            String constFileName = basename + "_const.csv";
            Log.info("Saving " + constFileName + "...");
            PrintWriter writer = new PrintWriter(constFileName, "UTF-8");
            for (String t : constantCounts.keySet()) {
                writer.println(t);
            }
            writer.close();

            String constProjFileName = basename + "_const_proj.csv";
            Log.info("Saving " + constProjFileName + "...");
            PrintWriter constProjWriter = new PrintWriter(constProjFileName, "UTF-8");
            for (String t : constantProjCounts.keySet()) {
                constProjWriter.println(t);
            }
            constProjWriter.close();

            String compFileName = basename + "_comp.csv";
            Log.info("Saving " + compFileName + "...");
            PrintWriter compWriter = new PrintWriter(compFileName, "UTF-8");
            for (String t : comparisonCounts.keySet()) {
                compWriter.println(t);
            }
            compWriter.close();

            String compProjFileName = basename + "_comp_proj.csv";
            Log.info("Saving " + compProjFileName + "...");
            PrintWriter compProjWriter = new PrintWriter(compProjFileName, "UTF-8");
            for (String t : comparisonProjCounts.keySet()) {
                compProjWriter.println(t);
            }
            compProjWriter.close();

            String predFileName = basename + "_pred.csv";
            Log.info("Saving " + predFileName + "...");
            PrintWriter predWriter = new PrintWriter(predFileName, "UTF-8");
            for (String t : predicateCounts.keySet()) {
                predWriter.println(t);
            }
            predWriter.close();

            String predProjFileName = basename + "_pred_proj.csv";
            Log.info("Saving " + predProjFileName + "...");
            PrintWriter predProjWriter = new PrintWriter(predProjFileName, "UTF-8");
            for (String t : predicateProjCounts.keySet()) {
                predProjWriter.println(t);
            }
            predProjWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
