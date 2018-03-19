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

    private int portNum;

    public Word2Vec(int portNum) {
        this.portNum = portNum;
    }

    public double getSimilarity(String word1, String word2){
        if (word1.equals(word2)) return 1.0;

        double similarityScore = 0.0;

        try {
            Socket socket = new Socket("localhost", this.portNum);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println(word1.toLowerCase() + ", " + word2.toLowerCase());
            similarityScore = Double.parseDouble(in.readLine());

            // normalize to 0, 1
            similarityScore  = (similarityScore + 1) / 2;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return similarityScore;
    }
}
