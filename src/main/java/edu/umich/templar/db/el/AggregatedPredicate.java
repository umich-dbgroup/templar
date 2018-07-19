package edu.umich.templar.db.el;

import java.io.Serializable;
import java.util.Objects;

/*
 * For predicates of the form:
 * e.g. count(publication.title) = max(count(publication.title))
 */
public class AggregatedPredicate extends DBElement implements Serializable {
    private String aggFunction; // Aggregate function ("count"/"sum")
    private Attribute attr;
    private String op;
    private String valueFunction; // "max"/"min"/"average"

    public AggregatedPredicate(String aggFunction, Attribute attr, String op, String valueFunction) {
        this.aggFunction = aggFunction;
        this.attr = attr;
        this.op = op;
        this.valueFunction = valueFunction;
    }

    public String getAggFunction() {
        return aggFunction;
    }

    @Override
    public Attribute getAttribute() {
        return attr;
    }

    public String getOp() {
        return op;
    }

    public String getValueFunction() {
        return valueFunction;
    }

    @Override
    public Relation getRelation() {
        return this.attr.getRelation();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AggregatedPredicate that = (AggregatedPredicate) o;
        return Objects.equals(aggFunction, that.aggFunction) &&
                Objects.equals(attr, that.attr) &&
                Objects.equals(op, that.op) &&
                Objects.equals(valueFunction, that.valueFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aggFunction, attr, op, valueFunction);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (this.aggFunction != null) {
            sb.append(aggFunction);
            sb.append('(');
        }

        sb.append(attr);

        if (this.aggFunction != null) {
            sb.append(')');
        }
        sb.append(' ');
        sb.append(op);
        sb.append(' ');
        sb.append(valueFunction);
        sb.append('(');
        if (this.aggFunction != null) {
            sb.append(aggFunction);
            sb.append('(');
        }
        sb.append(attr);
        if (this.aggFunction != null) {
            sb.append(')');
        }
        sb.append(')');


        return sb.toString();
    }
}
