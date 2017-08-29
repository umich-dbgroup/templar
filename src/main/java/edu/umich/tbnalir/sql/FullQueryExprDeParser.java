package edu.umich.tbnalir.sql;

import edu.umich.tbnalir.rdbms.Relation;
import edu.umich.tbnalir.util.Utils;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

import java.util.List;
import java.util.Map;

/**
 * Created by cjbaik on 8/29/17.
 */
public class FullQueryExprDeParser extends ExpressionDeParser {
    Map<String, Relation> relations; // Relations registered globally

    Map<String, Table> tables;
    Map<String, List<Alias>> aliases; // Table names to aliases

    public FullQueryExprDeParser() {
        this.tables = null;
        this.relations = null;
        this.aliases = null;
    }

    public void setTables(Map<String, Table> tables) {
        this.tables = tables;
    }

    public void setRelations(Map<String, Relation> relations) {
        this.relations = relations;
    }

    public void setAliases(Map<String, List<Alias>> aliases) {
        this.aliases = aliases;
    }

    @Override
    public void visit(Column col) {
        Table table = Utils.findTableForColumn(this.tables, this.relations, col);

        if (table == null) throw new RuntimeException("Could not find table for column: " + col.getColumnName());

        List<Alias> tableAliases = this.aliases.get(table.getName());
        if (tableAliases.size() > 1) {
            throw new RuntimeException("WARNING: More than 1 alias for table! Which to select?");
        }
        table.setAlias(tableAliases.get(0));
        col.setTable(table);

        this.getBuffer().append(col.getName(true));
    }
}
