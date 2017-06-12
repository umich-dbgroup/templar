package dataStructure;

import java.util.ArrayList;

import rdbms.Edge;
import rdbms.RDBMS;
import rdbms.Relation;

public class CBlock 
{
	public int blockID; 
	public String blockName = ""; 
	public BParseTreeNode root; 
	public ArrayList<BParseTreeNode> nodes = new ArrayList<BParseTreeNode>(); 
	public ArrayList<CGroup> relatedGroups = new ArrayList<CGroup>();

	public CBlock outerBlock = null; 
	public ArrayList<CBlock> innerBlocks = new ArrayList<CBlock>(); 
	public ArrayList<BParseTreeNode> outerRelatedNodes = new ArrayList<BParseTreeNode>(); 
	public ArrayList<CSQLElement> innerRelatedElements = new ArrayList<CSQLElement>(); 

	public ArrayList<CSQLElement> selectList = new ArrayList<CSQLElement>(); 
	public ArrayList<ArrayList<Relation>> singleRelations = new ArrayList<ArrayList<Relation>>(); 
	public ArrayList<Relation> addedRelations = new ArrayList<Relation>(); 
	public ArrayList<Edge> edges = new ArrayList<Edge>(); 

	public RDBMS db; 
	public String SQL = ""; 
	
	public CBlock(int blockID, BParseTreeNode root, CBlock outerBlock, RDBMS db)
	{
		this.blockID = blockID; 
		this.root = root; 
		this.outerBlock = outerBlock; 
		this.db = db; 
	}
	
	public void translate(ArrayList<CGroup> relatedGroups)
	{
		this.relatedGroups = relatedGroups; 
		for(int i = 0; i < innerBlocks.size(); i++)
		{
			innerBlocks.get(i).translate(relatedGroups); 
		}
		this.getGroupByNodes(); 
		this.getInvolvedRelations(); 
		this.getJoinPath(); 
		
		SQL = ""; 
		if(this.outerBlock != null)  
		{
			SQL += "\n\t("; 
		}

		SQL += "SELECT DISTINCT ";
		selectTranslate(); 
		if(selectList.size() > 0)
		{
			SQL += selectList.get(0).selectString(); 
		}
		for(int i = 1; i < selectList.size(); i++)
		{
			SQL += ", " + selectList.get(i).selectString(); 
		}		
		if(this.outerBlock == null)  
		{
			SQL += "\n"; 
		}
		else
		{
			SQL += " "; 
		}
		
		SQL += "FROM ";
		ArrayList<String >elements = this.fromTranslate(); 
		if(elements.size() > 0)
		{
			SQL += elements.get(0); 
		}
		for(int i = 1; i < elements.size(); i++)
		{
			SQL += ", " + elements.get(i); 
		}
		if(this.outerBlock == null)  
		{
			SQL += "\n"; 
		}
		
		elements = this.whereTranslate();
		if(elements.size() > 0)
		{
			if(this.outerBlock == null)  
			{
				if(!SQL.endsWith("\n"))
				{
					SQL += "\n"; 
				}
			}
			else
			{
				SQL += " "; 
			}
			SQL += "WHERE ";
			SQL += elements.get(0); 
			for(int i = 1; i < elements.size(); i++)
			{
				if(!elements.get(i).startsWith(" AND") && !elements.get(i).startsWith(" OR") 
					&& !elements.get(i).startsWith(" or") && !elements.get(i).startsWith(" and") && !elements.get(i).startsWith(")"))
				{
					SQL += " and ";  
				}
				SQL += elements.get(i);
			}
			if(this.outerBlock == null)  
			{
				SQL += "\n"; 
			}
		}
		
		elements = this.groupByTranslate();
		if(elements.size() > 0)
		{
			if(this.outerBlock != null)  
			{
				SQL += " "; 
			}
			SQL += "GROUP BY ";
			SQL += elements.get(0); 
			for(int i = 1; i < elements.size(); i++)
			{
				SQL += ", " + elements.get(i); 
			}
			if(this.outerBlock == null)  
			{
				SQL += "\n"; 
			}
		}

		elements = this.orderByTranslate();
		if(elements.size() > 0)
		{
			if(this.outerBlock == null)  
			{
				SQL += "\n"; 
			}
			else
			{
				SQL += " "; 
			}
			SQL += "ORDER BY ";
			SQL += elements.get(0); 
			for(int i = 1; i < elements.size(); i++)
			{
				SQL += ", " + elements.get(i); 
			}
			if(this.outerBlock == null)  
			{
				SQL += "\n"; 
			}
		}

		if(this.outerBlock != null)  
		{
			SQL += ")";
			SQL += " " + blockName; 
		}
	}

