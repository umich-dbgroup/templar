package components;

import java.util.ArrayList;
import java.util.LinkedList;

import rdbms.RDBMS;
import dataStructure.BParseTree;
import dataStructure.BParseTreeNode;
import dataStructure.CBlock;
import dataStructure.CGroup;

public class CTranslator
{
	BParseTree parseTree; 
	RDBMS db; 
	DFeedbackGenerator feedbackGen; 

	ArrayList<CGroup> relatedGroups; 
	ArrayList<CBlock> blocks; 
	public String SQL = ""; 
	
	public CTranslator(RDBMS db, DFeedbackGenerator feedbackGen) 
	{
		this.db = db; 
		this.feedbackGen = feedbackGen; 
	}
	
	public void build(BParseTree parseTree)
	{
		this.SQL = ""; 
		this.parseTree = parseTree; 
		relatedGroups = new ArrayList<CGroup>(); 
		blocks = new ArrayList<CBlock>(); 
		this.relatedGroupGen(parseTree.root); 
		this.relatedGroupAdjust(); 
		this.blockGen(parseTree.root, null); 
		if(!this.blocks.isEmpty())
		{
			this.translate(); 
		}
		feedbackGen.SQL(this.SQL); 
		feedbackGen.meta(blocks.get(0).selectList); 
	}
	
	public void relatedGroupGen(BParseTreeNode root)
	{
		CGroup group = new CGroup(); 
		LinkedList<BParseTreeNode> queue = new LinkedList<BParseTreeNode>(); 
		queue.add(root); 
		while(!queue.isEmpty())
		{
			BParseTreeNode curNode = queue.removeFirst(); 
			if(curNode.tokenType.equals("OT") && curNode.children.size() == 2 
				&& !curNode.children.get(0).tokenType.equals("VT") && !curNode.children.get(1).tokenType.equals("VT"))
			{
				relatedGroupGen(curNode.children.get(0));
				relatedGroupGen(curNode.children.get(1)); 
			}
			else
			{
				if(!curNode.tokenType.equals("OT") && !curNode.candidates.isEmpty())
				{
					group.nodes.add(curNode); 
				}
				for(int i = 0; i < curNode.children.size(); i++)
				{
					queue.add(curNode.children.get(i)); 
				}
			}
		}
		if(group.nodes.size() > 0)
		{
			relatedGroups.add(group); 
		}
	}
	
	public void relatedGroupAdjust()
	{
		for(int i = 0; i < relatedGroups.size(); i++)
		{
			relatedGroups.get(i).groupID = i; 
		}
		
		int groupSize = 0; 
		while(groupSize != relatedGroups.size())
		{
			groupSize = relatedGroups.size(); 
			for(int i = 0; i < groupSize; i++)
			{
				boolean ifMerged = false; 
				for(int j = i+1; j < groupSize; j++)
				{
					if(mergeGroup(i, j) == true)
					{
						ifMerged = true; 
						break; 
					}
				}
				if(ifMerged == true)
				{
					break; 
				}
			}
		}
	}
	
	public boolean mergeGroup(int groupID1, int groupID2)
	{
		CGroup group1 = relatedGroups.get(groupID1); 
		CGroup group2 = relatedGroups.get(groupID2); 
		
		for(int i = 0; i < group1.nodes.size(); i++)
		{
			BParseTreeNode node1 = group1.nodes.get(i); 
			for(int j = 0; j < group2.nodes.size(); j++)
			{
				BParseTreeNode node2 = group2.nodes.get(j); 
				if(node1.entityID == node2.entityID && node1.entityID > 0)
				{
					group1.nodes.addAll(group2.nodes);
					relatedGroups.remove(group2); 
					return true; 
				}
			}
		}
		
		return false; 
	}
	
	public CBlock blockGen(BParseTreeNode root, CBlock outerBlock)
	{
		CBlock block = new CBlock(blocks.size(), root, outerBlock, db); 
		blocks.add(block); 
		
		LinkedList<BParseTreeNode> queue = new LinkedList<BParseTreeNode>(); 
		queue.add(root); 
		if(root.tokenType.equals("FT") && root.children.size() == 1 && root.children.get(0).tokenType.equals("NT"))
		{
			block.blockName = root.function + "_" + root.children.get(0).label + block.blockID; 
		}
		else if(root.tokenType.equals("FT") && root.children.size() == 1 && root.children.get(0).tokenType.equals("FT") 
			&& root.children.get(0).children.size() == 1 && root.children.get(0).children.get(0).tokenType.equals("NT"))
		{
			block.blockName = root.function + "_" + root.children.get(0).function + "_" + root.children.get(0).children.get(0).label + block.blockID; 
		}
		
		while(!queue.isEmpty())
		{
			BParseTreeNode curNode = queue.removeFirst(); 
			if(!curNode.equals(root) && curNode.tokenType.equals("FT") && curNode.children.size() > 0)
			{
				block.innerBlocks.add(blockGen(curNode, block));
			}
			else
			{
				block.nodes.add(curNode); 
				for(int i = 0; i < curNode.children.size(); i++)
				{
					queue.add(curNode.children.get(i)); 
				}
			}
		}
		
		return block; 
	}
	
	public void translate()
	{
		blocks.get(0).translate(relatedGroups); 
		this.SQL = blocks.get(0).SQL; 
	}

	public void printForCheck()
	{
		parseTree.printForCheck();
		for(int i = 0; i < this.relatedGroups.size(); i++)
		{
			this.relatedGroups.get(i).printForCheck(); 
		}
		System.out.println("-----------------------------"); 
		for(int i = 0; i < this.blocks.size(); i++)
		{
			this.blocks.get(i).printForCheck(); 
		}
	}
}
