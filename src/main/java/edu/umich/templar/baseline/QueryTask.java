package edu.umich.templar.baseline;

import java.util.ArrayList;
import java.util.List;

public class QueryTask {
    private Integer queryId;
    private List<FragmentTask> fragmentTasks;

    public QueryTask(Integer queryId) {
        this.queryId = queryId;
        this.fragmentTasks = new ArrayList<>();
    }

    public Integer getQueryId() {
        return queryId;
    }

    public void addMapping(FragmentTask fragmentTask) {
        fragmentTasks.add(fragmentTask);
    }

    public List<FragmentTask> getFragmentTasks() {
        return this.fragmentTasks;
    }

    public int size() {
        return this.fragmentTasks.size();
    }
}