	public void selectTranslate()
	{
		boolean [] include = new boolean [nodes.size()]; 
		for(int i = 0; i < include.length; i++)
		{
			include[i] = false; 
		}
		
		for(int i = 0; i < nodes.size(); i++)
		{
			if(nodes.get(i).tokenType.equals("CMT"))
			{
				for(int j = 0; j < nodes.get(i).children.size(); j++)
				{
					BParseTreeNode node = nodes.get(i).children.get(j); 
					if(node.tokenType.equals("FT") || node.tokenType.equals("NT"))
					{
						String alias = ""; 
						BParseTreeNode mappedNode = null; 
						String function = ""; 
						if(node.tokenType.equals("NT"))
						{
							mappedNode = node; 
							CSQLElement selectElement = new CSQLElement(this, "normal", mappedNode, function, alias); 
							this.selectList.add(selectElement); 
						}
						else if(node.tokenType.equals("FT"))
						{
							BParseTreeNode innerNode = node.children.get(0); 
							for(int k = 0; k < this.innerBlocks.size(); k++)
							{
								for(int in = 0; in < this.innerBlocks.get(k).selectList.size(); in++)
								{
									if(this.innerBlocks.get(k).selectList.get(in).mappedNode.equals(innerNode))
									{
										CSQLElement element = this.innerBlocks.get(k).selectList.get(in); 
										CSQLElement selectElement = new CSQLElement(element.block, "inner", element.mappedNode, element.function, element.alias); 
										this.selectList.add(selectElement); 
									}
								}
							}
						}
					}
				}
			}
		}
		
		if(root.tokenType.equals("FT") && this.innerBlocks.size() == 0)
		{
			BParseTreeNode mappedNode = root.children.get(0); 
			String function = root.function; 
			String alias = ""; 
			if(function.equals("count"))
			{
				alias = root.function + "_" + root.children.get(0).candidates.get(root.children.get(0).choice).defaultAtt().relation.relName; 
			}
			else
			{
				alias = root.function + "_" + root.children.get(0).candidates.get(root.children.get(0).choice).defaultAtt().relation.relName 
											+ root.children.get(0).candidates.get(root.children.get(0).choice).defaultAtt().attName; 
			}
			CSQLElement selectElement = new CSQLElement(this, "normal", mappedNode, function, alias); 
			this.selectList.add(selectElement); 
			
			for(int i = 0; i < outerRelatedNodes.size(); i++)
			{
				selectElement = new CSQLElement(this, "normal", outerRelatedNodes.get(i), "", ""); 
				this.selectList.add(selectElement); 
			}
		}
		else if(root.tokenType.equals("FT"))
		{
			CBlock innerBlock = this.innerBlocks.get(0); 
			for(int i = 0; i < innerBlock.selectList.size(); i++)
			{
				if(innerBlock.selectList.get(i).mappedNode.equals(innerBlock.root.children.get(0)))
				{
					String function = root.function; 
					String alias = ""; 
					alias = root.function + "_" + innerBlock.selectList.get(i).alias; 
					CSQLElement selectElement = new CSQLElement(this, "normal", innerBlock.root.children.get(0), function, alias); 
					selectElement.preAlias = innerBlock.blockName + "." + innerBlock.selectList.get(i).alias; 
					this.selectList.add(selectElement); 
				}
			}
		}
		
		for(int i = 0; i < this.nodes.size(); i++)
		{
			BParseTreeNode node = this.nodes.get(i); 
			if(!node.tokenType.equals("NT"))
			{
				continue; 
			}
			
			if(node.candidates.isEmpty())
			{
				continue; 
			}
			
			boolean ifExist = false; 
			for(int j = 0; j < this.selectList.size(); j++)
			{
				if(this.selectList.get(j).mappedNode != null && this.selectList.get(j).mappedNode.equals(node))
				{
					ifExist = true; 
				}
				else if(!node.candidates.isEmpty() && !this.selectList.get(j).mappedNode.candidates.isEmpty()
						&& this.selectList.get(j).mappedNode.candidates.get(this.selectList.get(j).mappedNode.choice).defaultAtt().equals(node.candidates.get(node.choice).defaultAtt()))
				{
					ifExist = true; 
				}
				else if(this.selectList.get(j).mappedNode != null && node.entityID > 0 && node.entityID == selectList.get(j).mappedNode.entityID)
				{
					ifExist = true; 
				}
			}
			if(ifExist == false)
			{
				CSQLElement selectElement = new CSQLElement(this, "normal", node, "", ""); 
				this.selectList.add(selectElement); 
			}
		}
		
		if(!this.root.tokenType.equals("FT") 
			|| (this.root.children.size() > 0 && this.root.children.get(0).children.size() > 0 && this.root.children.get(0).children.get(0).tokenType.equals("FT")))
		{
			this.getInnerEquals(); 
			int size = this.selectList.size(); 
			for(int i = 0; i < innerRelatedElements.size(); i++)
			{
				boolean ifExist = false; 
				BParseTreeNode innerNode = innerRelatedElements.get(i).mappedNode; 
				for(int k = 0; k < size; k++)
				{
					BParseTreeNode node = this.selectList.get(k).mappedNode; 
					if(innerNode.entityID == node.entityID)
					{
						ifExist = true; 
						break; 
					}
				}
				
				if(ifExist == false)
				{
					this.selectList.add(innerRelatedElements.get(i)); 
				}
			}		
		}
	}
	
