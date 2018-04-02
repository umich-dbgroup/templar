package edu.umich.templar.scorer;

import edu.umich.templar.db.DBElement;
import edu.umich.templar.db.MatchedDBElement;
import edu.umich.templar.db.Relation;
import edu.umich.templar.log.graph.DBElementPair;
import edu.umich.templar.log.graph.LogGraph;
import edu.umich.templar.log.graph.LogGraphNode;
import edu.umich.templar.log.graph.LogGraphTree;
import edu.umich.templar.main.settings.Params;
import edu.umich.templar.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class LogGraphScorer implements InterpretationScorer {
    private LogGraph logGraph;
    private boolean includeSteiner;

    public LogGraphScorer(LogGraph logGraph, boolean includeSteiner) {
        this.logGraph = logGraph;
        this.includeSteiner = includeSteiner;
    }

    @Override
    public double score(List<MatchedDBElement> interp) {
        List<Double> sims = new ArrayList<>();
        List<Double> diceScores = new ArrayList<>();
        List<DBElement> els = new ArrayList<>();

        for (MatchedDBElement mel : interp) {
            sims.add(mel.getScore());
            DBElement newEl = this.logGraph.modifyElementForLevel(mel.getEl());

            // If two relations in the interpretation are identical, return 0.0
            // (i.e. we can have non-relation duplicates, for self-joins for example)
            if (els.contains(newEl) && newEl instanceof Relation) {
                System.out.println("Returning 0.0 because " + newEl + " is a duplicate in the interpretation.");
                return 0.0;
            }

            els.add(newEl);
        }

        double simScore = Utils.geometricMean(sims);

        // Interpretation score can be comprised of terminal fragments scores plus optional Steiner join score
        List<Double> interpScore = new ArrayList<>();

        // Calculate co-occurrence scores
        for (int i = 0; i < els.size(); i++) {
            for (int j = i+1; j < els.size(); j++) {
                DBElement el1 = els.get(i);
                DBElement el2 = els.get(j);

                // Only if both are not relations
                boolean bothNotRelations = !(el1 instanceof Relation || el2 instanceof Relation);

                boolean bothHaveCount = this.logGraph.count(el1) > 0 && this.logGraph.count(el2) > 0;

                if (bothNotRelations) {
                    if (bothHaveCount) {
                        DBElementPair pair = new DBElementPair(el1, el2);
                        double dice = (2.0 * (double) this.logGraph.cooccur(pair))
                                / (this.logGraph.count(el1) + this.logGraph.count(el2));
                        diceScores.add(dice);
                    } else {
                        diceScores.add(Params.EPSILON);
                    }
                }
            }
        }
        double terminalFragsScore = Math.max(Utils.geometricMean(diceScores), Params.EPSILON);

        if (this.includeSteiner) {
            // Calculate Steiner tree
            LogGraphTree steinerTree = this.logGraph.steiner(els);

            // If Steiner tree pruned away an element from the interpretation, return 0.0.
            for (DBElement el : els) {
                if (!steinerTree.contains(el)) {
                    System.out.println("Returning 0.0 because Steiner tree doesn't contain " + el);
                    return 0.0;
                }
            }

            double joinsScore = steinerTree.joinScore();
            interpScore.add(joinsScore);
        }

        interpScore.add(terminalFragsScore);

        double result = Params.CONF_FW * simScore + (1 - Params.CONF_FW) * Utils.geometricMean(interpScore);

        /*
        System.out.println();
        System.out.println(steinerTree.debug());
        System.out.println("Similarity score: " + simScore);
        System.out.println("Terminal fragments score: " + terminalFragsScore);
        System.out.println("Steiner tree (joins) score: " + joinsScore);
        System.out.println("COMBINED SCORE: " + result);
        */

        return result;
    }
}
