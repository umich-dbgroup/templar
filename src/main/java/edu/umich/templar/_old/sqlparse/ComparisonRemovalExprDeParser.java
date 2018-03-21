package edu.umich.templar._old.sqlparse;

import edu.umich.templar._old.Constants;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SubSelect;


/**
 * Created by cjbaik on 6/20/17.
 */
public class ComparisonRemovalExprDeParser extends ConstantRemovalExprDeParser {
    @Override
    protected FullQueryExprDeParser subParser() {
        ComparisonRemovalExprDeParser clone = new ComparisonRemovalExprDeParser();
        clone.setTables(this.tables);
        clone.setRelations(this.relations);
        clone.setAliases(this.aliases);
        clone.setOldAliasToTable(this.oldAliasToTable);
        clone.setOldToNewTables(this.oldToNewTables);
        return clone;
    }

    public String removeComparisonOperator(Expression expr) {
        StringBuilder sb = new StringBuilder();
        if (expr instanceof BinaryExpression) {
            BinaryExpression binary = (BinaryExpression) expr;
            sb.append(removeComparisonOperator(binary.getLeftExpression()));
            sb.append(" " + Constants.CMP + " ");
            sb.append(removeComparisonOperator(binary.getRightExpression()));
        } else if (expr instanceof Parenthesis) {
            sb.append('(');
            sb.append(removeComparisonOperator(((Parenthesis) expr).getExpression()));
            sb.append(')');
        } else if (expr instanceof DoubleValue || expr instanceof HexValue || expr instanceof LongValue
                || expr instanceof StringValue || expr instanceof DateValue || expr instanceof TimestampValue
                || expr instanceof TimeValue || expr instanceof DateTimeLiteralExpression
                || expr instanceof SignedExpression || expr instanceof Function) {
            sb.append(ConstantRemovalExprDeParser.removeConstantsFromExpr(expr));
        } else if (expr instanceof SubSelect) {
            SubSelect subSelect = (SubSelect) expr;
            FullQueryExprDeParser subParser = this.subParser();
            subSelect.accept((ExpressionVisitor) subParser);
            sb.append(subParser.getBuffer());
        } else {
            FullQueryExprDeParser subParser = this.subParser();
            expr.accept(subParser);
            sb.append(subParser.getBuffer());
        }
        return sb.toString();
    }

    @Override
    public void visit(EqualsTo equalsTo) {
        // If it is two column equivalences (likely to be join predicate)
        if (equalsTo.getLeftExpression() instanceof Column
                && equalsTo.getRightExpression() instanceof Column) {
            FullQueryExprDeParser leftParser = this.subParser();
            equalsTo.getLeftExpression().accept(leftParser);
            String left = leftParser.getBuffer().toString();

            FullQueryExprDeParser rightParser = this.subParser();
            equalsTo.getRightExpression().accept(rightParser);
            String right = rightParser.getBuffer().toString();

            if (left.equals(right)) {
                // Throw an error if we detect that there's a faulty/redundant join predicate
                throw new RuntimeException("Join predicate is: " + left + " = " + right);
            }

            if (left.compareTo(right) <= 0) {
                this.getBuffer().append(left);
                this.getBuffer().append(" = ");
                this.getBuffer().append(right);
            } else {
                this.getBuffer().append(right);
                this.getBuffer().append(" = ");
                this.getBuffer().append(left);
            }
        } else {
            this.getBuffer().append(removeComparisonOperator(equalsTo));
        }
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

    @Override
    public void visit(Parenthesis parenthesis) {
        this.getBuffer().append('(');
        this.getBuffer().append(removeComparisonOperator(parenthesis.getExpression()));
        this.getBuffer().append(')');
    }
}
