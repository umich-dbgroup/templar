package edu.umich.tbnalir.parse;

import com.esotericsoftware.minlog.Log;
import com.opencsv.CSVReader;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

/**
 * Created by cjbaik on 7/5/17.
 */
public class LogParserProcess {
    public static void main(String[] args) {
        String filename = args[0];
        String outBasename = FilenameUtils.getBaseName(filename);

        // Statistics
        int totalSQL = 0;
        int lastUpdate = 0;

        // Tokens to replace from JSqlParser TokenMgrError, so the whole process doesn't crash
        char[] tokensToReplace = {'#', '\u0018', '\u00a0', '\u2018', '\u201d', '\u00ac'};

        try {
            // Get next line to read, if exists
            File linePtrFile = new File(outBasename + ".nextline");

            int nextLineToRead = 0;
            if (linePtrFile.exists()) {
                nextLineToRead = Integer.valueOf(FileUtils.readFileToString(linePtrFile, "UTF-8"));
            }
            totalSQL += nextLineToRead;
            lastUpdate += nextLineToRead;

            File outFile = new File(outBasename + ".parsed");
            PrintWriter sqlWriter = new PrintWriter(outFile, "UTF-8");
            PrintWriter linePtrWriter = new PrintWriter(linePtrFile, "UTF-8");

            CSVReader csvr = new CSVReader(new FileReader(filename));
            String[] nextLine;

            int lineNumber = 0;
            while ((nextLine = csvr.readNext()) != null) {
                if (lineNumber < nextLineToRead) {
                    lineNumber++;
                    continue;
                }

                if (nextLine.length < 4) continue;

                String sql = nextLine[3];

                totalSQL++;
                if (totalSQL >= (lastUpdate + 20000)) {
                    Log.info("Parsed " + totalSQL + " statements...");
                    lastUpdate = totalSQL;
                }

                for (char token : tokensToReplace) {
                    sql = sql.replace(token, '_');
                }

                Log.debug("ORIGINAL: " + sql.replace("\n", " "));
                Statement stmt = null;
                try {
                    stmt = CCJSqlParserUtil.parse(sql);
                } catch (JSQLParserException e) {
                    if (Log.DEBUG) e.printStackTrace();
                    continue;
                } catch (Throwable t) {
                    // Write line number of next line to lead into file
                    linePtrWriter.println(lineNumber + 1);
                    linePtrWriter.close();

                    sqlWriter.close();

                    t.printStackTrace();
                    System.exit(1);
                }
                if (stmt == null) continue; // Case that it's not a select statement

                sqlWriter.println(stmt.toString());

                lineNumber++;
            }
            csvr.close();
            sqlWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
