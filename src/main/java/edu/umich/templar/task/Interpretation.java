package edu.umich.templar.task;

import edu.umich.templar.db.JoinPath;
import edu.umich.templar.db.MatchedDBElement;
import edu.umich.templar.log.graph.LogGraphTree;

import java.util.ArrayList;
import java.util.List;

public class Interpretation {
    private List<MatchedDBElement> mels;
    private double score;
    private JoinPath joinPath;

    public Interpretation(List<MatchedDBElement> mels) {
        this.mels = mels;
    }

    public List<MatchedDBElement> getElements() {
        return mels;
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

    public void setScore(double score) {
        this.score = score;
    }

    public JoinPath getJoinPath() {
        return joinPath;
    }

    public void setJoinPath(JoinPath joinPath) {
        this.joinPath = joinPath;
    }
}
