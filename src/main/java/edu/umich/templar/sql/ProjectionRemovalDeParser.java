package edu.umich.templar.sql;

import edu.umich.templar.rdbms.Relation;
import edu.umich.templar.util.Constants;
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
