package edu.umich.templar.sqlparse;

import edu.umich.templar.rdbms.Relation;
import edu.umich.templar.util.Constants;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;

import java.util.*;

/**
 * Created by cjbaik on 9/5/17.
 */
public class AttributeRemovalDeParser extends FullQueryDeParser {
    public AttributeRemovalDeParser(ExpressionVisitor expressionVisitor, StringBuilder buffer,
                                     Map<String, Relation> relations, boolean removeWhere) {
        super(expressionVisitor, buffer, relations, removeWhere);
    }


    @Override
    public void alphabetizeAndDeparseProjection(PlainSelect plainSelect) {
        List<String> projections = new ArrayList<>();

        if (plainSelect.getSelectItems() != null) {
            for (Iterator<SelectItem> iter = plainSelect.getSelectItems().iterator(); iter.hasNext(); ) {
                StringBuilder buffer = new StringBuilder();

                SelectItem selectItem = iter.next();
                if (selectItem instanceof SelectExpressionItem) {
                    SelectExpressionItem exprItem = (SelectExpressionItem) selectItem;
                    Expression expr = exprItem.getExpression();
                    if (expr instanceof Column || expr instanceof LiteralExpression) {
                        projections.add(Constants.COLUMN);
                    }
                } else {
                    FullQueryExprDeParser exprDeParser = new FullQueryExprDeParser();
                    FullQueryDeParser subParser = this.subParser(exprDeParser, buffer);
                    exprDeParser.setBuffer(buffer);
                    exprDeParser.setSelectVisitor(subParser);

                    selectItem.accept(subParser);
                    projections.add(buffer.toString());
                }
            }

            Collections.sort(projections);
            this.getBuffer().append(String.join(", ", projections));
        }
    }
}
