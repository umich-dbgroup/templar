package architecture;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import rdbms.RDBMS;
import components.ATokenizer;
import components.BStructureAdjustor;
import components.CTranslator; 
import components.DFeedbackGenerator;
import dataStructure.BParseTree;
import dataStructure.BParseTreeNode;

import edu.northwestern.at.morphadorner.corpuslinguistics.lemmatizer.EnglishLemmatizer;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

public class SystemInterface
{
	LexicalizedParser lexiParser = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz"); 
	String databaseName = "dblpcj"; 
	RDBMS dbms; 
	EnglishLemmatizer lemmatizer = new EnglishLemmatizer(); 
	ATokenizer tokenizer; 
	BStructureAdjustor adjustor;  
	CTranslator translator;
	public DFeedbackGenerator feedbackGen = new DFeedbackGenerator(); 
	
	BParseTree parseTree; 
	ArrayList<BParseTree> treeList; 
	ArrayList<BParseTree> specificList; 
	String SQL; 

	public SystemInterface() throws Exception 
	{
    	File tokenFile = new File("C:/Users/lifei/Desktop/data/tokens.xml"); 
    	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); 
    	DocumentBuilder builder = factory.newDocumentBuilder();
		dbms = new RDBMS(databaseName, lemmatizer); 

		tokenizer = new ATokenizer(lexiParser, builder.parse(tokenFile), new EnglishLemmatizer()); 

		treeList = new ArrayList<BParseTree>(); 
		specificList = new ArrayList<BParseTree>(); 

		SQL = ""; 
		adjustor = new BStructureAdjustor(treeList, specificList, dbms, feedbackGen, lemmatizer); 
		translator = new CTranslator(dbms, feedbackGen); 
	}
	
	public static void main(String [] args) throws Exception
	{
		SystemInterface inter = new SystemInterface();
		String input = "return me all the authors who have more papers than H. V. Jagadish on VLDB after 2005."; 
		inter.conductCommand("newQuery " + input); 
	}

	public void conductCommand(String command) throws ParserConfigurationException, SAXException, IOException, ClassNotFoundException, SQLException
	{
		String [] commandWords = command.split(" "); 

		if(commandWords[0].equals("useDatabase")) // Command 1: use database;  
		{
			this.useDatabase(commandWords[1]);
		}
		else if(commandWords[0].equals("newQuery")) // Command 2: new Query;
		{
			String sentence = command.substring(9); 
			this.newQuery(sentence); 
		}
		else if(commandWords[0].equals("map")) // Command 3: map a node to an element in the database;   
		{
			this.setMapping(Integer.parseInt(commandWords[1]), Integer.parseInt(commandWords[2])); 
		}
		else if(commandWords[0].equals("specify")) // Command 4: set a specific mapping;   
		{
			this.specificMap(Integer.parseInt(commandWords[1]), Integer.parseInt(commandWords[2])); 
		}
		else if(commandWords[0].equals("generalIntepretation")) // Command 4: these two words are not the same entity;   
		{
			this.setGeneralIntepretation(Integer.parseInt(commandWords[1])); 
		}
		else if(commandWords[0].equals("specificIntepretation")) // Command 4: these two words are not the same entity;   
		{
			this.setSpecificIntepretation(Integer.parseInt(commandWords[1])); 
		}

		feedbackGen.feedback += "feedback " + command; 
	}
	
	public void useDatabase(String databaseName)
	{
		this.databaseName = databaseName; 
	}
		
	public void newQuery(String NLQuery) throws ParserConfigurationException, SAXException, IOException, ClassNotFoundException, SQLException
	{
		parseTree = new BParseTree(); 
    	BParseTreeNode.NODEID = 1; 
        feedbackGen.clear("newSentence"); 
		tokenizer.newSentence(NLQuery, parseTree, dbms, feedbackGen); 
		if(feedbackGen.noMap.isEmpty())
		{
			adjustor.structureAdjust(parseTree); 
			translator.build(this.specificList.get(adjustor.specificChoice)); 
		}
		if(!translator.SQL.isEmpty())
		{
			System.out.println(translator.SQL); 
			ArrayList<ArrayList<String>> finalResults = dbms.conductSQL(translator.SQL); 
			feedbackGen.result(finalResults); 
		}
		
		feedbackGen.feedbackGen(); 
	}
	
	public void setMapping(int wordID, int choice) throws SQLException
	{
		dbms.setMapping(parseTree.searchNodeByID(wordID), choice); 
		feedbackGen.clear("setMapping"); 
		feedbackGen.mapping(parseTree); 
		adjustor.structureAdjust(parseTree); 
		translator.build(this.specificList.get(adjustor.specificChoice)); 
		if(!translator.SQL.isEmpty())
		{
			System.out.println(translator.SQL); 
			ArrayList<ArrayList<String>> finalResults = dbms.conductSQL(translator.SQL); 
			feedbackGen.result(finalResults); 
		}
		
		feedbackGen.feedbackGen(); 
	}
	
	public void specificMap(int wordID, int choice) throws SQLException
	{
		BParseTreeNode node = parseTree.allNodes.get(wordID); 
		node.candidates.get(node.choice).choice = choice; 
		feedbackGen.clear("setMapping"); 
		feedbackGen.mapping(parseTree); 
		adjustor.structureAdjust(parseTree); 
		translator.build(this.specificList.get(adjustor.specificChoice)); 
		if(!translator.SQL.isEmpty())
		{
			System.out.println(translator.SQL); 
			ArrayList<ArrayList<String>> finalResults = dbms.conductSQL(translator.SQL); 
			feedbackGen.result(finalResults); 
		}
		
		feedbackGen.feedbackGen(); 
	}

	public void setGeneralIntepretation(int choice) throws SQLException
	{
		adjustor.generalChoice = choice; 
		feedbackGen.clear("generalChoice"); 
		adjustor.generalChoice(choice); 
		translator.build(specificList.get(adjustor.specificChoice)); 
		if(!translator.SQL.isEmpty())
		{
			System.out.println(translator.SQL); 
			ArrayList<ArrayList<String>> finalResults = dbms.conductSQL(translator.SQL); 
			feedbackGen.result(finalResults); 
		}
		
		feedbackGen.feedbackGen(); 
	}
	
	public void setSpecificIntepretation(int choice) throws SQLException
	{
		adjustor.specificChoice = choice; 
		feedbackGen.clear("specificChoice"); 
		adjustor.specificChoice(choice); 
		translator.build(specificList.get(adjustor.specificChoice)); 
		if(!translator.SQL.isEmpty())
		{
			System.out.println(translator.SQL); 
			ArrayList<ArrayList<String>> finalResults = dbms.conductSQL(translator.SQL); 
			feedbackGen.result(finalResults); 
		}
		
		feedbackGen.feedbackGen(); 
	}
}