package edu.umich.templar.db;

import java.util.Objects;

public class GroupedAttribute extends DBElement {
    private Attribute attr;

    public GroupedAttribute(Attribute attr) {
        this.attr = attr;
    }

    public Attribute getAttr() {
        return attr;
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
