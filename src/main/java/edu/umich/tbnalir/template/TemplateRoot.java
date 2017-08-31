package edu.umich.tbnalir.template;

import edu.umich.tbnalir.rdbms.Attribute;
import edu.umich.tbnalir.rdbms.Relation;
import edu.umich.tbnalir.sql.*;
import edu.umich.tbnalir.util.Constants;
import edu.umich.tbnalir.util.Utils;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

import java.lang.reflect.Constructor;
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

    public Set<Template> generateNoPredicateProjectionTemplates() {
        // By default, have a WHERE clause that's empty
        PlainSelect ps = (PlainSelect) this.getSelect().getSelectBody();
        ps.setWhere(new LiteralExpression(Constants.PRED));

        return this.generateTemplates(TemplateRoot::noPredicateProjectionTemplate, true);
    }

    public Set<Template> generateNoPredicateTemplates(Set<Set<Attribute>> projections) {
        Set<Template> results = new HashSet<>();

        PlainSelect ps = (PlainSelect) this.getSelect().getSelectBody();

        // By default, have a WHERE clause that's empty
        ps.setWhere(new LiteralExpression(Constants.PRED));

        for (Set<Attribute> attrSet : projections) {
            if (attrSet.size() == 0) continue;

            List<SelectItem> selectItems = new ArrayList<>();

            for (Attribute attr : attrSet) {
                SelectExpressionItem item = new SelectExpressionItem();
                item.setExpression(attr.getColumn());
                selectItems.add(item);
            }

            ps.setSelectItems(selectItems);
            results.addAll(this.generateTemplates(TemplateRoot::noPredicateTemplate, false));
        }

        ps.setSelectItems(null); // reset select object
        return results;
    }

    public Set<Template> generateNoComparisonProjectionTemplates(Set<Set<Attribute>> predicateAttributes) {
        Set<Template> results = new HashSet<>();

        PlainSelect ps = (PlainSelect) this.getSelect().getSelectBody();

        Expression oldWhere = ps.getWhere();
        for (Set<Attribute> predAttrSet : predicateAttributes) {
            if (predAttrSet.size() == 0) continue;

            Expression predicate = null;

            for (Attribute predAttr : predAttrSet) {
                // type of binary expression doesn't matter because comparison is obscured
                predicate = this.generateComparisonPredicateExpr(predicate, EqualsTo.class, predAttr);

                ps.setWhere(predicate);

                results.addAll(this.generateTemplates(TemplateRoot::noComparisonProjectionTemplate, false));
            }
        }

        ps.setWhere(oldWhere); // reset WHERE predicate
        return results;
    }

    public Set<Template> generateNoComparisonTemplates(Set<Set<Attribute>> projections,
                                                                 Set<Set<Attribute>> predicateAttributes) {
        Set<Template> results = new HashSet<>();

        PlainSelect ps = (PlainSelect) this.getSelect().getSelectBody();

        for (Set<Attribute> attrSet : projections) {
            if (attrSet.size() == 0) continue;

            List<SelectItem> selectItems = new ArrayList<>();

            for (Attribute attr : attrSet) {
                SelectExpressionItem item = new SelectExpressionItem();
                item.setExpression(attr.getColumn());
                selectItems.add(item);
            }

            ps.setSelectItems(selectItems);

            Expression oldWhere = ps.getWhere();
            for (Set<Attribute> predAttrSet : predicateAttributes) {
                if (predAttrSet.size() == 0) continue;

                Expression predicate = null;

                for (Attribute predAttr : predAttrSet) {
                    predicate = this.generateComparisonPredicateExpr(predicate, EqualsTo.class, predAttr);

                    ps.setWhere(predicate);

                    results.addAll(this.generateTemplates(TemplateRoot::noComparisonTemplate, false));
                }
            }

            ps.setWhere(oldWhere); // reset WHERE predicate
        }

        ps.setSelectItems(null); // reset select clause
        return results;
    }

    public Expression generateComparisonPredicateExpr(Expression startingPred, Class binaryExprClass, Attribute attr) {
        try {
            Constructor<?> ctor = binaryExprClass.getConstructor();
            BinaryExpression binaryExpression = (BinaryExpression) ctor.newInstance();
            binaryExpression.setLeftExpression(attr.getColumn());
            binaryExpression.setRightExpression(new LiteralExpression(Utils.convertSQLTypetoConstant(attr.getType())));

            if (startingPred == null) {
                return binaryExpression;
            } else {
                return new AndExpression(startingPred, binaryExpression);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

    }

    public Set<Template> generateComparisonPredicatesRecursive(Function<Select, Template> templateFn,
                                                               Expression startingPred, List<Attribute> remainingAttr) {
        Set<Template> results = new HashSet<>();

        PlainSelect ps = (PlainSelect) this.getSelect().getSelectBody();
        Expression originalWhere = ps.getWhere();
        ps.setWhere(startingPred);
        results.addAll(this.generateTemplates(templateFn, false));

        ps.setWhere(originalWhere); // reset

        if (remainingAttr.isEmpty()) return results;

        Attribute attr = remainingAttr.remove(0);

        // Every type of attribute should support an equality predicate
        Expression predicate;
        predicate = this.generateComparisonPredicateExpr(startingPred, EqualsTo.class, attr);
        results.addAll(this.generateComparisonPredicatesRecursive(templateFn, predicate, new ArrayList<>(remainingAttr)));

        if (Utils.isSQLTypeNumeric(attr.getType())) {
            // For foreign/primary keys, only support equality predicates
            if (!attr.isFk() && !attr.isPk()) {
                predicate = this.generateComparisonPredicateExpr(startingPred, GreaterThan.class, attr);
                results.addAll(this.generateComparisonPredicatesRecursive(templateFn, predicate, new ArrayList<>(remainingAttr)));

                predicate = this.generateComparisonPredicateExpr(startingPred, GreaterThanEquals.class, attr);
                results.addAll(this.generateComparisonPredicatesRecursive(templateFn, predicate, new ArrayList<>(remainingAttr)));

                predicate = this.generateComparisonPredicateExpr(startingPred, MinorThan.class, attr);
                results.addAll(this.generateComparisonPredicatesRecursive(templateFn, predicate, new ArrayList<>(remainingAttr)));

                predicate = this.generateComparisonPredicateExpr(startingPred, MinorThanEquals.class, attr);
                results.addAll(this.generateComparisonPredicatesRecursive(templateFn, predicate, new ArrayList<>(remainingAttr)));

                // TODO: a more complex predicate could be "between" (> + <, >= + <=)
            }
        }

        return results;
    }

    public Set<Template> generateNoConstantProjectionTemplates(Set<Set<Attribute>> predicateAttributes) {
        Set<Template> results = new HashSet<>();

        PlainSelect ps = (PlainSelect) this.getSelect().getSelectBody();

        Expression oldWhere = ps.getWhere();
        for (Set<Attribute> predAttrSet : predicateAttributes) {
            if (predAttrSet.size() == 0) continue;

            results.addAll(this.generateComparisonPredicatesRecursive(TemplateRoot::noConstantProjectionTemplate,
                    null, new ArrayList<>(predAttrSet)));
        }

        ps.setWhere(oldWhere); // reset WHERE predicate
        return results;
    }

    public Set<Template> generateNoConstantTemplates(Set<Set<Attribute>> projections,
                                                     Set<Set<Attribute>> predicateAttributes) {
        Set<Template> results = new HashSet<>();

        PlainSelect ps = (PlainSelect) this.getSelect().getSelectBody();

        for (Set<Attribute> attrSet : projections) {
            if (attrSet.size() == 0) continue;

            List<SelectItem> selectItems = new ArrayList<>();

            for (Attribute attr : attrSet) {
                SelectExpressionItem item = new SelectExpressionItem();
                item.setExpression(attr.getColumn());
                selectItems.add(item);
            }

            ps.setSelectItems(selectItems);

            Expression oldWhere = ps.getWhere();
            for (Set<Attribute> predAttrSet : predicateAttributes) {
                if (predAttrSet.size() == 0) continue;

                results.addAll(this.generateComparisonPredicatesRecursive(TemplateRoot::noConstantTemplate,
                        null, new ArrayList<>(predAttrSet)));
            }

            ps.setWhere(oldWhere); // reset WHERE predicate
        }

        ps.setSelectItems(null); // reset select clause
        return results;
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

    public Set<Template> generateTemplates(Function<Select, Template> templateFn, boolean generateBlankPredicate) {
        Set<Template> templates = new HashSet<>();

        PlainSelect ps = (PlainSelect) this.select.getSelectBody();
        Expression originalWhere = ps.getWhere();

        // Bitmap used to generate each variant, 0 indicates right-most bit.
        // bit 0: distinct
        // bit 1: order
        // bit 2: where
        // -- commented out currently: bit X: top/limit --

        int numVariants = 3;
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
                orderList.add(order);
                ps.setOrderByElements(orderList);
            }

            int whereBit = (i >> 2) & 1;
            if (whereBit == 1) {
                if (generateBlankPredicate) ps.setWhere(new LiteralExpression(Constants.PRED));
            } else {
                // Only give an option of no WHERE clause in single table case
                if (ps.getJoins() == null || ps.getJoins().isEmpty()) ps.setWhere(null);
            }

            /*
            int topBit = (i >> 3) & 1;
            if (topBit == 1) {
                // Add variant for top for where condition
                Top top = new Top();
                top.setExpression(new LiteralExpression(Constants.TOP));
                ps.setTop(top);
            }*/

            Template template = templateFn.apply(this.select);
            templates.add(template);

            // Reset everything
            ps.setDistinct(null);
            ps.setOrderByElements(null);
            ps.setWhere(originalWhere);
            // ps.setTop(null);
        }

        return templates;
    }
}
