package components;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import dataStructure.ASentence;
import dataStructure.BParseTree;
import dataStructure.BParseTreeNode;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

public class AStanfordParser
{
	LexicalizedParser lexiParser; 
	
    ArrayList<String []> treeTable = new ArrayList<String []>(); // the dependency tree table: Position, Phrase, Tag, Parent, all strings; each phrase is an entry
    ArrayList<String> conjTable = new ArrayList<String>(); // conjunction table: a^b

	BParseTree parseTree; 

	public AStanfordParser(LexicalizedParser lexiParser) throws ParserConfigurationException, SAXException, IOException
	{
    	this.lexiParser = lexiParser; 
	}
	
	public void parse(String NLQuery, BParseTree parseTree, DFeedbackGenerator feedbackGen)
	{
		String beginning = "" + NLQuery.charAt(0); 
		beginning = beginning.toLowerCase(); 
		NLQuery = beginning + NLQuery.substring(1); 
		this.parseTree = parseTree; 
		this.stanfordParser(NLQuery); // parse the NL to treeTable and conjTable
		feedbackGen.newSentence(treeTable); // feedback the new sentence information; 
		
		// build up the tree; 
		this.buildOriginalTree(); // build the tree according to treeTable and conjTable; 
		this.fixConj(); // for and, but, or; change the parent/child relationship to silbling relationship;  
	}
	
	public void stanfordParser(String NLQuery) // use Stanford parser and store the results in treeTable and conjTable; 
	{
		treeTable = new ArrayList<String []>(); 
		conjTable = new ArrayList<String>(); 
		
		ASentence sentence = new ASentence(NLQuery); 
	    String [] words = new String [sentence.words.size()]; 
		Iterator<String> iterWords = sentence.words.iterator(); 
		for(int i = 0; i < words.length; i++)
		{
			words[i] = iterWords.next(); 
		}
		
		List<CoreLabel> rawWords = Sentence.toCoreLabelList(words); // use Stanford parser to parse a sentence; 
    	Tree parse = lexiParser.apply(rawWords); 
    	TreebankLanguagePack tlp = new PennTreebankLanguagePack();
    	GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
    	GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
    	List<TypedDependency> dependencyList = gs.typedDependencies(false);
    	
    	Iterator<Tree> iter = parse.iterator(); 
    	ArrayList<String> allNodes = new ArrayList<String>(); // allNodes in format root [num]\n S [num]\n...
    	while(iter.hasNext())
    	{
    		allNodes.add(iter.next().nodeString()); 
    	}
    	
    	ArrayList<String []> allWords = new ArrayList<String []>(); // allWords in format word, pos; 
    	String [] word = {"ROOT", "ROOT"}; 
		allWords.add(word); 
    	for(int i = 0; i < allNodes.size(); i++)
    	{
    		if(sentence.words.contains(allNodes.get(i)))
    		{
    			word = new String [2]; 
    			word[0] = allNodes.get(i); 
    			word[1] = allNodes.get(i-1).split(" ")[0]; 
    			allWords.add(word); 
    		}
    	}

    	for(int i = 0; i < dependencyList.size(); i++)
		{
    		TypedDependency curDep = dependencyList.get(i); 
    		String depIndex = ""; 
    		depIndex += curDep.dep().index(); 
    		String govIndex = ""; 
    		govIndex += curDep.gov().index();
    		if(curDep.reln().toString().startsWith("conj")) // put all and/or information in conjTable; 
    		{
    			String conj = ""; 
    			conj += govIndex; 
    			conj += " "; 
    			conj += depIndex; 
    			conjTable.add(conj); 
    		}
        	String [] treeTableEntry = {depIndex, curDep.dep().value(), allWords.get(curDep.dep().index())[1], govIndex, curDep.reln().toString()}; 
        	treeTable.add(treeTableEntry); // treeTableEntry is in format: depIndex, depValue, pos, govIndex, relationship; 
		}    	
	}

	public void buildOriginalTree() // build the original parsed tree use treeTable; 
	{
        boolean[] doneList = new boolean[treeTable.size()]; // each tag should be visited once
        for(int i = 0; i < doneList.length; i++)
        {
        	doneList[i] = false; 
        }
        
    	for(int i = 0; i < treeTable.size(); i++) // create the root; 
    	{
    		if(treeTable.get(i)[3].equals("0"))
    		{
    			parseTree.addNode(treeTable.get(i));
    			doneList[i] = true; 
    		}
    	}
    	
    	boolean finished = false;
    	while(finished == false)
    	{
    		for(int i = 0; i < treeTable.size(); i++)
    		{
    			if(doneList[i] == false)
    			{
    				if(parseTree.addNode(treeTable.get(i)) == true)
    				{
    					doneList[i] = true; 
    					break; 
    				}
    			}
    		}
    		
        	finished = true;
        	for(int i = 0; i < doneList.length; i++)
        	{
        		if(doneList[i] == false)
        		{
        			finished = false; 
        			break; 
        		}
        	}
    	}
   	}
	
	public void fixConj() // store or in the conj, then in nodes; 
	{
		if(conjTable.size() == 0)
		{
			return; 
		}
		
    	for(int i = 0; i < conjTable.size(); i++) // deal with the case: return me the papers by Jagadish, Hector or Dan suciu; 
    	{
    		String conj = conjTable.get(i); 
    		int govNum = Integer.parseInt(conj.split(" ")[0]); 
    		BParseTreeNode govNode = parseTree.searchNodeByOrder(govNum);

    		int depNum = Integer.parseInt(conj.split(" ")[1]); 
    		BParseTreeNode depNode = parseTree.searchNodeByOrder(depNum); 

    		boolean or = false; 
    		for(int j = 0; j < conjTable.size(); j++)
    		{
    			String newConj = conjTable.get(j); 
    			if(govNum == Integer.parseInt(newConj.split(" ")[0]))
    			{
    				BParseTreeNode newDep = parseTree.searchNodeByOrder(Integer.parseInt(newConj.split(" ")[1])); 
    	    		if(parseTree.searchNodeByOrder(newDep.wordOrder-1) != null)
    	    		{
    	    			String logic = parseTree.searchNodeByOrder(newDep.wordOrder-1).label; 
        	    		if(logic.equalsIgnoreCase("or"))
        	    		{
        					conjTable.set(i, conjTable.get(i) + " or"); 
        					depNode.leftRel = "or"; 
            	    		or = true; 
        	    		}    	    			
    	    		}
    			}
    		}
    		if(or == false)
    		{
				conjTable.set(i, conjTable.get(i) + " and"); 
				depNode.leftRel = "and"; 
    		}

    		depNode.parent = govNode.parent; 
    		govNode.parent.children.add(depNode); 
    		govNode.children.remove(depNode); 
    		depNode.relationship = govNode.relationship; 
    	}
	}
		
	public void printForCheck()
	{
    	for(int i = 0; i < treeTable.size(); i++)
    	{
    		for(int j = 0; j < treeTable.get(i).length; j++)
    		{
    			System.out.print(treeTable.get(i)[j] + " "); 
    		}
    		System.out.println(); 
    	}
    	System.out.println("----------------------------"); 
    	
    	for(int i = 0; i < conjTable.size(); i++)
    	{
    		System.out.println(conjTable.get(i)); 
    	}	
    	System.out.println("----------------------------"); 
	}
}