package edu.umich.templar.log.parse;

import edu.umich.templar.db.*;
import edu.umich.templar.db.el.*;
import net.sf.jsqlparser.expression.*;
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

    private String currentFunction;
    private Attribute currentColumn;

    public PredicateParser(Database db, Set<Relation> queryRelations) {
        this.db = db;
        this.queryRelations = queryRelations;
        this.predicates = new ArrayList<>();

        this.currentColumn = null;
        this.currentFunction = null;
    }

    public List<DBElement> getPredicates() {
        return predicates;
    }

    private void visitPredicate(BinaryExpression expr, String operator) {
        // Make sure left is column, right is NOT column (otherwise it is join predicate)
        boolean leftIsColumnOrFunction = expr.getLeftExpression() instanceof Column ||
                expr.getLeftExpression() instanceof Function;
        boolean rightIsColumn = (expr.getRightExpression() instanceof Column) &&
                ((Column) expr.getRightExpression()).getTable().getName() != null;

        if (leftIsColumnOrFunction && !rightIsColumn) {
            expr.getLeftExpression().accept(this);

            String value = expr.getRightExpression().toString();
            if (value.isEmpty()) {
                value = null;
            } else {
                // remove beginning and ending quotes
                value = value.replaceAll("^\"|\"$", "");
            }

            if (StringUtils.isNumeric(value)) {
                this.predicates.add(new NumericPredicate(this.currentColumn, operator, Double.valueOf(value),
                        this.currentFunction));

            } else {
                this.predicates.add(new TextPredicate(this.currentColumn, value));
            }

            this.currentColumn = null;
            this.currentFunction = null;
        }
    }

    @Override
    public void visit(Column column) {
        if (this.currentColumn != null) throw new RuntimeException("currentColumn already set!");
        this.currentColumn = ParserUtils.getAttributeFromColumn(this.db, this.queryRelations, column);
    }

    @Override
    public void visit(Function function) {
        if (!function.getName().equalsIgnoreCase("distinct")) {
            if (this.currentFunction != null) throw new RuntimeException("Function already set!");
            this.currentFunction = function.getName();
        }

        // Assume only one parameter for functions
        function.getParameters().getExpressions().get(0).accept(this);
    }

    @Override
    public void visit(Parenthesis parenthesis) {
        parenthesis.getExpression().accept(this);
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
