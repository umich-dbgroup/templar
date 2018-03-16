package edu.umich.templar.baseline;

public class FragmentTask {
    private String phrase;
    private String op;
    private String type;
    private String answer;

    public FragmentTask(String phrase, String op, String type, String answer) {
        this.phrase = phrase;
        this.op = op;
        this.type = type;
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

    public String getAnswer() {
        return answer;
    }
}
