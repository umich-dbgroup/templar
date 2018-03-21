package edu.umich.templar._old.rdbms;

/**
 * Created by cjbaik on 9/25/17.
 */
public class AliaslessAttribute {
    String relation;
    String attribute;

    public AliaslessAttribute(Attribute attr) {
        this.relation = attr.getRelation().getName();
        this.attribute = attr.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AliaslessAttribute that = (AliaslessAttribute) o;

        if (relation != null ? !relation.equals(that.relation) : that.relation != null) return false;
        return !(attribute != null ? !attribute.equals(that.attribute) : that.attribute != null);

    }

    @Override
    public int hashCode() {
        int result = relation != null ? relation.hashCode() : 0;
        result = 31 * result + (attribute != null ? attribute.hashCode() : 0);
        return result;
    }
}
