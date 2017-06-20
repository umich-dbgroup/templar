package rdbms;

import java.util.ArrayList;

public class AttributeGraph 
{
	public ArrayList<SchemaElement> nodes = new ArrayList<SchemaElement>(); 
	public ArrayList<Edge> edges = new ArrayList<Edge>(); 
	public int [][] distance; 
	public int [][] shortestDistance; 
	public int [][] pre; 

	public void buildGraph(ArrayList<Relation> relations, ArrayList<Edge> edges)
	{
		this.edges = edges; 
		
		for(int i = 0; i < relations.size(); i++)
		{
			Relation rel = relations.get(i); 
			SchemaElement relNode = new SchemaElement(rel); 
			nodes.add(relNode); 
			
			for(int j = 0; j < rel.allAtts.size(); j++)
			{
				Attribute att = rel.allAtts.get(j); 
				if(!att.attType.equals("fk") && !att.attType.equals("pk"))
				{
					SchemaElement attNode = new SchemaElement(att); 
					nodes.add(attNode); 
				}
			}
		}

		distance = new int [nodes.size()][nodes.size()]; 
		shortestDistance = new int [nodes.size()][nodes.size()];
		for(int i = 0; i < distance.length; i++)
		{
			for(int j = 0; j < distance.length; j++)
			{
				distance[i][j] = 999; 
			}
		}
		
		for(int i = 0; i < edges.size(); i++)
		{
			Edge edge = edges.get(i); 
			distance[this.searchNode(edge.left.attribute.relation)][this.searchNode(edge.right.attribute.relation)] = 1; 
			distance[this.searchNode(edge.right.attribute.relation)][this.searchNode(edge.left.attribute.relation)] = 1; 
		}
		
		for(int i = 0; i < relations.size(); i++)
		{
			Relation rel = relations.get(i); 
			for(int j = 0; j < rel.allAtts.size(); j++)
			{
				Attribute att = rel.allAtts.get(j); 
				if(!att.attType.equals("pk") && !att.attType.equals("fk"))
				{
					distance[this.searchNode(rel)][this.searchNode(att)] = 1; 
					distance[this.searchNode(att)][this.searchNode(rel)] = 1; 
				}
			}
		}
		
		for(int i = 0; i < nodes.size(); i++)
		{
			distance[i][i] = 0; 
		}
		
		pre = new int [nodes.size()][nodes.size()]; 
		for(int i = 0; i < pre.length; i++)
		{
			for(int j = 0; j < pre.length; j++)
			{
				pre[i][j] = j; 
			}
		}
		for(int i = 0; i < nodes.size(); i++)
		{
			dijkstra(i); 
		}
	}

	public void dijkstra(int source)
	{
		int [] localDistance = new int [nodes.size()]; 
		for(int i = 0; i < localDistance.length; i++)
		{
			localDistance[i] = distance[source][i]; 
		}
		pre[source][source] = source;
		boolean [] dealt = new boolean[nodes.size()]; 
		for(int i = 0; i < dealt.length; i++)
		{
			dealt[i] = false; 
		}
		dealt[source] = true; 
		
		boolean finished = false;
		while(finished == false)
		{
			int minDistance = 999; 
			int minOrder = -1; 
			for(int i = 0; i < distance.length; i++)
			{
				if(dealt[i] == false && localDistance[i] < minDistance)
				{
					minDistance = localDistance[i]; 
					minOrder = i; 
				}
			}
			
			dealt[minOrder] = true; 
			for(int i = 0; i < distance.length; i++)
			{
				if(dealt[i] == false && localDistance[minOrder] + distance[minOrder][i] < localDistance[i])
				{
					localDistance[i] = localDistance[minOrder] + distance[minOrder][i]; 
					pre[source][i] = minOrder; 
				}				
			}			
			
			finished = true; 
			for(int i = 0; i < dealt.length; i++)
			{
				if(dealt[i] == false)
				{
					finished = false; 
				}
			}
		}
		
		for(int i = 0; i < localDistance.length; i++)
		{
			shortestDistance[source][i] = localDistance[i]; 
		}
	}
	
	public int distance(SchemaElement source, SchemaElement distination)
	{
		return shortestDistance[this.searchNode(source)][this.searchNode(distination)]; 
	}
	
	public int searchNode(SchemaElement element)
	{
		for(int i = 0; i < nodes.size(); i++)
		{
			if(nodes.get(i).type.equals("relation") && element.type.equals("relation"))
			{
				if(nodes.get(i).relation.relNum == element.relation.relNum)
				{
					return i; 
				}
			}
			if(nodes.get(i).type.equals("attribute") && !element.type.equals("relation"))
			{
				if(nodes.get(i).attribute.relation.relNum == element.attribute.relation.relNum && nodes.get(i).attribute.attNum == element.attribute.attNum)
				{
					return i; 
				}
			}
		}
		
		return -1; 
	}
	
	public int searchNode(Relation relation)
	{
		for(int i = 0; i < nodes.size(); i++)
		{
			if(!nodes.get(i).type.equals("relation"))
			{
				continue; 
			}
			if(nodes.get(i).relation.equals(relation))
			{
				return i; 
			}
		}
		
		return -1; 
	}
	
	public int searchNode(Attribute attribute)
	{
		for(int i = 0; i < nodes.size(); i++)
		{
			if(nodes.get(i).type.equals("relation"))
			{
				continue; 
			}
			if(nodes.get(i).attribute.equals(attribute))
			{
				return i; 
			}
		}
		
		return -1; 
	}
	
	public void printForCheck()
	{
		System.out.print("\t"); 
		for(int i = 0; i < this.nodes.size(); i++)
		{
			this.nodes.get(i).printForCheck(); 
			System.out.print("\t"); 
		}
		System.out.println(); 
		
		for(int i = 0; i < this.nodes.size(); i++)
		{
			this.nodes.get(i).printForCheck(); 
			System.out.print("\t"); 
			for(int j = 0; j < this.nodes.size(); j++)
			{
				System.out.print(this.shortestDistance[i][j] + "\t"); 
			}
			System.out.println(); 
		}
	}
}
