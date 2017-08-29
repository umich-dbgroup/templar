package edu.umich.tbnalir.sql;

import edu.umich.tbnalir.rdbms.Relation;
import edu.umich.tbnalir.util.Constants;
import edu.umich.tbnalir.util.Utils;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.OracleHint;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.deparser.LimitDeparser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;

import java.util.*;

/**
 * Created by cjbaik on 8/28/17.
 */
public class FullQueryDeParser extends SelectDeParser {
    Map<String, Relation> relations; // Relations registered globally

    Map<String, Table> tables;
    Map<String, List<Alias>> aliases; // Table names to aliases
    Map<Alias, Alias> oldToNewAlias;

    public FullQueryDeParser(ExpressionVisitor expressionVisitor, StringBuilder buffer, Map<String, Relation> relations) {
        super(expressionVisitor, buffer);

        this.relations = relations;

        this.tables = new HashMap<>();
        this.aliases = new HashMap<>();
        this.oldToNewAlias = new HashMap<>();

        // Point expression visitor to same objects if needed
        if (expressionVisitor instanceof FullQueryExprDeParser) {
            FullQueryExprDeParser exprDeParser = (FullQueryExprDeParser) expressionVisitor;
            exprDeParser.setTables(this.tables);
            exprDeParser.setRelations(this.relations);
            exprDeParser.setAliases(this.aliases);
        }
    }

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

    protected void setTableAliasForFromItem(FromItem fromItem) {
        if (fromItem instanceof Table) {
            Alias oldAlias = fromItem.getAlias();
            Table table = (Table) fromItem;

            this.tables.put(table.getName(), table);

            String aliasStr = table.getName().toLowerCase() + "_a";
            Alias newAlias = new Alias(aliasStr);

            List<Alias> tableAliases = this.aliases.get(table.getName());
            if (tableAliases == null) {
                tableAliases = new ArrayList<>();
                this.aliases.put(table.getName(), tableAliases);
            }
            tableAliases.add(newAlias);

            table.setAlias(newAlias);

            if (oldAlias != null) {
                this.oldToNewAlias.put(oldAlias, newAlias);
            }
        }
    }

    protected void setTableAliases(FromItem fromItem, List<Join> joins) {
        // Do it for first FromItem
        this.setTableAliasForFromItem(fromItem);

        // Do it for each FromItem in each Join
        if (joins != null) {
            for (Join join : joins) {
                this.setTableAliasForFromItem(join.getRightItem());
            }
        }

    }

    public void deparseBeforeProjection(PlainSelect plainSelect) {
        this.setTableAliases(plainSelect.getFromItem(), plainSelect.getJoins());

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
            this.getBuffer().append(top).append(" ");
        }
    }

    public void deparsePredicate(PlainSelect plainSelect) {
        if (plainSelect.getWhere() != null) {
            this.getBuffer().append(" WHERE ");

            plainSelect.getWhere().accept(this.getExpressionVisitor());
        }
    }

    public void deparseAfterPredicate(PlainSelect plainSelect) {
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

    public void alphabetizeAndDeparseProjection(PlainSelect plainSelect) {
        List<String> projections = new ArrayList<>();

        for (Iterator<SelectItem> iter = plainSelect.getSelectItems().iterator(); iter.hasNext(); ) {
            SelectItem selectItem = iter.next();
            // selectItem.accept(this);
            projections.add(selectItem.toString());
            /*
            if (iter.hasNext()) {
                this.getBuffer().append(", ");
            }*/
        }

        Collections.sort(projections);
        this.getBuffer().append(String.join(", ", projections));
    }

    @Override
    public void visit(SelectExpressionItem selectExpressionItem) {
        Expression expr = selectExpressionItem.getExpression();
        if (expr instanceof Column) {
            Column col = (Column) expr;
            Table table = Utils.findTableForColumn(this.tables, this.relations, col);

            if (table == null) throw new RuntimeException("Could not find table for column: " + col.getColumnName());

            List<Alias> tableAliases = this.aliases.get(table.getName());
            if (tableAliases.size() > 1) {
                throw new RuntimeException("WARNING: More than 1 alias for table! Which to select?");
            }
            table.setAlias(tableAliases.get(0));
            col.setTable(table);

            this.getBuffer().append(col.getName(true));
        }
    }

    @Override
    public void visit(PlainSelect plainSelect) {
        this.deparseBeforeProjection(plainSelect);

        this.alphabetizeAndDeparseProjection(plainSelect);

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

        this.deparsePredicate(plainSelect);

        this.deparseAfterPredicate(plainSelect);
    }

    @Override
    public void visit(TableFunction tableFunction) {
        this.getBuffer().append(Utils.tableFunctionToString(tableFunction));
    }
}
