package edu.umich.templar.qf.agnostic;

import edu.umich.templar.qf.pieces.AttributeType;
import edu.umich.templar.qf.pieces.QFFunction;

/**
 * Created by cjbaik on 10/16/17.
 */
public class AgnosticOrderBy extends AgnosticQueryFragment {
    AttributeType type;
    QFFunction function;

    public AgnosticOrderBy(AttributeType type, QFFunction function) {
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
        sb.append("order[");

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

        AgnosticOrderBy that = (AgnosticOrderBy) o;

        return type == that.type;

    }

    @Override
    public int hashCode() {
        return type != null ? type.hashCode() : 0;
    }
}
