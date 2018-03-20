package edu.umich.templar.db;

public class AggregatedAttribute extends DBElement {
    String function;
    Attribute attr;

    public AggregatedAttribute(String function, Attribute attr) {
        this.function = function;
        this.attr = attr;
    }

    public String getFunction() {
        return function;
    }

    public Attribute getAttr() {
        return attr;
    }

    @Override
    public String toString() {
        return this.function + "(" + this.attr + ")";
    }
}
