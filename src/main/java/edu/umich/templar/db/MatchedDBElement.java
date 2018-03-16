package edu.umich.templar.db;

public class MatchedDBElement {
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
}
