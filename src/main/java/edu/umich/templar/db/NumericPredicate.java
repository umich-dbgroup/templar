package edu.umich.templar.db;

import java.util.List;
import java.util.Objects;

public class NumericPredicate extends DBElement {
    private Attribute attr;
    private String op;
    private Double value;
    private String function;

    public NumericPredicate(Attribute attr, String op, Double value, String function) {
        this.attr = attr;
        this.op = op;
        this.value = value;
        this.function = function;
    }

    public Attribute getAttr() {
        return attr;
    }

    public String getOp() {
        return op;
    }

    public Double getValue() {
        return value;
    }

    public String getFunction() {
        return function;
    }

    @Override
    public Relation getRelation() {
        return this.attr.getRelation();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NumericPredicate that = (NumericPredicate) o;
        return Objects.equals(attr, that.attr) &&
                Objects.equals(op, that.op) &&
                Objects.equals(value, that.value) &&
                Objects.equals(function, that.function);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attr, op, value, function);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (this.function != null) {
            result.append(this.function);
            result.append('(');
        }
        result.append(this.attr.toString());
        if (this.function != null) {
            result.append(')');
        }

        result.append(' ');
        result.append(this.op);
        result.append(' ');
        result.append(this.value.intValue());

        return result.toString();
    }
}
