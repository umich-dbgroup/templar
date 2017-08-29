package edu.umich.tbnalir.template;

import edu.umich.tbnalir.rdbms.Relation;
import edu.umich.tbnalir.sql.*;
import edu.umich.tbnalir.util.Constants;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

import java.util.*;
import java.util.function.Function;

/**
 * Created by cjbaik on 8/22/17.
 */
public class TemplateRoot {
    public static Map<String, Relation> relations;

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

    public static Template noConstantTemplate(Select select) {
        StringBuilder buffer = new StringBuilder();
        ConstantRemovalExprDeParser constantRemover = new ConstantRemovalExprDeParser();
        FullQueryDeParser deParser = new FullQueryDeParser(constantRemover, buffer, relations, false);
        constantRemover.setSelectVisitor(deParser);
        constantRemover.setBuffer(buffer);
        select.getSelectBody().accept(deParser);
        return new Template(buffer.toString().toLowerCase().intern(), TemplateType.NO_CONST);
    }

    public static Template noConstantProjectionTemplate(Select select) {
        StringBuilder buffer = new StringBuilder();
        ConstantRemovalExprDeParser constantRemover = new ConstantRemovalExprDeParser();
        ProjectionRemovalDeParser deParser = new ProjectionRemovalDeParser(constantRemover, buffer, relations, false);
        constantRemover.setSelectVisitor(deParser);
        constantRemover.setBuffer(buffer);
        select.getSelectBody().accept(deParser);
        return new Template(buffer.toString().toLowerCase().intern(), TemplateType.NO_CONST_PROJ);
    }

    public static Template noComparisonTemplate(Select select) {
        StringBuilder buffer = new StringBuilder();
        ComparisonRemovalExprDeParser comparisonRemover = new ComparisonRemovalExprDeParser();
        FullQueryDeParser deParser = new FullQueryDeParser(comparisonRemover, buffer, relations, false);
        comparisonRemover.setSelectVisitor(deParser);
        comparisonRemover.setBuffer(buffer);
        select.getSelectBody().accept(deParser);
        return new Template(buffer.toString().toLowerCase().intern(), TemplateType.NO_CONST_OP);
    }

    public static Template noComparisonProjectionTemplate(Select select) {
        StringBuilder buffer = new StringBuilder();
        ComparisonRemovalExprDeParser comparisonRemover = new ComparisonRemovalExprDeParser();
        ProjectionRemovalDeParser deParser = new ProjectionRemovalDeParser(comparisonRemover, buffer, relations, false);
        comparisonRemover.setSelectVisitor(deParser);
        comparisonRemover.setBuffer(buffer);
        select.getSelectBody().accept(deParser);
        return new Template(buffer.toString().toLowerCase().intern(), TemplateType.NO_CONST_OP_PROJ);
    }

    public static Template noPredicateTemplate(Select select) {
        StringBuilder buffer = new StringBuilder();
        ConstantRemovalExprDeParser predicateRemover = new ConstantRemovalExprDeParser();
        FullQueryDeParser deParser = new FullQueryDeParser(predicateRemover, buffer, relations, true);
        predicateRemover.setSelectVisitor(deParser);
        predicateRemover.setBuffer(buffer);
        select.getSelectBody().accept(deParser);
        return new Template(buffer.toString().toLowerCase().intern(), TemplateType.NO_PRED);
    }

    public static Template noPredicateProjectionTemplate(Select select) {
        StringBuilder buffer = new StringBuilder();
        ConstantRemovalExprDeParser predicateRemover = new ConstantRemovalExprDeParser();
        ProjectionRemovalDeParser deParser = new ProjectionRemovalDeParser(predicateRemover, buffer, relations, true);
        predicateRemover.setSelectVisitor(deParser);
        predicateRemover.setBuffer(buffer);
        select.getSelectBody().accept(deParser);
        return new Template(buffer.toString().toLowerCase().intern(), TemplateType.NO_PRED_PROJ);
    }

    public static Template fullQueryTemplate(Select select) {
        StringBuilder buffer = new StringBuilder();
        FullQueryExprDeParser exprDeParser = new FullQueryExprDeParser();
        FullQueryDeParser deParser = new FullQueryDeParser(exprDeParser, buffer, relations, false);
        exprDeParser.setSelectVisitor(deParser);
        exprDeParser.setBuffer(buffer);
        select.getSelectBody().accept(deParser);
        return new Template(buffer.toString().toLowerCase().intern(), TemplateType.FULL_QUERY);
    }

    /*
    public static Set<Template> generateTemplates(List<Select> stmts, Function<Select, Template> templateFn,
                                                String outFileName) {
        Set<Template> templates = new HashSet<>();
        Map<Template, Integer> templateCounts = new HashMap<>();
        for (Select stmt : stmts) {
            Template tmpl = templateFn.apply(stmt);
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
                Map<Template, Integer> sortedPredProjCounts = Utils.sortByValueDesc(templateCounts);
                for (Map.Entry<Template, Integer> e : sortedPredProjCounts.entrySet()) {
                    writer.println(e.getValue() + "\t" + e.getKey());
                }
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return templates;
    }*/

    public static List<String> generateTestTemplates(List<Select> stmts, Function<Select, String> templateFn) {
        List<String> templates = new ArrayList<>();
        for (Select stmt : stmts) {
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
    }

    public Set<Template> generateTemplates(Function<Select, Template> templateFn) {
        Set<Template> templates = new HashSet<>();

        PlainSelect ps = (PlainSelect) this.select.getSelectBody();

        // Bitmap used to generate each variant, 0 indicates right-most bit.
        // -- commented out currently: bit X: distinct --
        // bit 0: order
        // bit 1: where
        // -- commented out currently: bit X: top/limit --

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

            /*
            int topBit = (i >> 2) & 1;
            if (topBit == 1) {
                // Add variant for top for where condition
                Top top = new Top();
                top.setExpression(new LiteralExpression(Constants.TOP));
                ps.setTop(top);
            }*/

            Template template = templateFn.apply(this.select);
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
