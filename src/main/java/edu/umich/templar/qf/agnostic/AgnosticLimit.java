package edu.umich.templar.qf.agnostic;

/**
 * Created by cjbaik on 10/16/17.
 */
public class AgnosticLimit extends AgnosticQueryFragment {
    public AgnosticLimit() {
        super();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return 1;
    }
}