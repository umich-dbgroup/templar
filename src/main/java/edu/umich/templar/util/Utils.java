package edu.umich.templar.util;

import com.esotericsoftware.minlog.Log;
import edu.umich.templar._old.Constants;
import edu.umich.templar._old.qf.pieces.Operator;
import edu.umich.templar._old.rdbms.Attribute;
import edu.umich.templar._old.rdbms.Function;
import edu.umich.templar._old.rdbms.FunctionParameter;
import edu.umich.templar._old.rdbms.Relation;
import edu.umich.templar._old.sqlparse.ConstantRemovalExprDeParser;
import edu.umich.templar._old.sqlparse.LiteralExpression;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by cjbaik on 6/20/17.
 */
public class Utils {
    public static List<String> stopwords;

    static {
        stopwords = new ArrayList<>();
        try {
            List<String> stopwordsList = FileUtils.readLines(new File("libs/stopwords.txt"), "UTF-8");
            for (String word : stopwordsList) {
                stopwords.add(word.trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
    public static<T> Set<Set<T>> powerSet(List<T> list, int maxSize) {
        Set<Set<T>> powerSet = new HashSet<Set<T>>();

        int n = list.size();

        for( long i = 0; i < (1 << n); i++) {
            Set<T> element = new HashSet<T>();
            for( int j = 0; j < n; j++ )
                if( (i >> j) % 2 == 1 ) element.add(list.get(j));
            if (element.size() <= maxSize) powerSet.add(element);
        }

        return powerSet;
    }

    public static Table findTableForColumn(Map<String, Table> queryTables, Map<String, List<String>> aliases,
                                           Map<String, Relation> relations, Map<Table, Table> oldToNewTables,
                                           Map<String, Table> oldAliasToTable, Column col) {
        String tableName = col.getTable().getName();
        Alias tableAlias = col.getTable().getAlias();

        // if there is reference to new table, return it
        if (oldToNewTables != null && oldToNewTables.get(col.getTable()) != null) {
            return oldToNewTables.get(col.getTable());
        }

        // if table is already set correctly and alias exists, use it
        if (tableAlias != null) {
            List<String> aliasList = aliases.get(tableName);
            if (aliasList != null && aliasList.contains(tableAlias.getName())) {
                return col.getTable();
            }
        }

        // if _old to table name exists, use it
        Table table = oldAliasToTable.get(tableName);
        if (table != null) return table;

        if (col.getTable() != null && tableAlias != null) {
            Table tableByAlias = oldAliasToTable.get(col.getTable().getAlias().getName());
            if (tableByAlias != null) return tableByAlias;
        }

        // if table exists in list of query tables, return it
        Table tableExists = queryTables.get(tableName);
        if (tableExists != null) return tableExists;

        // if it does not match either, find table name from column name
        for (Map.Entry<String, Table> e : queryTables.entrySet()) {
            // Find the relation
            Relation r = relations.get(e.getKey());
            if (r != null) {
                for (Map.Entry<String, Attribute> attrEntry : r.getAttributes().entrySet()) {
                    if (attrEntry.getKey().equals(col.getColumnName())) {
                        return queryTables.get(r.getName());
                    }
                }
            }
        }

        return null;
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

    public static Runnable getParserRunnable(ConcurrentLinkedQueue<Select> stmts, String sql) {
        // Tokens to replace from JSqlParser TokenMgrError, so the whole process doesn't crash
        char[] tokensToReplace = {'#', '\u0018', '\u00a0', '\u2018', '\u201d', '\u00ac'};
        for (char token : tokensToReplace) {
            sql = sql.replace(token, '_');
        }
        final String finalSql = sql;

        return () -> {
            Statement stmt;
            try {
                stmt = CCJSqlParserUtil.parse(finalSql);
            } catch (JSQLParserException e) {
                if (Log.DEBUG) e.printStackTrace();
                // System.out.println(finalSql);
                return;
            } catch (Throwable t) {
                t.printStackTrace();
                return;
            }
            if (stmt == null || !(stmt instanceof Select)) {
                return; // Case that it's not a select statement
            }
            stmts.add((Select) stmt);
        };
    }

    public static List<Select> parseStatements(List<String> sqls) {
        try {
            int cores = Runtime.getRuntime().availableProcessors();
            ExecutorService pool = Executors.newFixedThreadPool(cores);
            ConcurrentLinkedQueue<Select> stmts = new ConcurrentLinkedQueue<>();
            for (String sql : sqls) {
                pool.submit(Utils.getParserRunnable(stmts, sql));
            }

            pool.shutdown();

            try {
                if (!pool.awaitTermination(10, TimeUnit.MINUTES)) {
                    pool.shutdownNow();
                    if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                        Log.error("Pool did not terminate");
                    }
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }

            Log.info("===== Parsing Results =====");
            Log.info("Total Queries: " + sqls.size());
            Log.info("Correctly Parsed: " + stmts.size() + "/" + sqls.size() + "\n");

            return new ArrayList<>(stmts);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Select> parseStatements(String queryLogFilename) {
        try {
            List<String> sqls = Files.readAllLines(Paths.get(queryLogFilename));
            return Utils.parseStatements(sqls);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Select> parseStatementsSequential(String queryLogFilename) {
        try {
            List<String> sqls = Files.readAllLines(Paths.get(queryLogFilename));
            return Utils.parseStatementsSequential(sqls);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Select> parseStatementsSequential(List<String> sqlLines) {
        List<Select> stmts = new ArrayList<>();

        for (String sqlLine : sqlLines) {
            // If we have multiple answers per line (residual from previous system, then pick only first)
            String sql = sqlLine.split("\t")[0];

            Statement stmt;
            try {
                stmt = CCJSqlParserUtil.parse(sql);
            } catch (JSQLParserException e) {
                throw new RuntimeException(e);
            }
            if (stmt == null || !(stmt instanceof Select)) {
                throw new RuntimeException("Encountered a non-select statement.");
            }
            stmts.add((Select) stmt);
        }

        Log.info("===== Parsing Results =====");
        Log.info("Total Queries: " + sqlLines.size());
        Log.info("Correctly Parsed: " + stmts.size() + "/" + sqlLines.size() + "\n");

        return stmts;
    }

    public static Integer getAliasIntFromAlias(String alias) {
        String[] splitList = alias.split("_");
        String lastSplit = splitList[splitList.length - 1];

        if (StringUtils.isNumeric(lastSplit)) {
            return Integer.valueOf(lastSplit);
        } else {
            return null;
        }
    }

    public static String removeNonAlphaNumeric(String str) {
        // Replace possible delimiter with a space, then replace the rest with nothing.
        return str.replaceAll("-", " ").replaceAll("[^A-Za-z0-9 ]", "");
    }

    public static String removeAliasIntFromAlias(String alias) {
        Pattern pattern = Pattern.compile("(.*)_[0-9]+");
        Matcher matcher = pattern.matcher(alias);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return alias;
        }
    }

    public static boolean isStopword(String word) {
        return Utils.stopwords.contains(word.trim().toLowerCase());
    }

    public static Column getColumnFromFunction(net.sf.jsqlparser.expression.Function function) {
        if (function.getParameters() != null && function.getParameters().getExpressions() != null &&
                function.getParameters().getExpressions().size() == 1) {
            for (Expression expr : function.getParameters().getExpressions()) {
                if (expr instanceof Parenthesis) {
                    expr = ((Parenthesis) expr).getExpression();
                }
                if (expr instanceof Column) {
                    return (Column) expr;
                }
            }
        }
        return null;
    }

    public static Attribute getAttributeFromColumn(Map<String, Relation> relations, List<Relation> queryRelations, Column column) {
        Relation rel = null;
        Attribute attr = null;
        if (column.getTable().getName() != null) {
            String tableName = Utils.removeAliasIntFromAlias(column.getTable().getName().trim().toLowerCase());
            rel = relations.get(tableName);
            if (rel == null) {
                for (Relation qr : queryRelations) {
                    if (qr.getAliasSet().contains(tableName)) {
                        rel = qr;
                        break;
                    }
                }
            }
            // if (rel == null) throw new RuntimeException("Relation " + column.getTable().getName() + " not found!");
            if (rel == null) return null;

            attr = rel.getAttributes().get(column.getColumnName().toLowerCase());
        } else {
            for (Relation qr : queryRelations) {
                attr = qr.getAttributes().get(column.getColumnName());
                if (attr != null) break;
            }
        }

        // if (attr == null) throw new RuntimeException("Attribute " + column.getColumnName() + " not found!");
        return attr;
    }

    public static Operator getOperatorFromString(String string) {
        switch (string) {
            case ">":
                return Operator.GT;
            case ">=":
                return Operator.GTE;
            case "<":
                return Operator.LT;
            case "<=":
                return Operator.LTE;
            case "!=":
                return Operator.NE;
            case "=":
                return Operator.EQ;
        }
        return null;
    }

    public static double geometricMean(List<Double> numbers) {
        if (numbers.size() == 0) return 0.0;

        double accum = 1.0;
        double root = 0.0;

        for (Double number : numbers) {
            accum *= number;
            root += 1;
        }

        return Math.pow(accum, 1.0 / root);
    }

    public static double mean(List<Double> numbers) {
        if (numbers.size() == 0) return 0.0;

        double accum = 0.0;
        for (Double number : numbers) {
            accum += number;
        }
        return accum / numbers.size();
    }

    public static double harmonicMean(double num1, double num2) {
        return (2 * num1 * num2) / (num1 + num2);
    }

    public static double harmonicMean(List<Double> numbers) {
        double numer = (double) numbers.size();

        double denom = 0.0;
        for (Double number : numbers) {
            denom += 1.0 / number;
        }
        return numer / denom;
    }
}
