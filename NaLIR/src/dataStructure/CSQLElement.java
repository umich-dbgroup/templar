package dataStructure;

public class CSQLElement 
{
	public CBlock block; 
	public String type = "normal"; // normal or inner; 
	public BParseTreeNode mappedNode; 
	public String function = ""; 
	public String alias = "";
	public String preAlias = ""; 
	
	public CSQLElement(CBlock block, String type, BParseTreeNode mappedNode, String function, String alias)
	{
		this.block = block; 
		this.type = type; 
		this.mappedNode = mappedNode; 
		this.function = function; 
		this.alias = alias; 
	}
	
	public String selectString()  
	{
		String result = ""; 
		if(type.equals("normal"))
		{
			if(!function.isEmpty())
			{
				result += function + "("; 
			}
			
			if(preAlias.isEmpty())
			{
				result += mappedNode.candidates.get(mappedNode.choice).defaultAtt().relation.relName + "." + mappedNode.candidates.get(mappedNode.choice).defaultAtt().attName; 
			}
			else
			{
				result += preAlias; 
			}
			
			if(!function.isEmpty())
			{
				result += ")"; 
			}
			
			if(!alias.isEmpty())
			{
				result += " AS " + alias; 
			}
		}
		else
		{
			result += block.blockName + "."; 
			if(!alias.isEmpty())
			{
				 result += alias; 
			}
			else
			{
				 result += mappedNode.candidates.get(mappedNode.choice).defaultAtt().attName; 
			}
		}
		return result; 
	}	

	public String conditionString()
	{
		String result = ""; 
		if(block.blockName.isEmpty())
		{
			result += mappedNode.candidates.get(mappedNode.choice).defaultAtt().relation.relName + "."; 
		}
		else
		{
			result += block.blockName + "."; 
		}
		if(!alias.isEmpty())
		{
			 result += alias; 
		}
		else
		{
			 result += mappedNode.candidates.get(mappedNode.choice).defaultAtt().attName; 
		}
		
		return result; 
	}
}
