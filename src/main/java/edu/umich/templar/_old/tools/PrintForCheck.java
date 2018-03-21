package edu.umich.templar._old.tools;

import edu.umich.templar._old.dataStructure.ParseTree;
import edu.umich.templar._old.dataStructure.ParseTreeNode;
import edu.umich.templar._old.Constants;

public class PrintForCheck 
{
	public static void allParseTreeNodePrintForCheck(ParseTree parseTree)
	{
		for(int i = 0; i < parseTree.allNodes.size(); i++)
		{
			ParseTreeNode node = parseTree.allNodes.get(i); 
			String result = "";
			result += node.nodeID + ". "; 
			result += node.label + ": "; 
			result += node.tokenType + "; "; 
			result += node.function + "; "; 
			result += node.QT + "; ";
			result += "(" + node.choice + ") "; 

			if(node.mappedElements.size() > 0)
			{
				for(int j = 0; j < node.mappedElements.size() && j < Constants.MAX_MAPPED_EL; j++)
				{
					result += node.mappedElements.get(j).printForCheck() + "| "; 
				}
			}
			System.out.println(result); 
		}
	}
}
