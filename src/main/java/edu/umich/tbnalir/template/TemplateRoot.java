package edu.umich.tbnalir.template;

import edu.umich.tbnalir.sql.*;
import edu.umich.tbnalir.util.Constants;
import net.sf.jsqlparser.statement.select.*;

import java.util.*;
import java.util.function.Function;

/**
 * Created by cjbaik on 8/22/17.
 */
public class TemplateRoot {
    private Select select; // JSQL object associated with this template

    public TemplateRoot(Select select) {
        this.select = select;
    }

    public Select getSelect() {
        return select;
    }

    /*
     * FUNCTIONS FOR CREATING TEMPLATES FROM A "Select"
     */

    public static String noConstantTemplate(Select select) {
        StringBuilder buffer = new StringBuilder();
        ConstantRemovalExprDeParser constantRemover = new ConstantRemovalExprDeParser();
        SelectConstantRemovalDeParser deParser = new SelectConstantRemovalDeParser(constantRemover, buffer, false);
        constantRemover.setSelectVisitor(deParser);
        constantRemover.setBuffer(buffer);
        select.getSelectBody().accept(deParser);
        return buffer.toString().toLowerCase().intern();
    }

    public static String noConstantProjectionTemplate(Select select) {
        StringBuilder buffer = new StringBuilder();
        ConstantRemovalExprDeParser constantRemover = new ConstantRemovalExprDeParser();
        ProjectionRemovalDeParser deParser = new ProjectionRemovalDeParser(constantRemover, buffer, false);
        constantRemover.setSelectVisitor(deParser);
        constantRemover.setBuffer(buffer);
        select.getSelectBody().accept(deParser);
        return buffer.toString().toLowerCase().intern();
    }

    public static String noComparisonTemplate(Select select) {
        StringBuilder buffer = new StringBuilder();
        ComparisonRemovalExprDeParser comparisonRemover = new ComparisonRemovalExprDeParser();
        SelectConstantRemovalDeParser deParser = new SelectConstantRemovalDeParser(comparisonRemover, buffer, false);
        comparisonRemover.setSelectVisitor(deParser);
        comparisonRemover.setBuffer(buffer);
        select.getSelectBody().accept(deParser);
        return buffer.toString().toLowerCase().intern();
    }

    public static String noComparisonProjectionTemplate(Select select) {
        StringBuilder buffer = new StringBuilder();
        ComparisonRemovalExprDeParser comparisonRemover = new ComparisonRemovalExprDeParser();
        ProjectionRemovalDeParser deParser = new ProjectionRemovalDeParser(comparisonRemover, buffer, false);
        comparisonRemover.setSelectVisitor(deParser);
        comparisonRemover.setBuffer(buffer);
        select.getSelectBody().accept(deParser);
        return buffer.toString().toLowerCase().intern();
    }

    public static String noPredicateTemplate(Select select) {
        StringBuilder buffer = new StringBuilder();
        ConstantRemovalExprDeParser predicateRemover = new ConstantRemovalExprDeParser();
        SelectConstantRemovalDeParser deParser = new SelectConstantRemovalDeParser(predicateRemover, buffer, true);
        predicateRemover.setSelectVisitor(deParser);
        predicateRemover.setBuffer(buffer);
        select.getSelectBody().accept(deParser);
        return buffer.toString().toLowerCase().intern();
    }

    public static String noPredicateProjectionTemplate(Select select) {
        StringBuilder buffer = new StringBuilder();
        ConstantRemovalExprDeParser predicateRemover = new ConstantRemovalExprDeParser();
        ProjectionRemovalDeParser deParser = new ProjectionRemovalDeParser(predicateRemover, buffer, true);
        predicateRemover.setSelectVisitor(deParser);
        predicateRemover.setBuffer(buffer);
        select.getSelectBody().accept(deParser);
        return buffer.toString().toLowerCase().intern();
    }

    /*
    public static Set<String> generateTemplates(List<Statement> stmts, Function<Statement, String> templateFn,
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

    public static List<String> generateTestTemplates(List<Statement> stmts, Function<Statement, String> templateFn) {
        List<String> templates = new ArrayList<>();
        for (Statement stmt : stmts) {
            templates.add(templateFn.apply(stmt));
        }
        return templates;
    }

    public static int testCoverage(Set<String> templates, List<String> test) {
        int covered = 0;
        for (String t : test) {
            if (templates.contains(t)) {
                covered++;
            }
        }
        return covered;
    }*/

    public Set<String> generateTemplates(Function<Select, String> templateFn) {
        Set<String> templates = new HashSet<>();

        PlainSelect ps = (PlainSelect) this.select.getSelectBody();

        // Bitmap used to generate each variant, 0 indicates right-most bit.
        // -- commented out currently: bit X: distinct --
        // bit 0: order
        // bit 1: where
        // bit 2: top

        int numVariants = 3;
        double iterLimit = Math.pow(2, numVariants);
        for (int i = 0; i < iterLimit; i++) {
            /*
            int distinctBit = i & 1;
            if (distinctBit == 1) {
                Distinct distinct = new Distinct();
                ps.setDistinct(distinct);
            }*/

            int orderBit = i & 1;
            if (orderBit == 1) {
                OrderByElement order = new OrderByElement();
                order.setExpression(new LiteralExpression(Constants.ORDER));
                List<OrderByElement> orderList = new ArrayList<>();
                orderList.add(order);
                ps.setOrderByElements(orderList);
            }

            int whereBit = (i >> 1) & 1;
            if (whereBit == 1) {
                ps.setWhere(new LiteralExpression(Constants.PRED));
            }

            int topBit = (i >> 2) & 1;
            if (topBit == 1) {
                // Add variant for top for where condition
                Top top = new Top();
                top.setExpression(new LiteralExpression(Constants.TOP));
                ps.setTop(top);
            }

            String template = templateFn.apply(this.select);
            templates.add(template);

            // Reset everything
            // ps.setDistinct(null);
            ps.setOrderByElements(null);
            ps.setWhere(null);
            ps.setTop(null);
        }

        return templates;
    }
}
