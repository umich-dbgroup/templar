package edu.umich.tbnalir;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.io.FilenameUtils;

/**
 * Created by cjbaik on 7/5/17.
 */
public class LogParser {
    public static void main(String[] args) {
        String filename = args[0];

        // Start LogParserProcess, whenever it dies, restart another one.
        String line = "java -cp build/libs/tb-nalir-all.jar edu.umich.tbnalir.parse.LogParserProcess "
                + filename;
        CommandLine cmdLine = CommandLine.parse(line);
        DefaultExecutor executor = new DefaultExecutor();
        try {
            int exitValue;
            do {
                exitValue = executor.execute(cmdLine);
            } while (exitValue != 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
