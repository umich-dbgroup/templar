package edu.umich.templar.dataStructure;

import edu.umich.templar.parse.DependencyInfo;

import java.io.Serializable;
import java.util.*;

@SuppressWarnings("serial")
public class ParseTree implements Serializable
{
	public ParseTreeNode root;
    Map<Integer, ParseTreeNode> nodeIndexMap = new HashMap<>();
	public ArrayList<ParseTreeNode> allNodes = new ArrayList<ParseTreeNode>();
	public ArrayList<ParseTreeNode> deletedNodes = new ArrayList<ParseTreeNode>(); 

	public ParseTree()
	{
		root = new ParseTreeNode(0, "ROOT", "ROOT", "ROOT", null); 
		allNodes.add(root); 
		root.tokenType = "ROOT";
	}

	public void addNodesRecursive(Map<Integer, List<DependencyInfo>> dependenciesMap,
                                          ParseTreeNode parent, DependencyInfo nodeInfo) {
        if (parent == null) {
            parent = this.root;
        }
        ParseTreeNode node = new ParseTreeNode(nodeInfo.getDepWord().getIndex(), nodeInfo.getDepWord().getText(),
                nodeInfo.getDepWord().getPos(), nodeInfo.getRelationship(), parent);
        parent.children.add(node);

        Integer nodeIndex = nodeInfo.getDepWord().getIndex();
        this.nodeIndexMap.put(nodeIndex, node);
        this.allNodes.add(node);

        // Recursively add next nodes
        List<DependencyInfo> childrenInfo = dependenciesMap.get(nodeIndex);
        if (childrenInfo != null && !childrenInfo.isEmpty()) {
            for (DependencyInfo childInfo : childrenInfo) {
                this.addNodesRecursive(dependenciesMap, node, childInfo);
            }
        }
    }

	public boolean buildNode(String [] input) // add a node when build a tree;
	{
		ParseTreeNode node; 
		
		if(root.children.isEmpty())
		{
			node = new ParseTreeNode(Integer.parseInt(input[0]), input[1], input[2], input[4], root); 
			root.children.add(node); 
			allNodes.add(node); 
			return true; 
		}
		else
		{			
			LinkedList<ParseTreeNode> list = new LinkedList<ParseTreeNode>(); 
			list.add(root); 
			while(!list.isEmpty())
			{
				ParseTreeNode parent = list.removeFirst(); 
				if(parent.wordOrder == Integer.parseInt(input[3]))
				{
					node = new ParseTreeNode(Integer.parseInt(input[0]), input[1], input[2], input[4], parent); 
					parent.children.add(node); 
					allNodes.add(node); 
					return true; 
				}
				list.addAll(parent.children); 
			}
		}
		return false; 
	}

    public ParseTreeNode getNode(int index) {
        return this.nodeIndexMap.get(index);
    }

	public ParseTreeNode searchNodeByOrder(int order)
	{
		for(int i = 0; i < this.allNodes.size(); i++)
		{
			if(this.allNodes.get(i).wordOrder == order)
			{
				return this.allNodes.get(i);
			}
		}
		return null;
	}
	
	public ParseTreeNode searchNodeByID(int ID)
	{
		for(int i = 0; i < this.allNodes.size(); i++)
		{
			if(this.allNodes.get(i).nodeID == ID)
			{
				return this.allNodes.get(i); 
			}
		}
		return null; 
	}
	
	public void deleteNode(ParseTreeNode node)
	{
		ParseTreeNode parent = node.parent; 
		node.parent = null; 
		int position = parent.children.indexOf(node); 
		parent.children.remove(node); 
		
		if(!node.leftRel.isEmpty() && node.children.size() > 0)
		{
			node.children.get(0).leftRel = node.leftRel; 
		}
		
		for(int i = 0; i < node.children.size(); i++)
		{
			parent.children.add(position+i, node.children.get(i)); 
			node.children.get(i).parent = parent; 
		}
		allNodes.remove(node); 
		
		if(!node.tokenType.equals("QT"))
		{
			this.deletedNodes.add(node); 
		}
	}
	
	public String toString() 
	{
		String result = "";
		LinkedList<ParseTreeNode> nodeList = new LinkedList<ParseTreeNode>(); 
		nodeList.add(root); 
		LinkedList<Integer> levelList = new LinkedList<Integer>(); 
		levelList.add(0); 
		
		while(!nodeList.isEmpty())
		{
			ParseTreeNode curNode = nodeList.removeLast(); 
			int curLevel = levelList.removeLast(); 
			for(int i = 0; i < curLevel; i++)
			{
				result += "    "; 
			}
			result += "(" + curNode.nodeID + ") ";
			result += curNode.label + " " + curNode.tokenType + "\n";
			
			for(int i = 0; i < curNode.children.size(); i++)
			{
				nodeList.add(curNode.children.get(curNode.children.size()-i-1)); 
				levelList.add(curLevel+1); 
			}
		}
		
		return result; 
	}
}
