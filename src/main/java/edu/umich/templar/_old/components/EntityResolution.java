package edu.umich.templar._old.components;

import edu.umich.templar._old.dataStructure.EntityPair;
import edu.umich.templar._old.dataStructure.ParseTreeNode;
import edu.umich.templar._old.dataStructure.Query;
import edu.umich.templar._old.rdbms.SchemaElement;

import java.util.ArrayList;

public class EntityResolution
{
	public static void entityResolute(Query query)
	{
		ArrayList<ParseTreeNode> nodes = query.parseTree.allNodes;
				
		for(int i = 0; i < nodes.size(); i++)
		{
			ParseTreeNode left = nodes.get(i);
			if(left.getChoiceMap() == null)
			{
				continue; 
			}
			SchemaElement leftMap = left.getChoiceMap().schemaElement;

			for(int j = i+1; j < nodes.size(); j++)
			{
				ParseTreeNode right = nodes.get(j); 
				if(right.getChoiceMap() == null)
				{
					continue; 
				}
				SchemaElement rightMap = right.getChoiceMap().schemaElement; 
				
				if(leftMap.equals(rightMap))
				{
					if(left.tokenType.equals("VTTEXT") && right.tokenType.equals("VTTEXT"))
					{
						if(left.label.equals(right.label))
						{
							EntityPair entityPair = new EntityPair(left, right);
							query.entities.add(entityPair); 
						}
						else
						{
							continue; 
						}
					}
					
					if((left.tokenType.equals("VTTEXT") && right.tokenType.equals("NT"))
						||(left.tokenType.equals("NT") && right.tokenType.equals("VTTEXT"))
						||(left.tokenType.equals("NT") && right.tokenType.equals("NT")))
					{
						if(Math.abs(left.wordOrder - right.wordOrder) > 2)
						{
							continue; 
						}
						else
						{
							EntityPair entityPair = new EntityPair(left, right);
							query.entities.add(entityPair); 
						}
					}
				}
			}		
		}
	}
}
