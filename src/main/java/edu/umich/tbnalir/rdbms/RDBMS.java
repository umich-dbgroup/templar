package edu.umich.tbnalir.rdbms;

import edu.umich.tbnalir.dataStructure.ParseTreeNode;
import edu.umich.tbnalir.util.Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RDBMS {
	public SchemaGraph schemaGraph; 
	public Connection conn;

	public ArrayList<String> history = new ArrayList<String>();

	public RDBMS(String database_name, String schemaGraphPrefix) throws Exception
	{
		String driver = "com.mysql.jdbc.Driver"; 
		String db_url = "jdbc:mysql://127.0.0.1:3306/";
		String user = "root";
		String password = null;
		Class.forName(driver);
		conn = DriverManager.getConnection(db_url, user, password);
		
		Statement statement = conn.createStatement(); 
		statement.execute("use " + database_name);

		// cjbaik: don't want to load history from database until I find out why
		// loadHistory(database_name);

		schemaGraph = new SchemaGraph(schemaGraphPrefix);
	}

	public List<Integer> getDistinctAttrCounts(Relation r, Attribute attr) {
        List<Integer> vals = new ArrayList<>();
        try {
            Statement statement = this.conn.createStatement();
			ResultSet results;
			if (Utils.isSQLTypeString(attr.getType())) {
                // Accelerate for text types by grouping by hash
                results = statement.executeQuery("SELECT COUNT(*) "
                        + "FROM " + r.getName() + " GROUP BY md5(" + attr.getName() + ")");
            } else {
                results = statement.executeQuery("SELECT COUNT(*) "
                        + "FROM " + r.getName() + " GROUP BY " + attr.getName());
            }
            while (results.next()) {
                vals.add(results.getInt(1));
            }

            return vals;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public int getRelationSize(Relation r) {
        try {
            Statement statement = this.conn.createStatement();
            ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + r.getName());
            if (result.next()) {
                return result.getInt(1);
            } else {
                throw new RuntimeException("Could not get relation size for " + r.getName());
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

	public ArrayList<ArrayList<String>> conductSQL(String query)
	{
		ArrayList<ArrayList<String>> finalResults = new ArrayList<ArrayList<String>>(); 
		try
		{
			Statement statement = conn.createStatement(); 

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
	
	public void loadHistory(String database) throws SQLException
	{
		Statement statement = conn.createStatement(); 
		String query = "SELECT * FROM " + database + ".history; "; 
		ResultSet results = statement.executeQuery(query); 
		
		while(results.next())
		{
			history.add(results.getString(2)); 
		}
	}
	
	public boolean isSchemaExist(ParseTreeNode treeNode) throws Exception
	{
//		ArrayList<SchemaElement> attributes = schemaGraph.getElementsByType("text number");
		ArrayList<SchemaElement> attributes = schemaGraph.getElementsByType("text int");

		for(int i = 0; i < attributes.size(); i++)
		{
			MappedSchemaElement element = attributes.get(i).isSchemaExist(treeNode.label); 
			if(element != null)
			{
				treeNode.mappedElements.add(element); 
			}
		}
		if(!treeNode.mappedElements.isEmpty())
		{
			return true; 
		}
		else
		{
			return false;
		}
	}
	
	public boolean isTextExist(ParseTreeNode treeNode) throws Exception 
	{
		ArrayList<SchemaElement> textAtts = schemaGraph.getElementsByType("text"); 
		for(int i = 0; i < textAtts.size(); i++)
		{
			MappedSchemaElement textAtt = textAtts.get(i).isTextExist(treeNode.label, conn); 
			if(textAtt != null)
			{
				
				treeNode.mappedElements.add(textAtt); 
			}
		}
		
		if(!treeNode.mappedElements.isEmpty())
		{
			return true; 
		}
		return false;
	}

	public boolean isNumExist(String operator, ParseTreeNode treeNode) throws Exception 
	{
//		ArrayList<SchemaElement> textAtts = schemaGraph.getElementsByType("number");
		ArrayList<SchemaElement> textAtts = schemaGraph.getElementsByType("int");
		for(int i = 0; i < textAtts.size(); i++)
		{
			MappedSchemaElement textAtt = textAtts.get(i).isNumExist(treeNode.label, operator, conn); 
			if(textAtt != null)
			{
				treeNode.mappedElements.add(textAtt); 
			}
		}
		
		if(!treeNode.mappedElements.isEmpty())
		{
			return true; 
		}
		return false;
	}
}
