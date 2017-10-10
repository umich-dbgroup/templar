package edu.umich.templar.template;

import edu.umich.templar.parse.*;
import edu.umich.templar.rdbms.Relation;
import org.apache.commons.collections.Bag;
import org.apache.commons.collections.bag.HashBag;

/**
 * Created by cjbaik on 10/10/17.
 */
public class QueryFragmentCounter {
    Bag counter;

    public QueryFragmentCounter() {
        this.counter = new HashBag();
    }

    public void addFragment(QueryFragment qf) {
        if (qf instanceof Projection) {
            Projection proj = (Projection) qf;
            this.counter.add(proj.toStringWithConsistentRelation().intern());
        } else if (qf instanceof Predicate) {
            Predicate pred = (Predicate) qf;
            this.counter.add(pred.toStringWithConsistentRelation().intern());
        } else if (qf instanceof Having) {
            Having having = (Having) qf;
            this.counter.add(having.toStringWithConsistentRelation().intern());
        } else if (qf instanceof Superlative) {
            Superlative superlative = (Superlative) qf;
            this.counter.add(superlative.toStringWithConsistentRelation().intern());
        } else {
            this.counter.add(qf.toString());
        }
    }

    public double getFreqScore(QueryFragment qf) {
        double totalSize = this.counter.size();
        double fragCount;
        if (qf instanceof Projection) {
            Projection proj = (Projection) qf;
            fragCount = this.counter.getCount(proj.toStringWithConsistentRelation().intern());
        } else if (qf instanceof Predicate) {
            Predicate pred = (Predicate) qf;
            fragCount = this.counter.getCount(pred.toStringWithConsistentRelation().intern());
        } else if (qf instanceof Having) {
            Having having = (Having) qf;
            fragCount = this.counter.getCount(having.toStringWithConsistentRelation().intern());
        } else if (qf instanceof Superlative) {
            Superlative superlative = (Superlative) qf;
            fragCount = this.counter.getCount(superlative.toStringWithConsistentRelation().intern());
        } else {
            fragCount = this.counter.getCount(qf.toString());
        }
        return fragCount / totalSize;
    }

    public void addRelation(Relation r) {
        this.counter.add(r.getName());
    }

    public double getFreqScore(Relation r) {
        double totalSize = this.counter.size();
        double relCount = this.counter.getCount(r.getName());
        return relCount / totalSize;
    }

    public double getFreqScore(String s) {
        double totalSize = this.counter.size();
        double relCount = this.counter.getCount(s);
        return relCount / totalSize;
    }

    public Bag getCounter() {
        return counter;
    }

    /* TODO: maybe.
    public void addJoinPath(Relation r1, Relation r2) {
    }*/
}
