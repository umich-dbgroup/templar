package edu.umich.templar.main;

import edu.umich.templar.db.Database;
import edu.umich.templar.log.LogCountGraph;
import edu.umich.templar.log.graph.LogGraph;
import edu.umich.templar.scorer.LogCountGraphScorer;
import edu.umich.templar.scorer.LogGraphScorer;
import edu.umich.templar.task.QueryTask;

import java.util.List;

public class Templar extends FragmentMapper {
    public Templar(Database database, List<QueryTask> queryTasks, boolean typeOracle, LogGraph logGraph) {
        super(database, queryTasks, typeOracle);

        // this.setScorer(new LogCountGraphScorer(logGraph));
        this.setScorer(new LogGraphScorer(logGraph));
    }
}
