package edu.umich.templar._old.qf;

import edu.umich.templar._old.dataStructure.ParseTreeNode;
import edu.umich.templar._old.rdbms.Relation;

/**
 * Created by cjbaik on 10/17/17.
 */
public class RelationFragment extends QueryFragment {
    Relation relation;

    public RelationFragment(Relation relation) {
        super();
        this.relation = relation;
    }

    public RelationFragment(ParseTreeNode node, Relation relation) {
        super(node);
        this.relation = relation;
    }

    public Relation getRelation() {
        return relation;
    }

    @Override
    public String toString() {
        return "rel[" + this.relation.toString() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RelationFragment that = (RelationFragment) o;

        return !(relation != null ? !relation.equals(that.relation) : that.relation != null);

    }

    @Override
    public int hashCode() {
        return relation != null ? relation.hashCode() : 0;
    }
}
