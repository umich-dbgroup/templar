package components;

import rdbms.RDBMS;
import dataStructure.ParseTree;
import dataStructure.ParseTreeNode;
import dataStructure.Query;

public class TreeStructureAdjustor 
{
	public static void treeStructureAdjust(Query query, RDBMS db)
	{
		ParseTree tree = query.parseTree; 
		
		for(int i = 0; i < tree.allNodes.size(); i++)
		{
			ParseTreeNode curNode = tree.allNodes.get(i); 
			if(curNode.tokenType.contains("FT") && curNode.children.isEmpty())
			{
				ParseTreeNode parent = curNode.parent; 
				
				if(parent.tokenType.equals("FT"))
				{
					tree.moveSubTree(curNode, parent); 
				}
				else
				{
					while(!parent.tokenType.contains("NT"))
					{
						parent = parent.parent; 
					}
					tree.moveSubTree(curNode, parent); 
				}				
			}
		}	
	
		for(int i = 0; i < tree.allNodes.size(); i++)
		{
			ParseTreeNode curNode = tree.allNodes.get(i); 
			if(curNode.tokenType.equals("FT") && curNode.children.size() > 1)
			{
				for(int j = 1; j < curNode.children.size(); j++)
				{
					tree.moveSubTree(curNode.children.get(0), curNode.children.get(1)); 
				}
			}
		}

		for(int i = 0; i < tree.allNodes.size(); i++)
		{
			ParseTreeNode curNode = tree.allNodes.get(i); 
			if(curNode.tokenType.contains("VT") && !curNode.children.isEmpty())
			{
				if(curNode.children.get(0).tokenType.equals("NT"))
				{
					ParseTreeNode child = curNode.children.get(0); 
					tree.moveSubTree(curNode.parent, child); 
					tree.moveSubTree(child, curNode); 
				}
				else
				{
					ParseTreeNode parent = curNode.parent; 
					while(!parent.tokenType.contains("NT"))
					{
						parent = parent.parent; 
					}					
					for(int j = 0; j < curNode.children.size(); j++)
					{
						tree.moveSubTree(parent, curNode.children.get(0)); 
					}					
				}
			}
		}
		
		for(int i = 0; i < tree.allNodes.size(); i++)
		{
			ParseTreeNode curNode = tree.allNodes.get(i); 
			if(curNode.tokenType.contains("OT") && curNode.children.isEmpty())
			{
				if(curNode.parent.tokenType.contains("NT"))
				{
					if(curNode.parent.children.get(0).tokenType.contains("VTNUM"))
					{
						tree.moveSubTree(curNode, curNode.parent.children.get(0)); 
					}
				}
			}
		}	
	}
}
