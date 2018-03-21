package edu.umich.templar._old.qf.pieces;

/**
 * Created by cjbaik on 10/16/17.
 */
public enum AttributeType {
    TEXT("text"), NUMBER("number"), ALL("*");

    private String text;

    AttributeType(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
