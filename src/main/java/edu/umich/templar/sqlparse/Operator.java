package edu.umich.templar.sqlparse;

/**
 * Created by cjbaik on 9/11/17.
 */
public enum Operator {
    LT("<"), LTE("<="), GT(">"), GTE(">="), EQ("="), NE("!=");

    private String text;

    Operator(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
