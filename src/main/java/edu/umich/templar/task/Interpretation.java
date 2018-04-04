package edu.umich.templar.task;

import edu.umich.templar.db.MatchedDBElement;
import edu.umich.templar.log.graph.LogGraphTree;

import java.util.ArrayList;
import java.util.List;

public class Interpretation {
    private List<MatchedDBElement> mels;
    private double score;
    private LogGraphTree joinPath;

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

    public LogGraphTree getJoinPath() {
        return joinPath;
    }

    public void setJoinPath(LogGraphTree joinPath) {
        this.joinPath = joinPath;
    }
}
