package edu.umich.templar.qf.agnostic;

import edu.umich.templar.qf.pieces.AttributeType;

/**
 * Created by cjbaik on 10/16/17.
 */
public class AgnosticGroupBy extends AgnosticQueryFragment {
    AttributeType type;

    public AgnosticGroupBy(AttributeType type) {
        super();
        this.type = type;
    }

    public AttributeType getType() {
        return type;
    }

    public void setType(AttributeType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "group[" + this.type.toString() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AgnosticGroupBy that = (AgnosticGroupBy) o;

        return type == that.type;

    }

    @Override
    public int hashCode() {
        return type != null ? type.hashCode() : 0;
    }
}
