package edu.umich.templar.log.graph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class LogGraphNodePair {
    private List<LogGraphNode> nodes;

    public LogGraphNodePair(LogGraphNode first, LogGraphNode second) {
        this.nodes = new ArrayList<>();
        this.nodes.add(first);
        this.nodes.add(second);
        this.nodes.sort(Comparator.comparing(LogGraphNode::hashCode));
    }

    public LogGraphNode getFirst() {
        return this.nodes.get(0);
    }

    public LogGraphNode getSecond() {
        return this.nodes.get(1);
    }

    @Override
    public String toString() {
        return this.getFirst() + ":" + this.getSecond();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogGraphNodePair that = (LogGraphNodePair) o;
        return Objects.equals(nodes, that.nodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodes);
    }
}
