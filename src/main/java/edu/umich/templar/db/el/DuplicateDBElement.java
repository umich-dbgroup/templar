package edu.umich.templar.db.el;

import java.util.Objects;

public class DuplicateDBElement extends DBElement {
    private int index;
    private DBElement el;

    public DuplicateDBElement(int index, DBElement el) {
        this.index = index;
        this.el = el;
    }

    public int getIndex() {
        return index;
    }

    public DBElement getEl() {
        return el;
    }

    @Override
    public String toString() {
        return this.el.toString() + "#" + this.index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DuplicateDBElement that = (DuplicateDBElement) o;
        return index == that.index &&
                Objects.equals(el, that.el);
    }

    @Override
    public int hashCode() {

        return Objects.hash(index, el);
    }
}
