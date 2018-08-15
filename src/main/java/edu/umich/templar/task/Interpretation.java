package edu.umich.templar.task;

import edu.umich.templar.db.*;
import edu.umich.templar.db.el.*;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Distinct;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.SelectUtils;

import java.util.*;

public class Interpretation {
    private List<MatchedDBElement> mels;
    private double score;
    private JoinPath joinPath;

    public Interpretation(List<MatchedDBElement> mels) {
        this.mels = mels;
    }

    private void addDBElementToSelect(Select select, Map<Relation, List<Table>> tablesMap,
                                      DBElement el, int aliasLevel) {
        PlainSelect ps = (PlainSelect) select.getSelectBody();

        if (el instanceof AggregatedAttribute) {
            AggregatedAttribute aggrAttr = (AggregatedAttribute) el;

            Column col = aggrAttr.getAttribute().toColumn(tablesMap, aliasLevel);
            Function aggrFunc = new Function();
            aggrFunc.setName(aggrAttr.getAggrFunction());
            List<Expression> exprList = new ArrayList<>();
            exprList.add(col);
            aggrFunc.setParameters(new ExpressionList(exprList));

            if (aggrAttr.getValueFunction() != null) {
                Function valFunc = new Function();
                valFunc.setName(aggrAttr.getValueFunction());
                List<Expression> exprList2 = new ArrayList<>();
                exprList2.add(aggrFunc);
                valFunc.setParameters(new ExpressionList(exprList2));
                SelectUtils.addExpression(select, valFunc);
            } else {
                SelectUtils.addExpression(select, aggrFunc);
            }
        } else if (el instanceof AggregatedPredicate) {
            // TODO: complicated, because it's basically like a top 1 query-type deal
        } else if (el instanceof Attribute) {
            Attribute attr = (Attribute) el;
            Column col = attr.toColumn(tablesMap, aliasLevel);
            SelectUtils.addExpression(select, col);
        } else if (el instanceof AttributeAndPredicate) {
            AttributeAndPredicate attrAndPred = (AttributeAndPredicate) el;
            DBElement attrEl = attrAndPred.getAttributePart();
            this.addDBElementToSelect(select, tablesMap, attrEl, aliasLevel);
        } else if (el instanceof DuplicateDBElement) {
            DuplicateDBElement dbEl = (DuplicateDBElement) el;
            this.addDBElementToSelect(select, tablesMap, dbEl.getEl(), aliasLevel + 1);
        } else if (el instanceof GroupedAttribute) {
            GroupedAttribute attr = (GroupedAttribute) el;
            Column col = attr.getAttribute().toColumn(tablesMap, aliasLevel);
            SelectUtils.addExpression(select, col);
            SelectUtils.addGroupBy(select, col);
        } else if (el instanceof NumericPredicate) {
            NumericPredicate pred = (NumericPredicate) el;

            // Add attribute to projection
            Column col = pred.getAttribute().toColumn(tablesMap, aliasLevel);
            SelectUtils.addExpression(select, col);

            BinaryExpression binaryExpression;
            switch (pred.getOp()) {
                case "=":
                    binaryExpression = new EqualsTo();
                    break;
                case ">=":
                    binaryExpression = new GreaterThanEquals();
                    break;
                case "<=":
                    binaryExpression = new MinorThanEquals();
                    break;
                case ">":
                    binaryExpression = new GreaterThan();
                    break;
                case "<":
                    binaryExpression = new MinorThan();
                    break;
                default:
                    throw new RuntimeException("Unexpected operator: " + pred.getOp());
            }
            binaryExpression.setLeftExpression(pred.getAttribute().toColumn(tablesMap, aliasLevel));
            binaryExpression.setRightExpression(new DoubleValue(pred.getValue().toString()));

            Expression where = ps.getWhere();
            if (where == null) {
                ps.setWhere(binaryExpression);
            } else {
                AndExpression andExpression = new AndExpression(where, binaryExpression);
                ps.setWhere(andExpression);
            }
        } else if (el instanceof TextPredicate) {
            TextPredicate pred = (TextPredicate) el;

            // Add attribute to projection
            Column col = pred.getAttribute().toColumn(tablesMap, aliasLevel);
            SelectUtils.addExpression(select, col);

            Expression where = ps.getWhere();

            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(pred.getAttribute().toColumn(tablesMap, aliasLevel));
            equalsTo.setRightExpression(new StringValue(pred.getValue()));

            if (where == null) {
                ps.setWhere(equalsTo);
            } else {
                AndExpression andExpression = new AndExpression(where, equalsTo);
                ps.setWhere(andExpression);
            }
        }
    }

