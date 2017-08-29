package edu.umich.tbnalir.rdbms;

import net.sf.jsqlparser.schema.Column;

/**
 * Created by cjbaik on 6/30/17.
 */
public class Attribute {
    Relation relation;
    String name;
    String type;

    Boolean fk; // if foreign key
    Boolean pk; // if primary key

    Double entropy; // information entropy of this attribute

    Column column; // get JSqlParser column object

    public Attribute(String name, String type) {
        this.name = name;
        this.type = type;

        this.fk = false;
        this.pk = false;

        this.column = null;
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

    public Boolean getFk() {
        return fk;
    }

    public void setFk(Boolean fk) {
        this.fk = fk;
    }

    public Boolean getPk() {
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
            this.column = new Column(this.relation.getTable(), this.name);
        }
        return column;
    }

    public void setColumn(Column column) {
        this.column = column;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
