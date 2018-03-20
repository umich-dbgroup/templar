package edu.umich.templar.log;

import edu.umich.templar.db.Attribute;
import edu.umich.templar.db.DBElement;
import edu.umich.templar.db.Database;
import edu.umich.templar.db.Relation;
import edu.umich.templar.log.parse.ParserUtils;
import edu.umich.templar.log.parse.PredicateParser;
import edu.umich.templar.log.parse.ProjectionParser;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LogFullTemplates {
    private Database db;
    private LogLevel mode;
    private Map<Set<DBElement>, Integer> setCounts;

    public LogFullTemplates(Database db, LogLevel mode) {
        this.db = db;
        this.mode = mode;
        this.setCounts = new HashMap<>();
    }

    /*
    private void setGroupBy(Set<DBElement> els, Attribute attr) {
        for (DBElement el : els) {
            if (el instanceof Attribute) {
                Attribute proj = (Attribute) el;
                if (proj.equals(attr)) {
                    proj.setGroupBy(true);
                    return;
                }
            }
        }
        throw new RuntimeException("Attribute <" + attr + "> not found in previous elements list.");
    } */

    public Set<DBElement> extractElementsFromSelect(PlainSelect ps) {
        Set<DBElement> elementsInSelect = new HashSet<>();

        // Relations
        Set<Relation> relations = new HashSet<>();
        if (ps.getFromItem() instanceof Table) {
            Table table = (Table) ps.getFromItem();
            Relation rel = this.db.getRelationByName(table.getName());

            if (rel == null) throw new RuntimeException("Unrecognized relation: " + table.getName());

            // TODO: what do you do about self-joins (?)
            relations.add(rel);
            elementsInSelect.add(rel);
        } else if (ps.getFromItem() instanceof SubSelect) {
            // in the case it is a subquery
            PlainSelect subPs = (PlainSelect) ((SubSelect) ps.getFromItem()).getSelectBody();
            elementsInSelect.addAll(this.extractElementsFromSelect(subPs));
        }

        if (ps.getJoins() != null) {
            for (Join join : ps.getJoins()) {
                if (join.getRightItem() instanceof Table) {
                    Table table = (Table) join.getRightItem();
                    Relation rel = this.db.getRelationByName(table.getName());

                    if (rel == null) throw new RuntimeException("Unrecognized relation: " + table.getName());

                    relations.add(rel);
                    elementsInSelect.add(rel);
                } else if (join.getRightItem() instanceof SubSelect) {
                    PlainSelect subPs = (PlainSelect) ((SubSelect) join.getRightItem()).getSelectBody();
                    elementsInSelect.addAll(this.extractElementsFromSelect(subPs));
                }
            }
        }

        // Projections
        for (SelectItem item : ps.getSelectItems()) {
            if (item instanceof SelectExpressionItem) {
                ProjectionParser projParser = new ProjectionParser(this.db, relations);
                Expression expr = ((SelectExpressionItem) item).getExpression();

                if (expr instanceof net.sf.jsqlparser.expression.Function || expr instanceof Column) {
                    expr.accept(projParser);
                    elementsInSelect.addAll(projParser.getAttributes());
                }
            }
        }

        // Predicates
        if (ps.getWhere() != null) {
            PredicateParser predicateParser = new PredicateParser(this.db, relations);
            ps.getWhere().accept(predicateParser);
            elementsInSelect.addAll(predicateParser.getPredicates());
        }

        /*
        // Havings
        if (ps.getHaving() != null) {
            HavingUnroller havingUnroller = new HavingUnroller(this.relations, relations);
            ps.getHaving().accept(havingUnroller);
            for (Having having : havingUnroller.getHavings()) {
                Having valuelessHaving = new Having(having.getAttribute(), null, null, having.getFunction());
                // Having valuelessHaving = new Having(having.getAttribute(), having.getOp(), null, having.getFunction());
                selectQFs.add(valuelessHaving);
            }
        }

        // Group By
        if (ps.getGroupByColumnReferences() != null) {
            for (Expression expr : ps.getGroupByColumnReferences()) {
                if (expr instanceof Column) {
                    Attribute attr = ParserUtils.getAttributeFromColumn(this.db, relations, (Column) expr);

                    // We require that if we're grouping by, the attribute should be in the projection.
                    this.setGroupBy(elementsInSelect, attr);
                }
            }
        }*/

        // increment each qf count once
        /*
        List<QueryFragment> storedSelectQFs = new ArrayList<>();
        for (QueryFragment qf : selectQFs) {
            QueryFragment storedQF = this.getOrInsertQF(qf);
            storedQF.incrementCount();
            storedSelectQFs.add(storedQF);
        }

        // add co-occurrence values for each query fragment in relation to each other
        for (int i = 0; i < storedSelectQFs.size(); i++) {
            for (int j = i+1; j < storedSelectQFs.size(); j++) {
                this.addQFPair(storedSelectQFs.get(i), storedSelectQFs.get(j));
            }
        }

        // add co-occurrence values for subqueries
        for (QueryFragment superQf : storedSelectQFs) {
            for (QueryFragment subQf : subselectQFs) {
                this.addQFPair(superQf, subQf);
            }
        }*/

        return elementsInSelect;

    }
}
