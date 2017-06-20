package rdbms;

import java.io.Serializable;

import org.json.simple.JSONObject;

@SuppressWarnings("serial")
public class Attribute implements Serializable
{
	public String attName = ""; 
	public String attType = ""; // id, string, num 
	int attNum = -1; 	
	String importance = ""; 
	public Relation relation; 
	
	public Attribute(JSONObject jsonAtt, int j, Relation relation) 
	{
		attName = (String) jsonAtt.get("name"); 
		attType = (String) jsonAtt.get("type"); 
		attNum = j; 
		if(jsonAtt.get("importance") != null)
		{
			importance = (String) jsonAtt.get("importance"); 
		}
		this.relation = relation; 
	}

	public void printForCheck()
	{
		System.out.print("(" + attNum + ")"); 
		System.out.print(relation.relName + "." + attName); 
		System.out.print("(" + attType + ")"); 
	}
}
