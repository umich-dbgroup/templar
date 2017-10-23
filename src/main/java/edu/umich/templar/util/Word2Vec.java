package edu.umich.templar.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.*;
import java.net.Socket;

public class Word2Vec {
    public static void main(String args[]){
        // double score = getSimilarity("hate", "dislike");
    }

    public static double getSimilarity(String word1, String word2){
        double similarityScore = 0.0;

        try {
            Socket socket = new Socket("localhost", 10000);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(word1 + ", " + word2);
            similarityScore = Double.parseDouble(in.readLine());
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*
        try{
            Process pr = Runtime.getRuntime().exec("python word2vec_client.py " +
                    word1 + " " + word2);
            BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String result = "";
            String line = "";
            while ((line = in.readLine()) != null) {
                result += line;
                System.out.println(result);
            }
            in.close();
            pr.waitFor();
            similarityScore = Double.parseDouble(result);
        } catch (Exception e){
            e.printStackTrace();
        }*/
        return similarityScore;
    }
}