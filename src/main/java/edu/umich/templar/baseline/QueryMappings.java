package edu.umich.templar.baseline;

import edu.umich.templar.db.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QueryMappings {
    // Activate the join penalty indiscriminately
    private boolean joinScore;

    private Database db;
    private List<FragmentMappings> fragmentMappingsList;

    public QueryMappings(Database db, boolean joinScore) {
        this.db = db;
        this.joinScore = joinScore;
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

        // add join scores according to SQLizer
        if (this.joinScore) {
            if (rels.size() > 1) {
                int joins = this.db.longestJoinPathLength(rels) - 1;
                accum *= Math.pow(1 - BaselineParams.SQLIZER_EPSILON, joins);
                root += joins;

                int failedJoins = rels.size() - joins - 1;
                accum *= Math.pow(BaselineParams.SQLIZER_EPSILON, failedJoins);
                root += failedJoins;
            }
        }

        return Math.pow(accum, 1.0 / root);
    }

    public List<Interpretation> findOptimalInterpretations() {
        List<MatchedDBElement> candInterp = new ArrayList<>();
        List<Interpretation> maxInterps = new ArrayList<>();
        double maxScore = 0.0;

        int totalInterpsCount = 1;
        int[] counters = new int[this.fragmentMappingsList.size()];
        int[] listSizes = new int[this.fragmentMappingsList.size()];
        for (int i = 0; i < counters.length; i++) {
            counters[i] = 0;
            listSizes[i] = this.fragmentMappingsList.get(i).size();
            totalInterpsCount *= listSizes[i];
        }

        for (int i = 0; i < totalInterpsCount; i++) {
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
            while (counters[counterIndex] >= listSizes[counterIndex]) {
                counters[counterIndex] = 0;

                counterIndex++;
                if (counterIndex >= counters.length) break;

                counters[counterIndex] += 1;
            }
        }

        return maxInterps;
    }
}
