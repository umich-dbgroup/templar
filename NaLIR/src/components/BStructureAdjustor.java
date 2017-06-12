package components;

import java.util.ArrayList;
import java.util.LinkedList;

import rdbms.Attribute;
import rdbms.RDBMS;
import tools.Functions;
import dataStructure.BParseTree;
import dataStructure.BParseTreeNode;
import edu.northwestern.at.morphadorner.corpuslinguistics.lemmatizer.EnglishLemmatizer;

public class BStructureAdjustor 
{
	RDBMS db; 
	BParseTree parseTree; 

	ArrayList<BParseTree> treeList; 
	public int generalChoice = 0; 
	ArrayList<BParseTree> specificList; 
	public int specificChoice = 0; 
	
	DFeedbackGenerator feedbackGen; 
	ArrayList<BParseTreeNode> deleteList; 
	EnglishLemmatizer lemmatizer; 
	
	ArrayList<Integer> history = new ArrayList<Integer>(); 
	ArrayList<Integer> historyCost = new ArrayList<Integer>(); 
	
	public BStructureAdjustor(ArrayList<BParseTree> treeList, ArrayList<BParseTree> specificList, RDBMS db, DFeedbackGenerator feedbackGen, EnglishLemmatizer lemmatizer)
	{
		this.treeList = treeList; 
		this.specificList = specificList; 
		this.db = db; 
		this.feedbackGen = feedbackGen; 
		this.lemmatizer = lemmatizer; 
	}
	
	public void structureAdjust(BParseTree parseTree)
	{
		this.parseTree = (BParseTree)Functions.depthClone(parseTree); 
		while(!treeList.isEmpty())
		{
			treeList.remove(0); 
		}
		while(!specificList.isEmpty())
		{
			specificList.remove(0); 
		}
		this.generalChoice = 0; 
		this.specificChoice = 0; 
		
		deleteList = new ArrayList<BParseTreeNode>(); 
		this.orderBy(); 
		this.QTMergeNT(); 
		this.NEG(); 
		this.OTLargerOREqual(); 
		this.deleteUselessNodes(); 
		feedbackGen.deleteUseless(deleteList); // feedback the deleteList information; 
		
		this.autoAdjust(); 
		for(int i = 0; i < treeList.size(); i++)
		{
			treeList.get(i).addImplicit(lemmatizer); 
			treeList.get(i).evaluate(db); 
		}
		feedbackGen.translateBack(treeList, "general", this.generalChoice); 

		this.specificAdjust(); 
		feedbackGen.translateBack(specificList, "specific", this.specificChoice); 

		this.entityResolution(); 
	}
	
	public void generalChoice(int choice)
	{
		while(!specificList.isEmpty())
		{
			specificList.remove(0); 
		}

		this.generalChoice = choice; 
		feedbackGen.translateBack(treeList, "general", this.generalChoice); 
		
		this.specificAdjust(); 
		feedbackGen.translateBack(specificList, "specific", this.specificChoice); 
		
		this.entityResolution(); 
	}
	
	public void specificChoice(int choice)
	{
		this.specificChoice = choice; 
		feedbackGen.translateBack(specificList, "specific", this.specificChoice); 

		this.entityResolution(); 
	}
	
