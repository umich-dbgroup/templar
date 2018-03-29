package edu.umich.templar.main;

import com.esotericsoftware.minlog.Log;
import edu.umich.templar.config.TemplarConfig;
import edu.umich.templar.db.Database;
import edu.umich.templar.log.LogCountGraph;
import edu.umich.templar.log.LogLevel;
import edu.umich.templar.log.graph.LogGraph;
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
            System.out.println("Usage: TemplarCV <testset> <set/frag> <full/no_const/no_const_op> <log %> <type-oracle>");
            System.exit(1);
        }
        String dbName = args[0];
        String prefix = "data/" + dbName + "/" + dbName;
        String fragsFile = prefix + "_all_fragments.csv";
        String sqlFile = prefix + "_all.sqls";
        String fkpkFile = prefix + ".fkpk.json";
        String mainAttrsFile = prefix + ".main_attrs.json";
        String projAttrsFile = prefix + ".proj_attrs.json";
        String candCacheFilename = prefix + ".cands.cache";

        boolean runLogGraph;
        if (args[1].equalsIgnoreCase("set")) {
            runLogGraph = false;
        } else if (args[1].equalsIgnoreCase("frag")) {
            runLogGraph = true;
        } else {
            throw new RuntimeException("Unknown log evaluation type. Use 'set' or 'frag'.");
        }

        LogLevel logLevel = null;
        if (args[2].equalsIgnoreCase("full")) {
            logLevel = LogLevel.FULL;
        } else if (args[2].equalsIgnoreCase("no_const")) {
            logLevel = LogLevel.NO_CONST;
        } else if (args[2].equalsIgnoreCase("no_const_op")) {
            logLevel = LogLevel.NO_CONST_OP;
        } else {
            throw new RuntimeException("Unknown LogLevel type.");
        }

        // Log percent (10 means use 10% of training set, 25 => 25%, etc.)
        Double logPercent = Double.valueOf(args[3]) / 100;

        Boolean typeOracle = Boolean.valueOf(args[4]);
        Boolean includeSteiner = Boolean.valueOf(args[5]);

        // Read config for database info
        Database db = new Database(TemplarConfig.getProperty("dbhost"),
                TemplarConfig.getIntegerProperty("dbport"),
                TemplarConfig.getProperty("dbuser"),
                TemplarConfig.getProperty("dbpassword"),
                dbName, fkpkFile, mainAttrsFile, projAttrsFile);

        List<String> sqls = new ArrayList<>();
        try {
            List<String> answerFileLines = FileUtils.readLines(new File(sqlFile), "UTF-8");
            for (String line : answerFileLines) {
                sqls.add(line.trim().split("\t")[0].trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<QueryTask> queryTasks = QueryTaskReader.readQueryTasks(fragsFile);

        List<Integer> shuffleIndexes = new ArrayList<>();
        for (int i = 0; i < queryTasks.size(); i++) {
            shuffleIndexes.add(i);
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
            // List<Integer> curFoldShuffleIndexes = shuffleIndexFolds.get(i);

            if (runLogGraph) {
                LogCountGraph logCountGraph = new LogCountGraph(db, logLevel);

                // Analyze everything in SQLs excluding current
                List<String> sqlLog = new ArrayList<>(sqls);
                sqlLog.removeAll(curFoldSQLs);

                System.out.println("Original log size: " + sqlLog.size());
                System.out.println("Trimming log to " + (logPercent * 100) + "%...");
                // Collections.shuffle(sqlLog, new Random(RANDOM_SEED));
                int logSize = ((Double) (logPercent * sqlLog.size())).intValue();
                sqlLog = sqlLog.subList(0, logSize);
                System.out.println("Final log size: " + sqlLog.size());

                logCountGraph.analyzeSQLs(sqlLog);

                LogGraph logGraph = new LogGraph(db, logCountGraph);

                Templar templar = new Templar(db, candCacheFilename, curFoldTasks,
                        typeOracle, logGraph, includeSteiner);

                templar.execute();
            } else {
                // TODO: LogFullTemplates
                continue;
            }
        }
    }
}
