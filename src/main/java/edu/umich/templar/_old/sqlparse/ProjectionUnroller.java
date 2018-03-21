package edu.umich.templar._old.sqlparse;

import edu.umich.templar._old.qf.Projection;
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
 * Created by cjbaik on 10/10/17.
 */
public class ProjectionUnroller extends ExpressionVisitorAdapter {
    Map<String, Relation> relations;
    List<Relation> queryRelations;
    List<Projection> projections;

    public ProjectionUnroller(Map<String, Relation> relations, List<Relation> queryRelations) {
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
            }
        }
    }

    @Override
    public void visit(AllColumns allColumns) {
        this.projections.add(new Projection(Attribute.allColumnsAttr(), null, null));
    }

    @Override
    public void visit(Column column) {
        if (column.getTable().getName() != null) {
            Attribute attr = Utils.getAttributeFromColumn(this.relations, this.queryRelations, column);
            if (attr != null) {
                this.projections.add(new Projection(attr, null, null));
            }
        }
    }
}
