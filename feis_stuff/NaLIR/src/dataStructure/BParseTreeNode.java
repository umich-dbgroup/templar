package dataStructure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

import rdbms.RDBMS;
import rdbms.SchemaElement;

@SuppressWarnings("serial")
public class BParseTreeNode implements Serializable
{
	public static int NODEID = 0; 
	public int nodeID; 
	public int wordOrder; 
	public int entityID = -1; 
	public String label; // word itself; 
	public String pos = "NA"; // part of speech; 
	public String relationship = "NA"; // dependency relationship; 
	public String tokenType = "NA"; // CMT, NT, VT and so forth; 
	public String function = "NA"; // only exist in OT and FT; 

	public String prep = ""; 
	
	public BParseTreeNode parent; 
	public ArrayList<BParseTreeNode> children = new ArrayList<BParseTreeNode>(); 
	public String leftRel = ""; // or/and
	
	public ArrayList<SchemaElement> candidates = new ArrayList<SchemaElement>(); 
	public int choice = 0; 
	
	public double evaluationScore = 0; 
	
	public BParseTreeNode(int wordOrder, String label, String pos, String relationship, BParseTreeNode parent) // node exist in sentence;
	{
		this.nodeID = NODEID;
		NODEID++; 
		this.wordOrder = wordOrder; 
		this.label = label; 
		this.pos = pos; 
		this.relationship = relationship; 
		this.parent = parent; 		
	}
	
	public BParseTreeNode(BParseTreeNode node, BParseTreeNode newParent) // copy from an exist node; 
	{
		this.nodeID = NODEID;
		NODEID++; 
		this.wordOrder = node.wordOrder;
		this.label = node.label; 
		this.pos = node.pos; 
		this.relationship = node.relationship; 
		this.tokenType = node.tokenType; 
		this.function = node.function;
		this.leftRel = node.leftRel; 
		this.prep = node.prep; 
		this.parent = newParent; 
		this.candidates = node.candidates; 
		this.choice = node.choice; 
		this.entityID = -1; 
	}	
	
	public BParseTreeNode getLeftSibling()
	{
		if(this.label.equals("ROOT"))
		{
			return null; 
		}
		int position = this.parent.children.indexOf(this); 
		if(position > 0)
		{
			return this.parent.children.get(position-1); 
		}
		return null; 
	}
	
	public BParseTreeNode getRightSibling()
	{
		if(this.label.equals("ROOT"))
		{
			return null; 
		}
		int position = this.parent.children.indexOf(this); 
		if(position < this.parent.children.size()-1)
		{
			return this.parent.children.get(position+1); 
		}
		return null; 
	}
	
	public BParseTreeNode findNumVTChild() // find a number child, or find a VT child; 
	{
		for(int i = 0; i < children.size(); i++)
		{
			if(children.get(i).tokenType.equals("VT") && children.get(i).pos.equals("CD") && children.get(i).children.size() == 0)
			{
				return children.get(i); 
			}
		}
		for(int i = 0; i < children.size(); i++)
		{
			if(children.get(i).tokenType.equals("VT") && children.get(i).children.size() == 0)
			{
				return children.get(i); 
			}
		}
		return null; 
	}
	
