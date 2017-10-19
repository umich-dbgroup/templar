package edu.umich.templar.qf.agnostic;

import java.util.Map;

/**
 * Created by cjbaik on 10/17/17.
 */
public class ScoredAgnosticQueryFragment {
    AgnosticQueryFragment aqf;
    Double similarity;

    public ScoredAgnosticQueryFragment(AgnosticQueryFragment aqf, Double similarity) {
        this.aqf = aqf;
        this.similarity = similarity;
    }

    public double getDiceCoefficient(ScoredAgnosticQueryFragment other) {
        if (this.equals(other)) return 1.0;

        boolean thisIsBlankOrRel = this.getAqf() instanceof AgnosticBlank || this.getAqf() instanceof AgnosticRelationFragment;
        boolean otherIsBlankOrRel = other.getAqf() instanceof AgnosticBlank || other.getAqf() instanceof AgnosticRelationFragment;
        if (thisIsBlankOrRel || otherIsBlankOrRel) {
            // TODO: instead of assigning a dice of 0.5, we could also assign it
            // as the average of the other component's cooccurrences in the graph, or another value...
            return this.similarity * other.similarity * 0.5;
        }

        double numer = 2 * this.getSimilarity() * other.getSimilarity() * this.getAqf().getCooccurrence(other.getAqf());
        double denom = Math.max(this.getAqf().getCount() + other.getAqf().getCount(), 1);
        return numer / denom;
    }

    public AgnosticQueryFragment getAqf() {
        return aqf;
    }

    public Double getSimilarity() {
        return similarity;
    }
}
