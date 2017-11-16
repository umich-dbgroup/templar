package edu.umich.templar;

import com.esotericsoftware.minlog.Log;
import edu.umich.templar.rdbms.SchemaGraph;
import edu.umich.templar.template.TemplateRoot;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by cjbaik on 11/16/17.
 */
public class CoverageSDSSFull {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: CoverageSDSSFull <query-log-filename>");
            System.err.println("Example: CoverageSDSSFull data/sdss/final/bestdr7_0.05.parsed");
            System.exit(1);
        }

        String queryLogFilename = args[0];

        SchemaGraph schemaGraph = null;
        try {
            schemaGraph = new SchemaGraph("data/sdss/schema/bestdr7");
            TemplateRoot.relations = schemaGraph.relations;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fix test query log as last 50% no matter what
        List<String> queryStr = null;
        try {
            Log.info("Reading queries into memory...");
            queryStr = FileUtils.readLines(new File(queryLogFilename), "UTF-8");
            Log.info("Done reading queries.");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        List<Select> queries = Utils.parseStatements(queryStr);

        double halfLog = Math.floor(0.5 * queries.size());

        Log.info("Parsing training queries...");
        List<Select> trainQueries = queries.subList(0, (int) halfLog);
        Set<String> trainStrs = trainQueries.stream().map(Select::toString).collect(Collectors.toSet());
        Log.info("Done parsing training queries.");
        Log.info("# of templates: " + trainStrs.size());

        Log.info("Parsing test queries...");
        List<Select> testQueries = queries.subList((int) halfLog, queries.size() - 1);
        List<String> testStrs = testQueries.stream().map(Select::toString).collect(Collectors.toList());
        Log.info("Done parsing test queries.");

        int coverage = 0;
        for (String test : testStrs) {
            if (trainStrs.contains(test)) {
                coverage++;
            }
        }
        double percent = (double) coverage / testStrs.size() * 100;
        Log.info("Test coverage: " + coverage + "/" + testStrs.size() + "(" + String.format("%.1f", percent) + "%)");
    }
}
