package edu.umich.templar.scorer;

import edu.umich.templar.db.Database;
import edu.umich.templar.db.JoinPath;
import edu.umich.templar.db.MatchedDBElement;
import edu.umich.templar.db.el.AttributeAndPredicate;
import edu.umich.templar.db.el.DBElement;
import edu.umich.templar.log.NLSQLLog;
import edu.umich.templar.log.graph.LogGraph;
import edu.umich.templar.log.graph.LogGraphTree;
import edu.umich.templar.task.Interpretation;
import edu.umich.templar.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class NLSQLScorer implements InterpretationScorer {
    private NLSQLLog nlsqlLog;
    private LogGraph schemaGraph;

    public NLSQLScorer(Database db, NLSQLLog nlsqlLog) {
        this.schemaGraph = new LogGraph(db);
        this.nlsqlLog = nlsqlLog;
    }

    @Override
    public double score(Interpretation interp) {
        List<Double> sims = new ArrayList<>();
        List<DBElement> els = new ArrayList<>();

        // AttrAndPreds that we can ignore when generating our Steiner tree later
        List<DBElement> ignoreDuplicates = new ArrayList<>();

        for (MatchedDBElement mel : interp.getElements()) {
            double weightedScore = 0.5 * mel.getScore() +
                    0.5 * this.nlsqlLog.getWeightedLikelihood(mel.getKeyword(), mel.getEl().toString());

            sims.add(weightedScore);

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

        // Calculate Steiner tree
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
        }

        return Utils.geometricMean(sims);
    }
}