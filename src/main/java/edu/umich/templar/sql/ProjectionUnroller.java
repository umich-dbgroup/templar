package edu.umich.templar.sql;

import edu.umich.templar.parse.Projection;
import edu.umich.templar.rdbms.Attribute;
import edu.umich.templar.rdbms.Relation;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.schema.Column;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by cjbaik on 10/10/17.
 */
public class ProjectionUnroller extends ExpressionVisitorAdapter {
    Map<String, Relation> relations;
    List<Projection> projections;

    public ProjectionUnroller(Map<String, Relation> relations) {
        this.relations = relations;
        this.projections = new ArrayList<>();
    }

    public List<Projection> getProjections() {
        return projections;
    }

    @Override
    public void visit(Function function) {
        String functionName = function.getName();
        Column col = null;
        if (function.getParameters() != null && function.getParameters().getExpressions() != null) {
            for (Expression expr : function.getParameters().getExpressions()) {
                if (expr instanceof Parenthesis) {
                    expr = ((Parenthesis) expr).getExpression();
                }
                if (expr instanceof Column) {
                    col = (Column) expr;
                    break;
                }
            }
        }

        if (col != null) {
            Attribute attr = Utils.getAttributeFromColumn(this.relations, col);
            this.projections.add(new Projection(attr, functionName.toLowerCase(), null));
        }
    }

    @Override
    public void visit(Column column) {
        Attribute attr = Utils.getAttributeFromColumn(relations, column);
        this.projections.add(new Projection(attr, null, null));
    }
}
