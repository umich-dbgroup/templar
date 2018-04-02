package edu.umich.templar.task;

public class QueryTaskResults {
    private QueryTask task;

    // Query task scores
    private boolean correctTies0;
    private boolean correctTies1;
    private double correctTiesFrac;

    // Fragment scores
    private int correctFragsTies0;
    private int correctFragsTies1;
    private double correctFragsTiesFrac;

    public QueryTaskResults(QueryTask task, boolean correctTies0, boolean correctTies1, double correctTiesFrac,
                            int correctFragsTies0, int correctFragsTies1, double correctFragsTiesFrac) {
        this.task = task;
        this.correctTies0 = correctTies0;
        this.correctTies1 = correctTies1;
        this.correctTiesFrac = correctTiesFrac;
        this.correctFragsTies0 = correctFragsTies0;
        this.correctFragsTies1 = correctFragsTies1;
        this.correctFragsTiesFrac = correctFragsTiesFrac;
    }

    public QueryTask getTask() {
        return task;
    }

    public boolean isCorrectTies0() {
        return correctTies0;
    }

    public boolean isCorrectTies1() {
        return correctTies1;
    }

    public double getCorrectTiesFrac() {
        return correctTiesFrac;
    }

    public int getCorrectFragsTies0() {
        return correctFragsTies0;
    }

    public int getCorrectFragsTies1() {
        return correctFragsTies1;
    }

    public double getCorrectFragsTiesFrac() {
        return correctFragsTiesFrac;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("QUERY > [Ties0: ");
        if (correctTies0) {
            sb.append("Y");
        } else {
            sb.append("N");
        }
        sb.append("] [Ties1: ");
        if (correctTies1) {
            sb.append("Y");
        } else {
            sb.append("N");
        }
        sb.append("] [Ties1/t: ");
        sb.append(String.format("%.2f", correctTiesFrac));
        sb.append("]\nFRAGMENT (Total: ");
        sb.append(task.size());
        sb.append(")> [Ties0: ");
        sb.append(correctFragsTies0);
        sb.append("] [Ties1: ");
        sb.append(correctFragsTies1);
        sb.append("] [Ties1/t: ");
        sb.append(String.format("%.2f", correctFragsTiesFrac));
        sb.append("]");
        return sb.toString();
    }
}
