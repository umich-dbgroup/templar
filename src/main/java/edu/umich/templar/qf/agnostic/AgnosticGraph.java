package edu.umich.templar.qf.agnostic;

import edu.umich.templar.qf.pieces.QFFunction;
import edu.umich.templar.rdbms.Attribute;
import edu.umich.templar.rdbms.RDBMS;
import edu.umich.templar.rdbms.Relation;
import edu.umich.templar.sqlparse.agnostic.AgnosticHavingUnroller;
import edu.umich.templar.sqlparse.agnostic.AgnosticPredicateUnroller;
import edu.umich.templar.sqlparse.agnostic.AgnosticProjectionUnroller;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by cjbaik on 10/16/17.
 */
public class AgnosticGraph {
    Map<String, Relation> relations;     // relation info
    List<AgnosticQueryFragment> qfList;  // all query fragments in graph

    public AgnosticGraph(Map<String, Relation> relations) {
        this.relations = relations;

        this.qfList = new ArrayList<>();
    }

    public static void main(String[] args) {
        RDBMS db;
        try {
            db = new RDBMS("mas", "data/mas/mas");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        AgnosticGraph agnosticGraph = new AgnosticGraph(db.schemaGraph.relations);

        List<Select> selects = Utils.parseStatements("data/mas/mas_all.ans");
        for (Select select : selects) {
            agnosticGraph.analyzeSelect((PlainSelect) select.getSelectBody());
        }

        System.out.println("CHECK");
    }

    public AgnosticQueryFragment getOrInsertQF(AgnosticQueryFragment qf) {
        for (AgnosticQueryFragment listQf : this.qfList) {
            if (qf.equals(listQf)) {
                return listQf;
            }
        }

        this.qfList.add(qf);
        return qf;
    }

    public void addQFPair(AgnosticQueryFragment first, AgnosticQueryFragment second) {
        first = this.getOrInsertQF(first);
        second = this.getOrInsertQF(second);

        first.incrementCooccurrence(second);
        if (!first.equals(second)) {
            second.incrementCooccurrence(first);
        }
    }

    public List<AgnosticQueryFragment> analyzeSelect(PlainSelect ps) {
        List<AgnosticQueryFragment> selectQFs = new ArrayList<>();
        List<AgnosticQueryFragment> subselectQFs = new ArrayList<>();

        // Relations

        // Add agnostic relation fragment (because there's no queries without any relations)
        selectQFs.add(new AgnosticRelationFragment());

        List<Relation> relations = new ArrayList<>();
        if (ps.getFromItem() instanceof Table) {
            Table table = (Table) ps.getFromItem();
            Relation rel = this.relations.get(table.getName());
            if (rel != null) {
                rel = new Relation(rel);
                if (table.getAlias() != null) {
                    rel.getAliasSet().add(table.getAlias().getName().trim());
                }
                relations.add(rel);
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
                        relations.add(rel);
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
                AgnosticProjectionUnroller projUnroller = new AgnosticProjectionUnroller(this.relations, relations);
                Expression expr = ((SelectExpressionItem) item).getExpression();

                if (expr instanceof net.sf.jsqlparser.expression.Function || expr instanceof Column) {
                    expr.accept(projUnroller);
                    selectQFs.addAll(projUnroller.getProjections());
                }
            }
        }

        // Predicates
        if (ps.getWhere() != null) {
            AgnosticPredicateUnroller predicateUnroller = new AgnosticPredicateUnroller(this.relations, relations);
            ps.getWhere().accept(predicateUnroller);
            selectQFs.addAll(predicateUnroller.getPredicates());
        }

        // Havings
        if (ps.getHaving() != null) {
            AgnosticHavingUnroller havingUnroller = new AgnosticHavingUnroller(this.relations, relations);
            ps.getHaving().accept(havingUnroller);
            selectQFs.addAll(havingUnroller.getHavings());
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
                        QFFunction qfFunction = null;
                        if (functionName != null) {
                            qfFunction = QFFunction.getFunction(functionName);
                        }
                        selectQFs.add(new AgnosticOrderBy(attr.getAttributeType(), qfFunction, !orderBy.isAsc()));
                    }
                }
            }
        }

        // Limit/Top
        if (ps.getLimit() != null || ps.getTop() != null) {
            selectQFs.add(new AgnosticLimit());
        }

        // Group By
        if (ps.getGroupByColumnReferences() != null) {
            for (Expression expr : ps.getGroupByColumnReferences()) {
                if (expr instanceof Column) {
                    Attribute attr = Utils.getAttributeFromColumn(this.relations, relations, (Column) expr);
                    if (attr != null) {
                        selectQFs.add(new AgnosticGroupBy(attr.getAttributeType()));
                    }
                }
            }
        }

        // increment each qf count once
        List<AgnosticQueryFragment> storedSelectQFs = new ArrayList<>();
        for (AgnosticQueryFragment qf : selectQFs) {
            AgnosticQueryFragment storedQF = this.getOrInsertQF(qf);
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
        for (AgnosticQueryFragment superQf : storedSelectQFs) {
            for (AgnosticQueryFragment subQf : subselectQFs) {
                this.addQFPair(superQf, subQf);
            }
        }

        return selectQFs;
    }
}
