package edu.umich.tbnalir.rdbms;

import org.apache.commons.collections.Bag;
import org.apache.commons.collections.bag.HashBag;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by cjbaik on 9/21/17.
 */
public class JoinPath {
    Set<JoinEdge> joinEdges;
    Set<Attribute> terminals;
    Set<Attribute> interiorVertices;
    Set<Relation> relations;

    // For computing equality of JoinPath, store aliasless versions
    Bag aliaslessTerminals;
    Bag aliaslessJoinEdges;

    // For convenience, store self-joins
    Set<JoinEdge> selfJoins;

    // Paths of consecutive join edges which use the same key
    // e.g. a1.aid = w.aid AND w.aid = a2.aid
    Set<JoinPath> consecutives;

    // If true, this JoinPath is used for constructing consecutives
    boolean consecutive = false;

    // Set<Set<JoinEdge>> consecutiveEdges;

    // Vertices on the interior of consecutive edges.
    // e.g. w.aid in example above
    // Set<Attribute> consecutiveInteriorVertices;

    public JoinPath() {
        this.joinEdges = new HashSet<>();
        this.aliaslessJoinEdges = new HashBag();

        this.terminals = new HashSet<>();
        this.interiorVertices = new HashSet<>();
        this.aliaslessTerminals = new HashBag();

        this.relations = new HashSet<>();
        this.selfJoins = new HashSet<>();

        this.consecutives = new HashSet<>();
    }

    public JoinPath(boolean consecutive) {
        this();
        this.consecutive = consecutive;
    }

    public JoinPath(JoinPath other) {
        this.joinEdges = new HashSet<>(other.joinEdges);
        this.aliaslessJoinEdges = new HashBag(other.aliaslessJoinEdges);

        this.terminals = new HashSet<>(other.terminals);
        this.interiorVertices = new HashSet<>(other.interiorVertices);
        this.aliaslessTerminals = new HashBag(other.aliaslessTerminals);

        this.relations = new HashSet<>(other.relations);
        this.selfJoins = new HashSet<>(other.selfJoins);

        this.consecutive = other.consecutive;

        this.consecutives = new HashSet<>();
        this.consecutives.addAll(other.consecutives.stream().map(JoinPath::new).collect(Collectors.toList()));
    }

    public boolean contains(JoinEdge edge) {
        return this.joinEdges.contains(edge);
    }

    public boolean isEmpty() {
        return this.joinEdges.isEmpty();
    }

    public Set<Attribute> getTerminals() {
        return terminals;
    }

    public Set<JoinPath> getConsecutives() {
        return consecutives;
    }

    public Set<Attribute> getInteriorVertices() {
        return interiorVertices;
    }

