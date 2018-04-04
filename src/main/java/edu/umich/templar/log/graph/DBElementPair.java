package edu.umich.templar.log.graph;

import edu.umich.templar.db.el.DBElement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class DBElementPair {
    private List<DBElement> elements;

    public DBElementPair(DBElement first, DBElement second) {
        this.elements = new ArrayList<>();
        this.elements.add(first);
        this.elements.add(second);
        this.elements.sort(Comparator.comparing(DBElement::hashCode));
    }

    public DBElement getFirst() {
        return this.elements.get(0);
    }

    public DBElement getSecond() {
        return this.elements.get(1);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DBElementPair that = (DBElementPair) o;
        return Objects.equals(elements, that.elements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elements);
    }
}
