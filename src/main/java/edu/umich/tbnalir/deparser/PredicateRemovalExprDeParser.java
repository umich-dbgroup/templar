package edu.umich.tbnalir.deparser;

import edu.umich.tbnalir.util.Constants;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.SignedExpression;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseAnd;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseOr;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseXor;
import net.sf.jsqlparser.expression.operators.relational.*;

/**
 * Created by cjbaik on 6/20/17.
 */
public class PredicateRemovalExprDeParser extends ConstantRemovalExprDeParser {
    @Override
    public void visit(Between between) {
        this.getBuffer().append(Constants.PRED);
    }

    @Override
    public void visit(EqualsTo equalsTo) {
        this.getBuffer().append(Constants.PRED);
    }

    @Override
    public void visit(NotExpression notExpr) {
        this.getBuffer().append(Constants.PRED);
    }

    @Override
    public void visit(GreaterThan greaterThan) {
        this.getBuffer().append(Constants.PRED);
    }

    @Override
    public void visit(GreaterThanEquals greaterThanEquals) {
        this.getBuffer().append(Constants.PRED);
    }

    @Override
    public void visit(InExpression inExpression) {
        this.getBuffer().append(Constants.PRED);
    }

    @Override
    public void visit(SignedExpression signedExpression) {
        this.getBuffer().append(Constants.PRED);
    }

    @Override
    public void visit(IsNullExpression isNullExpression) {
        this.getBuffer().append(Constants.PRED);
    }

    @Override
    public void visit(LikeExpression likeExpression) {
        this.getBuffer().append(Constants.PRED);
    }

    @Override
    public void visit(ExistsExpression existsExpression) {
        this.getBuffer().append(Constants.PRED);
    }

    @Override
    public void visit(MinorThan minorThan) {
        this.getBuffer().append(Constants.PRED);
    }

    @Override
    public void visit(MinorThanEquals minorThanEquals) {
        this.getBuffer().append(Constants.PRED);
    }

    @Override
    public void visit(NotEqualsTo notEqualsTo) {
        this.getBuffer().append(Constants.PRED);
    }

    @Override
    public void visit(BitwiseAnd bitwiseAnd) {
        this.getBuffer().append(Constants.PRED);
    }

    @Override
    public void visit(BitwiseOr bitwiseOr) {
        this.getBuffer().append(Constants.PRED);
    }

    @Override
    public void visit(BitwiseXor bitwiseXor) {
        this.getBuffer().append(Constants.PRED);
    }
}
