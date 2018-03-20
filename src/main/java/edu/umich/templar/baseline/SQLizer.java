package edu.umich.templar.baseline;

import com.opencsv.CSVReader;
import edu.umich.templar.db.*;
import edu.umich.templar.task.*;
import edu.umich.templar.util.Similarity;
import edu.umich.templar.util.Utils;
import org.apache.commons.lang3.StringUtils;

import java.io.FileReader;
import java.sql.ResultSet;
import java.util.*;

public class SQLizer {
    // If we assume that SQLizer knows the fragment type appropriate for each keyword always
    private boolean typeOracle;

    // Activate the join penalty indiscriminately
    private boolean joinScore;

    private Database db;
    private Similarity sim;

    private Map<Integer, QueryTask> queryTasks = new HashMap<>();

    public SQLizer(Database database, String filename, boolean typeOracle, boolean joinScore) {
        this.typeOracle = typeOracle;
        this.joinScore = joinScore;
        this.db = database;

        try {
            this.sim = new Similarity(10000);
            this.queryTasks = QueryTaskReader.readQueryTasks(filename);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Set<DBElement> getTextCandidateMatches(List<String> tokens, String fragType,
                                                   String op, List<String> functions) {
        Set<DBElement> cands = new HashSet<>();

        if (op.isEmpty()) op = "=";

        if (functions.size() == 1) {
            // If there's one function, we're dealing with an AggregatedAttribute or AggregatedPredicate
            if (fragType.equalsIgnoreCase("attr")) {
                for (Attribute attr : this.db.getAllAttributes()) {
                    cands.add(new AggregatedAttribute(functions.get(0), attr));
                }
            } else {
                for (Attribute attr : this.db.getAllAttributes()) {
                    cands.add(new AggregatedPredicate(null, attr, op, functions.get(0)));
                }
            }
        } else if (functions.size() > 1) {
            // If there's more than 1 function, it needs to be an AggregatedPredicate

            String aggFunction = null;
            String valueFunction = null;

            for (String function : functions) {
                if (function.equalsIgnoreCase("max") ||
                        function.equalsIgnoreCase("min") ||
                        function.equalsIgnoreCase("avg")) {
                    if (valueFunction != null) throw new RuntimeException("Value function already set!");
                    valueFunction = function;
                } else {
                    if (aggFunction != null) throw new RuntimeException("Aggregate function already set!");
                    aggFunction = function;
                }
            }

            for (Relation rel : this.db.getAllRelations()) {
                if (rel.getMainAttribute() != null) {
                    cands.add(new AggregatedPredicate(aggFunction, rel.getMainAttribute(), op, valueFunction));
                }
            }
        } else {
            // Add relations to candidates always
            cands.addAll(this.db.getAllRelations());

            // Only find attributes if we don't know what type, or it's not a relation
            boolean findAttributes = !this.typeOracle || !fragType.equalsIgnoreCase("rel");
            if (findAttributes) cands.addAll(this.db.getAllAttributes());

            // Skip tokens if we find an exact match
            boolean foundExactMatch = false;
            for (DBElement el : cands) {
                if (el instanceof Relation) {
                    Relation rel = (Relation) el;
                    double sim = this.sim.sim(rel.getName(), String.join(" ", tokens));
                    if (sim == 1.0) {
                        foundExactMatch = true;
                        break;
                    }
                } else if (el instanceof Attribute) {
                    Attribute attr = (Attribute) el;
                    double sim = this.sim.sim(attr.getName(), String.join(" ", tokens));
                    if (sim == 1.0) {
                        foundExactMatch = true;
                    }
                } else {
                    throw new RuntimeException("Unexpected DBElement type.");
                }
            }

            // If we are operating in type-oracle mode and fragment is a predicate, then add similar values as tokens
            // (otherwise, we know it's a projection/relation so we don't need to find values)
            boolean findSimValues = !this.typeOracle || fragType.equalsIgnoreCase("pred");

            if (!foundExactMatch && findSimValues) {
                cands.addAll(this.db.getSimilarValues(tokens));
            }
        }

        System.out.println("Cands for '" + String.join(" ", tokens) + "': " + cands.size());
        return cands;
    }

    private Set<MatchedDBElement> matchTextCandidates(List<String> tokens, Set<DBElement> textCands, String fragType) {
        Set<MatchedDBElement> matchedEls = new HashSet<>();

        for (DBElement cand : textCands) {
            if (cand instanceof Relation) {
                Relation rel = (Relation) cand;
                double sim = this.sim.sim(rel.getName(), String.join(" ", tokens));

                // If we have a type oracle activated and we know it's an attribute result, treat as such
                if (this.typeOracle && fragType.equalsIgnoreCase("attr")) {
                    if (rel.getMainAttribute() != null) {
                        matchedEls.add(new MatchedDBElement(rel.getMainAttribute(), sim));
                    }
                } else {
                    matchedEls.add(new MatchedDBElement(rel, sim));
                }
            } else if (cand instanceof Attribute) {
                Attribute attr = (Attribute) cand;
                List<Double> sims = new ArrayList<>();
                double relSim = this.sim.sim(attr.getRelation().getName(), String.join(" ", tokens));
                sims.add(relSim);
                double attrSim = this.sim.sim(attr.getCleanedName(), String.join(" ", tokens));
                sims.add(attrSim);

                double sim = Utils.geometricMean(sims);
                matchedEls.add(new MatchedDBElement(attr, sim));
            } else if (cand instanceof AggregatedAttribute) {
                AggregatedAttribute aggr = (AggregatedAttribute) cand;

                List<Double> sims = new ArrayList<>();
                double relSim = this.sim.sim(aggr.getAttr().getRelation().getName(), String.join(" ", tokens));
                sims.add(relSim);
                double attrSim = this.sim.sim(aggr.getAttr().getCleanedName(), String.join(" ", tokens));
                sims.add(attrSim);
                double sim = Utils.geometricMean(sims);

                matchedEls.add(new MatchedDBElement(aggr, sim));
            } else if (cand instanceof TextPredicate) {
                TextPredicate val = (TextPredicate) cand;

                // For cases like "VLDB conference" where the rel/attr name is embedded in the phrase
                List<String> checkTokens = new ArrayList<>();
                for (String token : tokens) {
                    // Only remove if it's the first or last token
                    if (tokens.indexOf(token) == 0 || tokens.indexOf(token) == (tokens.size() - 1)) {
                        boolean tokenIsAttrOrRel = token.equalsIgnoreCase(val.getAttribute().getCleanedName()) ||
                                token.equalsIgnoreCase(val.getAttribute().getRelation().getName());
                        if (!tokenIsAttrOrRel) {
                            checkTokens.add(token);
                        }
                    } else {
                        checkTokens.add(token);
                    }
                }

                double sim = this.sim.sim(val.getValue(), String.join(" ", checkTokens));
                matchedEls.add(new MatchedDBElement(val, sim));
            } else if (cand instanceof AggregatedPredicate) {
                AggregatedPredicate pred = (AggregatedPredicate) cand;

                List<Double> sims = new ArrayList<>();
                double relSim = this.sim.sim(pred.getAttr().getRelation().getName(), String.join(" ", tokens));
                sims.add(relSim);
                double attrSim = this.sim.sim(pred.getAttr().getCleanedName(), String.join(" ", tokens));
                sims.add(attrSim);
                double sim = Utils.geometricMean(sims);

                matchedEls.add(new MatchedDBElement(pred, sim));
            } else {
                throw new RuntimeException("Invalid DBElement type.");
            }
        }
        return matchedEls;
    }

    private Set<DBElement> getNumericCandidateMatches(String numericToken, String op, List<String> functions) {
        if (op.isEmpty()) op = "=";

        // If tokens, narrow it down first
        Set<DBElement> cands = new HashSet<>();

        // Add relations to candidates only and functions exist
        if (!functions.isEmpty()) {
            if (functions.size() > 1) throw new RuntimeException("Unexpected number of functions.");

            for (Relation rel : this.db.getAllRelations()) {
                // If single function, we can handle with NumericPredicate
                if (rel.getMainAttribute() != null) {
                    cands.add(new NumericPredicate(rel.getMainAttribute(), op,
                            Double.valueOf(numericToken), functions.get(0)));
                }
            }
        }

        // Only find similar values if the function isn't a count
        boolean findSimValues = !functions.contains("count");

        // Add numeric predicates
        if (findSimValues) {
            String function = functions.size() == 1? functions.get(0) : null;
            for (Attribute attr : this.db.getNumericAttributes()) {
                cands.add(new NumericPredicate(attr, op, Double.valueOf(numericToken), function));
            }
        }

        System.out.println("Cands for '" + op + " " + numericToken + "': " + cands.size());

        return cands;
    }

    private Set<MatchedDBElement> matchNumericCandidates(List<String> tokens, Set<DBElement> numericCands, String fragType) {
        Set<MatchedDBElement> matchedEls = new HashSet<>();

        for (DBElement cand : numericCands) {
            if (cand instanceof NumericPredicate) {
                NumericPredicate pred = (NumericPredicate) cand;

                List<Double> sims = new ArrayList<>();

                if (!tokens.isEmpty()) {
                    double attrSim = this.sim.sim(pred.getAttr().getCleanedName(), String.join(" ", tokens));
                    sims.add(attrSim);
                }

                // Check predicate
                try {
                    ResultSet rs = this.db.executeSQL("SELECT IF(EXISTS(SELECT * FROM " + pred.getAttr().getRelation().getName()
                            + " WHERE " + pred.getAttr().getName() + " " + pred.getOp() + " " + pred.getValue()
                            + "), 1, 0) AS result");
                    rs.next();
                    double databaseContentSim;
                    if (rs.getInt(1) > 0) {
                        databaseContentSim = 1 - BaselineParams.SQLIZER_EPSILON;
                    } else {
                        databaseContentSim = BaselineParams.SQLIZER_EPSILON;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                double predSim = Utils.geometricMean(sims);

                List<Double> selSims = new ArrayList<>();
                selSims.add(predSim);
                double relSim = this.sim.sim(pred.getAttr().getRelation().getName(), String.join(" ", tokens));
                selSims.add(relSim);
                double selSim = Utils.geometricMean(selSims);

                matchedEls.add(new MatchedDBElement(pred, selSim));
            } else {
                throw new RuntimeException("Invalid DBElement type.");
            }
        }
        return matchedEls;
    }

    private List<MatchedDBElement> pruneTopMatches(List<MatchedDBElement> matches) {
        List<MatchedDBElement> pruned = new ArrayList<>();

        double lastScore = 0.0;
        for (int i = 0; i < matches.size(); i++) {
            if (i < BaselineParams.MAX_TOP_CANDIDATES) {
                pruned.add(matches.get(i));
                lastScore = matches.get(i).getScore();
                continue;
            }

            // If we went beyond the MAX_TOP_CANDIDATES, only keep going so long as we have a tie
            if (matches.get(i).getScore() == lastScore) {
                pruned.add(matches.get(i));
            } else {
                break;
            }
        }
        return pruned;
    }

    private boolean executeQueryTask(QueryTask queryTask) {
        System.out.println("== QUERY ID: " + queryTask.getQueryId() + " ==");

        QueryMappings queryMappings = new QueryMappings(this.db, this.joinScore);
        for (FragmentTask fragmentTask : queryTask.getFragmentTasks()) {
            List<String> tokens = new ArrayList<>();
            String numericToken = null;

            for (String token : fragmentTask.getPhrase().split(" ")) {
                if (StringUtils.isNumeric(token)) {
                    numericToken = token;
                } else {
                    tokens.add(token);
                }
            }

            Set<DBElement> cands;
            Set<MatchedDBElement> matchedEls;
            if (numericToken == null) {
                cands = this.getTextCandidateMatches(tokens, fragmentTask.getType(),
                        fragmentTask.getOp(), fragmentTask.getFunctions());
                matchedEls = this.matchTextCandidates(tokens, cands, fragmentTask.getType());
            } else {
                cands = this.getNumericCandidateMatches(numericToken, fragmentTask.getOp(), fragmentTask.getFunctions());
                matchedEls = this.matchNumericCandidates(tokens, cands, fragmentTask.getType());
            }

            List<MatchedDBElement> sorted = new ArrayList<>(matchedEls);
            sorted.sort(Comparator.comparing(MatchedDBElement::getScore).reversed());
            FragmentMappings fragMappings = new FragmentMappings(fragmentTask,
                    this.pruneTopMatches(sorted));
            queryMappings.addFragmentMappings(fragMappings);
        }

        List<Interpretation> interps = queryMappings.findOptimalInterpretations();
        int correctFrags = 0;
        int totalFrags = queryTask.size();
        int ties = interps.size() - 1;

        System.out.println("TOTAL SCORE: " + interps.get(0).getScore() + ", TIES: " + ties);
        for (Interpretation interp : interps) {
            System.out.println("--");
            for (int i = 0; i < queryMappings.getFragmentMappingsList().size(); i++) {
                FragmentMappings fragmentMappings = queryMappings.getFragmentMappingsList().get(i);

                String answer = fragmentMappings.getTask().getAnswer();
                MatchedDBElement bestResult = interp.get(i);

                System.out.println(fragmentMappings.getTask().getPhrase() + " :: "
                        + answer + " : " + bestResult);
                if (bestResult.getEl().toString().equals(answer)) {
                    correctFrags++;
                }
            }
        }
        boolean correct = (correctFrags == totalFrags) && (ties == 0);

        System.out.println("== RESULT: " + (correct? "CORRECT" : "WRONG") + " == ");
        System.out.println();

        return correct;
    }

    public void execute(int startAt) {
        int totalTasks = 0;
        int correctTasks = 0;
        for (Map.Entry<Integer, QueryTask> e : this.queryTasks.entrySet()) {
            if (e.getKey() < startAt) continue;

            boolean correct = this.executeQueryTask(e.getValue());
            if (correct) correctTasks++;
            totalTasks++;

            System.out.println("SO FAR: " + correctTasks + "/" + totalTasks);
            System.out.println();
        }

        double accuracyPercent = (double) correctTasks / (double) totalTasks * 100;
        System.out.println("==== FINAL RESULTS ====");
        System.out.println(correctTasks + "/" + totalTasks + " (" + accuracyPercent + "%)");
    }

    public void execute() {
        this.execute(0);
    }

    public static void main(String[] args) {
        String dbHost = args[0];
        Integer dbPort = Integer.valueOf(args[1]);
        String dbUser = args[2];
        String dbPass = args[3].equalsIgnoreCase("null")? null : args[3];
        Boolean typeOracle = Boolean.valueOf(args[4]);
        Boolean joinScore = Boolean.valueOf(args[5]);

        Database database = new Database(dbHost, dbPort, dbUser, dbPass,
                "mas", "data/mas/mas.edges.json", "data/mas/mas.main_attrs.json");
        SQLizer sqlizer = new SQLizer(database, "data/mas/mas_all_fragments.csv", typeOracle, joinScore);

        if (args.length >= 7) {
            sqlizer.execute(Integer.valueOf(args[6]));
        } else {
            sqlizer.execute();
        }
    }
}
