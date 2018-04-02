package edu.umich.templar.task;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class AllQueryTaskResults {
    private List<QueryTaskResults> results;

    public AllQueryTaskResults() {
        this.results = new ArrayList<>();
    }

    public void addResult(QueryTaskResults qtr) {
        this.results.add(qtr);
    }

    public String toCSVString() {
        long allCorrectTies0 = this.results.stream()
                .map(QueryTaskResults::isCorrectTies0)
                .filter((a) -> a).count();
        long allCorrectTies1 = this.results.stream()
                .map(QueryTaskResults::isCorrectTies1)
                .filter((a) -> a).count();
        double allCorrectTiesFrac = this.results.stream()
                .map(QueryTaskResults::getCorrectTiesFrac)
                .mapToDouble(Double::doubleValue).sum();
        int totalQueries = this.results.size();

        int allCorrectFragsTies0 = this.results.stream()
                .map(QueryTaskResults::getCorrectFragsTies0)
                .mapToInt(Integer::intValue).sum();
        int allCorrectFragsTies1 = this.results.stream()
                .map(QueryTaskResults::getCorrectFragsTies1)
                .mapToInt(Integer::intValue).sum();
        double allCorrectFragsTiesFrac = this.results.stream()
                .map(QueryTaskResults::getCorrectFragsTiesFrac)
                .mapToDouble(Double::doubleValue).sum();
        int totalFrags = this.results.stream().map(QueryTaskResults::getTask).map(QueryTask::size)
                .mapToInt(Integer::intValue).sum();

        StringJoiner sj = new StringJoiner(",");
        sj.add(String.valueOf(allCorrectTies0));
        sj.add(String.valueOf(allCorrectTies1));
        sj.add(String.format("%.3f", allCorrectTiesFrac));
        sj.add(String.valueOf(totalQueries));
        sj.add(String.valueOf(allCorrectFragsTies0));
        sj.add(String.valueOf(allCorrectFragsTies1));
        sj.add(String.format("%.3f", allCorrectFragsTiesFrac));
        sj.add(String.valueOf(totalFrags));

        return sj.toString();
    }
}
