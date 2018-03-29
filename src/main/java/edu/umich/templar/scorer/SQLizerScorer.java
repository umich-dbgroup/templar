package edu.umich.templar.scorer;

import edu.umich.templar.db.*;
import edu.umich.templar.main.settings.Params;
import edu.umich.templar.util.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SQLizerScorer implements InterpretationScorer {
    private Database db;
    // private boolean joinScore;

    public SQLizerScorer(Database db) {
        this.db = db;
        // this.joinScore = joinScore;
    }

    @Override
    public double score(List<MatchedDBElement> interp) {
        List<Double> sims = new ArrayList<>();

        Set<Relation> rels = new HashSet<>();
        for (MatchedDBElement mel : interp) {
            sims.add(mel.getScore());

            // Add relation to set
            /*
            if (mel.getEl() instanceof Relation) {
                rels.add((Relation) mel.getEl());
            } else if (mel.getEl() instanceof Attribute) {
                rels.add(((Attribute) mel.getEl()).getRelation());
            } else if (mel.getEl() instanceof TextPredicate) {
                rels.add(((TextPredicate) mel.getEl()).getAttribute().getRelation());
            } else if (mel.getEl() instanceof NumericPredicate) {
                rels.add(((NumericPredicate) mel.getEl()).getAttr().getRelation());
            } else if (mel.getEl() instanceof AggregatedPredicate) {
                rels.add(((AggregatedPredicate) mel.getEl()).getAttr().getRelation());
            } else if (mel.getEl() instanceof AggregatedAttribute) {
                rels.add(((AggregatedAttribute) mel.getEl()).getAttr().getRelation());
            } else if (mel.getEl() instanceof GroupedAttribute) {
                rels.add(((GroupedAttribute) mel.getEl()).getAttr().getRelation());
            } else {
                throw new RuntimeException("Unknown DBElement type.");
            }*/
        }

        // add join scores according to SQLizer
        /*if (this.joinScore) {
            if (rels.size() > 1) {
                int joins = this.db.longestJoinPathLength(rels) - 1;
                for (int i = 0; i < joins; i++) {
                    sims.add(1 - Params.SQLIZER_EPSILON);
                }

                int failedJoins = rels.size() - joins - 1;
                for (int i = 0; i < failedJoins; i++) {
                    sims.add(Params.SQLIZER_EPSILON);
                }
            }
        }*/

        return Utils.geometricMean(sims);
    }
}
