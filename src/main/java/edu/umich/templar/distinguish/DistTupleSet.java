package edu.umich.templar.distinguish;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class DistTupleSet {
    private List<String> keywords;
    private List<DistTuple> tuples;

    public DistTupleSet(List<String> keywords) {
        this.keywords = keywords;
        this.tuples = new ArrayList<>();
    }

    public void add(DistTuple tuple) {
        this.tuples.add(tuple);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String header = String.join("\t", this.keywords);
        sb.append("---\n");
        sb.append(header);
        sb.append("\tInterp");
        sb.append('\n');
        sb.append("---\n");

        for (DistTuple tuple : this.tuples) {
            StringJoiner sj = new StringJoiner("\t");
            for (String kw : keywords) {
                sj.add(tuple.getTupleValue(kw));
            }
            sj.add(tuple.getInterp().toString());
            sb.append(sj.toString());
            sb.append('\n');
        }
        return sb.toString();
    }
}