	public ArrayList<String> fromTranslate()
	{
		ArrayList<String> elements = new ArrayList<String>(); 
		for(int i = 0; i < this.singleRelations.size(); i++)
		{
			for(int j = 0; j < this.singleRelations.get(i).size(); j++)
			{
				Relation relation = this.singleRelations.get(i).get(j); 
				if(!elements.contains(relation.relName))
				{
					elements.add(relation.relName); 
				}
			}
		}

		for(int i = 0; i < innerBlocks.size(); i++)
		{
			elements.add(innerBlocks.get(i).SQL); 
		}
		
		for(int i = 0; i < this.addedRelations.size(); i++)
		{
			elements.add(this.addedRelations.get(i).relName); 
		}
		
		return elements; 
	}
	
	public ArrayList<String> whereTranslate()
	{
		ArrayList<String> elements = new ArrayList<String>(); 
		
		for(int i = 0; i < this.nodes.size(); i++)
		{
			BParseTreeNode parent = this.nodes.get(i); 
			boolean ifAdded = false; 
	
			for(int j = 0; j < parent.children.size(); j++)
			{
				BParseTreeNode OT = parent.children.get(j); 
				if(parent.children.get(j).tokenType.equals("VT") && !parent.children.get(j).candidates.isEmpty() && !parent.tokenType.equals("OT"))
				{
					BParseTreeNode VT = parent.children.get(j); 
					String condition = VT.candidates.get(VT.choice).defaultAtt().relation.relName + "." + VT.candidates.get(VT.choice).defaultAtt().attName; 
					condition += " = "; 
					if(VT.candidates.get(VT.choice).defaultAtt().attType.equals("string"))
					{
						condition += "\""; 
					}
					if(VT.candidates.get(VT.choice).choice >= 0)
					{
						condition += VT.candidates.get(VT.choice).mappedValues.get(VT.candidates.get(VT.choice).choice); 
					}
					else
					{
						condition += VT.candidates.get(VT.choice).example; 
					}
					if(VT.candidates.get(VT.choice).defaultAtt().attType.equals("string"))
					{
						condition += "\""; 
					}
					elements.add(condition); 
					ifAdded = true;
					continue; 
				}
				else if(OT.tokenType.equals("OT") && OT.children.size() == 1 && OT.children.get(0).tokenType.equals("VT"))
				{
					BParseTreeNode VT = OT.children.get(0); 
					String condition = VT.candidates.get(VT.choice).defaultAtt().relation.relName + "." + VT.candidates.get(VT.choice).defaultAtt().attName; 
					condition += " " + OT.function + " "; 
					if(VT.candidates.get(VT.choice).defaultAtt().attType.equals("string"))
					{
						condition += "\""; 
					}
					if(VT.candidates.get(VT.choice).defaultAtt().attType.equals("num"))
					{
						condition += VT.label; 					
					}
					else
					{
						if(VT.candidates.get(VT.choice).choice >= 0)
						{
							condition += VT.candidates.get(VT.choice).mappedValues.get(VT.candidates.get(VT.choice).choice); 
						}
						else
						{
							condition += VT.candidates.get(VT.choice).example; 
						}
					}
					if(VT.candidates.get(VT.choice).defaultAtt().attType.equals("string"))
					{
						condition += "\""; 
					}
					elements.add(condition); 
					ifAdded = true;
				}
				else if(OT.tokenType.equals("OT") && OT.children.size() == 2)
				{
					BParseTreeNode left = OT.children.get(0); 
					if(left.tokenType.equals("FT"))
					{
						left = left.children.get(0); 
					}
					BParseTreeNode right = OT.children.get(1); 
					if(right.tokenType.equals("FT"))
					{
						right = right.children.get(0); 
					}
					
					String leftCondition = ""; 
					String rightCondition = ""; 
					if(left.tokenType.equals("NT"))
					{
						if(this.nodes.contains(left))
						{
							leftCondition += left.candidates.get(left.choice).defaultAtt().relation.relName + "." + left.candidates.get(left.choice).defaultAtt().attName;
						}
						else
						{
							for(int j0 = 0; j0 < this.innerBlocks.size(); j0++)
							{
								boolean ifExist = false; 
								CBlock innerBlock = this.innerBlocks.get(j0); 
								for(int k = 0; k < innerBlock.selectList.size(); k++)
								{
									CSQLElement innerElement = innerBlock.selectList.get(k); 
									if(innerElement.mappedNode.equals(left))
									{
										leftCondition += innerElement.conditionString(); 
										ifExist = true;  
										break; 
									}
								}
								if(ifExist == true)
								{
									break; 
								}
							}
						}
					}
					if(right.tokenType.equals("NT"))
					{
						if(this.nodes.contains(right))
						{
							rightCondition += right.candidates.get(right.choice).defaultAtt().relation.relName + "." + right.candidates.get(right.choice).defaultAtt().attName;
						}
						else
						{
							for(int j0 = 0; j0 < this.innerBlocks.size(); j0++)
							{
								boolean ifExist = false; 
								CBlock innerBlock = this.innerBlocks.get(j0); 
								for(int k = 0; k < innerBlock.selectList.size(); k++)
								{
									CSQLElement innerElement = innerBlock.selectList.get(k); 
									if(innerElement.mappedNode.equals(right))
									{
										rightCondition += innerElement.conditionString(); 
										ifExist = true;  
										break; 
									}
								}
								if(ifExist == true)
								{
									break; 
								}
							}
						}
					}
					else if(right.tokenType.equals("FT"))
					{
						if(right.parent.tokenType.equals("FT"))
						{
							right = right.parent; 
						}
						for(int in = 0; in < this.innerBlocks.size(); in++)
						{
							CBlock block = this.innerBlocks.get(in); 
							if(block.root.equals(right))
							{
								while(right.tokenType.equals("FT"))
								{
									right = right.children.get(0); 
								}
								for(int im = 0; im < block.selectList.size(); im++)
								{
									if(block.selectList.get(im).mappedNode.equals(right))
									{
										rightCondition += block.selectList.get(im).conditionString(); 
										break; 
									}
								}
								break; 
							}
						}
					}
					else if(right.tokenType.equals("VT"))
					{
						if(right.candidates.isEmpty())
						{
							rightCondition = right.label; 
						}
						else
						{
							if(!right.pos.equals("CD"))
							{
								rightCondition += "\""; 
							}
							if(right.candidates.get(right.choice).choice >= 0)
							{
								rightCondition += right.candidates.get(right.choice).mappedValues.get(right.candidates.get(right.choice).choice); 
							}
							else
							{
								rightCondition += right.candidates.get(right.choice).example; 
							}
							if(!right.pos.equals("CD"))
							{
								rightCondition += "\""; 
							}
						}
					}
					
					if(!leftCondition.isEmpty() || !rightCondition.isEmpty())
					{
						String condition = leftCondition + " " + OT.function + " " + rightCondition; 
						elements.add(condition); 
						ifAdded = true;
					}
				}
			}
			if(ifAdded == true)
			{
//==============================================================
			}
		}
		
		for(int i = 0; i < this.edges.size(); i++)
		{
			elements.add(edges.get(i).edgeToString()); 
		}
		
		for(int i = 0; i < this.innerRelatedElements.size(); i++)
		{
			CSQLElement element = this.innerRelatedElements.get(i); 
			for(int j = 0; j < this.nodes.size(); j++)
			{
				BParseTreeNode outerNode = this.nodes.get(j); 
				if(element.block.root.tokenType.equals("FT") && element.block.root.children.get(0).tokenType.equals("FT"))
				{
					continue; 
				}
				if(!outerNode.candidates.isEmpty() 
					&& outerNode.candidates.get(outerNode.choice).defaultAtt().equals(element.mappedNode.candidates.get(element.mappedNode.choice).defaultAtt()))
				{
					String result = outerNode.candidates.get(outerNode.choice).defaultAtt().relation.relName + "." 
							+ outerNode.candidates.get(outerNode.choice).defaultAtt().attName + " = " + element.conditionString(); 
					elements.add(result); 
					break; 
				}
			}
		}
		
		return elements; 
	}
	
