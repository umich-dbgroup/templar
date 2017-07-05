package edu.umich.tbnalir;

import com.esotericsoftware.minlog.Log;
import edu.umich.tbnalir.sql.*;
import edu.umich.tbnalir.util.Constants;
import edu.umich.tbnalir.util.Utils;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;

import java.io.PrintWriter;
import java.util.*;
import java.util.function.Function;

/**
 * Created by cjbaik on 6/30/17.
 */
public class TemplateGenerator {
    public String noConstantTemplate(Statement stmt) {
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            StringBuilder buffer = new StringBuilder();
            ConstantRemovalExprDeParser constantRemover = new ConstantRemovalExprDeParser();
            SelectConstantRemovalDeParser deParser = new SelectConstantRemovalDeParser(constantRemover, buffer, false);
            constantRemover.setSelectVisitor(deParser);
            constantRemover.setBuffer(buffer);
            select.getSelectBody().accept(deParser);
            return buffer.toString().toLowerCase().intern();
        }
        return null;
    }

    public String noConstantProjectionTemplate(Statement stmt) {
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            StringBuilder buffer = new StringBuilder();
            ConstantRemovalExprDeParser constantRemover = new ConstantRemovalExprDeParser();
            ProjectionRemovalDeParser deParser = new ProjectionRemovalDeParser(constantRemover, buffer, false);
            constantRemover.setSelectVisitor(deParser);
            constantRemover.setBuffer(buffer);
            select.getSelectBody().accept(deParser);
            return buffer.toString().toLowerCase().intern();
        }
        return null;
    }

    public String noComparisonTemplate(Statement stmt) {
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            StringBuilder buffer = new StringBuilder();
            ComparisonRemovalExprDeParser comparisonRemover = new ComparisonRemovalExprDeParser();
            SelectConstantRemovalDeParser deParser = new SelectConstantRemovalDeParser(comparisonRemover, buffer, false);
            comparisonRemover.setSelectVisitor(deParser);
            comparisonRemover.setBuffer(buffer);
            select.getSelectBody().accept(deParser);
            return buffer.toString().toLowerCase().intern();
        }
        return null;
    }

    public String noComparisonProjectionTemplate(Statement stmt) {
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            StringBuilder buffer = new StringBuilder();
            ComparisonRemovalExprDeParser comparisonRemover = new ComparisonRemovalExprDeParser();
            ProjectionRemovalDeParser deParser = new ProjectionRemovalDeParser(comparisonRemover, buffer, false);
            comparisonRemover.setSelectVisitor(deParser);
            comparisonRemover.setBuffer(buffer);
            select.getSelectBody().accept(deParser);
            return buffer.toString().toLowerCase().intern();
        }
        return null;
    }

    public String noPredicateTemplate(Statement stmt) {
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            StringBuilder buffer = new StringBuilder();
            ConstantRemovalExprDeParser predicateRemover = new ConstantRemovalExprDeParser();
            SelectConstantRemovalDeParser deParser = new SelectConstantRemovalDeParser(predicateRemover, buffer, true);
            predicateRemover.setSelectVisitor(deParser);
            predicateRemover.setBuffer(buffer);
            select.getSelectBody().accept(deParser);
            return buffer.toString().toLowerCase().intern();
        }
        return null;
    }

    public String noPredicateProjectionTemplate(Statement stmt) {
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            StringBuilder buffer = new StringBuilder();
            ConstantRemovalExprDeParser predicateRemover = new ConstantRemovalExprDeParser();
            ProjectionRemovalDeParser deParser = new ProjectionRemovalDeParser(predicateRemover, buffer, true);
            predicateRemover.setSelectVisitor(deParser);
            predicateRemover.setBuffer(buffer);
            select.getSelectBody().accept(deParser);
            return buffer.toString().toLowerCase().intern();
        }
        return null;
    }

    protected Set<String> generateTemplates(List<Statement> stmts, Function<Statement, String> templateFn,
                                         String outFileName) {
        Set<String> templates = new HashSet<>();
        Map<String, Integer> templateCounts = new HashMap<>();
        for (Statement stmt : stmts) {
            String tmpl = templateFn.apply(stmt);
            if (tmpl != null) {
                templates.add(tmpl);

                if (outFileName != null) {
                    Integer count = templateCounts.get(tmpl);
                    if (count == null) {
                        templateCounts.put(tmpl, 1);
                    } else {
                        templateCounts.put(tmpl, count + 1);
                    }
                }
            }
        }

        if (outFileName != null) {
            Log.info("Saving " + outFileName + "...");
            try {
                PrintWriter writer = new PrintWriter(outFileName, "UTF-8");
                Map<String, Integer> sortedPredProjCounts = Utils.sortByValueDesc(templateCounts);
                for (Map.Entry<String, Integer> e : sortedPredProjCounts.entrySet()) {
                    writer.println(e.getValue() + "\t" + e.getKey());
                }
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return templates;
    }

    public List<String> generateTestTemplates(List<Statement> stmts, Function<Statement, String> templateFn) {
        List<String> templates = new ArrayList<>();
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

    public List<String> generateTemplateVariants(Function<Statement, String> templateFn, Select select) {
        List<String> templates = new ArrayList<>();

        PlainSelect ps = (PlainSelect) select.getSelectBody();

        // Bitmap used to generate each variant, 0 indicates right-most bit.
        // bit 0: distinct
        // bit 1: order
        // bit 2: where
        // bit 3: top

        int numVariants = 4;
        double iterLimit = Math.pow(2, numVariants);
        for (int i = 0; i < iterLimit; i++) {

            int distinctBit = i & 1;
            if (distinctBit == 1) {
                Distinct distinct = new Distinct();
                ps.setDistinct(distinct);
            }

            int orderBit = (i >> 1) & 1;
            if (orderBit == 1) {
                OrderByElement order = new OrderByElement();
                order.setExpression(new LiteralExpression(Constants.ORDER));
                List<OrderByElement> orderList = new ArrayList<>();
                ps.setOrderByElements(orderList);
            }

            int whereBit = (i >> 2) & 1;
            if (whereBit == 1) {
                ps.setWhere(new LiteralExpression(Constants.PRED));

            }

            int topBit = (i >> 3) & 1;
            if (topBit == 1) {
                // Add variant for top for where condition
                Top top = new Top();
                top.setExpression(new LiteralExpression(Constants.TOP));
                ps.setTop(top);
            }

            String template = templateFn.apply(select);
            templates.add(template);

            // Reset everything
            ps.setDistinct(null);
            ps.setOrderByElements(null);
            ps.setWhere(null);
            ps.setTop(null);
        }

        return templates;
    }
}
