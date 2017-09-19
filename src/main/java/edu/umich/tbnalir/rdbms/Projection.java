package edu.umich.tbnalir.rdbms;

/**
 * Created by cjbaik on 9/12/17.
 */
public class Projection {
    String function;        // Function, if any
    String alias;           // Alias for attribute, if there is one
    Attribute attribute;

    boolean groupBy;        // If we want to GROUP BY this projection, set true

    public Projection(String alias, Attribute attribute, String function) {
        this.alias = alias;
        this.attribute = attribute;
        this.function = function;

        this.groupBy = false;
    }

    public boolean isGroupBy() {
        return groupBy;
    }

    public void setGroupBy(boolean groupBy) {
        this.groupBy = groupBy;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public boolean covers(Projection other) {
        if (this.equals(other)) return true;

        if (this.getAlias() == null && this.getAttribute().equals(other.getAttribute())) {
            return true;
        }

        return false;
    }

    public void applyAggregateFunction() {
        if (this.attribute.type.equals("int")) {
            this.function = "sum";
        } else {
            this.function = "count";
        }
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
            if (isCount) {
                sb.append("distinct(");
            }
        }
        if (this.alias != null) {
            sb.append(this.alias);
            sb.append(".");
        }
        sb.append(this.attribute.getName());
        if (this.function != null && !countingIntAttr) {
            sb.append(")");
            if (isCount) {
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

        if (alias != null ? !alias.equals(that.alias) : that.alias != null) return false;
        return !(attribute != null ? !attribute.equals(that.attribute) : that.attribute != null);

    }

    @Override
    public int hashCode() {
        int result = alias != null ? alias.hashCode() : 0;
        result = 31 * result + (attribute != null ? attribute.hashCode() : 0);
        return result;
    }
}
