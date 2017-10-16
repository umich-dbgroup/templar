package edu.umich.templar.qf.agnostic;

/**
 * Created by cjbaik on 10/16/17.
 */
public class AgnosticLimit extends AgnosticQueryFragment {
    boolean field;

    public AgnosticLimit() {
        super();
        this.field = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AgnosticLimit that = (AgnosticLimit) o;

        return field == that.field;

    }

    @Override
    public int hashCode() {
        return (field ? 1 : 0);
    }
}
