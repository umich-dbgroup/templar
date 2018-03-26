package edu.umich.templar.log.graph;

import java.util.ArrayList;
import java.util.List;

public class LogGraphPath {
    private List<LogGraphNode> nodes;

    public LogGraphPath() {
        this.nodes = new ArrayList<>();
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
}
