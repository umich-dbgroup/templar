package edu.umich.templar.baseline;

import edu.umich.templar.db.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QueryMappings {
    private Database db;
    private List<FragmentMappings> fragmentMappingsList;

    public QueryMappings(Database db) {
        this.db = db;
        this.fragmentMappingsList = new ArrayList<>();
    }

    public List<FragmentMappings> getFragmentMappingsList() {
        return fragmentMappingsList;
    }

    public void addFragmentMappings(FragmentMappings fragmentMappings) {
        this.fragmentMappingsList.add(fragmentMappings);
    }

    public double calculateScore(List<MatchedDBElement> interp) {
        double accum = 1.0;
        double root = 0;

        Set<Relation> rels = new HashSet<>();
        for (MatchedDBElement mel : interp) {
            accum *= mel.getScore();
            root++;

            // Add relation to set
            if (mel.getEl() instanceof Relation) {
                rels.add((Relation) mel.getEl());
            } else if (mel.getEl() instanceof Attribute) {
                rels.add(((Attribute) mel.getEl()).getRelation());
            } else if (mel.getEl() instanceof Value) {
                rels.add(((Value) mel.getEl()).getAttribute().getRelation());
            } else if (mel.getEl() instanceof NumericPredicate) {
                rels.add(((NumericPredicate) mel.getEl()).getAttr().getRelation());
            } else {
                throw new RuntimeException("Unknown DBElement type.");
            }
        }

        // find if there's a join path between these tables (i.e. are there any that don't connect to any other?)
        if (rels.size() > 1) {
            for (Relation r1 : rels) {
                boolean hasJoin = false;
                for (Relation r2 : rels) {
                    if (this.db.hasJoin(r1, r2)) {
                        hasJoin = true;
                        break;
                    }
                }
                if (hasJoin) {
                    accum *= 1 - BaselineParams.SQLIZER_EPSILON;
                    root++;
                } else {
                    accum *= BaselineParams.SQLIZER_EPSILON;
                    root++;
                }
            }
        }

        return Math.pow(accum, 1.0 / root);
    }

    public List<Interpretation> findOptimalInterpretations() {
        List<MatchedDBElement> candInterp = new ArrayList<>();
        List<Interpretation> maxInterps = new ArrayList<>();
        double maxScore = 0.0;

        int[] counters = new int[this.fragmentMappingsList.size()];
        for (int i = 0; i <  counters.length; i++) {
            counters[i] = 0;
        }

        for (int i = 0; i < (BaselineParams.MAX_TOP_CANDIDATES ^ this.fragmentMappingsList.size()); i++) {
            for (int j = 0; j < this.fragmentMappingsList.size(); j++) {
                FragmentMappings fragMappings = this.fragmentMappingsList.get(j);
                candInterp.add(fragMappings.get(counters[j]));
            }
            double score = this.calculateScore(candInterp);
            if (score > maxScore) {
                maxInterps = new ArrayList<>();
                maxInterps.add(new Interpretation(candInterp, score));
                maxScore = score;
            } else if (score == maxScore) {
                maxInterps.add(new Interpretation(candInterp, score));
            }
            candInterp = new ArrayList<>();

            int counterIndex = 0;
            counters[counterIndex]++;
            while (counters[counterIndex] >= BaselineParams.MAX_TOP_CANDIDATES) {
                counters[counterIndex] = 0;
                counters[counterIndex + 1] += 1;
            }
        }

        return maxInterps;
    }
}
