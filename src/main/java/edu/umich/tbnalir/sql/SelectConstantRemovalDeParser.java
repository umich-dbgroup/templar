package edu.umich.tbnalir.sql;

import edu.umich.tbnalir.rdbms.Relation;
import edu.umich.tbnalir.util.Constants;
import edu.umich.tbnalir.util.Utils;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.deparser.LimitDeparser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;

import java.util.*;

/**
 * Created by cjbaik on 6/20/17.
 */
public class SelectConstantRemovalDeParser extends FullQueryDeParser {
    boolean removeWhere;

    public SelectConstantRemovalDeParser(ExpressionVisitor expressionVisitor, StringBuilder buffer,
                                         Map<String, Relation> relations, boolean removeWhere) {
        super(expressionVisitor, buffer, relations);

        this.removeWhere = removeWhere;
    }

    @Override
    public void deparsePredicate(PlainSelect plainSelect) {
        if (plainSelect.getWhere() != null) {
            this.getBuffer().append(" WHERE ");

            if (this.removeWhere) {
                this.getBuffer().append(Constants.PRED);
            } else {
                plainSelect.getWhere().accept(this.getExpressionVisitor());
            }
        }
    }
}
