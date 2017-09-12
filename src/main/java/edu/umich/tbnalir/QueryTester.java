package edu.umich.tbnalir;

import com.esotericsoftware.minlog.Log;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.*;
import edu.umich.tbnalir.dataStructure.Query;
import edu.umich.tbnalir.rdbms.RDBMS;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cjbaik on 9/7/17.
 */
public class QueryTester {
    public static void main (String[] args) {
        String dbName = "mas";
        String prefix = "data/mas/mas";

        RDBMS db;
        try {
            db = new RDBMS(dbName, prefix);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        LexicalizedParser lexiParser = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");

        List<String> queries = new ArrayList<>();
        queries.add("can undergrads take 595?");
        queries.add("what is the easiest class I can take to fulfill the capstone requirement?");
        queries.add("can I take eecs 281 and eecs 370 in the same semester?");

        for (String queryStr : queries) {
            Log.info("=============");
            Log.info("QUERY: " + queryStr);
            Log.info("=============");
            Query query = new Query(queryStr, db.schemaGraph);

            Log.info("Parsing query with NL parser...");
            // StanfordNLParser.parse(query, lexiParser);

            List<CoreLabel> rawWords = Sentence.toCoreLabelList(query.sentence.outputWords); // use Stanford parser to parse a sentence;
            Tree parse = lexiParser.apply(rawWords);
            TreebankLanguagePack tlp = new PennTreebankLanguagePack();
            GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
            GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
            List<TypedDependency> dependencyList = gs.typedDependencies(false);

            for (TaggedWord tagged : parse.taggedYield()) {
                System.out.println(tagged.word() + ": " + tagged.tag());
            }

            System.out.println();

            for (TypedDependency dep : dependencyList) {
                System.out.println(dep);
            }

            System.out.println();
        }
    }
}
