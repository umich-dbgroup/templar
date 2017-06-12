package components;

import java.sql.SQLException;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import rdbms.RDBMS;
import rdbms.SchemaElement;
import dataStructure.BParseTree;
import dataStructure.BParseTreeNode;
import edu.northwestern.at.morphadorner.corpuslinguistics.lemmatizer.EnglishLemmatizer;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

public class ATokenizer 
{
    Document doc;
    AStanfordParser parser; 
    BParseTree parseTree; 
	EnglishLemmatizer lemmatizer; 
    
	ArrayList<BParseTreeNode> deleteList = new ArrayList<BParseTreeNode>(); 
    
    public ATokenizer(LexicalizedParser lexiParser, Document tokenDoc, EnglishLemmatizer lemmatizer) throws Exception
    {
    	doc = tokenDoc; 
    	parser = new AStanfordParser(lexiParser); 
    	this.lemmatizer = lemmatizer;
    }

    public void newSentence(String NLQuery, BParseTree parseTree, RDBMS db, DFeedbackGenerator feedbackGen) throws ClassNotFoundException, SQLException // process a new sentence; 
    {
		this.parseTree = parseTree; 
		parser.parse(NLQuery, this.parseTree, feedbackGen); 
    	this.tokenize();
    	this.candidateMapping(db); 
    	feedbackGen.mapping(parseTree); 
    }
    
	public void tokenize() throws ClassNotFoundException, SQLException // tokenize each node; 
	{		
		parseTree.root.tokenType = "ROOT"; // mark the root and the root's children; 
		for(int i = 0; i < parseTree.root.children.size(); i++) 
		{
			BParseTreeNode rootChild = parseTree.root.children.get(i);
			if((rootChild.pos.equals("VBP")||(rootChild.pos.equals("VB")))&& isOfType(parseTree, rootChild, "CMT_V", null)) // main verb is CMT (return)
			{
				rootChild.tokenType = "CMT"; 
			}
		}

		for(int i = 0; i < parseTree.allNodes.size(); i++)
		{
			BParseTreeNode curNode = parseTree.allNodes.get(i); 

			if(curNode.tokenType.equals("NA") && isOfType(parseTree, curNode, "NEG", null)) // if it is FT_A
            {
            	curNode.tokenType = "NEG";  
            }
		}
		
		for(int i = 0; i < parseTree.allNodes.size(); i++)
		{
			BParseTreeNode curNode = parseTree.allNodes.get(i); 
			if(curNode.tokenType.equals("NA") && curNode.relationship.equals("mwe")) // merge multi-word expression;
			{
				if(curNode.wordOrder > curNode.parent.wordOrder)
				{
					curNode.parent.label = curNode.parent.label + " " + curNode.label; 
				}
				else
				{
					curNode.parent.label = curNode.label + " " + curNode.parent.label; 					
				}
				parseTree.deleteNode(curNode); 
				break; 
			}
		}
		
		int curSize = 0; // delete and merge some nodes; 
		while(curSize != parseTree.allNodes.size())
		{
			curSize = parseTree.allNodes.size(); 
			for(int i = 0; i < parseTree.allNodes.size(); i++)
			{
				BParseTreeNode curNode = parseTree.allNodes.get(i); 

				if(curNode.tokenType.equals("NA") && isOfType(parseTree, curNode, "FT", "function")) // if it is FT_A
                {
                	curNode.tokenType = "FT";  
                }
                else if(curNode.tokenType.equals("NA") && isOfType(parseTree, curNode, "OT", "operator"))// OT_A? 
                {
                	curNode.tokenType = "OT";
                }
                else if(curNode.tokenType.equals("NA") && isOfType(parseTree, curNode, "OBT", null))// OBT?
                {
                	curNode.tokenType = "OBT";
                }
                else if(curNode.tokenType.equals("NA") && curNode.pos.startsWith("NN")) // if its POS is N
                {
                	curNode.tokenType = "NTVT"; 
                }
                else if(curNode.pos.equals("CD"))
                {
                	curNode.tokenType = "VT"; 
                }
                else if(curNode.tokenType.equals("NA") && curNode.tokenType.startsWith("JJ")) // if POS is JJ
                {
                    curNode.tokenType = "MM"; 
                } 
                else if(curNode.tokenType.equals("NA") && curNode.tokenType.equals("IN")) 
                {
                	if(isOfType(parseTree, curNode, "CM_P", null)) 
                	{
                        curNode.tokenType = "CM";
                    }
                } 
                else if(curNode.tokenType.equals("NA") && isOfType(parseTree, curNode, "QT", "quantity")) 
                {
                    curNode.tokenType = "QT";
                } 
                else if(curNode.tokenType.equals("NA") && (curNode.label.equalsIgnoreCase("and") || curNode.label.equalsIgnoreCase("or") || curNode.label.equalsIgnoreCase("but")))
                {
                    curNode.tokenType = "CC";
                }
			}
		}
	}

