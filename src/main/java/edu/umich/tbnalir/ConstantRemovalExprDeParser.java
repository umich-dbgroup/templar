package edu.umich.tbnalir;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

/**
 * Created by cjbaik on 6/20/17.
 */
public class ConstantRemovalExprDeParser extends ExpressionDeParser {
    public static String removeConstantsFromExpr(Expression expr) {
        StringBuilder sb = new StringBuilder();
        if (expr instanceof DoubleValue) {
            sb.append("#DOUBLE");
        } else if (expr instanceof HexValue) {
            sb.append("#HEX");
        } else if (expr instanceof LongValue) {
            sb.append("#LONG");
        } else if (expr instanceof StringValue) {
            sb.append("#STR");
        } else if (expr instanceof DateValue) {
            sb.append("#DATE");
        } else if (expr instanceof TimestampValue) {
            sb.append("#TIMESTAMP");
        } else if (expr instanceof TimeValue) {
            sb.append("#TIME");
        } else if (expr instanceof DateTimeLiteralExpression) {
            sb.append("#DATETIME");
        } else if (expr instanceof BinaryExpression) {
            sb.append(removeConstantsFromExpr(((BinaryExpression) expr).getLeftExpression()));
            sb.append(" / ");
            sb.append(removeConstantsFromExpr(((BinaryExpression) expr).getRightExpression()));
        } else {
            sb.append(expr.toString());
        }
        return sb.toString();
    }

    @Override
    public void visit(DoubleValue doubleValue) {
        this.getBuffer().append("#DOUBLE");
    }

    @Override
    public void visit(HexValue hexValue) {
        this.getBuffer().append("#HEX");
    }

    @Override
    public void visit(LongValue longValue) {
        this.getBuffer().append("#LONG");
    }

    @Override
    public void visit(StringValue stringValue) {
        this.getBuffer().append("#STR");
    }

    @Override
    public void visit(DateValue dateValue) {
        this.getBuffer().append("#DATE");
    }

    @Override
    public void visit(TimestampValue timestampValue) {
        this.getBuffer().append("#TIMESTAMP");
    }

    @Override
    public void visit(TimeValue timeValue) {
        this.getBuffer().append("#TIME");
    }

    @Override
    public void visit(CastExpression cast) {
        this.getBuffer().append("CAST(");
        this.getBuffer().append(removeConstantsFromExpr(cast.getLeftExpression()));
        this.getBuffer().append(" AS ");
        this.getBuffer().append(cast.getType());
        this.getBuffer().append(")");
    }

    @Override
    public void visit(DateTimeLiteralExpression literal) {
        this.getBuffer().append("#DATETIME");
    }
}
