package rdbms;

import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class SchemaElement implements Serializable
{
	public String type; 
	public Attribute attribute = null; 
	public Relation relation = null; 
	public ArrayList<String> mappedValues = new ArrayList<String>(); // for value; 
	public int choice = 0; 
	public String example = ""; 
	public double score = 0; 
	
	public SchemaElement(String type, Attribute attribute, String example)
	{
		this.type = type; // value, attribute, relation; 
		this.attribute = attribute; 
		this.example = example; 
	}
	
	public SchemaElement(Relation relation)
	{
		type = "relation"; 
		this.relation = relation; 
	}
	
	public SchemaElement(Attribute attribute)
	{
		type = "attribute"; 
		this.attribute = attribute; 
	}
	
	public Attribute defaultAtt()
	{
		if(type.equals("relation"))
		{
			return relation.primaryAtt; 
		}
		else
		{
			return attribute; 
		}
	}
	
	public String toString()
	{
		if(type.equals("relation"))
		{
			return relation.relName; 
		}
		else if(type.equals(attribute))
		{
			return attribute.attName; 
		}
		else
		{
			if(!mappedValues.isEmpty())
			{
				return mappedValues.get(choice); 
			}
			return attribute.attName; 
		}
	}
	
	public void printForCheck() 
	{
		if(type.equals("value"))
		{
			System.out.print(attribute.relation.relName + "." + attribute.attName);
//			if(score > 0)
//			{
//				System.out.print("(" + score + ")"); 
//			}

			if(!mappedValues.isEmpty())
			{
				System.out.print(":" + mappedValues.get(0)); 
			}
		}
		else if(type.equals("relation"))
		{
			System.out.print(relation.relName);
//			System.out.print("(" + score + ")"); 
		}
		else if(type.equals("attribute"))
		{
			System.out.print(attribute.relation.relName + "." + attribute.attName); 
//			System.out.print("(" + score + ")"); 
		}
	}
}