    public Statement getDistTupleSQL() {
        JoinPathNode root = this.joinPath.getFirstAlphabeticalNode();
        Set<JoinPathNode> visited = new HashSet<>();
        Queue<JoinPathNode> queue = new LinkedList<>();

        // Keep aliased tables
        Map<Relation, List<Table>> tablesMap = new HashMap<>();
        Map<JoinPathNode, Integer> aliasMap = new HashMap<>();

        Table rootTable = root.getRel().toTable();
        rootTable.setAlias(new Alias(root.getRel().getName() + "_0"));
        List<Table> rootTableList = new ArrayList<>();
        rootTableList.add(rootTable);
        tablesMap.put(root.getRel(), rootTableList);
        aliasMap.put(root, 0);

        Select select = SelectUtils.buildSelectFromTable(rootTable);

        PlainSelect ps = (PlainSelect) select.getSelectBody();
        ps.setDistinct(new Distinct());
        ps.setSelectItems(new ArrayList<>());

        visited.add(root);
        queue.add(root);

        while (!queue.isEmpty()) {
            JoinPathNode parent = queue.remove();
            for (JoinPathNode node : parent.getConnected()) {
                if (visited.contains(node)) continue;

                // Alias the table
                Table table = node.getRel().toTable();
                int count = tablesMap.get(node.getRel()) == null? 0 : tablesMap.get(node.getRel()).size();
                table.setAlias(new Alias(node.getRel().getName() + "_" + count));

                List<Table> nodeTableList = tablesMap.computeIfAbsent(node.getRel(), k -> new ArrayList<>());
                aliasMap.put(node, nodeTableList.size());
                nodeTableList.add(table);

                JoinOn joinOn = parent.getJoinOn(node);
                EqualsTo joinCond = new EqualsTo();

                // Find which table is which
                if (joinOn.getPk().getRelation().equals(parent.getRel())) {
                    joinCond.setLeftExpression(joinOn.getPk().toColumn(tablesMap, aliasMap.get(root)));
                    joinCond.setRightExpression(joinOn.getFk().toColumn(tablesMap, aliasMap.get(node)));
                } else {
                    joinCond.setLeftExpression(joinOn.getPk().toColumn(tablesMap, aliasMap.get(node)));
                    joinCond.setRightExpression(joinOn.getFk().toColumn(tablesMap, aliasMap.get(root)));
                }

                SelectUtils.addJoin(select, table, joinCond);

                visited.add(node);
                queue.add(node);
            }
        }

        Map<Attribute, Integer> aliasLevels = new HashMap<>();
        for (MatchedDBElement mel : mels) {
            // If projection or equality predicate, need to raise the alias level
            Integer aliasLevel = 0;
            if (mel.getEl() instanceof Attribute ||
                    mel.getEl() instanceof TextPredicate ||
                    (mel.getEl() instanceof NumericPredicate && ((NumericPredicate) mel.getEl()).getOp().equals("="))) {
                Attribute attr = mel.getEl().getAttribute();
                aliasLevel = aliasLevels.get(attr);
                if (aliasLevel == null) {
                    aliasLevel = 0;
                } else {
                    aliasLevel = aliasLevel + 1;
                }
                aliasLevels.put(attr, aliasLevel);
            }
            this.addDBElementToSelect(select, tablesMap, mel.getEl(), aliasLevel);
        }

        return select;
    }

    public List<MatchedDBElement> getElements() {
        return mels;
    }

    public void add(MatchedDBElement mel) {
        this.mels.add(mel);
    }

    public MatchedDBElement get(int i) {
        return this.mels.get(i);
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public JoinPath getJoinPath() {
        return joinPath;
    }

    public void setJoinPath(JoinPath joinPath) {
        this.joinPath = joinPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Interpretation that = (Interpretation) o;
        return Objects.equals(mels, that.mels) &&
                Objects.equals(joinPath, that.joinPath);
    }

    @Override
    public int hashCode() {

        return Objects.hash(mels, joinPath);
    }
}
