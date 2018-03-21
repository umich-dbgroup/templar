package edu.umich.templar._old.qf;

import edu.umich.templar._old.dataStructure.ParseTreeNode;
import edu.umich.templar._old.rdbms.Attribute;
import edu.umich.templar._old.qf.pieces.Operator;
import edu.umich.templar._old.Constants;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * Created by cjbaik on 9/27/17.
 */
public class Having extends QueryFragment {
    Operator op;
    String value;

    String function;   // function, like "count"

    public Having(Attribute attribute, Operator op, String value, String function) {
        super();
        this.attribute = attribute;
        this.op = op;
        this.value = value;
        this.function = function;
    }

    public Having(ParseTreeNode node, Attribute attribute, Operator op, String value, String function) {
        super(node);
        this.attribute = attribute;
        this.op = op;
        this.value = value;
        this.function = function;
    }

    public Having(Having other) {
        super();
        this.node = other.node;
        this.attribute = new Attribute(other.attribute);
        this.attribute.setRelation(other.attribute.getRelation());
        this.op = other.op;
        this.value = other.value;
        this.function = other.function;
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

    public String getFunction() {
        return function;
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
        sb.append(":");
        if (this.function != null) {
            sb.append(this.function);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        boolean isCount = this.function != null && this.function.equalsIgnoreCase("count");

        boolean countingIntAttr = this.function != null && this.function.equalsIgnoreCase("count")
                && this.attribute.getType().equals("int");

        if (this.function != null && !countingIntAttr) {
            sb.append(this.function);
            sb.append("(");
            if (isCount) {
                sb.append("distinct(");
            }
        }
        if (this.attribute != null) {
            sb.append(this.attribute.toString());
        } else {
            sb.append(Constants.COLUMN);
        }
        if (this.function != null && !countingIntAttr) {
            sb.append(")");
            if (isCount) {
                sb.append(")");
            }
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

        Having having = (Having) o;

        if (op != having.op) return false;
        if (value != null ? !value.equals(having.value) : having.value != null) return false;
        if (attribute != null ? !attribute.equals(having.attribute) : having.attribute != null) return false;
        return !(function != null ? !function.equals(having.function) : having.function != null);

    }

    @Override
    public int hashCode() {
        int result = op != null ? op.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (attribute != null ? attribute.hashCode() : 0);
        result = 31 * result + (function != null ? function.hashCode() : 0);
        return result;
    }
}
