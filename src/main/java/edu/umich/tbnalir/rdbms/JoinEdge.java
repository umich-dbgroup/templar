package edu.umich.tbnalir.rdbms;

/**
 * Created by cjbaik on 9/21/17.
 */
public class JoinEdge {
    // Attributes are ALWAYS ordered alphabetically (relation name, including alias + attribute name)
    Attribute first;
    Attribute second;

    boolean selfJoin;

    public JoinEdge(Attribute first, Attribute second) {
        String firstStr = first.getRelation().getName() + first.getRelation().getAliasInt() + first.getName();
        String secondStr = second.getRelation().getName() + second.getRelation().getAliasInt() + second.getName();

        if (firstStr.compareTo(secondStr) <= 0) {
            this.first = first;
            this.second = second;
        } else {
            this.first = second;
            this.second = first;
        }

        this.selfJoin = first.hasSameRelationNameAndNameAs(second);
    }

    public boolean isSelfJoin() {
        return selfJoin;
    }

    public Attribute getFirst() {
        return first;
    }

    public Attribute getSecond() {
        return second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JoinEdge joinEdge = (JoinEdge) o;

        if (first != null ? !first.equals(joinEdge.first) : joinEdge.first != null) return false;
        return !(second != null ? !second.equals(joinEdge.second) : joinEdge.second != null);

    }

    @Override
    public int hashCode() {
        int result = first != null ? first.hashCode() : 0;
        result = 31 * result + (second != null ? second.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return this.first.toString() + " - " + this.second.toString();
    }
}
