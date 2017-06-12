package dataStructure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Stack;

import edu.northwestern.at.morphadorner.corpuslinguistics.lemmatizer.EnglishLemmatizer;

import rdbms.Attribute;
import rdbms.RDBMS;
import tools.Functions;

@SuppressWarnings("serial")
public class BParseTree implements Serializable
{
	public BParseTreeNode root; 
	public ArrayList<BParseTreeNode> allNodes = new ArrayList<BParseTreeNode>(); 
	public double evaluationScore = 0; 
	public int step = 0; 

	public BParseTree()
	{
		root = new BParseTreeNode(0, "ROOT", "ROOT", "ROOT", null); 
		allNodes.add(root); 
		root.tokenType = "ROOT"; 
	}

	public boolean addNode(String [] input) // add a node when build a tree; 
	{
		BParseTreeNode node; 
		
		if(root.children.isEmpty())
		{
			node = new BParseTreeNode(Integer.parseInt(input[0]), input[1], input[2], input[4], root); 
			root.children.add(node); 
			allNodes.add(node); 
			return true; 
		}
		else
		{			
			LinkedList<BParseTreeNode> list = new LinkedList<BParseTreeNode>(); 
			list.add(root); 
			while(!list.isEmpty())
			{
				BParseTreeNode parent = list.removeFirst(); 
				if(parent.wordOrder == Integer.parseInt(input[3]))
				{
					node = new BParseTreeNode(Integer.parseInt(input[0]), input[1], input[2], input[4], parent); 
					parent.children.add(node); 
					allNodes.add(node); 
					return true; 
				}
				list.addAll(parent.children); 
			}
		}
		return false; 
	}
	
	public void deleteNode(BParseTreeNode node)
	{
		BParseTreeNode parent = node.parent; 
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
	}
	
	public boolean moveTree(BParseTreeNode child, BParseTreeNode parent, int position)
	{
		parent.children.add(position, child); 
		child.parent.children.remove(child); 
		child.parent = parent; 
		return true; 
	}
	
	public boolean moveTree(BParseTreeNode child, BParseTreeNode parent)
	{
		BParseTreeNode node = parent; 
		while(node != null)
		{
			if(node.equals(child))
			{
				return false; 
			}
			node = node.parent; 
		}
		
		int position = 0; 
		for(position = 0; position < parent.children.size(); position++)
		{
			if(parent.children.get(position).wordOrder > child.wordOrder)
			{
				break; 
			}
		}
		
		parent.children.add(position, child); 
		child.parent.children.remove(child); 
		child.parent = parent; 
		return true; 
	}

	public void moveTreeAfter(BParseTreeNode child, BParseTreeNode parent, int position)
	{
		BParseTreeNode oldParent = child.parent; 
		int oldPosition = oldParent.children.indexOf(child); 
		
		parent.children.add(position, child); 
		child.parent.children.remove(child); 
		child.parent = parent; 
		
		int num = 0; 
		for(int i = 0; i < child.children.size(); i++)
		{
			if(child.children.get(i).wordOrder < child.wordOrder)
			{
				moveTree(child.children.get(i), oldParent, oldPosition+num); 
				i--; 
				num++; 
			}
		}
	}
	
	public BParseTreeNode searchNodeByOrder(int order)
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
		
	public BParseTreeNode searchNodeByID(int nodeID)
	{
		for(int i = 0; i < this.allNodes.size(); i++)
		{
			if(this.allNodes.get(i).nodeID == nodeID)
			{
				return this.allNodes.get(i); 
			}
		}
		return null; 
	}
	
	public BParseTreeNode addTree(BParseTreeNode child, BParseTreeNode parent, int position)
	{
		BParseTreeNode root = null; 
		LinkedList<BParseTreeNode> childList = new LinkedList<BParseTreeNode>(); 
		LinkedList<BParseTreeNode> parentList = new LinkedList<BParseTreeNode>(); 
		LinkedList<Integer> positionList = new LinkedList<Integer>(); 
		childList.add(child); 
		parentList.add(parent); 
		positionList.add(position); 
		while(!childList.isEmpty())
		{
			child = childList.removeFirst(); 
			parent = parentList.removeFirst(); 
			position = positionList.removeFirst(); 
			BParseTreeNode newChild = this.addNode(child, parent, position); 
			if(root == null)
			{
				root = newChild; 
			}
			for(int i = 0; i < child.children.size(); i++)
			{
				childList.add(child.children.get(i));
				parentList.add(newChild); 
				positionList.add(i); 
			}
		}
		
		return root; 
	}
	
