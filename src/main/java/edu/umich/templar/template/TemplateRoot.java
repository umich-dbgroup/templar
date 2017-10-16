package edu.umich.templar.template;

import edu.umich.templar.qf.Predicate;
import edu.umich.templar.rdbms.Attribute;
import edu.umich.templar.rdbms.JoinPath;
import edu.umich.templar.qf.Projection;
import edu.umich.templar.rdbms.Relation;
import edu.umich.templar.sqlparse.*;
import edu.umich.templar.util.Constants;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.*;

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

    public static void resetAllRelations() {
        relations.entrySet().stream().map(Map.Entry::getValue).forEach(Relation::resetFromItem);
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

        PlainSelect ps = (PlainSelect) select.getSelectBody();
        FromItem oldFromItem = ps.getFromItem();
        List<Join> joins = ps.getJoins();
        ps.accept(deParser);
        ps.setFromItem(oldFromItem);
        ps.setJoins(joins);

        resetAllRelations();

        return new Template(buffer.toString().toLowerCase().intern(), TemplateType.NO_CONST);
    }

    public static Template noConstantProjectionTemplate(Select select) {
        StringBuilder buffer = new StringBuilder();
        ConstantRemovalExprDeParser constantRemover = new ConstantRemovalExprDeParser();
        ProjectionRemovalDeParser deParser = new ProjectionRemovalDeParser(constantRemover, buffer, relations, false);
        constantRemover.setSelectVisitor(deParser);
        constantRemover.setBuffer(buffer);

        PlainSelect ps = (PlainSelect) select.getSelectBody();
        FromItem oldFromItem = ps.getFromItem();
        List<Join> joins = ps.getJoins();
        ps.accept(deParser);
        ps.setFromItem(oldFromItem);
        ps.setJoins(joins);

        resetAllRelations();

        return new Template(buffer.toString().toLowerCase().intern(), TemplateType.NO_CONST_PROJ);
    }

    public static Template noComparisonTemplate(Select select) {
        StringBuilder buffer = new StringBuilder();
        ComparisonRemovalExprDeParser comparisonRemover = new ComparisonRemovalExprDeParser();
        FullQueryDeParser deParser = new FullQueryDeParser(comparisonRemover, buffer, relations, false);
        comparisonRemover.setSelectVisitor(deParser);
        comparisonRemover.setBuffer(buffer);

        PlainSelect ps = (PlainSelect) select.getSelectBody();
        FromItem oldFromItem = ps.getFromItem();
        List<Join> joins = ps.getJoins();
        ps.accept(deParser);
        ps.setFromItem(oldFromItem);
        ps.setJoins(joins);

        resetAllRelations();

        return new Template(buffer.toString().toLowerCase().intern(), TemplateType.NO_CONST_OP);
    }

    public static Template noComparisonProjectionTemplate(Select select) {
        StringBuilder buffer = new StringBuilder();
        ComparisonRemovalExprDeParser comparisonRemover = new ComparisonRemovalExprDeParser();
        ProjectionRemovalDeParser deParser = new ProjectionRemovalDeParser(comparisonRemover, buffer, relations, false);
        comparisonRemover.setSelectVisitor(deParser);
        comparisonRemover.setBuffer(buffer);

        PlainSelect ps = (PlainSelect) select.getSelectBody();
        FromItem oldFromItem = ps.getFromItem();
        List<Join> joins = ps.getJoins();
        ps.accept(deParser);
        ps.setFromItem(oldFromItem);
        ps.setJoins(joins);

        resetAllRelations();

        return new Template(buffer.toString().toLowerCase().intern(), TemplateType.NO_CONST_OP_PROJ);
    }

    public static Template noPredicateTemplate(Select select) {
        StringBuilder buffer = new StringBuilder();
        ConstantRemovalExprDeParser predicateRemover = new ConstantRemovalExprDeParser();
        FullQueryDeParser deParser = new FullQueryDeParser(predicateRemover, buffer, relations, true);
        predicateRemover.setSelectVisitor(deParser);
        predicateRemover.setBuffer(buffer);

        PlainSelect ps = (PlainSelect) select.getSelectBody();
        FromItem oldFromItem = ps.getFromItem();
        List<Join> joins = ps.getJoins();
        ps.accept(deParser);
        ps.setFromItem(oldFromItem);
        ps.setJoins(joins);

        resetAllRelations();

        return new Template(buffer.toString().toLowerCase().intern(), TemplateType.NO_PRED);
    }

    public static Template noAttributeConstantTemplate(Select select) {
        StringBuilder buffer = new StringBuilder();
        AttributeRemovalExprDeParser attributeRemover = new AttributeRemovalExprDeParser();
        AttributeRemovalDeParser deParser = new AttributeRemovalDeParser(attributeRemover, buffer, relations, false);
        attributeRemover.setSelectVisitor(deParser);
        attributeRemover.setBuffer(buffer);

        PlainSelect ps = (PlainSelect) select.getSelectBody();
        FromItem oldFromItem = ps.getFromItem();
        List<Join> joins = ps.getJoins();
        ps.accept(deParser);
        ps.setFromItem(oldFromItem);
        ps.setJoins(joins);

        resetAllRelations();

        return new Template(buffer.toString().toLowerCase().intern(), TemplateType.NO_ATTR_CONST);
    }

    public static Template noPredicateProjectionTemplate(Select select) {
        StringBuilder buffer = new StringBuilder();
        ConstantRemovalExprDeParser predicateRemover = new ConstantRemovalExprDeParser();
        ProjectionRemovalDeParser deParser = new ProjectionRemovalDeParser(predicateRemover, buffer, relations, true);
        predicateRemover.setSelectVisitor(deParser);
        predicateRemover.setBuffer(buffer);

        PlainSelect ps = (PlainSelect) select.getSelectBody();
        FromItem oldFromItem = ps.getFromItem();
        List<Join> joins = ps.getJoins();
        ps.accept(deParser);
        ps.setFromItem(oldFromItem);
        ps.setJoins(joins);

        resetAllRelations();

        return new Template(buffer.toString().toLowerCase().intern(), TemplateType.NO_PRED_PROJ);
    }

    public static Template fullQueryTemplate(Select select) {
        StringBuilder buffer = new StringBuilder();
        FullQueryExprDeParser exprDeParser = new FullQueryExprDeParser();
        FullQueryDeParser deParser = new FullQueryDeParser(exprDeParser, buffer, relations, false);
        exprDeParser.setSelectVisitor(deParser);
        exprDeParser.setBuffer(buffer);

        PlainSelect ps = (PlainSelect) select.getSelectBody();
        FromItem oldFromItem = ps.getFromItem();
        List<Join> joins = ps.getJoins();
        ps.accept(deParser);
        ps.setFromItem(oldFromItem);
        ps.setJoins(joins);

        resetAllRelations();

        return new Template(buffer.toString().toLowerCase().intern(), TemplateType.FULL_QUERY);
    }

    public Set<Template> generateNoAttributeConstantTemplates(int maxProjectionSize, int maxPredicateSize,
                                                              List<Relation> relations,
                                                              JoinPath joinPath) {
        Set<Template> results = new HashSet<>();

        PlainSelect ps = (PlainSelect) this.getSelect().getSelectBody();
        List<SelectItem> oldSelectItems = ps.getSelectItems();
        Expression oldWhere = ps.getWhere();

        List<SelectItem> selectItems = new ArrayList<>();
        ps.setSelectItems(selectItems);
        for (int i = 0; i < maxProjectionSize; i++) {
            Column col = new Column();
            SelectExpressionItem exprItem = new SelectExpressionItem();
            exprItem.setExpression(col);
            selectItems.add(exprItem);

            results.addAll(
                    this.generateAttributelessComparisonPredicatesRecursive(relations, joinPath,
                            TemplateRoot::noAttributeConstantTemplate, null, maxPredicateSize)
            );
        }

        // Reset projection/predicate
        ps.setSelectItems(oldSelectItems);
        ps.setWhere(oldWhere);

        return results;
    }

    public Set<Template> generateNoPredicateProjectionTemplates(List<Relation> relations,
                                                                JoinPath joinPath) {
        // By default, have a WHERE clause that's empty
        PlainSelect ps = (PlainSelect) this.getSelect().getSelectBody();
        ps.setWhere(new LiteralExpression(Constants.PRED));

        return this.generateTemplates(TemplateRoot::noPredicateProjectionTemplate, relations, joinPath);
    }

    public Set<Template> generateNoPredicateTemplates(Set<Set<Attribute>> projections, List<Relation> relations,
                                                      JoinPath joinPath) {
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
            results.addAll(this.generateTemplates(TemplateRoot::noPredicateTemplate, relations, joinPath));
        }

        ps.setSelectItems(null); // reset select object
        return results;
    }

    public Set<Template> generateNoComparisonProjectionTemplates(Set<Set<Attribute>> predicateAttributes,
                                                                 List<Relation> relations,
                                                                 JoinPath joinPath) {
        Set<Template> results = new HashSet<>();

        PlainSelect ps = (PlainSelect) this.getSelect().getSelectBody();

        Expression oldWhere = ps.getWhere();
        for (Set<Attribute> predAttrSet : predicateAttributes) {
            if (predAttrSet.size() == 0) continue;

            Expression predicate = null;

            for (Attribute predAttr : predAttrSet) {
                predicate = this.generateComparisonPredicateExpr(predicate, UnknownBinaryExpression.class, predAttr, false);

                ps.setWhere(predicate);

                results.addAll(this.generateTemplates(TemplateRoot::noComparisonProjectionTemplate, relations, joinPath));
            }
        }

        ps.setWhere(oldWhere); // reset WHERE predicate
        return results;
    }

    public Set<Template> generateNoComparisonTemplates(Set<Set<Attribute>> projections,
                                                       Set<Set<Attribute>> predicateAttributes,
                                                       List<Relation> relations,
                                                       JoinPath joinPath) {
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
                    predicate = this.generateComparisonPredicateExpr(predicate, UnknownBinaryExpression.class, predAttr, false);

                    ps.setWhere(predicate);

                    results.addAll(this.generateTemplates(TemplateRoot::noComparisonTemplate, relations, joinPath));
                }
            }

            ps.setWhere(oldWhere); // reset WHERE predicate
        }

        ps.setSelectItems(null); // reset select clause
        return results;
    }

    public Expression generateComparisonPredicateExpr(Expression startingPred, Class binaryExprClass, Attribute attr, boolean useOr) {
        try {
            Constructor<?> ctor = binaryExprClass.getConstructor();
            BinaryExpression binaryExpression = (BinaryExpression) ctor.newInstance();
            binaryExpression.setLeftExpression(attr.getColumn());
            binaryExpression.setRightExpression(new LiteralExpression(Utils.convertSQLTypetoConstant(attr.getType())));

            if (startingPred == null) {
                return binaryExpression;
            } else {
                if (useOr) {
                    return new OrExpression(startingPred, binaryExpression);
                } else {
                    return new AndExpression(startingPred, binaryExpression);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

    }

    public Set<Template> generateAttributelessComparisonPredicatesRecursive(List<Relation> relations,
                                                                            JoinPath joinPath,
                                                                            Function<Select, Template> templateFn,
                                                                            Expression startingPred, int depthLevel) {
        Set<Template> results = new HashSet<>();

        PlainSelect ps = (PlainSelect) this.getSelect().getSelectBody();
        Expression originalWhere = ps.getWhere();
        ps.setWhere(startingPred);
        results.addAll(this.generateTemplates(templateFn, relations, joinPath));

        ps.setWhere(originalWhere); // reset

        if (depthLevel == 0) return results;

        // generate only EqualsTo for text (perhaps include "LIKE")
        Expression predicate;
        Attribute attr = new Attribute("placeholder", "text");
        attr.setColumn(new Column());
        predicate = this.generateComparisonPredicateExpr(startingPred, EqualsTo.class, attr, false);
        results.addAll(this.generateAttributelessComparisonPredicatesRecursive(relations, joinPath,
                templateFn, predicate, depthLevel - 1));

        // generate various predicate types for nums
        attr = new Attribute("placeholder", "int");
        attr.setColumn(new Column());
        predicate = this.generateComparisonPredicateExpr(startingPred, EqualsTo.class, attr, false);
        results.addAll(this.generateAttributelessComparisonPredicatesRecursive(relations, joinPath,
                templateFn, predicate, depthLevel - 1));

        predicate = this.generateComparisonPredicateExpr(startingPred, GreaterThan.class, attr, false);
        results.addAll(this.generateAttributelessComparisonPredicatesRecursive(relations, joinPath,
                templateFn, predicate, depthLevel - 1));

        predicate = this.generateComparisonPredicateExpr(startingPred, GreaterThanEquals.class, attr, false);
        results.addAll(this.generateAttributelessComparisonPredicatesRecursive(relations, joinPath,
                templateFn, predicate, depthLevel - 1));

        predicate = this.generateComparisonPredicateExpr(startingPred, MinorThan.class, attr, false);
        results.addAll(this.generateAttributelessComparisonPredicatesRecursive(relations, joinPath,
                templateFn, predicate, depthLevel - 1));

        predicate = this.generateComparisonPredicateExpr(startingPred, MinorThanEquals.class, attr, false);
        results.addAll(this.generateAttributelessComparisonPredicatesRecursive(relations, joinPath,
                templateFn, predicate, depthLevel - 1));

        return results;
    }

    public Set<Template> generateComparisonPredicatesRecursive(List<Relation> relations,
                                                               JoinPath joinPath,
                                                               Function<Select, Template> templateFn,
                                                               Expression startingPred, List<Attribute> remainingAttr) {
        Set<Template> results = new HashSet<>();

        PlainSelect ps = (PlainSelect) this.getSelect().getSelectBody();
        Expression originalWhere = ps.getWhere();
        ps.setWhere(startingPred);
        results.addAll(this.generateTemplates(templateFn, relations, joinPath));

        ps.setWhere(originalWhere); // reset

        if (remainingAttr.isEmpty()) return results;

        Attribute attr = remainingAttr.remove(0);

        // Every type of attribute should support an equality predicate
        Expression predicate;
        predicate = this.generateComparisonPredicateExpr(startingPred, EqualsTo.class, attr, false);
        results.addAll(this.generateComparisonPredicatesRecursive(relations, joinPath, templateFn,
                predicate, new ArrayList<>(remainingAttr)));

        if (Utils.isSQLTypeNumeric(attr.getType())) {
            // For foreign/primary keys, only support equality predicates
            if (!attr.isFk() && !attr.isPk()) {
                predicate = this.generateComparisonPredicateExpr(startingPred, GreaterThan.class, attr, false);
                results.addAll(this.generateComparisonPredicatesRecursive(relations, joinPath, templateFn, predicate,
                        new ArrayList<>(remainingAttr)));

                predicate = this.generateComparisonPredicateExpr(startingPred, GreaterThanEquals.class, attr, false);
                results.addAll(this.generateComparisonPredicatesRecursive(relations, joinPath, templateFn, predicate,
                        new ArrayList<>(remainingAttr)));

                predicate = this.generateComparisonPredicateExpr(startingPred, MinorThan.class, attr, false);
                results.addAll(this.generateComparisonPredicatesRecursive(relations, joinPath, templateFn, predicate,
                        new ArrayList<>(remainingAttr)));

                predicate = this.generateComparisonPredicateExpr(startingPred, MinorThanEquals.class, attr, false);
                results.addAll(this.generateComparisonPredicatesRecursive(relations, joinPath, templateFn, predicate,
                        new ArrayList<>(remainingAttr)));

                // TODO: Could support >= and/or <= as well
                predicate = this.generateComparisonPredicateExpr(startingPred, MinorThan.class, attr, false);
                predicate = this.generateComparisonPredicateExpr(predicate, GreaterThan.class, attr, true);
                results.addAll(this.generateComparisonPredicatesRecursive(relations, joinPath, templateFn, predicate,
                        new ArrayList<>(remainingAttr)));

                predicate = this.generateComparisonPredicateExpr(startingPred, MinorThan.class, attr, false);
                predicate = this.generateComparisonPredicateExpr(predicate, GreaterThan.class, attr, false);
                results.addAll(this.generateComparisonPredicatesRecursive(relations, joinPath, templateFn, predicate,
                        new ArrayList<>(remainingAttr)));
            }
        }

        return results;
    }

    public Set<Template> generateNoConstantProjectionTemplates(Set<Set<Attribute>> predicateAttributes,
                                                               List<Relation> relations,
                                                               JoinPath joinPath) {
        Set<Template> results = new HashSet<>();

        PlainSelect ps = (PlainSelect) this.getSelect().getSelectBody();

        Expression oldWhere = ps.getWhere();
        for (Set<Attribute> predAttrSet : predicateAttributes) {
            if (predAttrSet.size() == 0) continue;

            results.addAll(this.generateComparisonPredicatesRecursive(relations, joinPath,
                    TemplateRoot::noConstantProjectionTemplate, null, new ArrayList<>(predAttrSet)));
        }

        ps.setWhere(oldWhere); // reset WHERE predicate
        return results;
    }

    public Set<Template> generateNoConstantTemplates(Set<Set<Attribute>> projections,
                                                     Set<Set<Attribute>> predicateAttributes,
                                                     List<Relation> relations,
                                                     JoinPath joinPath) {
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

                results.addAll(this.generateComparisonPredicatesRecursive(relations, joinPath,
                        TemplateRoot::noConstantTemplate, null, new ArrayList<>(predAttrSet)));
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

    public Set<Template> generateTemplates(Function<Select, Template> templateFn, List<Relation> relations,
                                           JoinPath joinPath) {
        Set<Template> templates = new HashSet<>();

        PlainSelect ps = (PlainSelect) this.select.getSelectBody();
        // Expression originalWhere = ps.getWhere();

        // Bitmap used to generate each variant, 0 indicates right-most bit.
        // bit 0: distinct
        // bit 1: order
        // bit 2: where
        // -- commented out currently: bit X: top/limit --

        int numVariants = 0;
        double iterLimit = Math.pow(2, numVariants);

        template_gen_loop:
        for (int i = 0; i < iterLimit; i++) {
            /*
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

            int topBit = (i >> 3) & 1;
            if (topBit == 1) {
                // Add variant for top for where condition
                Top top = new Top();
                top.setExpression(new LiteralExpression(Constants.TOP));
                ps.setTop(top);
            }*/

            Template template = templateFn.apply(this.select);

            /*
            // Set relations on template
            List<Relation> templateRelations = new ArrayList<>();

            FromItem fromItem = ps.getFromItem();
            if (fromItem instanceof Table) {
                Relation rel = TemplateRoot.relations.get(((Table) fromItem).getName());
                if (rel == null) throw new RuntimeException("Relation " + ((Table) fromItem).getName() + " not found!");

                templateRelations.add(rel);
            }

            for (Join join : ps.getJoins()) {
                FromItem joinFromItem = join.getRightItem();
                if (joinFromItem instanceof Table) {
                    Relation rel = TemplateRoot.relations.get(((Table) joinFromItem).getName());
                    if (rel == null) throw new RuntimeException("Relation " + ((Table) joinFromItem).getName() + " not found!");

                    templateRelations.add(rel);
                }
            }
            template.setRelations(templateRelations);*/
            template.setRelations(relations);
            template.setJoinPath(joinPath);

            // Set projections on template
            if (ps.getSelectItems() != null) {
                List<Projection> projections = new ArrayList<>();
                for (SelectItem item : ps.getSelectItems()) {
                    if (item instanceof SelectExpressionItem) {
                        SelectExpressionItem exprItem = (SelectExpressionItem) item;
                        if (exprItem.getExpression() instanceof Column) {
                            Column col = (Column) exprItem.getExpression();

                            if (col.getTable() == null) {
                                projections.add(null);
                                continue;
                            }

                            Relation rel = TemplateRoot.relations.get(col.getTable().getName());
                            if (rel == null)
                                throw new RuntimeException("Relation " + col.getTable().getName() + " not found!");
                            Integer aliasInt = Utils.getAliasIntFromAlias(col.getTable().getAlias().getName());
                            rel = new Relation(rel);
                            rel.setAliasInt(aliasInt);

                            Attribute attr = rel.getAttributes().get(col.getColumnName());
                            if (attr == null)
                                throw new RuntimeException("Attribute " + col.getColumnName() + " not found!");

                            if (col.getTable().getAlias() == null || col.getTable().getAlias().getName() == null)
                                throw new RuntimeException("Column alias for " + col + " not found!");

                            Projection proj = new Projection(attr, null, null);
                            projections.add(proj);
                        }
                    }
                }
                template.setProjections(projections);
            }

            // Set predicates on template
            Expression where = ps.getWhere();
            if (where != null) {
                PredicateUnroller predicateUnroller = new PredicateUnroller(TemplateRoot.relations, relations);
                where.accept(predicateUnroller);
                List<Predicate> preds = predicateUnroller.getPredicates();
                template.setPredicates(preds);
            }

            templates.add(template);

            // Reset everything
            // ps.setDistinct(null);
            // ps.setOrderByElements(null);
            // ps.setWhere(originalWhere);
            // ps.setTop(null);
        }

        return templates;
    }
}