	public void entityResolution()
	{
		for(int i = 0; i < parseTree.allNodes.size(); i++)
		{
			BParseTreeNode OT = parseTree.allNodes.get(i); 
			if(!OT.tokenType.equals("OT") || OT.children.size() != 2 || OT.parent == null || !OT.parent.tokenType.equals("NT"))
			{
				continue; 
			}
			
			BParseTreeNode left = OT.children.get(0); 
			BParseTreeNode right = OT.children.get(1); 
			Attribute parentAtt = OT.parent.candidates.get(OT.parent.choice).defaultAtt(); 
			
			ArrayList<BParseTreeNode> leftList = new ArrayList<BParseTreeNode>(); 
			leftList.add(left); 
			while(!leftList.isEmpty())
			{
				left = leftList.remove(0); 
				if(left.tokenType.equals("NT") && !left.candidates.isEmpty())
				{
					Attribute leftAtt = left.candidates.get(left.choice).defaultAtt(); 
					if(parentAtt.attName.equals(leftAtt.attName) && parentAtt.relation.relName.equals(leftAtt.relation.relName))
					{
						left.entityID = OT.parent.entityID; 
						break; 
					}
				}
				leftList.addAll(left.children); 
			}
			
			ArrayList<BParseTreeNode> rightList = new ArrayList<BParseTreeNode>(); 
			rightList.add(right); 
			while(!rightList.isEmpty())
			{
				right = rightList.remove(0); 
				if(right.tokenType.equals("NT") && !right.candidates.isEmpty())
				{
					Attribute rightAtt = right.candidates.get(right.choice).defaultAtt(); 
					if(parentAtt.attName.equals(rightAtt.attName) && parentAtt.relation.relName.equals(rightAtt.relation.relName))
					{
						boolean ifSame = true; 
						for(int j = 0; j < right.children.size(); j++)
						{
							BParseTreeNode rightVT = right.children.get(j); 
							Attribute rightAttribute = null;  
							while(!rightVT.tokenType.equals("VT") && rightVT.children.size() > 0)
							{
								rightVT = rightVT.children.get(0); 
							}
							if(rightVT.tokenType.equals("VT") && rightVT.candidates.size() > 0)
							{
								rightAttribute = rightVT.candidates.get(rightVT.choice).defaultAtt();
								boolean exist = false; 
								for(int k = 0; k < left.children.size(); k++)
								{
									BParseTreeNode leftVT = right.children.get(j); 
									while(!leftVT.tokenType.equals("VT") && leftVT.children.size() > 0)
									{
										leftVT = leftVT.children.get(0); 
									}
									if(leftVT.tokenType.equals("VT") && left.candidates.size() > 0)
									{
										Attribute leftAttribute = leftVT.candidates.get(leftVT.choice).defaultAtt(); 
										if(leftAttribute.attName.equals(rightAttribute.attName) && leftAttribute.relation.relName.equals(rightAttribute.relation.relName))
										{
											exist = true; 
										}
									}
								}
								if(exist == false)
								{
									ifSame = false; 
								}
							}
						}
						if(ifSame == true)
						{
							right.entityID = OT.parent.entityID; 
							break; 
						}
					}
				}
				rightList.addAll(right.children); 
			}
		}		
	}
	
