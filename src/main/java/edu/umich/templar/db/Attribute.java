package edu.umich.templar.db;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Created by cjbaik on 1/31/18.
 */
public class Attribute extends DBElement implements Serializable {
    private Relation relation;
    private AttributeType type;
    private Integer textLength;
    private String name;

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
        } else if (type.equalsIgnoreCase("*")) {
            this.type = AttributeType.ALL_COLUMNS;
        } else {
            throw new RuntimeException("Unrecognized type: " + type + " for " + rel.getName() + "." + name);
        }

        this.name = name;
    }

    @Override
    public Relation getRelation() {
        return relation;
    }

    public static Attribute allColumnsAttr() {
        return new Attribute(null, "*", "*");
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

    public boolean isMainAttr() {
        if (this.relation != null && this.relation.getMainAttribute() != null) {
            return this.relation.getMainAttribute().equals(this);
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.relation != null) {
            sb.append(this.relation.getName());
            sb.append('.');
        }
        sb.append(this.name);
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attribute attribute = (Attribute) o;
        return Objects.equals(relation, attribute.relation) &&
                type == attribute.type &&
                Objects.equals(name, attribute.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relation, type, name);
    }
}
