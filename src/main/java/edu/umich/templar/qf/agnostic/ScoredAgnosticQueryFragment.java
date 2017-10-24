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

    public double getMaxDiceCoefficient() {
        double maxDice = 0.0;
        for (Map.Entry<AgnosticQueryFragment, Integer> e : this.getAqf().getCooccurrenceMap().entrySet()) {
            double dice = (double) (2 * e.getValue()) / (this.getAqf().getCount() + e.getKey().getCount());
            if (dice > maxDice) {
                maxDice = dice;
            }
        }

        return maxDice;
    }

    public double getAverageDiceCoefficient() {
        double diceSum = 0.0;
        for (Map.Entry<AgnosticQueryFragment, Integer> e : this.getAqf().getCooccurrenceMap().entrySet()) {
            diceSum += (double) (2 * e.getValue()) / (this.getAqf().getCount() + e.getKey().getCount());
        }
        double denom = this.getAqf().getCooccurrenceMap().size();
        if (denom == 0) return 0.0;

        return diceSum / denom;
    }

    public double getWeightedDiceCoefficient(ScoredAgnosticQueryFragment other) {
        if (this.equals(other)) return 1.0;

        boolean thisIsBlankOrRel = this.getAqf() instanceof AgnosticBlank || this.getAqf() instanceof AgnosticRelationFragment;
        boolean otherIsBlankOrRel = other.getAqf() instanceof AgnosticBlank || other.getAqf() instanceof AgnosticRelationFragment;

        if (thisIsBlankOrRel && otherIsBlankOrRel) {
            return 0.0;
        } else if (thisIsBlankOrRel) {
            // return this.similarity * other.similarity * 0.5;
            // return this.similarity * other.similarity * other.getAverageDiceCoefficient();
            // return (1 - this.similarity) * other.similarity * other.getMaxDiceCoefficient();
            return other.getAverageDiceCoefficient();
        } else if (otherIsBlankOrRel) {
            // return this.similarity * other.similarity * 0.5;
            // return this.similarity * other.similarity * this.getAverageDiceCoefficient();
            // return this.similarity * (1 - other.similarity) * this.getMaxDiceCoefficient();
            return this.getAverageDiceCoefficient();
        }

        double numer = 2 * this.getAqf().getCooccurrence(other.getAqf());
        double denom = Math.max(this.getAqf().getCount() + other.getAqf().getCount(), 1);
        // return this.getSimilarity() * other.getSimilarity() * numer / denom;
        return numer / denom;
    }

    public AgnosticQueryFragment getAqf() {
        return aqf;
    }

    public Double getSimilarity() {
        return similarity;
    }
}
