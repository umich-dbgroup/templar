package edu.umich.tbnalir.parse;

import edu.umich.tbnalir.rdbms.Attribute;
import edu.umich.tbnalir.rdbms.Relation;

import java.util.*;

/**
 * Created by cjbaik on 9/11/17.
 */
public class PossibleTranslation {
    Set<Relation> relations;
    List<Projection> projections;
    List<Predicate> predicates;

    // Cache alias permutations
    Set<PossibleTranslation> permutations;

    Double translationScore;

    public PossibleTranslation() {
        this.relations = new HashSet<>();
        this.projections = new ArrayList<>();
        this.predicates = new ArrayList<>();

        this.permutations = null;
    }

    public PossibleTranslation(PossibleTranslation other) {
        this.relations = new HashSet<>(other.relations);
        this.projections = new ArrayList<>(other.projections);
        this.predicates = new ArrayList<>(other.predicates);
        this.translationScore = other.translationScore;

        this.permutations = null;
    }

    public Set<Relation> getRelations() {
        return relations;
    }

    public void setRelations(Set<Relation> relations) {
        this.relations = relations;
    }

    public List<Projection> getProjections() {
        return projections;
    }

    public void setProjections(List<Projection> projections) {
        this.projections = projections;
    }

    public List<Predicate> getPredicates() {
        return predicates;
    }

    public void setPredicates(List<Predicate> predicates) {
        this.predicates = predicates;
    }

    public Double getTranslationScore() {
        return translationScore;
    }

    public void setTranslationScore(Double translationScore) {
        this.translationScore = translationScore;
    }

    public Double getSimplicityScore() {
        return 1.0;
        // return 1d / relations.size();
    }

    public Double getTotalScore() {
        return this.getSimplicityScore() + this.translationScore;
    }

    private Set<PossibleTranslation> getAliasPermutationsHelper(PossibleTranslation pt,
                                                                Map<String, List<QueryFragment>> remaining) {
        Set<PossibleTranslation> results = new HashSet<>();
        Iterator<String> it = remaining.keySet().iterator();

        if (it.hasNext()) {
            String nextRel = it.next();
            List<QueryFragment> qfList = remaining.get(nextRel);
            remaining.remove(nextRel);

            // Get permutations if more than 1 option
            if (qfList.size() > 1) {
                for (int i = 0; i < qfList.size(); i++) {
                    List<Projection> newProj = new ArrayList<>(pt.getProjections());
                    List<Predicate> newPred = new ArrayList<>(pt.getPredicates());
                    for (int j = 0; j < qfList.size(); j++) {
                        int qfIndex = i + j;
                        while (qfIndex >= qfList.size()) {
                            qfIndex -= qfList.size();
                        }
                        QueryFragment curQf = qfList.get(qfIndex);

                        Attribute newAttr = new Attribute(curQf.getAttribute());
                        Relation newRel = new Relation(curQf.getAttribute().getRelation());
                        newAttr.setRelation(newRel);
                        newRel.setAliasInt(j);

                        if (curQf instanceof Projection) {
                            Projection copyProj = new Projection((Projection) curQf);
                            copyProj.setAttribute(newAttr);
                            newProj.add(copyProj);
                        } else {
                            Predicate copyPred = new Predicate((Predicate) curQf);
                            copyPred.setAttribute(newAttr);
                            newPred.add(copyPred);
                        }
                    }

                    PossibleTranslation newPt = new PossibleTranslation(pt);
                    newPt.setPredicates(newPred);
                    newPt.setProjections(newProj);

                    results.addAll(this.getAliasPermutationsHelper(newPt, new HashMap<>(remaining)));
                }
            } else {
                QueryFragment qf = qfList.get(0);

                PossibleTranslation newPt = new PossibleTranslation(pt);
                if (qf instanceof Projection) {
                    newPt.getProjections().add(new Projection((Projection) qf));
                } else {
                    newPt.getPredicates().add(new Predicate((Predicate) qf));
                }
                // Otherwise, move on to next
                results.addAll(this.getAliasPermutationsHelper(newPt, new HashMap<>(remaining)));
            }
        } else {
            // Base case, where we have no keys remaining. We should generate full translations here.
            results.add(pt);
        }

        return results;
    }

    // If we have a projection with author_0 and a predicate with author_1, for example,
    // this will print all possible permutations of aliases where the projection will have author_1 and
    // the predicate will have author_0 instead, etc.
    public Set<PossibleTranslation> getAliasPermutations() {
        if (this.permutations != null) return this.permutations;

        Set<PossibleTranslation> results = new HashSet<>();

        Map<String, List<QueryFragment>> relationsToQueryFragments = new HashMap<>();

        for (Projection proj : this.projections) {
            String relName = proj.getAttribute().getRelation().getName();
            List<QueryFragment> qfList = relationsToQueryFragments.get(relName);
            if (qfList == null) {
                qfList = new ArrayList<>();
                relationsToQueryFragments.put(relName, qfList);
            }
            qfList.add(proj);
        }

        for (Predicate pred : this.predicates) {
            String relName = pred.getAttribute().getRelation().getName();
            List<QueryFragment> qfList = relationsToQueryFragments.get(relName);
            if (qfList == null) {
                qfList = new ArrayList<>();
                relationsToQueryFragments.put(relName, qfList);
            }
            qfList.add(pred);
        }

        PossibleTranslation newPt = new PossibleTranslation();
        newPt.translationScore = this.translationScore;
        newPt.relations = new HashSet<>(this.relations);

        results.addAll(this.getAliasPermutationsHelper(newPt, relationsToQueryFragments));

        // Cache permutations
        this.permutations = results;

        return results;
    }

    public String toStringDebug() {
        StringBuilder sb = new StringBuilder();
        sb.append("SCORE: ");
        sb.append(this.getTotalScore());
        sb.append("; rels: [");

        StringJoiner relSj = new StringJoiner(",");
        for (Relation rel : this.relations) {
            if (rel == null) {
                relSj.add("null");
            } else {
                relSj.add(rel.getName());
            }
        }
        sb.append(relSj.toString());
        sb.append("]; projs: [");

        StringJoiner projSj = new StringJoiner(",");
        for (Projection proj : this.projections) {
            if (proj == null) {
                projSj.add("null");
            } else {
                projSj.add(proj.toString());
            }
        }
        sb.append(projSj.toString());
        sb.append("]; preds: [");

        StringJoiner predSj = new StringJoiner(",");
        this.predicates.stream().map(Predicate::toString).forEach(predSj::add);
        sb.append(predSj.toString());
        sb.append("]");

        return sb.toString();
    }
}