    public boolean isOfType(BParseTree tree, BParseTreeNode node, String token, String tag)
    {
    	if(isOfType(tree, node, token, 1, tag))
    	{
    		return true; 
    	}
    	else if(isOfType(tree, node, token, 2, tag))
    	{
    		return true; 
    	}
    	return false; 
    }
    
    // test if the given phrase belongs to a given token type: type = 1: lower case; type = 2: original case;  OBT
    public boolean isOfType(BParseTree tree, BParseTreeNode node, String token, int type, String tag) 
    {
    	String label = ""; 
    	if(type == 1)
    	{
    		label = node.label.toLowerCase(); 
    	}
    	else if(type == 2)
    	{
    		label = lemmatizer.lemmatize(node.label).toLowerCase(); 
    	}
    	
    	Element tokenE = (Element)(doc.getElementsByTagName(token)).item(0); // find the token first;        
        NodeList phrList = tokenE.getElementsByTagName("phrase"); // get its phrases

        for(int i = 0; i < phrList.getLength(); i++) // find the matching phrase
        {
            String phrText = phrList.item(i).getFirstChild().getNodeValue().trim();
            if(phrText.split(" ").length == 1 && !label.contains(" "))
            {
            	if(label.equals(phrText)) 
                {
                    node.tokenType = token; 
                    if(tag != null)
                    {
                        String attText = ((Element)phrList.item(i)).getElementsByTagName(tag).item(0).getFirstChild().getNodeValue().trim();
                        node.function = attText;
                    }
                    return true; 
                }
            }
            else if(phrText.split(" ").length == 1 && label.contains(" "))
            {
            	if(label.contains(phrText+" ")) 
                {
                    node.tokenType = token; 
                    if(tag != null)
                    {
                        String attText = ((Element)phrList.item(i)).getElementsByTagName(tag).item(0).getFirstChild().getNodeValue().trim();
                        node.function = attText;
                    }
                    return true; 
                }
            }
            else if(phrText.contains(label))
            {
            	if(phrText.equals(label))
            	{
            		return true; 
            	}

            	String [] phrWords = phrText.split(" "); 
            	int j = 0; 
            	while(j < phrWords.length)
            	{
            		if(phrWords[j].equals(label))
            		{
            			break; 
            		}
            		j++; 
            	}
            	
            	int index = node.wordOrder; 
            	if((index - j > 1))
            	{
                   	String wholePhrase = ""; 
            		for(int k = 0; (k<phrWords.length-1) && (tree.searchNodeByOrder(index-j+k)!=null); k++)
            		{
            			if(j == k)
            			{
                    		wholePhrase += label + " "; 
            			}
            			else
            			{
                    		wholePhrase += tree.searchNodeByOrder(index-j+k).label + " "; 
            			}
            		}
            		
            		if(tree.searchNodeByOrder(index-j+phrWords.length-1)!=null)
                	{
            			wholePhrase += tree.searchNodeByOrder(index-j+phrWords.length-1).label; 
                	}
                	
                	if(wholePhrase.contains(phrText))
            		{
                        node.tokenType = token; 
                        if(tag != null)
                        {
                            String attText = ((Element)phrList.item(i)).getElementsByTagName(tag).item(0).getFirstChild().getNodeValue().trim();
                            node.function = attText;
                        }
                        node.label = phrText; 
                		for(int k = 0; k < phrWords.length; k++)
                		{
                			if(j != k)
                			{
                				if(tree.searchNodeByOrder(index-j+k) != null)
                        		{
                					tree.deleteNode(tree.searchNodeByOrder(index-j+k)); 
                        		}
                			}
                		}
                        return true; 
            		}
            	}
            }
        }
        return false; 
    }    
    
