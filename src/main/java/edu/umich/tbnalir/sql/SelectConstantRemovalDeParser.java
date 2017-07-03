package edu.umich.tbnalir.sql;

import edu.umich.tbnalir.util.Constants;
import edu.umich.tbnalir.util.Utils;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.deparser.LimitDeparser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;

import java.util.*;

/**
 * Created by cjbaik on 6/20/17.
 */
public class SelectConstantRemovalDeParser extends SelectDeParser {
    boolean removeWhere;

    public void alphabetizeAndDeparseJoins(FromItem fromItem, List<Join> joins) {
        if (fromItem == null) return;

        boolean strangeJoins = false;

        // Alphabetize FromItem list
        List<String> fromItemList = new ArrayList<>();
        fromItemList.add(Utils.fromItemToString(fromItem));

        if (joins != null) {
            for (Join join : joins) {
                if (Utils.isStrangeJoin(join)) {
                    strangeJoins = true;
                    break;
                } else {
                    fromItemList.add(Utils.fromItemToString(join.getRightItem()));
                }
            }
        }

        this.getBuffer().append(" FROM ");
        if (strangeJoins) {
            fromItem.accept(this);

            for (Join join : joins) {
                deparseJoin(join);
            }
        } else {
            Collections.sort(fromItemList);
            StringJoiner sj = new StringJoiner(", ");
            for (String fi : fromItemList) {
                sj.add(fi);
            }
            this.getBuffer().append(sj.toString());
        }
    }

    @Override
    public void deparseJoin(Join join) {
        // If join is not left/right/cross/semi, convert to simple join.
        if (!Utils.isStrangeJoin(join)) {
            join.setSimple(true);
        }


        if (join.isSimple()) {
            this.getBuffer().append(", ");
        } else {
            if (join.isRight()) {
                this.getBuffer().append(" RIGHT");
            } else if (join.isNatural()) {
                this.getBuffer().append(" NATURAL");
            } else if (join.isFull()) {
                this.getBuffer().append(" FULL");
            } else if (join.isLeft()) {
                this.getBuffer().append(" LEFT");
            } else if (join.isCross()) {
                this.getBuffer().append(" CROSS");
            }

            /*
            if (join.isOuter()) {
                this.getBuffer().append(" OUTER");
            } else if (join.isInner()) {
                this.getBuffer().append(" INNER");
            }*/
            if (join.isSemi()) {
                this.getBuffer().append(" SEMI");
            }

            this.getBuffer().append(" JOIN ");

        }

        FromItem fromItem = join.getRightItem();
        fromItem.accept(this);

        /* hide ON expressions
        if (join.getOnExpression() != null) {
            this.getBuffer().append(" ON ");
            join.getOnExpression().accept(this.getExpressionVisitor());
        }*/

        if (join.getUsingColumns() != null) {
            this.getBuffer().append(" USING (");
            for (Iterator<Column> iterator = join.getUsingColumns().iterator(); iterator.hasNext();) {
                Column column = iterator.next();
                this.getBuffer().append(column.toString());
                if (iterator.hasNext()) {
                    this.getBuffer().append(", ");
                }
            }
            this.getBuffer().append(")");
        }
    }

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

        for (Iterator<SelectItem> iter = plainSelect.getSelectItems().iterator(); iter.hasNext(); ) {
            SelectItem selectItem = iter.next();
            selectItem.accept(this);
            if (iter.hasNext()) {
                this.getBuffer().append(", ");
            }
        }

        /* Remove INTO
        if (plainSelect.getIntoTables() != null) {
            this.getBuffer().append(" INTO ");
            for (Iterator<Table> iter = plainSelect.getIntoTables().iterator(); iter.hasNext();) {
                visit(iter.next());
                if (iter.hasNext()) {
                    this.getBuffer().append(", ");
                }
            }
        } */

        this.alphabetizeAndDeparseJoins(plainSelect.getFromItem(), plainSelect.getJoins());

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
            this.getBuffer().append(" ORDER BY " + Constants.ORDER);

            /*
            new OrderByDeParser(this.getExpressionVisitor(), this.getBuffer()).
                    deParse(plainSelect.isOracleSiblings(), plainSelect.getOrderByElements());
            */
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
        this.getBuffer().append(Utils.tableFunctionToString(tableFunction));
    }

    @Override
    public void visit(Table tableName) {
        this.getBuffer().append(Utils.tableToString(tableName));
    }
}
