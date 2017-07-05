package edu.umich.tbnalir;

import com.esotericsoftware.minlog.Log;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.io.FilenameUtils;

import java.io.File;

/**
 * Created by cjbaik on 7/5/17.
 */
public class LogParser {
    public static void main(String[] args) {
        String filename = args[0];

        // Clean up any leftover files from previous execution
        String outBasename = FilenameUtils.getBaseName(filename);
        File linePtrFile = new File(outBasename + ".nextline");
        if (linePtrFile.exists()) linePtrFile.delete();

        File outFile = new File(outBasename + ".parsed");
        if (outFile.exists()) outFile.delete();

        // Start LogParserProcess, whenever it dies, restart another one.
        String line = "java -cp build/libs/tb-nalir-all.jar edu.umich.tbnalir.parse.LogParserProcess "
                + filename;
        CommandLine cmdLine = CommandLine.parse(line);
        DefaultExecutor executor = new DefaultExecutor();
        int exitValue = 1;
        do {
            try {
                Log.info("Starting process: <" + cmdLine + ">");
                exitValue = executor.execute(cmdLine);
            } catch (ExecuteException e) {
                Log.info("Process terminated.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } while (exitValue != 0);
    }
}
