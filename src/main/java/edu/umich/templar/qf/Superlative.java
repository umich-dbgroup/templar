package edu.umich.templar.qf;

import edu.umich.templar.dataStructure.ParseTreeNode;
import edu.umich.templar.rdbms.Attribute;

/**
 * Created by cjbaik on 9/27/17.
 */
public class Superlative extends QueryFragment {
    boolean desc;   // true if descending

    String function;        // Function, if any

    public Superlative(Attribute attribute, String function, boolean desc) {
        super();
        this.attribute = attribute;
        this.function = function;
        this.desc = desc;
    }

    public Superlative(ParseTreeNode node, Attribute attribute, String function, boolean desc) {
        super(node);
        this.attribute = attribute;
        this.function = function;
        this.desc = desc;
    }

    public Superlative(Superlative other) {
        super();
        this.node = other.node;
        this.attribute = new Attribute(other.attribute);
        this.attribute.setRelation(other.attribute.getRelation());
        this.function = other.function;
        this.desc = other.desc;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public boolean isDesc() {
        return desc;
    }

    public boolean covers(Superlative other) {
        if (this.equals(other)) return true;

        if (this.getAttribute().equals(other.getAttribute())) {
            return true;
        }

        return false;
    }

    public void applyAggregateFunction() {
        if (this.attribute.getType().equals("int")) {
            this.function = "sum";
        } else {
            this.function = "count";
        }
    }

    public String toStringWithConsistentRelation() {
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
        sb.append(this.attribute.toStringWithConsistentRelation());
        if (this.function != null && !countingIntAttr) {
            sb.append(")");
            if (isCount) {
                sb.append(")");
            }
        }

        if (this.desc) {
            sb.append(" desc");
        } else {
            sb.append(" asc");
        }

        sb.append(" limit 1");

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
        sb.append(this.attribute.toString());
        if (this.function != null && !countingIntAttr) {
            sb.append(")");
            if (isCount) {
                sb.append(")");
            }
        }

        if (this.desc) {
            sb.append(" desc");
        } else {
            sb.append(" asc");
        }

        sb.append(" limit 1");

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Superlative that = (Superlative) o;

        if (desc != that.desc) return false;
        if (function != null ? !function.equals(that.function) : that.function != null) return false;
        return !(attribute != null ? !attribute.equals(that.attribute) : that.attribute != null);

    }

    @Override
    public int hashCode() {
        int result = function != null ? function.hashCode() : 0;
        result = 31 * result + (attribute != null ? attribute.hashCode() : 0);
        result = 31 * result + (desc ? 1 : 0);
        return result;
    }

}
