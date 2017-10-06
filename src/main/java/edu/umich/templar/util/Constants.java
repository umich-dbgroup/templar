package edu.umich.templar.util;

/**
 * Created by cjbaik on 6/30/17.
 */
public class Constants {
    public static final String TOP = "#top";
    public static final String COLUMN = "#col";
    public static final String PROJ = "#projection";
    public static final String PRED = "#predicate";
    public static final String CMP = "#cmp";
    public static final String NUM = "#num";
    public static final String STR = "#str";
    public static final String DATE = "#date";
    public static final String DATETIME = "#datetime";
    public static final String TIMESTAMP = "#timestamp";
    public static final String TIME = "#time";
    public static final String ORDER = "#order";

    // Constants for affinity score
    public static final Double EXACT_MATCH = 1.0;
    public static final Double SLOT_COVERS = 0.5;
    public static final Double NO_MATCH = 0.0;

    // Min similarity score to consider "matching"
    public static final Double MIN_SIM = 0.6;

    public static final Double PENALTY_RELATION_WITH_SUPERLATIVE = 0.75;
    public static final Double PENALTY_PREDICATE_COMMON_NOUN = 0.75;
}
