package edu.umich.tbnalir.util;

import edu.umich.tbnalir.rdbms.Attribute;
import edu.umich.tbnalir.rdbms.Function;
import edu.umich.tbnalir.rdbms.FunctionParameter;
import edu.umich.tbnalir.rdbms.Relation;
import edu.umich.tbnalir.sql.ConstantRemovalExprDeParser;
import edu.umich.tbnalir.sql.LiteralExpression;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by cjbaik on 6/20/17.
 */
public class Utils {
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValueDesc(Map<K, V> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public static boolean isSQLTypeString(String type) {
        switch (type) {
            case "varchar":
            case "varbinary":
            case "binary":
            case "sysname":
            case "text":
                return true;
            default:
                return false;
        }
    }

    public static boolean isSQLTypeNumeric(String type) {
        switch (type) {
            case "real":
            case "float":
            case "int":
            case "bigint":
            case "smallint":
            case "tinyint":
            case "bit":
                return true;
            default:
                return false;
        }
    }

    public static boolean isSQLTypeDate(String type) {
        return type.equals("date");
    }

    public static boolean isSQLTypeDateTime(String type) {
        return type.equals("datetime");
    }

    public static boolean isSQLTypeTime(String type) {
        return type.equals("time");
    }

    public static boolean isSQLTypeTimestamp(String type) {
        return type.equals("timestamp");
    }

    public static String convertSQLTypetoConstant(String type) {
        if (Utils.isSQLTypeNumeric(type)) return Constants.NUM;
        if (Utils.isSQLTypeString(type)) return Constants.STR;
        if (Utils.isSQLTypeDateTime(type)) return Constants.DATETIME;
        if (Utils.isSQLTypeDate(type)) return Constants.DATE;
        if (Utils.isSQLTypeTime(type)) return Constants.TIME;
        if (Utils.isSQLTypeTimestamp(type)) return Constants.TIMESTAMP;
        throw new IllegalArgumentException("Did not recognize function parameter type: <" + type + ">");
    }

    public static TableFunction convertFunctionToTableFunction(Function fn) {
        net.sf.jsqlparser.expression.Function jsqlFn = new net.sf.jsqlparser.expression.Function();
        jsqlFn.setName(fn.getName());

        ExpressionList expList = new ExpressionList();
        List<Expression> expListInternal = new ArrayList<>();
        for (int i = 1; i < fn.getInputs().size() + 1; i++) {
            FunctionParameter param = fn.getInputs().get(i);
            if (param == null) continue;
            expListInternal.add(new LiteralExpression(Utils.convertSQLTypetoConstant(param.getType())));
        }
        expList.setExpressions(expListInternal);
        jsqlFn.setParameters(expList);

        TableFunction tFn = new TableFunction();
        tFn.setFunction(jsqlFn);
        return tFn;
    }

    public static String tableFunctionToString(TableFunction tableFunction) {
        StringBuilder sb = new StringBuilder();
        net.sf.jsqlparser.expression.Function fn = tableFunction.getFunction();

        // Strip all prefixes from function name
        String[] fnNameArr = fn.getName().split("\\.");
        sb.append(fnNameArr[fnNameArr.length - 1]);
        sb.append('(');
        StringJoiner sj = new StringJoiner(",");
        for (Expression expr : fn.getParameters().getExpressions()) {
            sj.add(ConstantRemovalExprDeParser.removeConstantsFromExpr(expr));
        }
        sb.append(sj.toString());
        sb.append(')');

        return sb.toString().toLowerCase();
    }

    public static String fromItemToString(FromItem fromItem) {
        if (fromItem instanceof TableFunction) {
            return Utils.tableFunctionToString((TableFunction) fromItem);
        }
        return fromItem.toString().toLowerCase();
    }

    public static boolean isStrangeJoin(Join join) {
        return join.isLeft() || join.isRight() || join.isCross() || join.isSemi();
    }

    // Calculates information entropy
    // https://en.wikipedia.org/wiki/Entropy_(information_theory)
    public static double entropy(List<Double> eventProbs) {
        double entropy = 0;
        for (Double prob : eventProbs) {
            entropy -= prob * Math.log(prob) / Math.log(2);
        }
        return entropy;
    }

    // From: https://stackoverflow.com/a/14818944/1165779
    public static<T> Set<Set<T>> powerSet(List<T> list) {
        Set<Set<T>> powerSet = new HashSet<Set<T>>();

        int n = list.size();

        for( long i = 0; i < (1 << n); i++) {
            Set<T> element = new HashSet<T>();
            for( int j = 0; j < n; j++ )
                if( (i >> j) % 2 == 1 ) element.add(list.get(j));
            powerSet.add(element);
        }

        return powerSet;
    }

    public static Table findTableForColumn(Map<String, Table> queryTables, Map<String, Relation> relations,
                                           Map<String, String> oldAliasToTableName, Column col) {
        String tableName = col.getTable().getName();

        if (tableName == null || relations.get(tableName) == null) {
            // if old to table name exists, use it
            if (oldAliasToTableName != null) {
                String newTableName = oldAliasToTableName.get(tableName);
                if (newTableName != null) {
                    return queryTables.get(newTableName);
                }
            }

            // Find table name
            for (Map.Entry<String, Table> e : queryTables.entrySet()) {
                // Find the relation
                Relation r = relations.get(e.getKey());
                if (r == null) {
                    throw new RuntimeException("Table name in query: <" + e.getKey() + "> not found in schema.");
                }

                for (Map.Entry<String, Attribute> attrEntry : r.getAttributes().entrySet()) {
                    if (attrEntry.getKey().equals(col.getColumnName())) {
                        return queryTables.get(r.getName());
                    }
                }
            }
        } else {
            tableName = col.getTable().getName();
        }

        return queryTables.get(tableName);
    }

    public static Join addJoin(Select select, Attribute existingRelationAttr, Attribute joinedRelationAttr) {
        PlainSelect ps = (PlainSelect) select.getSelectBody();

        List<Join> joins = ps.getJoins();
        if (joins == null) {
            joins = new ArrayList<>();
            ps.setJoins(joins);
        }
        Join join = new Join();
        join.setRightItem(joinedRelationAttr.getRelation().getFromItem());

        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(existingRelationAttr.getColumn());
        equalsTo.setRightExpression(joinedRelationAttr.getColumn());
        join.setOnExpression(equalsTo);

        joins.add(join);

        return join;
    }
}
