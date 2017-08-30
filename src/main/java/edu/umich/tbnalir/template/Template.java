package edu.umich.tbnalir.template;

import com.esotericsoftware.minlog.Log;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;

/**
 * Created by cjbaik on 8/28/17.
 */
public class Template {
    String templateString;
    TemplateType type;

    public Template(String templateString, TemplateType type) {
        this.templateString = templateString;
        this.type = type;
    }

    public TemplateType getType() {
        return type;
    }

    public boolean covers(String string) {
        try {
            Statement stmt = CCJSqlParserUtil.parse(string);
            if (stmt instanceof Select) {
                return this.covers((Select) stmt);
            }
            return false;
        } catch (JSQLParserException e) {
            if (Log.DEBUG) e.printStackTrace();
            return false;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public boolean covers(Select select) {
        switch (this.type) {
            case NO_CONST:
                return this.equals(TemplateRoot.noConstantTemplate(select));
            case NO_CONST_PROJ:
                return this.equals(TemplateRoot.noConstantProjectionTemplate(select));
            case NO_CONST_OP:
                return this.equals(TemplateRoot.noComparisonTemplate(select));
            case NO_CONST_OP_PROJ:
                return this.equals(TemplateRoot.noComparisonProjectionTemplate(select));
            case NO_PRED:
                return this.equals(TemplateRoot.noPredicateTemplate(select));
            case NO_PRED_PROJ:
                return this.equals(TemplateRoot.noPredicateProjectionTemplate(select));
            case FULL_QUERY:
                return this.equals(TemplateRoot.fullQueryTemplate(select));
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Template template = (Template) o;

        return !(templateString != null ? !templateString.equals(template.templateString) : template.templateString != null);

    }

    @Override
    public int hashCode() {
        return templateString != null ? templateString.hashCode() : 0;
    }

    @Override
    public String toString() {
        return this.templateString;
    }
}
