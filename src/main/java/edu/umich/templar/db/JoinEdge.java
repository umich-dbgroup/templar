package edu.umich.templar.db;

import edu.umich.templar.db.el.Relation;

import java.util.Objects;

public class JoinEdge {
    private Relation r1;
    private Relation r2;

    public JoinEdge(Relation first, Relation second) {
        if (first.toString().compareTo(second.toString()) <= 0) {
            this.r1 = first;
            this.r2 = second;
        } else {
            this.r1 = second;
            this.r2 = first;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JoinEdge joinEdge = (JoinEdge) o;
        return Objects.equals(r1, joinEdge.r1) &&
                Objects.equals(r2, joinEdge.r2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(r1, r2);
    }

    @Override
    public String toString() {
        return this.r1.toString() + "-" + this.r2.toString();
    }
}
