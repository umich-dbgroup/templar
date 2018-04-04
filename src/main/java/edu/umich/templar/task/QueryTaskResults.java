package edu.umich.templar.task;

public class QueryTaskResults {
    private QueryTask task;

    // Overall query keyword mapping scores
    private boolean correctTies0;
    private boolean correctTies1;
    private double correctTiesFrac;

    // Fragment scores
    private int correctFragsTies0;
    private int correctFragsTies1;
    private double correctFragsTiesFrac;

    // Join path correct
    private boolean correctJoinTies0;
    private boolean correctJoinTies1;
    private double correctJoinTiesFrac;

    public QueryTaskResults(QueryTask task, boolean correctTies0, boolean correctTies1, double correctTiesFrac,
                            int correctFragsTies0, int correctFragsTies1, double correctFragsTiesFrac,
                            boolean correctJoinTies0, boolean correctJoinTies1, double correctJoinTiesFrac) {
        this.task = task;
        this.correctTies0 = correctTies0;
        this.correctTies1 = correctTies1;
        this.correctTiesFrac = correctTiesFrac;
        this.correctFragsTies0 = correctFragsTies0;
        this.correctFragsTies1 = correctFragsTies1;
        this.correctFragsTiesFrac = correctFragsTiesFrac;
        this.correctJoinTies0 = correctJoinTies0;
        this.correctJoinTies1 = correctJoinTies1;
        this.correctJoinTiesFrac = correctJoinTiesFrac;
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

    public boolean isCorrectJoinTies0() {
        return correctJoinTies0;
    }

    public boolean isCorrectJoinTies1() {
        return correctJoinTies1;
    }

    public double getCorrectJoinTiesFrac() {
        return correctJoinTiesFrac;
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
        sb.append("]\nJOIN PATH > [Ties0: ");
        if (correctJoinTies0) {
            sb.append("Y");
        } else {
            sb.append("N");
        }
        sb.append("] [Ties1: ");
        if (correctJoinTies1) {
            sb.append("Y");
        } else {
            sb.append("N");
        }
        sb.append("] [Ties1/t: ");
        sb.append(String.format("%.2f", correctJoinTiesFrac));
        sb.append("]");
        return sb.toString();
    }
}
