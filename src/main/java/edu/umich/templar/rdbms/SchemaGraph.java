package edu.umich.templar.rdbms;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import com.esotericsoftware.minlog.Log;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class SchemaGraph {
	public static double KeyEdge = 0.99; 
	public static double relEdge = 0.995; 
	public static double AttEdge = 0.995;

	public Map<String, Relation> relations;
	public Map<Attribute, Set<Attribute>> fkpkEdges;
	public Map<Attribute, Set<Attribute>> pkfkEdges;

	public ArrayList<SchemaElement> schemaElements = new ArrayList<SchemaElement>();
	public double [][] weights;  
	public double [][] shortestDistance; 
	public int [][] preElement; 
	
	public static void main(String [] args) throws IOException, ParseException
	{
		SchemaGraph graph = new SchemaGraph("dblp"); 
		graph.printForCheck(); 
	}

    public void loadRelationsFromFile(String relationsFileName) {
        JSONParser parser = new JSONParser();
        List<SchemaElement> schemaRelList = new ArrayList<>();

        Log.info("==============================");
        try {
            Log.info("Reading relations info...");
            JSONObject rels = (JSONObject) parser.parse(new FileReader(relationsFileName));
            for (Object relNameObj : rels.keySet()) {
                String relName = (String) relNameObj;

                JSONObject relInfo = (JSONObject) rels.get(relNameObj);
                JSONObject attrObj = (JSONObject) relInfo.get("attributes");

                // START: stuff for Fei's code to work
                SchemaElement relSchemaEl = new SchemaElement(schemaElements.size(), relName, (String) relInfo.get("type"));
                this.schemaElements.add(relSchemaEl);
                schemaRelList.add(relSchemaEl);
                relSchemaEl.relation = relSchemaEl;  // circular reference, for some strange reason
                // END: stuff for Fei's code to work

                Map<String, Attribute> attributeMap = new HashMap<>();
                Attribute primaryAttr = null;
                Attribute pk = null;
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
                        pk = attr;
                    }

                    if (attrInfo.get("entropy") != null) {
                        attr.setEntropy((Double) attrInfo.get("entropy"));
                    }

                    // START: stuff for Fei's code to work
                    SchemaElement attrSchemaEl = new SchemaElement(schemaElements.size(), attrName, attrType);
                    attrSchemaEl.relation = relSchemaEl;
                    relSchemaEl.attributes.add(attrSchemaEl);
                    this.schemaElements.add(attrSchemaEl);

                    if (attrInfo.get("question") != null) {
                        String question = (String) attrInfo.get("question");
                        attr.setQuestion(question);
                        attrSchemaEl.question = question;
                    }

                    if (attrInfo.get("importance") != null) {
                        relSchemaEl.defaultAttribute = attrSchemaEl;
                        primaryAttr = attr;
                    }

                    if (attr.isPk()) {
                        relSchemaEl.pk = attrSchemaEl;
                        attrSchemaEl.type = "pk";
                    }

                    if (attr.isFk()) {
                        attrSchemaEl.type = "fk";
                    }
                    // END: stuff for Fei's code to work
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

                    rel.setPk(pk);
                    rel.setPrimaryAttr(primaryAttr);

                    if (relInfo.get("join_table") != null) {
                        boolean joinTable = (Boolean) relInfo.get("join_table");
                        rel.setJoinTable(joinTable);
                        relSchemaEl.joinTable = joinTable;
                    }

                    if (relInfo.get("weak") != null) {
                        rel.setWeak((Boolean) relInfo.get("weak"));
                    }

                    if (relInfo.get("parent") != null) {
                        rel.setParent((String) relInfo.get("parent"));
                    }
                }
            }

            // START: stuff for Fei's code to work
            this.weights = new double [this.schemaElements.size()][this.schemaElements.size()];
            for(int i = 0; i < this.weights.length; i++)
            {
                for(int j = 0; j < this.weights.length; j++)
                {
                    this.weights[i][j] = 0;
                }
            }

            for (SchemaElement schemaRel : schemaRelList) {
                for(int j = 0; j < schemaRel.attributes.size(); j++)
                {
                    this.weights[schemaRel.elementID][schemaRel.attributes.get(j).elementID] = AttEdge;
                }
            }
            // END: stuff for Fei's code to work

            Log.info("Read " + this.relations.size() + " relations/views/functions.");
            Log.info("==============================\n");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public void loadEdgesFromFile(String edgesFileName) {

        // TODO: below code not accounted for
        // TODO: need to find and replace "relationship" (join tables) and "entity" (relations) in this file, find a replacement in schema somehow
        /*
        for(int i = 0; i < jsonEdges.size(); i++)
        {
            JSONObject jsonEdge = (JSONObject) jsonEdges.get(i);
            String leftRelName = (String) jsonEdge.get("foreignRelation");
            String leftAttName = (String) jsonEdge.get("foreignAttribute");
            String rightRelName = (String) jsonEdge.get("primaryRelation");

            int fk = this.searchAttribute(leftRelName, leftAttName);
            int pk = this.searchRelation(rightRelName);

            if(this.schemaElements.get(fk).relation.type.equals("relationship"))
            {
                weights[fk][pk] = relEdge;
            }
            else
            {
                weights[fk][pk] = KeyEdge;
            }
            schemaElements.get(pk).inElements.add(schemaElements.get(fk));
        }*/

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

                // START: stuff for Fei's code to work
                int fk = this.searchAttribute(foreignRelationStr, foreignAttributeStr);
                int pk = this.searchRelation(primaryRelationStr);

                // if(this.schemaElements.get(fk).relation.type.equals("relationship"))
                if (foreignRelation.isJoinTable()) {
                    weights[fk][pk] = relEdge;
                } else {
                    weights[fk][pk] = KeyEdge;
                }
                this.schemaElements.get(pk).inElements.add(this.schemaElements.get(fk));
                // END: stuff for Fei's code to work
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

	public SchemaGraph(String prefix) throws IOException, ParseException
	{
        String relationsFile = prefix + ".relations.json";
        String edgesFile = prefix + ".edges.json";

        this.relations = new HashMap<>();
        this.fkpkEdges = new HashMap<>();
        this.pkfkEdges = new HashMap<>();
        this.loadRelationsFromFile(relationsFile);
        this.loadEdgesFromFile(edgesFile);

        // TODO: commenting out. perhaps we don't need?
        // shortestDistanceCompute();
	}
	
	public void shortestDistanceCompute()
	{
		shortestDistance = new double[weights.length][weights.length]; 
		preElement = new int [weights.length][weights.length]; 

		for(int i = 0; i < weights.length; i++)
		{
			for(int j = 0; j < weights.length; j++)
			{
				if(weights[i][j] > weights[j][i])
				{
					weights[j][i] = weights[i][j]; 
				}
			}
			weights[i][i] = 1; 
		}

		for(int i = 0; i < weights.length; i++)
		{
			for(int j = 0; j < weights.length; j++)
			{
				shortestDistance[i][j] = weights[i][j]; 
			}
		}
		
		for(int i = 0; i < weights.length; i++)
		{
			dijkstra(i); 
		}
	}
	
	public void dijkstra(int source)
	{
		double [] localDistance = new double [schemaElements.size()]; 
		for(int i = 0; i < localDistance.length; i++)
		{
			localDistance[i] = weights[source][i]; 
		}
		
		for(int i = 0; i < preElement.length; i++)
		{
			preElement[source][i] = source; 
		}

		boolean [] dealt = new boolean[schemaElements.size()]; 
		for(int i = 0; i < dealt.length; i++)
		{
			dealt[i] = false; 
		}
		dealt[source] = true; 
		
		boolean finished = false;
		while(finished == false)
		{
			double maxDistance = 0; 
			int maxOrder = -1; 
			for(int i = 0; i < weights.length; i++)
			{
				if(dealt[i] == false && localDistance[i] > maxDistance)
				{
					maxDistance = localDistance[i]; 
					maxOrder = i; 
				}
			}
			
			dealt[maxOrder] = true; 
			for(int i = 0; i < weights.length; i++)
			{
				if(dealt[i] == false && localDistance[maxOrder]*weights[maxOrder][i] > localDistance[i])
				{
					localDistance[i] = localDistance[maxOrder]*weights[maxOrder][i]; 
					preElement[source][i] = maxOrder; 
				}				
			}			
			
			finished = true; 
			for(int i = 0; i < dealt.length; i++)
			{
				if(dealt[i] == false)
				{
					finished = false; 
				}
			}
		}
		
		for(int i = 0; i < localDistance.length; i++)
		{
			shortestDistance[source][i] = localDistance[i]; 
		}
	}
	
	public ArrayList<Edge> getJoinPath(SchemaElement left, SchemaElement right)
	{
		ArrayList<Edge> edges = new ArrayList<Edge>(); 
		int pre = right.elementID; 
		int cur = right.elementID; 
		
		while(schemaElements.get(cur).relation.elementID != left.relation.elementID)
		{
			pre = preElement[left.elementID][cur]; 
			if(schemaElements.get(cur).relation.elementID != schemaElements.get(pre).relation.elementID)
			{
				edges.add(new Edge(schemaElements.get(cur), schemaElements.get(pre))); 
			}
			cur = pre; 
		}
		
		return edges; 
	}
	
	public double distance(SchemaElement source, SchemaElement distination)
	{
		return shortestDistance[source.elementID][distination.elementID]; 
	}
	
	public ArrayList<SchemaElement> getNeighbors(SchemaElement element, String typeList)
	{
		String [] types = typeList.split(" "); 
		ArrayList<SchemaElement> neighbors = new ArrayList<SchemaElement>(); 
		for(int i = 0; i < schemaElements.size(); i++)
		{
			if(weights[element.elementID][i] > 0)
			{
				for(int j = 0; j < types.length; j++)
				{
					if(schemaElements.get(i).type.equals(types[j]))
					{
						neighbors.add(schemaElements.get(i)); 
					}
				}
			}
		}
		for(int i = 0; i < schemaElements.size(); i++)
		{
			if(weights[i][element.elementID] > 0)
			{
				for(int j = 0; j < types.length; j++)
				{
					if(schemaElements.get(i).type.equals(types[j]))
					{
						neighbors.add(schemaElements.get(i)); 
					}
				}
			}
		}
		
		return neighbors; 
	}
	
	public ArrayList<SchemaElement> getElementsByType(String typeList)
	{
		String [] types = typeList.split(" "); 
		ArrayList<SchemaElement> relations = new ArrayList<SchemaElement>(); 
		for(int i = 0; i < schemaElements.size(); i++)
		{
			for(int j = 0; j < types.length; j++)
			{
				if((schemaElements.get(i).type.equals(types[j])))
				{
					relations.add(schemaElements.get(i)); 
				}
			}
		}
		
		return relations; 
	}
	
	public int searchRelation(String relation_name)
	{
		for(int i = 0; i < schemaElements.size(); i++)
		{

//			if((schemaElements.get(i).type.equals("entity") || schemaElements.get(i).type.equals("relationship"))
//				&& schemaElements.get(i).name.equals(relation_name))
			if (schemaElements.get(i).type.equals("relation")
				&& schemaElements.get(i).name.equals(relation_name)) {
				return i; 
			}
		}
		
		return -1; 
	}

	public int searchAttribute(String relation_name, String attribute_name)
	{
		for(int i = 0; i < schemaElements.size(); i++)
		{
//			if((schemaElements.get(i).type.equals("entity") || schemaElements.get(i).type.equals("relationship"))
//				&& schemaElements.get(i).name.equals(relation_name))
            if (schemaElements.get(i).type.equals("relation")
                    && schemaElements.get(i).name.equals(relation_name)) {
				for(int j = i+1; j < schemaElements.size(); j++)
				{
					if(schemaElements.get(j).name.equals(attribute_name))
					{
						return j; 
					}
				}
			}
		}
		
		return -1; 
	}
	
	public void printForCheck()
	{
		ArrayList<SchemaElement> entities = this.schemaElements; 
		for(int i = 0; i < entities.size(); i++)
		{
//			if(entities.get(i).type.equals("entity") || entities.get(i).type.equals("relationship"))
			if(entities.get(i).type.equals("relation")) {
				System.out.print(i + ": " + entities.get(i).relation.name + "." + entities.get(i).name + ": "); 
				for(int j = 0; j < entities.get(i).attributes.size(); j++)
				{
					System.out.print(entities.get(i).attributes.get(j).name + " "); 
				}
				System.out.println(); 
			}
		}
		
//		System.out.println(); 
//		
//		for(int i = 0; i < shortestDistance.length; i++)
//		{
//			for(int j = 0; j < shortestDistance.length; j++)
//			{
//				System.out.print((double)Math.round(shortestDistance[i][j]*1000)/1000 + "\t"); 
//			}
//			System.out.println(); 
//		}
	}
}
