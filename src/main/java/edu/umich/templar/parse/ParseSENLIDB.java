package edu.umich.templar.parse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Created by cjbaik on 9/8/17.
 *
 * Parses SENLIDB json files into files with 1 SQL query per line.
 */
public class ParseSENLIDB {
    public static void extractSQL(String infile, String outfile) {
        try {
            JSONParser parser = new JSONParser();
            PrintWriter writer = new PrintWriter(new FileWriter(outfile));

            Object obj = parser.parse(new FileReader(infile));
            JSONArray jsonArray = (JSONArray) obj;

            for (Object arrayObj : jsonArray) {
                JSONObject jsonObj = (JSONObject) arrayObj;

                writer.println((String) jsonObj.get("sql"));
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main (String[] args) {
        // For training files
        // String infile = "data/raw/nlidb-datasets-master/SENLIDB/train.json";
        // String outfile = "data/senlidb/train_queries.txt";

        // For test files
        String infile = "data/raw/nlidb-datasets-master/SENLIDB/test.json";
        String outfile = "data/senlidb/test_queries.txt";

        ParseSENLIDB.extractSQL(infile, outfile);

    }
}
