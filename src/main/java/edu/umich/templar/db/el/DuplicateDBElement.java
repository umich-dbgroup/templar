package edu.umich.templar.db.el;

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
}
