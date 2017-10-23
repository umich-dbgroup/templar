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

    public double getMaxDiceCoefficient() {
        double maxDice = 0.0;
        for (Map.Entry<QueryFragment, Integer> e : this.getQf().getCooccurrenceMap().entrySet()) {
            double dice = (double) (2 * e.getValue()) / (this.getQf().getCount() + e.getKey().getCount());
            if (dice > maxDice) {
                maxDice = dice;
            }
        }

        return maxDice;
    }

    public double getAverageDiceCoefficient() {
        double diceSum = 0.0;
        for (Map.Entry<QueryFragment, Integer> e : this.getQf().getCooccurrenceMap().entrySet()) {
            diceSum += (double) (2 * e.getValue()) / (this.getQf().getCount() + e.getKey().getCount());
        }
        double denom = this.getQf().getCooccurrenceMap().size();

        if (denom == 0) return 0.0;

        return diceSum / denom;
    }

    public double getWeightedDiceCoefficient(ScoredQueryFragment other) {
        if (this.equals(other)) return 1.0;

        boolean thisIsBlank = this.getQf() instanceof BlankQueryFragment; //|| this.getQf() instanceof RelationFragment;
        boolean otherIsBlank = other.getQf() instanceof BlankQueryFragment; //|| other.getQf() instanceof RelationFragment;

        if (thisIsBlank && otherIsBlank) {
            return 0.0;
        } else if (thisIsBlank) {
            // return this.similarity * other.similarity * 0.5;
            return this.similarity * other.similarity * other.getAverageDiceCoefficient();
            // return (1 - this.similarity) * other.similarity * other.getMaxDiceCoefficient();
        } else if (otherIsBlank) {
            // return this.similarity * other.similarity * 0.5;
            return this.similarity * this.getAverageDiceCoefficient();
            // return this.similarity * (1 - other.similarity) * this.getMaxDiceCoefficient();
        }

        double numer = 2 * this.getQf().getCooccurrence(other.getQf());
        double denom = Math.max(this.getQf().getCount() + other.getQf().getCount(), 1);
        return this.similarity * other.similarity * numer / denom;
    }
}
