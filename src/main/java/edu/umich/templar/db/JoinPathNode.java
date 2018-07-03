package edu.umich.templar.db;

import edu.umich.templar.db.el.Relation;

import java.util.*;

public class JoinPathNode {
    private Relation rel;
    private int index;
    private Set<JoinPathNode> connected;
    private Map<JoinPathNode, JoinOn> joinOns;

    public JoinPathNode(Relation rel, int index) {
        this.rel = rel;
        this.index = index;
        this.connected = new HashSet<>();
        this.joinOns = new HashMap<>();
    }

    public void addEdge(JoinPathNode node, JoinOn joinOn) {
        this.connected.add(node);
        node.connected.add(this);
        this.joinOns.put(node, joinOn);
        node.joinOns.put(this, joinOn);
    }

    public Relation getRel() {
        return rel;
    }

    public Set<JoinPathNode> getConnected() {
        return connected;
    }

    public JoinOn getJoinOn(JoinPathNode connected) {
        return this.joinOns.get(connected);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.rel.toString());

        if (this.index > 0) {
            sb.append('#');
            sb.append(this.index);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JoinPathNode that = (JoinPathNode) o;
        return index == that.index &&
                Objects.equals(rel, that.rel);
    }

    @Override
    public int hashCode() {

        return Objects.hash(rel, index);
    }
}
