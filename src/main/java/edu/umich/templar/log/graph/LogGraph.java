package edu.umich.templar.log.graph;

import edu.umich.templar.db.el.*;
import edu.umich.templar.db.Database;
import edu.umich.templar.log.LogCountGraph;

import java.util.*;

public class LogGraph {
    private Database db;
    private LogCountGraph logCountGraph;

    private Map<DBElement, LogGraphNode> elementMap;
    private Set<LogGraphNode> nodes;

    private Map<LogGraphNodePair, LogGraphPath> shortestPaths;

    public LogGraph(Database db, LogCountGraph logCountGraph) {
        this.db = db;
        this.elementMap = new HashMap<>();
        this.nodes = new HashSet<>();
        this.logCountGraph = logCountGraph;

        this.shortestPaths = new HashMap<>();

        this.init(logCountGraph);
    }

    public LogGraph(Database db) {
        this.db = db;
        this.elementMap = new HashMap<>();
        this.nodes = new HashSet<>();
        this.logCountGraph = null;

        this.shortestPaths = new HashMap<>();
        this.initFromSchemaGraph();
    }

    private LogGraph deepClone(boolean schemaGraphOnly) {
        LogGraph cloned;
        if (this.logCountGraph != null) {
            cloned = new LogGraph(this.db, this.logCountGraph);
        } else {
            cloned = new LogGraph(this.db);
        }

        cloned.nodes = new HashSet<>();
        cloned.elementMap = new HashMap<>();
        for (Map.Entry<DBElement, LogGraphNode> e : this.elementMap.entrySet()) {
            LogGraphNode clonedNode = cloned.elementMap.get(e.getKey());
            if (clonedNode == null) {
                clonedNode = new LogGraphNode(e.getKey());
            }

            for (Map.Entry<LogGraphNode, Double> nodeEntry : e.getValue().getConnected().entrySet()) {
                LogGraphNode existingClonedNode = cloned.elementMap.get(nodeEntry.getKey().getElement());
                if (existingClonedNode == null) {
                    existingClonedNode = new LogGraphNode(nodeEntry.getKey().getElement());
                    cloned.nodes.add(existingClonedNode);
                    cloned.elementMap.put(nodeEntry.getKey().getElement(), existingClonedNode);
                }
                if (schemaGraphOnly) {
                    clonedNode.addEdge(existingClonedNode, 1.0);
                } else {
                    clonedNode.addEdge(existingClonedNode, nodeEntry.getValue());
                }
            }

            cloned.nodes.add(clonedNode);
            cloned.elementMap.put(e.getKey(), clonedNode);
        }

        cloned.shortestPaths = new HashMap<>(this.shortestPaths);
        return cloned;
    }

    public LogGraph deepClone() {
        return deepClone(false);
    }

    public LogGraph schemaGraphOnly() {
        return deepClone(true);
    }

    private void initFromSchemaGraph() {
        // Add all relations into graph at least
        for (Relation rel : this.db.getAllRelations()) {
            this.getOrAddNode(rel);
        }
    }

    // Initialize from LogCountGraph
    private void init(LogCountGraph logCountGraph) {
        // Add each node
        for (Map.Entry<DBElement, Integer> e : logCountGraph.getNodes().entrySet()) {
            DBElement el = e.getKey();
            this.getOrAddNode(el);
        }

        // Add each edge (only those which exist in the schema graph), and set weight to Dice score
        for (Map.Entry<DBElementPair, Integer> e : logCountGraph.getEdges().entrySet()) {
            DBElement firstEl = e.getKey().getFirst();
            DBElement secondEl = e.getKey().getSecond();

            boolean attributeEdge = (!(firstEl instanceof Relation) && firstEl.getRelation().equals(secondEl))
                    || (!(secondEl instanceof Relation) && secondEl.getRelation().equals(firstEl));
            boolean joinEdge = firstEl instanceof Relation && secondEl instanceof Relation
                    && this.db.hasJoin((Relation) firstEl, (Relation) secondEl);

            LogGraphNode first = this.getOrAddNode(firstEl);
            LogGraphNode second = this.getOrAddNode(secondEl);

            // if (attributeEdge || joinEdge) {
            if (joinEdge) {
                double dice = (2.0 * e.getValue()) / (logCountGraph.count(firstEl) + logCountGraph.count(secondEl));
                double diceDistance = 1.0 - dice;
                first.addEdge(second, diceDistance);
            }
        }
    }

    public DBElement modifyElementForLevel(DBElement el) {
        return this.logCountGraph.modifyElementForLevel(el);
    }

    public int count(DBElement el) {
        return this.logCountGraph.count(el);
    }

