package edu.umich.templar.template;

import edu.umich.templar.dataStructure.ParseTreeNode;
import edu.umich.templar.qf.*;
import edu.umich.templar.qf.agnostic.*;
import edu.umich.templar.rdbms.Attribute;
import edu.umich.templar.rdbms.Relation;
import edu.umich.templar.qf.pieces.Operator;
import edu.umich.templar.util.Constants;

import java.util.*;

/**
 * Created by cjbaik on 9/11/17.
 */
public class Translation {
    // SCORE MODE:
    // 0: NL only
    // 1: NL + Agnostic
    // 2: NL + QF
    public static int MODE = 0;

    List<ScoredQueryFragment> scoredQFs;     // all query fragments for QFGraph
    List<ScoredAgnosticQueryFragment> scoredAQFs; // all agnostic query fragments for AgnosticGraph

    // Map to store QF to scoredQFs/scoredAQFs for easy access
    Map<QueryFragment, List<ScoredQueryFragment>> scoredQFMap;
    Map<QueryFragment, List<ScoredAgnosticQueryFragment>> scoredAQFMap;

    // Maps query fragment to similarity scores for easy access during alias permutations generation
    Map<QueryFragment, Double> scoreMap;

    // Breakdown of query fragments by type
    Set<RelationFragment> relations;
    List<Projection> projections;
    List<Predicate> predicates;
    List<Having> havings;
    List<BlankQueryFragment> blanks;
    Superlative superlative;

    // Schema-agnostic and schema-aware graphs
    AgnosticGraph agnosticGraph;
    QFGraph qfGraph;

    // Parent translation if this is alias
    Translation parent;

    // Cache alias permutations
    Set<Translation> permutations;

    // Total translation score
    Double score;

    public Translation(AgnosticGraph agnosticGraph, QFGraph qfGraph) {
        this.agnosticGraph = agnosticGraph;
        this.qfGraph = qfGraph;

        this.scoredQFs = new ArrayList<>();
        this.scoredAQFs = new ArrayList<>();

        this.scoredAQFMap = new HashMap<>();
        this.scoredQFMap = new HashMap<>();

        this.relations = new HashSet<>();
        this.projections = new ArrayList<>();
        this.predicates = new ArrayList<>();
        this.havings = new ArrayList<>();
        this.blanks = new ArrayList<>();
        this.superlative = null;

        this.scoreMap = new HashMap<>();

        this.parent = null;
        this.permutations = null;
        this.score = null;
    }

    public Translation(Translation other) {
        this.agnosticGraph = other.agnosticGraph;
        this.qfGraph = other.qfGraph;

        this.scoredQFs = new ArrayList<>(other.scoredQFs);
        this.scoredAQFs = new ArrayList<>(other.scoredAQFs);

        this.scoredAQFMap = new HashMap<>(other.scoredAQFMap);
        this.scoredQFMap = new HashMap<>(other.scoredQFMap);

        this.relations = new HashSet<>(other.relations);
        this.projections = new ArrayList<>(other.projections);
        this.predicates = new ArrayList<>(other.predicates);
        this.havings = new ArrayList<>(other.havings);
        this.blanks = new ArrayList<>(other.blanks);
        if (other.superlative != null) {
            this.superlative = new Superlative(other.superlative);
        } else {
            this.superlative = null;
        }

        this.scoreMap = new HashMap<>(other.scoreMap);

        this.score = other.score;

        this.parent = other.parent;
        this.permutations = null;
    }

    private void addRelation(RelationFragment relationFragment) {
        this.relations.add(relationFragment);
    }

    public Set<RelationFragment> getRelations() {
        return relations;
    }

    private void addProjection(Projection projection) {
        this.projections.add(projection);
    }

    public List<Projection> getProjections() {
        return projections;
    }

    private void addPredicate(Predicate predicate) {
        this.predicates.add(predicate);
    }

    public List<Predicate> getPredicates() {
        return predicates;
    }

