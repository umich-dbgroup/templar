package edu.umich.templar.qf.agnostic;

import edu.umich.templar.qf.pieces.AttributeType;
import edu.umich.templar.qf.pieces.Operator;

/**
 * Created by cjbaik on 10/16/17.
 */
public class AgnosticPredicate extends AgnosticQueryFragment {
    AttributeType type;
    Operator op;

    public AgnosticPredicate(AttributeType type, Operator op) {
        super();
        this.type = type;
        this.op = op;
    }

    public AttributeType getType() {
        return type;
    }

    public void setType(AttributeType type) {
        this.type = type;
    }

    public Operator getOp() {
        return op;
    }

    public void setOp(Operator op) {
        this.op = op;
    }

    @Override
    public String toString() {
        return "pred[" + this.type.toString() + " " + this.op.toString() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AgnosticPredicate that = (AgnosticPredicate) o;

        if (type != that.type) return false;
        return op == that.op;

    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (op != null ? op.hashCode() : 0);
        return result;
    }
}
