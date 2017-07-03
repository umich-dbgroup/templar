package edu.umich.tbnalir;

import com.esotericsoftware.minlog.Log;
import edu.umich.tbnalir.sql.*;
import edu.umich.tbnalir.util.Constants;
import edu.umich.tbnalir.util.Utils;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Distinct;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.Top;

import java.io.PrintWriter;
import java.util.*;
import java.util.function.Function;

/**
 * Created by cjbaik on 6/30/17.
 */
public class TemplateGenerator {
    public String noConstantTemplate(Statement stmt) {
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            StringBuilder buffer = new StringBuilder();
            ConstantRemovalExprDeParser constantRemover = new ConstantRemovalExprDeParser();
            SelectConstantRemovalDeParser deParser = new SelectConstantRemovalDeParser(constantRemover, buffer, false);
            constantRemover.setSelectVisitor(deParser);
            constantRemover.setBuffer(buffer);
            select.getSelectBody().accept(deParser);
            return buffer.toString().toLowerCase().intern();
        }
        return null;
    }

    public String noConstantProjectionTemplate(Statement stmt) {
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            StringBuilder buffer = new StringBuilder();
            ConstantRemovalExprDeParser constantRemover = new ConstantRemovalExprDeParser();
            ProjectionRemovalDeParser deParser = new ProjectionRemovalDeParser(constantRemover, buffer, false);
            constantRemover.setSelectVisitor(deParser);
            constantRemover.setBuffer(buffer);
            select.getSelectBody().accept(deParser);
            return buffer.toString().toLowerCase().intern();
        }
        return null;
    }

    public String noComparisonTemplate(Statement stmt) {
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            StringBuilder buffer = new StringBuilder();
            ComparisonRemovalExprDeParser comparisonRemover = new ComparisonRemovalExprDeParser();
            SelectConstantRemovalDeParser deParser = new SelectConstantRemovalDeParser(comparisonRemover, buffer, false);
            comparisonRemover.setSelectVisitor(deParser);
            comparisonRemover.setBuffer(buffer);
            select.getSelectBody().accept(deParser);
            return buffer.toString().toLowerCase().intern();
        }
        return null;
    }

    public String noComparisonProjectionTemplate(Statement stmt) {
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            StringBuilder buffer = new StringBuilder();
            ComparisonRemovalExprDeParser comparisonRemover = new ComparisonRemovalExprDeParser();
            ProjectionRemovalDeParser deParser = new ProjectionRemovalDeParser(comparisonRemover, buffer, false);
            comparisonRemover.setSelectVisitor(deParser);
            comparisonRemover.setBuffer(buffer);
            select.getSelectBody().accept(deParser);
            return buffer.toString().toLowerCase().intern();
        }
        return null;
    }

    public String noPredicateTemplate(Statement stmt) {
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            StringBuilder buffer = new StringBuilder();
            ConstantRemovalExprDeParser predicateRemover = new ConstantRemovalExprDeParser();
            SelectConstantRemovalDeParser deParser = new SelectConstantRemovalDeParser(predicateRemover, buffer, true);
            predicateRemover.setSelectVisitor(deParser);
            predicateRemover.setBuffer(buffer);
            select.getSelectBody().accept(deParser);
            return buffer.toString().toLowerCase().intern();
        }
        return null;
    }

    public String noPredicateProjectionTemplate(Statement stmt) {
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            StringBuilder buffer = new StringBuilder();
            ConstantRemovalExprDeParser predicateRemover = new ConstantRemovalExprDeParser();
            ProjectionRemovalDeParser deParser = new ProjectionRemovalDeParser(predicateRemover, buffer, true);
            predicateRemover.setSelectVisitor(deParser);
            predicateRemover.setBuffer(buffer);
            select.getSelectBody().accept(deParser);
            return buffer.toString().toLowerCase().intern();
        }
        return null;
    }

    public Set<String> generateTemplates(List<Statement> stmts, Function<Statement, String> templateFn,
                                         String outFileName) {
        Set<String> templates = new HashSet<String>();
        Map<String, Integer> templateCounts = new HashMap<String, Integer>();
        for (Statement stmt : stmts) {
            String tmpl = templateFn.apply(stmt);
            if (tmpl != null) {
                templates.add(tmpl);

                if (outFileName != null) {
                    Integer count = templateCounts.get(tmpl);
                    if (count == null) {
                        templateCounts.put(tmpl, 1);
                    } else {
                        templateCounts.put(tmpl, count + 1);
                    }
                }
            }
        }

        if (outFileName != null) {
            Log.info("Saving " + outFileName + "...");
            try {
                PrintWriter writer = new PrintWriter(outFileName, "UTF-8");
                Map<String, Integer> sortedPredProjCounts = Utils.sortByValueDesc(templateCounts);
                for (Map.Entry<String, Integer> e : sortedPredProjCounts.entrySet()) {
                    writer.println(e.getValue() + "\t" + e.getKey());
                }
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return templates;
    }

    public List<String> generateTestTemplates(List<Statement> stmts, Function<Statement, String> templateFn) {
        List<String> templates = new ArrayList<String>();
        for (Statement stmt : stmts) {
            templates.add(templateFn.apply(stmt));
        }
        return templates;
    }

    public int testCoverage(Set<String> templates, List<String> test) {
        int covered = 0;
        for (String t : test) {
            if (templates.contains(t)) {
                covered++;
            }
        }
        return covered;
    }

    public List<String> generateTemplateVariants(Function<Statement, String> templateFn, Select select) {
        List<String> templates = new ArrayList<String>();

        PlainSelect ps = (PlainSelect) select.getSelectBody();

        // Vanilla Template
        String vanilla = templateFn.apply(select);
        templates.add(vanilla);

        // Vanilla + Distinct
        Distinct distinct = new Distinct();
        ps.setDistinct(distinct);
        String distinctTmpl = templateFn.apply(select);
        templates.add(distinctTmpl);
        ps.setDistinct(null);

        // Vanilla + Top
        Top top = new Top();
        top.setExpression(new LiteralExpression(Constants.TOP));
        ps.setTop(top);
        String topTmpl = templateFn.apply(select);
        templates.add(topTmpl);
        ps.setTop(null);

        // Where
        ps.setWhere(new LiteralExpression(Constants.PRED));
        String whereTmpl = templateFn.apply(select);
        templates.add(whereTmpl);

        // Where + Distinct
        ps.setDistinct(distinct);
        String whereDistinctTmpl = templateFn.apply(select);
        templates.add(whereDistinctTmpl);
        ps.setDistinct(null);

        // Where + Top
        ps.setTop(top);
        String whereTopTmpl = templateFn.apply(select);
        templates.add(whereTopTmpl);

        // Reset
        ps.setWhere(null);
        ps.setTop(null);

        return templates;
    }
}
