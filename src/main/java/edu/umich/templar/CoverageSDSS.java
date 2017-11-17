package edu.umich.templar;

import com.esotericsoftware.minlog.Log;
import edu.umich.templar.qf.*;
import edu.umich.templar.qf.Limit;
import edu.umich.templar.rdbms.Attribute;
import edu.umich.templar.rdbms.Relation;
import edu.umich.templar.rdbms.SchemaGraph;
import edu.umich.templar.sqlparse.*;
import edu.umich.templar.template.Template;
import edu.umich.templar.template.TemplateRoot;
import edu.umich.templar.util.CoverageHelper;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by cjbaik on 8/31/17.
 */
public class CoverageSDSS extends CoverageHelper {
    public Map<String, Relation> relations = new HashMap<>();

    public CoverageSDSS(String outFileName, String errFileName) {
        super(outFileName, errFileName);
    }

    public List<QueryFragment> fullFrag(PlainSelect ps) {
        List<QueryFragment> selectQFs = new ArrayList<>();

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
        } else if (ps.getFromItem() instanceof TableFunction) {
            TableFunction func = (TableFunction) ps.getFromItem();
            Relation rel = new edu.umich.templar.rdbms.Function(func.getFunction().getName(), "function", new HashMap<>(), new HashMap<>());
            selectQFs.add(new RelationFragment(rel));
        } else if (ps.getFromItem() instanceof SubSelect) {
            // in the case it is a subquery
            PlainSelect subPs = (PlainSelect) ((SubSelect) ps.getFromItem()).getSelectBody();
            selectQFs.addAll(this.fullFrag(subPs));
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
                } else if (join.getRightItem() instanceof TableFunction) {
                    TableFunction func = (TableFunction) join.getRightItem();
                    Relation rel = new edu.umich.templar.rdbms.Function(func.getFunction().getName(), "function", new HashMap<>(), new HashMap<>());
                    selectQFs.add(new RelationFragment(rel));
                } else if (join.getRightItem() instanceof SubSelect) {
                    PlainSelect subPs = (PlainSelect) ((SubSelect) join.getRightItem()).getSelectBody();
                    selectQFs.addAll(this.fullFrag(subPs));
                }
            }
        }

        // Projections
        for (SelectItem item : ps.getSelectItems()) {
            if (item instanceof SelectExpressionItem) {
                FullProjectionUnroller projUnroller = new FullProjectionUnroller(this.relations, relations);
                projUnroller.visit((SelectExpressionItem) item);
                selectQFs.addAll(projUnroller.getProjections());
            }
        }

        // Predicates
        if (ps.getWhere() != null) {
            FullPredicateUnroller predicateUnroller = new FullPredicateUnroller(this.relations, relations);
            ps.getWhere().accept(predicateUnroller);
            for (Predicate pred : predicateUnroller.getPredicates()) {
                Predicate valuelessPred = new Predicate(pred.getAttribute(), pred.getOp(), pred.getValue());
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
                Having valuelessHaving = new Having(having.getAttribute(), having.getOp(), having.getValue(), having.getFunction());
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
        return selectQFs;
    }

    public List<QueryFragment> noConstFrag(PlainSelect ps) {
        List<QueryFragment> selectQFs = new ArrayList<>();

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
        } else if (ps.getFromItem() instanceof TableFunction) {
            TableFunction func = (TableFunction) ps.getFromItem();
            Relation rel = new edu.umich.templar.rdbms.Function(func.getFunction().getName(), "function", new HashMap<>(), new HashMap<>());
            selectQFs.add(new RelationFragment(rel));
        } else if (ps.getFromItem() instanceof SubSelect) {
            // in the case it is a subquery
            PlainSelect subPs = (PlainSelect) ((SubSelect) ps.getFromItem()).getSelectBody();
            selectQFs.addAll(this.noConstFrag(subPs));
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
                } else if (join.getRightItem() instanceof TableFunction) {
                    TableFunction func = (TableFunction) join.getRightItem();
                    Relation rel = new edu.umich.templar.rdbms.Function(func.getFunction().getName(), "function", new HashMap<>(), new HashMap<>());
                    selectQFs.add(new RelationFragment(rel));
                } else if (join.getRightItem() instanceof SubSelect) {
                    PlainSelect subPs = (PlainSelect) ((SubSelect) join.getRightItem()).getSelectBody();
                    selectQFs.addAll(this.noConstFrag(subPs));
                }
            }
        }

        // Projections
        for (SelectItem item : ps.getSelectItems()) {
            if (item instanceof SelectExpressionItem) {
                NoConstProjectionUnroller projUnroller = new NoConstProjectionUnroller(this.relations, relations);
                projUnroller.visit((SelectExpressionItem) item);
                selectQFs.addAll(projUnroller.getProjections());
            }
        }

        // Predicates
        if (ps.getWhere() != null) {
            NoConstPredicateUnroller predicateUnroller = new NoConstPredicateUnroller(this.relations, relations);
            ps.getWhere().accept(predicateUnroller);
            for (Predicate pred : predicateUnroller.getPredicates()) {
                Predicate valuelessPred = new Predicate(pred.getAttribute(), pred.getOp(), null);
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
                Having valuelessHaving = new Having(having.getAttribute(), having.getOp(), null, having.getFunction());
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
        return selectQFs;
    }

    public List<QueryFragment> noConstOpFrag(PlainSelect ps) {
        List<QueryFragment> selectQFs = new ArrayList<>();

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
            selectQFs.addAll(this.noConstOpFrag(subPs));
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
                    selectQFs.addAll(this.noConstOpFrag(subPs));
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
                Predicate noConstOpPred = new Predicate(pred.getAttribute(), null, null);
                Relation noAliasRel = new Relation(pred.getAttribute().getRelation());
                noAliasRel.setAliasInt(0);
                noConstOpPred.getAttribute().setRelation(noAliasRel);
                selectQFs.add(noConstOpPred);
            }
        }

        // Havings
        if (ps.getHaving() != null) {
            HavingUnroller havingUnroller = new HavingUnroller(this.relations, relations);
            ps.getHaving().accept(havingUnroller);
            for (Having having : havingUnroller.getHavings()) {
                Having valuelessHaving = new Having(having.getAttribute(), null, null, having.getFunction());
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
        return selectQFs;
    }

    public void performFixedTestSet(LogTemplateGenerator logGen, List<Select> stmts) {
        double halfLog = Math.ceil(0.5 * stmts.size());
        List<Select> trainQueries = stmts.subList(0, (int) halfLog);
        List<Select> testQueries = stmts.subList((int) halfLog, stmts.size());

        /*
        Set<Template> logPredProjTmpl = logGen.generate(trainQueries, TemplateRoot::noPredicateProjectionTemplate);
        float logPredProjCoverage = this.calculateCoveragePercent(logPredProjTmpl, testQueries, TemplateRoot::noPredicateProjectionTemplate, false);

        Set<Template> logPredTmpl = logGen.generate(trainQueries, TemplateRoot::noPredicateTemplate);
        float logPredCoverage = this.calculateCoveragePercent(logPredTmpl, testQueries, TemplateRoot::noPredicateTemplate, false);

        Set<Template> logAttrConstTmpl = logGen.generate(trainQueries, TemplateRoot::noAttributeConstantTemplate);
        float logAttrConstCoverage = this.calculateCoveragePercent(logAttrConstTmpl, testQueries, TemplateRoot::noAttributeConstantTemplate, false);

        Set<Template> logCompProjTmpl = logGen.generate(trainQueries, TemplateRoot::noComparisonProjectionTemplate);
        float logCompProjCoverage = this.calculateCoveragePercent(logCompProjTmpl, testQueries, TemplateRoot::noComparisonProjectionTemplate, false);

        Set<Template> logCompTmpl = logGen.generate(trainQueries, TemplateRoot::noComparisonTemplate);
        float logCompCoverage = this.calculateCoveragePercent(logCompTmpl, testQueries, TemplateRoot::noComparisonTemplate, false);

        Set<Template> logConstProjTmpl = logGen.generate(trainQueries, TemplateRoot::noConstantProjectionTemplate);
        float logConstProjCoverage = this.calculateCoveragePercent(logConstProjTmpl, testQueries, TemplateRoot::noConstantProjectionTemplate, false);

        Set<Template> logConstTmpl = logGen.generate(trainQueries, TemplateRoot::noConstantTemplate);
        float logConstCoverage = this.calculateCoveragePercent(logConstTmpl, testQueries, TemplateRoot::noConstantTemplate, false);

        Set<Template> logFullTmpl = logGen.generate(trainQueries, TemplateRoot::fullQueryTemplate);
        float logFullCoverage = this.calculateCoveragePercent(logFullTmpl, testQueries, TemplateRoot::fullQueryTemplate, false);

        Log.info("Training Set Size: " + trainQueries.size());
        Log.info("Coverage Test Set Size: " + testQueries.size());
        Log.info("           \tc\tc_p\tcm\tcm_p\ta\tpd\tpd_p\tfull");
        Log.info("Log Coverage %:\t"
                + String.format("%.1f", logConstCoverage) + "%\t"
                + String.format("%.1f", logConstProjCoverage) + "%\t"
                + String.format("%.1f", logCompCoverage) + "%\t"
                + String.format("%.1f", logCompProjCoverage) + "%\t"
                + String.format("%.1f", logAttrConstCoverage) + "%\t"
                + String.format("%.1f", logPredCoverage) + "%\t"
                + String.format("%.1f", logPredProjCoverage) + "%\t"
                + String.format("%.1f", logFullCoverage) + "%\t");
        Log.info("Log Tmpl. Count:\t"
                + logConstTmpl.size() + "\t"
                + logConstProjTmpl.size() + "\t"
                + logCompTmpl.size() + "\t"
                + logCompProjTmpl.size() + "\t"
                + logAttrConstTmpl.size() + "\t"
                + logPredTmpl.size() + "\t"
                + logPredProjTmpl.size() + "\t"
                + logFullTmpl.size() + "\t\n");*/

        this.getCoverage("Full", this::fullFrag, trainQueries, testQueries);
        this.getCoverage("NoConst", this::noConstFrag, trainQueries, testQueries);
        this.getCoverage("NoConstOp", this::noConstOpFrag, trainQueries, testQueries);
    }

    public String getFullQueryString(List<QueryFragment> qfList) {
        return qfList.stream().map(QueryFragment::toString).sorted().collect(Collectors.joining(","));
    }

    public void getCoverage(String level, Function<PlainSelect, List<QueryFragment>> fragGen,
                                List<Select> trainQueries, List<Select> testQueries) {
        Set<QueryFragment> qfList = new HashSet<>();
        Set<String> fullQueries = new HashSet<>();
        for (Select train : trainQueries) {
            List<QueryFragment> frags = fragGen.apply((PlainSelect) train.getSelectBody());
            qfList.addAll(frags);
            fullQueries.add(this.getFullQueryString(frags));
        }

        int fullCoveredCount = 0;
        int fragCoveredCount = 0;
        for (Select test : testQueries) {
            boolean fragCovered = true;
            List<QueryFragment> testQfList = fragGen.apply((PlainSelect) test.getSelectBody());
            for (QueryFragment qf : testQfList) {
                if (!qfList.contains(qf)) {
                    fragCovered = false;
                    break;
                }
            }
            String fullTestQuery = this.getFullQueryString(testQfList);
            if (fullQueries.contains(fullTestQuery)) fullCoveredCount++;
            if (fragCovered) fragCoveredCount++;
        }

        double fullCov = (double) fullCoveredCount / testQueries.size() * 100;
        Log.info(level + " Templates: " + qfList.size());
        Log.info(level + " Coverage: " + fullCoveredCount + "/" + testQueries.size() + " (" + String.format("%.1f", fullCov) + "%)");

        double fragCov = (double) fragCoveredCount / testQueries.size() * 100;
        Log.info(level + "Frag Templates: " + qfList.size());
        Log.info(level + "Frag Coverage: " + fragCoveredCount + "/" + testQueries.size() + " (" + String.format("%.1f", fragCov) + "%)");
    }


    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: CoverageSDSS <query-log-filename>");
            System.err.println("Example: CoverageSDSS data/sdss/final/bestdr7_0.05.parsed");
            System.exit(1);
        }

        String queryLogFilename = args[0];

        CoverageSDSS cov = new CoverageSDSS("templates.out", "errors.out");
        SchemaGraph schemaGraph = null;
        try {
            schemaGraph = new SchemaGraph("data/sdss/schema/bestdr7");
            TemplateRoot.relations = schemaGraph.relations;
            cov.relations = schemaGraph.relations;
        } catch (Exception e) {
            e.printStackTrace();
        }

        LogTemplateGenerator logGen = new LogTemplateGenerator();

        Log.info("Parsing statements...");
        List<Select> stmts = Utils.parseStatements(queryLogFilename);
        Log.info("Done parsing statements.");

        Log.info("Filtering template-able statements...");
        List<Select> parsed = new ArrayList<>();
        for (Select stmt : stmts) {
            try {
                cov.fullFrag((PlainSelect) stmt.getSelectBody());
                parsed.add(stmt);
            } catch (Exception e) {
                // Silent. Don't parse.
            }
        }

        cov.performFixedTestSet(logGen, parsed);
        cov.finish();
    }
}