	public void specificAdjust()
	{
		parseTree = treeList.get(this.generalChoice); 
		specificList.add(parseTree); 
		BParseTree parseTree = (BParseTree)Functions.depthClone(specificList.get(0)); 
		for(int i = 0; i < parseTree.allNodes.size(); i++)
		{
			BParseTreeNode OT = parseTree.allNodes.get(i); 
			if(OT.tokenType.equals("OT") && OT.children.size() == 2 && !OT.children.get(0).tokenType.equals("VT") && !OT.children.get(1).tokenType.equals("VT"))
			{
				BParseTreeNode left = OT.children.get(0); 
				BParseTreeNode right = OT.children.get(1); 
				while(!left.children.isEmpty() && left.tokenType.equals("FT"))
				{
					left = left.children.get(0); 
				}
				while(!right.children.isEmpty() && right.tokenType.equals("FT"))
				{
					right = right.children.get(0); 
				}
				
				for(int j = 0; j < left.children.size(); j++)
				{
					BParseTreeNode leftChild = left.children.get(j); 
					while(!leftChild.children.isEmpty() && !leftChild.tokenType.equals("VT"))
					{
						leftChild = leftChild.children.get(0); 
					}
					if(leftChild.tokenType.equals("VT") && !leftChild.candidates.isEmpty())
					{
						Attribute leftAtt = leftChild.candidates.get(leftChild.choice).defaultAtt(); 
						ArrayList<BParseTreeNode> rightList = new ArrayList<BParseTreeNode>(); 
						rightList.add(right); 
						boolean exist = false; 
						while(!rightList.isEmpty())
						{
							BParseTreeNode rightNode = rightList.remove(0); 
							if(!rightNode.candidates.isEmpty())
							{
								Attribute rightAtt = rightNode.candidates.get(rightNode.choice).defaultAtt(); 
								if(leftAtt.attName.equals(rightAtt.attName) && leftAtt.relation.relName.equals(rightAtt.relation.relName))
								{
									exist = true; 
									break; 
								}
							}
							rightList.addAll(rightNode.children); 
						}
						
						if(exist == false)
						{
							parseTree.addTree(left.children.get(j), right, right.children.size()); 
							specificList.add(0, parseTree); 
							parseTree = (BParseTree)Functions.depthClone(specificList.get(0)); 
						}
					}
				}
				
				for(int j = 0; j < right.children.size(); j++)
				{
					BParseTreeNode rightChild = right.children.get(j); 
					while(!rightChild.children.isEmpty() && !rightChild.tokenType.equals("VT"))
					{
						rightChild = rightChild.children.get(0); 
					}
					if(rightChild.tokenType.equals("VT") && !rightChild.candidates.isEmpty())
					{
						Attribute rightAtt = rightChild.candidates.get(rightChild.choice).defaultAtt(); 
						ArrayList<BParseTreeNode> leftList = new ArrayList<BParseTreeNode>(); 
						leftList.add(left); 
						boolean exist = false; 
						while(!leftList.isEmpty())
						{
							BParseTreeNode leftNode = leftList.remove(0); 
							if(!leftNode.candidates.isEmpty())
							{
								Attribute leftAtt = leftNode.candidates.get(leftNode.choice).defaultAtt(); 
								if(rightAtt.attName.equals(leftAtt.attName) && rightAtt.relation.relName.equals(leftAtt.relation.relName))
								{
									exist = true; 
									break; 
								}
							}
							leftList.addAll(leftNode.children); 
						}
						
						if(exist == false)
						{
							parseTree.addTree(right.children.get(j), left, left.children.size()); 
							specificList.add(0, parseTree); 
							parseTree = (BParseTree)Functions.depthClone(specificList.get(0)); 
						}
					}
				}
			}
		}
		
		ArrayList<BParseTree> rankList = new ArrayList<BParseTree>();
		if(!specificList.isEmpty())
		{
			rankList.add(specificList.remove(specificList.size()-1)); 
		}
		while(!specificList.isEmpty())
		{
			rankList.add(0, specificList.remove(0)); 
		}
		while(!specificList.isEmpty())
		{
			specificList.remove(0); 
		}
		specificList.addAll(rankList); 
		this.parseTree = specificList.get(specificChoice); 		
	}
	
	public void removeInvalidTrees()
	{
		ArrayList<BParseTree> deletes = new ArrayList<BParseTree>(); 
		for(int i = 0; i < treeList.size(); i++)
		{
			BParseTree tree = treeList.get(i); 
			if(!tree.isValid())
			{
				deletes.add(tree); 
			}
		}
	}
	
