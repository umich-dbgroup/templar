package edu.umich.tbnalir.sql;

import edu.umich.tbnalir.rdbms.Relation;
import edu.umich.tbnalir.util.Constants;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.OracleHint;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.deparser.LimitDeparser;

import java.util.*;

/**
 * Created by cjbaik on 6/20/17.
 */
public class ProjectionRemovalDeParser extends SelectConstantRemovalDeParser {
    public ProjectionRemovalDeParser(ExpressionVisitor expressionVisitor, StringBuilder buffer,
                                     Map<String, Relation> relations, boolean removeWhere) {
        super(expressionVisitor, buffer, relations, removeWhere);
    }

    @Override
    public void alphabetizeAndDeparseProjection(PlainSelect plainSelect) {
        // Hide all select items and replace with blanket "projection"
        this.getBuffer().append(Constants.PROJ);
    }
}
