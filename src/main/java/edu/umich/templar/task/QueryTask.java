package edu.umich.templar.task;

import java.util.ArrayList;
import java.util.List;

public class QueryTask {
    private Integer queryId;
    private List<FragmentTask> fragmentTasks;
    private List<String> joinAnswers;

    public QueryTask(Integer queryId) {
        this.queryId = queryId;
        this.fragmentTasks = new ArrayList<>();
        this.joinAnswers = null;
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

    public List<String> getJoinAnswers() {
        return joinAnswers;
    }

    public void setJoinAnswers(List<String> joinAnswers) {
        this.joinAnswers = joinAnswers;
    }

    public int size() {
        return this.fragmentTasks.size();
    }

    public long sizeWithoutRels() {
        return this.fragmentTasks.stream().filter((a) -> !a.getType().equalsIgnoreCase("rel")).count();
    }
}
