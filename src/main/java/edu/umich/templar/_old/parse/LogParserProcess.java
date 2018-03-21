package edu.umich.templar._old.parse;

import com.esotericsoftware.minlog.Log;
import com.opencsv.CSVReader;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.*;

/**
 * Created by cjbaik on 7/5/17.
 */
public class LogParserProcess {
    // State
    public static int nextLineToRead = 0;
    public static int lineNumber = 0;

    // Tokens to replace from JSqlParser TokenMgrError, so the whole process doesn't crash
    public static char[] tokensToReplace = {'#', '\u0018', '\u00a0', '\u2018', '\u201d', '\u00ac', '\ufffd'};

    // Statistics
    public static int totalSQL = 0;
    public static int lastUpdate = 0;

    public static void parseSQL(String sql, PrintWriter linePtrWriter, PrintWriter sqlWriter) {
        totalSQL++;
        if (totalSQL >= (lastUpdate + 20000)) {
            Log.info("Parsed " + totalSQL + " statements...");
            lastUpdate = totalSQL;
        }

        for (char token : tokensToReplace) {
            sql = sql.replace(token, '_');
        }

        // Replace all unicode
//        sql = sql.replaceAll("\\p{C}", "_");
//        sql = sql.replaceAll("\\p{Z}", "_");
//        sql = sql.replaceAll("\\p{M}", "_");
//        sql = sql.replaceAll("\\p{Po}", "_");

        Statement stmt = null;
        try {
            stmt = CCJSqlParserUtil.parse(sql);
        } catch (JSQLParserException e) {
            if (Log.DEBUG) {
                Log.debug("ORIGINAL: " + sql.replace("\n", " "));
                e.printStackTrace();
            }
            return;
        } catch (Throwable t) {
            // Write line number of next line to lead into file
            linePtrWriter.println(lineNumber + 1);
            linePtrWriter.close();

            sqlWriter.close();

            t.printStackTrace();
            System.exit(1);
        }
        if (stmt == null) return; // Case that it's not a select statement

        sqlWriter.println(stmt.toString());

        lineNumber++;
    }

    public static void parsePlain(String filename, PrintWriter linePtrWriter, PrintWriter sqlWriter) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String nextLine;

            while ((nextLine = reader.readLine()) != null) {
                if (lineNumber < nextLineToRead) {
                    lineNumber++;
                    continue;
                }

                LogParserProcess.parseSQL(nextLine, linePtrWriter, sqlWriter);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void parseCSV(String filename, PrintWriter linePtrWriter, PrintWriter sqlWriter) {
        try {
            CSVReader csvr = new CSVReader(new FileReader(filename));
            String[] nextLine;

            while ((nextLine = csvr.readNext()) != null) {
                if (lineNumber < nextLineToRead) {
                    lineNumber++;
                    continue;
                }

                if (nextLine.length < 4) continue;

                LogParserProcess.parseCSV(nextLine[3], linePtrWriter, sqlWriter);
            }
            csvr.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String filename = args[0];
        String outBasename = FilenameUtils.getBaseName(filename);
        Boolean parseCSV = Boolean.valueOf(args[1]);

        try {
            // Get next line to read, if exists
            File linePtrFile = new File(outBasename + ".nextline");

            if (linePtrFile.exists()) {
                nextLineToRead = Integer.valueOf(FileUtils.readFileToString(linePtrFile, "UTF-8").trim());
            }
            totalSQL += nextLineToRead;
            lastUpdate += nextLineToRead;

            File outFile = new File(outBasename + ".parsed");
            PrintWriter sqlWriter = new PrintWriter(new FileOutputStream(outFile, true));
            PrintWriter linePtrWriter = new PrintWriter(linePtrFile, "UTF-8");

            if (parseCSV) {
                LogParserProcess.parseCSV(filename, linePtrWriter, sqlWriter);
            } else {
                LogParserProcess.parsePlain(filename, linePtrWriter, sqlWriter);
            }

            sqlWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