	public BParseTreeNode addNode(BParseTreeNode child, BParseTreeNode parent, int position)
	{
		if((child == null) || (parent == null) || (parent.children.size() < position))
		{
			return null; 
		}

		BParseTreeNode newChild = new BParseTreeNode(child, parent); 
		parent.children.add(position, newChild); 
		this.allNodes.add(newChild); 
		
		return newChild; 
	}
	
	public int height()
	{
		int height = 0; 
		for(int i = 0; i < allNodes.size(); i++)
		{
			BParseTreeNode node = allNodes.get(i); 
			int nodeHeight = 0; 
			while(node.parent != null)
			{
				nodeHeight++; 
				node = node.parent; 
			}
			if(nodeHeight > height)
			{
				height = nodeHeight; 
			}
		}
		return height; 
	}
	
	public int distance(BParseTreeNode node1, BParseTreeNode node2)
	{
		int distance = 0; 
		int height1 = 0; 
		BParseTreeNode node = node1; 
		while(node.parent != null)
		{
			node = node.parent; 
			height1++; 
		}
		int height2 = 0; 
		node = node2; 
		while(node.parent != null)
		{
			node = node.parent; 
			height2++; 
		}
		
		if(height1 > height2)
		{
			distance = height1 - height2; 
			for(int i = 0; i < height1 - height2; i++)
			{
				node1 = node1.parent; 
			}
		}
		else
		{
			distance = height2 - height1; 
			for(int i = 0; i < height2 - height1; i++)
			{
				node2 = node2.parent; 
			}
		}
		
		while(node1.nodeID != node2.nodeID)
		{
			distance++; 
			node1 = node1.parent; 
			node2 = node2.parent; 
		}
		
		return distance; 
	}

	public int hash()
	{
		String result = ""; 
		Stack<BParseTreeNode> stack = new Stack<BParseTreeNode>(); 
		Stack<Boolean> stackB = new Stack<Boolean>(); 
		
		stack.add(this.root); 
		stack.add(this.root); 
		stackB.add(false); 
		stackB.add(true); 
		
		while(!stack.isEmpty())
		{
			BParseTreeNode node = stack.pop(); 
			boolean extensible = stackB.pop(); 
			
			result += node.nodeID + " "; 
			
			if(extensible == true)
			{
				for(int i = node.children.size() - 1; i >= 0; i--)
				{
					stack.add(node.children.get(i)); 
					stack.add(node.children.get(i)); 
					stackB.add(false); 
					stackB.add(true); 
				}
			}
		}
		return result.hashCode(); 
	}
	
	public void evaluate(RDBMS db) 
	{
		this.evaluationScore = 0; 
		for(int i = 0; i < allNodes.size(); i++)
		{
			this.allNodes.get(i).evaluationScore = 0; 
		}
		for(int i = 0; i < allNodes.size(); i++)
		{
			allNodes.get(i).evaluate(this, db); 
		}
		for(int i = 0; i < allNodes.size(); i++)
		{
			this.evaluationScore += allNodes.get(i).evaluationScore; 
		}
	}
	