    private void addHaving(Having having) {
        this.havings.add(having);
    }

    public List<Having> getHavings() {
        return havings;
    }

    private void addBlank(BlankQueryFragment blank) {
        this.blanks.add(blank);
    }

    public Superlative getSuperlative() {
        return superlative;
    }

    private void setSuperlative(Superlative superlative) {
        if (this.superlative != null) throw new RuntimeException("Tried to set duplicate superlative.");

        this.superlative = superlative;
    }

    public Translation getParent() {
        return parent;
    }

    public void setParent(Translation parent) {
        this.parent = parent;
    }

    public Double getNLAGScore() {
        double totalSim = 0.0;
        for (ScoredAgnosticQueryFragment aqf : this.scoredAQFs) {
            if (aqf.getSimilarity() < Constants.SIM_AUG_THRESHOLD) {
                double cooccurSum = 0.0;
                for (ScoredAgnosticQueryFragment scoredAQF : this.scoredAQFs) {
                    cooccurSum += aqf.getWeightedDiceCoefficient(scoredAQF);
                }
                double cooccurScore = cooccurSum / this.scoredAQFs.size();
                double scaledCooccur = cooccurScore * (1 - Constants.SIM_AUG_THRESHOLD) + Constants.SIM_AUG_THRESHOLD;
                totalSim += scaledCooccur;
            } else {
                totalSim += aqf.getSimilarity();
            }
        }

        return totalSim / this.scoredAQFs.size();
    }

    public Double getNLQFScore() {
        double totalSim = 0.0;
        for (ScoredQueryFragment qf : this.scoredQFs) {
            if (qf.getSimilarity() < Constants.SIM_AUG_THRESHOLD) {
                double cooccurSum = 0.0;
                for (ScoredQueryFragment scoredQF : this.scoredQFs) {
                    cooccurSum += qf.getWeightedDiceCoefficient(scoredQF);
                }
                double cooccurScore = cooccurSum / this.scoredQFs.size();
                double scaledCooccur = cooccurScore * (1 - Constants.SIM_AUG_THRESHOLD) + Constants.SIM_AUG_THRESHOLD;
                totalSim += scaledCooccur;
            } else {
                totalSim += qf.getSimilarity();
            }
        }

        return totalSim / this.scoredQFs.size();
    }

    public Double getNLScore() {
        double totalSim = 0.0;
        for (ScoredQueryFragment qf : this.scoredQFs) {
            totalSim += qf.getSimilarity();
        }

        return totalSim / this.scoredQFs.size();
    }

    /*
    public Double getQFGraphScore() {
        double diceSum = 0.0;
        for (int i = 0; i < this.scoredQFs.size(); i++) {
            ScoredQueryFragment qfi = this.scoredQFs.get(i);
            for (int j = 0; j < this.scoredQFs.size(); j++) {
                ScoredQueryFragment qfj = this.scoredQFs.get(j);
                diceSum += qfi.getWeightedDiceCoefficient(qfj);
            }
        }
        double totalCount = this.scoredQFs.size();
        return diceSum / totalCount / totalCount;
    }

    public Double getAgnosticGraphScore() {
        double diceSum = 0.0;
        for (int i = 0; i < this.scoredAQFs.size(); i++) {
            ScoredAgnosticQueryFragment qfi = this.scoredAQFs.get(i);
            for (int j = 0; j < this.scoredAQFs.size(); j++) {
                ScoredAgnosticQueryFragment qfj = this.scoredAQFs.get(j);
                diceSum += qfi.getWeightedDiceCoefficient(qfj);
            }
        }
        double totalCount = this.scoredAQFs.size();
        return diceSum / totalCount / totalCount;
    }*/

    public double getSimilarity(QueryFragment qf) {
        return this.scoreMap.get(qf);
    }

    public double setSimilarity(QueryFragment qf, double similarity) {
        return this.scoreMap.put(qf, similarity);
    }

    public void setScore(double score) {
        this.score = score;
    }

