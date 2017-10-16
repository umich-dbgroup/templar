package edu.umich.templar.sqlparse.agnostic;

import edu.umich.templar.qf.agnostic.AgnosticHaving;
import edu.umich.templar.qf.pieces.Operator;
import edu.umich.templar.qf.pieces.QFFunction;
import edu.umich.templar.rdbms.Attribute;
import edu.umich.templar.rdbms.Relation;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by cjbaik on 10/16/17.
 */
public class AgnosticHavingUnroller extends ExpressionVisitorAdapter {
    List<AgnosticHaving> havings;
    List<Relation> queryRelations;
    Map<String, Relation> relations;

    public AgnosticHavingUnroller(Map<String, Relation> relations, List<Relation> queryRelations) {
        this.relations = relations;
        this.queryRelations = queryRelations;
        this.havings = new ArrayList<>();
    }

    public Map<String, Relation> getRelations() {
        return relations;
    }

    public List<AgnosticHaving> getHavings() {
        return havings;
    }

    public void visitHaving(BinaryExpression expr, Operator operator) {
        if (!(expr.getLeftExpression() instanceof Function)) return;

        Function function = (Function) expr.getLeftExpression();

        Column column = Utils.getColumnFromFunction(function);

        /*String value = expr.getRightExpression().toString();
        if (value.isEmpty()) value = null;*/

        Attribute attr = null;
        // String alias = null;
        if (column != null && column.getTable().getName() != null) {
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
            AgnosticHaving having = new AgnosticHaving(attr.getAttributeType(),
                    QFFunction.getFunction(function.getName()), operator);
            this.havings.add(having);
        }
    }

    @Override
    public void visit(EqualsTo expr) {
        this.visitHaving(expr, Operator.EQ);
    }

    @Override
    public void visit(GreaterThan expr) {
        this.visitHaving(expr, Operator.GT);
    }

    @Override
    public void visit(GreaterThanEquals expr) {
        this.visitHaving(expr, Operator.GTE);
    }

    @Override
    public void visit(MinorThan expr) {
        this.visitHaving(expr, Operator.LT);
    }

    @Override
    public void visit(MinorThanEquals expr) {
        this.visitHaving(expr, Operator.LTE);
    }

    @Override
    public void visit(NotEqualsTo expr) {
        this.visitHaving(expr, Operator.NE);
    }

    @Override
    public void visit(AndExpression expr) {
        expr.getLeftExpression().accept(this);
        expr.getRightExpression().accept(this);
    }
}
