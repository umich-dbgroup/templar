package edu.umich.tbnalir.rdbms;

import edu.umich.tbnalir.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by cjbaik on 6/30/17.
 */
public class Relation {
    String name;
    RelationType type;
    Map<String, Attribute> attributes;

    List<Attribute> rankedAttributes; // attributes ranked by entropy

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

        this.rankedAttributes = null;
    }

    public List<Attribute> rankAttributesByEntropy(RDBMS db) {
        if (this.rankedAttributes != null) return this.rankedAttributes;

        List<Attribute> ranked = new ArrayList<>();

        int relationSize = db.getRelationSize(this);
        for (Attribute attr : this.attributes.values()) {
            Map<String, Integer> valToOccurrence = db.getAttrDistinctCount(this, attr);
            List<Double> probs = new ArrayList<>();

            for (Map.Entry<String, Integer> e : valToOccurrence.entrySet()) {
                probs.add((double) e.getValue() / (double) relationSize);
            }

            double entropy = Utils.entropy(probs);
            attr.setEntropy(entropy);
        }

        // sort in descending order of entropy
        ranked.sort((a, b) -> Double.compare(b.getEntropy(), a.getEntropy()));

        // cache information
        this.rankedAttributes = ranked;

        return ranked;
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

    @Override
    public String toString() {
        return this.getName();
    }
}
