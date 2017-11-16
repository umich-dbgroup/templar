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
        List<String> queries = null;
        try {
            queries = FileUtils.readLines(new File(queryLogFilename), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        double halfLog = Math.floor(0.5 * queries.size());

        Log.info("Parsing training queries...");
        List<String> trainQueries = queries.subList(0, (int) halfLog);
        List<Select> trainStmts = Utils.parseStatements(trainQueries);
        Set<String> trainStrs = trainStmts.stream().map(Select::toString).collect(Collectors.toSet());
        Log.info("Done parsing training queries.");
        Log.info("# of templates: " + trainStrs.size());

        Log.info("Parsing test queries...");
        List<String> testQueries = queries.subList((int) halfLog, queries.size() - 1);
        List<Select> testStmts = Utils.parseStatements(testQueries);
        List<String> testStrs = testStmts.stream().map(Select::toString).collect(Collectors.toList());
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
