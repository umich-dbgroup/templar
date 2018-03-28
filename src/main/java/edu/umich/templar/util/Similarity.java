package edu.umich.templar.util;

import edu.northwestern.at.morphadorner.corpuslinguistics.lemmatizer.PorterStemmerLemmatizer;

public class Similarity {
    private Word2Vec word2vec;
    private PorterStemmerLemmatizer lemmatizer;

    public Similarity(int word2vecPort) {
        this.word2vec = new Word2Vec(word2vecPort);
        this.lemmatizer = new PorterStemmerLemmatizer();
    }

    public double sim(String word1, String word2) {
        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();

        if (word1.equals(word2)) return 1.0;

        // Special case: Use the Stemmer only if word1 and word2 are both single tokens
        String[] word1Split = word1.split(" ");
        String[] word2Split = word2.split(" ");
        if (word1Split.length == 1 && word2Split.length == 1) {
            if (this.lemmatizer.lemmatize(word1).equals(this.lemmatizer.lemmatize(word2))) {
                return 1.0;
            }
        }

        return this.word2vec.getSimilarity(word1, word2);
    }

}
