package edu.umich.templar.task;

import edu.umich.templar.db.*;
import edu.umich.templar.scorer.InterpretationScorer;

import java.util.ArrayList;
import java.util.List;

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
