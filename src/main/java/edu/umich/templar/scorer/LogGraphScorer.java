package edu.umich.templar.scorer;

import edu.umich.templar.db.DBElement;
import edu.umich.templar.db.MatchedDBElement;
import edu.umich.templar.db.Relation;
import edu.umich.templar.log.LogGraph;
import edu.umich.templar.log.graph.DBElementPair;
import edu.umich.templar.main.settings.Params;
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
        // List<Double> nodeCounts = new ArrayList<>();
        // List<Double> edgeCounts = new ArrayList<>();
        List<Double> sims = new ArrayList<>();
        List<Double> diceScores = new ArrayList<>();

        // int numNodes = 0;
        for (MatchedDBElement mel : interp) {
            sims.add(mel.getScore());
            // nodeCounts.add((double) this.logGraph.count(mel.getEl()));

            // Only non-relations
            // if (!(mel.getEl() instanceof Relation)) numNodes++;
        }
        double simScore = Utils.geometricMean(sims);

        // Calculate co-occurrence scores
        for (int i = 0; i < interp.size(); i++) {
            for (int j = i+1; j < interp.size(); j++) {
                DBElement el1 = this.logGraph.modifyElementForLevel(interp.get(i).getEl());
                DBElement el2 = this.logGraph.modifyElementForLevel(interp.get(j).getEl());

                // Only if both are not relations
                boolean bothNotRelations = !(el1 instanceof Relation || el2 instanceof Relation);

                // Only if at least one is not relation
                boolean atLeastOneNotRelation = !(el1 instanceof Relation) || !(el2 instanceof Relation);

                boolean bothHaveCount = this.logGraph.count(el1) > 0 && this.logGraph.count(el2) > 0;

                // if (bothNotRelations) {
                if (true) {
                    if (bothHaveCount) {
                        DBElementPair pair = new DBElementPair(el1, el2);
                        double dice = (2.0 * (double) this.logGraph.cooccur(pair))
                                / (this.logGraph.count(el1) + this.logGraph.count(el2));
                        diceScores.add(dice);
                    } else {
                        diceScores.add(0.0);
                    }
                }
            }
        }

        // Calculate average dice score (will be at least epsilon)
        double logScore = Math.max(Utils.mean(diceScores), Params.EPSILON);

        // Calculate avg edge count / # nodes
        // double logScore = Utils.mean(edgeCounts) / numNodes;

        // Calculate average edge count / average node count
        // double logScore = Utils.mean(edgeCounts) / Utils.mean(nodeCounts);

        return Utils.harmonicMean(simScore, logScore);
    }
}
