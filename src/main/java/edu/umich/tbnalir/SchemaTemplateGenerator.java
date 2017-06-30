package edu.umich.tbnalir;

import com.esotericsoftware.minlog.Log;
import edu.umich.tbnalir.rdbms.Attribute;
import edu.umich.tbnalir.rdbms.Function;
import edu.umich.tbnalir.rdbms.FunctionParameter;
import edu.umich.tbnalir.rdbms.Relation;
import edu.umich.tbnalir.util.Constants;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.UserVariable;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.util.SelectUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by cjbaik on 6/30/17.
 */
public class SchemaTemplateGenerator {
    static Map<String, Relation> relations = new HashMap<String, Relation>();
    static Map<Attribute, Attribute> fkpkEdges = new HashMap<Attribute, Attribute>();

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: <json-input-prefix>");
            System.err.println("Example: SchemaTemplateGenerator data/sdss/schema/bestdr7");
            System.exit(1);
        }

        String prefix = args[0];
        String relationsFile = prefix + ".relations.json";
        String edgesFile = prefix + ".edges.json";

        // read in schema from json files
        JSONParser parser = new JSONParser();

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

        // TODO: construct predicate/projection templates

        // For level 0 (no joins)
        for (Map.Entry<String, Relation> e : relations.entrySet()) {
            Relation r = e.getValue();

            StringBuffer sb = new StringBuffer();
            sb.append("SELECT ");
            sb.append(Constants.PROJ);
            sb.append(" FROM ");
            sb.append(r);
            sb.append(" WHERE ");
            sb.append(Constants.PRED);
        }

        // TODO: read predicate/projection templates from query log

        // TODO: measure coverage
    }
}
