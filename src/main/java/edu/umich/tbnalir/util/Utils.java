package edu.umich.tbnalir.util;

import edu.umich.tbnalir.rdbms.Attribute;
import edu.umich.tbnalir.rdbms.Function;
import edu.umich.tbnalir.rdbms.FunctionParameter;
import edu.umich.tbnalir.rdbms.Relation;
import edu.umich.tbnalir.sql.ConstantRemovalExprDeParser;
import edu.umich.tbnalir.sql.LiteralExpression;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.TableFunction;

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

    public static String convertSQLTypetoConstant(String type) {
        switch (type) {
            case "varchar":
            case "varbinary":
            case "binary":
            case "sysname":
                return Constants.STR;
            case "real":
            case "float":
            case "int":
            case "bigint":
            case "smallint":
            case "tinyint":
            case "bit":
                return Constants.NUM;
            case "datetime":
                return Constants.DATETIME;
            case "time":
                return Constants.TIME;
            case "timestamp":
                return Constants.TIMESTAMP;
            case "date":
                return Constants.DATE;
            default:
                throw new IllegalArgumentException("Did not recognize function parameter type: <" + type + ">");
        }
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

}
