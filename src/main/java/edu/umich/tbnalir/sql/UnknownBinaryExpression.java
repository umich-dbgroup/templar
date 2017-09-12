package edu.umich.tbnalir.sql;

import edu.umich.tbnalir.rdbms.Attribute;
import edu.umich.tbnalir.rdbms.Relation;
import edu.umich.tbnalir.util.Constants;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

/**
 * Created by cjbaik on 9/12/17.
 */
public class UnknownBinaryExpression extends BinaryExpression {
    @Override
    public String getStringExpression() {
        return this.getLeftExpression().toString() + " " + Constants.CMP + " " + this.getRightExpression().toString();
    }

    @Override
    public void accept(ExpressionVisitor expressionVisitor) {
        if (expressionVisitor instanceof ExpressionDeParser) {
            ExpressionDeParser exprDeParser = (ExpressionDeParser) expressionVisitor;
            exprDeParser.getBuffer().append(this.getStringExpression());
        }

        // If this is a comparison predicate
        if (expressionVisitor instanceof PredicateUnroller) {
            PredicateUnroller unroller = (PredicateUnroller) expressionVisitor;

            if (!(this.getLeftExpression() instanceof Column)) {
                throw new RuntimeException("If I don't have a column, what is it??");
            }

            Column col = (Column) this.getLeftExpression();

            if (col.getTable() == null) {
                throw new RuntimeException("Why is the column table null?");
            }

            String alias;
            if (col.getTable().getAlias() != null && col.getTable().getAlias().getName() != null) {
                alias = col.getTable().getAlias().getName();
            } else {
                alias = col.getTable().getName();
            }

            String attrName = col.getColumnName();

            String value = this.getRightExpression().toString();

            String relName = col.getTable().getName();
            Relation rel = unroller.getRelations().get(relName);
            if (rel == null) throw new RuntimeException("Could not find relation: " + relName);

            Attribute attrObj = rel.getAttributes().get(attrName);
            if (attrObj == null) throw new RuntimeException("Could not find attribute: " + attrName + " in relation: " + relName);

            Predicate pred = new Predicate(attrObj, null, value);
            pred.setAlias(alias);
            unroller.getPredicates().add(pred);
        }
    }
}
