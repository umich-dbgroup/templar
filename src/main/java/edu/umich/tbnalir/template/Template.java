package edu.umich.tbnalir.template;

import com.esotericsoftware.minlog.Log;
import edu.umich.tbnalir.parse.PossibleTranslation;
import edu.umich.tbnalir.rdbms.Attribute;
import edu.umich.tbnalir.rdbms.Projection;
import edu.umich.tbnalir.rdbms.Relation;
import edu.umich.tbnalir.sql.Operator;
import edu.umich.tbnalir.sql.Predicate;
import edu.umich.tbnalir.util.Constants;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Created by cjbaik on 8/28/17.
 */
public class Template {
    String templateString;
    TemplateType type;

    Set<Relation> relations;  // relations in this template
    List<Projection> projections; // projection columns in this template
    List<Predicate> predicates;  // predicates in this template

    public Template(String templateString, TemplateType type) {
        this.templateString = templateString;
        this.type = type;
        this.relations = new HashSet<>();
        this.projections = new ArrayList<>();
        this.predicates = new ArrayList<>();
    }

    public TemplateType getType() {
        return type;
    }

    public Set<Relation> getRelations() {
        return relations;
    }

    public void setRelations(Set<Relation> relations) {
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

    public Double getSimplicityScore() {
        return 1d / (relations.size() + projections.size() + predicates.size());
    }

    public InstantiatedTemplate instantiate(PossibleTranslation translation) {
        String result = this.templateString;

        // TODO: add in score of similarity of query parse tree structure to template parse tree structure

        // CHECK RELATIONS COVERAGE
        int relationCount = this.relations.size();
        Set<Relation> testRelations = new HashSet<>(this.relations);
        testRelations.addAll(translation.getRelations());
        int finalRelationCount = testRelations.size();

        if (relationCount != finalRelationCount) return null;

        double affinityAccum = 0;

        // CHECK PROJECTIONS COVERAGE
        if (this.templateString.contains(Constants.PROJ)) {
            // (1) in the case of a projection-hidden template
            StringJoiner sj = new StringJoiner(", ");

            for (Projection transProj : translation.getProjections()) {
                // If the relation isn't contained in the template, punt!
                if (!this.templateString.contains(transProj.getAlias())) return null;

                sj.add(transProj.toString());
            }

            affinityAccum += Constants.SLOT_COVERS * translation.getProjections().size();

            result = result.replace(Constants.PROJ, sj.toString());
        } else {
            // (2) in the case of a projection-containing template
            List<Projection> projectionsList = new ArrayList<>(this.projections);
            for (Projection transProj : translation.getProjections()) {
                if (projectionsList.contains(transProj)) {
                    projectionsList.remove(transProj);
                    continue;
                }

                // If the relation isn't contained in the template, punt!
                if (!this.templateString.contains(transProj.getAlias())) return null;

                Integer bestAttrIndex = null;
                boolean bestAttrIsNull = false;
                String bestAttrAlias = null;

                int i = 0;
                for (Projection tmplProj : projectionsList) {
                    if (tmplProj == null) {
                        if (bestAttrIndex == null) {
                            bestAttrIndex = i;
                            bestAttrIsNull = true;
                            // bestAttrAlias = transProj.getAlias();
                        }
                    } else if (tmplProj.covers(transProj)) {
                        bestAttrIndex = i;
                        bestAttrIsNull = false;
                        break;
                    }
                    i++;
                }

                if (bestAttrIndex == null) {
                    affinityAccum += Constants.NO_MATCH;
                } else {
                    projectionsList.remove(bestAttrIndex.intValue());
                    if (bestAttrIsNull) {
                        //transProj.setAlias(bestAttrAlias);
                        result = result.replaceFirst(Constants.COLUMN, transProj.toString());
                        affinityAccum += Constants.SLOT_COVERS;
                    } else {
                        affinityAccum += Constants.EXACT_MATCH;
                    }
                }
            }
        }

        // CHECK PREDICATES COVERAGE
        List<Predicate> predicatesList = new ArrayList<>(this.predicates);
        if (predicatesList.size() < translation.getPredicates().size()) return null;

        if (this.templateString.contains(Constants.PRED)) {
            // (1) in the case of a predicate-hidden template
            StringJoiner sj = new StringJoiner(", ");

            // Special case: no predicates
            if (translation.getPredicates().isEmpty()) {
                result = result.replace(" where " + Constants.PRED, "");
            } else {
                for (Predicate transPred : translation.getPredicates()) {
                    // If the relation isn't contained in the template, punt!
                    if (!this.templateString.contains(transPred.getAlias())) return null;

                    sj.add(transPred.toString());
                }

                affinityAccum += Constants.SLOT_COVERS * translation.getPredicates().size();

                result = result.replace(Constants.PRED, sj.toString());
            }
        } else {
            // (2) in the case of a predicate-visible template
            for (Predicate transPred : translation.getPredicates()) {
                // If the relation isn't contained in the template, punt!
                if (!this.templateString.contains(transPred.getAlias())) return null;

                int mostDetailedMatchCount = 0;
                Predicate mostDetailedMatch = null;

                Attribute transAttr = transPred.getAttr();
                Operator transOp = transPred.getOp();
                String transValue = transPred.getValue();

                for (Predicate tmplPred : predicatesList) {
                    boolean attrNull = tmplPred.getAttr() == null;
                    boolean attrMatches = !attrNull && tmplPred.getAttr().equals(transAttr);
                    boolean attrCovered = attrNull || attrMatches;

                    boolean opNull = tmplPred.getOp() == null;
                    boolean opMatches = !opNull && tmplPred.getOp().equals(transOp);
                    boolean opCovered = opNull || opMatches;

                    boolean valNull = tmplPred.getValue() == null;
                    boolean valMatches = !valNull && tmplPred.getValue().equals(transValue);
                    boolean valNum = tmplPred.getValue().equals(Constants.NUM) && StringUtils.isNumeric(transValue);
                    boolean valText = tmplPred.getValue().equals(Constants.STR) && !StringUtils.isNumeric(transValue);
                    boolean valCovered = valNull || valMatches || valNum || valText;

                    int matchCount = 0;
                    if (attrCovered && opCovered && valCovered) {
                        if (attrMatches) matchCount++;
                        if (opMatches) matchCount++;
                        if (valMatches) matchCount++;

                        if (mostDetailedMatch == null || matchCount > mostDetailedMatchCount) {
                            mostDetailedMatch = tmplPred;
                            mostDetailedMatchCount = matchCount;

                            if (matchCount == 3) break;
                        }
                    }
                }

                if (mostDetailedMatch == null) {
                    affinityAccum += Constants.NO_MATCH;
                } else {
                    predicatesList.remove(mostDetailedMatch);

                    // Fill in predicates
                    StringBuilder toReplace = new StringBuilder();
                    if (mostDetailedMatch.getAttr() == null) {
                        toReplace.append(Constants.COLUMN);
                    } else {
                        toReplace.append(mostDetailedMatch.getAlias());
                        toReplace.append(".");
                        toReplace.append(mostDetailedMatch.getAttr().toString());
                    }
                    toReplace.append(" ");

                    if (mostDetailedMatch.getOp() == null) {
                        toReplace.append(Constants.CMP);
                    } else {
                        toReplace.append(mostDetailedMatch.getOp().toString());
                    }
                    toReplace.append(" ");

                    toReplace.append(mostDetailedMatch.getValue());

                    Predicate resultPred = new Predicate(transPred.getAttr(), transPred.getOp(), transPred.getValue());
                    resultPred.setAlias(transPred.getAlias());

                    /*
                    if (mostDetailedMatch.getAlias() != null) {
                        resultPred.setAlias(mostDetailedMatch.getAlias());
                    }*/

                    result = result.replaceFirst(toReplace.toString(), resultPred.toString());

                    affinityAccum += (double) mostDetailedMatchCount / 3.0;
                }
            }
        }

        // If any slots remain after instantiation, punt!
        if (result.contains("#")) return null;

        // TODO: calculate this later
        double templatePopularity = 1d;

        int complexityScore = this.projections.size() + this.relations.size() + this.predicates.size();

        InstantiatedTemplate instTmpl = new InstantiatedTemplate(result);
        instTmpl.setNlScore(translation.getTotalScore());
        instTmpl.setTemplatePopularity(templatePopularity);
        instTmpl.setNlTemplateAffinity(affinityAccum / complexityScore);
        return instTmpl;
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
