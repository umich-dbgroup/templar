package architecture;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import components.ATokenizer;
import components.BStructureAdjustor;
import components.CTranslator;
import components.DFeedbackGenerator;
import dataStructure.BParseTree;
import dataStructure.BParseTreeNode;

import rdbms.RDBMS;
import edu.northwestern.at.morphadorner.corpuslinguistics.lemmatizer.EnglishLemmatizer;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

public class CommandInterface 
{
	LexicalizedParser lexiParser; 
	EnglishLemmatizer lemmatizer; 
	RDBMS dbms; 
	
	ATokenizer tokenizer; 
	BStructureAdjustor adjustor;  
	CTranslator translator;
	public DFeedbackGenerator feedbackGen = new DFeedbackGenerator(); 
	
	BParseTree parseTree; 
	ArrayList<BParseTree> treeList; 
	ArrayList<BParseTree> specificList; 
	String SQL = ""; 

	public static void main(String [] args) throws Exception
	{
		CommandInterface system = new CommandInterface(); 
		String query = ""; 
		system.newQuery(query); 
		
		Scanner scan = new Scanner(System.in); 
		while(scan.hasNextLine())
		{
			system.ConductCommand(scan.nextLine()); 
			scan = new Scanner(System.in); 
		}
	}
	
	public CommandInterface() throws Exception
	{
		lexiParser = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz"); 
		lemmatizer = new EnglishLemmatizer(); 
		String defaultDB = "mas"; 
		dbms = new RDBMS(defaultDB, lemmatizer); 
		feedbackGen.clear("history"); 
		feedbackGen.loadHistory(dbms.history); 
		feedbackGen.feedbackGen(); 

		File tokenFile = new File("/Users/feili/Dropbox/workspace/NaLIR/src/zfiles/tokens.xml"); 
    	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); 
    	DocumentBuilder builder = factory.newDocumentBuilder();
		
		tokenizer = new ATokenizer(lexiParser, builder.parse(tokenFile), new EnglishLemmatizer()); 
		
		treeList = new ArrayList<BParseTree>(); 
		specificList = new ArrayList<BParseTree>(); 

		adjustor = new BStructureAdjustor(treeList, specificList, dbms, feedbackGen, lemmatizer); 
		translator = new CTranslator(dbms, feedbackGen); 
	}
	
	public void ConductCommand(String command) throws Exception
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
	}
	
	public void useDatabase(String databaseName) throws Exception
	{
		dbms = new RDBMS(databaseName, lemmatizer); 
		feedbackGen.clear("history"); 
		feedbackGen.loadHistory(dbms.history); 
		feedbackGen.feedbackGen(); 
	}

	public void newQuery(String NLQuery) throws ParserConfigurationException, SAXException, IOException, ClassNotFoundException, SQLException
	{
		parseTree = new BParseTree(); 
    	BParseTreeNode.NODEID = 1; 
        feedbackGen.clear("newSentence"); 
		tokenizer.newSentence(NLQuery, parseTree, dbms, feedbackGen); // mapping
		tokenizer.printForCheck(); 
		
//		if(feedbackGen.noMap.isEmpty())
//		{
//			adjustor.structureAdjust(parseTree); 
//			translator.build(this.specificList.get(adjustor.specificChoice)); 
//		}
//		if(!translator.SQL.isEmpty())
//		{
//			System.out.println(translator.SQL); 
//			ArrayList<ArrayList<String>> finalResults = dbms.conductSQL(translator.SQL); 
//			feedbackGen.result(finalResults); 
//		}
		
		feedbackGen.feedbackGen(); 
	}

	
}
