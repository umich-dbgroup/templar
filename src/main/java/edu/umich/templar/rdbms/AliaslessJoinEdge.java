package edu.umich.templar.rdbms;

/**
 * Created by cjbaik on 9/25/17.
 */
public class AliaslessJoinEdge {
    AliaslessAttribute first;
    AliaslessAttribute second;

    public AliaslessJoinEdge(JoinEdge edge) {
        this.first = new AliaslessAttribute(edge.getFirst());
        this.second = new AliaslessAttribute(edge.getSecond());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AliaslessJoinEdge that = (AliaslessJoinEdge) o;

        if (first != null ? !first.equals(that.first) : that.first != null) return false;
        return !(second != null ? !second.equals(that.second) : that.second != null);

    }

    @Override
    public int hashCode() {
        int result = first != null ? first.hashCode() : 0;
        result = 31 * result + (second != null ? second.hashCode() : 0);
        return result;
    }
}
