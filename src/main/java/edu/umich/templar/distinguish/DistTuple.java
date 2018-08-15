package edu.umich.templar.distinguish;

import edu.umich.templar.task.Interpretation;

import java.util.HashMap;
import java.util.Map;

public class DistTuple {
    private Interpretation interp;
    private Map<String, String> tupleValues;

    public DistTuple(Interpretation interp) {
        this.interp = interp;
        this.tupleValues = new HashMap<>();
    }

    public Interpretation getInterp() {
        return interp;
    }

    public void setTupleValue(String keyword, String value) {
        this.tupleValues.put(keyword, value);
    }

    public String getTupleValue(String keyword) {
        return this.tupleValues.get(keyword);
    }
}
