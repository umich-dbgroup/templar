package edu.umich.templar.db.el;

import java.io.Serializable;
import java.util.Objects;

public class AggregatedAttribute extends DBElement implements Serializable {
    String valueFunction;
    String aggrFunction;
    Attribute attr;

    public AggregatedAttribute(String valueFunction, String aggrFunction, Attribute attr) {
        this.valueFunction = valueFunction;
        this.aggrFunction = aggrFunction;
        this.attr = attr;
    }

    public String getValueFunction() {
        return valueFunction;
    }

    public String getAggrFunction() {
        return aggrFunction;
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
        return Objects.equals(valueFunction, that.valueFunction) &&
                Objects.equals(aggrFunction, that.aggrFunction) &&
                Objects.equals(attr, that.attr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueFunction, aggrFunction, attr);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.valueFunction != null) {
            sb.append(this.valueFunction);
            sb.append('(');
        }
        if (this.aggrFunction != null) {
            sb.append(this.aggrFunction);
            sb.append('(');
        }
        sb.append(this.attr);
        if (this.aggrFunction != null) {
            sb.append(')');
        }
        if (this.valueFunction != null) {
            sb.append(')');
        }
        return sb.toString();
    }
}
