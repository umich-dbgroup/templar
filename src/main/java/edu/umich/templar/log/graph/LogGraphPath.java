package edu.umich.templar.log.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LogGraphPath {
    private List<LogGraphNode> nodes;

    public LogGraphPath() {
        this.nodes = new ArrayList<>();
    }

    public LogGraphPath(LogGraphPath other) {
        this.nodes = new ArrayList<>(other.nodes);
    }

    public int length() {
        return this.nodes.size();
    }

    public List<LogGraphNode> getNodes() {
        return nodes;
    }

    public void addNode(LogGraphNode node) {
        if (!this.nodes.isEmpty()) {
            LogGraphNode lastNode = this.nodes.get(this.nodes.size() - 1);
            if (!lastNode.isConnected(node)) {
                throw new RuntimeException(lastNode + " is not connected to " + node);
            }
        }
        this.nodes.add(node);
    }

    public double distance() {
        double distance = 0.0;

        for (int i = 1; i < this.nodes.size(); i++) {
            LogGraphNode thisNode = this.nodes.get(i);
            LogGraphNode prevNode = this.nodes.get(i-1);

            distance += thisNode.getWeight(prevNode);
        }

        return distance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogGraphPath that = (LogGraphPath) o;
        return Objects.equals(nodes, that.nodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodes);
    }
}
