package edu.umich.tbnalir.template;

import java.util.ArrayList;

/**
 * Created by cjbaik on 8/22/17.
 */
public class Template {
    private String string;
    private TemplateType type;

    private ArrayList<String> tables;
    private ArrayList<String> predicates;
    private ArrayList<String> columns;

    public Template(String string, TemplateType type) {
        this.string = string;
        this.type = type;

        this.tables = new ArrayList<>();
        this.predicates= new ArrayList<>();
        this.columns = new ArrayList<>();
    }
}
