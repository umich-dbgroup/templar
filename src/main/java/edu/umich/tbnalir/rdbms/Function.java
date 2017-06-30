package edu.umich.tbnalir.rdbms;

import java.util.Map;

/**
 * Created by cjbaik on 6/30/17.
 */
public class Function extends Relation {
    Map<String, FunctionParameter> inputs;

    public Function(String name, String type, Map<String, Attribute> attributes,
                    Map<String, FunctionParameter> inputs) {
        super(name, type, attributes);

        this.inputs = inputs;
    }

    public Map<String, FunctionParameter> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, FunctionParameter> inputs) {
        this.inputs = inputs;
    }

}
