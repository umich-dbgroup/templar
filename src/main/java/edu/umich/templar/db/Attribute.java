package edu.umich.templar.db;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by cjbaik on 1/31/18.
 */
public class Attribute extends DBElement implements Serializable {
    Relation relation;
    AttributeType type;
    Integer textLength;
    String name;

    public Attribute(Relation rel, String type, String name) {
        this.relation = rel;

        if (type.equalsIgnoreCase("varchar") || type.equalsIgnoreCase("text") || type.equalsIgnoreCase("char")) {
            this.type = AttributeType.TEXT;
        } else if (type.toLowerCase().startsWith("int") || type.equalsIgnoreCase("integer")) {
            this.type = AttributeType.INT;
        } else if (type.equalsIgnoreCase("double") || type.equalsIgnoreCase("float")) {
            this.type = AttributeType.DOUBLE;
        } else if (type.equalsIgnoreCase("time")) {
            this.type = AttributeType.TIME;
        } else if (type.equalsIgnoreCase("timestamp")) {
            this.type = AttributeType.TIMESTAMP;
        } else {
            throw new RuntimeException("Unrecognized type: " + type + " for " + rel.getName() + "." + name);
        }

        this.name = name;
    }

    public Relation getRelation() {
        return relation;
    }

    public AttributeType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getCleanedName() {
        return this.name.replaceAll("_", " ");
    }

    public List<String> getNameTokens() {
        List<String> result = new ArrayList<>();
        Collections.addAll(result, this.name.toLowerCase().split("_"));
        return result;
    }

    public Column toColumn() {
        Column col = new Column(this.name);
        col.setTable(new Table(this.relation.getName()));
        return col;
    }

    public Integer getTextLength() {
        return textLength;
    }

    public void setTextLength(Integer textLength) {
        this.textLength = textLength;
    }

    @Override
    public String toString() {
        return this.relation.getName() + "." + this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute attribute = (Attribute) o;

        if (relation != null ? !relation.equals(attribute.relation) : attribute.relation != null) return false;
        if (type != attribute.type) return false;
        return !(name != null ? !name.equals(attribute.name) : attribute.name != null);

    }

    @Override
    public int hashCode() {
        int result = relation != null ? relation.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
