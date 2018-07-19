package edu.umich.templar.db.el;

import java.io.Serializable;
import java.util.Objects;

public class GroupedAttribute extends DBElement implements Serializable {
    private Attribute attr;

    public GroupedAttribute(Attribute attr) {
        this.attr = attr;
    }

    @Override
    public Attribute getAttribute() {
        return attr;
    }

    @Override
    public Relation getRelation() {
        return this.attr.getRelation();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupedAttribute that = (GroupedAttribute) o;
        return Objects.equals(attr, that.attr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attr);
    }

    @Override
    public String toString() {
        return this.attr.toString();
    }
}
