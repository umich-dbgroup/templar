package edu.umich.tbnalir.sql;

import edu.umich.tbnalir.util.Constants;
import net.sf.jsqlparser.expression.operators.relational.*;

/**
 * Created by cjbaik on 6/20/17.
 */
public class ComparisonRemovalExprDeParser extends ConstantRemovalExprDeParser {
    public static String removeComparisonOperator(ComparisonOperator cmp) {
        StringBuilder sb = new StringBuilder();
        sb.append(ConstantRemovalExprDeParser.removeConstantsFromExpr(cmp.getLeftExpression()));
        sb.append(" " + Constants.CMP + " ");
        sb.append(ConstantRemovalExprDeParser.removeConstantsFromExpr(cmp.getRightExpression()));
        return sb.toString();
    }

    @Override
    public void visit(EqualsTo equalsTo) {
        this.getBuffer().append(removeComparisonOperator(equalsTo));
    }

    @Override
    public void visit(GreaterThan greaterThan) {
        this.getBuffer().append(removeComparisonOperator(greaterThan));
    }

    @Override
    public void visit(GreaterThanEquals greaterThanEquals) {
        this.getBuffer().append(removeComparisonOperator(greaterThanEquals));
    }

    @Override
    public void visit(MinorThan minorThan) {
        this.getBuffer().append(removeComparisonOperator(minorThan));
    }

    @Override
    public void visit(MinorThanEquals minorThanEquals) {
        this.getBuffer().append(removeComparisonOperator(minorThanEquals));
    }

    @Override
    public void visit(NotEqualsTo notEqualsTo) {
        this.getBuffer().append(removeComparisonOperator(notEqualsTo));
    }
}