    public int cooccur(DBElementPair pair) {
        return this.logCountGraph.cooccur(pair);
    }

    private void propagateFork(DuplicateDBElement dupl) {
        LogGraphNode duplNode = this.getOrAddNode(dupl);
        LogGraphNode origNode = this.getOrAddNode(dupl.getEl());

        Set<LogGraphNode> origVisited = new HashSet<>();
        Stack<LogGraphNode> origStack = new Stack<>();
        origStack.push(origNode);
        Stack<LogGraphNode> duplStack = new Stack<>();
        duplStack.push(duplNode);

        while (!origStack.isEmpty()) {
            LogGraphNode origCurNode = origStack.pop();
            LogGraphNode duplCurNode = duplStack.pop();

            origVisited.add(origCurNode);

            boolean origCurIsRel = origCurNode.getElement() instanceof Relation;

            for (Map.Entry<LogGraphNode, Double> e : origCurNode.getConnected().entrySet()) {
                LogGraphNode origNext = e.getKey();

                // If origNext is already traversed, skip!
                if (origVisited.contains(origNext)) continue;

                // If connected node is not relation, why bother?
                boolean origNextIsRel = origNext.getElement() instanceof Relation;
                if (!origNextIsRel) continue;

                boolean isTerminalFKPKEdge = false;
                if (origCurIsRel) {
                    isTerminalFKPKEdge = this.db.isFP((Relation) origCurNode.getElement(),
                            (Relation) origNext.getElement());
                }

                if (isTerminalFKPKEdge) {
                    // Don't duplicate relation, just link duplicate to it, and terminate
                    duplCurNode.addEdge(e.getKey(), e.getValue());
                } else {
                    // Duplicate next node and edge, add to stack to traverse
                    DuplicateDBElement duplEl = this.duplicate(origNext.getElement());
                    LogGraphNode duplNext = this.getOrAddNode(duplEl);
                    duplCurNode.addEdge(duplNext, e.getValue());

                    origStack.push(origNext);
                    duplStack.push(duplNext);
                }
            }
        }
    }

    private DuplicateDBElement duplicate(DBElement el) {
        int index = 1;
        DuplicateDBElement dupl;
        do {
            dupl = new DuplicateDBElement(index++, el);
        } while (this.elementMap.containsKey(dupl));
        return dupl;
    }

    // Forks schema graph for points given as needed.
    // WARNING: irreversibly changes this LogGraph! recommend use on a "deepClone()"d LogGraph.
    public void forkSchemaGraph(List<DBElement> points, List<DBElement> ignoreDuplicates) {
        // Check for duplicates
        Map<Attribute, List<DBElement>> duplicatesCheck = new HashMap<>();
        for (DBElement point : points) {
            if (ignoreDuplicates.contains(point)) continue;

            Attribute attr = null;
            if (point instanceof AggregatedAttribute) {
                attr = ((AggregatedAttribute) point).getAttr();
            } else if (point instanceof AggregatedPredicate) {
                attr = ((AggregatedPredicate) point).getAttr();
            } else if (point instanceof Attribute) {
                attr = (Attribute) point;
            } else if (point instanceof GroupedAttribute) {
                attr = ((GroupedAttribute) point).getAttr();
            } else if (point instanceof NumericPredicate) {
                attr = ((NumericPredicate) point).getAttr();
            } else if (point instanceof TextPredicate) {
                attr = ((TextPredicate) point).getAttribute();
            } else if (point instanceof Relation) {
                // No-op
            } else {
                throw new RuntimeException("Unexpected DBElement type for: " + point);
            }
            if (attr != null) {
                List<DBElement> attrList = duplicatesCheck.computeIfAbsent(attr, k -> new ArrayList<>());
                attrList.add(point);
            }
        }
        for (Map.Entry<Attribute, List<DBElement>> e : duplicatesCheck.entrySet()) {
            // If there are any duplicates, we propagate the fork
            if (e.getValue().size() > 1) {
                for (int i = 1; i < e.getValue().size(); i++) {
                    // First, convert to DuplicateDBElement and replace in points
                    DBElement point = e.getValue().get(i);
                    DuplicateDBElement dupl = this.duplicate(point);
                    points.remove(point);
                    points.add(dupl);

                    this.propagateFork(dupl);
                }
            }
        }
    }