	public void evaluate(BParseTree tree, RDBMS db) 
	{
		BParseTreeNode before = null;
		if(this.getLeftSibling() != null)
		{
			before = this.getLeftSibling(); 
		}
		else if(this.parent != null)
		{
			before = this.parent; 
		}
		if(before != null)
		{
			int cost = 2; 
			if(this.wordOrder > before.wordOrder)
			{
				cost = this.wordOrder - before.wordOrder; 
			}
			else
			{
				cost = before.wordOrder - this.wordOrder; 
			}
			if(cost > 2 && (this.tokenType.equals("OT") || this.parent.tokenType.equals("OT")))
			{
				cost = 2; 
			}
			if(this.wordOrder < 0 || before.wordOrder < 0)
			{
				cost = 0; 
			}
			
			this.evaluationScore += 6*((double)cost)/tree.allNodes.size(); 
		}
		
		if(this.tokenType.equals("ROOT"))
		{
			for(int i = 0; i < this.children.size(); i++)
			{
				if(!this.children.get(i).tokenType.equals("CMT") && !this.children.get(i).tokenType.equals("OBT"))
				{
					this.children.get(i).evaluationScore += 10; 
				}
			}
		}
		if(this.tokenType.equals("CMT"))
		{
			for(int i = 0; i < this.children.size(); i++)
			{
				if(!(this.children.get(i).tokenType.equals("NT") || this.children.get(i).tokenType.endsWith("FT") 
						|| (this.tokenType.equals("OT") && this.children.get(i).children.size() == 1 
						&& this.children.get(i).children.get(0).tokenType.equals("VT") && !this.children.get(i).children.get(0).candidates.isEmpty())))
				{
					this.children.get(i).evaluationScore += 10; 
				}
			}			
		}
		else if(this.tokenType.equals("VT"))
		{
			for(int i = 0; i < this.children.size(); i++)
			{
				this.children.get(i).evaluationScore += 10; 
			}
		}
		else if(this.tokenType.equals("NA"))
		{
			for(int i = 0; i < this.children.size(); i++)
			{
				this.children.get(i).evaluationScore += 3; 
			}
		}
		else if(this.tokenType.equals("NT") && !this.candidates.isEmpty())
		{
			for(int i = 0; i < this.children.size(); i++)
			{
				BParseTreeNode child = this.children.get(i); 
				if((child.tokenType.equals("NT") || (child.tokenType.equals("VT")) && !child.candidates.isEmpty()))
				{
					int distance = db.distance(this.candidates.get(0), child.candidates.get(0)); 
					if(distance > 0)
					{
//						child.evaluationScore += distance - 1; 
					}
				}
			}
		}
		else if(this.tokenType.equals("FT") && this.function.equals("count"))
		{
			if(this.children.size() == 0)
			{
				this.evaluationScore += 10; 
				for(int i = 0; i < tree.allNodes.size(); i++)
				{
					if(tree.allNodes.get(i).tokenType.equals("NT"))
					{
						if(!tree.allNodes.get(i).equals(this))
						{
							tree.allNodes.get(i).evaluationScore += ((double)3)/tree.distance(this, tree.allNodes.get(i)); 
						}
					}
				}
			}
			
			if(this.parent.tokenType.equals("NT"))
			{
				ArrayList<BParseTreeNode> list = this.getFollowers(); 
				boolean exist = false; 
				for(int i = 0; i < list.size(); i++)
				{
					if(list.get(i).tokenType.equals("VT") && list.get(i).pos.equals("CD") && list.get(i).candidates.isEmpty())
					{
						exist = true; 
					}
				}
				
				if(exist == false)
				{
					this.evaluationScore += 5; 
				}
			}
			
			for(int i = 0; i < this.children.size(); i++)
			{
				BParseTreeNode child = this.children.get(i); 
				if(!child.tokenType.equals("NT"))
				{
					child.evaluationScore += 10; 
				}
				
				if(this.children.size() > 1)
				{
					child.evaluationScore += 3; 
				}
			}
		}
		else if(this.tokenType.equals("FT"))
		{
			if(this.children.size() == 0)
			{
				this.evaluationScore += 10; 
				for(int i = 0; i < tree.allNodes.size(); i++)
				{
					if(tree.allNodes.get(i).tokenType.equals("NT") || tree.allNodes.get(i).tokenType.equals("FT"))
					{
						if(!tree.allNodes.get(i).equals(this) && !tree.allNodes.get(i).parent.equals(this))
						{
							tree.allNodes.get(i).evaluationScore += ((double)3)/tree.distance(this, tree.allNodes.get(i)); 
						}
					}
				}
			}
			
			if(this.parent.tokenType.equals("NT"))
			{
				ArrayList<BParseTreeNode> list = this.getFollowers(); 
				boolean exist = false; 
				for(int i = 0; i < list.size(); i++)
				{
					if(list.get(i).tokenType.equals("VT") && list.get(i).pos.equals("CD") && list.get(i).candidates.isEmpty())
					{
						exist = true; 
					}
				}
				
				if(exist == false)
				{
					this.evaluationScore += 5; 
				}
			}
			
			for(int i = 0; i < this.children.size(); i++)
			{
				BParseTreeNode child = this.children.get(i); 
				if(!child.tokenType.equals("NT") && !child.tokenType.equals("FT"))
				{
					child.evaluationScore += 10; 
				}
				
				if(this.children.size() > 1)
				{
					child.evaluationScore += 3; 
				}
			}
		}
		else if(this.tokenType.equals("OT"))
		{
			if(this.parent.tokenType.equals("NT") && !this.parent.candidates.isEmpty() && this.children.size() == 1 && this.children.get(0).candidates.size() > 0
				&& this.children.get(0).tokenType.equals("VT") && this.children.get(0).pos.equals("CD"))
			{
				int distance = db.distance(this.parent.candidates.get(this.parent.choice), this.children.get(0).candidates.get(this.children.get(0).choice)); 
				if(distance > 1)
				{
					this.evaluationScore += distance-1; 
				}
			}
			
			if(this.relationship.endsWith("mod") && this.children.size() == 0)
			{
				this.evaluationScore += 10; 
			}
			
			if(this.parent.tokenType.equals("NT") && !this.parent.candidates.isEmpty())
			{
				int maxDistance = 0; 
				for(int i = 0; i < this.children.size(); i++)
				{
					if(!this.children.get(i).candidates.isEmpty())
					{
						int distance = db.distance(this.parent.candidates.get(choice), this.children.get(i).candidates.get(0)); 
						if(distance > maxDistance)
						{
							maxDistance = distance; 
						}
					}
				}
				
				if(maxDistance > 0)
				{
//					this.evaluationScore += maxDistance - 1; 
				}
			}
			
			if(this.children.size() == 2)
			{
				if((this.children.get(0).tokenType.equals("VT")) && (this.children.get(1).tokenType.equals("VT")))
				{
					this.children.get(0).evaluationScore += 3; 
					this.children.get(1).evaluationScore += 3; 
				}
				else if(!this.children.get(0).candidates.isEmpty() && !this.children.get(1).candidates.isEmpty())
				{
					int distance = db.distance(this.children.get(0).candidates.get(choice), this.children.get(1).candidates.get(choice)); 
					if(distance > 0)
					{
						this.children.get(0).evaluationScore += distance - 1; 
						this.children.get(1).evaluationScore += distance - 1; 
					}	
				}
			}
			
			int NTVT = 0; 
			int VT = 0; 			
			for(int i = 0; i < this.children.size(); i++)
			{
				if(this.children.get(i).tokenType.equals("VT") && !this.children.get(i).candidates.isEmpty() && this.children.get(i).pos.equals("CD"))
				{
					NTVT++; 
					VT++; 
				}
				else if(this.children.get(i).tokenType.equals("VT") || this.children.get(i).tokenType.equals("NT") || this.children.get(i).tokenType.equals("FT"))
				{
					NTVT++; 
				}
			}
			if(NTVT < 2 && VT < 1)
			{
				this.evaluationScore += 5; 
				for(int i = 0; i < tree.allNodes.size(); i++)
				{
					if(tree.allNodes.get(i).tokenType.equals("NT") || tree.allNodes.get(i).tokenType.equals("VT") || tree.allNodes.get(i).tokenType.equals("FT"))
					{
						if(!tree.allNodes.get(i).equals(this) && !tree.allNodes.get(i).equals(this.parent) && !tree.allNodes.get(i).parent.equals(this))
						{
							tree.allNodes.get(i).evaluationScore += ((double)3)/tree.distance(this, tree.allNodes.get(i)); 
							if(NTVT == 0)
							{
								tree.allNodes.get(i).evaluationScore += ((double)3)/tree.distance(this, tree.allNodes.get(i)); 								
							}
						}
					}
				}
			}
			
			for(int i = 0; i < this.children.size(); i++)
			{
				BParseTreeNode child = this.children.get(i); 
				if(!child.tokenType.equals("NT") && !child.tokenType.equals("VT") && !child.tokenType.equals("FT"))
				{
					child.evaluationScore += 10; 
				}
				
				if(this.children.size() > 2)
				{
					child.evaluationScore += 3; 
				}
			}
		}		
	}
	
