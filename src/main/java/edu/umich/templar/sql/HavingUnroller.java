package edu.umich.templar.sql;

import edu.umich.templar.parse.Having;
import edu.umich.templar.rdbms.Attribute;
import edu.umich.templar.rdbms.Relation;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by cjbaik on 9/11/17.
 */
public class HavingUnroller extends ExpressionVisitorAdapter {
    List<Having> havings;
    Map<String, Relation> relations;

    public HavingUnroller(Map<String, Relation> relations) {
        this.relations = relations;

        this.havings = new ArrayList<>();
    }

    public Map<String, Relation> getRelations() {
        return relations;
    }

    public List<Having> getHavings() {
        return havings;
    }

    public void visitHaving(BinaryExpression expr, Operator operator) {
        Function function = (Function) expr.getLeftExpression();

        Column column = null;
        for (Expression fnExpr : function.getParameters().getExpressions()) {
            if (fnExpr instanceof Parenthesis) {
                fnExpr = ((Parenthesis) fnExpr).getExpression();
            }
            if (fnExpr instanceof Column) {
                column = (Column) fnExpr;
            }
        }

        String value = expr.getRightExpression().toString();
        if (value.isEmpty()) value = null;

        Attribute attr = null;
        String alias = null;
        if (column.getTable() != null) {
            attr = Utils.getAttributeFromColumn(this.relations, column);

            if (column.getTable().getAlias() != null && column.getTable().getAlias().getName() != null) {
                alias = column.getTable().getAlias().getName();
            } else {
                alias = column.getTable().getName();
            }
        }

        if (alias != null) {
            Attribute newAttr = new Attribute(attr);
            Relation newRel = new Relation(attr.getRelation());
            newAttr.setRelation(newRel);
            newRel.setAliasInt(Utils.getAliasIntFromAlias(alias));
            attr = newAttr;
        }

        Having pred = new Having(attr, operator, value, function.getName());
        this.havings.add(pred);
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
}
