package edu.umich.templar.qf.agnostic;

import edu.umich.templar.qf.pieces.AttributeType;
import edu.umich.templar.qf.pieces.QFFunction;

/**
 * Created by cjbaik on 10/16/17.
 */
public class AgnosticOrderBy extends AgnosticQueryFragment {
    AttributeType type;
    QFFunction function;
    boolean desc;

    public AgnosticOrderBy(AttributeType type, QFFunction function, boolean desc) {
        super();
        this.type = type;
        this.function = function;
        this.desc = desc;
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

        if (desc != that.desc) return false;
        if (type != that.type) return false;
        return function == that.function;

    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (function != null ? function.hashCode() : 0);
        result = 31 * result + (desc ? 1 : 0);
        return result;
    }
}
