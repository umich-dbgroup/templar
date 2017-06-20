package rdbms;

import java.io.Serializable;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import edu.northwestern.at.morphadorner.corpuslinguistics.lemmatizer.EnglishLemmatizer;

import tools.Functions;

@SuppressWarnings("serial")
public class Relation implements Serializable
{
	public String relName = ""; 
	public String type = ""; 
	public int relNum = -1; 
	ArrayList<Attribute> allAtts = new ArrayList<Attribute>(); 
	Attribute PK; 
	Attribute primaryAtt; 
	ArrayList<Attribute> importantAtts = new ArrayList<Attribute>(); 
	
	public Relation(JSONObject jsonRelation, int i) 
	{
		relName = (String)jsonRelation.get("name"); 
		type = (String)jsonRelation.get("type");
		relNum = i; 
		
		JSONArray jsonAtts = (JSONArray)jsonRelation.get("attributes"); 
		for(int j = 0; j < jsonAtts.size(); j++)
		{
			JSONObject jsonAtt = (JSONObject)jsonAtts.get(j); 
			Attribute attribute = new Attribute(jsonAtt, j, this); 
			
			allAtts.add(attribute); 
			if(!attribute.importance.isEmpty())
			{
				importantAtts.add(attribute); 
			}
			if(attribute.importance.equals("primary"))
			{
				primaryAtt = attribute; 
			}
			if(attribute.attType.equals("pk"))
			{
				PK = attribute; 
			}
		}
	}

	public void isSchemaExist(String tag, ArrayList<SchemaElement> candidateElements, EnglishLemmatizer lemmatizer)
	{
		double sim = Functions.wordNetSim(tag, relName, lemmatizer); 
		SchemaElement element = new SchemaElement(this); 
		element.score = sim; 
		if(!importantAtts.isEmpty())
		{
			candidateElements.add(element); 
		}
		
		for(int i = 0; i < allAtts.size(); i++)
		{
			if(!allAtts.get(i).attType.contains("id"))
			{
				sim = Functions.wordNetSim(tag, allAtts.get(i).attName, lemmatizer); 
				element = new SchemaElement(allAtts.get(i)); 
				element.score = sim; 
				candidateElements.add(element); 
			}
		}
	}
	
	public Attribute findAttributeByName(String attName)
	{
		for(int i = 0; i < allAtts.size(); i++)
		{
			if(allAtts.get(i).attName.equals(attName))
			{
				return allAtts.get(i); 
			}
		}
		
		System.out.println("error: cannot find the attribute named " + attName); 
		return null; 
	}
	
	public void printForCheck()
	{
		System.out.println("(" + relNum + ")" + relName + "(" + type + "):"); 
		System.out.print("attributes: "); 
		for(int i = 0; i < allAtts.size(); i++)
		{
			allAtts.get(i).printForCheck(); 
			System.out.print("; "); 
		}
		
		if(PK != null)
		{
			System.out.println(); 
			System.out.print("primaryKey: "); 
			System.out.print(PK.attName + "; "); 
		}
		
		if(primaryAtt != null)
		{
			System.out.print("primaryAtt: "); 
			System.out.print(primaryAtt.attName + "; "); 
		}
		if(!importantAtts.isEmpty())
		{
			System.out.println(); 
			System.out.print("importantAtts: "); 
			for(int i = 0; i < importantAtts.size(); i++)
			{
				System.out.print(importantAtts.get(i).attName + "; "); 
			}
		}
		System.out.println(); 
	}
}
