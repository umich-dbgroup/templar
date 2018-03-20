package edu.umich.templar.log.parse;

import edu.umich.templar.db.Attribute;
import edu.umich.templar.db.Database;
import edu.umich.templar.db.Relation;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.AllColumns;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProjectionParser extends ExpressionVisitorAdapter {
    private Database db;
    private Set<Relation> queryRelations;
    private List<Attribute> attributes;

    public ProjectionParser(Database db, Set<Relation> queryRelations) {
        this.db = db;
        this.queryRelations = queryRelations;
        this.attributes = new ArrayList<>();
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    @Override
    public void visit(Function function) {
        String functionName = function.getName();

        // In the case that it's "all columns"
        if (function.isAllColumns()) {
            this.attributes.add(Attribute.allColumnsAttr());
        } else {
            Column col = Utils.getColumnFromFunction(function);

            if (col != null) {
                Attribute attr = ParserUtils.getAttributeFromColumn(this.db, this.queryRelations, col);
                this.attributes.add(attr);
            }
        }
    }

    @Override
    public void visit(AllColumns allColumns) {
        this.attributes.add(Attribute.allColumnsAttr());
    }

    @Override
    public void visit(Column column) {
        Attribute attr = ParserUtils.getAttributeFromColumn(this.db, this.queryRelations, column);
        this.attributes.add(attr);
    }
}
