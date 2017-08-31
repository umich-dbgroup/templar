package edu.umich.tbnalir.template;

import com.esotericsoftware.minlog.Log;
import edu.umich.tbnalir.rdbms.*;
import edu.umich.tbnalir.util.Utils;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.*;

/**
 * Created by cjbaik on 6/30/17.
 */
public class SchemaDataTemplateGenerator {
    RDBMS db;

    int joinLevel;

    Map<String, Relation> relations;
    Map<Attribute, Set<Attribute>> fkpkEdges;
    Map<Attribute, Set<Attribute>> pkfkEdges;

    // keeps track of relation lists already generated so we can skip them. in form: <relation1Name><relation2Name>...
    Set<String> relationListsAlreadyGenerated;

    public SchemaDataTemplateGenerator(String relationsFile, String edgesFile, int joinLevel, RDBMS db) {
        this.joinLevel = joinLevel;
        this.relations = new HashMap<>();
        TemplateRoot.relations = this.relations; // Kind of a hack, but want to store globally for TemplateRoot fns

        this.fkpkEdges = new HashMap<>();
        this.pkfkEdges = new HashMap<>();
        this.db = db;

        this.loadRelationsFromFile(relationsFile);
        this.loadEdgesFromFile(edgesFile);

        this.relationListsAlreadyGenerated = new HashSet<>();
    }

    public Map<String, Relation> getRelations() {
        return relations;
    }

