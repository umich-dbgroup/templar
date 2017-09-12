package edu.umich.tbnalir.sql;

import edu.umich.tbnalir.rdbms.Attribute;
import edu.umich.tbnalir.rdbms.Relation;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by cjbaik on 9/11/17.
 */
public class PredicateUnroller extends ExpressionVisitorAdapter {
    List<Predicate> predicates;
    Map<String, Relation> relations;

    public PredicateUnroller(Map<String, Relation> relations) {
        this.relations = relations;

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
        boolean rightIsColumn = expr.getRightExpression() instanceof Column;

        if (leftIsColumn && !rightIsColumn) {
            Column column = (Column) expr.getLeftExpression();

            String value = expr.getRightExpression().toString();
            if (value.isEmpty()) value = null;

            Attribute attr = null;
            String alias = null;
            if (column.getTable() != null) {
                Relation rel = this.relations.get(column.getTable().getName());
                if (rel == null) throw new RuntimeException("Relation " + column.getTable().getName() + " not found!");

                attr = rel.getAttributes().get(column.getColumnName());
                if (attr == null) throw new RuntimeException("Attribute " + column.getColumnName() + " not found!");

                if (column.getTable().getAlias() != null && column.getTable().getAlias().getName() != null) {
                    alias = column.getTable().getAlias().getName();
                } else {
                    alias = column.getTable().getName();
                }
            }

            Predicate pred = new Predicate(attr, operator, value);
            pred.setAlias(alias);
            this.predicates.add(pred);
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
