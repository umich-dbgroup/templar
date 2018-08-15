package edu.umich.templar.task;

import edu.umich.templar.db.*;
import edu.umich.templar.db.el.AttributeAndPredicate;
import edu.umich.templar.db.el.DBElement;
import edu.umich.templar.log.graph.LogGraph;
import edu.umich.templar.log.graph.LogGraphTree;
import edu.umich.templar.scorer.InterpretationScorer;
import edu.umich.templar.util.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QueryMappings {
    private InterpretationScorer scorer;
    private List<FragmentMappings> fragmentMappingsList;
    private Integer totalInterpsCount;

    public QueryMappings(InterpretationScorer scorer) {
        this.scorer = scorer;
        this.fragmentMappingsList = new ArrayList<>();
    }

    public List<FragmentMappings> getFragmentMappingsList() {
        return fragmentMappingsList;
    }

    public void addFragmentMappings(FragmentMappings fragmentMappings) {
        this.fragmentMappingsList.add(fragmentMappings);
    }

    public Integer getTotalInterpsCount() {
        return totalInterpsCount;
    }

    public List<Interpretation> getMultipleJoinPathInterps(Database db, List<MatchedDBElement> interpEls) {
        List<Interpretation> results = new ArrayList<>();

        List<DBElement> els = new ArrayList<>();

        // AttrAndPreds that we can ignore when generating our Steiner tree later
        List<DBElement> ignoreDuplicates = new ArrayList<>();

        for (MatchedDBElement mel : interpEls) {
            if (mel.getEl() instanceof AttributeAndPredicate) {
                els.add(((AttributeAndPredicate) mel.getEl()).getPredicate());
                els.add(((AttributeAndPredicate) mel.getEl()).getAttributePart());

                ignoreDuplicates.add(((AttributeAndPredicate) mel.getEl()).getAttributePart());
            } else {
                els.add(mel.getEl());
            }
        }

        LogGraph schemaGraph = new LogGraph(db).schemaGraphOnly();
        schemaGraph.forkSchemaGraph(els, ignoreDuplicates);

        // Calculate Steiner tree
        Set<LogGraphTree> steinerTrees = schemaGraph.steiner(els);

        outer:
        for (LogGraphTree steinerTree : steinerTrees) {
            Interpretation interp = new Interpretation(interpEls);

            // In the case of an invalid Steiner tree (e.g. only one node which is a relation)
            if (steinerTree == null) {
                // interp.setJoinPath(new JoinPath());
                continue;
            }

            for (DBElement el : els) {
                if (!steinerTree.contains(el)) {
                    continue outer;
                }
            }

            // Set join path on interpretation
            interp.setJoinPath(steinerTree.getJoinPath());
            results.add(interp);
        }
        return results;
    }

    public Set<Interpretation> getAllInterpretations(Database db) {
        List<MatchedDBElement> candInterp = new ArrayList<>();
        Set<Interpretation> interps = new HashSet<>();

        int totalInterpsCount = 1;
        int[] counters = new int[this.fragmentMappingsList.size()];
        int[] listSizes = new int[this.fragmentMappingsList.size()];
        for (int i = 0; i < counters.length; i++) {
            counters[i] = 0;
            listSizes[i] = this.fragmentMappingsList.get(i).size();
            totalInterpsCount *= listSizes[i];
        }

        // System.out.println("TOTAL INTERPS COUNT: " + totalInterpsCount);
        this.totalInterpsCount = totalInterpsCount;

        for (int i = 0; i < totalInterpsCount; i++) {
            for (int j = 0; j < this.fragmentMappingsList.size(); j++) {
                FragmentMappings fragMappings = this.fragmentMappingsList.get(j);
                candInterp.add(fragMappings.get(counters[j]));
            }

            // Interpretation interpObj = new Interpretation(candInterp);

            // Find possible join paths given candInterp, and create multiple interpretations
            List<Interpretation> joinPathInterps = this.getMultipleJoinPathInterps(db, candInterp);
            for (Interpretation jpInterp : joinPathInterps) {
                double score = this.scorer.score(jpInterp);
                jpInterp.setScore(score);
                interps.add(jpInterp);
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

        return interps;
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

        System.out.println("TOTAL INTERPS COUNT: " + totalInterpsCount);
        this.totalInterpsCount = totalInterpsCount;

        for (int i = 0; i < totalInterpsCount; i++) {
            for (int j = 0; j < this.fragmentMappingsList.size(); j++) {
                FragmentMappings fragMappings = this.fragmentMappingsList.get(j);
                candInterp.add(fragMappings.get(counters[j]));
            }

            Interpretation interpObj = new Interpretation(candInterp);
            double score = this.scorer.score(interpObj);
            interpObj.setScore(score);

            if (score > maxScore) {
                maxInterps = new ArrayList<>();
                maxInterps.add(interpObj);
                maxScore = score;
            } else if (score == maxScore) {
                maxInterps.add(interpObj);
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