	public void autoAdjust()
	{
		history = new ArrayList<Integer>(); 
		history.add(parseTree.hash()); 
		historyCost.add(parseTree.step); 
		LinkedList<BParseTree> queue = new LinkedList<BParseTree>(); 
		queue.add(parseTree); 

		parseTree.evaluate(db); 

		while(!queue.isEmpty())
		{
			BParseTree tree = queue.removeFirst(); 
			
			ArrayList<BParseTree> improveResults = improve(tree); 
			queue.addAll(improveResults); 
			if(improveResults.isEmpty())
			{
				treeList.add(tree); 
			}
		}
		
		for(int i = 0; i < treeList.size(); i++)
		{
			int minPosition = 0; 
			double minValue = 9999; 
			
			for(int j = i; j < treeList.size(); j++)
			{
				if((treeList.get(j).evaluationScore) < minValue)
				{
					minPosition = j; 
					minValue = treeList.get(j).evaluationScore; 
				}
			}
			
			BParseTree minTree = treeList.get(minPosition); 
			treeList.remove(minTree); 
			treeList.add(i, minTree); 
		}
		
		for(int i = 0; i < treeList.size(); i++)
		{
			this.deleteUselessNodes(treeList.get(i)); 
		}
		
		history = new ArrayList<Integer>(); 
		ArrayList<Integer> costs = new ArrayList<Integer>(); 
		ArrayList<Integer> positions = new ArrayList<Integer>(); 
		ArrayList<BParseTree> deleteTrees = new ArrayList<BParseTree>(); 
		for(int i = 0; i < treeList.size(); i++)
		{
			int index = history.indexOf(treeList.get(i).hash()); 
			if(index < 0)
			{
				history.add(treeList.get(i).hash());
				costs.add(treeList.get(i).step);
				positions.add(i); 
			}
			else
			{
				if(treeList.get(i).step < costs.get(index))
				{
					costs.set(index, treeList.get(i).step); 
					deleteTrees.add(treeList.get(positions.get(index))); 
				}
				else
				{
					deleteTrees.add(treeList.get(i)); 
				}
			}
		}
		treeList.removeAll(deleteTrees); 
	}
	
	public ArrayList<BParseTree> improve(BParseTree inputTree) 
	{
		BParseTree tree = inputTree; 
		ArrayList<BParseTree> improveResults = new ArrayList<BParseTree>(); 
		
		ArrayList<BParseTreeNode> rankedList = new ArrayList<BParseTreeNode>(); 
		rankedList.addAll(inputTree.allNodes); 
		
		for(int i = 0; i < rankedList.size(); i++)
		{
			double max = -1; 
			int maxPosition = 0; 
			for(int j = i; j < rankedList.size(); j++)
			{
				if(rankedList.get(j).evaluationScore > max)
				{
					max = rankedList.get(j).evaluationScore; 
					maxPosition = j; 
				}
			}
			BParseTreeNode maxNode = rankedList.get(maxPosition); 
			rankedList.remove(maxNode);  
			rankedList.add(i, maxNode); 
		}
		
		int K = 1; 
		if(tree.allNodes.size() < 17)
		{
			K = 2; 
		}
		if(tree.allNodes.size() < 14)
		{
			K = 3; 
		}
		else if(tree.allNodes.size() < 13)
		{
			K = 5; 
		}
				
		for(int i = 0; i < rankedList.size() && i < K; i++)
		{
			LinkedList<BParseTree> improveTrees = improveNode(inputTree, rankedList.get(i)); 
			improveResults.addAll(improveTrees); 
		}
		
		return improveResults; 
	}
	
