package edu.umich.templar.db;

import edu.umich.templar.log.graph.LogGraphNode;

import java.util.*;

public class JoinPath {
    private Set<JoinPathNode> nodes;

    public JoinPath() {
        this.nodes = new HashSet<>();
    }

    public void addNode(JoinPathNode node) {
        this.nodes.add(node);
    }

    public JoinPathNode getFirstAlphabeticalNode() {
        if (this.nodes.isEmpty()) return null;

        List<JoinPathNode> nodes = new ArrayList<>(this.nodes);
        nodes.sort(Comparator.comparing(JoinPathNode::toString));
        return nodes.get(0);
    }

    public String debug() {
        StringBuilder sb = new StringBuilder();

        JoinPathNode first = this.getFirstAlphabeticalNode();

        Map<JoinPathNode, Integer> depth = new HashMap<>();
        Stack<JoinPathNode> stack = new Stack<>();
        stack.push(first);
        depth.put(first, 0);

        while (!stack.isEmpty()) {
            JoinPathNode curNode = stack.pop();

            Integer curDepth = depth.get(curNode);

            for (JoinPathNode other : curNode.getConnected()) {
                Integer otherDepth = depth.get(other);

                if (otherDepth == null) {
                    // Other has not been traversed, push to stack
                    stack.push(other);
                } else if (curDepth == null){
                    // Other has already been traversed, increase depth of this
                    curDepth = otherDepth + 1;
                }

            }

            depth.put(curNode, curDepth);

            for (int i = 0; i < curDepth; i++) {
                sb.append('\t');
            }
            if (curDepth > 0) {
                sb.append('└');
                sb.append(' ');
            }
            sb.append(curNode.toString());
            sb.append('\n');
        }
        return sb.toString();
    }

    public String toStringRecursive(JoinPathNode node, JoinPathNode parent) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.toString());

        List<JoinPathNode> sortedConnected = new ArrayList<>(node.getConnected());
        sortedConnected.remove(parent);
        if (!sortedConnected.isEmpty()) {
            sortedConnected.sort(Comparator.comparing(JoinPathNode::toString));
            sb.append('(');
            StringJoiner sj = new StringJoiner(",");
            for (JoinPathNode other : sortedConnected) {
                sj.add(this.toStringRecursive(other, node));
            }
            sb.append(sj.toString());
            sb.append(')');
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        JoinPathNode first = this.getFirstAlphabeticalNode();
        return this.toStringRecursive(first, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JoinPath joinPath = (JoinPath) o;
        return Objects.equals(nodes, joinPath.nodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodes);
    }
}
