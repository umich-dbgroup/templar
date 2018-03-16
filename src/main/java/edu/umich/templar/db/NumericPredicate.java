package edu.umich.templar.db;

public class NumericPredicate extends DBElement {
    private Attribute attr;
    private String op;
    private Double value;

    public NumericPredicate(Attribute attr, String op, Double value) {
        this.attr = attr;
        this.op = op;
        this.value = value;
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

    @Override
    public String toString() {
        return this.attr.toString() + " " + this.op + " " + this.value.intValue();
    }
}
