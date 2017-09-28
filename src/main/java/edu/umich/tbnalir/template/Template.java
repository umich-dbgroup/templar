package edu.umich.tbnalir.template;

import com.esotericsoftware.minlog.Log;
import edu.umich.tbnalir.parse.Having;
import edu.umich.tbnalir.parse.PossibleTranslation;
import edu.umich.tbnalir.rdbms.Attribute;
import edu.umich.tbnalir.rdbms.JoinEdge;
import edu.umich.tbnalir.rdbms.JoinPath;
import edu.umich.tbnalir.parse.Projection;
import edu.umich.tbnalir.rdbms.Relation;
import edu.umich.tbnalir.sql.Operator;
import edu.umich.tbnalir.parse.Predicate;
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

    public Double getSimplicityScore() {
        return 1d / (relations.size() + projections.size() + predicates.size());
    }

    public InstantiatedTemplate instantiate(PossibleTranslation translation) {
        String result = this.templateString;

        /*
        if (this.templateString.contains("from author as author_0, author as author_1, cite as cite_0, publication as publication_0, publication as publication_1, writes as writes_0, writes as writes_1 where ")) {
            int y = 0;
        }

        Relation a0 = new Relation("author", "relation", new HashMap<>());
        Relation a1 = new Relation("author", "relation", new HashMap<>());
        a1.setAliasInt(1);

        Relation c0 = new Relation("cite", "relation", new HashMap<>());

        Relation w0 = new Relation("writes", "relation", new HashMap<>());
        Relation w1 = new Relation("writes", "relation", new HashMap<>());
        w1.setAliasInt(1);

        Relation p0 = new Relation("publication", "relation", new HashMap<>());
        Relation p1 = new Relation("publication", "relation", new HashMap<>());
        p1.setAliasInt(1);

        // TODO: for debug
        if (this.relations.contains(a0) && this.relations.contains(a1)
                && this.relations.contains(w0) && this.relations.contains(w1)
                && this.relations.contains(p0) && this.relations.size() == 5 && translation.getTotalScore() > 2.211) {
            int x = 0;
        }*/

        // TODO: add in score of similarity of query parse tree structure to template parse tree structure

        // CHECK RELATIONS COVERAGE
        int relationCount = this.relations.size();
        Set<Relation> testRelations = new HashSet<>(this.relations);
        testRelations.addAll(translation.getRelations());
        int finalRelationCount = testRelations.size();

        if (relationCount != finalRelationCount) return null;

        // RULE: Only one vertex of a self-join should have projection/predicates associated with its relation.
        for (JoinEdge selfJoin : this.joinPath.getSelfJoins()) {
            boolean firstVertexHasProjPred = false;
            boolean secondVertexHasProjPred = false;

            for (Projection proj : translation.getProjections()) {
                if (selfJoin.getFirst().hasSameRelationAs(proj.getAttribute())) {
                    firstVertexHasProjPred = true;
                }
                if (selfJoin.getSecond().hasSameRelationAs(proj.getAttribute())) {
                    secondVertexHasProjPred = true;
                }
            }

            for (Predicate pred : translation.getPredicates()) {
                if (selfJoin.getFirst().hasSameRelationAs(pred.getAttribute())) {
                    firstVertexHasProjPred = true;
                }
                if (selfJoin.getSecond().hasSameRelationAs(pred.getAttribute())) {
                    secondVertexHasProjPred = true;
                }
            }

            if (firstVertexHasProjPred && secondVertexHasProjPred) {
                return null;
            }
        }

        // RULE: For a non-empty join path, each relation on the terminal of a join path must have
        // at least 1 projection, predicate, or having corresponding to that relation.
        if (!this.joinPath.isEmpty()) {
            terminal_check:
            for (Attribute terminalAttr : this.joinPath.getTerminals()) {
                for (Projection proj : translation.getProjections()) {
                    if (proj.getAttribute().hasSameRelationAs(terminalAttr)) {
                        continue terminal_check;
                    }
                }

                for (Predicate pred : translation.getPredicates()) {
                    if (pred.getAttribute().hasSameRelationAs(terminalAttr)) {
                        continue terminal_check;
                    }
                }

                for (Having having : translation.getHavings()) {
                    if (having.getAttribute().hasSameRelationAs(terminalAttr)) {
                        continue terminal_check;
                    }
                }

                // If we got here, there was a relation of a terminal in the join path
                // for which there was no proj/pred
                return null;
            }
        }

        // RULE: At least 1 relation of an interior vertex in a pseudo-self-join must have a projection/predicate/having
        // associated with it.
        consec_check:
        for (JoinPath consecutive : this.joinPath.getConsecutives()) {
            boolean isPseudoSelfJoin = true;
            Attribute lastAttr = null;
            for (Attribute term : consecutive.getTerminals()) {
                if (lastAttr == null) {
                    lastAttr = term;
                } else {
                    if (!term.hasSameRelationNameAndNameAs(lastAttr)) {
                        isPseudoSelfJoin = false;
                        break;
                    }
                }
            }
            if (isPseudoSelfJoin) {
                for (Attribute consecVertex : consecutive.getInteriorVertices()) {
                    for (Projection proj : translation.getProjections()) {
                        if (proj.getAttribute().hasSameRelationAs(consecVertex)) {
                            continue consec_check;
                        }
                    }

                    for (Predicate pred : translation.getPredicates()) {
                        if (pred.getAttribute().hasSameRelationAs(consecVertex)) {
                            continue consec_check;
                        }
                    }

                    for (Having having : translation.getHavings()) {
                        if (having.getAttribute().hasSameRelationAs(consecVertex)) {
                            continue consec_check;
                        }
                    }
                }

                // Failed rule.
                return null;
            }
        }


        double affinityAccum = 0;

        boolean subquery = false;

        // INSTANTIATE PROJECTION
        if (this.templateString.contains(Constants.PROJ)) {
            // (1) in the case of a projection-hidden template
            StringJoiner sj = new StringJoiner(", ");

            StringJoiner groupBySj = new StringJoiner(", ");

            for (Projection transProj : translation.getProjections()) {
                // If the relation isn't contained in the template, punt!
                if (!this.templateString.contains(" " + transProj.getAttribute().getRelation().toString())) return null;

                // If there's a HAVING and a function on this, defer to a subquery.
                if (translation.getHavings().size() > 0 && transProj.getFunction() != null) {
                    result = "select " + transProj.getFunction() + "(*) from (" + result;
                    subquery = true;
                    sj.add(transProj.getAttribute().toString());
                } else {
                    sj.add(transProj.toString());
                }

                // if GROUP BY, add to string
                if (transProj.isGroupBy() || translation.getHavings().size() > 0 ||
                        (translation.getSuperlative() != null && translation.getSuperlative().getFunction() != null)) {
                    groupBySj.add(transProj.getAttribute().toString());
                }
            }

            affinityAccum += Constants.SLOT_COVERS * translation.getProjections().size();

            result = result.replace(Constants.PROJ, sj.toString());
            if (!groupBySj.toString().isEmpty()) {
                result = result + " group by " + groupBySj.toString();
            }
        } else {
            // (2) in the case of a projection-containing template
            /*
            List<Projection> projectionsList = new ArrayList<>(this.projections);
            for (Projection transProj : translation.getProjections()) {
                if (projectionsList.contains(transProj)) {
                    projectionsList.remove(transProj);
                    continue;
                }

                // If the relation isn't contained in the template, punt!
                if (!this.templateString.contains(" " + transProj.getAttribute().getRelation().toString())) return null;

                // if GROUP BY, add to string
                if (transProj.isGroupBy()) {
                    result = result + " group by " + transProj.toString();
                }

                Integer bestAttrIndex = null;
                boolean bestAttrIsNull = false;

                int i = 0;
                for (Projection tmplProj : projectionsList) {
                    if (tmplProj == null) {
                        if (bestAttrIndex == null) {
                            bestAttrIndex = i;
                            bestAttrIsNull = true;
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
                        result = result.replaceFirst(Constants.COLUMN, transProj.toString());
                        affinityAccum += Constants.SLOT_COVERS;
                    } else {
                        affinityAccum += Constants.EXACT_MATCH;
                    }
                }
            }

            // If we have projections that are unaccounted for, punt!
            if (!projectionsList.isEmpty()) return null;*/
        }

        // CHECK PREDICATES COVERAGE
        List<Predicate> predicatesList = new ArrayList<>(this.predicates);

        if (this.templateString.contains(Constants.PRED)) {
            // (1) in the case of a predicate-hidden template
            StringBuilder sb = new StringBuilder();

            // Special case: no predicates
            if (translation.getPredicates().isEmpty()) {
                result = result.replace(" where " + Constants.PRED, "");
            } else {
                Map<Attribute, String[]> usedAttributes = new HashMap<>();

                translation.getPredicates().sort((a, b) -> a.toString().compareTo(b.toString()));

                int prevUsedIndex = 0;
                for (Predicate transPred : translation.getPredicates()) {
                    // If the relation isn't contained in the template, punt!
                    if (!this.templateString.contains(" " + transPred.getAttribute().getRelation().toString())) return null;

                    // In certain cases, use OR instead of AND because of how numeric predicates are combined
                    boolean useOr = false;

                    // Used attr contains: {OP, VAL}
                    String[] usedAttr = usedAttributes.get(transPred.getAttribute());
                    if (usedAttr != null && StringUtils.isNumeric(transPred.getValue())
                            && StringUtils.isNumeric(usedAttr[1])) {
                        String usedOp = usedAttr[0];
                        Double usedVal = Double.valueOf(usedAttr[1]);
                        Double curVal = Double.valueOf(transPred.getValue());

                        if (usedOp.equals(">") || usedOp.equals(">=")) {
                            if (transPred.getOp().equals(Operator.LT)) {
                                if (curVal < usedVal) {
                                    useOr = true;
                                }
                            }
                        } else if (usedOp.equals("<") || usedOp.equals("<=")){
                            if (transPred.getOp().equals(Operator.GT)) {
                                if (curVal > usedVal) {
                                    useOr = true;
                                }
                            }
                        }
                    }

                    if (sb.length() != 0) {
                        if (useOr) {
                            sb.append(" or ");
                        } else {
                            sb.append(" and ");
                        }
                    }

                    String[] addToUsedAttr = {transPred.getOp().toString(), transPred.getValue()};
                    usedAttributes.put(transPred.getAttribute(), addToUsedAttr);

                    int curIndex = sb.length();
                    sb.append(transPred.toString());
                    if (useOr) {
                        sb.insert(prevUsedIndex, "(");
                        sb.append(")");
                    }
                    prevUsedIndex = curIndex;
                }

                affinityAccum += Constants.SLOT_COVERS * translation.getPredicates().size();

                result = result.replace(Constants.PRED, sb.toString());
            }
        } else {
            /*
            if (predicatesList.size() < translation.getPredicates().size()) return null;

            // (2) in the case of a predicate-visible template
            for (Predicate transPred : translation.getPredicates()) {
                // If the relation isn't contained in the template, punt!
                if (!this.templateString.contains(" " + transPred.getAttribute().getRelation().toString())) return null;

                int mostDetailedMatchCount = 0;
                Predicate mostDetailedMatch = null;

                Attribute transAttr = transPred.getAttribute();
                Operator transOp = transPred.getOp();
                String transValue = transPred.getValue();

                for (Predicate tmplPred : predicatesList) {
                    boolean attrNull = tmplPred.getAttribute() == null;
                    boolean attrMatches = !attrNull && tmplPred.getAttribute().equals(transAttr);
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
                    if (mostDetailedMatch.getAttribute() == null) {
                        toReplace.append(Constants.COLUMN);
                    } else {
                        toReplace.append(mostDetailedMatch.getAttribute().toString());
                    }
                    toReplace.append(" ");

                    if (mostDetailedMatch.getOp() == null) {
                        toReplace.append(Constants.CMP);
                    } else {
                        toReplace.append(mostDetailedMatch.getOp().toString());
                    }
                    toReplace.append(" ");

                    toReplace.append(mostDetailedMatch.getValue());

                    Predicate resultPred = new Predicate(transPred.getAttribute(), transPred.getOp(), transPred.getValue());
                    // In case the alias is different in the translation
                    resultPred.getAttribute().setRelation(transPred.getAttribute().getRelation());

                    /*
                    if (mostDetailedMatch.getAlias() != null) {
                        resultPred.setAlias(mostDetailedMatch.getAlias());
                    }

                    result = result.replaceFirst(toReplace.toString(), resultPred.toString());

                    affinityAccum += (double) mostDetailedMatchCount / 3.0;
                }
            }*/
        }

        StringJoiner havingSj = new StringJoiner(" and ");
        for (Having having : translation.getHavings()) {
            // If the relation isn't contained in the template, punt!
            if (!this.templateString.contains(" " + having.getAttribute().getRelation().toString())) return null;

            havingSj.add(having.toString());
        }
        if (!havingSj.toString().isEmpty()) {
            result += " having " + havingSj.toString();
        }

        if (translation.getSuperlative() != null) {
            result += " order by " + translation.getSuperlative().toString();
        }

        if (subquery) {
            result += ") as subquery";
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
        instTmpl.setTemplate(this);
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
