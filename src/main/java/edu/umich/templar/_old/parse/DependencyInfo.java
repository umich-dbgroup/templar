package edu.umich.templar._old.parse;

/**
 * Created by cjbaik on 9/5/17.
 */
public class DependencyInfo {
    Word govWord;           // governor word
    Word depWord;           // dependent word
    String relationship;    // relationship

    public DependencyInfo(Word govWord, Word depWord, String relationship) {
        this.govWord = govWord;
        this.depWord = depWord;
        this.relationship = relationship;
    }

    public Word getGovWord() {
        return govWord;
    }

    public void setGovWord(Word govWord) {
        this.govWord = govWord;
    }

    public Word getDepWord() {
        return depWord;
    }

    public void setDepWord(Word depWord) {
        this.depWord = depWord;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }
}
