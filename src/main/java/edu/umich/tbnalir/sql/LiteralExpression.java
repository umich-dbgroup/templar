package edu.umich.tbnalir.sql;

import edu.umich.tbnalir.rdbms.Attribute;
import edu.umich.tbnalir.rdbms.Relation;
import edu.umich.tbnalir.template.TemplateRoot;
import edu.umich.tbnalir.util.Constants;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

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
        if (expressionVisitor instanceof ExpressionDeParser) {
            ExpressionDeParser exprDeParser = (ExpressionDeParser) expressionVisitor;
            exprDeParser.getBuffer().append(this.toString());
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
