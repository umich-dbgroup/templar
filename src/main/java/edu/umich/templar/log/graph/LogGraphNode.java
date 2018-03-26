package edu.umich.templar.log.graph;

import edu.umich.templar.db.DBElement;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LogGraphNode {
    private DBElement element;

    private Map<LogGraphNode, Double> connected;

    public LogGraphNode(DBElement element) {
        this.element = element;
        this.connected = new HashMap<>();
    }

    public DBElement getElement() {
        return element;
    }

    public Double getWeight(LogGraphNode node) {
        return this.connected.get(node);
    }

    public void addEdge(LogGraphNode node, Double weight) {
        this.connected.put(node, weight);
        node.connected.put(this, weight);
    }

    public Map<LogGraphNode, Double> getConnected() {
        return connected;
    }

    public boolean isConnected(LogGraphNode node) {
        return this.connected.get(node) != null;
    }

    @Override
    public String toString() {
        return this.element.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogGraphNode that = (LogGraphNode) o;
        return Objects.equals(element, that.element);
    }

    @Override
    public int hashCode() {
        return Objects.hash(element);
    }
}
