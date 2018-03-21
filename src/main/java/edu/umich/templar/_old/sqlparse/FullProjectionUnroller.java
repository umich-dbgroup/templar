package edu.umich.templar._old.sqlparse;

import edu.umich.templar._old.qf.Projection;
import edu.umich.templar._old.rdbms.Attribute;
import edu.umich.templar._old.rdbms.Relation;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Created by cjbaik on 10/10/17.
 */
public class FullProjectionUnroller extends ExpressionVisitorAdapter {
    Map<String, Relation> relations;
    List<Relation> queryRelations;
    List<Projection> projections;

    public FullProjectionUnroller(Map<String, Relation> relations, List<Relation> queryRelations) {
        this.relations = relations;
        this.queryRelations = queryRelations;
        this.projections = new ArrayList<>();
    }

    public List<Projection> getProjections() {
        return projections;
    }

    @Override
    public void visit(Function function) {
        String functionName = function.getName();

        // In the case that it's "all columns"
        if (function.isAllColumns()) {
            this.projections.add(new Projection(Attribute.allColumnsAttr(), functionName, null));
        } else {
            Column col = Utils.getColumnFromFunction(function);

            if (col != null) {
                Attribute attr = Utils.getAttributeFromColumn(this.relations, this.queryRelations, col);
                if (attr != null) {
                    this.projections.add(new Projection(attr, functionName.toLowerCase(), null));
                }
            } else {
                StringJoiner params = new StringJoiner(",");
                for (Expression expr : function.getParameters().getExpressions()) {
                    FullQueryExprDeParser deParser = new FullQueryExprDeParser();
                    expr.accept(deParser);
                    params.add(deParser.getBuffer().toString());
                }
                this.projections.add(new Projection(new Attribute(params.toString(), "params"), functionName.toLowerCase(), null));
            }
        }
    }

    @Override
    public void visit(AllColumns allColumns) {
        this.projections.add(new Projection(Attribute.allColumnsAttr(), null, null));
    }

    @Override
    public void visit(Column column) {
        boolean found = false;
        try {
            if (column.getTable().getName() != null) {
                Attribute attr = Utils.getAttributeFromColumn(this.relations, this.queryRelations, column);
                if (attr != null) {
                    this.projections.add(new Projection(attr, null, null));
                    found = true;
                }
            }
        } catch (Exception e) {
            // silent failure
        }

        if (!found) {
            this.projections.add(new Projection(new Attribute(column.toString(), "unknown"), null, null));
        }
    }

    @Override
    public void visit(SelectExpressionItem selectExpressionItem) {
        Expression expr = selectExpressionItem.getExpression();
        if (expr instanceof Function || expr instanceof Column) {
            expr.accept(this);
        } else {
            FullQueryExprDeParser deParser = new FullQueryExprDeParser();
            expr.accept(deParser);
            this.projections.add(new Projection(new Attribute(deParser.getBuffer().toString(), "expr"), null, null));
        }
    }
}