    public Double getScore() {
        if (this.score != null) return this.score;

        if (MODE == 0) {
            this.score = this.getNLScore();
        }
        if (MODE == 1) {
            this.score = this.getNLAGScore();
        }
        if (MODE == 2) {
            this.score = this.getNLQFScore();
        }
        /*
        double product = this.getNLScore();
        int root = 1;

        if (this.agnosticGraph != null) {
            product *= this.getAgnosticGraphScore();
            root++;
        }
        if (this.qfGraph != null) {
            product *= this.getQFGraphScore();
            root++;
        }

        switch (root) {
            case 1:
                this.score = product;
                break;
            case 2:
                this.score = Math.sqrt(product);
                break;
            case 3:
                this.score = Math.cbrt(product);
                break;
        }

        // TODO: testing only NL score;
        this.score = this.getNLScore();*/

        return this.score;
    }

    public void removeQueryFragment(QueryFragment qf) {
        this.score = null;

        if (qf instanceof RelationFragment) {
            this.relations.remove(qf);
        } else if (qf instanceof Projection) {
            this.projections.remove(qf);
        } else if (qf instanceof Predicate) {
            this.predicates.remove(qf);
        } else if (qf instanceof Having) {
            this.havings.remove(qf);
        } else if (qf instanceof Superlative) {
            this.superlative = null;
        } else if (qf instanceof BlankQueryFragment) {
            this.blanks.remove(qf);
        } else {
            throw new RuntimeException("Unrecognized query fragment type!");
        }

        List<ScoredAgnosticQueryFragment> scoredAQFList = this.scoredAQFMap.get(qf);
        this.scoredAQFs.removeAll(scoredAQFList);
        this.scoredAQFMap.remove(qf);

        List<ScoredQueryFragment> scoredQFList = this.scoredQFMap.get(qf);
        this.scoredQFs.removeAll(scoredQFList);
        this.scoredQFMap.remove(qf);

        this.scoreMap.remove(qf);
    }

    public void addQueryFragment(QueryFragment qf, double similarity) {
        this.score = null;

        if (qf instanceof RelationFragment) {
            this.addRelation((RelationFragment) qf);
        } else if (qf instanceof Projection) {
            this.addProjection((Projection) qf);
        } else if (qf instanceof Predicate) {
            this.addPredicate((Predicate) qf);
        } else if (qf instanceof Having) {
            this.addHaving((Having) qf);
        } else if (qf instanceof Superlative) {
            this.setSuperlative((Superlative) qf);
        } else if (qf instanceof BlankQueryFragment) {
            this.addBlank((BlankQueryFragment) qf);
        } else {
            throw new RuntimeException("Unrecognized query fragment type!");
        }

        // Add all agnostic query fragments as needed
        List<AgnosticQueryFragment> agnosticQFs = qf.convertToAgnostic();
        List<ScoredAgnosticQueryFragment> qfScoredAQFList = new ArrayList<>();

        this.scoredAQFMap.put(qf, qfScoredAQFList);

        for (AgnosticQueryFragment aqf : agnosticQFs) {
            if (this.agnosticGraph != null) {
                aqf = this.agnosticGraph.getOrInsertQF(aqf);
            }
            ScoredAgnosticQueryFragment scoredAQF = new ScoredAgnosticQueryFragment(aqf, similarity);
            qfScoredAQFList.add(scoredAQF);
            this.scoredAQFs.add(scoredAQF);
        }

        // Add all regular query fragments as needed
        List<QueryFragment> qfTemplates = qf.convertToQFTemplate();
        List<ScoredQueryFragment> qfScoredQFList = new ArrayList<>();

        this.scoredQFMap.put(qf, qfScoredQFList);

        for (QueryFragment qfTemplate : qfTemplates) {
            if (this.qfGraph != null) {
                qfTemplate = this.qfGraph.getOrInsertQF(qfTemplate);
            }
            ScoredQueryFragment sqf = new ScoredQueryFragment(qfTemplate, similarity);
            qfScoredQFList.add(sqf);
            this.scoredQFs.add(sqf);
        }

        this.scoreMap.put(qf, similarity);
    }

