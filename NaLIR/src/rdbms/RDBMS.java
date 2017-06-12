package rdbms;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import dataStructure.BParseTreeNode;
import edu.northwestern.at.morphadorner.corpuslinguistics.lemmatizer.EnglishLemmatizer;
import tools.Functions;

public class RDBMS 
{
	Connection conn; 
	EnglishLemmatizer lemmatizer; 
	public ArrayList<String> history = new ArrayList<String>(); 

	public String databaseName; 
	ArrayList<Relation> relations = new ArrayList<Relation>();
	ArrayList<Edge> edges = new ArrayList<Edge>(); 
	public SchemaGraph schemaGraph = new SchemaGraph(); 
	public AttributeGraph attributeGraph = new AttributeGraph(); 

	public static void main(String [] args) throws Exception
	{
		new EnglishLemmatizer();
		RDBMS db = new RDBMS("mas", null); 
		db.attributeGraph.printForCheck(); 
	}
	
	public RDBMS(String databaseName, EnglishLemmatizer lemmatizer) throws Exception
	{
		this.databaseName = databaseName; 
		String driver = "com.mysql.jdbc.Driver"; // connect to the database; 
		String url = "jdbc:mysql://127.0.0.1:3306/";
		String user = "root";
		Class.forName(driver);
		conn = DriverManager.getConnection(url, user, "");
		this.lemmatizer = lemmatizer; 

		this.loadHistory(databaseName); // 0.3 load history from rdbms; 
		this.loadSchema(databaseName); // 0.1 load schema from txt;
		this.loadGraph(databaseName); // 0.2 load schema graph from txt; 
	}
	
	public void loadHistory(String databaseName) throws Exception
	{
		Statement statement = conn.createStatement(); 
		String loadSQL = "SELECT content FROM " + databaseName + ".history;"; 
		ResultSet resultSet = statement.executeQuery(loadSQL); 
		while(resultSet.next())
		{
			history.add(resultSet.getString(1)); 
		}
	}
	
	public boolean loadSchema(String databaseName) throws Exception // 0.1 load schema from json; 
	{
		JSONParser parser = new JSONParser();
		JSONArray jsonRelations = (JSONArray)parser.parse(Functions.readFile("/Users/feili/Dropbox/workspace/NaLIR/src/zfiles/" + databaseName + "Relations.json")); 
		for(int i = 0; i < jsonRelations.size(); i++)
		{
			JSONObject jsonRelation = (JSONObject) jsonRelations.get(i); 
			Relation relation = new Relation(jsonRelation, i); 
			relations.add(relation); 
		}
		
		if(relations.size() > 0)
		{
			return true; 
		}
		else
		{
			return false; 
		}
	}
	
	public boolean loadGraph(String databaseName) throws Exception // 0.2 load schema graph from txt; 
	{
		JSONParser parser = new JSONParser();
		JSONArray jsonEdges = (JSONArray)parser.parse(Functions.readFile("/Users/feili/Dropbox/workspace/NaLIR/src/zfiles/" + databaseName + "Edges.json")); 

		for(int i = 0; i < jsonEdges.size(); i++)
		{
			JSONObject jsonEdge = (JSONObject) jsonEdges.get(i); 
			String leftRelName = (String) jsonEdge.get("foreignRelation"); 
			String leftAttName = (String) jsonEdge.get("foreignAttribute"); 
			String rightRelName = (String) jsonEdge.get("primaryRelation"); 
			String rightAttName = (String) jsonEdge.get("primaryAttribute"); 
			
			Relation leftRelation = this.findRelationByName(leftRelName); 
			Attribute leftAttribute = leftRelation.findAttributeByName(leftAttName); 
			Relation rightRelation = this.findRelationByName(rightRelName); 
			Attribute rightAttribute = rightRelation.findAttributeByName(rightAttName); 

			Edge edge = new Edge(new SchemaElement(leftAttribute), new SchemaElement(rightAttribute)); 
			edges.add(edge); 
		}
		schemaGraph.buildGraph(relations, edges); 
		attributeGraph.buildGraph(relations, edges); 

		if(schemaGraph.edges.isEmpty())
		{
			return false; 
		}
		else
		{
			return true; 
		}
	}
	
