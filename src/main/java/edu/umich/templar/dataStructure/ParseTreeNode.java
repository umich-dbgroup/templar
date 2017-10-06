package edu.umich.templar.dataStructure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.umich.templar.rdbms.MappedSchemaElement;

@SuppressWarnings("serial")
public class ParseTreeNode implements Serializable
{
	static int NODEID = 0; 
	public int nodeID; 
	public int wordOrder = -1; // word order in the sentence; 
	public String label; // word itself; 
	public String pos = "NA"; // part of speech; 
	public String relationship = "NA"; // dependency relationship; 
	public String tokenType = "NA"; // CMT, NT, VT and so forth; 
	public String function = "NA"; // only exist in OT and FT;

    // Added by cjbaik
	public String attachedOT = null; // the attached OT function if it's a VT
    public String attachedSuperlative = null; // the attached superlative (min/max) if exists

	public ParseTreeNode parent; 
	public List<ParseTreeNode> children = new ArrayList<ParseTreeNode>();

	public String QT = ""; 
	public String prep = ""; 
	public String leftRel = ""; 
	public List<MappedSchemaElement> mappedElements = new ArrayList<MappedSchemaElement>();
	public int choice = -1; 
	public boolean isAdded = false; 
	
	public ParseTreeNode(int wordOrder, String label, String pos, String relationship, ParseTreeNode parent) // node exist in sentence;
	{
		this.nodeID = NODEID;
		NODEID++; 
		this.wordOrder = wordOrder; 
		this.label = label; 
		this.pos = pos; 
		this.relationship = relationship; 
		this.parent = parent; 		
	}

	public MappedSchemaElement getChoiceMap()
	{
		if(choice >= 0 && mappedElements.size() > 0)
		{
			return mappedElements.get(choice); 
		}
		return null; 
	}
	
	public boolean subTreeContain(ParseTreeNode child)
	{
		ArrayList<ParseTreeNode> nodeList = new ArrayList<ParseTreeNode>(); 
		nodeList.add(this); 
		while(!nodeList.isEmpty())
		{
			ParseTreeNode node = nodeList.remove(0); 
			
			if(node.mappedElements.isEmpty() || child.mappedElements.isEmpty() || node.choice < 0 || child.choice < 0)
			{
				continue; 
			}
			else if(node.mappedElements.get(node.choice).schemaElement.equals(child.mappedElements.get(child.choice).schemaElement))
			{
				return true; 
			}
			
			for(int i = 0; i < node.children.size(); i++)
			{
				nodeList.add(node.children.get(i)); 
			}
		}
		
		return false; 
	}
	
	public String toOT()
	{
		if(this.pos.endsWith("JJR"))
		{
			return label + " than"; 
		}
		else
		{
			return label; 
		}
	}

	public boolean isFirstMappedDescendantOfCMT() {
        boolean result = false;
        ParseTreeNode parentCheck = this.parent;
        while (!parentCheck.label.equals("ROOT")) {
            if (parentCheck.tokenType.equals("NT") || parentCheck.tokenType.startsWith("VT")) {
                break;
            } else if (parentCheck.tokenType.equals("CMT")) {
                result = true;
                break;
            }
            parentCheck = parentCheck.parent;
        }
        return result;
	}
}
