package edu.umich.templar.qf;

import java.util.Map;

/**
 * Created by cjbaik on 10/18/17.
 */
public class ScoredQueryFragment {
    QueryFragment qf;
    double similarity;

    public ScoredQueryFragment(QueryFragment qf, double similarity) {
        this.qf = qf;
        this.similarity = similarity;
    }

    public QueryFragment getQf() {
        return qf;
    }

    public double getSimilarity() {
        return similarity;
    }

    public double getDiceCoefficient(ScoredQueryFragment other) {
        if (this.equals(other)) return 1.0;

        boolean thisIsBlank = this.getQf() instanceof BlankQueryFragment; //|| this.getQf() instanceof RelationFragment;
        boolean otherIsBlank = other.getQf() instanceof BlankQueryFragment; //|| other.getQf() instanceof RelationFragment;
        if (thisIsBlank || otherIsBlank) {
            // TODO: instead of assigning a dice of 0.5, we could also assign it
            // as the average of the other component's cooccurrences in the graph, or another value...
            return this.similarity * other.similarity * 0.5;
        }

        double numer = 2 * this.getSimilarity() * other.getSimilarity() * this.getQf().getCooccurrence(other.getQf());
        double denom = Math.max(this.getQf().getCount() + other.getQf().getCount(), 1);
        return numer / denom;
    }
}
