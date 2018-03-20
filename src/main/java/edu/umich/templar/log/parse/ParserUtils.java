package edu.umich.templar.log.parse;

import edu.umich.templar.db.Attribute;
import edu.umich.templar.db.Database;
import edu.umich.templar.db.Relation;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.schema.Column;

import java.util.Set;

public class ParserUtils {
    public static Attribute getAttributeFromColumn(Database db, Set<Relation> queryRelations, Column column) {
        Relation rel = null;
        Attribute attr = null;
        if (column.getTable().getName() != null) {
            String tableName = Utils.removeAliasIntFromAlias(column.getTable().getName().trim().toLowerCase());
            rel = db.getRelationByName(tableName);

            if (rel == null) throw new RuntimeException("Relation " + column.getTable().getName() + " not found!");

            attr = rel.getAttribute(column.getColumnName().toLowerCase());
        } else {
            for (Relation qr : queryRelations) {
                attr = qr.getAttribute(column.getColumnName());
                if (attr != null) break;
            }
        }

        if (attr == null) throw new RuntimeException("Attribute " + column.getColumnName() + " not found!");
        return attr;
    }
}
