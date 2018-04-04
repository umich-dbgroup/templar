package edu.umich.templar.scorer;

import edu.umich.templar.db.*;
import edu.umich.templar.db.el.AttributeAndPredicate;
import edu.umich.templar.db.el.DBElement;
import edu.umich.templar.db.el.Relation;
import edu.umich.templar.log.graph.LogGraph;
import edu.umich.templar.log.graph.LogGraphTree;
import edu.umich.templar.task.Interpretation;
import edu.umich.templar.util.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SQLizerScorer implements InterpretationScorer {
    private LogGraph schemaGraph;

    public SQLizerScorer(Database db) {
        this.schemaGraph = new LogGraph(db);
    }

    @Override
    public double score(Interpretation interp) {
        List<Double> sims = new ArrayList<>();
        List<DBElement> els = new ArrayList<>();

        for (MatchedDBElement mel : interp.getElements()) {
            sims.add(mel.getScore());

            if (mel.getEl() instanceof AttributeAndPredicate) {
                els.add(((AttributeAndPredicate) mel.getEl()).getPredicate());
                els.add(((AttributeAndPredicate) mel.getEl()).getAttribute());
            } else {
                els.add(mel.getEl());
            }
        }

        LogGraph schemaGraph = this.schemaGraph.schemaGraphOnly();
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

        return Utils.geometricMean(sims);
    }
}
