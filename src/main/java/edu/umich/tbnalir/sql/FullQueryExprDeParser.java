package edu.umich.tbnalir.sql;

import edu.umich.tbnalir.rdbms.Relation;
import edu.umich.tbnalir.util.Utils;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by cjbaik on 8/29/17.
 */
public class FullQueryExprDeParser extends ExpressionDeParser {
    Map<String, Relation> relations; // Relations registered globally

    Map<String, Table> tables;
    Map<Table, Table> oldToNewTables;
    Map<String, List<String>> aliases; // Table names to aliases
    Map<String, Table> oldAliasToTable; // Old alias to table name

    public FullQueryExprDeParser() {
        this.tables = null;
        this.oldToNewTables = null;
        this.relations = null;
        this.aliases = null;
        this.oldAliasToTable = null;
    }

    public void setTables(Map<String, Table> tables) {
        this.tables = tables;
    }

    public void setOldToNewTables(Map<Table, Table> oldToNewTables) {
        this.oldToNewTables = oldToNewTables;
    }

    public void setRelations(Map<String, Relation> relations) {
        this.relations = relations;
    }

    public void setAliases(Map<String, List<String>> aliases) {
        this.aliases = aliases;
    }

    public void setOldAliasToTable(Map<String, Table> oldAliasToTable) {
        this.oldAliasToTable = oldAliasToTable;
    }

    protected FullQueryExprDeParser subParser() {
        FullQueryExprDeParser clone = new FullQueryExprDeParser();
        clone.setTables(this.tables);
        clone.setRelations(this.relations);
        clone.setAliases(this.aliases);
        clone.setOldAliasToTable(this.oldAliasToTable);
        clone.setOldToNewTables(this.oldToNewTables);
        return clone;
    }

    private void getExprList(List<OrderedPredicate> exprList, BinaryExpression expr, String operator) {
        Expression left = expr.getLeftExpression();
        if (left instanceof AndExpression) {
            this.getExprList(exprList, (BinaryExpression) left, " AND ");
        } else if (left instanceof OrExpression) {
            this.getExprList(exprList, (BinaryExpression) left, " OR ");
        } else {
            StringBuilder buffer = new StringBuilder();
            FullQueryExprDeParser subParser = this.subParser();
            subParser.setBuffer(buffer);

            left.accept(subParser);
            exprList.add(new OrderedPredicate(operator, buffer.toString()));
        }

        Expression right = expr.getRightExpression();
        if (right instanceof AndExpression) {
            this.getExprList(exprList, (BinaryExpression) right, " AND ");
        } else if (right instanceof OrExpression) {
            this.getExprList(exprList, (BinaryExpression) right, " OR ");
        } else {
            StringBuilder buffer = new StringBuilder();
            FullQueryExprDeParser subParser = this.subParser();
            subParser.setBuffer(buffer);

            right.accept(subParser);
            exprList.add(new OrderedPredicate(operator, buffer.toString()));
        }
    }

    private void alphabetizeExprList(BinaryExpression expr) {
        List<OrderedPredicate> exprList = new ArrayList<>();
        if (expr instanceof AndExpression) {
            this.getExprList(exprList, expr, " AND ");
        } else if (expr instanceof OrExpression) {
            this.getExprList(exprList, expr, " OR ");
        }

        exprList.sort((a, b) -> a.getPredicate().compareTo(b.getPredicate()));

        int i = 0;
        for (OrderedPredicate op : exprList) {
            if (i != 0) {
                this.getBuffer().append(op.getOperator());
            }
            this.getBuffer().append(op.getPredicate());
            i++;
        }
    }

    @Override
    public void visit(AndExpression andExpression) {
        this.alphabetizeExprList(andExpression);
    }

    @Override
    public void visit(OrExpression orExpression) {
        this.alphabetizeExprList(orExpression);
    }

    @Override
    public void visit(EqualsTo equalsTo) {
        StringBuilder leftBuffer = new StringBuilder();
        FullQueryExprDeParser leftParser = this.subParser();
        leftParser.setBuffer(leftBuffer);
        equalsTo.getLeftExpression().accept(leftParser);
        String left = leftBuffer.toString();

        StringBuilder rightBuffer = new StringBuilder();
        FullQueryExprDeParser rightParser = this.subParser();
        rightParser.setBuffer(rightBuffer);
        equalsTo.getRightExpression().accept(rightParser);
        String right = rightBuffer.toString();

        // Alphabetize equalsTo, only if both sides are columns
        if (equalsTo.getLeftExpression() instanceof Column
                && equalsTo.getRightExpression() instanceof Column) {
            int compare = left.compareTo(right);
            if (compare <= 0) {
                this.getBuffer().append(left);
                this.getBuffer().append(" = ");
                this.getBuffer().append(right);
            } else {
                this.getBuffer().append(right);
                this.getBuffer().append(" = ");
                this.getBuffer().append(left);
            }
        } else {
            this.getBuffer().append(left);
            this.getBuffer().append(" = ");
            this.getBuffer().append(right);
        }
    }

    @Override
    public void visit(Column col) {
        Table table = Utils.findTableForColumn(this.tables, this.aliases, this.relations, this.oldToNewTables, this.oldAliasToTable, col);

        if (table == null) throw new RuntimeException("Could not find table for column: " + col.getColumnName());

        col.setTable(table);

        this.getBuffer().append(col.getName(true));
    }
}
