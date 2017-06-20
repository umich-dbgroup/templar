package dataStructure;

import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class CGroup implements Serializable
{
	public int groupID; 
	public ArrayList<BParseTreeNode> nodes = new ArrayList<BParseTreeNode>(); 

	public void printForCheck()
	{
		System.out.print("group_" + groupID + ": ");
		for(int i = 0; i < nodes.size(); i++)
		{
			System.out.print(nodes.get(i).nodeID + "; "); 
		}
		System.out.println(); 
	}
}