	public ArrayList<BParseTreeNode> getFollowers()
	{
		LinkedList<BParseTreeNode> queue = new LinkedList<BParseTreeNode>(); 
		ArrayList<BParseTreeNode> result = new ArrayList<BParseTreeNode>(); 
		queue.add(this); 
		while(queue.isEmpty())
		{
			BParseTreeNode node = queue.removeFirst(); 
			if(!node.equals(this))
			{
				result.add(node);
			}
			for(int i = 0; i < node.children.size(); i++)
			{
				queue.add(node.children.get(i)); 
			}
		}
		
		return result; 
	}
	
	public void printForCheck()
	{
		System.out.print("nodeID: " + nodeID + "; "); 
		System.out.print("label: " + label + "; "); 
		System.out.print("tokenType: " + tokenType + "; "); 
		System.out.print("wordOrder: " + wordOrder + "; "); 
		System.out.print(pos + " " + relationship + " " + function + "; "); 
		System.out.print("prep: " + this.prep + "; "); 
		if(parent != null)
		{
			System.out.print("parent " + parent.nodeID  + "; "); 
		}
		if(!leftRel.isEmpty())
		{
			System.out.print("leftRel: " + leftRel + "; "); 
		}
		if(!candidates.isEmpty())
		{
			System.out.print("map: "); 
		}
		for(int i = 0; i < candidates.size(); i++)
		{
			candidates.get(i).printForCheck(); 
			System.out.print("; "); 
		}
		
		System.out.println(); 
	}

}