package edu.umich.templar._old.sqlparse.agnostic;

import edu.umich.templar._old.qf.agnostic.AgnosticProjection;
import edu.umich.templar._old.qf.pieces.AttributeType;
import edu.umich.templar._old.qf.pieces.QFFunction;
import edu.umich.templar._old.rdbms.Attribute;
import edu.umich.templar._old.rdbms.Relation;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.AllColumns;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by cjbaik on 10/16/17.
 */
public class AgnosticProjectionUnroller extends ExpressionVisitorAdapter {
    Map<String, Relation> relations;
    List<Relation> queryRelations;
    List<AgnosticProjection> projections;

    public AgnosticProjectionUnroller(Map<String, Relation> relations, List<Relation> queryRelations) {
        this.relations = relations;
        this.queryRelations = queryRelations;
        this.projections = new ArrayList<>();
    }

    public List<AgnosticProjection> getProjections() {
        return projections;
    }

    @Override
    public void visit(Function function) {
        String functionName = function.getName();

        // In the case that it's "all columns"
        if (function.isAllColumns()) {
            this.projections.add(new AgnosticProjection(AttributeType.ALL, QFFunction.getFunction(functionName)));
        } else {
            Column col = Utils.getColumnFromFunction(function);

            if (col != null) {
                Attribute attr = Utils.getAttributeFromColumn(this.relations, this.queryRelations, col);
                if (attr != null) {
                    this.projections.add(new AgnosticProjection(attr.getAttributeType(), QFFunction.getFunction(functionName)));
                }
            }
        }
    }

    @Override
    public void visit(AllColumns allColumns) {
        this.projections.add(new AgnosticProjection(AttributeType.ALL, null));
    }

    @Override
    public void visit(Column column) {
        if (column.getTable().getName() != null) {
            Attribute attr = Utils.getAttributeFromColumn(this.relations, this.queryRelations, column);
            if (attr != null) {
                this.projections.add(new AgnosticProjection(attr.getAttributeType(), null));
            }
        }
    }
}
