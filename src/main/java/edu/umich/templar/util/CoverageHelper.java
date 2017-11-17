package edu.umich.templar.util;

import edu.umich.templar.template.Template;
import net.sf.jsqlparser.statement.select.Select;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.function.Function;

/**
 * Created by cjbaik on 9/8/17.
 */
public class CoverageHelper {
    PrintWriter outWriter;
    PrintWriter errWriter;

    public CoverageHelper(String outFileName, String errFileName) {
        try {
            this.outWriter = new PrintWriter(new FileWriter(outFileName));
            this.errWriter = new PrintWriter(new FileWriter(errFileName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void finish() {
        this.outWriter.close();
        this.errWriter.close();
    }

    public float calculateCoveragePercent(Collection<Template> generatedTemplates, Collection<Select> testStatements,
                                          Function<Select, Template> templateFn, boolean writeResults) {
        float covered = 0;

        for (Select select : testStatements) {
            Template testTemplate = null;
            try {
                testTemplate = templateFn.apply(select);
            } catch (Exception e) {
                // Silent failure
            }

            if (generatedTemplates.contains(testTemplate)) {
                covered++;
            } else {
                if (writeResults) {
                    this.errWriter.println(testTemplate);
                }
            }
        }

        if (writeResults) {
            generatedTemplates.stream().sorted((a, b) -> a.toString().compareTo(b.toString()))
                    .forEach(this.outWriter::println);
        }

        return covered / testStatements.size() * 100;
    }
}
