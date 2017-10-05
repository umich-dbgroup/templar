package edu.umich.templar.rdbms;

/**
 * Created by cjbaik on 6/30/17.
 */
public class FunctionParameter {
    String name;
    String type;
    Integer index;

    public FunctionParameter(String name, String type, Integer index) {
        this.name = name;
        this.type = type;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

}
