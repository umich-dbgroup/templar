package edu.umich.tbnalir;

import com.esotericsoftware.minlog.Log;
import edu.umich.tbnalir.rdbms.*;
import edu.umich.tbnalir.template.TemplateFunctions;
import edu.umich.tbnalir.util.Utils;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.SelectUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.io.PrintWriter;
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

    public SchemaDataTemplateGenerator(String relationsFile, String edgesFile, int joinLevel, RDBMS db) {
        this.joinLevel = joinLevel;
        this.relations = new HashMap<>();
        this.fkpkEdges = new HashMap<>();
        this.pkfkEdges = new HashMap<>();
        this.db = db;

        this.loadRelationsFromFile(relationsFile);
        this.loadEdgesFromFile(edgesFile);
    }

    public Map<String, Relation> getRelations() {
        return relations;
    }

    public Map<Attribute, Set<Attribute>> getFkpkEdges() {
        return fkpkEdges;
    }

    public Map<Attribute, Set<Attribute>> getPkfkEdges() {
        return pkfkEdges;
    }

    public Join createSimpleJoinFromAttribute(Attribute attr) {
        Join join = new Join();

        if (attr.getRelation() instanceof Function) {
            TableFunction tFn = Utils.convertFunctionToTableFunction((Function) attr.getRelation());
            join.setRightItem(tFn);
        } else {
            join.setRightItem(new Table(attr.getRelation().toString()));
        }

        // For simple joins (i.e. of the form "table 1, table 2")
        join.setSimple(true);

        return join;
    }

    public void addPKFKJoinTemplates(Set<String> templates, Relation rel, Select select, PlainSelect ps) {
        for (Map.Entry<String, Attribute> attrEntry : rel.getAttributes().entrySet()) {
            Set<Attribute> fks = this.pkfkEdges.get(attrEntry.getValue());

            if (fks != null) {
                for (Attribute fk : fks) {
                    if (fk != null) {
                        Join join = createSimpleJoinFromAttribute(fk);

                        List<Join> joins = ps.getJoins();
                        if (joins == null) {
                            joins = new ArrayList<>();
                            ps.setJoins(joins);
                        }
                        joins.add(join);

                        templates.addAll(TemplateFunctions.generateTemplateVariants(TemplateFunctions::noPredicateProjectionTemplate, select));

                        // Remove last join to reset to original state
                        joins.remove(joins.size() - 1);
                    }
                }
            }
        }
    }

    public void addFKPKJoinTemplatesRecursive(Set<String> templates, Relation rel, Select select,
                                              PlainSelect ps, int joinLevelsLeft) {
        if (joinLevelsLeft == 0) return;

        for (Map.Entry<String, Attribute> attrEntry : rel.getAttributes().entrySet()) {
            Set<Attribute> pks = fkpkEdges.get(attrEntry.getValue());
            if (pks != null) {
                for (Attribute pk : pks) {
                    if (pk != null) {
                        Join firstJoin = this.createSimpleJoinFromAttribute(pk);

                        List<Join> joins = ps.getJoins();
                        if (joins == null) {
                            joins = new ArrayList<>();
                            ps.setJoins(joins);
                        }
                        joins.add(firstJoin);

                        templates.addAll(TemplateFunctions.generateTemplateVariants(TemplateFunctions::noPredicateProjectionTemplate, select));

                        // Next level joins
                        if (joinLevelsLeft > 1) {
                            FromItem fromItem = firstJoin.getRightItem();

                            Relation nextJoinRelation = this.relations.get(fromItem.toString());
                            if (nextJoinRelation == null) {
                                throw new RuntimeException("Could not find item <" + fromItem.toString() + ">.");
                            }

                            // PK from the second table (FK -> PK <- FK)
                            this.addPKFKJoinTemplates(templates, nextJoinRelation, select, ps);

                            // FK from the second table (FK -> PK/FK -> PK)
                            this.addFKPKJoinTemplatesRecursive(templates, nextJoinRelation, select, ps, joinLevelsLeft - 1);
                        }

                        // Remove the last join to reset state
                        joins.remove(joins.size() - 1);
                    }
                }
            }
        }
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

    public Set<String> getTemplatesForRelation(Relation r) {
        Set<String> templates = new HashSet<>();

        Select select;
        if (r instanceof Function) {
            TableFunction tFn = Utils.convertFunctionToTableFunction((Function) r);
            select = new Select();
            PlainSelect ps = new PlainSelect();
            ps.setFromItem(tFn);
            select.setSelectBody(ps);
        } else {
            Table table = new Table(r.toString());
            select = SelectUtils.buildSelectFromTable(table);
        }

        PlainSelect ps = (PlainSelect) select.getSelectBody();

        // TODO: Generate predicate/projection templates - move this to Template static fn?
        templates.addAll(TemplateFunctions.generateTemplateVariants(TemplateFunctions::noPredicateProjectionTemplate, select));

        // TODO: Use data distribution (cardinality of attributes, perhaps?) to hypothesize projections, predicates (attributes, ops, constants)

        // Handle joins
        this.addFKPKJoinTemplatesRecursive(templates, r, select, ps, this.joinLevel);

        return templates;
    }

    public Set<String> generate() {
        Set<String> templates = new HashSet<>();

        Log.info("==============================");
        Log.info("Generating templates using schema for join level: " + this.joinLevel);

        for (Map.Entry<String, Relation> e : this.relations.entrySet()) {
            templates.addAll(this.getTemplatesForRelation(e.getValue()));
        }

        Log.info("Done generating " + templates.size() + " templates.");
        Log.info("==============================\n");
        return templates;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: <schema-prefix> <join-level> <db-name>");
            System.err.println("Example: SchemaDataTemplateGenerator data/mas/schema/mas 0 mas");
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
        Set<String> templates = tg.generate();

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

        String templateOutFile = "templates.out";
        Log.info("==============================");
        Log.info("Printing all templates generated to <" + templateOutFile + ">.");
        try {
            PrintWriter tmplWriter = new PrintWriter(templateOutFile, "UTF-8");
            for (String tmpl : templates) {
                tmplWriter.println(tmpl);
            }
            tmplWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.info("==============================");
    }
}