	public boolean isSchemaExist(String tag, ArrayList<SchemaElement> candidateElements) 
	{
		while(candidateElements.size() > 0)
		{
			candidateElements.remove(0); 
		}
		for(int i = 0; i < relations.size(); i++)
		{
			relations.get(i).isSchemaExist(tag, candidateElements, lemmatizer); 
		}
		if(!candidateElements.isEmpty())
		{
			return true; 
		}
		else
		{
			return false;
		}
	}
		
	public boolean isValueExist(String operator, String value, ArrayList<SchemaElement> candidateElements) throws SQLException, ClassNotFoundException 
	{
		while(candidateElements.size() > 0)
		{
			candidateElements.remove(0); 
		}

		Statement statement = conn.createStatement(); 
		String query = ""; 
		
		if(Functions.isNumeric(value))
		{
			if(operator.equals(">=") || operator.equals(">"))
			{
				query = "SELECT number, relation_name, attribute_name FROM " + databaseName + ".index_number WHERE number " + operator + " " + value 
						+ " AND " + "number " + "<" + " " + Integer.parseInt(value)*1.5 + " GROUP BY relation_name, attribute_name; "; 
			}
			else if(operator.equals("<=") || operator.equals("<"))
			{
				query = "SELECT number, relation_name, attribute_name FROM " + databaseName + ".index_number WHERE number " + operator + " " + value 
						+ " AND " + "number " + ">" + " " + Integer.parseInt(value)*0.75 + " GROUP BY relation_name, attribute_name; "; 
			}
			else if(operator.equals("="))
			{
				query = "SELECT number, relation_name, attribute_name FROM " + databaseName + ".index_number WHERE number " + ">" + " " + Integer.parseInt(value)*0.75 
					+ " AND " + "number " + "<" + " " + Integer.parseInt(value)*1.5 + " GROUP BY relation_name, attribute_name; "; 
			}
			else 
			{
				query = "SELECT number, relation_name, attribute_name FROM " + databaseName + ".index_number WHERE number " + operator + " " + value 
						+ " GROUP BY relation_name, attribute_name; "; 		
			}
		}
		else if(!Functions.isNumeric(value))
		{
			query = "SELECT DISTINCT string, relation_name, attribute_name from " + databaseName + ".index_string where string LIKE \"%" + value + "%\"" 
				+ " GROUP BY relation_name, attribute_name; "; 
		}
		
		ResultSet results = statement.executeQuery(query); 
		while(results.next())
		{
			Relation relation = findRelationByName(results.getString(2)); 
			Attribute attribute = relation.findAttributeByName(results.getString(3)); 
			SchemaElement schemaElement = new SchemaElement("value", attribute, results.getString(1)); 
			candidateElements.add(schemaElement); 
		}
		
		if(!Functions.isNumeric(value))
		{
			for(int i = 0; i < candidateElements.size(); i++)
			{
				candidateElements.get(i).score = Functions.pqSim(value, candidateElements.get(i).example); 
			}
			
			for(int i = 0; i < candidateElements.size(); i++)
			{
				for(int j = i+1; j < candidateElements.size(); j++)
				{
					if(candidateElements.get(i).score < candidateElements.get(j).score)
					{
						SchemaElement element = candidateElements.get(i); 
						candidateElements.set(i, candidateElements.get(j)); 
						candidateElements.set(j, element); 
					}
				}
			}
		}
		else
		{
			for(int i = 0; i < candidateElements.size(); i++)
			{
				candidateElements.get(i).score = 1; 
			}
		}
				
		if(!candidateElements.isEmpty())
		{
			return true; 
		}
		return false;
	}
	
