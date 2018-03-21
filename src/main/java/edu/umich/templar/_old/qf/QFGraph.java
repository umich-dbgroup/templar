package edu.umich.templar._old.qf;

import edu.umich.templar._old.rdbms.Attribute;
import edu.umich.templar._old.rdbms.RDBMS;
import edu.umich.templar._old.rdbms.Relation;
import edu.umich.templar._old.sqlparse.HavingUnroller;
import edu.umich.templar._old.sqlparse.PredicateUnroller;
import edu.umich.templar._old.sqlparse.ProjectionUnroller;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by cjbaik on 10/18/17.
 */
public class QFGraph {
    Map<String, Relation> relations;   // relation info
    List<QueryFragment> qfList;        // all query fragments in graph

    Double maxDice;

    public QFGraph(Map<String, Relation> relations) {
        this.relations = relations;

        this.qfList = new ArrayList<>();

        this.maxDice = null;
    }

    public static void main(String[] args) {
        RDBMS db;
        try {
            db = new RDBMS("mas", "data/mas/mas");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        QFGraph qfGraph = new QFGraph(db.schemaGraph.relations);

        List<Select> selects = Utils.parseStatements("data/mas/temp.ans");
        for (Select select : selects) {
            qfGraph.analyzeSelect((PlainSelect) select.getSelectBody());
        }

        System.out.println("CHECK");
    }

    public QueryFragment getOrInsertQF(QueryFragment qf) {
        if (qf instanceof BlankQueryFragment) return qf;

        for (QueryFragment listQf : this.qfList) {
            if (qf.equals(listQf)) {
                return listQf;
            }
        }

        this.qfList.add(qf);
        return qf;
    }

    public void addQFPair(QueryFragment first, QueryFragment second) {
        first = this.getOrInsertQF(first);
        second = this.getOrInsertQF(second);

        first.incrementCooccurrence(second);
        if (!first.equals(second)) {
            second.incrementCooccurrence(first);
        }
    }

    public List<QueryFragment> analyzeSelect(PlainSelect ps) {
        List<QueryFragment> selectQFs = new ArrayList<>();
        List<QueryFragment> subselectQFs = new ArrayList<>();

        // Relations
        List<Relation> relations = new ArrayList<>();
        if (ps.getFromItem() instanceof Table) {
            Table table = (Table) ps.getFromItem();
            Relation rel = this.relations.get(table.getName());
            if (rel != null) {
                rel = new Relation(rel);
                if (table.getAlias() != null) {
                    rel.getAliasSet().add(table.getAlias().getName().trim());
                }
                int aliasInt = 0;
                while (relations.contains(rel)) {
                    rel = new Relation(rel);
                    rel.setAliasInt(++aliasInt);
                }
                relations.add(rel);
                selectQFs.add(new RelationFragment(rel));
            }
        } else if (ps.getFromItem() instanceof SubSelect) {
            // in the case it is a subquery
            PlainSelect subPs = (PlainSelect) ((SubSelect) ps.getFromItem()).getSelectBody();
            subselectQFs.addAll(this.analyzeSelect(subPs));
        }

        if (ps.getJoins() != null) {
            for (Join join : ps.getJoins()) {
                if (join.getRightItem() instanceof Table) {
                    Table table = (Table) join.getRightItem();
                    Relation rel = this.relations.get(table.getName());
                    if (rel != null) {
                        rel = new Relation(rel);
                        if (table.getAlias() != null) {
                            rel.getAliasSet().add(table.getAlias().getName().trim());
                        }
                        int aliasInt = 0;
                        while (relations.contains(rel)) {
                            rel = new Relation(rel);
                            rel.setAliasInt(++aliasInt);
                        }
                        relations.add(rel);
                        selectQFs.add(new RelationFragment(rel));
                    }
                } else if (join.getRightItem() instanceof SubSelect) {
                    PlainSelect subPs = (PlainSelect) ((SubSelect) join.getRightItem()).getSelectBody();
                    subselectQFs.addAll(this.analyzeSelect(subPs));
                }
            }
        }

        // Projections
        for (SelectItem item : ps.getSelectItems()) {
            if (item instanceof SelectExpressionItem) {
                ProjectionUnroller projUnroller = new ProjectionUnroller(this.relations, relations);
                Expression expr = ((SelectExpressionItem) item).getExpression();

                if (expr instanceof net.sf.jsqlparser.expression.Function || expr instanceof Column) {
                    expr.accept(projUnroller);
                    selectQFs.addAll(projUnroller.getProjections());
                }
            }
        }

        // Predicates
        if (ps.getWhere() != null) {
            PredicateUnroller predicateUnroller = new PredicateUnroller(this.relations, relations);
            ps.getWhere().accept(predicateUnroller);
            for (Predicate pred : predicateUnroller.getPredicates()) {
                Predicate valuelessPred = new Predicate(pred.getAttribute(), null, null);
                // NumericPredicate valuelessPred = new NumericPredicate(pred.getAttribute(), pred.getOp(), null);
                Relation noAliasRel = new Relation(pred.getAttribute().getRelation());
                noAliasRel.setAliasInt(0);
                valuelessPred.getAttribute().setRelation(noAliasRel);
                selectQFs.add(valuelessPred);
            }
        }

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

        // Order By
        if (ps.getOrderByElements() != null) {
            for (OrderByElement orderBy : ps.getOrderByElements()) {
                Column col = null;
                String functionName = null;

                if (orderBy.getExpression() instanceof Column) {
                    col = (Column) orderBy.getExpression();
                } else if (orderBy.getExpression() instanceof net.sf.jsqlparser.expression.Function) {
                    net.sf.jsqlparser.expression.Function func = (net.sf.jsqlparser.expression.Function) orderBy.getExpression();
                    col = Utils.getColumnFromFunction(func);
                    functionName = func.getName();
                }

                if (col != null) {
                    Attribute attr = Utils.getAttributeFromColumn(this.relations, relations, col);
                    if (attr != null) {
                        selectQFs.add(new OrderBy(attr, functionName, !orderBy.isAsc()));
                    }
                }
            }
        }

        // Limit/Top
        if (ps.getLimit() != null || ps.getTop() != null) {
            selectQFs.add(new Limit());
        }

        // Group By
        if (ps.getGroupByColumnReferences() != null) {
            for (Expression expr : ps.getGroupByColumnReferences()) {
                if (expr instanceof Column) {
                    Attribute attr = Utils.getAttributeFromColumn(this.relations, relations, (Column) expr);
                    if (attr != null) {
                        selectQFs.add(new GroupBy(attr));
                    }
                }
            }
        }

        // increment each qf count once
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
        }

        return selectQFs;
    }
}
