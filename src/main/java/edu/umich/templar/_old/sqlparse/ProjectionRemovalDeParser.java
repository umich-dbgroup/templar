package edu.umich.templar._old.sqlparse;

import edu.umich.templar._old.rdbms.Relation;
import edu.umich.templar._old.Constants;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.statement.select.*;

import java.util.*;

/**
 * Created by cjbaik on 6/20/17.
 */
public class ProjectionRemovalDeParser extends FullQueryDeParser {
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
