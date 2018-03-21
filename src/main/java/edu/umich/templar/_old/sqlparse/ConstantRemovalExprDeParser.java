package edu.umich.templar._old.sqlparse;

import edu.umich.templar._old.Constants;
import net.sf.jsqlparser.expression.*;

/**
 * Created by cjbaik on 6/20/17.
 */
public class ConstantRemovalExprDeParser extends FullQueryExprDeParser {
    @Override
    protected FullQueryExprDeParser subParser() {
        ConstantRemovalExprDeParser clone = new ConstantRemovalExprDeParser();
        clone.setTables(this.tables);
        clone.setRelations(this.relations);
        clone.setAliases(this.aliases);
        clone.setOldAliasToTable(this.oldAliasToTable);
        clone.setOldToNewTables(this.oldToNewTables);
        return clone;
    }

    public static String removeConstantsFromExpr(Expression expr) {
        StringBuilder sb = new StringBuilder();
        if (expr instanceof DoubleValue) {
            sb.append(Constants.NUM);
        } else if (expr instanceof HexValue) {
            sb.append(Constants.NUM);
        } else if (expr instanceof LongValue) {
            sb.append(Constants.NUM);
        } else if (expr instanceof StringValue) {
            sb.append(Constants.STR);
        } else if (expr instanceof DateValue) {
            sb.append(Constants.DATE);
        } else if (expr instanceof TimestampValue) {
            sb.append(Constants.TIMESTAMP);
        } else if (expr instanceof TimeValue) {
            sb.append(Constants.TIME);
        } else if (expr instanceof DateTimeLiteralExpression) {
            sb.append(Constants.DATETIME);
        } else if (expr instanceof SignedExpression) {
            sb.append(removeConstantsFromExpr(((SignedExpression) expr).getExpression()));
        } else if (expr instanceof BinaryExpression) {
            sb.append(removeConstantsFromExpr(((BinaryExpression) expr).getLeftExpression()));
            sb.append(((BinaryExpression) expr).getStringExpression());
            sb.append(removeConstantsFromExpr(((BinaryExpression) expr).getRightExpression()));
        } else if (expr instanceof Parenthesis) {
            sb.append('(');
            sb.append(removeConstantsFromExpr(((Parenthesis) expr).getExpression()));
            sb.append(')');
        } else if (expr instanceof Function) {
            Function fn = (Function) expr;
            sb.append(fn.getName());
            sb.append('(');
            if (fn.getParameters() != null && fn.getParameters().getExpressions() != null) {
                for (Expression p : fn.getParameters().getExpressions()) {
                    sb.append(ConstantRemovalExprDeParser.removeConstantsFromExpr(p));
                    sb.append(',');
                }
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append(')');
        } else {
            sb.append(expr.toString());
        }
        return sb.toString();
    }

    @Override
    public void visit(DoubleValue doubleValue) {
        this.getBuffer().append(Constants.NUM);
    }

    @Override
    public void visit(HexValue hexValue) {
        this.getBuffer().append(Constants.NUM);
    }

    @Override
    public void visit(SignedExpression signedExpression) {
        this.getBuffer().append(removeConstantsFromExpr(signedExpression.getExpression()));
    }

    @Override
    public void visit(LongValue longValue) {
        this.getBuffer().append(Constants.NUM);
    }

    @Override
    public void visit(StringValue stringValue) {
        this.getBuffer().append(Constants.STR);
    }

    @Override
    public void visit(Function fn) {
        this.getBuffer().append(fn.getName());
        this.getBuffer().append('(');
        if (fn.getParameters() != null && fn.getParameters().getExpressions() != null) {
            for (Expression p : fn.getParameters().getExpressions()) {
                this.getBuffer().append(ConstantRemovalExprDeParser.removeConstantsFromExpr(p));
                this.getBuffer().append(',');
            }
            this.getBuffer().deleteCharAt(this.getBuffer().length() - 1);
        }
        this.getBuffer().append(')');
    }

    @Override
    public void visit(DateValue dateValue) {
        this.getBuffer().append(Constants.DATE);
    }

    @Override
    public void visit(TimestampValue timestampValue) {
        this.getBuffer().append(Constants.TIMESTAMP);
    }

    @Override
    public void visit(TimeValue timeValue) {
        this.getBuffer().append(Constants.TIME);
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
        this.getBuffer().append(Constants.DATETIME);
    }
}
