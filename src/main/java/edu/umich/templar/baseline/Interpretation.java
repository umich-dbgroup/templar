package edu.umich.templar.baseline;

import edu.umich.templar.db.MatchedDBElement;

import java.util.ArrayList;
import java.util.List;

public class Interpretation {
    private List<MatchedDBElement> mels;
    private double score;

    public Interpretation(List<MatchedDBElement> mels, double score) {
        this.mels = mels;
        this.score = score;
    }

    public void add(MatchedDBElement mel) {
        this.mels.add(mel);
    }

    public MatchedDBElement get(int i) {
        return this.mels.get(i);
    }

    public double getScore() {
        return score;
    }
}
