package edu.umich.tbnalir.parse;

import edu.umich.tbnalir.rdbms.Attribute;

/**
 * Created by cjbaik on 9/25/17.
 */
public class QueryFragment {
    protected Attribute attribute;

    public Attribute getAttribute() {
        return attribute;
    }

    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }
}
