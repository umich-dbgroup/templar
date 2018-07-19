package edu.umich.templar.log;

import edu.umich.templar.db.*;
import edu.umich.templar.log.parse.ParserUtils;
import edu.umich.templar.log.parse.PredicateParser;
import edu.umich.templar.log.parse.AttributeParser;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

import java.util.*;

public class LogFullTemplates {
    /*
    private Database db;
    private LogLevel mode;
    private Map<Set<DBElement>, Integer> setCounts;

    public static void main(String[] args) {
        // For testing
        String dbHost = args[0];
        Integer dbPort = Integer.valueOf(args[1]);
        String dbUser = args[2];
        String dbPass = args[3].equalsIgnoreCase("null")? null : args[3];

        Database database = new Database(dbHost, dbPort, dbUser, dbPass,
                "mas", "data/mas/mas.edges.json", "data/mas/mas.main_attrs.json",
                "data/mas/mas.proj_attrs.json");

        LogFullTemplates logFullTemplates = new LogFullTemplates(database, LogLevel.NO_CONST_OP);
        List<Select> selects = Utils.parseStatementsSequential("data/mas/mas_valid.ans");
        for (Select select : selects) {
            logFullTemplates.extractElementsFromSelect((PlainSelect) select.getSelectBody(), 0);
        }
    }

    public LogFullTemplates(Database db, LogLevel mode) {
        this.db = db;
        this.mode = mode;
        this.setCounts = new HashMap<>();
    }

    private Set<DBElement> modifyElementsForLevel(Set<DBElement> els) {
        // No modification required if FULL level
        if (this.mode.equals(LogLevel.FULL)) return els;

        Set<DBElement> newEls = new HashSet<>();
        for (DBElement el : els) {
            if (el instanceof AggregatedPredicate) {
                AggregatedPredicate pred = (AggregatedPredicate) el;

                if (this.mode.equals(LogLevel.NO_CONST_OP)) {
                    newEls.add(new AggregatedPredicate(pred.getAggFunction(), pred.getAttributePart(),
                            "?", pred.getValueFunction()));
                } else {
                    // No modifications involved for NO_CONST
                    newEls.add(el);
                }
            } else if (el instanceof NumericPredicate) {
                NumericPredicate pred = (NumericPredicate) el;

                if (this.mode.equals(LogLevel.NO_CONST)) {
                    newEls.add(new NumericPredicate(pred.getAttributePart(), pred.getOp(), 0.0, pred.getFunction()));
                } else if (this.mode.equals(LogLevel.NO_CONST_OP)) {
                    newEls.add(new NumericPredicate(pred.getAttributePart(), "?", 0.0, pred.getFunction()));
                }

            } else if (el instanceof TextPredicate) {
                TextPredicate pred = (TextPredicate) el;

                if (this.mode.equals(LogLevel.NO_CONST)) {
                    newEls.add(new TextPredicate(pred.getAttributePart(), ""));
                } else if (this.mode.equals(LogLevel.NO_CONST_OP)) {
                    newEls.add(new TextPredicate(pred.getAttributePart(), ""));
                }

            } else {
                newEls.add(el);
            }
        }
        return newEls;
    }

    private void setGroupBy(Set<DBElement> els, Attribute attr) {
        for (DBElement el : els) {
            if (el instanceof Attribute) {
                Attribute proj = (Attribute) el;
                if (proj.equals(attr)) {
                    els.add(new GroupedAttribute(attr));
                    els.remove(el);
                    return;
                }
            }
        }

        throw new RuntimeException("Attribute <" + attr + "> not found in previous elements list.");
    }

    public Set<DBElement> extractElementsFromSelect(PlainSelect ps, int subLevel) {
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
            elementsInSelect.addAll(this.extractElementsFromSelect(subPs, subLevel + 1));
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
                    elementsInSelect.addAll(this.extractElementsFromSelect(subPs, subLevel + 1));
                }
            }
        }

        // Projections
        for (SelectItem item : ps.getSelectItems()) {
            if (item instanceof SelectExpressionItem) {
                AttributeParser projParser = new AttributeParser(this.db, relations);
                Expression expr = ((SelectExpressionItem) item).getExpression();

                if (expr instanceof net.sf.jsqlparser.expression.Function || expr instanceof Column) {
                    expr.accept(projParser);
                    elementsInSelect.addAll(projParser.getAttributes());
                }
            }
        }

        // If any projections are aggregates of all columns, then find other projection and increment
        AggregatedAttribute aggregatedAllColumns = null;
        DBElement otherProjection = null;
        for (DBElement el : elementsInSelect) {
            if (el instanceof AggregatedAttribute) {
                AggregatedAttribute aggr = (AggregatedAttribute) el;
                if (aggr.getAttributePart().getName().equals("*")) {
                    aggregatedAllColumns = aggr;
                }
            } else if (el instanceof Attribute) {
                if (otherProjection != null) throw new RuntimeException("Already found other projection!");
                otherProjection = el;
            } else if (el instanceof GroupedAttribute) {
                if (otherProjection != null) throw new RuntimeException("Already found other projection!");
                otherProjection = el;
            }
        }
        if (aggregatedAllColumns != null && otherProjection != null) {
            if (otherProjection instanceof GroupedAttribute) {
                Attribute attr = ((GroupedAttribute) otherProjection).getAttributePart();
                elementsInSelect.add(new AggregatedAttribute(aggregatedAllColumns.getFunction(), attr));
            } else {
                elementsInSelect.add(new AggregatedAttribute(aggregatedAllColumns.getFunction(),
                        (Attribute) otherProjection));
            }
            elementsInSelect.remove(aggregatedAllColumns);
            elementsInSelect.remove(otherProjection);
        }

        // Predicates
        if (ps.getWhere() != null) {
            PredicateParser predicateParser = new PredicateParser(this.db, relations);
            ps.getWhere().accept(predicateParser);
            elementsInSelect.addAll(predicateParser.getPredicates());
        }

        // Havings
        if (ps.getHaving() != null) {
            PredicateParser havingParser = new PredicateParser(this.db, relations);
            ps.getHaving().accept(havingParser);
            elementsInSelect.addAll(havingParser.getPredicates());
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
        }

        // Top-1 Queries (assuming they are written in the form ORDER BY attribute DESC limit 1)
        if (ps.getOrderByElements() != null) {
            for (OrderByElement orderByElement : ps.getOrderByElements()) {
                String valueFunction = orderByElement.isAsc()? "min" : "max";

                AttributeParser orderParser = new AttributeParser(this.db, relations);
                orderByElement.getExpression().accept(orderParser);
                for (DBElement el : orderParser.getAttributes()) {
                    if (el instanceof AggregatedAttribute) {
                        AggregatedAttribute attr = (AggregatedAttribute) el;
                        elementsInSelect.add(new AggregatedPredicate(attr.getFunction(), attr.getAttributePart(),
                                "=", valueFunction));
                    } else if (el instanceof Attribute) {
                        Attribute attr = (Attribute) el;
                        elementsInSelect.add(new AggregatedPredicate(null, attr, "=", valueFunction));
                    }
                }
            }
        }

        // Only at top level, add to main log set
        if (subLevel == 0) {
            elementsInSelect = this.modifyElementsForLevel(elementsInSelect);
            this.setCounts.merge(elementsInSelect, 1, (a, b) -> a + b);
        }

        return elementsInSelect;
    }*/
}
