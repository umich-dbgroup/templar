package tools;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.lti.jawjaw.pobj.POS;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.lexical_db.data.Concept;
import edu.cmu.lti.ws4j.Relatedness;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.northwestern.at.morphadorner.corpuslinguistics.lemmatizer.EnglishLemmatizer;

public class Functions 
{
	public static int Q = 2; 
    private static ILexicalDatabase db = new NictWordNet();
    private static RelatednessCalculator wordnet = new WuPalmer(db); 
	private static List<POS[]> posPairs = wordnet.getPOSPairs();

	public static void main(String [] args)
	{
		String string1 = "lifei"; 
		String string2 = "lifeo"; 
		
		ArrayList<Integer> list1 = new ArrayList<Integer>(); 
		for(int i = 0; i < string1.length(); i++)
		{
			list1.add((int) string1.charAt(i)); 
		}
		ArrayList<Integer> list2 = new ArrayList<Integer>(); 
		for(int i = 0; i < string2.length(); i++)
		{
			list2.add((int) string2.charAt(i)); 
		}
		
		System.out.println(stringEditDistance(list1, list2)); 
	}
	
	@SuppressWarnings("resource")
	public static String readFile(String fileName) throws IOException //read file to a String
	{
		String result = ""; 
		
        File a = new File(fileName);
        if(a.exists())
        {
        	FileInputStream fi = new FileInputStream(a);
        	InputStreamReader isr = new InputStreamReader(fi, "GBk");
        	BufferedReader bfin = new BufferedReader(isr);
        	String rLine = "";
        	while((rLine = bfin.readLine()) != null)
        	{
        		result += rLine + "\n";
        	}		
        }
        else
        {
        	System.out.println("no such file: " + fileName);
        }     
		return result; 
	}
	
	public static double similarity(String word1, String word2, EnglishLemmatizer lemmatizer) throws Exception
	{
		double similarity = 0; 
		
		similarity = wordNetSim(word1, word2, lemmatizer); 
		if(similarity < pqSim(word1, word2))
		{
			similarity = pqSim(word1, word2); 
		}
		similarity += pqSim(word1, word2)/10; 
		
		return similarity; 
	}
	
	public static double wordNetSim(String word1, String word2, EnglishLemmatizer lemmatizer)
	{
		double sim = wordNetSimCompute(word1, word2); 
		if(wordnet.calcRelatednessOfWords(lemmatizer.lemmatize(word1), word2) > sim)
		{
			sim = wordNetSimCompute(lemmatizer.lemmatize(word1), word2); 
		}
		if(wordnet.calcRelatednessOfWords(lemmatizer.lemmatize(word1), lemmatizer.lemmatize(word2)) > sim)
		{
			sim = wordNetSimCompute(lemmatizer.lemmatize(word1), lemmatizer.lemmatize(word2)); 
		}
		if(wordnet.calcRelatednessOfWords(word1, lemmatizer.lemmatize(word2)) > sim)
		{
			sim = wordNetSimCompute(word1, lemmatizer.lemmatize(word2)); 
		}
		
		return sim; 
	}
	
	public static double wordNetSimCompute(String word1, String word2)
	{
		double sim = -1D;
		for(POS[] posPair: posPairs) 
		{
		    List<Concept> synsets1 = (List<Concept>)db.getAllConcepts(word1, posPair[0].toString());
		    List<Concept> synsets2 = (List<Concept>)db.getAllConcepts(word2, posPair[1].toString());

		    for(Concept synset1: synsets1)
		    {
		        for (Concept synset2: synsets2)
		        {
		            Relatedness relatedness = wordnet.calcRelatednessOfSynset(synset1, synset2);
		            double score = relatedness.getScore();
		            if (score > sim) { 
		                sim = score;
		            }
		        }
		    }
		}

		if (sim == -1D) 
		{
		    sim = 0.0;
		}
		
		return sim; 
	}
	
	public static double pqSim(String a, String b)
	{
		a = a.toLowerCase(); 
		b = b.toLowerCase(); 
		
		double similarity = 0; 
		String [] arrayA = new String [a.length() - Q + 1]; 
		for(int i = 0; i < arrayA.length; i++)
		{
			arrayA[i] = ""; 
			for(int j = 0; j < Q; j++)
			{
				arrayA[i] += a.charAt(i+j); 
			}
		}
		
		String [] arrayB = new String [b.length() - Q + 1]; 
		for(int i = 0; i < arrayB.length; i++)
		{
			arrayB[i] = ""; 
			for(int j = 0; j < Q; j++)
			{
				arrayB[i] += b.charAt(i+j); 
			}
		}
		
		int same = 0; 
		for(int i = 0; i < arrayA.length; i++)
		{
			for(int j = 0; j < arrayB.length; j++)
			{
				if(arrayA[i].equals(arrayB[j]))
				{
					same++; 
					arrayA[i] = "a"; 
					arrayB[j] = "b"; 
				}
			}
		}
		
		if(arrayA.length != 0 || arrayB.length != 0)
		{
			similarity = 2*(double)same/((double)arrayA.length + (double)arrayB.length); 
		}

		return Math.sqrt(similarity); 
	}
	
	public static Object depthClone(Object srcObj)
	{  
        Object cloneObj = null;  
        try {  
            ByteArrayOutputStream out = new ByteArrayOutputStream();  
            ObjectOutputStream oo = new ObjectOutputStream(out);  
            oo.writeObject(srcObj);  
              
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());  
            ObjectInputStream oi = new ObjectInputStream(in);  
            cloneObj = oi.readObject();           
        } catch (IOException e) {  
            e.printStackTrace();  
        } catch (ClassNotFoundException e) {  
            e.printStackTrace();  
        }  
        return cloneObj;  
    }  
	
	public static int stringEditDistance(ArrayList<Integer> string1, ArrayList<Integer> string2)
	{
		int [][] matrix = new int [string1.size()][string2.size()]; 
		matrix[0][0] = 0; 
		for(int i = 0; i < string1.size(); i++)
		{
			matrix[i][0] = i; 
		}
		for(int j = 0; j < string2.size(); j++)
		{
			matrix[0][j] = j; 
		}
		
		for(int i = 1; i < string1.size(); i++)
		{
			for(int j = 1; j < string2.size(); j++)
			{
				if(matrix[i-1][j] < matrix[i][j-1])
				{
					matrix[i][j] = matrix[i-1][j] + 1; 
				}
				else
				{
					matrix[i][j] = matrix[i][j-1] + 1; 					
				}
				
				if(string1.get(i) == string2.get(j) && matrix[i-1][j-1] < matrix[i][j])
				{
					matrix[i][j] = matrix[i-1][j-1]; 
				}
			}
		}

		return matrix[string1.size()-1][string2.size()-1]; 
	}
	
	public static boolean isNumeric(String str)
	{ 
		try 
		{ 
			Integer.parseInt(str); 
			return true; 
		} 
		catch (NumberFormatException e) 
		{ 
			return false; 
		} 
	}
}
