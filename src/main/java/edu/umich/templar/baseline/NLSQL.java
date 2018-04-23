package edu.umich.templar.baseline;

import edu.umich.templar.db.Database;
import edu.umich.templar.log.NLSQLLog;
import edu.umich.templar.main.PipelineCore;
import edu.umich.templar.scorer.NLSQLScorer;
import edu.umich.templar.task.QueryTask;
import edu.umich.templar.util.Similarity;

import java.util.List;

public class NLSQL extends PipelineCore {
    public NLSQL(Database database, String candCacheFilename, List<QueryTask> queryTasks, boolean typeOracle,
                 NLSQLLog nlsqlLog) {
        super(database, candCacheFilename, queryTasks, typeOracle);

        this.db = database;
        this.setScorer(new NLSQLScorer(database, nlsqlLog));

        try {
            this.sim = new Similarity(10000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
