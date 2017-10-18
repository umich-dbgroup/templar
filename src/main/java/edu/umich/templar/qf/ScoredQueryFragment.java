package edu.umich.templar.qf;

/**
 * Created by cjbaik on 10/18/17.
 */
public class ScoredQueryFragment {
    QueryFragment qf;
    Double similarity;

    public ScoredQueryFragment(QueryFragment qf, Double similarity) {
        this.qf = qf;
        this.similarity = similarity;
    }

    public QueryFragment getQf() {
        return qf;
    }

    public Double getSimilarity() {
        return similarity;
    }

    public double getDiceCoefficient(ScoredQueryFragment other) {
        if (this.equals(other)) return 1.0;

        double numer = 2 * this.getSimilarity() * other.getSimilarity() * this.getQf().getCooccurrence(other.getQf());
        double denom = Math.max(this.getQf().getCount() + other.getQf().getCount(), 1);
        return numer / denom;
    }
}
