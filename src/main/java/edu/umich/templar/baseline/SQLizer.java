package edu.umich.templar.baseline;

import edu.umich.templar.config.TemplarConfig;
import edu.umich.templar.db.*;
import edu.umich.templar.main.CoreArchitecture;
import edu.umich.templar.scorer.SQLizerScorer;
import edu.umich.templar.task.*;
import edu.umich.templar.util.Similarity;

import java.util.List;

public class SQLizer extends CoreArchitecture {
    // Activate the SQLizer join penalty indiscriminately
    // private boolean joinScore;

    public SQLizer(Database database, String candCacheFilename, List<QueryTask> queryTasks, boolean typeOracle) {
        super(database, candCacheFilename, queryTasks, typeOracle);

        this.db = database;
        // this.joinScore = joinScore;
        this.setScorer(new SQLizerScorer(this.db));

        try {
            this.sim = new Similarity(10000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        String dbName = args[0];
        String prefix = "data/" + dbName + "/" + dbName;
        String fkpkFile = prefix + ".fkpk.json";
        String fragsFile = prefix + "_keywords.csv";
        String joinsFile = prefix + "_joins.csv";
        String mainAttrsFile = prefix + ".main_attrs.json";
        String projAttrsFile = prefix + ".proj_attrs.json";
        String candCacheFilename = prefix + ".cands.cache";

        Boolean typeOracle = Boolean.valueOf(args[1]);
        // Boolean joinScore = Boolean.valueOf(args[2]);

        // Read config for database info
        Database db = new Database(TemplarConfig.getProperty("dbhost"),
                TemplarConfig.getIntegerProperty("dbport"),
                TemplarConfig.getProperty("dbuser"),
                TemplarConfig.getProperty("dbpassword"),
                dbName, fkpkFile, mainAttrsFile, projAttrsFile);

        SQLizer sqlizer = new SQLizer(db, candCacheFilename,
                QueryTaskReader.readQueryTasks(fragsFile, joinsFile), typeOracle);

        if (args.length >= 4) {
            sqlizer.execute(Integer.valueOf(args[3]));
        } else {
            sqlizer.execute();
        }
    }
}
