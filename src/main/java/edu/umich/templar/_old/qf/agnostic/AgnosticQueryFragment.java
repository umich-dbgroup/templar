package edu.umich.templar._old.qf.agnostic;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by cjbaik on 10/16/17.
 */
public class AgnosticQueryFragment {
    int count;                                                // number of total appearances;
    Map<AgnosticQueryFragment, Integer> cooccurrence;         // co-occurrence with other query fragments

    public AgnosticQueryFragment() {
        this.count = 0;
        this.cooccurrence = new HashMap<>();
    }

    public Map<AgnosticQueryFragment, Integer> getCooccurrenceMap() {
        return cooccurrence;
    }

    public int getCooccurrence(AgnosticQueryFragment other) {
        Integer count = this.cooccurrence.get(other);
        if (count == null) {
            return 0;
        }
        return count;
    }

    public int getCount() {
        return this.count;
    }

    public void incrementCount() {
        this.count++;
    }

    public void incrementCooccurrence(AgnosticQueryFragment other) {
        Integer count = this.cooccurrence.get(other);
        if (count == null) {
            count = 1;
        } else {
            count++;
        }
        this.cooccurrence.put(other, count);
    }
}
