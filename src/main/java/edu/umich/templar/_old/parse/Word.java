package edu.umich.templar._old.parse;

/**
 * Created by cjbaik on 9/5/17.
 */
public class Word {
    Integer index;  // index of text in sentence
    String text;
    String pos;     // Part-of-speech

    public Word(Integer index, String text, String pos) {
        this.index = index;
        this.text = text;
        this.pos = pos;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }
}