	public LinkedList<BParseTree> improveNode(BParseTree inputTree, BParseTreeNode node)
	{
		LinkedList<BParseTree> improveResults = new LinkedList<BParseTree>(); 
		
		if(node.tokenType.equals("FT") && node.relationship.endsWith("mod") && node.children.size() == 0)
		{
			BParseTree tree = (BParseTree)Functions.depthClone(inputTree); 
			BParseTreeNode parent = tree.searchNodeByID(node.nodeID).parent; 
			tree.moveTree(tree.searchNodeByID(node.nodeID), tree.searchNodeByID(parent.nodeID).parent);
			tree.moveTree(tree.searchNodeByID(parent.nodeID), tree.searchNodeByID(node.nodeID)); 
			tree.searchNodeByID(node.nodeID).relationship = "NA"; 
			tree.evaluate(db); 
			tree.step += 1; 
			if(tree.evaluationScore+tree.step < inputTree.evaluationScore+inputTree.step)
			{
				int treeHash = tree.hash(); 
				int index = this.history.indexOf(treeHash); 
				if(index < 0)
				{
					improveResults.add(tree); 
					this.history.add(treeHash); 
					this.historyCost.add(tree.step); 
				}
				else if(this.historyCost.get(index) > tree.step)
				{
					historyCost.set(index, tree.step); 
					improveResults.add(tree); 
				}
			}
		}
		else if(node.tokenType.equals("FT"))
		{
			for(int i = 0; i < inputTree.allNodes.size(); i++)
			{
				BParseTreeNode curNode = inputTree.allNodes.get(i); 
				if(node.nodeID != curNode.nodeID 
					&& (curNode.tokenType.equals("OT") || curNode.tokenType.equals("NT") || (curNode.tokenType.equals("FT") && !curNode.function.equals("count"))))
				{
					BParseTree tree = (BParseTree)Functions.depthClone(inputTree); 
					tree.moveTree(tree.searchNodeByID(node.nodeID), tree.allNodes.get(i)); 
					tree.evaluate(db); 
					tree.step += inputTree.distance(node, curNode)-1; 
					
					if(tree.evaluationScore+tree.step < inputTree.evaluationScore+inputTree.step)
					{
						int treeHash = tree.hash(); 
						int index = this.history.indexOf(treeHash); 
						if(index < 0)
						{
							improveResults.add(tree); 
							this.history.add(treeHash); 
							this.historyCost.add(tree.step); 
						}
						else if(this.historyCost.get(index) > tree.step)
						{
							historyCost.set(index, tree.step); 
							improveResults.add(tree); 
						}
					}
				}
			}
		}
		else if(node.tokenType.equals("OT") && (node.relationship.endsWith("mod") || (node.relationship.equals("mwe"))))
		{
			if(node.parent.tokenType.equals("VT") && node.parent.candidates.isEmpty() && node.parent.parent.tokenType.equals("NT"))
			{
				BParseTree tree = (BParseTree)Functions.depthClone(inputTree); 
				BParseTreeNode parent = node.parent; 
				BParseTreeNode grandP = node.parent.parent; 
				tree.moveTree(tree.searchNodeByID(node.nodeID), tree.searchNodeByID(node.parent.parent.parent.nodeID)); 
				tree.moveTree(tree.searchNodeByID(grandP.nodeID), tree.searchNodeByID(node.nodeID)); 
				tree.moveTree(tree.searchNodeByID(parent.nodeID), tree.searchNodeByID(node.nodeID)); 
				node.relationship = "NA"; 
				tree.evaluate(db); 
				tree.step += 2; 
				if(tree.evaluationScore+tree.step < inputTree.evaluationScore+inputTree.step)
				{
					int treeHash = tree.hash(); 
					int index = this.history.indexOf(treeHash); 
					if(index < 0)
					{
						improveResults.add(tree); 
						this.history.add(treeHash); 
						this.historyCost.add(tree.step); 
					}
					else if(this.historyCost.get(index) > tree.step)
					{
						historyCost.set(index, tree.step); 
						improveResults.add(tree); 
					}
				}
			}
			else if(node.relationship.endsWith("mod") && node.parent.parent != null)
			{
				BParseTree tree = (BParseTree)Functions.depthClone(inputTree); 
				BParseTreeNode parent = tree.searchNodeByID(node.nodeID).parent; 
				tree.moveTree(tree.searchNodeByID(node.nodeID), tree.searchNodeByID(parent.parent.nodeID));
				tree.moveTree(tree.searchNodeByID(parent.nodeID), tree.searchNodeByID(node.nodeID)); 
				node.relationship = "NA"; 
				tree.evaluate(db); 
				tree.step += 1; 
				if(tree.evaluationScore+tree.step < inputTree.evaluationScore+inputTree.step)
				{
					int treeHash = tree.hash(); 
					int index = this.history.indexOf(treeHash); 
					if(index < 0)
					{
						improveResults.add(tree); 
						this.history.add(treeHash); 
						this.historyCost.add(tree.step); 
					}
					else if(this.historyCost.get(index) > tree.step)
					{
						historyCost.set(index, tree.step); 
						improveResults.add(tree); 
					}
				}
			}
		}
		else if(node.tokenType.equals("OT") 
			&& ((node.children.size() > 1) || (node.children.size() == 1 && node.children.get(0).tokenType.equals("VT") && !node.children.get(0).candidates.isEmpty())))
		{
			for(int i = 0; i < inputTree.allNodes.size(); i++)
			{
				BParseTreeNode curNode = inputTree.allNodes.get(i); 
				if(curNode.tokenType.equals("NT"))
				{
					BParseTree tree = (BParseTree)Functions.depthClone(inputTree); 
					tree.moveTree(tree.searchNodeByID(node.nodeID), tree.allNodes.get(i)); 
					tree.evaluate(db); 
					tree.step += inputTree.distance(node, curNode) - 1; 
				
					if(tree.evaluationScore+tree.step < inputTree.evaluationScore+inputTree.step)
					{
						int treeHash = tree.hash(); 
						int index = this.history.indexOf(treeHash); 
						if(index < 0)
						{
							improveResults.add(tree); 
							this.history.add(treeHash); 
							this.historyCost.add(tree.step); 
						}
						else if(this.historyCost.get(index) > tree.step)
						{
							improveResults.add(tree); 
							historyCost.set(index, tree.step); 
						}
					}
				}
			}			
		}
		else if(node.tokenType.equals("VT"))
		{
			for(int i = 0; i < inputTree.allNodes.size(); i++)
			{
				BParseTreeNode curNode = inputTree.allNodes.get(i); 
				if(!node.equals(curNode) && (curNode.tokenType.equals("OT") || curNode.tokenType.equals("NT")))
				{
					BParseTree tree = (BParseTree)Functions.depthClone(inputTree); 
					tree.moveTree(tree.searchNodeByID(node.nodeID), tree.allNodes.get(i)); 
					tree.evaluate(db); 
					tree.step += inputTree.distance(node, curNode)-1; 
					
					if(tree.evaluationScore+tree.step < inputTree.evaluationScore+inputTree.step)
					{
						int treeHash = tree.hash(); 
						int index = this.history.indexOf(treeHash); 
						if(index < 0)
						{
							improveResults.add(tree); 
							this.history.add(treeHash); 
							this.historyCost.add(tree.step); 
						}
						else if(this.historyCost.get(index) > tree.step)
						{
							historyCost.set(index, tree.step); 
							improveResults.add(tree); 
						}
					}
				}
			}
		}
		else if(node.tokenType.equals("NT"))
		{
			for(int i = 0; i < inputTree.allNodes.size(); i++)
			{
				BParseTreeNode curNode = inputTree.allNodes.get(i); 
				if(!node.equals(curNode) && (curNode.tokenType.equals("OT") || curNode.tokenType.equals("NT") || curNode.tokenType.equals("FT")))
				{
					BParseTree tree = (BParseTree)Functions.depthClone(inputTree); 
					tree.moveTree(tree.searchNodeByID(node.nodeID), tree.allNodes.get(i)); 
					tree.evaluate(db); 
					tree.step += inputTree.distance(node, curNode)-1; 
					
					if(tree.evaluationScore+tree.step < inputTree.evaluationScore+inputTree.step)
					{
						int treeHash = tree.hash(); 
						int index = this.history.indexOf(treeHash); 
						if(index < 0)
						{
							improveResults.add(tree); 
							this.history.add(treeHash); 
							this.historyCost.add(tree.step); 
						}
						else if(this.historyCost.get(index) > tree.step)
						{
							historyCost.set(index, tree.step); 
							improveResults.add(tree); 
						}
					}
				}
			}
		}

		return improveResults; 
	}
	
