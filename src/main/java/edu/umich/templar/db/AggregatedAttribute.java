package edu.umich.templar.db;

import java.io.Serializable;
import java.util.Objects;

public class AggregatedAttribute extends DBElement implements Serializable {
    String function;
    Attribute attr;

    public AggregatedAttribute(String function, Attribute attr) {
        this.function = function;
        this.attr = attr;
    }

    public String getFunction() {
        return function;
    }

    public Attribute getAttr() {
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
        AggregatedAttribute that = (AggregatedAttribute) o;
        return Objects.equals(function, that.function) &&
                Objects.equals(attr, that.attr);
    }

    @Override
    public int hashCode() {

        return Objects.hash(function, attr);
    }

    @Override
    public String toString() {
        return this.function + "(" + this.attr + ")";
    }
}
