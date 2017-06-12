package architecture;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

import dataStructure.Query;
import rdbms.RDBMS;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

public class ExperimentsInterface 
{
	LexicalizedParser lexiParser; 
	RDBMS db; 
	Document tokens; 

	public static void main(String [] args) throws Exception
	{
		String driver = "com.mysql.jdbc.Driver"; 
		String db_url = "jdbc:mysql://127.0.0.1:3306/";
		String user = "root";
		String password = "";
		Class.forName(driver);
		Connection conn = DriverManager.getConnection(db_url, user, password);
		
		Statement statement = conn.createStatement(); 
		ResultSet rs = statement.executeQuery("SELECT qid, NLQ FROM mas.testingSet"); 

		ArrayList<Integer> ids = new ArrayList<Integer>(); 
		ArrayList<String> testingSet = new ArrayList<String>(); 
		while(rs.next())
		{
			ids.add(rs.getInt(1)); 
			testingSet.add(rs.getString(2)); 
		}	
		
		ExperimentsInterface experiment = new ExperimentsInterface(); 
		experiment.runExperiments(ids, testingSet); 
	}
	
	public ExperimentsInterface() throws Exception
	{
		lexiParser = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz"); 

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); 
    	DocumentBuilder builder = factory.newDocumentBuilder();
		tokens = builder.parse(new File("/Users/feili/Dropbox/workspace/NaLIRWeb/src/zfiles/tokens.xml")); 
		db = new RDBMS("mas"); 
	}
	
	public void runExperiments(ArrayList<Integer> ids, ArrayList<String> testingSet) throws Exception
	{
		for(int i = 0; i < testingSet.size(); i++)
		{
			Query query = new Query(testingSet.get(i), db.schemaGraph);
			components.StanfordNLParser.parse(query, lexiParser); 
			components.NodeMapper.phraseProcess(query, db, tokens); 
			components.EntityResolution.entityResolute(query); 
			components.TreeStructureAdjustor.treeStructureAdjust(query, db); 
			
			String toPrint = ""; 
			toPrint += ids.get(i) + ". " + testingSet.get(i) + "\n"; 
			toPrint += query.parseTree.toString(); 
			toPrint += tools.PrintForCheck.allParseTreeNodePrintForCheck(query.parseTree); 
			for(int j = 0; j < query.entities.size(); j++)
			{
				toPrint += query.entities.get(j).printForCheck() + "\n"; 
			}
			toPrint += "=================\n"; 
			tools.BasicFunctions.writeFile("/Users/feili/Dropbox/workspace/TBNaLIROnline/src/zfiles/testingResults", toPrint, true); 

			System.out.println(ids.get(i) + ". " + testingSet.get(i)); 
		}		
	}
}