	public void addImplicit(EnglishLemmatizer lemmatizer)
	{
		for(int i = 0; i < allNodes.size(); i++)
		{
			BParseTreeNode OT = allNodes.get(i); 
			if(OT.tokenType.equals("OT") && OT.children.size() == 2)
			{
				BParseTreeNode left = OT.children.get(0); 
				BParseTreeNode right = OT.children.get(1); 
				
				if(left.tokenType.equals("VT"))
				{
					OT.children.set(0, right); 
					OT.children.set(1, left); 
					left = OT.children.get(0); 
					right = OT.children.get(1); 
				}
				
				if(right.tokenType.equals("VT") && !right.candidates.isEmpty() && right.candidates.get(right.choice).attribute.attType.equals("string"))
				{
					if(left.tokenType.equals("NT") && !left.candidates.isEmpty())
					{
						BParseTreeNode add = (BParseTreeNode)Functions.depthClone(left); 
						add.nodeID = BParseTreeNode.NODEID; 
						add.wordOrder = -1; 
						add.tokenType = "NT"; 
						add.parent = right.parent; 
						BParseTreeNode.NODEID++; 
						add.children = new ArrayList<BParseTreeNode>(); 
						OT.children.add(add); 
						this.allNodes.add(add); 
						this.moveTree(right, add, 0); 
						right = add; 
					}
				}
			}
		}		
		
		for(int i = 0; i < allNodes.size(); i++)
		{
			BParseTreeNode node = allNodes.get(i); 
			
			if(node.tokenType.equals("NT") && (node.parent.tokenType.equals("OT") || (node.parent.tokenType.equals("FT") && !node.parent.function.equals("count"))))
			{
				if(node.candidates.get(node.choice).type.equals("relation") 
					|| (node.candidates.get(node.choice).type.equals("attribute") && node.candidates.get(node.choice).attribute.attType.equals("string")))
				{
					BParseTreeNode add = new BParseTreeNode(-1, "number of", "NA", "NA", node.parent); 
					add.tokenType = "FT"; 
					add.function = "count"; 
					this.allNodes.add(add); 
					int index = node.parent.children.indexOf(node); 
					node.parent.children.add(index, add); 
					moveTree(node, add, 0); 
				}
			}
		}
		
		for(int i = 0; i < this.allNodes.size(); i++)
		{
			if(this.allNodes.get(i).tokenType.equals("NT"))
			{
				this.allNodes.get(i).entityID = this.allNodes.get(i).nodeID; 
			}
		}
		
		for(int i = 0; i < this.allNodes.size(); i++)
		{
			BParseTreeNode OT = this.allNodes.get(i); 
			if(OT.tokenType.equals("OT") && OT.parent.tokenType.equals("NT") && !OT.parent.candidates.isEmpty() && OT.children.size() == 2)
			{
				Attribute parentAtt = OT.parent.candidates.get(OT.parent.choice).defaultAtt(); 
				BParseTreeNode left = OT.children.get(0); 
				while(left.tokenType.equals("FT") && !left.children.isEmpty())
				{
					left = left.children.get(0); 
				}
				if(left.tokenType.equals("NT"))
				{
					ArrayList<BParseTreeNode> list = new ArrayList<BParseTreeNode>(); 
					list.add(left); 
					boolean exist = false; 
					while(!list.isEmpty())
					{
						BParseTreeNode node = list.remove(0); 
						if(!node.candidates.isEmpty() && node.candidates.get(node.choice).defaultAtt().equals(parentAtt))
						{
							exist = true; 
							break; 
						}
						list.addAll(node.children); 
					}
					if(exist == false)
					{
						BParseTreeNode add = (BParseTreeNode)Functions.depthClone(OT.parent); 
						add.parent = left; 
						this.allNodes.add(add); 
						left.children.add(0, add); 
						add.children = new ArrayList<BParseTreeNode>(); 
						add.function = "NA"; 
						add.label = lemmatizer.lemmatize(add.label); 
						add.nodeID = BParseTreeNode.NODEID; 
						BParseTreeNode.NODEID++; 
					}
				}
				
				BParseTreeNode right = OT.children.get(1); 
				while(right.tokenType.equals("FT") && !right.children.isEmpty())
				{
					right = right.children.get(0); 
				}
				if(right.tokenType.equals("NT"))
				{
					ArrayList<BParseTreeNode> list = new ArrayList<BParseTreeNode>(); 
					list.add(right); 
					boolean exist = false; 
					while(!list.isEmpty())
					{
						BParseTreeNode node = list.remove(0); 
						if(!node.candidates.isEmpty() && node.candidates.get(node.choice).defaultAtt().equals(parentAtt))
						{
							exist = true; 
							break; 
						}
						list.addAll(node.children); 
					}
					if(exist == false)
					{
						BParseTreeNode add = (BParseTreeNode)Functions.depthClone(OT.parent); 
						add.parent = right; 
						add.function = "NA"; 
						right.children.add(0, add); 
						this.allNodes.add(add); 
						add.label = lemmatizer.lemmatize(add.label); 
						add.children = new ArrayList<BParseTreeNode>(); 
						add.nodeID = BParseTreeNode.NODEID; 
						BParseTreeNode.NODEID++; 
					}
				}
			}
		}
	}
	