    public void loadRelationsFromFile(String relationsFileName) {
        JSONParser parser = new JSONParser();

        Log.info("==============================");
        try {
            Log.info("Reading relations info...");
            JSONObject rels = (JSONObject) parser.parse(new FileReader(relationsFileName));
            for (Object relNameObj : rels.keySet()) {
                String relName = (String) relNameObj;
                JSONObject relInfo = (JSONObject) rels.get(relNameObj);
                JSONObject attrObj = (JSONObject) relInfo.get("attributes");
                Map<String, Attribute> attributeMap = new HashMap<>();
                for (Object attrNameObj : attrObj.keySet()) {
                    JSONObject attrInfo = (JSONObject) attrObj.get(attrNameObj);
                    String attrName = (String) attrInfo.get("name");
                    String attrType = (String) attrInfo.get("type");
                    Attribute attr = new Attribute(attrName, attrType);
                    attributeMap.put(attrName, attr);

                    if (attrInfo.get("fk") != null) {
                        attr.setFk((Boolean) attrInfo.get("fk"));
                    }
                    if (attrInfo.get("pk") != null) {
                        attr.setPk((Boolean) attrInfo.get("pk"));
                    }

                    if (attrInfo.get("entropy") != null) {
                        attr.setEntropy((Double) attrInfo.get("entropy"));
                    }
                }


                // For functions, also add parameters
                if (relInfo.containsKey("inputs")) {
                    JSONObject inputObj = (JSONObject) relInfo.get("inputs");
                    Map<Integer, FunctionParameter> inputs = new HashMap<>();
                    for (Object inputNameObj : inputObj.keySet()) {
                        JSONObject inputInfoObj = (JSONObject) inputObj.get(inputNameObj);
                        String inputName = (String) inputInfoObj.get("name");
                        String inputType = (String) inputInfoObj.get("type");
                        Integer inputIndex = Integer.valueOf((String) inputInfoObj.get("index"));
                        FunctionParameter param = new FunctionParameter(inputName, inputType, inputIndex);
                        inputs.put(inputIndex, param);
                    }

                    Function fn = new Function(relName, (String) relInfo.get("type"), attributeMap, inputs);
                    this.relations.put(relName, fn);
                } else {
                    Relation rel = new Relation(relName, (String) relInfo.get("type"), attributeMap);
                    this.relations.put(relName, rel);
                }

            }
            Log.info("Read " + this.relations.size() + " relations/views/functions.");
            Log.info("==============================\n");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public void loadEdgesFromFile(String edgesFileName) {
        try {
            JSONParser parser = new JSONParser();
            Log.info("==============================");
            Log.info("Reading edges info...");
            JSONArray edgesArr = (JSONArray) parser.parse(new FileReader(edgesFileName));
            for (Object edgeObj : edgesArr) {
                JSONObject edge = (JSONObject) edgeObj;

                String foreignRelationStr = (String) edge.get("foreignRelation");
                if (foreignRelationStr == null) {
                    Log.error("Foreign relation not included in edge definition.");
                    continue;
                }
                Relation foreignRelation = relations.get(foreignRelationStr);
                if (foreignRelation == null) {
                    Log.error("Could not find relation <" + foreignRelationStr + "> in schema.");
                    continue;
                }
                String foreignAttributeStr = (String) edge.get("foreignAttribute");
                if (foreignAttributeStr == null) {
                    Log.error("Foreign attribute not included in edge definition.");
                    continue;
                }
                Attribute foreignAttribute = foreignRelation.getAttributes().get(foreignAttributeStr);
                if (foreignAttribute == null) {
                    Log.error("Could not find attribute <" + foreignAttributeStr +
                            "> in relation <" + foreignRelation + ">");
                    continue;
                }

                String primaryRelationStr = (String) edge.get("primaryRelation");
                if (primaryRelationStr == null) {
                    Log.error("Primary relation not included in edge definition.");
                    continue;
                }
                Relation primaryRelation = relations.get(primaryRelationStr);
                if (primaryRelation == null) {
                    Log.error("Could not find relation <" + primaryRelationStr + "> in schema.");
                    continue;
                }
                String primaryAttributeStr = (String) edge.get("primaryAttribute");
                if (primaryAttributeStr == null) {
                    Log.error("Primary attribute not included in edge definition.");
                    continue;
                }
                Attribute primaryAttribute = primaryRelation.getAttributes().get(primaryAttributeStr);
                if (primaryAttribute == null) {
                    Log.error("Could not find attribute <" + primaryAttributeStr +
                            "> in relation <" + primaryRelation + ">");
                    continue;
                }

                    /*
                    Log.info("FK: " + foreignRelation + "/" + foreignAttribute + "\t"
                            + "PK: " + primaryRelation + "/" + primaryAttribute);
                            */

                Set<Attribute> pks = this.fkpkEdges.get(foreignAttribute);
                if (pks == null) {
                    pks = new HashSet<>();
                    this.fkpkEdges.put(foreignAttribute, pks);
                }
                pks.add(primaryAttribute);

                Set<Attribute> fks = this.pkfkEdges.get(primaryAttribute);
                if (fks == null) {
                    fks = new HashSet<>();
                    this.pkfkEdges.put(primaryAttribute, fks);
                }
                fks.add(foreignAttribute);
            }

            int fkpkLength = 0;
            for (Map.Entry<Attribute, Set<Attribute>> e : this.fkpkEdges.entrySet()) {
                fkpkLength += e.getValue().size();
            }
            Log.info("Read " + fkpkLength + " FK-PK relationships.");

            int pkfkLength = 0;
            for (Map.Entry<Attribute, Set<Attribute>> e : this.pkfkEdges.entrySet()) {
                pkfkLength += e.getValue().size();
            }
            Log.info("Read " + pkfkLength + " PK-FK relationships.");
            Log.info("==============================\n");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public Set<Set<Attribute>> guessProjections(List<Relation> relations, Integer chooseTop, Integer maxSize) {
        List<Attribute> attributes = new ArrayList<>();

        for (Relation r : relations) {
            // Assume we don't want any primary/foreign keys
            r.getRankedAttributes().stream().filter(attr -> !attr.isFk() && !attr.isPk()).forEach(attributes::add);
        }

        // Select up to max size only to do power sets (from all relations combined, using entropy)
        if (maxSize != null) {
            attributes.sort((a, b) -> a.getEntropy().compareTo(b.getEntropy()));
            attributes = attributes.subList(0, Math.min(attributes.size(), chooseTop));
        }

        return Utils.powerSet(attributes, maxSize);
    }

    public Set<Set<Attribute>> guessPredicateAttributes(List<Relation> relations, Integer chooseTop, Integer maxSize) {
        List<Attribute> attributes = new ArrayList<>();

        for (Relation r : relations) {
            // Assume we don't want any primary/foreign keys
            r.getRankedAttributes().stream().filter(attr -> !attr.isFk() && !attr.isPk()).forEach(attributes::add);
        }

        // Select up to max size only to do power sets (from all relations combined, using entropy)
        if (maxSize != null) {
            attributes.sort((a, b) -> a.getEntropy().compareTo(b.getEntropy()));
            attributes = attributes.subList(0, Math.min(attributes.size(), chooseTop));
        }

        return Utils.powerSet(attributes, maxSize);
    }

    public Set<Template> getTemplatesForRelationsRecursive(Select select, List<Relation> relations, int joinLevelsLeft) {
        // Check that we haven't already generated this relation list
        StringBuilder relationsListNameBuf = new StringBuilder();
        relations.stream().map(Relation::getName).sorted().forEach(relationsListNameBuf::append);
        String relationsListName = relationsListNameBuf.toString();
        if (this.relationListsAlreadyGenerated.contains(relationsListName)) {
            return new HashSet<>();
        }
        this.relationListsAlreadyGenerated.add(relationsListName);

        Set<Template> templates = new HashSet<>();
        PlainSelect ps = (PlainSelect) select.getSelectBody();

        if (joinLevelsLeft > 0) {
            for (Relation rel : relations) {
                for (Map.Entry<String, Attribute> attrEntry : rel.getAttributes().entrySet()) {
                    Attribute attr = attrEntry.getValue();
                    Set<Attribute> fks = this.pkfkEdges.get(attrEntry.getValue());
                    if (fks != null) {
                        for (Attribute otherFk : fks) {
                            // Assumes we don't have join tables that reference the same table twice
                            if (relations.contains(otherFk.getRelation())) continue;

                            Join join = Utils.addJoin(select, attr, otherFk);
                            List<Relation> newRelations = new ArrayList<>(relations);
                            newRelations.add(otherFk.getRelation());

                            templates.addAll(this.getTemplatesForRelationsRecursive(select, newRelations, joinLevelsLeft - 1));

                            // Reset Select to original state after recursion
                            ps.getJoins().remove(join);
                        }
                    }

                    Set<Attribute> pks = this.fkpkEdges.get(attrEntry.getValue());
                    if (pks != null) {
                        for (Attribute otherPk : pks) {
                            // Assumes we don't have join tables that reference the same table twice
                            if (relations.contains(otherPk.getRelation())) continue;

                            Join join = Utils.addJoin(select, attr, otherPk);
                            List<Relation> newRelations = new ArrayList<>(relations);
                            newRelations.add(otherPk.getRelation());

                            templates.addAll(this.getTemplatesForRelationsRecursive(select, newRelations, joinLevelsLeft - 1));

                            // Reset Select to original state after recursion
                            ps.getJoins().remove(join);
                        }
                    }
                }
            }
        }

        for (Relation r : relations) {
            // Rank attributes by entropy first
            r.rankAttributesByEntropy(this.db);
        }

        TemplateRoot tr = new TemplateRoot(select);

        /*
         * Generate templates
         */

        // Determines how max size of projections. If null, use all.
        Integer chooseTopProjection = 4;
        Integer maxProjectionSize = 2;
        Integer chooseTopPredicate = 4;
        Integer maxPredicateSize = 3;

        Set<Set<Attribute>> projections = this.guessProjections(relations, chooseTopProjection, maxProjectionSize);
        Set<Set<Attribute>> predicateAttributes = this.guessPredicateAttributes(relations, chooseTopPredicate, maxPredicateSize);

        templates.addAll(tr.generateNoPredicateProjectionTemplates());
        templates.addAll(tr.generateNoPredicateTemplates(projections));
        templates.addAll(tr.generateNoComparisonProjectionTemplates(predicateAttributes));
        templates.addAll(tr.generateNoComparisonTemplates(projections, predicateAttributes));
        templates.addAll(tr.generateNoConstantProjectionTemplates(predicateAttributes));
        templates.addAll(tr.generateNoConstantTemplates(projections, predicateAttributes));

        // TODO: for each hypothesized full query template (guess attributes, operators, constants, and projections)

        return templates;
    }

    public Set<Template> generate() {
        Set<Template> templates = new HashSet<>();

        Log.info("==============================");
        Log.info("Generating templates using schema for join level: " + this.joinLevel);

        for (Map.Entry<String, Relation> e : this.relations.entrySet()) {
            Relation r = e.getValue();
            List<Relation> relations = new ArrayList<>();
            relations.add(r);

            Select select = new Select();
            PlainSelect ps = new PlainSelect();
            select.setSelectBody(ps);
            ps.setFromItem(r.getFromItem());

            templates.addAll(this.getTemplatesForRelationsRecursive(select, relations, this.joinLevel));
        }

        Log.info("Done generating " + templates.size() + " templates.");

        int fullQueryCount = 0;
        int noPredProjCount = 0;
        int noPredCount = 0;
        int noCompProjCount = 0;
        int noCompCount = 0;
        int noConstProjCount = 0;
        int noConstCount = 0;
        for (Template template : templates) {
            switch (template.getType()) {
                case FULL_QUERY:
                    fullQueryCount++;
                    break;
                case NO_PRED_PROJ:
                    noPredProjCount++;
                    break;
                case NO_PRED:
                    noPredCount++;
                    break;
                case NO_CONST_OP_PROJ:
                    noCompProjCount++;
                    break;
                case NO_CONST_OP:
                    noCompCount++;
                    break;
                case NO_CONST_PROJ:
                    noConstProjCount++;
                    break;
                case NO_CONST:
                    noConstCount++;
                    break;
            }
        }

        /*
        Log.info("Full Query Count: " + fullQueryCount);
        Log.info("No Predicate/Projection: " + noPredProjCount);
        Log.info("No Predicate: " + noPredCount);
        Log.info("No Comparison/Projection: " + noCompProjCount);
        Log.info("No Comparison: " + noCompCount);
        Log.info("No Constants/Projection: " + noConstProjCount);
        Log.info("No Constants: " + noConstCount);*/

        Log.info("==============================\n");
        return templates;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: <schema-prefix> <join-level> <db-name>");
            System.err.println("Example: SchemaDataTemplateGenerator data/mas/mas 0 mas");
            System.exit(1);
        }

        String prefix = args[0];
        String relationsFile = prefix + ".relations.json";
        String edgesFile = prefix + ".edges.json";

        Integer joinLevel = Integer.valueOf(args[1]);
        String dbName = args[2];

        RDBMS db = null;
        try {
            db = new RDBMS(dbName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        SchemaDataTemplateGenerator tg = new SchemaDataTemplateGenerator(relationsFile, edgesFile, joinLevel, db);
        // Set<Template> templates = tg.generate();

        /*
        String errorFileName = "template_errors.out";

        Log.info("==============================");
        Log.info("Reading templates generated from query log (i.e. test set)...");
        Map<String, Integer> testTemplateCount = new HashMap<>();
        try {
            CSVReader reader = new CSVReader(new FileReader(logTemplatesFile), '\t');
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                String testTmpl = nextLine[1].toLowerCase();
                testTemplateCount.put(testTmpl, Integer.valueOf(nextLine[0]));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.info("Finished reading query log templates.");
        Log.info("==============================\n");

        int covered = 0;
        int total = 0;
        CSVWriter writer;
        try {
            writer = new CSVWriter(new FileWriter(errorFileName), '\t', CSVWriter.NO_QUOTE_CHARACTER);

            for (Map.Entry<String, Integer> e : Utils.sortByValueDesc(testTemplateCount).entrySet()) {
                total += e.getValue();
                if (templates.contains(e.getKey())) {
                    covered += e.getValue();
                } else {
                    String[] row = new String[2];
                    row[0] = e.getValue().toString();
                    row[1] = e.getKey();
                    writer.writeNext(row);
                }
            }


            Log.info("==============================");
            Log.info("Measuring coverage...");
            float predProjCoverage = (float) covered / total * 100;
            Log.info("Number of templates: " + templates.size());
            Log.info(String.format("Coverage: %d / %d (%.1f)", covered, total, predProjCoverage) + "%");
            Log.info("==============================\n");

            Log.info("==============================");
            Log.info("Templates not covered were printed to <" + errorFileName + ">.");

            writer.close();

            Log.info("==============================");

        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        // Test coverage for random string
        // String test = "SELECT journal.homepage FROM journal WHERE journal.name = 'PVLDB'";
        // String test = "SELECT j.homepage FROM journal j WHERE j.name = 'PVLDB'";
        // String test = "SELECT j.homepage FROM journal AS j WHERE j.name = 'PVLDB'";
        // String test = "SELECT homepage FROM journal AS j WHERE journal.name = 'PVLDB'";
        // String test = "SELECT a.name FROM author a, publication p, journal j, writes w WHERE j.name = 'PVLDB' AND a.aid = w.aid AND w.pid = p.pid AND p.jid = j.jid";
        // String test = "SELECT a.name FROM author AS a JOIN writes AS w ON a.aid = w.aid JOIN publication AS p ON w.pid = p.pid JOIN journal AS j ON p.jid = j.jid WHERE j.name = 'PVLDB'";
        // String test = "SELECT a.name FROM author AS a JOIN writes AS w ON a.aid = w.aid JOIN publication AS p ON w.pid = p.pid JOIN journal AS j ON p.jid = j.jid WHERE j.name = 'PVLDB'";
        // String test = "SELECT a.name FROM author AS a JOIN writes AS w ON a.aid = w.aid JOIN publication AS p ON w.pid = p.pid JOIN journal AS j ON p.jid = j.jid WHERE j.name = 'PVLDB' AND (j.name = 'THIS' OR j.name = 'OTHER')";
        String test = "SELECT p.title FROM publication p, writes w, author a, organization o WHERE p.pid = w.pid AND w.aid = a.aid AND a.oid = o.oid AND o.name = 'University of Michigan'";
        // String test = "select publication_0.title from author as author_0, organization as organization_0, publication as publication_0, writes as writes_0 where author_0.aid = writes_0.aid and author_0.oid = organization_0.oid and organization_0.name = 'university of michigan' and publication_0.pid = writes_0.pid";
        try {
            Statement stmt = CCJSqlParserUtil.parse(test);
            System.out.println(stmt);
            System.out.println(TemplateRoot.fullQueryTemplate((Select) stmt));
            System.out.println(TemplateRoot.noConstantTemplate((Select) stmt));
            System.out.println(TemplateRoot.noConstantProjectionTemplate((Select) stmt));
            System.out.println(TemplateRoot.noComparisonTemplate((Select) stmt));
            System.out.println(TemplateRoot.noComparisonProjectionTemplate((Select) stmt));
            System.out.println(TemplateRoot.noPredicateTemplate((Select) stmt));
            System.out.println(TemplateRoot.noPredicateProjectionTemplate((Select) stmt));

            System.out.println(stmt);
        } catch (JSQLParserException e) {
            if (Log.DEBUG) e.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        /*
        String templateOutFile = "templates.out";
        Log.info("==============================");
        Log.info("Printing all templates generated to <" + templateOutFile + ">.");
        try {
            PrintWriter tmplWriter = new PrintWriter(templateOutFile, "UTF-8");
            for (Template tmpl : templates) {
                tmplWriter.println(tmpl);
            }
            tmplWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.info("==============================");*/
    }
}