	public void specificValue(ArrayList<BParseTreeNode> VTNodes) throws SQLException
	{
		Statement statement = conn.createStatement(); 
		ResultSet results; 
		
    	for(int i = 0; i < VTNodes.size(); i++)
    	{
    		BParseTreeNode VT = VTNodes.get(i); 
    		if(!Functions.isNumeric(VT.label))
    		{
    			if(!VT.candidates.isEmpty())
    			{
    				SchemaElement element = VT.candidates.get(0); 
    				String specificQuery = "SELECT DISTINCT " + databaseName + "." + element.attribute.relation.relName + "." + element.attribute.attName
    					+ " FROM " + databaseName + "." + element.attribute.relation.relName
    					+ " WHERE " + databaseName + "." + element.attribute.relation.relName + "." + element.attribute.attName + " LIKE \"%" + VT.label + "%\";"; 
    				results = statement.executeQuery(specificQuery); 
    				
    				for(int j = 0; j < 10 && results.next(); j++)
    				{
    					element.mappedValues.add(results.getString(1)); 
    				}
    	    		if(VT.candidates.get(0).mappedValues.size() > 1)
    	    		{
    	    			VT.candidates.get(0).choice = -1; 
    	    		}
    			}
    		}
    	}
	}
	
	public int distance(SchemaElement left, SchemaElement right)
	{
		return this.attributeGraph.shortestDistance[this.attributeGraph.searchNode(left)][this.attributeGraph.searchNode(right)]; 
	}
	
	public void setMapping(BParseTreeNode node, int choice) throws SQLException
	{
		node.choice = choice; 
		SchemaElement element = node.candidates.get(choice); 

		String specificQuery = "SELECT DISTINCT " + databaseName + "." + element.attribute.relation.relName + "." + element.attribute.attName
				+ " FROM " + databaseName + "." + element.attribute.relation.relName
				+ " WHERE " + databaseName + "." + element.attribute.relation.relName + "." + element.attribute.attName + " LIKE \"%" + node.label + "%\";"; 

		Statement statement = conn.createStatement(); 
		ResultSet results = statement.executeQuery(specificQuery); 
			
		element.mappedValues.clear(); 
		for(int i = 0; i < 10 && results.next(); i++)
		{
			element.mappedValues.add(results.getString(1)); 
		}
		if(element.mappedValues.size() > 1)
		{
			element.choice = -1; 
		}
	}
	
	public ArrayList<ArrayList<String>> conductSQL(String query)
	{
		ArrayList<ArrayList<String>> finalResults = new ArrayList<ArrayList<String>>(); 
		try
		{
			Statement statement = conn.createStatement(); 
			statement.execute("use " + this.databaseName); 

			ResultSet result = statement.executeQuery(query);
			while(result.next())
			{
				int columnSize = result.getMetaData().getColumnCount(); 
				ArrayList<String> row = new ArrayList<String>(); 
				for(int i = 0; i < columnSize; i++)
				{
					row.add(result.getString(i+1)); 
				}
				finalResults.add(row); 
			}
			
			return finalResults; 
		} catch(Exception e)
		{
			return new ArrayList<ArrayList<String>>(); 
		}
	}
	
	public String printFinalResults(ArrayList<ArrayList<String>> finalResults)
	{
		String result = ""; 
		for(int i = 0; i < finalResults.size(); i++)
		{
			for(int j = 0; j < finalResults.get(i).size(); j++)
			{
				result += finalResults.get(i).get(j) + "| \t"; 
			}
			result += "\n"; 
		}
		
		return result; 
	}
	
	public Relation findRelationByName(String relationName)
	{
		for(int i = 0; i < relations.size(); i++)
		{
			if(relations.get(i).relName.equals(relationName))
			{
				return relations.get(i); 
			}
		}
		
		System.out.println("cannot find relation: " + relationName); 
		return null; 
	}
	
	public void printForCheck()
	{
		System.out.println("Database: " + databaseName + ":");  
		for(int i = 0; i < relations.size(); i++)
		{
			relations.get(i).printForCheck(); 
		}
	}	
}