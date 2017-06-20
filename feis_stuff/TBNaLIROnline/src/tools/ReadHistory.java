package tools;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class ReadHistory 
{
	public static void main(String [] args) throws ClassNotFoundException, SQLException, IOException
	{
		ReadHistory read = new ReadHistory(); 
		read.readHistory(); 
	}
	
	public void readHistory() throws SQLException, ClassNotFoundException, IOException
	{
		String driver = "com.mysql.jdbc.Driver"; 
		String db_url = "jdbc:mysql://127.0.0.1:3306/";
		String user = "root";
		String password = "";
		Class.forName(driver);
		Connection conn = DriverManager.getConnection(db_url, user, password);
		Statement statement = conn.createStatement(); 

		String file = "/Users/feili/Dropbox/workspace/TBNaLIR/src/zfiles/testingSet"; 
		String fileContent = BasicFunctions.readFile(file); 
		
		Scanner scan = new Scanner(fileContent); 
		String category = ""; 
		while(scan.hasNextLine())
		{
			String qid = ""; 
			String query = scan.nextLine(); 

			if(query.isEmpty())
			{
				continue; 
			}
			
			if(query.charAt(0) >= 'A' && query.charAt(0) <= 'Z')
			{
				category = query; 
				continue; 
			}
			else if(query.charAt(0) >= '0' && query.charAt(0) <= '9')
			{
				qid = query.substring(0, query.indexOf(". ")); 
				query = query.substring(query.indexOf("show")); 
			}
			
			String insert = "INSERT mas.testingSet VALUES (" + qid + ", " + "'" + query + "', '" + category + "'" + ")"; 
			statement.executeUpdate(insert); 
		}
	}
}
