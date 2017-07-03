package edu.umich.tbnalir;

import com.esotericsoftware.minlog.Log;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import edu.umich.tbnalir.rdbms.Attribute;
import edu.umich.tbnalir.rdbms.Function;
import edu.umich.tbnalir.rdbms.FunctionParameter;
import edu.umich.tbnalir.rdbms.Relation;
import edu.umich.tbnalir.sql.LiteralExpression;
import edu.umich.tbnalir.util.Constants;
import edu.umich.tbnalir.util.Utils;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.SelectUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by cjbaik on 6/30/17.
 */
public class SchemaTemplateGenerator extends TemplateGenerator {
    static Map<String, Relation> relations = new HashMap<String, Relation>();
    static Map<Attribute, Set<Attribute>> fkpkEdges = new HashMap<Attribute, Set<Attribute>>();

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: <schema-prefix> <query-log-templates-csv>");
            System.err.println("Example: SchemaTemplateGenerator data/sdss/schema/bestdr7 bestdr7_0.05_pred_proj.csv");
            System.exit(1);
        }

        String prefix = args[0];
        String relationsFile = prefix + ".relations.json";
        String edgesFile = prefix + ".edges.json";

        String logTemplatesFile = args[1];

        String errorFileName = "template_errors.out";

        // read in schema from json files
        JSONParser parser = new JSONParser();

        Log.info("==============================");
        try {
            Log.info("Reading relations info...");
            JSONObject rels = (JSONObject) parser.parse(new FileReader(relationsFile));
            for (Object relNameObj : rels.keySet()) {
                String relName = (String) relNameObj;
                JSONObject relInfo = (JSONObject) rels.get(relNameObj);
                JSONObject attrObj = (JSONObject) relInfo.get("attributes");
                Map<String, Attribute> attributeMap = new HashMap<String, Attribute>();
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
                    Map<Integer, FunctionParameter> inputs = new HashMap<Integer, FunctionParameter>();
                    for (Object inputNameObj : inputObj.keySet()) {
                        JSONObject inputInfoObj = (JSONObject) inputObj.get(inputNameObj);
                        String inputName = (String) inputInfoObj.get("name");
                        String inputType = (String) inputInfoObj.get("type");
                        Integer inputIndex = Integer.valueOf((String) inputInfoObj.get("index"));
                        FunctionParameter param = new FunctionParameter(inputName, inputType, inputIndex);
                        inputs.put(inputIndex, param);
                    }

                    Function fn = new Function(relName, (String) relInfo.get("type"), attributeMap, inputs);
                    relations.put(relName, fn);
                } else {
                    Relation rel = new Relation(relName, (String) relInfo.get("type"), attributeMap);
                    relations.put(relName, rel);
                }

            }
            Log.info("Read " + relations.size() + " relations/views/functions.");

            Log.info("Reading edges info...");
            JSONArray edgesArr = (JSONArray) parser.parse(new FileReader(edgesFile));
            for (Object edgeObj : edgesArr) {
                JSONObject edge = (JSONObject) edgeObj;

                Object foreignRelationStr = edge.get("foreignRelation");
                if (foreignRelationStr == null) {
                    Log.error("Foreign relation not included in edge definition.");
                    continue;
                }
                Relation foreignRelation = relations.get(foreignRelationStr);
                if (foreignRelation == null) {
                    Log.error("Could not find relation <" + foreignRelationStr + "> in schema.");
                    continue;
                }
                Object foreignAttributeStr = edge.get("foreignAttribute");
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

                Object primaryRelationStr = edge.get("primaryRelation");
                if (primaryRelationStr == null) {
                    Log.error("Primary relation not included in edge definition.");
                    continue;
                }
                Relation primaryRelation = relations.get(primaryRelationStr);
                if (primaryRelation == null) {
                    Log.error("Could not find relation <" + primaryRelationStr + "> in schema.");
                    continue;
                }
                Object primaryAttributeStr = edge.get("primaryAttribute");
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

                Set<Attribute> pks = fkpkEdges.get(foreignAttribute);
                if (pks == null) {
                    pks = new HashSet<Attribute>();
                    fkpkEdges.put(foreignAttribute, pks);
                }
                pks.add(primaryAttribute);
            }

            int fkpkLength = 0;
            for (Map.Entry<Attribute, Set<Attribute>> e : fkpkEdges.entrySet()) {
                fkpkLength += e.getValue().size();
            }
            Log.info("Read " + fkpkLength + " FK-PK relationships.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.info("==============================\n");

        // read predicate/projection templates from query log
        Log.info("==============================");
        Log.info("Reading templates generated from query log (i.e. test set)...");
        Map<String, Integer> testTemplateCount = new HashMap<String, Integer>();
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


        Log.info("==============================");
        Log.info("Generating templates using schema...");
        Set<String> templates = new HashSet<String>();

        SchemaTemplateGenerator tg = new SchemaTemplateGenerator();

        for (Map.Entry<String, Relation> e : relations.entrySet()) {
            Relation r = e.getValue();

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

            templates.addAll(tg.generateTemplateVariants(tg::noPredicateProjectionTemplate, select));

            // Handling joins
            for (Map.Entry<String, Attribute> attrEntry : r.getAttributes().entrySet()) {
                Set<Attribute> pks = fkpkEdges.get(attrEntry.getValue());
                if (pks != null) {
                    for (Attribute pk : pks) {
                        if (pk != null) {
                            Join join = new Join();

                            if (pk.getRelation() instanceof Function) {
                                TableFunction tFn = Utils.convertFunctionToTableFunction((Function) pk.getRelation());
                                join.setRightItem(tFn);
                            } else {
                                join.setRightItem(new Table(pk.getRelation().toString()));
                            }

                            // For simple joins ("table 1, table 2")
                            join.setSimple(true);

                            List<Join> joins = new ArrayList<Join>();
                            joins.add(join);
                            ps.setJoins(joins);
                            templates.addAll(tg.generateTemplateVariants(tg::noPredicateProjectionTemplate, select));
                        }
                    }
                }
            }
        }

        Log.info("Done generating " + templates.size() + " templates.");
        Log.info("==============================\n");

        CSVWriter writer = null;
        try {
            writer = new CSVWriter(new FileWriter(errorFileName), '\t', CSVWriter.NO_QUOTE_CHARACTER);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int covered = 0;
        int total = 0;
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
        try {
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.info("==============================");

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
