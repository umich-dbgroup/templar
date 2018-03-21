package edu.umich.templar._old.tools;

import java.util.List;
import java.util.Map;

import edu.umich.templar._old.rdbms.MappedSchemaElement;

import edu.umich.templar._old.dataStructure.ParseTreeNode;

import edu.cmu.lti.jawjaw.pobj.POS;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.lexical_db.data.Concept;
import edu.cmu.lti.ws4j.Relatedness;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.northwestern.at.morphadorner.corpuslinguistics.lemmatizer.EnglishLemmatizer;
import edu.umich.templar._old.Constants;
import edu.umich.templar.util.Word2Vec;
import info.debatty.java.stringsimilarity.Cosine;
import org.apache.commons.collections4.map.LRUMap;
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
		// System.out.println(similarity("publication", "publication_num"));
	}
	
	public static String lemmatize(String word) throws Exception
	{
		if(lemmatizer == null)
		{
			lemmatizer = new EnglishLemmatizer(); 
		}
		return lemmatizer.lemmatize(word); 
	}

    static Map<String, Double> cosineSimCache = new LRUMap<>(Constants.MAX_CACHE_SIZE);
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
            if (element.mappedValues.size() > 0) element.similarity = 1.0;
            else element.similarity = 0.0;
            /*
            double actual = Double.parseDouble(nodeLabel);
            List<Double> sims = new ArrayList<>();
            for (String val : element.mappedValues) {
                double diff = Math.abs(actual - Double.parseDouble(val));
                sims.add(diff);
            }

            Collections.sort(sims, Collections.reverseOrder());

            double sum = 0;
            for (int i = 0; i < element.mappedValues.size(); i++) {
                sum += Double.parseDouble(element.mappedValues.get(i));
            }

            double size = Double.parseDouble(nodeLabel) * element.mappedValues.size();

            double penalty = Math.abs(sum - size) / size;
            // Set a threshold for the penalty
            element.similarity = Math.max(1 - penalty, Constants.MIN_SIM);*/
		} else {
			double [] sims = new double[element.mappedValues.size()];
			List<String> mappedValues = element.mappedValues;
			for(int i = 0; i < mappedValues.size(); i++)
			{
                String key = nodeLabel + ":" + mappedValues.get(i);

                Double sim = cosineSimCache.get(key);
                if (sim == null) {
                    double cosineSim = SimFunctions.cosineSim(nodeLabel, mappedValues.get(i));
                    double lemmatizedSim = SimFunctions.cosineSim(lemmatize(nodeLabel, "NN"), lemmatize(mappedValues.get(i), "NN"));
                    sim = Math.max(lemmatizedSim, cosineSim);
                    cosineSimCache.put(key, sim);
                }
                sims[i] = sim;
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
            /*
            double sum = 0;
            double[] weights = {0.6, 0.3, 0.1};

            // The bottom threshold should be at most 0.95 if there is no mapped value for a position
            double bottomThreshold = Math.min(0.95, sims[0]);
            for (int i = 0; i < weights.length; i++) {
                if (i >= (sims.length - 1)) {
                    sum += bottomThreshold * weights[i];
                } else {
                    sum += sims[i] * weights[i];
                }
            }

			element.similarity = sum;*/
            element.similarity = sims[0];

			// Special case: we penalize if value text attribute is not a proper noun or adjective
            if (element.schemaElement.type.equals("text") && !(treeNode.pos.equals("NNP") || treeNode.pos.equals("JJ"))) {
                element.similarity *= 0.95;
            }
		}
	}
	
	public static boolean ifSchemaSimilar(String word1, String word1pos, String word2, String word2pos) throws Exception
	{
		double similarity = similarity(word1, word1pos, word2, word2pos);
		if(similarity > 0.5)
		{
			return true; 
		}
		else
		{
			return false; 
		}
	}
	
	public static double similarity(String word1, String word1pos, String word2, String word2pos)
	{
        /*
		double similarity;

		double wordnetScore = wordNetSim(word1, word1pos, word2, word2pos);
        double pqScore = pqSim(word1, word2);
		if(wordnetScore < pqScore) {
			similarity = pqScore;
		} else {
            // this seems like an arbitrary boost to me.
            //similarity += pqSim(word1, word2) / 10;

            similarity = 0.8 * wordnetScore + 0.2 * pqScore;
        }
		
		return similarity;
	    */

        if (word1.equals(word2)) return 1.0;
        return word2vecSim(word1, word1pos, word2, word2pos);
    }

    public static String convertStanfordPosToMorphadornerPos(String pos) {
        if (pos.equals("CD")) {
            return "numeral";
        } else if (pos.startsWith("JJ")) {
            return "adjective";
        } else if (pos.startsWith("NN")) {
            return "noun";
        } else if (pos.startsWith("VB")) {
            return "verb";
        } else {
            return null;
        }
    }

	public static double wordNetSim(String word1, String word1pos, String word2, String word2pos) throws Exception
	{
		// double sim = wordNetSimCompute(word1, word2);
		String [] words1 = word1.split("_"); 
		String [] words2 = word2.split("_"); 

		double maxScore = 0.0;
		for(int i = 0; i < words1.length; i++)
		{
			for(int j = 0; j < words2.length; j++)
			{
                String word1lemma;
                if (word1pos != null) {
                    word1lemma = lemmatizer.lemmatize(words1[i], convertStanfordPosToMorphadornerPos(word1pos));
                } else {
                    word1lemma = lemmatizer.lemmatize(words1[i]);
                }

                String word2lemma;
                if (word2pos != null) {
                    word2lemma = lemmatizer.lemmatize(words2[j], convertStanfordPosToMorphadornerPos(word2pos));
                } else {
                    word2lemma = lemmatizer.lemmatize(words2[j]);
                }

				double sim_part = wordNetSimCompute(word1lemma, word2lemma);
                if (sim_part > maxScore) {
                    maxScore = sim_part;
                }
                /*
				if(sim_part > sim)
				{
					sim = sim_part; 
				}*/
			}
		}
        return maxScore;
		// return sim;
	}

    static Cosine cosine = new Cosine(2);
    static Map<String, Map<String, Integer>> profileCache = new LRUMap<>(Constants.MAX_CACHE_SIZE);

    public static double cosineSim(String word1, String word2) {
        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();

        if (word1.equals(word2)) return 1.0;

        Map<String, Integer> word1Profile = profileCache.get(word1);
        if (word1Profile == null) {
            word1Profile = cosine.getProfile(word1);
            profileCache.put(word1, word1Profile);
        }

        Map<String, Integer> word2Profile = profileCache.get(word2);
        if (word2Profile == null) {
            word2Profile = cosine.getProfile(word2);
            profileCache.put(word2, word2Profile);
        }

        return cosine.similarity(word1Profile, word2Profile);
    }

    static Map<String, Double> cachedSim = new LRUMap<>(Constants.MAX_CACHE_SIZE);
    static Word2Vec word2vec = new Word2Vec(10000);
    public static double word2vecSim(String word1, String word2) {
        String combined;
        if (word1.compareTo(word2) < 0) {
            combined = word1 + ":" + word2;
        } else {
            combined = word2 + ":" + word1;
        }
        Double sim = cachedSim.get(combined);
        if (sim == null) {
            sim = word2vec.getSimilarity(word1, word2);

            // scale so it's 0 to 1
            sim = (sim + 1.0) / 2;

            cachedSim.put(combined, sim);
        }
        return sim;
    }

    static Map<String, String> lemmaCache = new LRUMap<>(Constants.MAX_CACHE_SIZE);
    public static String lemmatize(String word, String wordPos) {
        String key = word + ":" + wordPos;
        String cached = lemmaCache.get(key);
        if (cached != null) {
            return cached;
        } else {
            wordPos = convertStanfordPosToMorphadornerPos(wordPos);
            String result = lemmatizer.lemmatize(word, wordPos);
            lemmaCache.put(key, result);
            return result;
        }
    }

	public static double word2vecSim(String word1, String word1pos, String word2, String word2pos) {
        String [] words1 = word1.split("_");
        String [] words2 = word2.split("_");

        if (word1pos != null) word1pos = convertStanfordPosToMorphadornerPos(word1pos);
        if (word2pos != null) word2pos = convertStanfordPosToMorphadornerPos(word2pos);

        double maxScore = 0.0;
        for (int i = 0; i < words1.length; i++) {
            for (int j = 0; j < words2.length; j++) {
                String first = words1[i];
                String firstLemma;
                if (word1pos != null) {
                    firstLemma = lemmatizer.lemmatize(first, word1pos);
                } else {
                    firstLemma = lemmatizer.lemmatize(first);
                }
                String second = words2[j];
                String secondLemma;
                if (word2pos != null) {
                    secondLemma = lemmatizer.lemmatize(second, word2pos);
                } else {
                    secondLemma = lemmatizer.lemmatize(second);
                }

                // Ignore POS issues if they are same word
                if (first.equals(second)) maxScore = 1.0;

                double sim = word2vecSim(firstLemma, secondLemma);
                maxScore = Math.max(maxScore, sim);

                String firstPlural = firstLemma + "s";
                sim = word2vecSim(firstPlural, secondLemma);
                maxScore = Math.max(maxScore, sim);

                String secondPlural = secondLemma + "s";
                sim = word2vecSim(firstLemma, secondPlural);
                maxScore = Math.max(maxScore, sim);

                sim = word2vecSim(firstPlural, secondPlural);
                maxScore = Math.max(maxScore, sim);
            }
        }
        return maxScore;
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
