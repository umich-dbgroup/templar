package components;

import java.util.ArrayList;

import rdbms.SchemaElement;
import tools.Functions;

import dataStructure.BParseTree;
import dataStructure.BParseTreeNode;
import dataStructure.CSQLElement;

public class DFeedbackGenerator 
{
	public ArrayList<String> history = new ArrayList<String>(); 
	public ArrayList<String> newSentences = new ArrayList<String>(); 
	public ArrayList<String> noMap = new ArrayList<String>(); 
	public ArrayList<String> maps = new ArrayList<String>(); 
	public ArrayList<String> useLess = new ArrayList<String>(); 
	public ArrayList<String> generalIntepretations = new ArrayList<String>(); 
	public ArrayList<String> specificIntepretations = new ArrayList<String>(); 
	public ArrayList<String> SQL = new ArrayList<String>(); 
	public String metaData = ""; 
	public ArrayList<String> finalResults = new ArrayList<String>(); 

	public String feedback = ""; 
	
	public void clear(String type)
	{
		if(type.equals("history"))
		{
			history = new ArrayList<String>(); 
			newSentences = new ArrayList<String>(); 
			noMap = new ArrayList<String>(); 
			maps = new ArrayList<String>(); 
			useLess = new ArrayList<String>(); 
			generalIntepretations = new ArrayList<String>(); 
			specificIntepretations = new ArrayList<String>(); 
			SQL = new ArrayList<String>(); 
			metaData = ""; 
			finalResults = new ArrayList<String>(); 			
		}
		if(type.equals("newSentence"))
		{
			newSentences = new ArrayList<String>(); 
			noMap = new ArrayList<String>(); 
			maps = new ArrayList<String>(); 
			useLess = new ArrayList<String>(); 
			generalIntepretations = new ArrayList<String>(); 
			specificIntepretations = new ArrayList<String>(); 
			SQL = new ArrayList<String>(); 
			metaData = ""; 
			finalResults = new ArrayList<String>(); 
		}
		else if(type.equals("setMapping"))
		{
			maps = new ArrayList<String>(); 
			useLess = new ArrayList<String>(); 
			generalIntepretations = new ArrayList<String>(); 
			specificIntepretations = new ArrayList<String>(); 
			SQL = new ArrayList<String>(); 
			metaData = ""; 
			finalResults = new ArrayList<String>(); 
		}
		else if(type.equals("generalChoice"))
		{
			generalIntepretations = new ArrayList<String>(); 
			specificIntepretations = new ArrayList<String>(); 
			SQL = new ArrayList<String>(); 
			metaData = ""; 
			finalResults = new ArrayList<String>(); 
		}
		else if(type.equals("specificChoice"))
		{
			specificIntepretations = new ArrayList<String>(); 
			SQL = new ArrayList<String>(); 
			metaData = ""; 
			finalResults = new ArrayList<String>(); 
		}
		feedback = ""; 
	}
	
	public void loadHistory(ArrayList<String> history)
	{
		this.history = history; 
	}
	
	public void feedbackGen()
	{
		for(int i = 0; i < history.size(); i++)
		{
			feedback += "#history " + history.get(i) + "\n"; 
		}
		for(int i = 0; i < newSentences.size(); i++)
		{
			feedback += newSentences.get(i) + "\n"; 
		}
		for(int i = 0; i < noMap.size(); i++)
		{
			feedback += noMap.get(i) + "\n"; 
		}
		if(noMap.size() > 0)
		{
			return; 
		}
		for(int i = 0; i < maps.size(); i++)
		{
			feedback += maps.get(i) + "\n"; 
		}
		for(int i = 0; i < useLess.size(); i++)
		{
			feedback += useLess.get(i) + "\n"; 
		}
		for(int i = 0; i < this.generalIntepretations.size(); i++)
		{
			feedback += this.generalIntepretations.get(i) + "\n"; 
		}
		for(int i = 0; i < this.specificIntepretations.size(); i++)
		{
			feedback += this.specificIntepretations.get(i) + "\n"; 
		}
		for(int i = 0; i < this.SQL.size(); i++)
		{
			feedback += this.SQL.get(i) + "\n"; 
		}
		feedback += this.metaData + "\n"; 
		for(int i = 0; i < this.finalResults.size(); i++)
		{
			feedback += this.finalResults.get(i) + "\n"; 
		}
		System.out.println("----------------------------"); 
		System.out.println(feedback); 
	}
	
