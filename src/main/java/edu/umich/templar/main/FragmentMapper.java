package edu.umich.templar.main;

import edu.umich.templar.db.*;
import edu.umich.templar.main.settings.Params;
import edu.umich.templar.scorer.InterpretationScorer;
import edu.umich.templar.scorer.SimpleScorer;
import edu.umich.templar.task.*;
import edu.umich.templar.util.Similarity;
import edu.umich.templar.util.Utils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.*;
import java.sql.ResultSet;
import java.util.*;

public class FragmentMapper {
    protected Database db;
    protected Similarity sim;

    // Change the scorer based on what type of system we're using
    protected InterpretationScorer scorer;

    // If we assume that the parser correctly discerned the type
    protected boolean typeOracle;

    protected List<QueryTask> queryTasks;

    // Candidate cache contains <phrase>:<type> -> pruned candidate mappings
    private Map<String, List<MatchedDBElement>> candidateCache;
    private String candCacheFilename;

    public FragmentMapper(Database database, String candCacheFilename, List<QueryTask> queryTasks, boolean typeOracle) {
        this.db = database;
        this.typeOracle = typeOracle;
        this.queryTasks = queryTasks;

        this.candCacheFilename = candCacheFilename;
        this.loadCacheIfExists();

        // Default scorer is SimpleScorer
        this.scorer = new SimpleScorer();

        try {
            this.sim = new Similarity(10000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadCacheIfExists() {
        File file = new File(this.candCacheFilename);
        if (file.exists()) {
            try {
                System.out.println("Loading cache from: <" + this.candCacheFilename + ">");
                FileInputStream fin = new FileInputStream(file);
                ObjectInputStream oin = new ObjectInputStream(fin);
                this.candidateCache = (Map<String, List<MatchedDBElement>>) oin.readObject();
                oin.close();
                fin.close();
                System.out.println("Cache loaded!");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            this.candidateCache = new HashMap<>();
        }
    }

    private void saveCache() {
        System.out.println("Saving cache...");
        File file = new File(this.candCacheFilename);
        try {
            FileOutputStream fout = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(this.candidateCache);
            oos.close();
            fout.close();
            System.out.println("Saved cache to <" + this.candCacheFilename + ">!");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setScorer(InterpretationScorer scorer) {
        this.scorer = scorer;
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
            } else if (fragType.contains("attr") && fragType.contains("pred")) {
                // If it's both an attribute and a predicate, attach the function to the attribute,
                // and create a predicate for all similar values
                for (TextPredicate textPred : this.db.getSimilarValues(tokens)) {
                    cands.add(
                            new AttributeAndPredicate(
                                    new AggregatedAttribute(functions.get(0), textPred.getRelation().getProjAttribute()),
                                    textPred));
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
            boolean findSimValues = !this.typeOracle || fragType.contains("pred");

            if (!foundExactMatch && findSimValues) {
                List<TextPredicate> simValues = this.db.getSimilarValues(tokens);

                if (this.typeOracle && fragType.contains("pred") && fragType.contains("attr")) {
                    for (TextPredicate pred : simValues) {
                        cands.add(new AttributeAndPredicate(pred.getRelation().getProjAttribute(), pred));
                    }
                } else {
                    cands.addAll(simValues);
                }
            }
        }

        System.out.println("Cands for '" + String.join(" ", tokens) + "': " + cands.size());
        return cands;
    }

    private double matchTextPredicate(TextPredicate pred, List<String> tokens) {
        // For cases like "VLDB conference" where the rel/attr name is embedded in the phrase
        List<String> checkTokens = new ArrayList<>();
        for (String token : tokens) {
            // Only remove if it's the first or last token
            if (tokens.indexOf(token) == 0 || tokens.indexOf(token) == (tokens.size() - 1)) {
                boolean tokenIsAttrOrRel = token.equalsIgnoreCase(pred.getAttribute().getCleanedName()) ||
                        token.equalsIgnoreCase(pred.getAttribute().getRelation().getName());
                if (!tokenIsAttrOrRel) {
                    checkTokens.add(token);
                }
            } else {
                checkTokens.add(token);
            }
        }

        return this.sim.sim(pred.getValue().replaceAll("[^A-Za-z0-9 ]", ""), String.join(" ", checkTokens));
    }

    private Set<MatchedDBElement> matchTextCandidates(List<String> tokens, Set<DBElement> textCands,
                                                      String fragType, Boolean groupBy) {
        Set<MatchedDBElement> matchedEls = new HashSet<>();

        for (DBElement cand : textCands) {
            if (cand instanceof Relation) {
                Relation rel = (Relation) cand;
                double sim = this.sim.sim(rel.getName(), String.join(" ", tokens));

                // If we have a type oracle activated and we know it's an attribute result, treat as such
                if (this.typeOracle && fragType.equalsIgnoreCase("attr")) {
                    if (rel.getMainAttribute() != null) {
                        if (groupBy) {
                            matchedEls.add(new MatchedDBElement(new GroupedAttribute(rel.getMainAttribute()), sim));
                        } else {
                            matchedEls.add(new MatchedDBElement(rel.getMainAttribute(), sim));
                        }
                    }
                } else if (this.typeOracle && !fragType.equalsIgnoreCase("rel")) {
                    continue;
                } else {
                    matchedEls.add(new MatchedDBElement(rel, sim));
                }
            } else if (cand instanceof Attribute) {
                Attribute attr = (Attribute) cand;
                double sim = this.sim.sim(attr.getCleanedName(), String.join(" ", tokens));
                if (groupBy) {
                    matchedEls.add(new MatchedDBElement(new GroupedAttribute(attr), sim));
                } else {
                    matchedEls.add(new MatchedDBElement(attr, sim));
                }
            } else if (cand instanceof AggregatedAttribute) {
                AggregatedAttribute aggr = (AggregatedAttribute) cand;

                double attrSim = this.sim.sim(aggr.getAttr().getCleanedName(), String.join(" ", tokens));

                double sim;
                if (aggr.getAttr().isMainAttr()) {
                    double relSim = this.sim.sim(aggr.getAttr().getRelation().getName(), String.join(" ", tokens));
                    sim = Math.max(relSim, attrSim);
                } else {
                    sim = attrSim;
                }

                matchedEls.add(new MatchedDBElement(aggr, sim));
            } else if (cand instanceof TextPredicate) {
                TextPredicate val = (TextPredicate) cand;
                matchedEls.add(new MatchedDBElement(cand, this.matchTextPredicate(val, tokens)));
            } else if (cand instanceof AggregatedPredicate) {
                AggregatedPredicate pred = (AggregatedPredicate) cand;

                double attrSim = this.sim.sim(pred.getAttr().getCleanedName(), String.join(" ", tokens));

                double sim;
                if (pred.getAttr().isMainAttr()) {
                    double relSim = this.sim.sim(pred.getAttr().getRelation().getName(), String.join(" ", tokens));
                    sim = Math.max(relSim, attrSim);
                } else {
                    sim = attrSim;
                }

                matchedEls.add(new MatchedDBElement(pred, sim));
            } else if (cand instanceof AttributeAndPredicate) {
                AttributeAndPredicate attrPred = (AttributeAndPredicate) cand;
                matchedEls.add(new MatchedDBElement(cand,
                        this.matchTextPredicate((TextPredicate) attrPred.getPredicate(), tokens)));
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
                    if (rs.getInt(1) > 0) {
                        sims.add(1 - Params.SQLIZER_EPSILON);
                    } else {
                        sims.add(Params.SQLIZER_EPSILON);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                double predSim = Utils.geometricMean(sims);

                matchedEls.add(new MatchedDBElement(pred, predSim));
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
            // If we ever get to a score below MIN_SIM, just stop and skip
            if (matches.get(i).getScore() < Params.MIN_SIM) {
                break;
            }

            // If we came to the end of a list of exact scores
            if (lastScore > 0.99 && matches.get(i).getScore() < lastScore) {
                break;
            }

            if (i < Params.MAX_TOP_CANDIDATES) {
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

        QueryMappings queryMappings = new QueryMappings(this.scorer);
        for (FragmentTask fragmentTask : queryTask.getFragmentTasks()) {
            List<MatchedDBElement> pruned = this.candidateCache.get(fragmentTask.getKeyString());

            if (pruned == null) {
                List<String> tokens = new ArrayList<>();
                String numericToken = null;

                for (String token : fragmentTask.getPhrase().split(" ")) {
                    if (NumberUtils.isCreatable(token)) {
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
                    matchedEls = this.matchTextCandidates(tokens, cands, fragmentTask.getType(), fragmentTask.getGroupBy());
                } else {
                    cands = this.getNumericCandidateMatches(numericToken, fragmentTask.getOp(), fragmentTask.getFunctions());
                    matchedEls = this.matchNumericCandidates(tokens, cands, fragmentTask.getType());
                }

                List<MatchedDBElement> sorted = new ArrayList<>(matchedEls);
                sorted.sort(Comparator.comparing(MatchedDBElement::getScore).reversed());

                pruned = this.pruneTopMatches(sorted);
                this.candidateCache.put(fragmentTask.getKeyString(), pruned);
            }

            System.out.println("Pruned candidates for " + fragmentTask.getPhrase() + ": " + pruned.size());

            for (MatchedDBElement mel : pruned) {
                System.out.println(" - " + mel);
            }

            FragmentMappings fragMappings = new FragmentMappings(fragmentTask, pruned);
            queryMappings.addFragmentMappings(fragMappings);
        }

        List<Interpretation> interps = queryMappings.findOptimalInterpretations();

        int totalFrags = queryTask.size();
        int ties = interps.size() - 1;

        int correctInterps = 0;

        System.out.println("TOTAL SCORE: " + interps.get(0).getScore() + ", TIES: " + ties);
        for (Interpretation interp : interps) {
            System.out.println("--");
            int correctFrags = 0;

            for (int i = 0; i < queryMappings.getFragmentMappingsList().size(); i++) {
                FragmentMappings fragmentMappings = queryMappings.getFragmentMappingsList().get(i);

                List<String> answers = fragmentMappings.getTask().getAnswers();
                MatchedDBElement bestResult = interp.get(i);

                System.out.println(fragmentMappings.getTask().getPhrase() + " :: "
                        + String.join("; ", answers) + " : " + bestResult);
                if (answers.contains(bestResult.getEl().toString())) {
                    correctFrags++;
                }
            }
            if (correctFrags == totalFrags) correctInterps++;
        }

        boolean correct = (correctInterps == interps.size());

        System.out.println("== RESULT: " + (correct? "CORRECT" : "WRONG") + " == ");
        System.out.println();

        return correct;
    }

    public void execute(Integer queryId) {
        int totalTasks = 0;
        int correctTasks = 0;
        for (QueryTask queryTask : this.queryTasks) {
            if (queryId != null & !queryTask.getQueryId().equals(queryId)) continue;

            boolean correct = this.executeQueryTask(queryTask);
            if (correct) correctTasks++;
            totalTasks++;

            if ((totalTasks % Params.CACHE_SAVE_INTERVAL) == 0) {
                this.saveCache();
            }

            System.out.println("SO FAR: " + correctTasks + "/" + totalTasks);
            System.out.println();
        }

        // Final save for cache
        this.saveCache();

        double accuracyPercent = (double) correctTasks / (double) totalTasks * 100;
        System.out.println("==== FINAL RESULTS ====");
        System.out.println(correctTasks + "/" + totalTasks + " (" + accuracyPercent + "%)");
    }

    public void execute() {
        this.execute(null);
    }
}
