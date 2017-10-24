package edu.umich.templar.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umich.templar.dataStructure.ParseTree;
import edu.umich.templar.dataStructure.ParseTreeNode;
import edu.umich.templar.dataStructure.Query;
import edu.umich.templar.rdbms.MappedSchemaElement;
import edu.umich.templar.rdbms.RDBMS;
import edu.umich.templar.tools.BasicFunctions;
import edu.umich.templar.tools.SimFunctions;
import edu.umich.templar.util.Utils;
import org.apache.commons.lang3.math.NumberUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class NodeMapper
{
	public static void phraseProcess(Query query, RDBMS db, Document tokens) throws Exception
	{
		tokenize(query, tokens); 
		deleteUseless(query); 
		map(query, db); 
		deleteNoMatch(query); 
		individualRanking(query); 
		groupRanking(query, db); 
	}
	
	public static void tokenize(Query query, Document tokens) throws Exception
	{
		ParseTree parseTree = query.parseTree;
		parseTree.root.tokenType = "ROOT"; // mark the root and the root's children; 

        boolean cmtFound = false;
		for(int i = 0; i < parseTree.root.children.size(); i++) {
            ParseTreeNode rootChild = parseTree.root.children.get(i);
            if (isOfType(tokens, parseTree, rootChild, "CMT_V", null)) // main verb is CMT (return)
            {
                rootChild.tokenType = "CMT";
                cmtFound = true;
            }
		}

        // Handle "what", "which", "how many", "who", etc
        List<ParseTreeNode> toDelete = new ArrayList<>();
        for (int i = 0; i < parseTree.allNodes.size(); i++) {
            ParseTreeNode curNode = parseTree.allNodes.get(i);
            if (!cmtFound && (curNode.pos.equals("WDT") || curNode.pos.equals("WP")) && curNode.children.isEmpty()) {
                // Only return if question is direct object
                if (!curNode.relationship.equals("dobj") && !curNode.relationship.equals("dep")) continue;

                // for most question words
                ParseTreeNode object = null;
                for (ParseTreeNode sibling : curNode.parent.children) {
                    if (sibling.relationship.startsWith("nsubj") && !sibling.equals(curNode)) {
                        object = sibling;
                        break;
                    }
                }

                // Could not find object
                if (object == null) continue;

                curNode.tokenType = "CMT";

                // Shove them all after the root
                List<ParseTreeNode> rootChildren = new ArrayList<>(parseTree.root.children);
                curNode.parent.children.remove(curNode);
                curNode.parent = parseTree.root;
                parseTree.root.children.clear();
                parseTree.root.children.add(curNode);

                object.parent = curNode;
                curNode.children.add(object);

                for (ParseTreeNode rootChild : rootChildren) {
                    if (!rootChild.equals(curNode) && !rootChild.equals(object)) {
                        rootChild.parent = object;
                        rootChild.children.remove(object);
                        object.children.add(rootChild);
                    }
                }

            } else if (!cmtFound && curNode.pos.equals("WRB") && curNode.parent.pos.equals("JJ") && curNode.children.isEmpty()) {
                // for "HOW MANY"

                // Merge the two nodes to one "how many" node
                ParseTreeNode parent = curNode.parent;
                curNode.label = curNode.label + " " + parent.label;
                curNode.tokenType = "CMT";
                curNode.function = "count";

                parent.children.remove(curNode);

                // In the case that it's referring to an object directly (e.g. "How many people have bought...?"
                if (parent.relationship.equals("amod")) {
                    ParseTreeNode object = parent.parent;

                    // Disconnect parent from object
                    object.children.remove(parent);
                    toDelete.add(parent);

                    // Shove them all after the root
                    List<ParseTreeNode> rootChildren = new ArrayList<>(parseTree.root.children);
                    curNode.parent = parseTree.root;
                    parseTree.root.children.clear();
                    parseTree.root.children.add(curNode);

                    object.parent = curNode;
                    curNode.children.add(object);

                    for (ParseTreeNode rootChild : rootChildren) {
                        if (!rootChild.equals(parent) && !rootChild.equals(object)) {
                            rootChild.parent = object;
                            rootChild.children.remove(object);
                            object.children.add(rootChild);
                        }
                    }
                } else if (parent.relationship.equals("dep")) {
                    // Otherwise, need to get subject of verb (e.g. "How many ____ exist?")
                    ParseTreeNode object = null;
                    for (ParseTreeNode uncle : parent.parent.children) {
                        if (uncle.relationship.startsWith("nsubj")) {
                            object = uncle;
                            break;
                        }
                    }

                    // Could not find object
                    if (object == null) continue;

                    // Remove object from parent/children
                    // object.parent.children.addAll(object.children);
                    // for (ParseTreeNode cousin : object.children) {
                    // cousin.parent = object.parent;
                    // }

                    // Disconnect parent from object
                    toDelete.add(parent);

                    // Shove them all after the root
                    List<ParseTreeNode> rootChildren = new ArrayList<>(parseTree.root.children);
                    curNode.parent = parseTree.root;
                    parseTree.root.children.clear();
                    parseTree.root.children.add(curNode);

                    object.parent = curNode;
                    curNode.children.add(object);

                    for (ParseTreeNode rootChild : rootChildren) {
                        if (!rootChild.equals(parent) && !rootChild.equals(object)) {
                            rootChild.parent = object;
                            rootChild.children.remove(object);
                            object.children.add(rootChild);
                        }
                    }
                }
            } else if (curNode.relationship.equals("xcomp") && curNode.parent.tokenType.equals("CMT")) {
                // Strange interpretations like for "return me the papers written by H. V. Jagadish and Yunyao Li on PVLDB after 2005."
                // Tree is derived as return -xcomp-> written -dep-> papers, and relationships need to be swapped
                ParseTreeNode toElevate = null;
                for (ParseTreeNode child : curNode.children) {
                    if (child.relationship.equals("dep")) {
                        toElevate = child;
                    }
                }
                if (toElevate != null) {
                    List<ParseTreeNode> curNodeChildren = curNode.children;
                    curNodeChildren.remove(toElevate);
                    ParseTreeNode curNodeParent = curNode.parent;

                    curNode.parent = toElevate;
                    curNodeParent.children.remove(curNode);
                    curNode.children = toElevate.children;
                    for (ParseTreeNode child : curNode.children) {
                        child.parent = curNode;
                    }

                    toElevate.parent = curNodeParent;
                    toElevate.parent.children.add(toElevate);
                    toElevate.children = curNodeChildren;
                    toElevate.children.add(curNode);
                    for (ParseTreeNode child : toElevate.children) {
                        child.parent = toElevate;
                    }
                }
            }
        }

		for(int i = 0; i < parseTree.allNodes.size(); i++)
		{
			ParseTreeNode curNode = parseTree.allNodes.get(i); 

			if(curNode.tokenType.equals("NA") && isOfType(tokens, parseTree, curNode, "NEG", null)) // if it is NEG
            {
            	curNode.tokenType = "NEG";  
            }
		}

		for(int i = 0; i < parseTree.allNodes.size(); i++)
		{
			ParseTreeNode curNode = parseTree.allNodes.get(i); 
			if(curNode.tokenType.equals("NA") && (curNode.relationship.equals("mwe"))) // merge multi-word expression;
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
				i--; 
			}
		}
		
		int curSize = 0; // delete and merge some nodes; 
		while(curSize != parseTree.allNodes.size())
		{
			curSize = parseTree.allNodes.size(); 
			for(int i = 0; i < parseTree.allNodes.size(); i++)
			{
				ParseTreeNode curNode = parseTree.allNodes.get(i); 

				if(curNode.tokenType.equals("NA") && isOfType(tokens, parseTree, curNode, "FT", "function")) // if it is FT_A
                {
                	curNode.tokenType = "FT";  
                }
                else if(curNode.tokenType.equals("NA") && isOfType(tokens, parseTree, curNode, "OT", "operator"))// OT_A? 
                {
                	curNode.tokenType = "OT";
                }
                else if(curNode.tokenType.equals("NA") && isOfType(tokens, parseTree, curNode, "OBT", null))// OBT?
                {
                	curNode.tokenType = "OBT";
                }
                else if(NumberUtils.isCreatable(curNode.label))
                {
                	curNode.tokenType = "VT"; 
                }
                else if(curNode.tokenType.equals("NA") &&
						(curNode.pos.startsWith("NN") || curNode.pos.equals("CD") || curNode.pos.startsWith("VB"))) // if its POS is
                {
                    curNode.tokenType = "NTVT";
                }
                else if(curNode.tokenType.equals("NA") && curNode.pos.startsWith("JJ"))
                {
                	curNode.tokenType = "JJ"; 
                }
                else if(curNode.tokenType.equals("NA") && isOfType(tokens, parseTree, curNode, "QT", "quantity")) 
                {
                    curNode.tokenType = "QT";
                } 
			}
		}
	}
	
	public static void deleteUseless(Query query)
	{
		ParseTree parseTree = query.parseTree; 
		query.originalParseTree = (ParseTree) BasicFunctions.depthClone(parseTree); 
		
		for(int i = 0; i < parseTree.allNodes.size(); i++)
		{
			if(parseTree.allNodes.get(i).tokenType.equals("NA") || parseTree.allNodes.get(i).tokenType.equals("QT"))
			{
				ParseTreeNode curNode = parseTree.allNodes.get(i); 
				if(curNode.label.equals("on") || curNode.label.equals("in") || curNode.label.equals("of") || curNode.label.equals("by"))
				{
					if(!curNode.children.isEmpty())
					{
						curNode.children.get(0).prep = curNode.label; 
					}
				}
				
				if(curNode.tokenType.equals("QT"))
				{
					curNode.parent.QT = curNode.function; 
				}
				
				parseTree.deleteNode(curNode); 
				i--; 
			}
		}
	}
	
	public static void map(Query query, RDBMS db) throws Exception {
        ParseTree parseTree = query.parseTree;
        ArrayList<ParseTreeNode> allNodes = parseTree.allNodes;

        for (int i = 0; i < allNodes.size(); i++) {
            ParseTreeNode treeNode = allNodes.get(i);
            if (treeNode.tokenType.equals("NTVT") || treeNode.tokenType.equals("JJ")) // schema+text
            {
                db.isSchemaExist(treeNode);

                // Only check for value tokens if we aren't dealing with a verb (which will probably be an attribute
                // or a relation).
                if (!treeNode.pos.startsWith("VB")) {
                    db.isTextExist(treeNode);
                }

                if (treeNode.mappedElements.size() == 0) {
                    treeNode.tokenType = "NA";
                    continue;
                }

                for (int j = 0; j < treeNode.mappedElements.size(); j++) {
                    MappedSchemaElement mappedElement = treeNode.mappedElements.get(j);
                    SimFunctions.similarity(treeNode, mappedElement);
                }
                Collections.sort(treeNode.mappedElements);

            } else if (treeNode.tokenType.equals("VT")) { // num
                String OT = "=";
                if (treeNode.parent.tokenType.equals("OT")) {
                    OT = treeNode.parent.function;
                } else {
                    for (ParseTreeNode child : treeNode.children) {
                        if (child.tokenType.equals("OT")) {
                            OT = child.function;
                        }
                    }
                }
                db.isNumExist(OT, treeNode);

                treeNode.tokenType = "VTNUM";
                treeNode.attachedOT = OT;

                for (int j = 0; j < treeNode.mappedElements.size(); j++) {
                    MappedSchemaElement mappedElement = treeNode.mappedElements.get(j);
                    SimFunctions.similarity(treeNode, mappedElement);
                }
                Collections.sort(treeNode.mappedElements);
            }
        }

        for (int i = 0; i < allNodes.size(); i++) {
            ParseTreeNode treeNode = allNodes.get(i);

            // Make sure this isn't a stopword
            if (Utils.isStopword(treeNode.label)) continue;

            if (treeNode.tokenType.equals("NTVT") || treeNode.tokenType.equals("JJ")) { // schema+text
                // Added by cjbaik
                // If this word is related as an adjective to another word, then try the multi-word expression as well.
                String originalLabel = treeNode.label;
                MappedSchemaElement origElement = treeNode.mappedElements.get(0);
                List<MappedSchemaElement> origMappedElements = new ArrayList<>(treeNode.mappedElements);

                for (ParseTreeNode[] adjEntry : query.adjTable) {
                    ParseTreeNode relatedNode;
                    if (adjEntry[0].equals(treeNode)) {
                        relatedNode = adjEntry[1];
                    } else if (adjEntry[1].equals(treeNode)) {
                        relatedNode = adjEntry[0];
                    } else {
                        continue;
                    }
                    // Make sure the related node comes after the current word
                    if (relatedNode.wordOrder <= treeNode.wordOrder) continue;

                    // Make sure it hasn't already been removed
                    if (!parseTree.allNodes.contains(relatedNode)) continue;

                    treeNode.label = treeNode.label + " " + relatedNode.label;
                    db.isSchemaExist(treeNode);
                    db.isTextExist(treeNode);

                    // Make sure it has mapped elements. If not, skip
                    if (relatedNode.mappedElements.isEmpty()) continue;

                    // Original related element
                    MappedSchemaElement origRelatedEl = relatedNode.mappedElements.get(0);

                    // Only check and re-sort the new mapped elements
                    List<MappedSchemaElement> newMappedElements = new ArrayList<>();
                    for (int j = origMappedElements.size() + 1; j < treeNode.mappedElements.size(); j++) {
                        MappedSchemaElement mappedElement = treeNode.mappedElements.get(j);
                        SimFunctions.similarity(treeNode, mappedElement);
                        newMappedElements.add(mappedElement);
                    }
                    Collections.sort(newMappedElements);

                    // Make sure it has mapped elements. If not, skip
                    if (newMappedElements.isEmpty()) continue;

                    MappedSchemaElement newElement = newMappedElements.get(0);

                    if (!newElement.equals(origElement) &&
                            newElement.similarity >= origElement.similarity &&
                            newElement.similarity >= origRelatedEl.similarity) {
                        // Keep tree in multi-word form if new similarity is higher
                        treeNode.relationship = relatedNode.relationship;
                        treeNode.mappedElements.removeAll(origMappedElements);
                        for (ParseTreeNode relatedChild : relatedNode.children) {
                            if (!relatedChild.equals(treeNode)) {
                                relatedChild.parent = treeNode;
                                relatedChild.children.remove(treeNode);
                                treeNode.children.add(relatedChild);
                            }
                        }
                        if (treeNode.parent.equals(relatedNode)) {
                            treeNode.parent = relatedNode.parent;
                            relatedNode.parent.children.add(treeNode);
                        }
                        relatedNode.children.clear();
                        relatedNode.parent.children.remove(relatedNode);
                        parseTree.allNodes.remove(relatedNode);
                        i--;
                    } else {
                        // Otherwise, revert to option where it's not multi-word elements
                        treeNode.label = originalLabel;
                        treeNode.mappedElements = origMappedElements;
                    }
                    break;
                }
            }
        }
	}
	
	public static void deleteNoMatch(Query query)
	{
		ParseTree parseTree = query.parseTree; 
		
		for(int i = 0; i < parseTree.allNodes.size(); i++)
		{
			if(parseTree.allNodes.get(i).tokenType.equals("NA"))
			{
				ParseTreeNode curNode = parseTree.allNodes.get(i); 
				parseTree.deleteNode(curNode); 
				if(curNode.label.equals("on") || curNode.label.equals("in"))
				{
					curNode.parent.prep = curNode.label; 
				}
				i--; 
			}
		}
	}

	public static void individualRanking(Query query)
	{
		ArrayList<ParseTreeNode> treeNodes = query.parseTree.allNodes;
		for(int i = 0; i < treeNodes.size(); i++)
		{
			if(treeNodes.get(i).mappedElements.isEmpty())
			{
				continue; 
			}
			
			ParseTreeNode treeNode = treeNodes.get(i); 
			List<MappedSchemaElement> mappedList = treeNode.mappedElements;
			for(int j = 0; j < mappedList.size(); j++)
			{
				MappedSchemaElement mappedElement = mappedList.get(j); 
				SimFunctions.similarity(treeNode, mappedElement);
			}
			
			Collections.sort(mappedList); 
		}
		
		treeNodes = query.parseTree.allNodes;
		for(int i = 0; i < treeNodes.size(); i++)
		{
			if(!treeNodes.get(i).tokenType.equals("NTVT"))
			{
				continue; 
			}

			List<MappedSchemaElement> deleteList = new ArrayList<MappedSchemaElement>();
			ParseTreeNode treeNode = treeNodes.get(i); 
			List<MappedSchemaElement> mappedList = treeNode.mappedElements;
			for(int j = 0; j < mappedList.size(); j++)
			{
				MappedSchemaElement NT = mappedList.get(j); 
				for(int k = j+1; k < mappedList.size(); k++)
				{
					MappedSchemaElement VT = mappedList.get(k); 
					if(NT.mappedValues.isEmpty() && !VT.mappedValues.isEmpty() && NT.schemaElement.equals(VT.schemaElement))
					{
						if(NT.similarity >= VT.similarity)
						{
							VT.similarity = NT.similarity; 
							VT.choice = -1; 
							int VTposition = treeNode.mappedElements.indexOf(VT); 
							treeNode.mappedElements.set(treeNode.mappedElements.indexOf(NT), VT); 
							treeNode.mappedElements.set(VTposition, NT); 
						}
						deleteList.add(NT); 
					}
				}
			}
			
			treeNode.mappedElements.removeAll(deleteList); 
		}
	}

	public static void groupRanking(Query query, RDBMS db)
	{
		ParseTreeNode rooot = query.parseTree.allNodes.get(0); 
		double roootScore = 0; 
		for(int i = 0; i < query.parseTree.allNodes.size(); i++)
		{
			ParseTreeNode node = query.parseTree.allNodes.get(i); 
			double score = 0; 
			if(!node.mappedElements.isEmpty())
			{
				if(node.mappedElements.size() == 1)
				{
					score = 1; 
				}
				else
				{
					score = 1 - node.mappedElements.get(1).similarity/node.mappedElements.get(0).similarity; 
				}
				
				if(score >= roootScore)
				{
					rooot = node; 
					roootScore = score; 
				}
			}
		}
		
		if(rooot.label.equals("ROOT"))
		{
			return; 
		}
		rooot.choice = 0; 
		
		boolean [] done = new boolean [query.parseTree.allNodes.size()]; 
		for(int i = 0; i < done.length; i++)
		{
			done[i] = false; 
		}

		ArrayList<ParseTreeNode> queue = new ArrayList<ParseTreeNode>(); 
		queue.add(rooot); 
		queue.add(rooot); 
		
		while(!queue.isEmpty())
		{
			ParseTreeNode parent = queue.remove(0); 
			ParseTreeNode child = queue.remove(0); 
			
			if(done[query.parseTree.allNodes.indexOf(child)] == false)
			{
				if(!parent.equals(child))
				{
					int maxPosition = 0; 
					double maxScore = 0; 
					List<MappedSchemaElement> mappedElements = child.mappedElements;
					for(int i = 0; i < mappedElements.size(); i++)
					{
						MappedSchemaElement parentElement = parent.mappedElements.get(parent.choice); 
						MappedSchemaElement childElement = child.mappedElements.get(i); 
						// double distance = db.schemaGraph.distance(parentElement.schemaElement, childElement.schemaElement);
						double curScore = parentElement.similarity * childElement.similarity; // * distance;
						
						if(curScore > maxScore)
						{
							maxScore = curScore; 
							maxPosition = i; 
						}
					}
					child.choice = maxPosition;
				}
				
				if(child.mappedElements.isEmpty())
				{
					for(int i = 0; i < child.children.size(); i++)
					{
						queue.add(parent); 
						queue.add(child.children.get(i)); 
					}
					if(child.parent != null)
					{
						queue.add(parent); 
						queue.add(child.parent); 
					}
				}
				else
				{
					for(int i = 0; i < child.children.size(); i++)
					{
						queue.add(child); 
						queue.add(child.children.get(i)); 						
					}
					if(child.parent != null)
					{
						queue.add(child); 
						queue.add(child.parent); 
					}
				}
				
				done[query.parseTree.allNodes.indexOf(child)] = true; 
			}
		}
		
		for(int i = 0; i < query.parseTree.allNodes.size(); i++)
		{
			ParseTreeNode node = query.parseTree.allNodes.get(i); 
			if(node.tokenType.equals("NTVT") || node.tokenType.equals("JJ"))
			{
				if(node.mappedElements.size() > 0)
				{
					if(node.mappedElements.get(node.choice).mappedValues.size() == 0 || node.mappedElements.get(node.choice).choice == -1)
					{
						node.tokenType = "NT"; 
					}
					else
					{
						node.tokenType = "VTTEXT"; 
					}
				}
			}
			
		}
	}

	public static boolean isOfType(Document tokens, ParseTree tree, ParseTreeNode node, String token, String tag) throws Exception
    {
    	if(isOfType(tokens, tree, node, token, 1, tag))
    	{
    		return true; 
    	}
    	else if(isOfType(tokens, tree, node, token, 2, tag))
    	{
    		return true; 
    	}
    	return false; 
    }
    	
    // test if the given phrase belongs to a given token type: type = 1: lower case; type = 2: original case;  OBT
    public static boolean isOfType(Document tokens, ParseTree tree, ParseTreeNode node, String token, int type, String tag) throws Exception 
    {
    	String label = ""; 
    	if(type == 1)
    	{
    		label = node.label.toLowerCase(); 
    	}
    	else if(type == 2)
    	{
    		label = SimFunctions.lemmatize(node.label).toLowerCase(); 
    	}
    	
    	Element tokenE = (Element)(tokens.getElementsByTagName(token)).item(0); // find the token first;        
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
}
