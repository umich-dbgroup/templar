package edu.umich.tbnalir.sql;

import edu.umich.tbnalir.rdbms.Attribute;
import edu.umich.tbnalir.util.Constants;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by cjbaik on 9/11/17.
 */
public class Predicate {
    String alias; // If attribute relation has an alias.
    Attribute attr;
    Operator op;
    String value;

    public Predicate(Attribute attr, Operator op, String value) {
        this.attr = attr;
        this.op = op;
        this.value = value;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Attribute getAttr() {
        return attr;
    }

    public void setAttr(Attribute attr) {
        this.attr = attr;
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

        if (this.attr != null) {
            if (this.alias != null) {
                sb.append(this.alias);
            } else {
                sb.append(this.attr.getRelation().getName());
            }
            sb.append(".");
            sb.append(this.attr.getName());
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

        if (!StringUtils.isNumeric(this.value)) {
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

        if (attr != null ? !attr.equals(predicate.attr) : predicate.attr != null) return false;
        if (op != predicate.op) return false;
        return !(value != null ? !value.equals(predicate.value) : predicate.value != null);

    }

    @Override
    public int hashCode() {
        int result = attr != null ? attr.hashCode() : 0;
        result = 31 * result + (op != null ? op.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
