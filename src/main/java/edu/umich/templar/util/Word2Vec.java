package edu.umich.templar.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Word2Vec {
    public static void main(String args[]){
        // double score = getSimilarity("hate", "dislike");
    }

    private static int MAX_RETRY = 5;
    private int portNum;

    public Word2Vec(int portNum) {
        this.portNum = portNum;
    }

    private double getSimilarity(String word1, String word2, int retry) {
        if (word1.equals(word2)) return 1.0;

        if (retry > MAX_RETRY) {
            throw new RuntimeException("Get similarity failed for <" + word1 + ", " + word2 + "> after " + retry + " retries.");
        }

        double similarityScore;

        try {
            Socket socket = new Socket("localhost", this.portNum);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println(word1.toLowerCase() + ", " + word2.toLowerCase());
            similarityScore = Double.parseDouble(in.readLine());
            socket.close();

            // normalize to 0, 1
            similarityScore  = (similarityScore + 1) / 2;
        } catch (Exception e) {
            // Retry up to n times.
            return this.getSimilarity(word1, word2, retry + 1);
        }
        return similarityScore;
    }

    public double getSimilarity(String word1, String word2){
        return this.getSimilarity(word1, word2, 0);
    }
}
