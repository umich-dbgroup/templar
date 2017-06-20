package edu.umich.tbnalir;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.statement.select.TableFunction;
import net.sf.jsqlparser.util.deparser.SelectDeParser;

/**
 * Created by cjbaik on 6/20/17.
 */
public class UDFRemovalDeParser extends SelectDeParser {
    public UDFRemovalDeParser(ExpressionVisitor expressionVisitor, StringBuilder buffer) {
        super(expressionVisitor, buffer);
    }

    @Override
    public void visit(TableFunction tableFunction) {
        Function fn = tableFunction.getFunction();
        this.getBuffer().append(fn.getName());
        this.getBuffer().append('(');
        StringBuilder sb = new StringBuilder();
        for (Expression expr : fn.getParameters().getExpressions()) {
            sb.append(ConstantRemovalExprDeParser.removeConstantsFromExpr(expr));
            sb.append(',');
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(')');
        this.getBuffer().append(sb.toString());
    }
}
