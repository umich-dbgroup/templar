package edu.umich.tbnalir.rdbms;

/**
 * Created by cjbaik on 6/30/17.
 */
public class Attribute {
    String name;
    String type;

    public Attribute(String name, String type) {
        this.name = name;
        this.type = type;
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
}