	public ArrayList<String> groupByTranslate()
	{
		ArrayList<String> elements = new ArrayList<String>(); 
		for(int i = 0; i < this.outerRelatedNodes.size(); i++)
		{
			BParseTreeNode node = this.outerRelatedNodes.get(i); 
			if(!node.candidates.isEmpty() && this.root.tokenType.equals("FT") && this.root.function.equals("count"))
			{
				String element = node.candidates.get(node.choice).defaultAtt().relation.relName + "." +node.candidates.get(node.choice).defaultAtt().attName;  
				elements.add(element); 
			}
		}
		
//		for(int i = 0; i < this.innerRelatedElements.size(); i++)
//		{
//			CSQLElement element = this.innerRelatedElements.get(i); 
//			for(int j = 0; j < this.nodes.size(); j++)
//			{
//				ParsedTreeNode outerNode = this.nodes.get(j); 
//				if(outerNode.matchingElement != null && outerNode.matchingElement.attribute.equals(element.mappedNode.matchingElement.attribute))
//				{
//					String result = outerNode.matchingElement.attribute.relation.relName + "." + outerNode.matchingElement.attribute.attName; 
//					elements.add(result); 
//					break; 
//				}
//			}
//		}
		
		for(int i = 0; i < this.nodes.size(); i++)
		{
			BParseTreeNode node = this.nodes.get(i); 
			if(node.tokenType.equals("NT") && node.function.equals("each"))
			{
				elements.add(node.candidates.get(node.choice).defaultAtt().relation.relName + '.' + node.candidates.get(node.choice).defaultAtt().attName); 
			}
		}
		
		return elements; 
	}
	
