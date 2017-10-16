package edu.umich.templar.parse;

import edu.umich.templar.qf.*;
import edu.umich.templar.rdbms.Attribute;
import edu.umich.templar.rdbms.Relation;
import edu.umich.templar.qf.pieces.Operator;

import java.util.*;

/**
 * Created by cjbaik on 9/11/17.
 */
public class PossibleTranslation {
    Set<Relation> relations;
    List<Projection> projections;
    List<Predicate> predicates;
    List<Having> havings;
    Superlative superlative;

    // Cache alias permutations
    Set<PossibleTranslation> permutations;

    Double translationScore;

    public PossibleTranslation() {
        this.relations = new HashSet<>();
        this.projections = new ArrayList<>();
        this.predicates = new ArrayList<>();
        this.havings = new ArrayList<>();
        this.superlative = null;

        this.permutations = null;
    }

    public PossibleTranslation(PossibleTranslation other) {
        this.relations = new HashSet<>(other.relations);
        this.projections = new ArrayList<>(other.projections);
        this.predicates = new ArrayList<>(other.predicates);
        this.havings = new ArrayList<>(other.havings);
        if (other.superlative != null) {
            this.superlative = new Superlative(other.superlative);
        } else {
            this.superlative = null;
        }

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

    public List<Having> getHavings() {
        return havings;
    }

    public void setHavings(List<Having> havings) {
        this.havings = havings;
    }

    public Superlative getSuperlative() {
        return superlative;
    }

    public void setSuperlative(Superlative superlative) {
        this.superlative = superlative;
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
        return this.translationScore;
    }

    private Set<PossibleTranslation> getAliasPermutationsHelper(PossibleTranslation pt,
                                                                Map<String, List<QueryFragment>> remaining) {
        Set<PossibleTranslation> results = new HashSet<>();
        Iterator<String> it = remaining.keySet().iterator();

        if (it.hasNext()) {
            String nextAttr = it.next();
            List<QueryFragment> qfList = remaining.get(nextAttr);
            remaining.remove(nextAttr);

            // Get permutations if more than 1 option
            if (qfList.size() > 1) {
                for (int i = 0; i < qfList.size(); i++) {
                    List<Projection> newProj = new ArrayList<>(pt.getProjections());
                    List<Predicate> newPred = new ArrayList<>(pt.getPredicates());
                    List<Having> newHaving = new ArrayList<>(pt.getHavings());
                    Superlative superlative = null;
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
                        } else if (curQf instanceof Predicate) {
                            Predicate copyPred = new Predicate((Predicate) curQf);
                            copyPred.setAttribute(newAttr);
                            newPred.add(copyPred);
                        } else if (curQf instanceof Having) {
                            Having copyHaving = new Having((Having) curQf);
                            copyHaving.setAttribute(newAttr);
                            newHaving.add(copyHaving);
                        } else {
                            Superlative copySuperlative = new Superlative((Superlative) curQf);
                            copySuperlative.setAttribute(newAttr);
                            superlative = copySuperlative;
                        }
                    }

                    PossibleTranslation newPt = new PossibleTranslation(pt);
                    newPt.setHavings(newHaving);
                    newPt.setPredicates(newPred);
                    newPt.setProjections(newProj);
                    newPt.setSuperlative(superlative);

                    results.addAll(this.getAliasPermutationsHelper(newPt, new HashMap<>(remaining)));
                }
            } else {
                QueryFragment qf = qfList.get(0);

                PossibleTranslation newPt = new PossibleTranslation(pt);
                if (qf instanceof Projection) {
                    newPt.getProjections().add(new Projection((Projection) qf));
                } else if (qf instanceof Predicate) {
                    newPt.getPredicates().add(new Predicate((Predicate) qf));
                } else if (qf instanceof Having) {
                    newPt.getHavings().add(new Having((Having) qf));
                } else {
                    newPt.setSuperlative((Superlative) qf);
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

        Map<String, List<QueryFragment>> attributesToQueryFragments = new HashMap<>();

        for (Projection proj : this.projections) {
            String attrName = proj.getAttribute().toString();
            List<QueryFragment> qfList = attributesToQueryFragments.get(attrName);
            if (qfList == null) {
                qfList = new ArrayList<>();
                attributesToQueryFragments.put(attrName, qfList);
            }
            qfList.add(proj);
        }

        for (Predicate pred : this.predicates) {
            String attrName = pred.getAttribute().toString();
            // If operator isn't =, then we give a separate bucket
            if (!pred.getOp().equals(Operator.EQ)) {
                attrName += pred.getOp().toString();
            }

            List<QueryFragment> qfList = attributesToQueryFragments.get(attrName);
            if (qfList == null) {
                qfList = new ArrayList<>();
                attributesToQueryFragments.put(attrName, qfList);
            }
            qfList.add(pred);
        }

        for (Having having : this.havings) {
            String attrName = having.getAttribute().toString();
            List<QueryFragment> qfList = attributesToQueryFragments.get(attrName);
            if (qfList == null) {
                qfList = new ArrayList<>();
                attributesToQueryFragments.put(attrName, qfList);
            }
            qfList.add(having);
        }

        if (this.superlative != null) {
            String attrName = this.superlative.getAttribute().toString();
            List<QueryFragment> qfList = attributesToQueryFragments.get(attrName);
            if (qfList == null) {
                qfList = new ArrayList<>();
                attributesToQueryFragments.put(attrName, qfList);
            }
            qfList.add(this.superlative);
        }

        PossibleTranslation newPt = new PossibleTranslation();
        newPt.translationScore = this.translationScore;
        newPt.relations = new HashSet<>(this.relations);

        results.addAll(this.getAliasPermutationsHelper(newPt, attributesToQueryFragments));

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
        sb.append("]; havings: [");

        StringJoiner havingSj = new StringJoiner(",");
        this.havings.stream().map(Having::toString).forEach(havingSj::add);
        sb.append(havingSj.toString());
        sb.append("]; superlative: [");

        if (this.superlative != null) {
            sb.append(this.superlative.toString());
        }
        sb.append("]");

        return sb.toString();
    }
}
