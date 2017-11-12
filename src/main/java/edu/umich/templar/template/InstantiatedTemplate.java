package edu.umich.templar.template;

import edu.umich.templar.qf.*;
import edu.umich.templar.qf.pieces.Operator;
import edu.umich.templar.rdbms.Attribute;
import edu.umich.templar.rdbms.JoinEdge;
import edu.umich.templar.rdbms.JoinPath;
import edu.umich.templar.rdbms.Relation;
import edu.umich.templar.util.Constants;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.*;

/**
 * Created by cjbaik on 9/12/17.
 */
public class InstantiatedTemplate {
    Template template;
    Translation translation;
    String value;

    public InstantiatedTemplate(Template template, Translation translation) {
        this.template = template;
        this.translation = translation;
        this.value = this.instantiate();
    }

    /*
    public boolean isEquivalentPermutationTo(InstantiatedTemplate other) {
        // In the case that two templates are equivalent (i.e. symmetrical) due to alias permutations
        // e.g. author_0.name = "first" AND author_1.name = "second" VS.
        //      author_0.name = "second" AND author_1.name = "first"

        if (!this.template.equals(other.template)) return false;
        if (this.translation.getParent() == null || other.translation.getParent() == null) return false;
        if (!this.translation.getParent().equals(other.translation.getParent())) return false;

        // The entire join path must be consecutive
        if (this.template.getJoinPath().getConsecutives().size() != 1) return false;
        JoinPath consecs = this.template.getJoinPath().getConsecutives().iterator().next();
        return this.template.getJoinPath().equals(consecs);
    }*/

