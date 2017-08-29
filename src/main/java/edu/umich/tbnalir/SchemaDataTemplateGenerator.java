package edu.umich.tbnalir;

import com.esotericsoftware.minlog.Log;
import edu.umich.tbnalir.rdbms.*;
import edu.umich.tbnalir.template.Template;
import edu.umich.tbnalir.template.TemplateRoot;
import edu.umich.tbnalir.util.Utils;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
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
        TemplateRoot.relations = this.relations; // Kind of a hack, but want to store globally for TemplateRoot fns

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

    public void addPKFKJoinTemplates(Set<Template> templates, Relation rel, TemplateRoot tr) {
        Select select = tr.getSelect();
        PlainSelect ps = (PlainSelect) select.getSelectBody();

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

                        templates.addAll(tr.generateTemplates(TemplateRoot::noPredicateProjectionTemplate));

                        // Remove last join to reset to original state
                        joins.remove(joins.size() - 1);
                    }
                }
            }
        }
    }

    public void addFKPKJoinTemplatesRecursive(Set<Template> templates, Relation rel, TemplateRoot tr, int joinLevelsLeft) {
        if (joinLevelsLeft == 0) return;

        Select select = tr.getSelect();
        PlainSelect ps = (PlainSelect) select.getSelectBody();

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

                        templates.addAll(tr.generateTemplates(TemplateRoot::noPredicateProjectionTemplate));

                        // Next level joins
                        if (joinLevelsLeft > 1) {
                            FromItem fromItem = firstJoin.getRightItem();

                            Relation nextJoinRelation = this.relations.get(fromItem.toString());
                            if (nextJoinRelation == null) {
                                throw new RuntimeException("Could not find item <" + fromItem.toString() + ">.");
                            }

                            // PK from the second table (FK -> PK <- FK)
                            this.addPKFKJoinTemplates(templates, nextJoinRelation, tr);

                            // FK from the second table (FK -> PK/FK -> PK)
                            this.addFKPKJoinTemplatesRecursive(templates, nextJoinRelation, tr, joinLevelsLeft - 1);
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

    public Set<Template> getTemplatesForRelation(Relation r) {
        Set<Template> templates = new HashSet<>();

        Select select;
        if (r instanceof Function) {
            TableFunction tFn = Utils.convertFunctionToTableFunction((Function) r);
            select = new Select();
            PlainSelect ps = new PlainSelect();
            ps.setFromItem(tFn);
            select.setSelectBody(ps);
        } else {
            select = SelectUtils.buildSelectFromTable(r.getTable());
        }

        TemplateRoot tr = new TemplateRoot(select);

        templates.addAll(tr.generateTemplates(TemplateRoot::noPredicateProjectionTemplate));

        // TODO: what to do in case that r is instanceof Function?

        // Rank attributes by entropy first (will be reused)
        List<Attribute> attributes = r.rankAttributesByEntropy(this.db);

        // TODO: exclude PK/FK from ranked attributes?

        // hypothesize predicate templates (guess attributes in projections)
        // naive: get power set of all attributes
        Set<Set<Attribute>> projections = Utils.powerSet(attributes);

        PlainSelect ps = (PlainSelect) tr.getSelect().getSelectBody();

        for (Set<Attribute> attrSet : projections) {
            if (attrSet.size() == 0) continue;

            List<SelectItem> selectItems = new ArrayList<>();

            for (Attribute attr : attrSet) {
                SelectExpressionItem item = new SelectExpressionItem();
                item.setExpression(attr.getColumn());
                selectItems.add(item);
            }

            ps.setSelectItems(selectItems);
            templates.addAll(tr.generateTemplates(TemplateRoot::noPredicateTemplate));
        }

        ps.setSelectItems(null); // reset select object

        // TODO: for each hypothesized comparison/projection template (guess attributes used in predicates)
        // TODO: for each hypothesized comparison template (guess attributes and projections used)
        // TODO: for each hypothesized constant/projection template (guess attributes and operators)
        // TODO: for each hypothesized constant template (guess attributes, operators, and projections)
        // TODO: for each hypothesized full query template (guess attributes, operators, constants, and projections)

        // Handle joins
        // this.addFKPKJoinTemplatesRecursive(templates, r, tr, this.joinLevel);

        return templates;
    }

    public Set<Template> generate() {
        Set<Template> templates = new HashSet<>();

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
        Set<Template> templates = tg.generate();

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
        /*
        try {
            Statement stmt = CCJSqlParserUtil.parse(test);
            System.out.println(TemplateRoot.fullQueryTemplate((Select) stmt));
            System.out.println(TemplateRoot.noConstantTemplate((Select) stmt));
            System.out.println(TemplateRoot.noConstantProjectionTemplate((Select) stmt));
            System.out.println(TemplateRoot.noComparisonTemplate((Select) stmt));
            System.out.println(TemplateRoot.noComparisonProjectionTemplate((Select) stmt));
            System.out.println(TemplateRoot.noPredicateTemplate((Select) stmt));
            System.out.println(TemplateRoot.noPredicateProjectionTemplate((Select) stmt));
        } catch (JSQLParserException e) {
            if (Log.DEBUG) e.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }*/

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
        Log.info("==============================");
    }
}