	public void orderBy() // move the subTree rooted at OBT to the root; 
	{
		for(int i = 0; i < parseTree.allNodes.size(); i++)
		{
			if(parseTree.allNodes.get(i).tokenType.equals("OBT"))
			{
				parseTree.moveTreeAfter(parseTree.allNodes.get(i), parseTree.root, parseTree.root.children.size()); 
			}
		}
	}
	
	public void QTMergeNT() // add each/all/any to NT; 
	{
		ArrayList<BParseTreeNode> deleteList = new ArrayList<BParseTreeNode>(); 
		for(int i = 0; i < parseTree.allNodes.size(); i++)
		{
			if(parseTree.allNodes.get(i).tokenType.equals("QT"))
			{
				parseTree.allNodes.get(i).parent.function = parseTree.allNodes.get(i).function; 
				deleteList.add(parseTree.allNodes.get(i)); 
			}
		}
		
		for(int i = 0; i < deleteList.size(); i++)
		{
			parseTree.deleteNode(deleteList.get(i)); 
		}
	}
	
	public void NEG() // change no large than to less than;  
	{
		ArrayList<BParseTreeNode> localDeleteList = new ArrayList<BParseTreeNode>(); 
		for(int i = 0; i < parseTree.allNodes.size(); i++)
		{
			BParseTreeNode curNode = parseTree.allNodes.get(i); 
			BParseTreeNode OT = null; 
			if(curNode.tokenType.equals("NEG"))
			{
				if(curNode.parent.tokenType.equals("OT"))
				{
					OT = curNode.parent; 
				}
				else if(curNode.getRightSibling() != null && curNode.getRightSibling().tokenType.equals("OT"))
				{
					OT = curNode.getRightSibling(); 
				}
				if(OT != null)
				{
					OT.label = "not " + OT.label; 
					if(OT.function.equals(">"))
					{
						OT.function = "<="; 
						OT.label = "no larger than"; 
					}
					else if(OT.function.equals("<"))
					{
						OT.function = ">="; 
						OT.label = "no smaller than"; 
					}
					else if(OT.function.equals("<="))
					{
						OT.function = ">"; 
						OT.label = "larger than"; 
					}
					else if(OT.function.equals(">="))
					{
						OT.function = "<"; 
						OT.label = "smaller than"; 
					}
					else if(OT.function.equals("="))
					{
						OT.function = "<>"; 
						OT.label = "equal to"; 
					}
					else if(OT.function.equals("<>"))
					{
						OT.function = "="; 
						OT.label = "not equal to"; 
					}
					localDeleteList.add(curNode); 
				}
				else if(curNode.parent.tokenType.equals("VT"))
				{
					curNode.parent.function = "not"; 
					localDeleteList.add(curNode); 
				}
				else
				{
					BParseTreeNode NT = null; 
					if(curNode.parent.tokenType.equals("NT"))
					{
						NT = curNode.parent; 
					}
					else if(curNode.getRightSibling() != null && curNode.getRightSibling().tokenType.equals("NT"))
					{
						NT = curNode.getRightSibling(); 
					}
					if(NT != null)
					{
						NT.function = "not"; 
						localDeleteList.add(curNode); 
					}
				}
			}
		}
		
		for(int i = 0; i < localDeleteList.size(); i++)
		{
			parseTree.deleteNode(localDeleteList.get(i)); 
		}
	}	
	