    public void candidateMapping(RDBMS db) throws ClassNotFoundException, SQLException
    {
    	ArrayList<BParseTreeNode> NTNodes = new ArrayList<BParseTreeNode>(); 
    	ArrayList<BParseTreeNode> VTNodes = new ArrayList<BParseTreeNode>(); 
    	
    	for(int i = 0; i < parseTree.allNodes.size(); i++)
    	{
    		BParseTreeNode curNode = parseTree.allNodes.get(i); 
    		
    		if(curNode.tokenType.equals("NTVT"))
    		{
                if(curNode.label.split(" ").length > 1 || Character.isUpperCase(curNode.label.charAt(0)) || Character.isDigit(curNode.label.charAt(0))) 
                { 	// if it is a VT
                    curNode.tokenType = "VT"; 
                    VTNodes.add(curNode); 
                } 
                else // if it is not a VT
                {
                	curNode.tokenType = "NT"; 
                	NTNodes.add(curNode); 
                }
    		}
    		else if(curNode.tokenType.equals("VT"))
    		{
    			VTNodes.add(curNode); 
    		}
    	}

    	for(int i = 0; i < NTNodes.size(); i++)
    	{
    		BParseTreeNode NT = NTNodes.get(i); 
    		db.isSchemaExist(NT.label, NT.candidates); 	
    	}
        
    	for(int i = 0; i < VTNodes.size(); i++)
    	{
    		boolean hasOT = false; 
    		BParseTreeNode VT = VTNodes.get(i); 
    		if(VT.parent.tokenType.equals("OT"))
    		{
		        db.isValueExist(VT.parent.function, VT.label, VT.candidates); 
		        hasOT = true; 
    		}
    		else
    		{
        		for(int j = 0; j < VT.children.size(); j++)
        		{
        			if(VT.children.get(j).tokenType.equals("OT"))
        			{
        		        db.isValueExist(VT.children.get(j).function, VT.label, VT.candidates); 
        		        hasOT = true; 
        		        break; 
        			}
        		}    			
    		}    		
    		
    		if(hasOT == false)
    		{
		        db.isValueExist("=", VT.label, VT.candidates); 
    		}
    	}
    	
    	for(int i = 0; i < NTNodes.size(); i++)
    	{
    		BParseTreeNode NT = NTNodes.get(i); 
    		for(int j = 0; j < NT.candidates.size(); j++)
    		{
    			double max = 0; 
    			int position = 0; 
    			for(int k = j; k < NT.candidates.size(); k++)
    			{
    				if(NT.candidates.get(k).score > max)
    				{
    					max = NT.candidates.get(k).score; 
    					position = k; 
    				}
    			}
    			
    			if(j != position)
    			{
        			SchemaElement temp = NT.candidates.get(j); 
        			NT.candidates.set(j, NT.candidates.get(position)); 
        			NT.candidates.set(position, temp); 
    			}
    		}
    		
    		while(NT.candidates.size() > 1 && (NT.candidates.get(NT.candidates.size()-1).score*1.5 < NT.candidates.get(0).score || NT.candidates.get(0).score > 0.99))
    		{
    			NT.candidates.remove(NT.candidates.size()-1); 
    		}
    		while(NT.candidates.size() > 5)
    		{
    			NT.candidates.remove(NT.candidates.size()-1); 
    		}
    	}
    	
    	boolean [] done = new boolean[parseTree.allNodes.size()]; 
    	for(int i = 0; i < done.length; i++)
    	{
    		done[i] = false; 
    	}
    	
    	ArrayList<BParseTreeNode> NTVTNodes = new ArrayList<BParseTreeNode>(); 
    	for(int i = 0; i < NTNodes.size(); i++)
    	{
    		if(NTNodes.get(i).candidates.size() > 0)
    		{
    			NTVTNodes.add(NTNodes.get(i)); 
    		}
    	}
    	for(int i = 0; i < VTNodes.size(); i++)
    	{
    		if(VTNodes.get(i).candidates.size() > 0)
    		{
    			NTVTNodes.add(VTNodes.get(i)); 
    		}
    	}

    	int center = 0; 
    	double centerValue = 0; 
    	
    	for(int i = 0; i < NTVTNodes.size(); i++)
    	{
    		if(NTVTNodes.get(i).candidates.size() < 2)
    		{
    			if(NTVTNodes.get(i).candidates.get(0).score/0.1 > centerValue)
    			{
        			center = NTVTNodes.get(i).nodeID; 
        			centerValue = NTVTNodes.get(i).candidates.get(0).score/0.1; 
    			}
    		}
    		else if(NTVTNodes.get(i).candidates.get(1).score < 0.1)
    		{
    			if(NTVTNodes.get(i).candidates.get(0).score/0.1 > centerValue)
    			{
        			center = NTVTNodes.get(i).nodeID; 
        			centerValue = NTVTNodes.get(i).candidates.get(0).score/0.1; 
    			}
    		}
    		else if(NTVTNodes.get(i).candidates.get(0).score/NTVTNodes.get(i).candidates.get(1).score > centerValue)
    		{
    			center = NTVTNodes.get(i).nodeID; 
    			centerValue = NTVTNodes.get(i).candidates.get(0).score/NTVTNodes.get(i).candidates.get(1).score; 
    		}
    	}
    	
    	ArrayList<BParseTreeNode> queue = new ArrayList<BParseTreeNode>(); 
    	ArrayList<BParseTreeNode> queueC = new ArrayList<BParseTreeNode>(); 
    	
    	queue.add(parseTree.searchNodeByID(center)); 
    	queueC.add(parseTree.searchNodeByID(center));
    	
    	while(queue.size() > 0)
    	{
    		BParseTreeNode node = queue.remove(0); 
    		BParseTreeNode nodeC = queueC.remove(0); 

    		done[parseTree.allNodes.indexOf(node)] = true; 
    		
    		if(node != nodeC && NTVTNodes.contains(node))
    		{
    			double max = 0; 
    			for(int i = 0; i < node.candidates.size(); i++)
    			{
    				int distance = db.distance(node.candidates.get(i), nodeC.candidates.get(0)); 
    				if(node.candidates.get(i).score/(distance+15) > max)
    				{
    					max = node.candidates.get(i).score/(distance+15); 
    					SchemaElement maxElement = node.candidates.get(i); 
    					node.candidates.remove(maxElement); 
    					node.candidates.add(0, maxElement); 
    				}
    			}
        		if(node.parent != null && done[parseTree.allNodes.indexOf(node.parent)] == false)
        		{
        			queue.add(node.parent); 
        			queueC.add(node); 
        		}
        		for(int i = 0; i < node.children.size(); i++)
        		{
        			if(done[parseTree.allNodes.indexOf(node.children.get(i))] == false)
        			{
            			queue.add(node.children.get(i)); 
            			queueC.add(node);         				
        			}
        		}
    		}
    		else
    		{
        		if(node.parent != null)
        		{
        			queue.add(node.parent); 
        			queueC.add(nodeC); 
        		}
        		for(int i = 0; i < node.children.size(); i++)
        		{
        			if(done[parseTree.allNodes.indexOf(node.children.get(i))] == false)
        			{
            			queue.add(node.children.get(i)); 
            			queueC.add(nodeC);         				
        			}
        		}
    		}
    	}
    	
    	db.specificValue(VTNodes); 
    }
    
	public void deleteUselessNodes() // delete the words which cannot map to SQL fragments; 
	{
		int treeSize = parseTree.allNodes.size(); 
		do
		{
			treeSize = parseTree.allNodes.size(); 
			for(int i = 0; i < parseTree.allNodes.size(); i++)
			{
				String type = parseTree.allNodes.get(i).tokenType;  
				if(!type.equals("NT") && !type.equals("VT") && !type.equals("OBT") && !type.equals("NEG") && !type.equals("QT")
					&& !type.equals("CMT") && !type.equals("FT")  && !type.equals("OT") && !type.equals("ROOT") && parseTree.allNodes.get(i).children.size() < 2)
				{
					deleteList.add(parseTree.allNodes.get(i)); 
				}
			}
			
			for(int i = 0; i < deleteList.size(); i++)
			{
				parseTree.deleteNode(deleteList.get(i)); 
			}
			deleteList = new ArrayList<BParseTreeNode>(); 
		}
		while(treeSize != parseTree.allNodes.size()); 
	}
	
	public void printForCheck()
	{
		parser.printForCheck(); 
    	parseTree.printForCheck(); 
    	System.out.println("----------------------------"); 
	}
}
