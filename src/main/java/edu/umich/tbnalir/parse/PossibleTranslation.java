package edu.umich.tbnalir.parse;

import edu.umich.tbnalir.rdbms.Attribute;
import edu.umich.tbnalir.rdbms.Projection;
import edu.umich.tbnalir.rdbms.Relation;
import edu.umich.tbnalir.sql.Predicate;

import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Created by cjbaik on 9/11/17.
 */
public class PossibleTranslation {
    Set<Relation> relations;
    List<Projection> projections;
    List<Predicate> predicates;

    Double translationScore;

    public PossibleTranslation() {}

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