	public void OTLargerOREqual()
	{
		ArrayList<BParseTreeNode> localDeleteList = new ArrayList<BParseTreeNode>(); 
		for(int i = 0; i < parseTree.allNodes.size(); i++)
		{
			BParseTreeNode curNode = parseTree.allNodes.get(i); 
			if(curNode.tokenType.equals("OT") && curNode.leftRel.equals("or"))
			{
				BParseTreeNode left = parseTree.searchNodeByID(curNode.nodeID-2); 
				if(left != null && left.tokenType.equals("OT"))
				{
					String leftOP = left.function; 
					String rightOP = curNode.function; 
					if(left.children.size() == 0 || curNode.children.size() == 0)
					{
						if((leftOP.equals(">") && rightOP.equals("<")) || (leftOP.equals("<") && rightOP.equals(">")))
						{
							leftOP = "<>"; 
						}
						else if((leftOP.equals(">") && rightOP.equals("=")) || (leftOP.equals("=") && rightOP.equals(">")))
						{
							leftOP = ">="; 
						}
						else if((leftOP.equals("<") && rightOP.equals("=")) || (leftOP.equals("=") && rightOP.equals("<")))
						{
							leftOP = "<="; 
						}
						else
						{
							leftOP = null; 
						}

						left.label = left.label + " or " + curNode.label; 
						if(leftOP != null)
						{
							left.function = leftOP; 
						}
						for(int j = 0; curNode.children.size() > 0; j++)
						{
							parseTree.moveTree(curNode.children.get(0), left, left.children.size()+j); 
						}
						localDeleteList.add(curNode); 
					}
				}
			}
		}
		
		for(int i = 0; i < localDeleteList.size(); i++)
		{
			parseTree.deleteNode(localDeleteList.get(i)); 
		}
	}	