    private String instantiate() {
        String templateString = this.template.toString();
        String result = templateString;

        // CHECK RELATIONS COVERAGE
        int relationCount = this.template.getRelations().size();
        Set<Relation> testRelations = new HashSet<>(this.template.getRelations());
        testRelations.addAll(this.translation.getReferencedRelations());
        int finalRelationCount = testRelations.size();

        if (relationCount != finalRelationCount) return null;

        // RULE: Only one vertex of a self-join should have projection/predicates associated with its relation.
        /*
        for (JoinEdge selfJoin : this.template.getJoinPath().getSelfJoins()) {
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
        }*/

        // RULE: For a non-empty join path, each relation on the terminal of a join path must have
        // at least 1 projection, predicate, having, or superlative corresponding to that relation.
        if (!this.template.getJoinPath().isEmpty()) {
            terminal_check:
            for (Attribute terminalAttr : this.template.getJoinPath().getTerminals()) {
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

                if (translation.getSuperlative() != null) {
                    if (translation.getSuperlative().getAttribute().hasSameRelationAs(terminalAttr)) {
                        continue;
                    }
                }

                // If we got here, there was a relation of a terminal in the join path
                // for which there was no proj/pred
                return null;
            }
        }

        // RULE: At least 1 relation of an interior vertex in a pseudo-self-join must have a projection/predicate/having
        // associated with it.
        /*
        consec_check:
        for (JoinPath consecutive : this.template.getJoinPath().getConsecutives()) {
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

                    if (translation.getSuperlative() != null) {
                        if (translation.getSuperlative().getAttribute().hasSameRelationAs(consecVertex)) {
                            continue consec_check;
                        }
                    }
                }

                // Failed rule.
                return null;
            }
        }*/

        boolean subquery = false;

        // INSTANTIATE PROJECTION
        if (templateString.contains(Constants.PROJ)) {
            // (1) in the case of a projection-hidden template
            StringJoiner sj = new StringJoiner(", ");

            StringJoiner groupBySj = new StringJoiner(", ");

            for (Projection transProj : translation.getProjections()) {
                // If the relation isn't contained in the template, punt!
                if (!templateString.contains(" " + transProj.getAttribute().getRelation().toString())) return null;

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

            result = result.replace(Constants.PROJ, sj.toString());
            if (!groupBySj.toString().isEmpty()) {
                result = result + " group by " + groupBySj.toString();
            }
        }

        // CHECK PREDICATES COVERAGE
        if (templateString.contains(Constants.PRED)) {
            // (1) in the case of a predicate-hidden template
            StringBuilder sb = new StringBuilder();

            // Special case: no predicates
            if (translation.getPredicates().isEmpty()) {
                // If there is a join
                result = result.replace(" where " + Constants.PRED + " and ", " where ");
                // If there is not a join
                result = result.replace(" where " + Constants.PRED, "");
            } else {
                Map<Attribute, String[]> usedAttributes = new HashMap<>();

                translation.getPredicates().sort((a, b) -> a.toString().compareTo(b.toString()));

                int prevUsedIndex = 0;
                for (Predicate transPred : translation.getPredicates()) {
                    // If the relation isn't contained in the template, punt!
                    if (!templateString.contains(" " + transPred.getAttribute().getRelation().toString())) return null;

                    // In certain cases, use OR instead of AND because of how numeric predicates are combined
                    boolean useOr = false;

                    // Used attr contains: {OP, VAL}
                    String[] usedAttr = usedAttributes.get(transPred.getAttribute());
                    if (usedAttr != null && NumberUtils.isCreatable(transPred.getValue())
                            && NumberUtils.isCreatable(usedAttr[1])) {
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

                result = result.replace(Constants.PRED, sb.toString());
            }
        }

        StringJoiner havingSj = new StringJoiner(" and ");
        for (Having having : translation.getHavings()) {
            // If the relation isn't contained in the template, punt!
            if (!templateString.contains(" " + having.getAttribute().getRelation().toString())) return null;

            havingSj.add(having.toString());
        }
        if (!havingSj.toString().isEmpty()) {
            result += " having " + havingSj.toString();
        }

        if (translation.getSuperlative() != null) {
            // If the relation isn't contained in the template, punt!
            if (!templateString.contains(" " + translation.getSuperlative().getAttribute().getRelation().toString())) return null;
            result += " order by " + translation.getSuperlative().toString();
        }

        if (subquery) {
            result += ") as subquery";
        }

        // If any slots remain after instantiation, punt!
        if (result.contains("#")) return null;
        return result;
    }

    public Double getQFJoinPathScore() {
        double avgDice;

        QFGraph qfGraph = this.translation.qfGraph;
        if (qfGraph == null) {
            avgDice = 1.0;
        } else {
            if (this.template.getJoinPath().isEmpty()) {
                avgDice = 1.0;
            } else {
                double diceSum = 0.0;
                for (JoinEdge edge : this.template.getJoinPath().getJoinEdges()) {
                    RelationFragment firstFrag = new RelationFragment(edge.getFirst().getRelation());
                    RelationFragment secondFrag = new RelationFragment(edge.getSecond().getRelation());
                    QueryFragment first = qfGraph.getOrInsertQF(firstFrag);
                    QueryFragment second = qfGraph.getOrInsertQF(secondFrag);

                    double numer = 2 * first.getCooccurrence(second);
                    double denom = Math.max(first.getCount() + second.getCount(), 1);
                    diceSum += numer / denom;
                }
                avgDice = diceSum / this.template.getJoinPath().getJoinEdges().size();
            }

        }

        return avgDice / this.template.getRelations().size();
    }

    public Double getScore() {
        double delta = 0.01;
        double epsilon = 0.001;

        double templateScore = ((1.0 - delta) * this.getSimplicity()) + (delta * this.getQFJoinPathScore());
        return ((1.0 - epsilon) * this.translation.getScore()) + (epsilon * templateScore);
    }

    private Double getSimplicity() {
        double simplicity;
        // An empty join path is actually a query with 1 relation.
        if (this.template.getJoinPath().getRelationCount() == 0) {
            simplicity = 1.0;
        } else {
            simplicity = 1.0 / this.template.getJoinPath().getRelationCount();
        }

        // TODO: normalize to 0.5, but is there a cleaner way to do this?
        // return (simplicity * 0.5) + 0.25;
        return Math.sqrt(simplicity);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "TOT: " + this.getScore() + "; TR: " + this.translation.getScore() + "; QF: " + this.getQFJoinPathScore() + "; " + this.value;
    }

}
