package edu.umich.templar.scorer;

import edu.umich.templar.db.*;
import edu.umich.templar.log.LogCountGraph;
import edu.umich.templar.log.graph.DBElementPair;
import edu.umich.templar.main.settings.Params;
import edu.umich.templar.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class LogCountGraphScorer implements InterpretationScorer {
    private LogCountGraph logCountGraph;

    public LogCountGraphScorer(LogCountGraph logCountGraph) {
        this.logCountGraph = logCountGraph;
    }

    @Override
    public double score(List<MatchedDBElement> interp) {
        List<Double> sims = new ArrayList<>();
        List<Double> diceScores = new ArrayList<>();

        for (MatchedDBElement mel : interp) {
            sims.add(mel.getScore());
        }

        double simScore = Utils.geometricMean(sims);

        // Calculate co-occurrence scores
        for (int i = 0; i < interp.size(); i++) {
            for (int j = i+1; j < interp.size(); j++) {
                DBElement el1 = this.logCountGraph.modifyElementForLevel(interp.get(i).getEl());
                DBElement el2 = this.logCountGraph.modifyElementForLevel(interp.get(j).getEl());

                // Only if both are not relations
                boolean bothNotRelations = !(el1 instanceof Relation || el2 instanceof Relation);

                // Only if at least one is not relation
                boolean atLeastOneNotRelation = !(el1 instanceof Relation) || !(el2 instanceof Relation);

                boolean bothHaveCount = this.logCountGraph.count(el1) > 0 && this.logCountGraph.count(el2) > 0;

                if (bothNotRelations) {
                // if (true) {
                    if (bothHaveCount) {
                        DBElementPair pair = new DBElementPair(el1, el2);
                        double dice = (2.0 * (double) this.logCountGraph.cooccur(pair))
                                / (this.logCountGraph.count(el1) + this.logCountGraph.count(el2));
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