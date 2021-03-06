package edu.umich.templar._old.qf.pieces;

/**
 * Created by cjbaik on 10/16/17.
 */
public enum QFFunction {
    SUM("sum"), COUNT("count"), MAX("max"), MIN("min"), AVG("avg");

    private String text;

    QFFunction(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

    public static QFFunction getFunction(String funcName) {
        funcName = funcName.toLowerCase();
        if (funcName.equals("sum")) {
            return QFFunction.SUM;
        }
        if (funcName.equals("count")) {
            return QFFunction.COUNT;
        }
        if (funcName.equals("max")) {
            return QFFunction.MAX;
        }
        if (funcName.equals("min")) {
            return QFFunction.MIN;
        }
        if (funcName.equals("avg")) {
            return QFFunction.AVG;
        }
        throw new RuntimeException("Could not find function: " + funcName);
    }
}
