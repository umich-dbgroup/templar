package edu.umich.templar._old.template;

import com.esotericsoftware.minlog.Log;
import edu.umich.templar._old.rdbms.JoinPath;
import edu.umich.templar._old.qf.Projection;
import edu.umich.templar._old.rdbms.Relation;
import edu.umich.templar._old.qf.Predicate;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;

import java.util.*;

/**
 * Created by cjbaik on 8/28/17.
 */
public class Template {
    String templateString;
    TemplateType type;

    List<Relation> relations;  // relations in this template
    List<Projection> projections; // projection columns in this template
    List<Predicate> predicates;  // predicates in this template

    JoinPath joinPath;

    public Template(String templateString, TemplateType type) {
        this.templateString = templateString;
        this.type = type;
        this.relations = new ArrayList<>();
        this.projections = new ArrayList<>();
        this.predicates = new ArrayList<>();
        this.joinPath = new JoinPath();
    }

    public TemplateType getType() {
        return type;
    }

    public List<Relation> getRelations() {
        return relations;
    }

    public void setRelations(List<Relation> relations) {
        this.relations = relations;
    }

    public List<Projection> getProjections() {
        return projections;
    }

    public void setProjections(List<Projection> projections) {
        this.projections = projections;
    }

    public List<Predicate> getPredicates() {
        return predicates;
    }

    public void setPredicates(List<Predicate> predicates) {
        this.predicates = predicates;
    }

    public JoinPath getJoinPath() {
        return joinPath;
    }

    public void setJoinPath(JoinPath joinPath) {
        this.joinPath = joinPath;
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

    public String toStringDebug() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.templateString);
        sb.append("\n\trels: [");

        StringJoiner relSj = new StringJoiner(",");
        for (Relation rel : this.relations) {
            if (rel == null) {
                relSj.add("null");
            } else {
                relSj.add(rel.getName());
            }
        }
        sb.append(relSj.toString());
        sb.append("]; projs: [");

        StringJoiner projSj = new StringJoiner(",");
        for (Projection proj : this.projections) {
            if (proj == null) {
                projSj.add("null");
            } else {
                projSj.add(proj.toString());
            }
        }
        sb.append(projSj.toString());
        sb.append("]; preds: [");

        StringJoiner predSj = new StringJoiner(",");
        this.predicates.stream().map(Predicate::toString).forEach(predSj::add);
        sb.append(predSj.toString());
        sb.append("]");

        return sb.toString();
    }
}
