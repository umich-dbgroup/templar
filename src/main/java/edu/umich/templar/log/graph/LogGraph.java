package edu.umich.templar.log.graph;

import edu.umich.templar.db.AttributeAndPredicate;
import edu.umich.templar.db.DBElement;
import edu.umich.templar.db.Database;
import edu.umich.templar.db.Relation;
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

    private void addNode(LogGraphNode node) {
        this.elementMap.put(node.getElement(), node);
        this.nodes.add(node);
    }

    private LogGraphNode getOrAddNode(DBElement el) {
        LogGraphNode node = this.elementMap.get(el);

        if (node != null) return node;
        else {
            node = new LogGraphNode(el);
            this.elementMap.put(el, node);
            this.nodes.add(node);
        }

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

        LogGraphTree mst = new LogGraphTree(curVertex);

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

        LogGraphTree mst = new LogGraphTree(curVertex);

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
            if (point instanceof AttributeAndPredicate) {
                AttributeAndPredicate attrPred = (AttributeAndPredicate) point;
                LogGraphNode attrPoint = this.getOrAddNode(attrPred.getAttribute());
                pointNodes.add(attrPoint);

                LogGraphNode predPoint = this.getOrAddNode(attrPred.getPredicate());
                pointNodes.add(predPoint);

                this.findShortestPathToAllOtherNodes(attrPoint);
                this.findShortestPathToAllOtherNodes(predPoint);
            } else {
                LogGraphNode pointNode = this.getOrAddNode(point);
                this.findShortestPathToAllOtherNodes(pointNode);
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
            // Leaves can also NOT be relations!
            if (!pointNodes.contains(leaf) || leaf.getElement() instanceof Relation) {
                LogGraphNode parent = finalMST.getParent(leaf);
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
