package edu.umich.templar.log.parse;

import edu.umich.templar.db.*;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PredicateParser extends ExpressionVisitorAdapter {
    private Database db;
    private Set<Relation> queryRelations;
    private List<DBElement> predicates;

    public PredicateParser(Database db, Set<Relation> queryRelations) {
        this.db = db;
        this.queryRelations = queryRelations;
        this.predicates = new ArrayList<>();
    }

    public List<DBElement> getPredicates() {
        return predicates;
    }

    public void visitPredicate(BinaryExpression expr, String operator) {
        // Make sure left is column, right is NOT column (otherwise it is join predicate)
        boolean leftIsColumn = expr.getLeftExpression() instanceof Column;
        boolean rightIsColumn = (expr.getRightExpression() instanceof Column) &&
                ((Column) expr.getRightExpression()).getTable().getName() != null;

        if (leftIsColumn && !rightIsColumn) {
            Column column = (Column) expr.getLeftExpression();

            String value = expr.getRightExpression().toString();
            if (value.isEmpty()) {
                value = null;
            } else {
                // remove beginning and ending quotes
                value = value.replaceAll("^\"|\"$", "");
            }

            Attribute attr = ParserUtils.getAttributeFromColumn(this.db, this.queryRelations, column);

            if (StringUtils.isNumeric(value)) {
                this.predicates.add(new NumericPredicate(attr, operator, Double.valueOf(value), null));
            } else {
                this.predicates.add(new TextPredicate(attr, value));
            }
        }
    }

    @Override
    public void visit(EqualsTo expr) {
        this.visitPredicate(expr, "=");
    }

    @Override
    public void visit(GreaterThan expr) {
        this.visitPredicate(expr, ">");
    }

    @Override
    public void visit(GreaterThanEquals expr) {
        this.visitPredicate(expr, ">=");
    }

    @Override
    public void visit(MinorThan expr) {
        this.visitPredicate(expr, "<");
    }

    @Override
    public void visit(MinorThanEquals expr) {
        this.visitPredicate(expr, "<=");
    }

    @Override
    public void visit(NotEqualsTo expr) {
        this.visitPredicate(expr, "<>");
    }
}
