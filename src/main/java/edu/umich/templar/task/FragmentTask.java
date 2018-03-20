package edu.umich.templar.task;

import java.util.List;

public class FragmentTask {
    private String phrase;
    private String op;
    private String type;
    private List<String> functions;
    private Boolean groupBy;
    private String answer;

    public FragmentTask(String phrase, String op, String type, List<String> functions, Boolean groupBy, String answer) {
        this.phrase = phrase;
        this.op = op;
        this.type = type;
        this.functions = functions;
        this.groupBy = groupBy;
        this.answer = answer;
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

    public String getAnswer() {
        return answer;
    }
}
