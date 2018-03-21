package edu.umich.templar.scorer;

import edu.umich.templar.db.MatchedDBElement;
import edu.umich.templar.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class LogGraphScorer implements InterpretationScorer {
    @Override
    public double score(List<MatchedDBElement> interp) {
        List<Double> sims = new ArrayList<>();

        for (MatchedDBElement mel : interp) {
            sims.add(mel.getScore());
        }
        return Utils.geometricMean(sims);
    }
}
