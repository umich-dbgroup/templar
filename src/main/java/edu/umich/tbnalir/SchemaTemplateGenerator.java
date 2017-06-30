package edu.umich.tbnalir;

import edu.umich.tbnalir.rdbms.Attribute;
import edu.umich.tbnalir.rdbms.Relation;
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
            JSONObject rels = (JSONObject) parser.parse(new FileReader(relationsFile));
            for (Object relNameObj : rels.keySet()) {
                String relName = (String) relNameObj;
                JSONObject relInfo = (JSONObject) rels.get(relNameObj);
                JSONArray attrArray = (JSONArray) relInfo.get("attributes");
                Map<String, Attribute> attributeMap = new HashMap<String, Attribute>();
                for (Object attrInfoObj : attrArray) {
                    JSONObject attrInfo = (JSONObject) attrInfoObj;
                    String attrName = (String) attrInfo.get("name");
                    String attrType = (String) attrInfo.get("type");
                    Attribute attr = new Attribute(attrName, attrType);
                    attributeMap.put(attrName, attr);
                }

                Relation rel = new Relation(relName, (String) relInfo.get("type"), attributeMap);
                relations.put(relName, rel);
            }

            // TODO: READ EDGES

        } catch (Exception e) {
            e.printStackTrace();
        }


        // TODO: construct predicate/projection templates

        // TODO: generate predicate/projection templates from query log

        // TODO: measure coverage
    }
}
