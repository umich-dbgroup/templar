package edu.umich.templar.log;

import edu.umich.templar.task.FragmentTask;
import edu.umich.templar.task.QueryTask;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NLSQLLog {
    Map<String, Bag> lexicon;

    public NLSQLLog() {
        this.lexicon = new HashMap<>();
    }

    public void analyzeQueryTasks(List<QueryTask> tasks) {
        for (QueryTask task : tasks) {
            for (FragmentTask ft : task.getFragmentTasks()) {
                Bag bag = this.lexicon.get(ft.getPhrase());
                if (bag == null) {
                    bag = new HashBag();
                    this.lexicon.put(ft.getPhrase(), bag);
                }

                bag.addAll(ft.getAnswers());
            }
        }
    }

    public double getWeightedLikelihood(String keyword, String answer) {
        Bag bag = this.lexicon.get(keyword);
        if (bag == null) {
            return 0.0;
        }

        return ((double) bag.getCount(answer)) / bag.size();
    }
}