	public String centerTranslate(BParseTreeNode node)
	{
		String result = ""; 
		if(node.tokenType.equals("NT"))
		{
			ArrayList<BParseTreeNode> OTs = new ArrayList<BParseTreeNode>(); 
			for(int i = 0; i < node.children.size(); i++)
			{
				if(!node.children.get(i).tokenType.equals("OT") || node.children.get(i).children.size() == 1)
				{
					String prep = ""; 
					if(!node.children.get(i).prep.isEmpty())
					{
						prep = node.children.get(i).prep; 
					}
					else if(!node.children.get(i).tokenType.equals("OT"))
					{
						prep = "of"; 
					}
					if(!node.children.get(i).tokenType.equals("OT"))
					{
						result += " "; 
					}
					result += prep + this.conditionTranslate(node.children.get(i));
				}
				else
				{
					OTs.add(node.children.get(i)); 
				}
			}
			
			if(!OTs.isEmpty())
			{
				result += ", where"; 
				for(int i = 0; i < OTs.size(); i++)
				{
					result += this.conditionTranslate(OTs.get(i)); 
				}
			}
		}
		
		return result; 
	}
	
	public String conditionTranslate(BParseTreeNode node)
	{
		String result = ""; 
		if(node.tokenType.equals("NT"))
		{
			if(!node.parent.tokenType.equals("FT") && node.function.equals("NA"))
			{
				result += " the"; 
			}
			else if(!node.parent.tokenType.equals("FT") && !node.function.equals("NA"))
			{
				result += " " + node.function; 
			}
			result += " " + node.label; 
			result += centerTranslate(node); 
		}
		else if(node.tokenType.equals("VT"))
		{
			result += " " + node.label; 
		}
		else if(node.tokenType.equals("OT"))
		{
			if(node.children.size() == 2)
			{
				BParseTreeNode left = node.children.get(0); 
				BParseTreeNode right = node.children.get(1); 
				result += conditionTranslate(left); 
				String label = node.label; 
				if(!label.contains("than"))
				{
					label += " than"; 
				}
				result += " is " + label + conditionTranslate(right); 
			}
			else if(node.children.size() == 1)
			{
				result += " " + node.label + conditionTranslate(node.children.get(0)); 
			}
		}
		else if(node.tokenType.equals("FT"))
		{
			if(!node.parent.tokenType.equals("FT"))
			{
				result += " the"; 
			}
			result += " " + node.label; 
			if(node.children.size() > 0)
			{
				result += conditionTranslate(node.children.get(0)); 
			}
		}

		return result; 
	}
	
	public String translateToNL()
	{
		String result = ""; 
		if(root.children.size() < 1)
		{
			return result; 
		}
		
		BParseTreeNode CMT = root.children.get(0); 
		result += CMT.label + " the"; 
		for(int i = 0; i < CMT.children.size(); i++)
		{
			BParseTreeNode returnNode = CMT.children.get(i); 
			if(CMT.children.size() > 1 && i == CMT.children.size() - 1)
			{
				result += " and"; 
			}
			while(returnNode.tokenType.equals("FT"))
			{
				if(returnNode.parent.tokenType.equals("CMT"))
				{
					result += " the";  
				}
				result += " " + returnNode.label; 
				if(returnNode.children.size() == 0)
				{
					break; 
				}
				returnNode = returnNode.children.get(0); 
			}
			if(returnNode.tokenType.equals("NT"))
			{
				result += " " + returnNode.label + ","; 
			}
		}
		if(result.endsWith(","))
		{
			result = result.substring(0, result.length()-1); 
		}
		
		ArrayList<BParseTreeNode> whereCenters = new ArrayList<BParseTreeNode>(); 
		for(int i = 0; i < CMT.children.size(); i++)
		{
			BParseTreeNode returnNode = CMT.children.get(i); 
			while(returnNode.tokenType.equals("FT"))
			{
				if(returnNode.children.size() == 0)
				{
					break; 
				}
				returnNode = returnNode.children.get(0); 
			}
			if(returnNode.tokenType.equals("NT") && returnNode.children.size() > 0)
			{
				whereCenters.add(returnNode); 
			}
		}
		
		if(whereCenters.size() != 1)
		{
			return result; 
		}
		else
		{
			result += this.centerTranslate(whereCenters.get(0)); 
		}
		
		return result; 
	}
	
