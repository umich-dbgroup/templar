package rdbms;

public class Edge 
{
	public SchemaElement left; 
	public SchemaElement right; 
	
	public Edge(SchemaElement left, SchemaElement right)
	{
		this.left = left; 
		this.right = right; 
	}
	
	public String edgeToString()
	{
		String result = ""; 
		result += left.defaultAtt().relation.relName + "." + left.defaultAtt().attName + " = " + right.defaultAtt().relation.relName + "." + right.defaultAtt().attName; 
		return result; 
	}
	
	public void printForCheck()
	{
		left.printForCheck(); 
		System.out.print(" = "); 
		right.printForCheck(); 
		System.out.print("; "); 
	}
}
