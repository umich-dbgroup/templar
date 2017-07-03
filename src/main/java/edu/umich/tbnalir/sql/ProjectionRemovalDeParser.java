package edu.umich.tbnalir.sql;

import edu.umich.tbnalir.util.Constants;
import edu.umich.tbnalir.util.Utils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.OracleHint;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.deparser.LimitDeparser;
import net.sf.jsqlparser.util.deparser.OrderByDeParser;

import java.util.*;

/**
 * Created by cjbaik on 6/20/17.
 */
public class ProjectionRemovalDeParser extends SelectConstantRemovalDeParser {
    public ProjectionRemovalDeParser(ExpressionVisitor expressionVisitor, StringBuilder buffer, boolean removeWhere) {
        super(expressionVisitor, buffer, removeWhere);
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
                        iterator(); iter.hasNext(); ) {
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
            this.getBuffer().append("TOP " + Constants.TOP + " ");
            //this.getBuffer().append(top).append(" ");
        }

        // Hide all select items and replace with blanket "projection"
        this.getBuffer().append(Constants.PROJ);

        /*
        for (Iterator<SelectItem> iter = plainSelect.getSelectItems().iterator(); iter.hasNext();) {
            SelectItem selectItem = iter.next();
            selectItem.accept(this);
            if (iter.hasNext()) {
                this.getBuffer().append(", ");
            }
        }
        */

        /* Remove INTO
        if (plainSelect.getIntoTables() != null) {
            this.getBuffer().append(" INTO ");
            for (Iterator<Table> iter = plainSelect.getIntoTables().iterator(); iter.hasNext(); ) {
                visit(iter.next());
                if (iter.hasNext()) {
                    this.getBuffer().append(", ");
                }
            }
        }*/

        this.alphabetizeAndDeparseJoins(plainSelect.getFromItem(), plainSelect.getJoins());

        if (plainSelect.getWhere() != null) {
            this.getBuffer().append(" WHERE ");

            if (this.removeWhere) {
                this.getBuffer().append(Constants.PRED + " ");
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
                    hasNext(); ) {
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
            this.getBuffer().append(" ORDER BY " + Constants.ORDER);

            // obscure out ORDER BY
            /*new OrderByDeParser(this.getExpressionVisitor(), this.getBuffer()).
                    deParse(plainSelect.isOracleSiblings(), plainSelect.getOrderByElements());*/
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

}
