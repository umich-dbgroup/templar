package edu.umich.templar.baseline;

import edu.umich.templar.db.*;
import edu.umich.templar.main.FragmentMapper;
import edu.umich.templar.scorer.SQLizerScorer;
import edu.umich.templar.task.*;
import edu.umich.templar.util.Similarity;

public class SQLizer extends FragmentMapper {
    // Activate the SQLizer join penalty indiscriminately
    private boolean joinScore;

    public SQLizer(Database database, String filename, boolean typeOracle, boolean joinScore) {
        super(database, filename, typeOracle);

        this.db = database;
        this.joinScore = joinScore;
        this.setScorer(new SQLizerScorer(this.db, this.joinScore));

        try {
            this.sim = new Similarity(10000);
            this.queryTasks = QueryTaskReader.readQueryTasks(filename);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        String dbHost = args[0];
        Integer dbPort = Integer.valueOf(args[1]);
        String dbUser = args[2];
        String dbPass = args[3].equalsIgnoreCase("null")? null : args[3];
        Boolean typeOracle = Boolean.valueOf(args[4]);
        Boolean joinScore = Boolean.valueOf(args[5]);

        Database database = new Database(dbHost, dbPort, dbUser, dbPass,
                "mas", "data/mas/mas.edges.json", "data/mas/mas.main_attrs.json");
        SQLizer sqlizer = new SQLizer(database, "data/mas/mas_all_fragments.csv", typeOracle, joinScore);

        if (args.length >= 7) {
            sqlizer.execute(Integer.valueOf(args[6]));
        } else {
            sqlizer.execute();
        }
    }
}
