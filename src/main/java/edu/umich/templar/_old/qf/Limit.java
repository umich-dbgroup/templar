package edu.umich.templar._old.qf;

/**
 * Created by cjbaik on 10/18/17.
 */
public class Limit extends QueryFragment {
    public Limit() {
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
