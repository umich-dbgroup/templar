package edu.umich.templar.db;

public class AttributeAndPredicate extends DBElement {
    private DBElement attribute;
    private DBElement predicate;

    public AttributeAndPredicate(DBElement attribute, DBElement predicate) {
        this.attribute = attribute;
        this.predicate = predicate;
    }

    public DBElement getAttribute() {
        return attribute;
    }

    public DBElement getPredicate() {
        return predicate;
    }

    @Override
    public Relation getRelation() {
        return this.predicate.getRelation();
    }

    @Override
    public String toString() {
        return this.predicate + "," + this.attribute;
    }
}
