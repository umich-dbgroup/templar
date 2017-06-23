package edu.umich.tbnalir;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.OracleHint;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.deparser.LimitDeparser;
import net.sf.jsqlparser.util.deparser.OrderByDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;

import java.util.Iterator;

/**
 * Created by cjbaik on 6/20/17.
 */
public class SelectConstantRemovalDeParser extends SelectDeParser {
    boolean removeWhere;

    public SelectConstantRemovalDeParser(ExpressionVisitor expressionVisitor, StringBuilder buffer, boolean removeWhere) {
        super(expressionVisitor, buffer);

        this.removeWhere = removeWhere;
    }

    @Override
    public void visit(PlainSelect plainSelect) {
        if (plainSelect.isUseBrackets()) {
            this.getBuffer().append("(");
        }
        this.getBuffer().append("SELECT ");

        OracleHint hint = plainSelect.getOracleHint();
        if (hint != null) {
            this.getBuffer().append(hint).append(" ");
        }

        Skip skip = plainSelect.getSkip();
        if (skip != null) {
            this.getBuffer().append(skip).append(" ");
        }

        First first = plainSelect.getFirst();
        if (first != null) {
            this.getBuffer().append(first).append(" ");
        }

        if (plainSelect.getDistinct() != null) {
            if (plainSelect.getDistinct().isUseUnique()) {
                this.getBuffer().append("UNIQUE ");
            } else {
                this.getBuffer().append("DISTINCT ");
            }
            if (plainSelect.getDistinct().getOnSelectItems() != null) {
                this.getBuffer().append("ON (");
                for (Iterator<SelectItem> iter = plainSelect.getDistinct().getOnSelectItems().
                        iterator(); iter.hasNext();) {
                    SelectItem selectItem = iter.next();
                    selectItem.accept(this);
                    if (iter.hasNext()) {
                        this.getBuffer().append(", ");
                    }
                }
                this.getBuffer().append(") ");
            }

        }

        Top top = plainSelect.getTop();
        if (top != null) {
            // ABSTRACT OUT TOP N
            this.getBuffer().append("TOP #TOP ");
            //this.getBuffer().append(top).append(" ");
        }

        for (Iterator<SelectItem> iter = plainSelect.getSelectItems().iterator(); iter.hasNext();) {
            SelectItem selectItem = iter.next();
            selectItem.accept(this);
            if (iter.hasNext()) {
                this.getBuffer().append(", ");
            }
        }

        if (plainSelect.getIntoTables() != null) {
            this.getBuffer().append(" INTO ");
            for (Iterator<Table> iter = plainSelect.getIntoTables().iterator(); iter.hasNext();) {
                visit(iter.next());
                if (iter.hasNext()) {
                    this.getBuffer().append(", ");
                }
            }
        }

        if (plainSelect.getFromItem() != null) {
            this.getBuffer().append(" FROM ");
            plainSelect.getFromItem().accept(this);
        }

        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                deparseJoin(join);
            }
        }

        if (plainSelect.getWhere() != null) {
            this.getBuffer().append(" WHERE ");

            if (this.removeWhere) {
                this.getBuffer().append("#PREDICATE ");
            } else {
                plainSelect.getWhere().accept(this.getExpressionVisitor());
            }
        }

        if (plainSelect.getOracleHierarchical() != null) {
            plainSelect.getOracleHierarchical().accept(this.getExpressionVisitor());
        }

        if (plainSelect.getGroupByColumnReferences() != null) {
            this.getBuffer().append(" GROUP BY ");
            for (Iterator<Expression> iter = plainSelect.getGroupByColumnReferences().iterator(); iter.
                    hasNext();) {
                Expression columnReference = iter.next();
                columnReference.accept(this.getExpressionVisitor());
                if (iter.hasNext()) {
                    this.getBuffer().append(", ");
                }
            }
        }

        if (plainSelect.getHaving() != null) {
            this.getBuffer().append(" HAVING ");
            plainSelect.getHaving().accept(this.getExpressionVisitor());
        }

        if (plainSelect.getOrderByElements() != null) {
            new OrderByDeParser(this.getExpressionVisitor(), this.getBuffer()).
                    deParse(plainSelect.isOracleSiblings(), plainSelect.getOrderByElements());
        }

        if (plainSelect.getLimit() != null) {
            new LimitDeparser(this.getBuffer()).deParse(plainSelect.getLimit());
        }
        if (plainSelect.getOffset() != null) {
            deparseOffset(plainSelect.getOffset());
        }
        if (plainSelect.getFetch() != null) {
            deparseFetch(plainSelect.getFetch());
        }
        if (plainSelect.isForUpdate()) {
            this.getBuffer().append(" FOR UPDATE");
            if (plainSelect.getForUpdateTable() != null) {
                this.getBuffer().append(" OF ").append(plainSelect.getForUpdateTable());
            }
            if (plainSelect.getWait() != null) {
                // wait's toString will do the formatting for us
                this.getBuffer().append(plainSelect.getWait());
            }
        }
        if (plainSelect.isUseBrackets()) {
            this.getBuffer().append(")");
        }
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