    private LogGraphNode getOrAddNode(DBElement el) {
        LogGraphNode node = this.elementMap.get(el);

        if (node != null) return node;
        else {
            node = new LogGraphNode(el);
            this.elementMap.put(el, node);
            this.nodes.add(node);
        }

        // Ignore schema graph implications for DuplicateDBElements
        if (el instanceof DuplicateDBElement) return node;

        // Add links from schema graph if it's not a relation
        // Attributes/proj/sel, etc. -> Relations
        if (!(el instanceof Relation)) {
            LogGraphNode relNode = this.getOrAddNode(el.getRelation());
            node.addEdge(relNode, 1.0);
        } else {
            // Find all FK-PKs
            Relation rel = (Relation) el;

            for (Relation rel2 : this.db.getAllRelations()) {
                if (!rel2.equals(rel) && this.db.hasJoin(rel, rel2)) {
                    LogGraphNode rel2Node = this.getOrAddNode(rel2);
                    node.addEdge(rel2Node, 1.0);
                }
            }
        }

        return node;
    }

    private LogGraphTree prims(List<LogGraphNode> nodes) {
        LogGraphNode curVertex = nodes.remove(0);

        LogGraphTree mst = new LogGraphTree(this.db, curVertex);

        while (!nodes.isEmpty()) {
            LogGraphNode minPointParent = null;
            LogGraphNode minPointNode = null;
            double minDist = 99999999999.0;
            for (LogGraphNode treeVertex : mst.getNodes()) {
                for (LogGraphNode point : nodes) {
                    Double dist = treeVertex.getWeight(point);
                    if (dist == null) continue;
                    if (dist < minDist) {
                        minPointParent = treeVertex;
                        minPointNode = point;
                        minDist = dist;
                    }
                }
            }
            mst.addNode(minPointParent, minPointNode);
            nodes.remove(minPointNode);
        }

        return mst;
    }

    private LogGraphTree shortestPathPrims(List<LogGraphNode> nodes) {
        LogGraphNode curVertex = nodes.remove(0);

        LogGraphTree mst = new LogGraphTree(this.db, curVertex);

        while (!nodes.isEmpty()) {
            LogGraphNode minPointParent = null;
            LogGraphNode minPointNode = null;
            double minDist = 99999999999.0;
            for (LogGraphNode treeVertex : mst.getNodes()) {
                for (LogGraphNode point : nodes) {
                    LogGraphNodePair pair = new LogGraphNodePair(treeVertex, point);

                    LogGraphPath shortestPath = this.shortestPaths.get(pair);

                    if (shortestPath == null) {
                        throw new RuntimeException("Could not find shortest path for " + pair);
                    }
                    double pathDist = shortestPath.distance();
                    if (pathDist < minDist) {
                        minPointParent = treeVertex;
                        minPointNode = point;
                        minDist = pathDist;
                    }
                }
            }
            mst.addNode(minPointParent, minPointNode);
            nodes.remove(minPointNode);
        }

        return mst;
    }

