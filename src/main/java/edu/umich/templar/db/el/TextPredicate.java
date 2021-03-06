package edu.umich.templar.db.el;

import java.io.Serializable;

/**
 * Created by cjbaik on 1/31/18.
 */
public class TextPredicate extends DBElement implements Serializable {
    Attribute attribute;
    String value;

    public TextPredicate(Attribute attr, String value) {
        this.attribute = attr;
        this.value = value;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public String getValue() {
        return value;
    }

    @Override
    public Relation getRelation() {
        return this.attribute.getRelation();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TextPredicate value1 = (TextPredicate) o;

        if (attribute != null ? !attribute.equals(value1.attribute) : value1.attribute != null) return false;
        return !(value != null ? !value.equals(value1.value) : value1.value != null);

    }

    @Override
    public int hashCode() {
        int result = attribute != null ? attribute.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return this.attribute.toString() + " = '" + this.value + "'";
    }
}
