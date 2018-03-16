package edu.umich.templar.baseline;

import com.opencsv.CSVReader;
import edu.umich.templar.db.*;
import edu.umich.templar.util.Similarity;
import org.apache.commons.lang3.StringUtils;

import java.io.FileReader;
import java.sql.ResultSet;
import java.util.*;

public class SQLizer {
    private Database db;
    private Similarity sim;

    private Map<Integer, QueryTask> queryTasks = new HashMap<>();

    public SQLizer(Database database, String filename) {
        this.db = database;
        this.db.loadFKPK("data/mas/mas.edges.json");

        try {
            CSVReader reader = new CSVReader(new FileReader(filename));
            this.sim = new Similarity(10000);

            this.readQueryTasks(reader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void readQueryTasks(CSVReader reader) {
        try {
            // Skip first header line
            reader.readNext();

            this.queryTasks = new HashMap<>();

            // Read all fragmentTasks in from CSV file
            String [] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                int queryId = Integer.valueOf(nextLine[0]);
                String phrase = nextLine[1];
                String op = nextLine[2];
                String type = nextLine[3];
                String answer = nextLine[7];

                QueryTask task = this.queryTasks.get(queryId);
                if (task == null) {
                    task = new QueryTask(queryId);
                    this.queryTasks.put(queryId, task);
                }

                task.addMapping(new FragmentTask(phrase, op, type, answer));
            }
            reader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<DBElement> getTextCandidateMatches(List<String> tokens) {
        List<DBElement> cands = new ArrayList<>();

        // Add relations, attributes to check
        cands.addAll(this.db.getAllRelations());
        cands.addAll(this.db.getAllAttributes());

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

        if (!foundExactMatch) cands.addAll(this.db.getSimilarValues(tokens));

        System.out.println("Cands for '" + String.join(" ", tokens) + "': " + cands.size());
        return cands;
    }

    private List<MatchedDBElement> matchTextCandidates(List<String> tokens, List<DBElement> textCands) {
        List<MatchedDBElement> matchedEls = new ArrayList<>();

        for (DBElement cand : textCands) {
            if (cand instanceof Relation) {
                Relation rel = (Relation) cand;
                double sim = this.sim.sim(rel.getName(), String.join(" ", tokens));
                matchedEls.add(new MatchedDBElement(rel, sim));
            } else if (cand instanceof Attribute) {
                Attribute attr = (Attribute) cand;
                double sim = this.sim.sim(attr.getCleanedName(), String.join(" ", tokens));
                matchedEls.add(new MatchedDBElement(attr, sim));
            } else if (cand instanceof Value) {
                Value val = (Value) cand;

                // For cases like "VLDB conference" where the rel/attr name is embedded in the phrase
                List<String> checkTokens = new ArrayList<>();
                for (String token : tokens) {
                    boolean tokenIsAttrOrRel = token.equalsIgnoreCase(val.getAttribute().getCleanedName()) ||
                            token.equalsIgnoreCase(val.getAttribute().getRelation().getName());
                    if (!tokenIsAttrOrRel) {
                        checkTokens.add(token);
                    }
                }

                double sim = this.sim.sim(val.getValue(), String.join(" ", checkTokens));
                matchedEls.add(new MatchedDBElement(val, sim));
            } else {
                throw new RuntimeException("Invalid DBElement type.");
            }
        }
        return matchedEls;
    }

    private List<DBElement> getNumericCandidateMatches(String numericToken, String op) {
        if (op.isEmpty()) op = "=";

        // If tokens, narrow it down first
        List<DBElement> cands = new ArrayList<>();

        // Add relations, attributes to check
        cands.addAll(this.db.getAllRelations());
        cands.addAll(this.db.getNumericAttributes());

        // Add numeric predicates
        for (Attribute attr : this.db.getNumericAttributes()) {
            cands.add(new NumericPredicate(attr, op, Double.valueOf(numericToken)));
        }

        System.out.println("Cands for '" + op + " " + numericToken + "': " + cands.size());

        return cands;
    }

    private List<MatchedDBElement> matchNumericCandidates(List<String> tokens, List<DBElement> numericCands) {
        List<MatchedDBElement> matchedEls = new ArrayList<>();

        for (DBElement cand : numericCands) {
            if (cand instanceof Relation) {
                Relation rel = (Relation) cand;
                double sim = this.sim.sim(rel.getName(), String.join(" ", tokens));
                matchedEls.add(new MatchedDBElement(rel, sim));
            } else if (cand instanceof Attribute) {
                Attribute attr = (Attribute) cand;
                double sim = this.sim.sim(attr.getCleanedName(), String.join(" ", tokens));
                matchedEls.add(new MatchedDBElement(attr, sim));
            } else if (cand instanceof NumericPredicate) {
                NumericPredicate pred = (NumericPredicate) cand;

                double sim = 1.0;

                if (!tokens.isEmpty()) {
                    sim = this.sim.sim(pred.getAttr().getCleanedName(), String.join(" ", tokens));
                }

                // Check predicate
                try {
                    ResultSet rs = this.db.executeSQL("SELECT IF(EXISTS(SELECT * FROM " + pred.getAttr().getRelation().getName()
                            + " WHERE " + pred.getAttr().getName() + " " + pred.getOp() + " " + pred.getValue()
                            + "), 1, 0) AS result");
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        sim *= 1 - BaselineParams.SQLIZER_EPSILON;
                    } else {
                        sim *= BaselineParams.SQLIZER_EPSILON;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                matchedEls.add(new MatchedDBElement(pred, Math.pow(sim, 1/2)));
            } else {
                throw new RuntimeException("Invalid DBElement type.");
            }
        }
        return matchedEls;
    }

    private void executeQueryTask(QueryTask queryTask) {
        System.out.println("== QUERY ID: " + queryTask.getQueryId() + " ==");

        QueryMappings queryMappings = new QueryMappings(this.db);
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

            List<DBElement> cands;
            List<MatchedDBElement> matchedEls;
            if (numericToken == null) {
                cands = this.getTextCandidateMatches(tokens);
                matchedEls = this.matchTextCandidates(tokens, cands);
            } else {
                cands = this.getNumericCandidateMatches(numericToken, fragmentTask.getOp());
                matchedEls = this.matchNumericCandidates(tokens, cands);
            }
            matchedEls.sort(Comparator.comparing(MatchedDBElement::getScore).reversed());
            FragmentMappings fragMappings = new FragmentMappings(fragmentTask,
                    matchedEls.subList(0, BaselineParams.MAX_TOP_CANDIDATES));
            queryMappings.addFragmentMappings(fragMappings);
        }

        List<Interpretation> interps = queryMappings.findOptimalInterpretations();
        int correctFrags = 0;
        int totalFrags = queryTask.size();
        int ties = interps.size() - 1;

        System.out.println("TOTAL SCORE: " + interps.get(0).getScore() + ", TIES: " + ties);
        for (int i = 0; i < queryMappings.getFragmentMappingsList().size(); i++) {
            FragmentMappings fragmentMappings = queryMappings.getFragmentMappingsList().get(i);

            String answer = fragmentMappings.getTask().getAnswer();
            MatchedDBElement bestResult = interps.get(0).get(i);

            System.out.println(fragmentMappings.getTask().getPhrase() + " :: "
                    + answer  + " : " + bestResult);
            if (bestResult.getEl().toString().equals(answer)) {
                correctFrags++;
            }
        }
        boolean correct = (correctFrags == totalFrags) && (ties == 0);

        System.out.println("== RESULT: " + (correct? "CORRECT" : "WRONG") + " == ");
        System.out.println();
    }

    public void execute(int startAt) {
        for (Map.Entry<Integer, QueryTask> e : this.queryTasks.entrySet()) {
            if (e.getKey() < startAt) continue;

            this.executeQueryTask(e.getValue());
        }
    }

    public void execute() {
        this.execute(0);
    }

    public static void main(String[] args) {
        Database database = new Database("localhost", 3306, "root", null, "mas");
        SQLizer sqlizer = new SQLizer(database, "data/mas/mas_all_fragments.csv");
        sqlizer.execute(7);
    }
}
