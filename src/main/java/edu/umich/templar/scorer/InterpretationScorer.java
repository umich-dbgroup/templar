package edu.umich.templar.scorer;

import edu.umich.templar.db.*;
import edu.umich.templar.task.Interpretation;

import java.util.List;

public interface InterpretationScorer {
    double score(Interpretation interp);
}