    // Find the minimum Steiner tree that connects all the points.
    public LogGraphTree steiner(List<DBElement> points) {
        // Step 1: Make sure shortest paths are calculated for all points
        List<LogGraphNode> pointNodes = new ArrayList<>();
        for (DBElement point : points) {
            LogGraphNode pointNode = this.getOrAddNode(point);
            this.findShortestPathToAllOtherNodes(pointNode);
            if (!pointNodes.contains(pointNode)) {
                pointNodes.add(pointNode);
            }
        }

        // Step 2: Find MST of shortest paths graph: run Prim's algorithm
        LogGraphTree mst = this.shortestPathPrims(new ArrayList<>(pointNodes));

        // Step 3: Replace each edge in the MST with the full nodes from the shortest paths
        Set<LogGraphNode> fullSubgraph = new HashSet<>();
        Stack<LogGraphNode> visitStack = new Stack<>();
        visitStack.push(mst.getRoot());

        while (!visitStack.isEmpty()) {
            LogGraphNode curMSTNode = visitStack.pop();
            fullSubgraph.add(curMSTNode);
            if (mst.hasChildren(curMSTNode)) {
                for (LogGraphNode child : mst.getChildren(curMSTNode)) {
                    LogGraphNodePair pair = new LogGraphNodePair(curMSTNode, child);
                    LogGraphPath path = this.shortestPaths.get(pair);
                    fullSubgraph.addAll(path.getNodes());
                    visitStack.push(child);
                }
            }
        }

        // Step 4: Find the MST of fullSubgraph, again using Prim's.
        for (LogGraphNode node : fullSubgraph) {
            this.findShortestPathToAllOtherNodes(node);
        }
        LogGraphTree finalMST = this.prims(new ArrayList<>(fullSubgraph));

        // Step 5: Construct a Steiner tree by deleting edges so that all the leaves are Steiner points
        Stack<LogGraphNode> leaves = new Stack<>();
        leaves.addAll(finalMST.getLeaves());

        while (!leaves.isEmpty()) {
            LogGraphNode leaf = leaves.pop();

            // Leaves can also NOT be relations, unless there are at least 2 FK-PK rels from it
            boolean prunableRelation = false;

            Relation rel = null;
            if (leaf.getElement() instanceof Relation) {
                rel = (Relation) leaf.getElement();
            } else if (leaf.getElement() instanceof DuplicateDBElement) {
                DuplicateDBElement dupl = (DuplicateDBElement) leaf.getElement();
                if (dupl.getEl() instanceof Relation) {
                    rel = (Relation) dupl.getEl();
                }
            }

            if (rel != null) {
                prunableRelation = true;

                // By definition, a leaf can only have one connected node (its "parent")
                LogGraphNode parent = null;
                for (Map.Entry<LogGraphNode, Double> e : leaf.getConnected().entrySet()) {
                    parent = e.getKey();
                }

                Relation parentRel = null;
                if (parent.getElement() instanceof Relation) {
                    parentRel = (Relation) parent.getElement();
                } else if (parent.getElement() instanceof DuplicateDBElement) {
                    DuplicateDBElement dupl = (DuplicateDBElement) parent.getElement();
                    if (dupl.getEl() instanceof Relation) {
                        parentRel = (Relation) dupl.getEl();
                    }
                }

                if (parentRel != null && this.db.fpCount(rel, parentRel) >= 2) {
                    prunableRelation = false;

                    // A HACK to support self-joins for "cite". In theory this will work with a diff. implementation.
                    // Assuming a max of 2 FK-PK links from a leaf table to parent
                    DuplicateDBElement duplParent = this.duplicate(parentRel);
                    LogGraphNode duplParentNode = this.getOrAddNode(duplParent);
                    finalMST.addNode(leaf, duplParentNode);

                    Set<LogGraphNode> children = new HashSet<>(finalMST.getChildren(parent));
                    for (LogGraphNode sibling : children) {
                        duplParentNode.addEdge(sibling, parent.getWeight(sibling));
                        if (!sibling.equals(leaf) && sibling.getElement() instanceof DuplicateDBElement) {
                            finalMST.changeParent(sibling, duplParentNode);
                        }
                    }
                }
            }

            if (!pointNodes.contains(leaf) || prunableRelation) {
                LogGraphNode parent = finalMST.getParent(leaf);
                if (parent == null) {
                    return null;
                }
                // Traverse up the leaf and remove from final MST
                finalMST.removeNode(leaf);
                if (finalMST.isLeaf(parent)) {
                    leaves.push(parent);
                }
            }
        }

        return finalMST;
    }

    // Using Dijkstra's algorithm from: https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm#Algorithm
    private void findShortestPathToAllOtherNodes(LogGraphNode node) {
        List<LogGraphNode> unvisited = new ArrayList<>();
        Map<LogGraphNode, Double> dist = new HashMap<>();
        Map<LogGraphNode, LogGraphNode> prev = new HashMap<>();

        double infinity = 99999999.0;

        for (LogGraphNode n : this.nodes) {
            unvisited.add(n);
            if (n.equals(node)) {
                dist.put(n, 0.0);
            } else {
                dist.put(n, infinity);
            }
        }

        while (!unvisited.isEmpty()) {
            // Find node with minimum distance
            LogGraphNode minNode = null;
            Double minDist = infinity;
            for (Map.Entry<LogGraphNode, Double> e : dist.entrySet()) {
                if (!unvisited.contains(e.getKey())) continue;
                if (minNode == null || e.getValue() < minDist) {
                    minNode = e.getKey();
                    minDist = e.getValue();
                }
            }

            unvisited.remove(minNode);

            for (Map.Entry<LogGraphNode, Double> e : minNode.getConnected().entrySet()) {
                double alt = minDist + e.getValue();

                if (alt < dist.get(e.getKey())) {
                    dist.put(e.getKey(), alt);
                    prev.put(e.getKey(), minNode);
                }
            }
        }

        // Reverse traversal to add all shortest distance paths from target -> source
        for (Map.Entry<LogGraphNode, LogGraphNode> e : prev.entrySet()) {
            LogGraphPath path = new LogGraphPath();

            LogGraphNode target = e.getKey();
            LogGraphNode curNode = target;

            while (prev.get(curNode) != null) {
                path.addNode(curNode);
                curNode = prev.get(curNode);
            }
            path.addNode(curNode);

            LogGraphNodePair shortestPathPair = new LogGraphNodePair(node, target);
            this.shortestPaths.put(shortestPathPair, path);
        }
    }
}
