package edu.umich.tbnalir.tools;

import java.util.ArrayList;
import java.util.List;

import edu.umich.tbnalir.rdbms.MappedSchemaElement;

import edu.umich.tbnalir.dataStructure.ParseTreeNode;

import edu.cmu.lti.jawjaw.pobj.POS;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.lexical_db.data.Concept;
import edu.cmu.lti.ws4j.Relatedness;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.northwestern.at.morphadorner.corpuslinguistics.lemmatizer.EnglishLemmatizer;
import org.apache.commons.lang3.math.NumberUtils;

public class SimFunctions 
{
	private static int Q = 2; 

	private static EnglishLemmatizer lemmatizer; 

	private static ILexicalDatabase db = new NictWordNet();
    private static RelatednessCalculator wordnet = new WuPalmer(db); 
	private static List<POS[]> posPairs = wordnet.getPOSPairs();

	public static void main(String [] args) throws Exception
	{
		System.out.println(similarity("publication", "publication_num")); 
	}
	
	public static String lemmatize(String word) throws Exception
	{
		if(lemmatizer == null)
		{
			lemmatizer = new EnglishLemmatizer(); 
		}
		return lemmatizer.lemmatize(word); 
	}
	
	public static void similarity(ParseTreeNode treeNode, MappedSchemaElement element)
	{
		if(element.similarity > 0)
		{
			return; 
		}

		String nodeLabel = treeNode.label;

		if(NumberUtils.isCreatable(nodeLabel) &&
				(element.schemaElement.type.equals("int") || element.schemaElement.type.equals("double")))
		{
            double sum = 0;
            for (int i = 0; i < element.mappedValues.size(); i++) {
                sum += Double.parseDouble(element.mappedValues.get(i));
            }

            double size = Double.parseDouble(nodeLabel) * element.mappedValues.size();

            double penalty = Math.abs(sum - size) / size;
            // Set a threshold for the penalty
            element.similarity = Math.max(1 - penalty, 0.5);
		} else {
			double [] sims = new double[element.mappedValues.size()];
			List<String> mappedValues = element.mappedValues;
			for(int i = 0; i < mappedValues.size(); i++)
			{
				sims[i] = SimFunctions.pqSim(nodeLabel, mappedValues.get(i)); 
			}			
			
			for(int i = 0; i < mappedValues.size(); i++)
			{
				for(int j = i + 1; j < mappedValues.size(); j++)
				{
					if(sims[j] > sims[i])
					{
						double tempSim = sims[j]; 
						sims[j] = sims[i]; 
						sims[i] = tempSim; 
						String tempValue = mappedValues.get(j); 
						mappedValues.set(j, mappedValues.get(i)); 
						mappedValues.set(i, tempValue); 
					}
				}
			}

            // Limit max element mapped values size here
            int maxSize = Math.min(10, element.mappedValues.size());
            element.mappedValues = element.mappedValues.subList(0, maxSize);

			element.choice = 0;

            // Average the top 3 (if not exist, provide 0.95)
            double sum = 0;
            double max = 3;
            double[] weights = {0.6, 0.3, 0.1};

            // The bottom threshold should be at most 0.95 if there is no mapped value for a position
            double bottomThreshold = Math.min(0.95, sims[0]);
            for (int i = 0; i < max; i++) {
                if (i >= (sims.length - 1)) {
                    sum += bottomThreshold * weights[i];
                } else {
                    sum += sims[i] * weights[i];
                }
            }

			element.similarity = sum;

			// Special case: we penalize if value text attribute is not a proper noun or adjective
            if (element.schemaElement.type.equals("text") && !(treeNode.pos.equals("NNP") || treeNode.pos.equals("JJ"))) {
                element.similarity *= 0.95;
            }
		}
	}
	
	public static boolean ifSchemaSimilar(String word1, String word2) throws Exception
	{
		double similarity = similarity(word1, word2); 
		if(similarity > 0.5)
		{
			return true; 
		}
		else
		{
			return false; 
		}
	}
	
	public static double similarity(String word1, String word2) throws Exception
	{
		double similarity;

		double wordnetScore = wordNetSim(word1, word2);
        double pqScore = pqSim(word1, word2);
		if(wordnetScore < pqScore) {
			similarity = pqScore;
		} else {
            // TODO: this seems like an arbitrary boost to me.
            //similarity += pqSim(word1, word2) / 10;

            similarity = 0.8 * wordnetScore + 0.2 * pqScore;
        }
		
		return similarity; 
	}
	
	public static double wordNetSim(String word1, String word2) throws Exception
	{
		// double sim = wordNetSimCompute(word1, word2);
		String [] words1 = word1.split("_"); 
		String [] words2 = word2.split("_"); 

        double totalScoreForI = 0.0;
		for(int i = 0; i < words1.length; i++)
		{
            double maxScoreForThisI = 0.0;
			for(int j = 0; j < words2.length; j++)
			{
				double sim_part = wordNetSimCompute(lemmatizer.lemmatize(words1[i]), lemmatizer.lemmatize(words2[j]));
                if (sim_part > maxScoreForThisI) {
                    maxScoreForThisI = sim_part;
                }
                /*
				if(sim_part > sim)
				{
					sim = sim_part; 
				}*/
			}
            totalScoreForI += maxScoreForThisI;
		}

        return totalScoreForI / words1.length;
		
		// return sim;
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
		            if (score > sim) 
		            { 
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
		if(a.isEmpty() || b.isEmpty())
		{
			return 0; 
		}
		
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
}