    public boolean containsNode(ParseTreeNode node) {
        for (RelationFragment rel : this.relations) {
            if (rel.getNode().equals(node)) return true;
        }

        for (Projection proj : this.projections) {
            if (proj.getNode().equals(node)) return true;
        }

        for (Predicate pred : this.predicates) {
            if (pred.getNode().equals(node)) return true;
        }

        for (Having having : this.havings) {
            if (having.getNode().equals(node)) return true;
        }

        for (BlankQueryFragment blank : this.blanks) {
            if (blank.getNode().equals(node)) return true;
        }

        if (this.superlative != null && this.superlative.getNode().equals(node)) return true;

        return false;
    }

    public Set<Relation> getReferencedRelations() {
        Set<Relation> result = new HashSet<>();
        for (RelationFragment rel : this.relations) {
            result.add(rel.getRelation());
        }
        for (Projection proj : this.projections) {
            result.add(proj.getAttribute().getRelation());
        }
        for (Predicate pred : this.predicates) {
            result.add(pred.getAttribute().getRelation());
        }
        for (Having having : this.havings) {
            result.add(having.getAttribute().getRelation());
        }
        if (this.superlative != null) {
            result.add(this.superlative.getAttribute().getRelation());
        }
        return result;
    }

    private Set<Translation> getAliasPermutationsHelper(Translation pt, Map<String, List<ScoredQueryFragment>> remaining) {
        Set<Translation> results = new HashSet<>();
        Iterator<String> it = remaining.keySet().iterator();

        if (it.hasNext()) {
            String nextAttr = it.next();
            List<ScoredQueryFragment> qfList = remaining.get(nextAttr);
            remaining.remove(nextAttr);

            // Get permutations if more than 1 option
            if (qfList.size() > 1) {
                for (int i = 0; i < qfList.size(); i++) {
                    Translation newPt = new Translation(pt);

                    for (int j = 0; j < qfList.size(); j++) {
                        int qfIndex = i + j;
                        while (qfIndex >= qfList.size()) {
                            qfIndex -= qfList.size();
                        }
                        ScoredQueryFragment curQf = qfList.get(qfIndex);

                        Attribute newAttr = new Attribute(curQf.getQf().getAttribute());
                        Relation newRel = new Relation(curQf.getQf().getAttribute().getRelation());
                        newAttr.setRelation(newRel);
                        newRel.setAliasInt(j);

                        if (curQf.getQf() instanceof Projection) {
                            Projection copyProj = new Projection((Projection) curQf.getQf());
                            copyProj.setAttribute(newAttr);
                            newPt.addQueryFragment(copyProj, curQf.getSimilarity());
                        } else if (curQf.getQf() instanceof Predicate) {
                            Predicate copyPred = new Predicate((Predicate) curQf.getQf());
                            copyPred.setAttribute(newAttr);
                            newPt.addQueryFragment(copyPred, curQf.getSimilarity());
                        } else if (curQf.getQf() instanceof Having) {
                            Having copyHaving = new Having((Having) curQf.getQf());
                            copyHaving.setAttribute(newAttr);
                            newPt.addQueryFragment(copyHaving, curQf.getSimilarity());
                        } else {
                            Superlative copySuperlative = new Superlative((Superlative) curQf.getQf());
                            copySuperlative.setAttribute(newAttr);
                            newPt.addQueryFragment(copySuperlative, curQf.getSimilarity());
                        }
                    }

                    results.addAll(this.getAliasPermutationsHelper(newPt, new HashMap<>(remaining)));
                }
            } else {
                ScoredQueryFragment qf = qfList.get(0);

                Translation newPt = new Translation(pt);
                if (qf.getQf() instanceof Projection) {
                    Projection copyProj = new Projection((Projection) qf.getQf());
                    newPt.addQueryFragment(copyProj, qf.getSimilarity());
                } else if (qf.getQf() instanceof Predicate) {
                    Predicate copyPred = new Predicate((Predicate) qf.getQf());
                    newPt.addQueryFragment(copyPred, qf.getSimilarity());
                } else if (qf.getQf() instanceof Having) {
                    Having copyHaving = new Having((Having) qf.getQf());
                    newPt.addQueryFragment(copyHaving, qf.getSimilarity());
                } else {
                    Superlative copySuperlative = new Superlative((Superlative) qf.getQf());
                    newPt.addQueryFragment(copySuperlative, qf.getSimilarity());
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
    public Set<Translation> getAliasPermutations() {
        if (this.permutations != null) return this.permutations;

        Set<Translation> results = new HashSet<>();

        Map<String, List<ScoredQueryFragment>> attributesToQueryFragments = new HashMap<>();

        for (Projection proj : this.projections) {
            String attrName = proj.getAttribute().toString();
            List<ScoredQueryFragment> qfList = attributesToQueryFragments.get(attrName);
            if (qfList == null) {
                qfList = new ArrayList<>();
                attributesToQueryFragments.put(attrName, qfList);
            }
            qfList.add(new ScoredQueryFragment(proj, this.scoreMap.get(proj)));
        }

        for (Predicate pred : this.predicates) {
            String attrName = pred.getAttribute().toString();
            // If operator isn't =, then we give a separate bucket
            if (!pred.getOp().equals(Operator.EQ)) {
                attrName += pred.getOp().toString();
            }

            List<ScoredQueryFragment> qfList = attributesToQueryFragments.get(attrName);
            if (qfList == null) {
                qfList = new ArrayList<>();
                attributesToQueryFragments.put(attrName, qfList);
            }
            qfList.add(new ScoredQueryFragment(pred, this.scoreMap.get(pred)));
        }

        for (Having having : this.havings) {
            String attrName = having.getAttribute().toString();
            List<ScoredQueryFragment> qfList = attributesToQueryFragments.get(attrName);
            if (qfList == null) {
                qfList = new ArrayList<>();
                attributesToQueryFragments.put(attrName, qfList);
            }
            qfList.add(new ScoredQueryFragment(having, this.scoreMap.get(having)));
        }

        if (this.superlative != null) {
            String attrName = this.superlative.getAttribute().toString();
            List<ScoredQueryFragment> qfList = attributesToQueryFragments.get(attrName);
            if (qfList == null) {
                qfList = new ArrayList<>();
                attributesToQueryFragments.put(attrName, qfList);
            }
            qfList.add(new ScoredQueryFragment(this.superlative, this.scoreMap.get(this.superlative)));
        }

        // Copy over blanks and relationfragments since they don't need to be aliased
        Translation newPt = new Translation(this.agnosticGraph, this.qfGraph);
        for (BlankQueryFragment blank : this.blanks) {
            newPt.addQueryFragment(blank, this.scoreMap.get(blank));
        }
        for (RelationFragment rel : this.relations) {
            newPt.addQueryFragment(rel, this.scoreMap.get(rel));
        }

        results.addAll(this.getAliasPermutationsHelper(newPt, attributesToQueryFragments));

        // Cache permutations
        this.permutations = results;
        for (Translation perm : this.permutations) {
            perm.setParent(this);
            perm.setScore(this.score);
        }

        return results;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TOT: ");
        sb.append(this.getScore());
        sb.append("; NL: ");
        sb.append(this.getNLScore());
        sb.append("; NL+AG: ");
        sb.append(this.getNLAGScore());
        sb.append("; NL+QF: ");
        sb.append(this.getNLQFScore());
        sb.append("; rels: [");

        StringJoiner relSj = new StringJoiner(",");
        for (RelationFragment rel : this.relations) {
            if (rel == null) {
                relSj.add("null");
            } else {
                relSj.add(rel.getRelation().getName());
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
        sb.append("]; blanks: ");
        sb.append(this.blanks.size());

        return sb.toString();
    }
}