	public ArrayList<String> orderByTranslate()
	{
		ArrayList<String> elements = new ArrayList<String>(); 

		return elements; 
	}
	
	public void getInvolvedRelations()
	{
		for(int i = 0; i < this.relatedGroups.size(); i++)
		{
			ArrayList<Relation> relations = new ArrayList<Relation>(); 
			CGroup group = relatedGroups.get(i); 
			for(int j = 0; j < this.nodes.size(); j++)
			{
				if(group.nodes.contains(this.nodes.get(j)))
				{
					if(!relations.contains(this.nodes.get(j).candidates.get(this.nodes.get(j).choice).defaultAtt().relation))
					{
						relations.add(this.nodes.get(j).candidates.get(this.nodes.get(j).choice).defaultAtt().relation); 
					}
				}
			}
			if(!relations.isEmpty())
			{
				this.singleRelations.add(relations); 
			}
		}
	}
		
	public void getGroupByNodes()
	{
		if(this.outerBlock == null)
		{
			return; 
		}
		for(int i = 0; i < relatedGroups.size(); i++)
		{
			for(int j = 0; j < nodes.size(); j++)
			{
				if(relatedGroups.get(i).nodes.contains(nodes.get(j)))
				{
					CGroup group = relatedGroups.get(i); 
					outerRelatedNodes.addAll(group.nodes); 
					break; 
				}
			}
		}
		
		ArrayList<BParseTreeNode> deleteList = new ArrayList<BParseTreeNode>(); 
		for(int i = 0; i < outerRelatedNodes.size(); i++)
		{
			if(this.outerBlock != null)
			{
				if(this.nodes.contains(outerRelatedNodes.get(i)) || !outerBlock.nodes.contains(outerRelatedNodes.get(i)))
				deleteList.add(outerRelatedNodes.get(i)); 
			}
		}
		outerRelatedNodes.removeAll(deleteList); 
	}
	
