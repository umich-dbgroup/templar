package edu.umich.templar.sqlparse;

/**
 * Created by cjbaik on 8/29/17.
 */
public class OrderedPredicate {
    String operator;
    String predicate;

    public OrderedPredicate(String operator, String predicate) {
        this.operator = operator;
        this.predicate = predicate;
    }

    public String getOperator() {
        return operator;
    }

    public String getPredicate() {
        return predicate;
    }
}
