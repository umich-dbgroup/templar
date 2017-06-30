package edu.umich.tbnalir.sql;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;

/**
 * Created by cjbaik on 6/30/17.
 */
public class LiteralExpression implements Expression {
    public String value;

    public LiteralExpression(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public void accept(ExpressionVisitor expressionVisitor) {
        // Do nothing
    }

    @Override
    public String toString() {
        return value;
    }
}
