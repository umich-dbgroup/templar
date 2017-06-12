package dataStructure;

import java.util.ArrayList;

public class ASentence 
{
	public ArrayList<String> words = new ArrayList<String>(); 
	
	public ASentence(String NLQuery)
	{
		wordSplit(NLQuery); 
		wordMerge(); 
	}
	
	public void wordSplit(String NLQuery)
	{
		String curWord = ""; 
		while(NLQuery.charAt(NLQuery.length()-1) == '.' || NLQuery.charAt(NLQuery.length()-1) == ' '  || NLQuery.charAt(NLQuery.length()-1) == '?' 
			|| NLQuery.charAt(NLQuery.length()-1) == '\t' || NLQuery.charAt(NLQuery.length()-1) == '\n')
		{
			NLQuery = NLQuery.substring(0, NLQuery.length() - 1); 
		}
		NLQuery += " "; 
		
		boolean ifCited = false; // if the word is in a "", for example "Star Wars". 
		for(int i = 0; i < NLQuery.length(); i++)
		{
			char c = NLQuery.charAt(i); 
			if(c == '\t' || c == '\n' || c == ' ')
			{
				if(ifCited == false)
				{
					words.add(curWord); 
					curWord = ""; 
					while(i < NLQuery.length()-1 && (NLQuery.charAt(i+1) == '\t' || NLQuery.charAt(i+1) == '\n' 
						|| NLQuery.charAt(i+1) == ' ' || NLQuery.charAt(i+1) == ','))
					{
						i++; 
					}
				}
				else
				{
					curWord += NLQuery.charAt(i); 
				}
			}
			else if(c == '\'')
			{
				if(ifCited == false)
				{
					if(NLQuery.charAt(i+1) == 't')
					{
						curWord += NLQuery.charAt(i); 
					}
					else
					{
						words.add(curWord); 
						words.add("\'s"); 
						curWord = ""; 
						if(i < NLQuery.length()-1 && NLQuery.charAt(i+1) == 's')
						{
							i++; 
						}
						i++; 
					}
				}
				else
				{
					curWord += NLQuery.charAt(i); 
				}				
			}
			else if(c == ',')
			{
				if(ifCited == false)
				{
					words.add(curWord); 
					words.add(","); 
					curWord = ""; 
					while(i < NLQuery.length()-1 && (NLQuery.charAt(i+1) == '\t' || NLQuery.charAt(i+1) == '\n' || NLQuery.charAt(i+1) == ' ' 
						|| NLQuery.charAt(i+1) == ','))
					{
						i++; 
					}
				}
				else
				{
					curWord += NLQuery.charAt(i); 
				}
			}
			else if(c == '\"')
			{
				if(ifCited == false)
				{
					ifCited = true; 
				}
				else
				{
					ifCited = false; 
				}
			}
			else
			{
				curWord += NLQuery.charAt(i); 				
			}
		}	
	}

	public void wordMerge() // merge Fei Li into one word "Fei Li"; 
	{
		int state = 0; 
		int start = 0; 
		int end = 0; 
		for(int i = 0; i < words.size(); i++)
		{
			String word = words.get(i); 
			boolean supper = isSupper(word); 
			
			if(state == 0)
			{
				if(supper == true)
				{
					state = 1; 
					start = i; 
					end = i; 
				}
			}
			else if(state == 1)
			{
				if(supper == false)
				{
					if(end > start)
					{
						String mergeWord = words.get(start); 
						for(int j = start+1; j <= end; j++)
						{
							mergeWord += " " + words.get(j); 
						}
						words.set(start, mergeWord); 
						for(int j = start+1; j <= end; j++)
						{
							words.remove(start+1); 
							i--; 
						}
					}
					
					state = 0; 
				}
				else if(supper == true)
				{
					end++; 
				}
			}
		}
		
		if(state == 1)
		{
			if(end > start)
			{
				String mergeWord = words.get(start); 
				for(int j = start+1; j <= end; j++)
				{
					mergeWord += " " + words.get(j); 
				}
				words.set(start, mergeWord); 
				for(int j = start+1; j <= end; j++)
				{
					words.remove(start+1); 
				}
			}
		}
	}
	
	public static boolean isSupper(String word) 
	{
		if(word.charAt(0) <= 'Z' && word.charAt(0) >= 'A')
		{
			return true; 
		}
		else
		{
			return false; 
		}
	}
}
