package edu.umich.tbnalir.rdbms;

import com.esotericsoftware.minlog.Log;
import edu.umich.tbnalir.util.Utils;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;

import java.util.*;

/**
 * Created by cjbaik on 6/30/17.
 */
public class Relation {
    String name;
    RelationType type;
    Attribute primaryAttr;
    Map<String, Attribute> attributes;

    Integer aliasInt; // aliasInt

    boolean joinTable;  // true if it is a join table

    boolean weak;       // true if it is a weak entity
    String parent;      // name of parent relation if weak entity

    FromItem fromItem;

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

        this.primaryAttr = null;

        this.joinTable = false;
        this.weak = false;
        this.parent = null;
        this.fromItem = null;
        this.rankedAttributes = null;

        this.aliasInt = 0;
    }

    public Relation(Relation other) {
        this.name = other.name;
        this.type = other.type;

        this.attributes = new HashMap<>();
        for (Map.Entry<String, Attribute> e : other.attributes.entrySet()) {
            Attribute copyAttr = new Attribute(e.getValue());
            copyAttr.setRelation(this);
            this.attributes.put(e.getKey(), copyAttr);
        }

        if (other.primaryAttr != null) {
            this.primaryAttr = this.attributes.get(other.primaryAttr.getName());
        }

        this.joinTable = other.joinTable;
        this.weak = other.weak;
        this.parent = other.parent;
        this.fromItem = null;

        this.rankedAttributes = new ArrayList<>();
        for (Attribute otherAttr : other.rankedAttributes) {
            this.rankedAttributes.add(this.attributes.get(otherAttr.getName()));
        }

        this.aliasInt = 0;
    }

    public Integer getAliasInt() {
        return aliasInt;
    }

    public void setAliasInt(Integer aliasInt) {
        this.aliasInt = aliasInt;
    }

    public boolean isJoinTable() {
        return joinTable;
    }

    public void setJoinTable(boolean joinTable) {
        this.joinTable = joinTable;
    }

    public List<Attribute> rankAttributesByEntropy(RDBMS db) {
        if (this.rankedAttributes != null) return this.rankedAttributes;

        Log.info("Calculating attribute entropy for " + this.name + "...");
        List<Attribute> ranked = new ArrayList<>();

        int relationSize = db.getRelationSize(this);
        for (Attribute attr : this.attributes.values()) {
            if (attr.getEntropy() == null) {
                List<Integer> distinctCounts = db.getDistinctAttrCounts(this, attr);
                List<Double> probs = new ArrayList<>();

                for (Integer count : distinctCounts) {
                    probs.add((double) count / (double) relationSize);
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

    public List<Attribute> getRankedAttributes() {
        if (this.rankedAttributes == null) {
            throw new RuntimeException("Call rankAttributesByEntropy() before trying to get ranked attributes.");
        }
        return this.rankedAttributes;
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

    public Attribute getPrimaryAttr() {
        return primaryAttr;
    }

    public void setPrimaryAttr(Attribute primaryAttr) {
        this.primaryAttr = primaryAttr;
    }

    public void resetFromItem() {
        this.fromItem = null;
    }

    public boolean isWeak() {
        return weak;
    }

    public void setWeak(boolean weak) {
        this.weak = weak;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public FromItem getFromItem() {
        if (this.fromItem == null) {
            if (this instanceof Function) {
                this.fromItem = Utils.convertFunctionToTableFunction((Function) this);
            } else {
                Alias alias = new Alias(this.name + "_" + aliasInt);
                this.fromItem = new Table(this.name);
                this.fromItem.setAlias(alias);
            }
        }

        return fromItem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Relation relation = (Relation) o;

        if (name != null ? !name.equals(relation.name) : relation.name != null) return false;
        return !(aliasInt != null ? !aliasInt.equals(relation.aliasInt) : relation.aliasInt != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (aliasInt != null ? aliasInt.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return this.name + "_" + this.aliasInt;
    }
}
