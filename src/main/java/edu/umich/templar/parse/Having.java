package edu.umich.templar.parse;

import edu.umich.templar.rdbms.Attribute;
import edu.umich.templar.sql.Operator;
import edu.umich.templar.util.Constants;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * Created by cjbaik on 9/27/17.
 */
public class Having extends QueryFragment {
    Operator op;
    String value;

    String function;   // function, like "count"

    public Having(Attribute attribute, Operator op, String value, String function) {
        this.attribute = attribute;
        this.op = op;
        this.value = value;
        this.function = function;
    }

    public Having(Having other) {
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
}