	public void deleteUselessNodes() // delete the words which cannot map to SQL fragments; 
	{
		ArrayList<BParseTreeNode> localDeleteList = new ArrayList<BParseTreeNode>(); 

		int treeSize = parseTree.allNodes.size(); 
		do
		{
			treeSize = parseTree.allNodes.size(); 
			for(int i = 0; i < parseTree.allNodes.size(); i++)
			{
				String type = parseTree.allNodes.get(i).tokenType;  
				if(!type.equals("NT") && !type.equals("VT") && !type.equals("OBT") && !type.equals("NEG") && !type.equals("QT") && !type.equals("CMT") && !type.equals("FT") 
					&& !type.equals("OT") && !type.equals("ROOT"))
				{
					if(parseTree.allNodes.get(i).children.size() < 2 || parseTree.allNodes.get(i).parent.children.size() == 1)
					{
						localDeleteList.add(parseTree.allNodes.get(i)); 
					}
				}
			}
			
			for(int i = 0; i < localDeleteList.size(); i++)
			{
				for(int j = 0; j < localDeleteList.get(i).children.size(); j++)
				{
					BParseTreeNode NTVT = localDeleteList.get(i).children.get(j); 
					if(localDeleteList.get(i).pos.equals("IN") && !localDeleteList.get(i).label.contains("than") && (NTVT.tokenType.equals("NT") || NTVT.tokenType.equals("VT")))
					{
						if(NTVT.prep.isEmpty())
						{
							NTVT.prep = localDeleteList.get(i).label; 
						}
					}
				}
				parseTree.deleteNode(localDeleteList.get(i)); 
			}
			localDeleteList = new ArrayList<BParseTreeNode>(); 
		}
		while(treeSize != parseTree.allNodes.size()); 
	}
	
	public void deleteUselessNodes(BParseTree parseTree) // delete the words which cannot map to SQL fragments; 
	{
		ArrayList<BParseTreeNode> localDeleteList = new ArrayList<BParseTreeNode>(); 
		for(int i = 0; i < parseTree.allNodes.size(); i++)
		{
			String type = parseTree.allNodes.get(i).tokenType;  
			if(!type.equals("NT") && !type.equals("VT") && !type.equals("OBT") && !type.equals("NEG") && !type.equals("QT")
				&& !type.equals("CMT") && !type.equals("FT")  && !type.equals("OT") && !type.equals("ROOT"))
			{
				localDeleteList.add(parseTree.allNodes.get(i)); 
			}
		}
		
		for(int i = 0; i < localDeleteList.size(); i++)
		{
			for(int j = 0; j < localDeleteList.get(i).children.size(); j++)
			{
				BParseTreeNode NTVT = localDeleteList.get(i).children.get(j); 
				if(localDeleteList.get(i).pos.equals("IN") && !localDeleteList.get(i).label.contains("than") && (NTVT.tokenType.equals("NT") || NTVT.tokenType.equals("VT")))
				{
					if(NTVT.prep.isEmpty())
					{
						NTVT.prep = localDeleteList.get(i).label; 
					}
				}
			}
			parseTree.deleteNode(localDeleteList.get(i)); 
		}
	}

	public void printForCheck()
	{
		parseTree.printForCheck(); 
		for(int i = 0; i < this.specificList.size() && i < 5; i++)
		{
			this.specificList.get(i).printForCheck(); 
		}
	}
}
