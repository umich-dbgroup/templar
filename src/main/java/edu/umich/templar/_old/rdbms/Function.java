package edu.umich.templar._old.rdbms;

import edu.umich.templar.util.Utils;

import java.util.Map;
import java.util.StringJoiner;

/**
 * Created by cjbaik on 6/30/17.
 */
public class Function extends Relation {
    Map<Integer, FunctionParameter> inputs;

    public Function(String name, String type, Map<String, Attribute> attributes,
                    Map<Integer, FunctionParameter> inputs) {
        super(name, type, attributes);

        this.inputs = inputs;
    }

    public Map<Integer, FunctionParameter> getInputs() {
        return inputs;
    }

    public void setInputs(Map<Integer, FunctionParameter> inputs) {
        this.inputs = inputs;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName());
        sb.append('(');

        StringJoiner sj = new StringJoiner(",");
        for (int i = 1; i < this.inputs.size() + 1; i++) {
            if (this.inputs.get(i) == null) continue;
            String type = this.inputs.get(i).getType();
            sj.add(Utils.convertSQLTypetoConstant(type));
        }
        sb.append(sj.toString());
        sb.append(')');

        return sb.toString();
    }

}
