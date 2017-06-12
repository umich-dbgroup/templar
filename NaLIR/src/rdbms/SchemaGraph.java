package rdbms;

import java.util.ArrayList;

public class SchemaGraph 
{
	public ArrayList<Relation> relations = new ArrayList<Relation>(); 
	public ArrayList<SchemaElement> nodes = new ArrayList<SchemaElement>(); 
	public ArrayList<Edge> edges = new ArrayList<Edge>(); 
	public int [][] distance; 
	public int [][] shortestDistance; 
	public int [][] pre; 

	public void buildGraph(ArrayList<Relation> relations, ArrayList<Edge> edges)
	{
		this.relations = relations; 
		this.edges = edges; 
		
		distance = new int [this.relations.size()][this.relations.size()]; 
		shortestDistance = new int [this.relations.size()][this.relations.size()];
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
			distance[edge.left.defaultAtt().relation.relNum][edge.right.defaultAtt().relation.relNum] = 1; 
			distance[edge.right.defaultAtt().relation.relNum][edge.left.defaultAtt().relation.relNum] = 1; 
		}
		
		for(int i = 0; i < relations.size(); i++)
		{
			distance[i][i] = 0; 
		}
		
		pre = new int [relations.size()][relations.size()]; 
		for(int i = 0; i < pre.length; i++)
		{
			for(int j = 0; j < pre.length; j++)
			{
				pre[i][j] = j; 
			}
		}
		for(int i = 0; i < relations.size(); i++)
		{
			dijkstra(i); 
		}
	}
	
	public ArrayList<Edge> getPath(int source, int distination)
	{
		ArrayList<Integer> nodeList = new ArrayList<Integer>(); 
		ArrayList<Edge> edgeList = new ArrayList<Edge>(); 
		int current = distination; 
		int previous = pre[source][distination]; 
		while(current != previous)
		{
			nodeList.add(current);
			current = previous; 
			previous = pre[source][previous]; 
		}
		nodeList.add(current); 
		if(current != source)
		{
			nodeList.add(source); 
		}
		
		for(int i = 0; i < nodeList.size()-1; i++)
		{
			Edge edge = getEdge(relations.get(nodeList.get(i)), relations.get(nodeList.get(i+1))); 
			edgeList.add(edge); 
		}
		
		return edgeList; 
	}
	
	public void dijkstra(int source)
	{
		int [] localDistance = new int [relations.size()]; 
		for(int i = 0; i < localDistance.length; i++)
		{
			localDistance[i] = distance[source][i]; 
		}
		pre[source][source] = source;
		boolean [] dealt = new boolean[relations.size()]; 
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
	
	public Edge getEdge(Relation left, Relation right)
	{
		for(int i = 0; i < this.edges.size(); i++)
		{
			Edge edge = this.edges.get(i); 
			if((edge.left.defaultAtt().relation.equals(left) && edge.right.defaultAtt().relation.equals(right)) 
				|| (edge.left.defaultAtt().relation.equals(right) && edge.right.defaultAtt().relation.equals(left)))
			{
				return edge; 
			}
		}
		return null; 
	}
	
	public void printForCheck()
	{
		for(int i = 0; i < relations.size(); i++)
		{
			System.out.println(i + ":" + relations.get(i).relName); 
		}
		
		System.out.print("\t"); 
		for(int i = 0; i < relations.size(); i++)
		{
			System.out.print(i + "\t"); 
		}
		System.out.println(); 

		for(int i = 0; i < distance.length; i++)
		{
			System.out.print(i + "\t"); 
			for(int j = 0; j < distance.length; j++)
			{
				System.out.print(shortestDistance[i][j] + "\t"); 
			}
			System.out.println(); 
		}
		System.out.println(); 

		for(int i = 0; i < distance.length; i++)
		{
			System.out.print(i + "\t"); 
			for(int j = 0; j < distance.length; j++)
			{
				System.out.print(pre[i][j] + "\t"); 
			}
			System.out.println(); 
		}
	}
}
