package edu.umich.templar.task;

import java.util.List;

public class FragmentTask {
    private String phrase;
    private String op;
    private String type;
    private List<String> functions;
    private Boolean groupBy;
    private List<String> answers;

    public FragmentTask(String phrase, String op, String type, List<String> functions, Boolean groupBy, List<String> answers) {
        this.phrase = phrase;
        this.op = op;
        this.type = type;
        this.functions = functions;
        this.groupBy = groupBy;
        this.answers = answers;
    }

    public String getPhrase() {
        return phrase;
    }

    public String getOp() {
        return op;
    }

    public String getType() {
        return type;
    }

    public List<String> getFunctions() {
        return functions;
    }

    public Boolean getGroupBy() {
        return groupBy;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public String getKeyString() {
        return this.phrase + ":" + this.op + ":" + this.type + ":"
                + String.join(",", this.functions) + ":" + this.groupBy.toString();
    }

    public void setType(String type) {
        this.type = type;
    }
}
