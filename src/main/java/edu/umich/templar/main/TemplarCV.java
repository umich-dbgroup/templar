package edu.umich.templar.main;

import com.esotericsoftware.minlog.Log;
import edu.umich.templar.config.TemplarConfig;
import edu.umich.templar.db.Database;
import edu.umich.templar.log.LogLevel;
import edu.umich.templar.task.QueryTask;
import edu.umich.templar.task.QueryTaskReader;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.*;

/**
 * Created by cjbaik on 10/23/17.
 */
public class TemplarCV {
    // Trial 1: seed 1234
    public static int RANDOM_SEED = 1234;

    public static int NUM_FOLDS = 4;

    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Usage: TemplarCV <testset> <set/frag> <full/no_const/no_const_op> <log %>");
            System.out.println("Example: TemplarCV mas 6 yelp,imdb 2 10");
            System.exit(1);
        }
        String dbName = args[0];
        String prefix = "data/" + dbName + "/" + dbName;
        String fragsFile = prefix + "_all_fragments.csv";
        String sqlFile = prefix + "_all.ans";
        String fkpkFile = prefix + ".edges.json";
        String mainAttrsFile = prefix + ".main_attrs.json";

        if (args[2].equalsIgnoreCase("set")) {

        } else if (args[2].equalsIgnoreCase("frag")) {

        } else {
            throw new RuntimeException("Unknown log evaluation type. Use 'set' or 'frag'.");
        }

        LogLevel logLevel = null;
        if (args[3].equalsIgnoreCase("full")) {
            logLevel = LogLevel.FULL;
        } else if (args[3].equalsIgnoreCase("no_const")) {
            logLevel = LogLevel.NO_CONST;
        } else if (args[3].equalsIgnoreCase("no_const_op")) {
            logLevel = LogLevel.NO_CONST_OP;
        } else {
            throw new RuntimeException("Unknown LogLevel type.");
        }

        // Log percent (10 means use 10% of training set, 25 => 25%, etc.)
        Double logPercent = Double.valueOf(args[4]) / 100;

        // Read config for database info
        Database db = new Database(TemplarConfig.getProperty("dbhost"),
                TemplarConfig.getIntegerProperty("dbport"),
                TemplarConfig.getProperty("dbuser"),
                TemplarConfig.getProperty("dbpassword"),
                dbName, fkpkFile, mainAttrsFile);

        List<String> sqls = new ArrayList<>();
        try {
            List<String> answerFileLines = FileUtils.readLines(new File(sqlFile), "UTF-8");
            for (String line : answerFileLines) {
                sqls.add(line.trim().split("\t")[0].trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<Integer, QueryTask> queryTasks = QueryTaskReader.readQueryTasks(fragsFile);



        List<Integer> shuffleIndexes = new ArrayList<>();
        for (Map.Entry<Integer, QueryTask> e : queryTasks.entrySet()) {
            shuffleIndexes.add(e.getKey());
        }
        Collections.shuffle(shuffleIndexes, new Random(RANDOM_SEED));

        // Initialize based on number of folds
        List<List<QueryTask>> queryTaskFolds = new ArrayList<>();
        List<List<String>> sqlFolds = new ArrayList<>();
        List<List<Integer>> shuffleIndexFolds = new ArrayList<>();
        for (int i = 0; i < NUM_FOLDS; i++) {
            queryTaskFolds.add(new ArrayList<>());
            sqlFolds.add(new ArrayList<>());
            shuffleIndexFolds.add(new ArrayList<>());
        }

        // pass in task/sql pairs to each fold
        for (int i = 0; i < shuffleIndexes.size(); i++) {
            int foldIndex = i % NUM_FOLDS;
            queryTaskFolds.get(foldIndex).add(queryTasks.get(shuffleIndexes.get(i)));
            sqlFolds.get(foldIndex).add(sqls.get(shuffleIndexes.get(i)));
            shuffleIndexFolds.get(foldIndex).add(shuffleIndexes.get(i));
        }

        // MAIN LOOP
        for (int i = 0; i < NUM_FOLDS; i++) {
            Log.info("===== FOLD " + i + " =====");
            List<QueryTask> curFoldTasks = queryTaskFolds.get(i);
            List<String> curFoldSQLs = sqlFolds.get(i);
            List<Integer> curFoldShuffleIndexes = shuffleIndexFolds.get(i);
        }
    }
}
