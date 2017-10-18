package edu.umich.templar.qf;

import edu.umich.templar.rdbms.Attribute;

/**
 * Created by cjbaik on 9/12/17.
 */
public class Projection extends QueryFragment {
    String function;        // Function, if any

    boolean groupBy;        // If we want to GROUP BY this projection, set true

    public Projection(Attribute attribute, String function, String qt) {
        super();
        this.attribute = attribute;
        this.function = function;

        this.groupBy = qt != null && qt.equals("each");
    }

    public Projection(Projection other) {
        super();
        this.attribute = new Attribute(other.attribute);
        this.attribute.setRelation(other.attribute.getRelation());

        this.function = other.function;
        this.groupBy = other.groupBy;
    }

    public boolean isGroupBy() {
        return groupBy;
    }

    public void setGroupBy(boolean groupBy) {
        this.groupBy = groupBy;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public boolean covers(Projection other) {
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
        String function = this.function == null ? "" : this.function;
        String groupByStr = this.groupBy ? "group" : "";
        return this.attribute.getRelation().getName() + "." + this.attribute.getName()
                + ":" + function + ":" + groupByStr;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // TODO: explore this more, but are all COUNT functions going to be DISTINCT?
        boolean isCount = this.function != null && this.function.equalsIgnoreCase("count");

        boolean countingIntAttr = this.function != null && this.function.equalsIgnoreCase("count")
                && this.attribute.getType().equals("int");

        if (this.function != null && !countingIntAttr) {
            sb.append(this.function);
            sb.append("(");
            if (isCount && !this.attribute.getType().equals("*")) {
                sb.append("distinct(");
            }
        }
        sb.append(this.attribute.toString());
        if (this.function != null && !countingIntAttr) {
            sb.append(")");
            if (isCount && !this.attribute.getType().equals("*")) {
                sb.append(")");
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Projection that = (Projection) o;

        if (groupBy != that.groupBy) return false;
        if (function != null ? !function.equals(that.function) : that.function != null) return false;
        return !(attribute != null ? !attribute.equals(that.attribute) : that.attribute != null);

    }

    @Override
    public int hashCode() {
        int result = function != null ? function.hashCode() : 0;
        result = 31 * result + (attribute != null ? attribute.hashCode() : 0);
        result = 31 * result + (groupBy ? 1 : 0);
        return result;
    }
}