	public boolean isValid()
	{
		if(root.children.size() < 1)
		{
			return false; 
		}
		
		BParseTreeNode CMT = root.children.get(0); 
		if(!CMT.tokenType.equals("CMT"))
		{
			return false; 
		}
		
		for(int i = 0; i < CMT.children.size(); i++)
		{
			BParseTreeNode returnNode = CMT.children.get(i); 
			while(returnNode.tokenType.equals("FT"))
			{
				if(returnNode.children.size() == 0)
				{
					return false; 
				}
				returnNode = returnNode.children.get(0); 
			}
			if(!returnNode.tokenType.equals("NT"))
			{
				return false; 
			}
		}
		
		ArrayList<BParseTreeNode> whereCenters = new ArrayList<BParseTreeNode>(); 
		for(int i = 0; i < CMT.children.size(); i++)
		{
			BParseTreeNode returnNode = CMT.children.get(i); 
			while(returnNode.tokenType.equals("FT"))
			{
				if(returnNode.children.size() == 0)
				{
					break; 
				}
				returnNode = returnNode.children.get(0); 
			}
			if(returnNode.tokenType.equals("NT") && returnNode.children.size() > 0)
			{
				whereCenters.add(returnNode); 
			}
		}
		
		if(whereCenters.size() != 1)
		{
			return false; 
		}
		else
		{
			return this.centerValid(whereCenters.get(0)); 
		}
	}
	
	public boolean centerValid(BParseTreeNode node)
	{
		if(node.tokenType.equals("NT"))
		{
			for(int i = 0; i < node.children.size(); i++)
			{
				if(!this.conditionValid(node.children.get(i)))
				{
					return false; 
				}
			}
		}
		
		return true; 
	}
	
	public boolean conditionValid(BParseTreeNode node)
	{
		if(node.tokenType.equals("NT"))
		{
			return centerValid(node); 
		}
		else if(node.tokenType.equals("VT"))
		{
			return true;  
		}
		else if(node.tokenType.equals("OT"))
		{
			if(node.children.size() == 2)
			{
				BParseTreeNode left = node.children.get(0); 
				BParseTreeNode right = node.children.get(1); 
				if(!conditionValid(left))
				{
					return false; 
				}
				if(!conditionValid(right))
				{
					return false; 
				}
			}
			else if(node.children.size() == 1)
			{
				return conditionValid(node.children.get(0)); 
			}
		}
		else if(node.tokenType.equals("FT"))
		{
			if(node.children.size() > 0)
			{
				return conditionValid(node.children.get(0)); 
			}
		}

		return true; 
	}
	
	public void printForCheck() 
	{
		System.out.println("NL: " + this.translateToNL()); 
		System.out.println("Parsed Tree: " + this.evaluationScore + " [" + this.step + "] (" + this.height() + ")"); 
		LinkedList<BParseTreeNode> nodeList = new LinkedList<BParseTreeNode>(); 
		nodeList.add(root); 
		LinkedList<Integer> levelList = new LinkedList<Integer>(); 
		levelList.add(0); 
		
		while(!nodeList.isEmpty())
		{
			BParseTreeNode curNode = nodeList.removeLast(); 
			int curLevel = levelList.removeLast(); 
			for(int i = 0; i < curLevel; i++)
			{
				System.out.print("    "); 
			}
			System.out.print("(" + curNode.nodeID); 
			if(curNode.entityID >= 0)
			{
				System.out.print("," + curNode.entityID); 
			}
			System.out.print(")"); 
			if(!curNode.function.equals("NA"))
			{
				System.out.print("(" + curNode.function + ") "); 
			}
			System.out.print(curNode.label); 
			System.out.println(" [" + curNode.evaluationScore + "]"); 
			
			for(int i = 0; i < curNode.children.size(); i++)
			{
				nodeList.add(curNode.children.get(curNode.children.size()-i-1)); 
				levelList.add(curLevel+1); 
			}
		}
		
		System.out.println("All Nodes:"); 
		for(int i = 0; i < allNodes.size(); i++)
		{
			allNodes.get(i).printForCheck(); 				
		}
		System.out.println("-----------------------------"); 
	}
}
