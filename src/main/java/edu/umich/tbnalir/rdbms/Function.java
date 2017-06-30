package edu.umich.tbnalir.rdbms;

import edu.umich.tbnalir.util.Constants;

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
        StringBuffer sb = new StringBuffer();
        sb.append(this.getName());
        sb.append('(');

        StringJoiner sj = new StringJoiner(",");
        for (int i = 1; i < this.inputs.size() + 1; i++) {
            if (this.inputs.get(i) == null) continue;
            String type = this.inputs.get(i).getType();
            switch (type) {
                case "varchar":
                    sj.add(Constants.STR);
                    break;
                case "real":
                    sj.add(Constants.NUM);
                    break;
                case "float":
                    sj.add(Constants.NUM);
                    break;
                case "int":
                    sj.add(Constants.NUM);
                    break;
                case "bigint":
                    sj.add(Constants.NUM);
                    break;
                default:
                    throw new IllegalArgumentException("Did not recognize function parameter type: <" + type + ">");
            }
        }
        sb.append(sj.toString());
        sb.append(')');

        return sb.toString();
    }

}
