package edu.umich.templar.scorer;

import edu.umich.templar.db.DBElement;
import edu.umich.templar.db.MatchedDBElement;
import edu.umich.templar.log.graph.LogGraph;
import edu.umich.templar.log.graph.LogGraphTree;
import edu.umich.templar.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class LogGraphScorer implements InterpretationScorer {
    private LogGraph logGraph;

    public LogGraphScorer(LogGraph logGraph) {
        this.logGraph = logGraph;
    }

    @Override
    public double score(List<MatchedDBElement> interp) {
        List<Double> sims = new ArrayList<>();
        List<Double> diceScores = new ArrayList<>();
        List<DBElement> els = new ArrayList<>();

        for (MatchedDBElement mel : interp) {
            sims.add(mel.getScore());
            els.add(mel.getEl());
        }

        double simScore = Utils.geometricMean(sims);

        // Calculate Steiner tree
        LogGraphTree steinerTree = this.logGraph.steiner(els);
        System.out.println();
        System.out.println(steinerTree.debug());
        double logScore = steinerTree.totalDistance();

        double harmonicMean = Utils.harmonicMean(simScore, logScore);

        // TODO: when we run Query 139, we also need the non-relation query fragment co-occurrence scores as a 3rd score
        System.out.println("Similarity score: " + simScore);
        System.out.println("Steiner tree score: " + logScore);
        System.out.println("HARMONIC MEAN: " + harmonicMean);
        return harmonicMean;
    }
}