	public void mapping(BParseTree tree)
	{
		for(int i = 0; i < tree.allNodes.size(); i++)
		{
			BParseTreeNode node = tree.allNodes.get(i); 
			if(node.tokenType.equals("NT"))
			{
				if(node.candidates.isEmpty())
				{
					noMap.add("noMap " + node.label + " " + node.wordOrder); 
				}
				else
				{
					String sentence = "map " + node.label + " " + node.wordOrder + " " + node.choice + "; "; 
					for(int j = 0; j < node.candidates.size(); j++)
					{
						SchemaElement element = node.candidates.get(j); 
						if(element.type.equals("relation"))
						{
							sentence += element.relation.relName + "; "; 
						}
						else if(element.type.equals("attribute"))
						{
							sentence += element.attribute.relation.relName + "'s " + element.attribute.attName + "; "; 							
						}
					}
					maps.add(sentence); 
				}
			}
			else if(node.tokenType.equals("VT"))
			{
				if(!Functions.isNumeric(node.label) && node.candidates.isEmpty())
				{
					noMap.add("noMap " + node.label + " " + node.wordOrder); 
				}

				String sentence = "map " + node.label + " " + node.wordOrder + " " + node.choice + "; "; 
				for(int j = 0; j < node.candidates.size(); j++)
				{
					SchemaElement element = node.candidates.get(j); 
					sentence += element.attribute.relation.relName + "'s " + element.attribute.attName + "; "; 	
				}
				if(node.candidates.size() > 0)
				{
					if(!node.candidates.get(node.choice).mappedValues.isEmpty())
					{
						sentence += node.candidates.get(node.choice).choice + "# "; 
						for(int j = 0; j < node.candidates.get(node.choice).mappedValues.size(); j++)
						{
							sentence += node.candidates.get(node.choice).mappedValues.get(j) + "# "; 
						}
					}
					maps.add(sentence); 
				}
			}
		}
	}
	
	public void newSentence(ArrayList<String []> treeTable)
	{
		for(int i = 0; i < treeTable.size(); i++)
		{
			String sentence = "sentence ";
			sentence += treeTable.get(i)[0] + " " + treeTable.get(i)[1]; 
			newSentences.add(sentence); 
		}
	}
	
	public void deleteUseless(ArrayList<BParseTreeNode> deleteList)
	{
		for(int i = 0; i < deleteList.size(); i++)
		{
			BParseTreeNode curNode = deleteList.get(i); 
			if(!curNode.tokenType.equals("CC"))
			{
				String useless = "useless " + curNode.wordOrder + " " + curNode.label; 
				useLess.add(useless); 
			}
		}
	}

	public void translateBack(ArrayList<BParseTree> treeList, String type, int choice) 
	{
		if(type.equals("general"))
		{
			this.generalIntepretations = new ArrayList<String>(); 
			this.generalIntepretations.add("general " + choice); 
			for(int i = 0; i < treeList.size() && i < 5; i++)
			{
				BParseTree tree = treeList.get(i); 
				this.generalIntepretations.add("general " + tree.translateToNL()); 
			}
		}
		else if(type.equals("specific"))
		{
			this.specificIntepretations = new ArrayList<String>(); 
			this.specificIntepretations.add("specific " + choice); 
			for(int i = 0; i < treeList.size() && i < 5; i++)
			{
				BParseTree tree = treeList.get(i); 
				this.specificIntepretations.add("specific " + tree.translateToNL()); 
			}
		}
	}

	public void SQL(String SQL) 
	{
		String [] SQLLines = SQL.split("\n"); 
		for(int i = 0; i < SQLLines.length; i++)
		{
			this.SQL.add("SQL " + SQLLines[i]); 
		}
	}
	
	public void meta(ArrayList<CSQLElement> metaData)
	{
		String row = "result "; 
		for(int j = 0; j < metaData.size()-1; j++)
		{
			row += metaData.get(j).conditionString() + "###"; 
		}
		row += metaData.get(metaData.size()-1).conditionString(); 
		this.finalResults.add(row); 
		
	}

	public void result(ArrayList<ArrayList<String>> finalResults) 
	{
		for(int i = 0; i < finalResults.size(); i++)
		{
			String row = "result "; 
			ArrayList<String> line = finalResults.get(i); 
			for(int j = 0; j < line.size()-1; j++)
			{
				row += line.get(j) + "###"; 
			}
			row += line.get(line.size()-1); 
			this.finalResults.add(row); 
		}
	}
}
