package edu.umich.templar.qf.agnostic;

import edu.umich.templar.qf.pieces.AttributeType;
import edu.umich.templar.qf.pieces.Operator;
import edu.umich.templar.qf.pieces.QFFunction;

/**
 * Created by cjbaik on 10/16/17.
 */
public class AgnosticHaving extends AgnosticQueryFragment {
    AttributeType type;
    QFFunction function;
    Operator op;

    public AgnosticHaving(AttributeType type, QFFunction function, Operator op) {
        super();
        this.type = type;
        this.function = function;
        this.op = op;
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

    public Operator getOp() {
        return op;
    }

    public void setOp(Operator op) {
        this.op = op;
    }

    @Override
    public String toString() {
        return "having[" + this.function.toString() + "(" + this.type.toString() + ")" + " " + this.op.toString() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AgnosticHaving that = (AgnosticHaving) o;

        if (type != that.type) return false;
        if (function != that.function) return false;
        return op == that.op;

    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (function != null ? function.hashCode() : 0);
        result = 31 * result + (op != null ? op.hashCode() : 0);
        return result;
    }
}
