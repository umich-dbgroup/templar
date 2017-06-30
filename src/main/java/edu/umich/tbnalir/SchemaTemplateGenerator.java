package edu.umich.tbnalir;

import com.esotericsoftware.minlog.Log;
import com.opencsv.CSVReader;
import edu.umich.tbnalir.rdbms.Attribute;
import edu.umich.tbnalir.rdbms.Function;
import edu.umich.tbnalir.rdbms.FunctionParameter;
import edu.umich.tbnalir.rdbms.Relation;
import edu.umich.tbnalir.sql.LiteralExpression;
import edu.umich.tbnalir.util.Constants;
import edu.umich.tbnalir.util.Utils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.TableFunction;
import net.sf.jsqlparser.statement.select.Top;
import net.sf.jsqlparser.util.SelectUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.*;

/**
 * Created by cjbaik on 6/30/17.
 */
public class SchemaTemplateGenerator extends TemplateGenerator {
    static Map<String, Relation> relations = new HashMap<String, Relation>();
    static Map<Attribute, Attribute> fkpkEdges = new HashMap<Attribute, Attribute>();

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

                Relation foreignRelation = relations.get(edge.get("foreignRelation"));
                Attribute foreignAttribute = foreignRelation.getAttributes().get(edge.get("foreignAttribute"));

                Relation primaryRelation = relations.get(edge.get("primaryRelation"));
                Attribute primaryAttribute = primaryRelation.getAttributes().get(edge.get("primaryAttribute"));

                fkpkEdges.put(foreignAttribute, primaryAttribute);
            }
            Log.info("Read " + fkpkEdges.size() + " FK-PK relationships.");
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

        Log.info("Generating level 0 (no joins, single tables only)...");
        for (Map.Entry<String, Relation> e : relations.entrySet()) {
            Relation r = e.getValue();

            Select select;
            if (r instanceof Function) {
                net.sf.jsqlparser.expression.Function fn = new net.sf.jsqlparser.expression.Function();
                fn.setName(r.getName());

                ExpressionList expList = new ExpressionList();
                List<Expression> expListInternal = new ArrayList<Expression>();
                Function mFunc = (Function) r;
                for (int i = 1; i < mFunc.getInputs().size() + 1; i++) {
                    FunctionParameter param = mFunc.getInputs().get(i);
                    if (param == null) continue;
                    expListInternal.add(new LiteralExpression(Utils.convertSQLTypetoConstant(param.getType())));
                }
                expList.setExpressions(expListInternal);
                fn.setParameters(expList);

                TableFunction tFn = new TableFunction();
                tFn.setFunction(fn);

                select = new Select();
                PlainSelect ps = new PlainSelect();
                ps.setFromItem(tFn);
                select.setSelectBody(ps);
            } else {
                Table table = new Table(r.toString());
                select = SelectUtils.buildSelectFromTable(table);
            }

            PlainSelect ps = (PlainSelect) select.getSelectBody();
            ps.setWhere(new LiteralExpression(Constants.PRED));

            // Add vanilla version
            String vanillaTmpl = tg.noPredicateProjectionTemplate(select);
            templates.add(vanillaTmpl);

            // Add top version
            Top top = new Top();
            top.setExpression(new LiteralExpression(Constants.TOP));
            ps.setTop(top);

            String topTmpl = tg.noPredicateProjectionTemplate(select);
            templates.add(topTmpl);
        }

        int covered = 0;
        int total = 0;
        for (Map.Entry<String, Integer> e : testTemplateCount.entrySet()) {
            total += e.getValue();
            if (templates.contains(e.getKey())) {
                covered += e.getValue();
            }
        }
        Log.info("Done generating level 0: " + templates.size() + " templates.");
        Log.info("==============================\n");

        Log.info("==============================");
        Log.info("Measuring coverage...");
        float predProjCoverage = (float) covered / total * 100;
        Log.info("Number of templates: " + templates.size());
        Log.info(String.format("Coverage: %d / %d (%.1f)", covered, total, predProjCoverage) + "%");
        Log.info("==============================");
    }
}
