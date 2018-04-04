package edu.umich.templar.scorer;

import edu.umich.templar.db.el.AttributeAndPredicate;
import edu.umich.templar.db.el.DBElement;
import edu.umich.templar.db.MatchedDBElement;
import edu.umich.templar.db.el.Relation;
import edu.umich.templar.log.graph.DBElementPair;
import edu.umich.templar.log.graph.LogGraph;
import edu.umich.templar.log.graph.LogGraphTree;
import edu.umich.templar.main.settings.Params;
import edu.umich.templar.task.Interpretation;
import edu.umich.templar.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class LogGraphScorer implements InterpretationScorer {
    private LogGraph logGraph;
    private boolean includeJoin;

    public LogGraphScorer(LogGraph logGraph, boolean includeJoin) {
        this.logGraph = logGraph;
        this.includeJoin = includeJoin;
    }

    @Override
    public double score(Interpretation interp) {
        List<Double> sims = new ArrayList<>();
        List<Double> diceScores = new ArrayList<>();
        List<DBElement> els = new ArrayList<>();

        for (MatchedDBElement mel : interp.getElements()) {
            sims.add(mel.getScore());
            DBElement newEl = this.logGraph.modifyElementForLevel(mel.getEl());

            if (newEl instanceof AttributeAndPredicate) {
                els.add(((AttributeAndPredicate) newEl).getPredicate());
                els.add(((AttributeAndPredicate) newEl).getAttribute());
            } else {
                els.add(newEl);
            }
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

        if (this.includeJoin) {
            LogGraph logGraphClone = this.logGraph.deepClone();
            logGraphClone.forkSchemaGraph(els);

            // Calculate Steiner tree
            LogGraphTree steinerTree = logGraphClone.steiner(els);

            // Set join path on interpretation
            interp.setJoinPath(steinerTree.getJoinPath());

            // System.out.println(steinerTree.debug());

            // If Steiner tree pruned away an element from the interpretation, return 0.0.
            for (DBElement el : els) {
                if (!steinerTree.contains(el)) {
                    System.out.println("Returning 0.0 because Steiner tree doesn't contain " + el);
                    return 0.0;
                }
            }

            double joinsScore = steinerTree.joinScore();
            interpScore.add(joinsScore);
        } else {
            LogGraph schemaGraph = this.logGraph.schemaGraphOnly();
            schemaGraph.forkSchemaGraph(els);

            // Calculate Steiner tree
            LogGraphTree steinerTree = schemaGraph.steiner(els);

            // Set join path on interpretation
            interp.setJoinPath(steinerTree.getJoinPath());

            for (DBElement el : els) {
                if (!steinerTree.contains(el)) {
                    System.out.println("Returning 0.0 because Steiner tree doesn't contain " + el);
                    return 0.0;
                }
            }

            // Don't add joins score if log graph not activated
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
