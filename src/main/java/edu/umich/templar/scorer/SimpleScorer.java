package edu.umich.templar.scorer;

import edu.umich.templar.db.MatchedDBElement;
import edu.umich.templar.task.Interpretation;
import edu.umich.templar.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class SimpleScorer implements InterpretationScorer {
    @Override
    public double score(Interpretation interp) {
        List<Double> sims = new ArrayList<>();

        for (MatchedDBElement mel : interp.getElements()) {
            sims.add(mel.getScore());
        }
        return Utils.geometricMean(sims);
    }
}
