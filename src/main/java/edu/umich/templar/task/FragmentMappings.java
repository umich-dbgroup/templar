package edu.umich.templar.task;

import edu.umich.templar.db.MatchedDBElement;

import java.util.List;

public class FragmentMappings {
    private FragmentTask task;
    private List<MatchedDBElement> cands;

    public FragmentMappings(FragmentTask task, List<MatchedDBElement> cands) {
        this.task = task;
        this.cands = cands;
    }

    public FragmentTask getTask() {
        return task;
    }

    public MatchedDBElement get(int i) {
        return this.cands.get(i);
    }

    public int size() {
        return this.cands.size();
    }
}