	public void getInnerEquals()
	{
		ArrayList<CGroup> includeGroup = new ArrayList<CGroup>(); 
		for(int i = 0; i < nodes.size(); i++)
		{
			BParseTreeNode curNode = nodes.get(i); 
			for(int j = 0; j < relatedGroups.size(); j++)
			{
				CGroup group = relatedGroups.get(j); 
				if(group.nodes.contains(curNode))
				{
					if(includeGroup.contains(group))
					{
						continue; 
					}
					else
					{
						includeGroup.add(group); 
					}
					for(int k = 0; k < group.nodes.size(); k++)
					{
						for(int in = 0; in < this.innerBlocks.size(); in++)
						{
							CSQLElement element = this.innerBlocks.get(in).findSelectElementForNode(group.nodes.get(k)); 
							if(element != null)
							{
								CSQLElement newElement = new CSQLElement(element.block, "inner", element.mappedNode, element.function, element.alias); 
								innerRelatedElements.add(newElement); 
							}
						}
					}
				}
			}
		}
		
		for(int i = 0; i < this.innerBlocks.size(); i++)
		{
			CBlock innerBlock = this.innerBlocks.get(i); 
			for(int j = 0; j < innerBlock.selectList.size(); j++)
			{
				boolean ifExist = false; 
				for(int k = 0; k < innerRelatedElements.size(); k++)
				{
					if(innerRelatedElements.get(k).mappedNode.equals(innerBlock.selectList.get(j).mappedNode))
					{
						ifExist = true; 
					}
				}
				
				if(ifExist == false)
				{
					CSQLElement element = innerBlock.selectList.get(j); 
					CSQLElement newElement = new CSQLElement(element.block, "inner", element.mappedNode, element.function, element.alias); 
					innerRelatedElements.add(newElement);
				}
			}
		}
	}

