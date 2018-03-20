package edu.umich.templar.db;

import java.util.Objects;

public class GroupedAttribute {
    private Attribute attr;

    public GroupedAttribute(Attribute attr) {
        this.attr = attr;
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
}
