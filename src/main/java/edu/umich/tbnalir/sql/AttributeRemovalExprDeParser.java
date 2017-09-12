package edu.umich.tbnalir.sql;

import edu.umich.tbnalir.util.Constants;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by cjbaik on 9/5/17.
 */
public class AttributeRemovalExprDeParser extends ConstantRemovalExprDeParser {
    protected FullQueryExprDeParser fullQuerySubParser() {
        FullQueryExprDeParser clone = new FullQueryExprDeParser();
        clone.setTables(this.tables);
        clone.setRelations(this.relations);
        clone.setAliases(this.aliases);
        clone.setOldAliasToTable(this.oldAliasToTable);
        clone.setOldToNewTables(this.oldToNewTables);
        return clone;
    }

    @Override
    protected FullQueryExprDeParser subParser() {
        AttributeRemovalExprDeParser clone = new AttributeRemovalExprDeParser();
        clone.setTables(this.tables);
        clone.setRelations(this.relations);
        clone.setAliases(this.aliases);
        clone.setOldAliasToTable(this.oldAliasToTable);
        clone.setOldToNewTables(this.oldToNewTables);
        return clone;
    }

    @Override
    public void visit(EqualsTo equalsTo) {
        // If it is two column equivalences (likely to be join predicate)
        if (equalsTo.getLeftExpression() instanceof Column
                && equalsTo.getRightExpression() instanceof Column) {
            FullQueryExprDeParser leftParser = this.fullQuerySubParser();
            equalsTo.getLeftExpression().accept(leftParser);
            String left = leftParser.getBuffer().toString();

            FullQueryExprDeParser rightParser = this.fullQuerySubParser();
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
            FullQueryExprDeParser leftParser = this.subParser();
            equalsTo.getLeftExpression().accept(leftParser);
            String left = leftParser.getBuffer().toString();

            FullQueryExprDeParser rightParser = this.subParser();
            equalsTo.getRightExpression().accept(rightParser);
            String right = rightParser.getBuffer().toString();

            this.getBuffer().append(left);
            this.getBuffer().append(" = ");
            this.getBuffer().append(right);
        }
    }

    @Override
    public void visit(Column col) {
        this.getBuffer().append(Constants.COLUMN);
    }
}