	public CSQLElement findSelectElementForNode(BParseTreeNode node)
	{
		for(int i = 0; i < this.selectList.size(); i++)
		{
			if(this.selectList.get(i).mappedNode.equals(node))
			{
				return this.selectList.get(i); 
			}
		}
		return null; 
	}
	
	public void getJoinPath()
	{
		for(int i = 0; i < singleRelations.size(); i++)
		{
			ArrayList<Relation> rels = singleRelations.get(i); 
			if(rels.size() == 1)
			{
				continue; 
			}
			
			for(int j = 1; j < rels.size(); j++)
			{
				ArrayList<Edge> curEdges = db.schemaGraph.getPath(rels.get(0).relNum, rels.get(j).relNum);  
				edges.addAll(curEdges); 
				for(int k = 0; k < curEdges.size(); k++)
				{
					Relation left = curEdges.get(k).left.defaultAtt().relation; 
					boolean leftExist = false; 
					for(int in = 0; in < rels.size(); in++)
					{
						if(rels.get(in).relName.equals(left.relName))
						{
							leftExist = true; 
						}
					}
					for(int in = 0; in < addedRelations.size(); in++)
					{
						if(addedRelations.get(in).relName.equals(left.relName))
						{
							leftExist = true; 
						}
					}
					if(leftExist == false)
					{
						addedRelations.add(left); 
					}

					Relation right = curEdges.get(k).right.defaultAtt().relation; 
					boolean rightExist = false; 
					for(int in = 0; in < rels.size(); in++)
					{
						if(rels.get(in).relName.equals(right.relName))
						{
							rightExist = true; 
						}
					}
					for(int in = 0; in < addedRelations.size(); in++)
					{
						if(addedRelations.get(in).relName.equals(right.relName))
						{
							rightExist = true; 
						}
					}
					if(rightExist == false)
					{
						addedRelations.add(right); 
					}

				}
			}
		}		
	}
	
	public void printForCheck()
	{
		System.out.print("block_" + this.blockID + ": "); 
		System.out.print("nodes: "); 
		for(int i = 0; i < this.nodes.size(); i++)
		{
			System.out.print(this.nodes.get(i).nodeID + "; "); 
		}
		if(this.outerBlock != null)
		{
			System.out.print("outer: block_" + this.outerBlock.blockID + "; "); 
		}
		if(!this.innerBlocks.isEmpty())
		{
			System.out.print("inners: "); 
			for(int i = 0; i < this.innerBlocks.size(); i++)
			{
				System.out.print("block_" + this.innerBlocks.get(i).blockID + "; "); 
			}
		}
		if(!this.outerRelatedNodes.isEmpty())
		{
			System.out.print("outerRelatedNodes: "); 
			for(int i = 0; i < this.outerRelatedNodes.size(); i++)
			{
				System.out.print(this.outerRelatedNodes.get(i).nodeID + "; "); 
			}
		}
		if(!this.singleRelations.isEmpty())
		{
			System.out.print("singleRelations: "); 
			for(int i = 0; i < this.singleRelations.size(); i++)
			{
				for(int j = 0; j < this.singleRelations.get(i).size(); j++)
				{
					System.out.print(this.singleRelations.get(i).get(j).relName + "; "); 
				}
			}
		}
		if(!edges.isEmpty())
		{
			System.out.print("edges: "); 
			for(int i = 0; i < this.edges.size(); i++)
			{
				this.edges.get(i).printForCheck(); 
			}
		}
		if(this.outerBlock == null)
		{
			System.out.println(); 
		}
		System.out.println(this.SQL); 
		if(this.outerBlock != null)
		{
			System.out.println(); 
		}
	}
}