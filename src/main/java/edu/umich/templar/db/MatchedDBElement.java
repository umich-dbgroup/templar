package edu.umich.templar.db;

import edu.umich.templar.db.el.DBElement;

import java.io.Serializable;
import java.util.Objects;

public class MatchedDBElement implements Serializable {
    DBElement el;
    double score;

    public MatchedDBElement(DBElement el, double score) {
        this.el = el;
        this.score = score;
    }

    public DBElement getEl() {
        return el;
    }

    public double getScore() {
        return score;
    }

    @Override
    public String toString() {
        return this.el + " (" + this.score + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchedDBElement that = (MatchedDBElement) o;
        return Double.compare(that.score, score) == 0 &&
                Objects.equals(el, that.el);
    }

    @Override
    public int hashCode() {

        return Objects.hash(el, score);
    }
}
