package edu.umich.templar.template;

import edu.umich.templar.qf.*;
import edu.umich.templar.rdbms.Attribute;
import edu.umich.templar.rdbms.RDBMS;
import edu.umich.templar.rdbms.Relation;
import edu.umich.templar.sqlparse.HavingUnroller;
import edu.umich.templar.sqlparse.PredicateUnroller;
import edu.umich.templar.sqlparse.ProjectionUnroller;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import org.apache.commons.collections.Bag;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;

/**
 * Created by cjbaik on 10/10/17.
 */
public class NLSQLLogCounter {
    Map<String, Relation> relations; // relations from schema

    QueryFragmentCounter universalCounter;            // main counter for all query fragments, regardless of tokens
    Map<String, QueryFragmentCounter> tokenCounters;   // keeps a query fragment counter for each token

    public static void main(String[] args) {
        RDBMS db;
        try {
            db = new RDBMS("senlidb", "data/senlidb/senlidb");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        NLSQLLogCounter nlsqlLogCounter = new NLSQLLogCounter(db.schemaGraph.relations);

        List<String> nlq = new ArrayList<>();
        try {
            nlq = FileUtils.readLines(new File("data/senlidb/test.txt"), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<Select> selects = Utils.parseStatements("data/senlidb/test.ans");
        for (int i = 0; i < selects.size(); i++) {
            String[] tokens = StringUtils.split(nlq.get(i));
            nlsqlLogCounter.addNLQSQLPair(Arrays.asList(tokens), selects.get(i));
        }

        // QueryFragmentCounter counter =  nlsqlLogCounter.getTokenCounters().get("people");
        QueryFragmentCounter counter = nlsqlLogCounter.getUniversalCounter();

        Bag counterBag = counter.getCounter();
        Map<String, Integer> fragByFrequency = new HashMap<>();
        for (Object item : counterBag) {
            fragByFrequency.put((String) item, counterBag.getCount(item));
        }

        Map<String, Integer> sortedFragByFrequency = Utils.sortByValueDesc(fragByFrequency);
        for (Map.Entry<String, Integer> e : sortedFragByFrequency.entrySet()) {
            System.out.println(e.getKey() + ": " + counter.getCounter().getCount(e.getKey()));
        }
    }

    public NLSQLLogCounter(Map<String, Relation> relations) {
        this.universalCounter = new QueryFragmentCounter();
        this.tokenCounters = new HashMap<>();
        this.relations = relations;
        Map<String, Relation> lowercaseRelations = new HashMap<>();

        // TODO: Kind of a hack, but for each relation, save the relation to a lowercase version of the key as well
        for (Map.Entry<String, Relation> e : this.relations.entrySet()) {
            lowercaseRelations.put(e.getKey().toLowerCase(), e.getValue());
        }

        this.relations.putAll(lowercaseRelations);
    }

    public QueryFragmentCounter getUniversalCounter() {
        return universalCounter;
    }

    public Map<String, QueryFragmentCounter> getTokenCounters() {
        return tokenCounters;
    }

    // If tokens are null or empty, we only count universal tokens
    public void addNLQSQLPair(List<String> tokens, Select select) {
        // Only handle PlainSelect
        if (!(select.getSelectBody() instanceof PlainSelect)) return;

        // Parse Select into query fragments
        PlainSelect ps = (PlainSelect) select.getSelectBody();

        List<Projection> projections = new ArrayList<>();
        List<Relation> relations = new ArrayList<>();
        List<Predicate> predicates = new ArrayList<>();
        List<Having> havings = new ArrayList<>();
        List<Superlative> superlatives = new ArrayList<>();

        // System.out.println(select);

        // Relations
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
                }
            }
        }

        // Projections
        for (SelectItem item : ps.getSelectItems()) {
            if (item instanceof SelectExpressionItem) {
                ProjectionUnroller projUnroller = new ProjectionUnroller(this.relations, relations);
                Expression expr = ((SelectExpressionItem) item).getExpression();
                if (expr instanceof Function || expr instanceof Column) {
                    expr.accept(projUnroller);
                    projections.addAll(projUnroller.getProjections());
                }
            }
        }
        // Check group by for projections
        if (ps.getGroupByColumnReferences() != null) {
            for (Expression expr : ps.getGroupByColumnReferences()) {
                if (expr instanceof Column) {
                    Attribute attr = Utils.getAttributeFromColumn(this.relations, relations, (Column) expr);
                    if (attr != null) {
                        for (Projection proj : projections) {
                            if (attr.hasSameRelationNameAndNameAs(proj.getAttribute())) {
                                proj.setGroupBy(true);
                            }
                        }
                    }
                }
            }
        }

        // Predicates
        if (ps.getWhere() != null) {
            PredicateUnroller predicateUnroller = new PredicateUnroller(this.relations, relations);
            ps.getWhere().accept(predicateUnroller);
            predicates.addAll(predicateUnroller.getPredicates());
        }

        // Havings
        if (ps.getHaving() != null) {
            HavingUnroller havingUnroller = new HavingUnroller(this.relations, relations);
            ps.getHaving().accept(havingUnroller);
            havings.addAll(havingUnroller.getHavings());
        }

        // TODO: superlatives (ORDER BY (maybe with function) + LIMIT)

        // For each token, bag the objects
        if (tokens != null) {
            for (String token : tokens) {
                token = token.toLowerCase();
                QueryFragmentCounter tkCounter = this.tokenCounters.get(token);
                if (tkCounter == null) {
                    tkCounter = new QueryFragmentCounter();
                    this.tokenCounters.put(token, tkCounter);
                }

                for (Relation relation : relations) {
                    tkCounter.addRelation(relation);
                }
                for (Projection proj : projections) {
                    tkCounter.addFragment(proj);
                }
                for (Predicate pred : predicates) {
                    tkCounter.addFragment(pred);
                }
                for (Having having : havings) {
                    tkCounter.addFragment(having);
                }
                for (Superlative superlative : superlatives) {
                    tkCounter.addFragment(superlative);
                }
            }
        }

        // Add everything to universal counter
        for (Relation relation : relations) {
            this.universalCounter.addRelation(relation);
        }
        for (Projection proj : projections) {
            this.universalCounter.addFragment(proj);
        }
        for (Predicate pred : predicates) {
            this.universalCounter.addFragment(pred);
        }
        for (Having having : havings) {
            this.universalCounter.addFragment(having);
        }
        for (Superlative superlative : superlatives) {
            this.universalCounter.addFragment(superlative);
        }
    }

    public double getUniversalFreqScore(QueryFragment qf) {
        return this.universalCounter.getFreqScore(qf);
    }

    public double getUniversalFreqScore(Relation r) {
        return this.universalCounter.getFreqScore(r);
    }

    public double getFreqScore(String token, QueryFragment qf) {
        QueryFragmentCounter qfCounter = this.tokenCounters.get(token);
        if (qfCounter == null) {
            return 0;
        }
        return qfCounter.getFreqScore(qf);
    }

    public double getFreqScore(String token, Relation r) {
        QueryFragmentCounter qfCounter = this.tokenCounters.get(token);
        if (qfCounter == null) {
            return 0;
        }
        return qfCounter.getFreqScore(r);
    }

    // TODO: Are there any fragments we didn't map to? Add them in.
    public List<QueryFragment> getTopNFragments(String token, int n) {
        return new ArrayList<>();
    }
}
