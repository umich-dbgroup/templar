package edu.umich.templar.baseline;

import java.util.ArrayList;
import java.util.List;

public class Mappings {
    Integer queryId;
    List<Mapping> mappings;

    public Mappings(Integer queryId) {
        this.queryId = queryId;
        this.mappings = new ArrayList<>();
    }

    public void addMapping(Mapping mapping) {
        mappings.add(mapping);
    }
}
