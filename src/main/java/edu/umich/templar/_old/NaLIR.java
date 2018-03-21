package edu.umich.templar._old;

import com.esotericsoftware.minlog.Log;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.umich.templar._old.components.*;
import edu.umich.templar._old.dataStructure.NLSentence;
import edu.umich.templar._old.dataStructure.Query;
import edu.umich.templar._old.rdbms.RDBMS;
import edu.umich.templar._old.tools.PrintForCheck;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/**
 * Created by cjbaik on 9/5/17.
 */
public class NaLIR {
    public static void main(String[] args) {
        RDBMS db;
        try {
            db = new RDBMS("mas", "data/mas/mas");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        // String queryStr = "return me the publications written by \"H. V. Jagadish\" and \"Yunyao Li\" on PVLDB after 2005.";
        String queryStr = "return me the homepage of PVLDB.";
        Log.info("QUERY: " + queryStr);
        Query query = new Query(queryStr, db.schemaGraph);

        Log.info("Parsing query with NL parser...");
        LexicalizedParser lexiParser = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
        StanfordNLParser.parse(query, lexiParser);

        Log.info("FragmentTask nodes to token types...");
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document tokens = builder.parse(new File("libs/tokens.xml"));
            NodeMapper.phraseProcess(query, db, tokens);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        System.out.println("PARSE TREE:");
        System.out.println(query.parseTree);

        Log.info("Resolving entities (if appear multiple times in same query)...");
        EntityResolution.entityResolute(query);
        System.out.println("Entities: " + query.entities);
        System.out.println();

        Log.info("Adjusting tree structure if necessary...");
        TreeStructureAdjustor.treeStructureAdjust(query, db);
        System.out.println("Adjusted tree count: " + query.adjustedTrees.size());
        System.out.println("QUERY TREE:");
        System.out.println(query.queryTree);
        System.out.println();

        Log.info("Explaining query...");
        try {
            Explainer.explain(query);
            for (NLSentence sentence : query.NLSentences) {
                sentence.printForCheck();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println();

        Log.info("Translating SQL...");
        SQLTranslator.translate(query, db);
        System.out.println();

        Log.info("Printing query tree...");
        // System.out.println(query.queryTree.allNodes.size());
        // query.NLSentences.get(query.queryTreeID).printForCheck();
        // System.out.println(query.queryTree.toString());
        PrintForCheck.allParseTreeNodePrintForCheck(query.queryTree);
    }
}
