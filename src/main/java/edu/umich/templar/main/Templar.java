package edu.umich.templar.main;

import edu.umich.templar.db.Database;
import edu.umich.templar.log.LogGraph;

public class Templar extends FragmentMapper {
    private LogGraph logGraph;

    public Templar(Database database, String filename, boolean typeOracle, LogGraph logGraph) {
        super(database, filename, typeOracle);

        this.logGraph = logGraph;
    }
}
