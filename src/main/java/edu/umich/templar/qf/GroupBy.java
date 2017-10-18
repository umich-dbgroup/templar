package edu.umich.templar.qf;

import edu.umich.templar.rdbms.Attribute;

/**
 * Created by cjbaik on 10/18/17.
 */
public class GroupBy extends QueryFragment {
    public GroupBy(Attribute attr) {
        super();
        this.attribute = attr;
    }

    @Override
    public String toString() {
        return "group[" + this.attribute.toString() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GroupBy groupBy = (GroupBy) o;

        return !(attribute != null ? !attribute.equals(groupBy.attribute) : groupBy.attribute != null);

    }

    @Override
    public int hashCode() {
        return attribute != null ? attribute.hashCode() : 0;
    }
}
