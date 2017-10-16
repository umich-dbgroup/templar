package edu.umich.templar.sqlparse.agnostic;

import edu.umich.templar.qf.agnostic.AgnosticPredicate;
import edu.umich.templar.qf.pieces.Operator;
import edu.umich.templar.rdbms.Attribute;
import edu.umich.templar.rdbms.Relation;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by cjbaik on 10/16/17.
 */
public class AgnosticPredicateUnroller extends ExpressionVisitorAdapter {
    List<AgnosticPredicate> predicates;
    List<Relation> queryRelations;
    Map<String, Relation> relations;

    public AgnosticPredicateUnroller(Map<String, Relation> relations, List<Relation> queryRelations) {
        this.relations = relations;
        this.queryRelations = queryRelations;
        this.predicates = new ArrayList<>();
    }

    public Map<String, Relation> getRelations() {
        return relations;
    }

    public List<AgnosticPredicate> getPredicates() {
        return predicates;
    }

    public void visitPredicate(BinaryExpression expr, Operator operator) {
        // Make sure left is column, right is NOT column (otherwise it is join predicate)
        boolean leftIsColumn = expr.getLeftExpression() instanceof Column;
        boolean rightIsColumn = (expr.getRightExpression() instanceof Column) &&
                ((Column) expr.getRightExpression()).getTable().getName() != null;

        if (leftIsColumn && !rightIsColumn) {
            Column column = (Column) expr.getLeftExpression();

            /*String value = expr.getRightExpression().toString();
            if (value.isEmpty()) {
                value = null;
            } else {
                // remove beginning and ending quotes
                value = value.replaceAll("^\"|\"$", "");
            }*/

            Attribute attr = null;
            // String alias = null;
            if (column.getTable() != null) {
                attr = Utils.getAttributeFromColumn(this.relations, this.queryRelations, column);

                /*
                if (column.getTable().getAlias() != null && column.getTable().getAlias().getName() != null) {
                    alias = column.getTable().getAlias().getName();
                } else {
                    alias = column.getTable().getName();
                }*/
            }

            /*if (alias != null && attr != null) {
                Attribute newAttr = new Attribute(attr);
                Relation newRel = new Relation(attr.getRelation());
                newAttr.setRelation(newRel);
                newRel.setAliasInt(Utils.getAliasIntFromAlias(alias));
                attr = newAttr;
            }*/

            if (attr != null) {
                AgnosticPredicate pred = new AgnosticPredicate(attr.getAttributeType(), operator);
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
}
