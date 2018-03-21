package edu.umich.templar._old.sqlparse;

import edu.umich.templar._old.rdbms.Relation;
import edu.umich.templar._old.Constants;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.OracleHint;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
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
    Map<Table, Table> oldToNewTables;
    Map<String, List<String>> aliases; // Table names to aliases
    Map<String, Table> oldAliasToTable;

    List<Expression> joinPredicates;

    boolean removeWhere; // If true, we replace the WHERE clause with "#PRED"

    public FullQueryDeParser(ExpressionVisitor expressionVisitor,
                             StringBuilder buffer, Map<String, Relation> relations, boolean removeWhere) {
        super(expressionVisitor, buffer);

        this.relations = relations;

        this.tables = new HashMap<>();
        this.oldToNewTables = new HashMap<>();
        this.aliases = new HashMap<>();
        this.oldAliasToTable = new HashMap<>();

        this.joinPredicates = new ArrayList<>();

        this.removeWhere = removeWhere;

        // Point expression visitor to same objects if needed
        if (expressionVisitor instanceof FullQueryExprDeParser) {
            FullQueryExprDeParser exprDeParser = (FullQueryExprDeParser) expressionVisitor;
            exprDeParser.setTables(this.tables);
            exprDeParser.setRelations(this.relations);
            exprDeParser.setAliases(this.aliases);
            exprDeParser.setOldAliasToTable(this.oldAliasToTable);
            exprDeParser.setOldToNewTables(this.oldToNewTables);
        }
    }

    public void setTables(Map<String, Table> tables) {
        this.tables = tables;
    }

    public void setOldToNewTables(Map<Table, Table> oldToNewTables) {
        this.oldToNewTables = oldToNewTables;
    }

    public void setAliases(Map<String, List<String>> aliases) {
        this.aliases = aliases;
    }

    public void setOldAliasToTable(Map<String, Table> oldAliasToTable) {
        this.oldAliasToTable = oldAliasToTable;
    }

    protected FullQueryDeParser subParser(ExpressionVisitor expressionVisitor, StringBuilder buffer) {
        FullQueryDeParser clone = new FullQueryDeParser(expressionVisitor, buffer, this.relations, this.removeWhere);
        clone.setTables(this.tables);
        clone.setAliases(this.aliases);
        clone.setOldAliasToTable(this.oldAliasToTable);
        clone.setOldToNewTables(this.oldToNewTables);
        return clone;
    }

    public void alphabetizeAndDeparseJoins(FromItem fromItem, List<Join> joins) {
        if (fromItem == null) return;

        boolean strangeJoins = false;

        // Alphabetize FromItem list
        List<String> fromItemList = new ArrayList<>();
        fromItemList.add(Utils.fromItemToString(fromItem));

        if (joins != null) {
            for (Join join : joins) {
                if (join.getOnExpression() != null) {
                    this.joinPredicates.add(join.getOnExpression());
                }

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

    protected FromItem setTableAliasForFromItem(FromItem fromItem) {
        if (fromItem instanceof Table) {
            Alias oldAlias = fromItem.getAlias();
            Table table = (Table) fromItem;

            Table newTable;
            if (this.oldToNewTables.get(table) == null) {
                // Create a new table so that we can revert to the _old one later with no repercussions
                newTable = new Table(table.getName());

                this.oldToNewTables.put(table, newTable);
                this.tables.put(table.getName(), newTable);
            } else {
                newTable = this.oldToNewTables.get(table);
            }

            String aliasStr = newTable.getName().toLowerCase() + "_";

            List<String> tableAliases = this.aliases.get(newTable.getName());
            if (tableAliases == null) {
                tableAliases = new ArrayList<>();
                this.aliases.put(newTable.getName(), tableAliases);
            }
            aliasStr += String.valueOf(tableAliases.size());

            Alias newAlias = new Alias(aliasStr);
            tableAliases.add(newAlias.getName());

            newTable.setAlias(newAlias);

            if (oldAlias != null) {
                this.oldAliasToTable.put(oldAlias.getName(), newTable);
            }
            return newTable;
        } else {
            if (fromItem.getAlias() != null) {
                // Handle tables aliased from nested SELECT
                this.tables.put(fromItem.getAlias().getName(), new Table(fromItem.getAlias().getName()));
            }
        }
        return fromItem;
    }

    protected void setTableAliases(PlainSelect ps) {
        // Do it for first FromItem
        FromItem fromItem = this.setTableAliasForFromItem(ps.getFromItem());
        ps.setFromItem(fromItem);

        // Do it for each FromItem in each Join
        List<Join> newJoins = new ArrayList<>();
        if (ps.getJoins() != null) {
            for (Join join : ps.getJoins()) {
                FromItem joinItem = this.setTableAliasForFromItem(join.getRightItem());

                Join newJoin = new Join();
                newJoin.setRightItem(joinItem);
                newJoin.setOnExpression(join.getOnExpression());
                newJoins.add(newJoin);
            }
        }
        ps.setJoins(newJoins);

    }

    public void deparseBeforeProjection(PlainSelect plainSelect) {
        this.setTableAliases(plainSelect);

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

            Expression expr;
            if (this.removeWhere) {
                expr = new LiteralExpression(Constants.PRED);
            } else {
                expr = plainSelect.getWhere();
            }

            // Add and consume join predicates from ON expressions
            while (!this.joinPredicates.isEmpty()) {
                Expression pred = this.joinPredicates.remove(0);
                if (expr == null) {
                    expr = pred;
                } else {
                    expr = new AndExpression(expr, pred);
                }
            }

            expr.accept(this.getExpressionVisitor());
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

        if (plainSelect.getSelectItems() != null) {
            for (Iterator<SelectItem> iter = plainSelect.getSelectItems().iterator(); iter.hasNext(); ) {
                StringBuilder buffer = new StringBuilder();
                FullQueryExprDeParser exprDeParser = new FullQueryExprDeParser();
                FullQueryDeParser subParser = this.subParser(exprDeParser, buffer);
                exprDeParser.setBuffer(buffer);
                exprDeParser.setSelectVisitor(subParser);

                SelectItem selectItem = iter.next();
                selectItem.accept(subParser);
                projections.add(buffer.toString());
                /*
                if (iter.hasNext()) {
                    this.getBuffer().append(", ");
                }*/
            }
        }

        Collections.sort(projections);
        this.getBuffer().append(String.join(", ", projections));
    }

    @Override
    public void visit(SelectExpressionItem selectExpressionItem) {
        Expression expr = selectExpressionItem.getExpression();
        if (expr instanceof Column) {
            Column col = (Column) expr;
            Table table = Utils.findTableForColumn(this.tables, this.aliases, this.relations, this.oldToNewTables, this.oldAliasToTable, col);

            if (table == null) throw new RuntimeException("Could not find table for column: " + col.getColumnName());

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
