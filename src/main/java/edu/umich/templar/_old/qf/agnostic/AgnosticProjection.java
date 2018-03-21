package edu.umich.templar._old.qf.agnostic;

import edu.umich.templar._old.qf.pieces.AttributeType;
import edu.umich.templar._old.qf.pieces.QFFunction;

/**
 * Created by cjbaik on 10/16/17.
 */
public class AgnosticProjection extends AgnosticQueryFragment {
    AttributeType type;
    QFFunction function;

    public AgnosticProjection(AttributeType type, QFFunction function) {
        super();
        this.type = type;
        this.function = function;
    }

    public AttributeType getType() {
        return type;
    }

    public void setType(AttributeType type) {
        this.type = type;
    }

    public QFFunction getFunction() {
        return function;
    }

    public void setFunction(QFFunction function) {
        this.function = function;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("proj[");

        if (this.function != null) {
            sb.append(this.function.toString());
            sb.append('(');
        }

        sb.append(this.type.toString());

        if (this.function != null) {
            sb.append(')');
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AgnosticProjection that = (AgnosticProjection) o;

        if (type != that.type) return false;
        return function == that.function;

    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (function != null ? function.hashCode() : 0);
        return result;
    }
}
