package edu.umich.templar.qf;

import edu.umich.templar.rdbms.Attribute;
import edu.umich.templar.qf.pieces.Operator;
import edu.umich.templar.util.Constants;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * Created by cjbaik on 9/11/17.
 */
public class Predicate extends QueryFragment {
    Operator op;
    String value;

    public Predicate(Attribute attribute, Operator op, String value) {
        super();
        this.attribute = attribute;
        this.op = op;
        this.value = value;
    }

    public Predicate(Predicate other) {
        super();
        this.attribute = new Attribute(other.attribute);
        this.attribute.setRelation(other.attribute.getRelation());
        this.op = other.op;
        this.value = other.value;
    }

    public Operator getOp() {
        return op;
    }

    public void setOp(Operator op) {
        this.op = op;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String toStringWithConsistentRelation() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.attribute.getRelation().getName());
        sb.append(".");
        sb.append(this.attribute.getName());
        if (this.op != null) {
            sb.append(this.op);
        } else {
            sb.append("o?");
        }
        if (this.value != null) {
            sb.append(this.value);
        } else {
            sb.append("v?");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (this.attribute != null) {
            sb.append(this.attribute.toString());
        } else {
            sb.append(Constants.COLUMN);
        }
        sb.append(" ");

        if (op == null) {
            sb.append(Constants.CMP);
        } else {
            sb.append(op.toString());
        }
        sb.append(" ");

        if (!NumberUtils.isCreatable(this.value)) {
            sb.append("\"");
            sb.append(this.value);
            sb.append("\"");
        } else {
            sb.append(this.value);
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Predicate predicate = (Predicate) o;

        if (attribute != null ? !attribute.equals(predicate.attribute) : predicate.attribute != null) return false;
        if (op != predicate.op) return false;
        return !(value != null ? !value.equals(predicate.value) : predicate.value != null);

    }

    @Override
    public int hashCode() {
        int result = attribute != null ? attribute.hashCode() : 0;
        result = 31 * result + (op != null ? op.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
