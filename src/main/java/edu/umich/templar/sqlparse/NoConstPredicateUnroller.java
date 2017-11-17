package edu.umich.templar.sqlparse;

import edu.umich.templar.qf.Predicate;
import edu.umich.templar.qf.pieces.Operator;
import edu.umich.templar.rdbms.Attribute;
import edu.umich.templar.rdbms.Relation;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by cjbaik on 9/11/17.
 */
public class NoConstPredicateUnroller extends ExpressionVisitorAdapter {
    List<Predicate> predicates;
    List<Relation> queryRelations;
    Map<String, Relation> relations;

    public NoConstPredicateUnroller(Map<String, Relation> relations, List<Relation> queryRelations) {
        this.relations = relations;
        this.queryRelations = queryRelations;
        this.predicates = new ArrayList<>();
    }

    public Map<String, Relation> getRelations() {
        return relations;
    }

    public List<Predicate> getPredicates() {
        return predicates;
    }

    public void visitPredicate(BinaryExpression expr, Operator operator) {
        // Make sure left is column, right is NOT column (otherwise it is join predicate)
        boolean leftIsColumn = expr.getLeftExpression() instanceof Column;
        boolean rightIsColumn = (expr.getRightExpression() instanceof Column) &&
                ((Column) expr.getRightExpression()).getTable().getName() != null;

        if (leftIsColumn && !rightIsColumn) {
            Column column = (Column) expr.getLeftExpression();

            String value = expr.getRightExpression().toString();
            if (value.isEmpty()) {
                value = null;
            } else {
                // remove beginning and ending quotes
                value = value.replaceAll("^\"|\"$", "");
            }

            Attribute attr = null;
            String alias = null;
            if (column.getTable() != null) {
                attr = Utils.getAttributeFromColumn(this.relations, this.queryRelations, column);

                if (column.getTable().getAlias() != null && column.getTable().getAlias().getName() != null) {
                    alias = column.getTable().getAlias().getName();
                } else {
                    alias = column.getTable().getName();
                }
            }

            if (alias != null && attr != null) {
                Attribute newAttr = new Attribute(attr);
                Relation newRel = new Relation(attr.getRelation());
                newAttr.setRelation(newRel);
                newRel.setAliasInt(Utils.getAliasIntFromAlias(alias));
                attr = newAttr;
            }

            if (attr != null) {
                Predicate pred = new Predicate(attr, operator, value);
                this.predicates.add(pred);
            }
        }
    }

    @Override
    public void visit(EqualsTo expr) {
        this.visitPredicate(expr, Operator.EQ);
    }

    @Override
    public void visit(GreaterThan expr) {
        this.visitPredicate(expr, Operator.GT);
    }

    @Override
    public void visit(GreaterThanEquals expr) {
        this.visitPredicate(expr, Operator.GTE);
    }

    @Override
    public void visit(MinorThan expr) {
        this.visitPredicate(expr, Operator.LT);
    }

    @Override
    public void visit(MinorThanEquals expr) {
        this.visitPredicate(expr, Operator.LTE);
    }

    @Override
    public void visit(NotEqualsTo expr) {
        this.visitPredicate(expr, Operator.NE);
    }

    @Override
    public void visit(Between expr) {
        Expression left = expr.getLeftExpression();
        if (left instanceof Column) {
            Column column = (Column) left;
            Attribute attr = null;
            String alias = null;
            if (column.getTable() != null) {
                attr = Utils.getAttributeFromColumn(this.relations, this.queryRelations, column);

                if (column.getTable().getAlias() != null && column.getTable().getAlias().getName() != null) {
                    alias = column.getTable().getAlias().getName();
                } else {
                    alias = column.getTable().getName();
                }
            }

            if (alias != null && attr != null) {
                Attribute newAttr = new Attribute(attr);
                Relation newRel = new Relation(attr.getRelation());
                newAttr.setRelation(newRel);
                newRel.setAliasInt(Utils.getAliasIntFromAlias(alias));
                attr = newAttr;
            }

            if (attr != null) {
                Predicate pred = new Predicate(attr, Operator.BETWEEN, expr.getBetweenExpressionStart() + "," + expr.getBetweenExpressionEnd());
                this.predicates.add(pred);
            }
        } else {
            ConstantRemovalExprDeParser deParser = new ConstantRemovalExprDeParser();
            try {
                left.accept(deParser);
                Predicate pred = new Predicate(new Attribute(deParser.getBuffer().toString(), "expr"), Operator.BETWEEN, expr.getBetweenExpressionStart() + "," + expr.getBetweenExpressionEnd());
                this.predicates.add(pred);
            } catch (Exception e) {
                // Silent failure.
            }
        }
    }
}
