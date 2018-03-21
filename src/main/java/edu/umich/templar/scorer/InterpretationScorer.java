package edu.umich.templar.scorer;

import edu.umich.templar.db.*;
import java.util.List;

public interface InterpretationScorer {
    double score(List<MatchedDBElement> interp);
}
