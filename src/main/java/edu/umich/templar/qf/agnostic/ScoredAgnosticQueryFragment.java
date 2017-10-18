package edu.umich.templar.qf.agnostic;

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
