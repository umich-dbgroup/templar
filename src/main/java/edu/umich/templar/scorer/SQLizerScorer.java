package edu.umich.templar.scorer;

import edu.umich.templar.db.*;
import edu.umich.templar.db.el.AttributeAndPredicate;
import edu.umich.templar.db.el.DBElement;
import edu.umich.templar.log.graph.LogGraph;
import edu.umich.templar.log.graph.LogGraphTree;
import edu.umich.templar.task.Interpretation;
import edu.umich.templar.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class SQLizerScorer implements InterpretationScorer {
    private LogGraph schemaGraph;

    public SQLizerScorer(Database db) {
        this.schemaGraph = new LogGraph(db);
    }

    @Override
    public double score(Interpretation interp) {
        List<Double> sims = new ArrayList<>();
        List<DBElement> els = new ArrayList<>();

        // AttrAndPreds that we can ignore when generating our Steiner tree later
        List<DBElement> ignoreDuplicates = new ArrayList<>();

        for (MatchedDBElement mel : interp.getElements()) {
            sims.add(mel.getScore());

            if (mel.getEl() instanceof AttributeAndPredicate) {
                els.add(((AttributeAndPredicate) mel.getEl()).getPredicate());
                els.add(((AttributeAndPredicate) mel.getEl()).getAttributePart());

                ignoreDuplicates.add(((AttributeAndPredicate) mel.getEl()).getAttributePart());
            } else {
                els.add(mel.getEl());
            }
        }

        LogGraph schemaGraph = this.schemaGraph.schemaGraphOnly();
        schemaGraph.forkSchemaGraph(els, ignoreDuplicates);

        // Calculate Steiner tree -- MOVED
        /*
        LogGraphTree steinerTree = schemaGraph.steiner(els);

        // In the case of an invalid Steiner tree (e.g. only one node which is a relation)
        if (steinerTree == null) {
            interp.setJoinPath(new JoinPath());
            return 0.0;
        }

        // Set join path on interpretation
        interp.setJoinPath(steinerTree.getJoinPath());

        for (DBElement el : els) {
            if (!steinerTree.contains(el)) {
                System.out.println("Returning 0.0 because Steiner tree doesn't contain " + el);
                return 0.0;
            }
        } */

        // Don't add joins score if log graph not activated

        return Utils.geometricMean(sims);
    }
}
