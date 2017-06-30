package edu.umich.tbnalir.rdbms;

import java.util.Map;

/**
 * Created by cjbaik on 6/30/17.
 */
public class Relation {
    String name;
    RelationType type;
    Map<String, Attribute> attributes;

    public Relation(String name, String type, Map<String, Attribute> attributes) {
        this.name = name;

        if (type.equals("relation")) {
            this.type = RelationType.RELATION;
        } else if (type.equals("view")) {
            this.type = RelationType.VIEW;
        } else if (type.equals("function")) {
            this.type = RelationType.FUNCTION;
        } else {
            throw new IllegalArgumentException("Invalid type for relation: <" + type + "> detected.");
        }

        this.attributes = attributes;
        for (Map.Entry<String, Attribute> e : attributes.entrySet()) {
            e.getValue().setRelation(this);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RelationType getType() {
        return type;
    }

    public void setType(RelationType type) {
        this.type = type;
    }

    public Map<String, Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Attribute> attributes) {
        this.attributes = attributes;
    }
}
