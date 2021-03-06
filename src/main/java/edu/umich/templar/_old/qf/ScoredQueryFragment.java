package edu.umich.templar._old.qf;

import java.util.Map;

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

    public double getDiceCoefficient(ScoredQueryFragment other) {
        // Return 0 if identical
        if (this.equals(other)) return 0.0;

        double numer = 2 * this.getQf().getCooccurrence(other.getQf());
        double denom = Math.max(this.getQf().getCount() + other.getQf().getCount(), 1);
        return numer / denom;
    }

    public double getWeightedDiceCoefficient(ScoredQueryFragment other) {
        // if (this.equals(other)) return 1.0;

        // Return 0, in fact...
        if (this.equals(other)) return 0.0;

        /*
        boolean thisIsBlank = this.getQf() instanceof BlankQueryFragment; //|| this.getQf() instanceof RelationFragment;
        boolean otherIsBlank = other.getQf() instanceof BlankQueryFragment; //|| other.getQf() instanceof RelationFragment;

        if (thisIsBlank && otherIsBlank) {
            return 0.0;
        } else if (thisIsBlank) {
            // return this.similarity * other.similarity * 0.5;
            // return (1 - this.similarity) * other.similarity * other.getAverageDiceCoefficient();
            // return Constants.MIN_SIM * other.getAverageDiceCoefficient();
            // return other.getAverageDiceCoefficient();
            // return (1 - this.similarity) * other.similarity * other.getMaxDiceCoefficient();

            // Preferred working one
            // return (1 - this.similarity) * other.getAverageDiceCoefficient();

            // Cleaner one
            return other.getAverageDiceCoefficient();
        } else if (otherIsBlank) {
            // return this.similarity * other.similarity * 0.5;
            // return this.similarity * (1 - other.similarity) * this.getAverageDiceCoefficient();
            // return this.similarity * this.getAverageDiceCoefficient();
            // return this.getAverageDiceCoefficient();
            // return this.similarity * (1 - other.similarity) * this.getMaxDiceCoefficient();

            // Preferred working one
            // return (1 - this.similarity) * this.getAverageDiceCoefficient();

            // Cleaner one
            return this.getAverageDiceCoefficient();
        }*/

        double numer = 2 * this.getQf().getCooccurrence(other.getQf());
        double denom = Math.max(this.getQf().getCount() + other.getQf().getCount(), 1);
        // return this.similarity * other.similarity * numer / denom;

        // Best working one
        // return this.similarity * numer / denom;

        // Cleaner one
        return numer / denom;
    }

    public double getConditionalProbability(ScoredQueryFragment other) {
        double numer = this.getQf().getCooccurrence(other.getQf());
        double denom = Math.max(this.getQf().getCount(), 1);
        return numer / denom;
    }

    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }
}
