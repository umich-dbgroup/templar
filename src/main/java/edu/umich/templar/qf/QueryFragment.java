package edu.umich.templar.qf;

import edu.umich.templar.qf.agnostic.*;
import edu.umich.templar.qf.pieces.QFFunction;
import edu.umich.templar.rdbms.Attribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by cjbaik on 9/25/17.
 */
public class QueryFragment {
    protected Attribute attribute;

    int count;                                        // number of total appearances
    Map<QueryFragment, Integer> cooccurrence;         // co-occurrence with other query fragments

    public QueryFragment() {
        this.count = 0;
        this.cooccurrence = new HashMap<>();
        this.attribute = null;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    public List<QueryFragment> convertToQFTemplate() {
        List<QueryFragment> result = new ArrayList<>();

        if (this instanceof BlankQueryFragment) {
            result.add(new BlankQueryFragment());
        } else if (this instanceof RelationFragment) {
            result.add(new RelationFragment(((RelationFragment) this).getRelation()));
        } else if (this instanceof Projection) {
            Projection proj = (Projection) this;
            Projection projTmpl = new Projection(proj.getAttribute().canonical(), proj.getFunction(), null);
            result.add(projTmpl);

            // also add group by if included
            if (proj.isGroupBy()) {
                result.add(new GroupBy(proj.getAttribute().canonical()));
            }
        } else if (this instanceof Predicate) {
            Predicate pred = (Predicate) this;

            Predicate newPred = new Predicate(pred.getAttribute(), pred.getOp(), null);
            result.add(newPred);
        } else if (this instanceof Having) {
            Having having = (Having) this;
            Having newHaving = new Having(having.getAttribute(), having.getOp(), null, having.getFunction());
            result.add(newHaving);
        } else if (this instanceof Superlative) {
            Superlative superlative = (Superlative) this;

            // Order by
            result.add(new OrderBy(superlative.getAttribute(), superlative.getFunction(), superlative.isDesc()));

            // Limit
            result.add(new Limit());
        } else {
            throw new RuntimeException("Unrecognized query fragment type!");
        }
        return result;
    }

    public List<AgnosticQueryFragment> convertToAgnostic() {
        List<AgnosticQueryFragment> result = new ArrayList<>();

        if (this instanceof BlankQueryFragment) {
            result.add(new AgnosticBlank());
        } else if (this instanceof RelationFragment) {
            result.add(new AgnosticRelationFragment());
        } else if (this instanceof Projection) {
            Projection proj = (Projection) this;
            QFFunction thisFunction = null;
            if (proj.getFunction() != null) {
                thisFunction = QFFunction.getFunction(proj.getFunction());
            }
            AgnosticProjection agProj = new AgnosticProjection(proj.getAttribute().getAttributeType(), thisFunction);
            result.add(agProj);

            // also add group by if included
            if (proj.isGroupBy()) {
                result.add(new AgnosticGroupBy(proj.getAttribute().getAttributeType()));
            }
        } else if (this instanceof Predicate) {
            Predicate pred = (Predicate) this;

            AgnosticPredicate agPred = new AgnosticPredicate(pred.getAttribute().getAttributeType(), pred.getOp());
            result.add(agPred);
        } else if (this instanceof Having) {
            Having having = (Having) this;
            QFFunction thisFunction = QFFunction.getFunction(having.getFunction());
            AgnosticHaving agHaving = new AgnosticHaving(having.getAttribute().getAttributeType(), thisFunction, having.getOp());
            result.add(agHaving);
        } else if (this instanceof Superlative) {
            Superlative superlative = (Superlative) this;
            QFFunction thisFunction = null;
            if (superlative.getFunction() != null) {
                thisFunction = QFFunction.getFunction(superlative.getFunction());
            }

            // Order by
            result.add(new AgnosticOrderBy(superlative.getAttribute().getAttributeType(), thisFunction, superlative.isDesc()));

            // Limit
            result.add(new AgnosticLimit());
        } else {
            throw new RuntimeException("Unrecognized query fragment type!");
        }
        return result;
    }

    public int getCooccurrence(QueryFragment other) {
        Integer count = this.cooccurrence.get(other);
        if (count == null) {
            return 0;
        }
        return count;
    }

    public int getCount() {
        return this.count;
    }

    public void incrementCount() {
        this.count++;
    }

    public void incrementCooccurrence(QueryFragment other) {
        Integer count = this.cooccurrence.get(other);
        if (count == null) {
            count = 1;
        } else {
            count++;
        }
        this.cooccurrence.put(other, count);
    }

}
