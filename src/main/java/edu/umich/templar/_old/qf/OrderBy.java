package edu.umich.templar._old.qf;

import edu.umich.templar._old.rdbms.Attribute;

/**
 * Created by cjbaik on 10/18/17.
 */
public class OrderBy extends QueryFragment {
    String function;
    boolean desc;

    public OrderBy(Attribute attr, String function, boolean desc) {
        super();
        this.attribute = attr;
        this.function = function;
        this.desc = desc;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("order[");
        if (this.function != null) {
            sb.append(this.function);
            sb.append('(');
        }

        sb.append(this.attribute);

        if (this.function != null) {
            sb.append(')');
        }

        if (this.desc) {
            sb.append(":desc");
        } else {
            sb.append(":asc");
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OrderBy orderBy = (OrderBy) o;

        if (desc != orderBy.desc) return false;
        if (attribute != null ? !attribute.equals(orderBy.attribute) : orderBy.attribute != null) return false;
        return !(function != null ? !function.equals(orderBy.function) : orderBy.function != null);

    }

    @Override
    public int hashCode() {
        int result = function != null ? function.hashCode() : 0;
        result = 31 * result + (attribute != null ? attribute.hashCode() : 0);
        result = 31 * result + (desc ? 1 : 0);
        return result;
    }
}