    public boolean passesSelfJoinCheck() {
        for (JoinEdge selfJoin : this.selfJoins) {
            // RULE: A self-join cannot be the edge leading to a terminal vertex
            if (this.terminals.contains(selfJoin.getFirst()) ||
                    this.terminals.contains(selfJoin.getSecond())) {
                return false;
            }

            // RULE: The relations of the vertices of a self-join must be
            // joined with two instances of the same relation.
            Relation firstRelation = null;
            Relation secondRelation = null;
            for (JoinEdge edge : this.joinEdges) {
                if (edge.equals(selfJoin)) continue;

                Relation selfJoinFirst = selfJoin.getFirst().getRelation();
                Relation selfJoinSecond = selfJoin.getSecond().getRelation();
                Relation edgeFirst = edge.getFirst().getRelation();
                Relation edgeSecond = edge.getSecond().getRelation();

                if (firstRelation == null) {
                    if (selfJoinFirst.equals(edgeFirst)) {
                        firstRelation = edge.getSecond().getRelation();
                    }
                    if (selfJoinFirst.equals(edgeSecond)) {
                        firstRelation = edge.getFirst().getRelation();
                    }
                }

                if (secondRelation == null) {
                    if (selfJoinSecond.equals(edgeFirst)) {
                        secondRelation = edge.getSecond().getRelation();
                    }
                    if (selfJoinSecond.equals(edgeSecond)) {
                        secondRelation = edge.getFirst().getRelation();
                    }
                }
            }

            if (firstRelation == null || secondRelation == null)
                throw new RuntimeException("We already checked terminals, so this should never happen.");
            if (!firstRelation.getName().equals(secondRelation.getName())) return false;
        }

        // RULE: A pseudo-self-join cannot be the edge leading to a terminal vertex.
        for (JoinPath consec : this.consecutives) {
            for (Attribute term : consec.getTerminals()) {
                if (this.terminals.contains(term) && term.isPk()) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean updateTerminalsIfRelationMatches(JoinEdge edge, Attribute existingVertex) {
        Relation rel = existingVertex.getRelation();

        if (edge.getFirst().getRelation().equals(rel)) {
            if (this.terminals.contains(existingVertex)) {
                this.terminals.remove(existingVertex);
                this.interiorVertices.add(existingVertex);
                this.aliaslessTerminals.remove(new AliaslessAttribute(existingVertex), 1);
            }
            this.terminals.add(edge.getSecond());
            this.aliaslessTerminals.add(new AliaslessAttribute(edge.getSecond()));
            return true;
        }

        if (edge.getSecond().getRelation().equals(rel)) {
            if (this.terminals.contains(existingVertex)) {
                this.terminals.remove(existingVertex);
                this.interiorVertices.add(existingVertex);
                this.aliaslessTerminals.remove(new AliaslessAttribute(existingVertex), 1);
            }
            this.terminals.add(edge.getFirst());
            this.aliaslessTerminals.add(new AliaslessAttribute(edge.getFirst()));
            return true;
        }

        return false;
    }

    public boolean updateConsecutiveEdges(JoinEdge edge, JoinEdge existing) {
        // In the case that attr is consecutive (i.e. it's not an attribute with a different key to the same relation)
        boolean isConsecutive = false;
        if (edge.getFirst().equals(existing.getFirst()) || edge.getFirst().equals(existing.getSecond())) {
            // this.consecutiveInteriorVertices.add(edge.getFirst());
            isConsecutive = true;
        }

        if (edge.getSecond().equals(existing.getFirst()) || edge.getSecond().equals(existing.getSecond())) {
            // this.consecutiveInteriorVertices.add(edge.getSecond());
            isConsecutive = true;
        }

        if (isConsecutive) {
            boolean added = false;
            for (JoinPath consecutives : this.consecutives) {
                if (consecutives.contains(existing)) {
                    consecutives.add(edge);
                    added = true;
                    break;
                }
            }
            if (!added) {
                JoinPath joinPath = new JoinPath(true);
                joinPath.add(edge);
                joinPath.add(existing);
                this.consecutives.add(joinPath);
            }
        }

        return true;
    }

    public boolean add(JoinEdge edge) {
        // Require new join edges to have at least one novel relation
        if (this.relations.contains(edge.getFirst().getRelation()) &&
                this.relations.contains(edge.getSecond().getRelation())) {
            return false;
        }

        if (this.terminals.isEmpty()) {
            this.terminals.add(edge.getFirst());
            this.terminals.add(edge.getSecond());

            this.aliaslessTerminals.add(new AliaslessAttribute(edge.getFirst()));
            this.aliaslessTerminals.add(new AliaslessAttribute(edge.getSecond()));
        } else {
            boolean terminalsUpdated = false;
            for (JoinEdge existing : this.joinEdges) {
                // If either relation is already in terminals, remove it from terminals.
                // Add the non-represented vertex relation to terminals.
                if (!terminalsUpdated) {
                    if (this.updateTerminalsIfRelationMatches(edge, existing.getFirst())) {
                        terminalsUpdated = true;
                    } else if (this.updateTerminalsIfRelationMatches(edge, existing.getSecond())) {
                        terminalsUpdated = true;
                    }
                }

                // Update consecutive edges (only if this isn't already a JoinPath used for storing consecutives)
                if (!this.consecutive && !this.updateConsecutiveEdges(edge, existing)) {
                    return false;
                }
            }
        }

        this.joinEdges.add(edge);
        this.aliaslessJoinEdges.add(new AliaslessJoinEdge(edge));
        this.relations.add(edge.getFirst().getRelation());
        this.relations.add(edge.getSecond().getRelation());

        if (edge.isSelfJoin()) {
            this.selfJoins.add(edge);
        }

        return true;
    }

    public Set<JoinEdge> getSelfJoins() {
        return this.selfJoins;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JoinPath joinPath = (JoinPath) o;

        if (aliaslessTerminals != null ? !aliaslessTerminals.equals(joinPath.aliaslessTerminals) : joinPath.aliaslessTerminals != null)
            return false;
        return !(aliaslessJoinEdges != null ? !aliaslessJoinEdges.equals(joinPath.aliaslessJoinEdges) : joinPath.aliaslessJoinEdges != null);

    }

    @Override
    public int hashCode() {
        int result = aliaslessTerminals != null ? aliaslessTerminals.hashCode() : 0;
        result = 31 * result + (aliaslessJoinEdges != null ? aliaslessJoinEdges.hashCode() : 0);
        return result;
    }
}
