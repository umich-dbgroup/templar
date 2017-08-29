package edu.umich.tbnalir.rdbms;

import com.esotericsoftware.minlog.Log;
import edu.umich.tbnalir.util.Utils;
import net.sf.jsqlparser.schema.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by cjbaik on 6/30/17.
 */
public class Relation {
    String name;
    RelationType type;
    Map<String, Attribute> attributes;

    Table table;

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

        this.table = null;
        this.rankedAttributes = null;
    }

    public List<Attribute> rankAttributesByEntropy(RDBMS db) {
        if (this.rankedAttributes != null) return this.rankedAttributes;

        Log.info("Calculating attribute entropy for " + this.name + "...");
        List<Attribute> ranked = new ArrayList<>();

        int relationSize = db.getRelationSize(this);
        for (Attribute attr : this.attributes.values()) {
            if (attr.getEntropy() == null) {
                Map<String, Integer> valToOccurrence = db.getAttrDistinctCount(this, attr);
                List<Double> probs = new ArrayList<>();

                for (Map.Entry<String, Integer> e : valToOccurrence.entrySet()) {
                    probs.add((double) e.getValue() / (double) relationSize);
                }

                double entropy = Utils.entropy(probs);
                attr.setEntropy(entropy);
            }

            ranked.add(attr);
        }

        // sort in descending order of entropy
        ranked.sort((a, b) -> Double.compare(b.getEntropy(), a.getEntropy()));

        // Debug logging
        for (Attribute attr : ranked) {
            Log.info("Relation: " + this.name + ", Attribute: " + attr.getName() + ", Entropy: " + attr.getEntropy());
        }

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

    public Table getTable() {
        if (this.table == null) this.table = new Table(this.name);
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
