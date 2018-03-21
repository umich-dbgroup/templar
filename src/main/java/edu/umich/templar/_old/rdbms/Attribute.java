package edu.umich.templar._old.rdbms;

import edu.umich.templar._old.qf.pieces.AttributeType;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

/**
 * Created by cjbaik on 6/30/17.
 */
public class Attribute {
    Relation relation;
    String name;
    String type;

    String question;    // associated question word (e.g. "who" if exists)

    Boolean fk; // if foreign key
    Boolean pk; // if primary key

    Double entropy; // information entropy of this attribute

    Column column; // get JSqlParser column object

    public Attribute(String name, String type) {
        this.name = name;
        this.type = type;

        this.question = null;

        this.fk = false;
        this.pk = false;

        this.column = null;
    }

    public Attribute(Attribute other) {
        this.name = other.name;
        this.type = other.type;

        this.question = other.question;

        this.fk = other.fk;
        this.pk = other.pk;

        this.entropy = other.entropy;
    }

    public static Attribute allColumnsAttr() {
        Attribute attr = new Attribute("*", "*");
        return attr;
    }

    public AttributeType getAttributeType() {
        if (this.getType().equals("text")) {
            return AttributeType.TEXT;
        }
        if (this.getType().equals("int") || this.getType().equals("double")) {
            return AttributeType.NUMBER;
        }
        throw new RuntimeException("Cannot find attribute type: " + this.type);
    }

    public boolean hasSameRelationAs(Attribute other) {
        return this.relation.equals(other.relation);
    }

    public boolean hasSameRelationNameAndNameAs(Attribute other) {
        return this.relation.name.equals(other.relation.name) &&
                this.name.equals(other.name);
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

    public Relation getRelation() {
        return relation;
    }

    public void setRelation(Relation relation) {
        this.relation = relation;
    }

    public Boolean isFk() {
        return fk;
    }

    public void setFk(Boolean fk) {
        this.fk = fk;
    }

    public Boolean isPk() {
        return pk;
    }

    public void setPk(Boolean pk) {
        this.pk = pk;
    }

    public Double getEntropy() {
        return entropy;
    }

    public void setEntropy(Double entropy) {
        this.entropy = entropy;
    }

    public Column getColumn() {
        if (this.column == null) {
            if (this.relation instanceof Function) {
                return null;
            }
            this.column = new Column((Table) this.relation.getFromItem(), this.name);
        }
        return column;
    }

    public void setColumn(Column column) {
        this.column = column;
    }

    public String toStringWithConsistentRelation() {
        return this.relation.getName() + "." + this.name;
    }

    public Attribute canonical() {
        Attribute attr = new Attribute(this);
        if (this.relation != null) {
            Relation canonicalRel = new Relation(this.relation);
            canonicalRel.setAliasInt(0);
            attr.setRelation(canonicalRel);
        }
        return attr;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    @Override
    public String toString() {
        if (this.relation == null) {
            return this.name;
        }
        return this.relation.toString() + "." + this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute attribute = (Attribute) o;

        if (relation != null ? !relation.equals(attribute.relation) : attribute.relation != null) return false;
        return !(name != null ? !name.equals(attribute.name) : attribute.name != null);

    }

    @Override
    public int hashCode() {
        int result = relation != null ? relation.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
