package edu.umich.templar.log.graph;

import edu.umich.templar.db.AttributeAndPredicate;
import edu.umich.templar.db.DBElement;
import edu.umich.templar.db.Relation;
import edu.umich.templar.util.Utils;

import java.util.*;

public class LogGraphTree {
    private LogGraphNode root;
    private Set<LogGraphNode> nodes;
    private Map<LogGraphNode, LogGraphNode> parents;
    private Map<LogGraphNode, Set<LogGraphNode>> children;
    private Set<LogGraphNode> leaves;

    public LogGraphTree(LogGraphNode root) {
        this.root = root;
        this.nodes = new HashSet<>();
        this.leaves = new HashSet<>();
        this.nodes.add(root);
        this.leaves.add(root);

        this.parents = new HashMap<>();
        this.children = new HashMap<>();
    }

    public Set<LogGraphNode> getNodes() {
        return nodes;
    }

    public LogGraphNode getRoot() {
        return root;
    }

    public LogGraphNode getParent(LogGraphNode child) {
        if (this.root.equals(child)) {
            Set<LogGraphNode> parents = this.children.get(this.root);
            if (parents.size() != 1) {
                throw new RuntimeException("Node is root and has more than one child, so no parent.");
            }
            Iterator<LogGraphNode> iter = parents.iterator();
            return iter.next();
        } else {
            return this.parents.get(child);
        }
    }

    public boolean hasChildren(LogGraphNode parent) {
        return this.children.get(parent) != null && !this.children.get(parent).isEmpty();
    }

    public Set<LogGraphNode> getChildren(LogGraphNode parent) {
        return this.children.get(parent);
    }

    public void addNode(LogGraphNode parent, LogGraphNode node) {
        if (!this.nodes.contains(parent)) {
            throw new RuntimeException("Parent does not exist in tree!");
        }
        this.nodes.add(node);
        this.leaves.add(node);

        Set<LogGraphNode> childrenSet = this.children.computeIfAbsent(parent, k -> new HashSet<>());
        childrenSet.add(node);

        this.parents.put(node, parent);
        if (parent.equals(this.root)) {
            if (childrenSet.size() > 1) {
                this.leaves.remove(this.root);
            }
        } else {
            this.leaves.remove(parent);
        }

    }

    public boolean isLeaf(LogGraphNode node) {
        Set<LogGraphNode> children = this.children.get(node);
        return (node.equals(this.root) && this.children.get(this.root).size() <= 1)
                || children == null || children.isEmpty();
    }

    public void removeNode(LogGraphNode node) {
        Set<LogGraphNode> children = this.children.get(node);
        LogGraphNode parent = this.parents.get(node);

        // If not root, then delete all siblings
        if (!this.root.equals(node)) {
            Set<LogGraphNode> siblings = this.children.get(parent);
            siblings.remove(node);

            if (children != null) {
                for (LogGraphNode child : children) {
                    this.parents.put(child, parent);
                    siblings.add(child);
                }
            } else {
                this.leaves.remove(node);
            }

            if (parent.equals(this.root) && siblings.size() <= 1) {
                this.leaves.add(parent);
            }
        } else {
            if (children.size() != 1) throw new RuntimeException("Cannot remove root with more than 1 child.");

            this.children.put(this.root, null);

            Iterator<LogGraphNode> iter = children.iterator();
            LogGraphNode newRoot = iter.next();
            this.root = newRoot;
            this.parents.put(newRoot, null);
        }

        this.nodes.remove(node);
    }

    public Set<LogGraphNode> getLeaves() {
        return leaves;
    }

    public double joinScore() {
        Stack<LogGraphNode> stack = new Stack<>();
        stack.push(this.root);

        List<Double> weights = new ArrayList<>();
        while (!stack.isEmpty()) {
            LogGraphNode node = stack.pop();
            if (this.hasChildren(node)) {
                for (LogGraphNode child : this.children.get(node)) {
                    // Only if both nodes are relations
                    if (child.getElement() instanceof Relation && node.getElement() instanceof Relation) {
                        weights.add(node.getWeight(child));
                    }
                    stack.push(child);
                }
            }
        }

        // Return perfect score
        if (weights.isEmpty()) return 1.0;

        // TODO: Can't figure out best way to calculate this, nor how it should play out in the scorer.
        // return Utils.geometricMean(weights);

        // Return the mean of the weights, but divide by n^2 instead of n to penalize long paths
        return Utils.mean(weights) / weights.size();
    }

    public boolean contains(DBElement el) {
        List<DBElement> toFind = new ArrayList<>();
        if (el instanceof AttributeAndPredicate) {
            toFind.add(((AttributeAndPredicate) el).getAttribute());
            toFind.add(((AttributeAndPredicate) el).getPredicate());
        } else {
            toFind.add(el);
        }

        for (LogGraphNode node : this.nodes) {
            if (toFind.contains(node.getElement())) {
                toFind.remove(node.getElement());
            }
        }
        return toFind.isEmpty();
    }

    public String debug() {
        StringBuilder sb = new StringBuilder();

        Map<LogGraphNode, Integer> depth = new HashMap<>();
        Stack<LogGraphNode> stack = new Stack<>();
        stack.push(this.root);
        depth.put(this.root, 0);

        while (!stack.isEmpty()) {
            LogGraphNode curNode = stack.pop();
            LogGraphNode parent = this.parents.get(curNode);

            Integer curDepth = depth.get(curNode);
            if (curDepth == null) {
                curDepth = depth.get(parent) + 1;
                depth.put(curNode, curDepth);
            }

            for (int i = 0; i < curDepth; i++) {
                sb.append('\t');
            }
            if (curDepth > 0) {
                sb.append('└');
                sb.append(' ');
            }

            sb.append(curNode.toString());
            if (parent != null) {
                sb.append(" (");
                sb.append(parent.getWeight(curNode));
                sb.append(')');
            }

            sb.append('\n');

            if (this.hasChildren(curNode)) {
                stack.addAll(this.children.get(curNode));
            }
        }
        return sb.toString();
    }
}
