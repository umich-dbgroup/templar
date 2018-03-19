package edu.umich.templar.db;

import net.sf.jsqlparser.schema.Table;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cjbaik on 1/31/18.
 */
public class Relation extends DBElement implements Serializable {
    String name;
    List<Attribute> attributes;
    Attribute mainAttribute;

    public Relation(String name) {
        this.name = name;
        this.attributes = new ArrayList<>();
        this.mainAttribute = null;
    }

    public void addAttribute(Attribute attr) {
        this.attributes.add(attr);
    }

    public String getName() {
        return name;
    }

    public List<Attribute> getAttributes() {
        return this.attributes;
    }

    public Attribute getMainAttribute() {
        return mainAttribute;
    }

    public void setMainAttribute(String attrName) {
        for (Attribute attr : this.attributes) {
            if (attrName.equalsIgnoreCase(attr.getName())) {
                this.mainAttribute = attr;
                return;
            }
        }

        throw new RuntimeException("Could not find attribute with name <"
                + attrName + "> in relation <" + this.getName() + ">");
    }

    public Table toTable() {
        return new Table(this.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Relation relation = (Relation) o;

        return name.equals(relation.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return this.name;
    }
}
