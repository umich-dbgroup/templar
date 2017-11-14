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

    public static final int MAX_MAPPED_EL = 7;

    // Min similarity score to consider "matching"
    public static final Double MIN_SIM = 0.65;
    public static final Double MIN_REL_SIM = 0.8;

    public static final Double PENALTY_UNLIKELY_PROJECTION = 0.5;
    public static final Double PENALTY_RELATION_WITH_SUPERLATIVE = 0.3;
    public static final Double PENALTY_RELATION_WITH_ADJECTIVE = 0.5;
    public static final Double PENALTY_PREDICATE_COMMON_NOUN = 0.5;
    public static final Double PENALTY_PREDICATE_WITH_SUPERLATIVE = 0.3;

    // Threshold to augment NL similarity scores with co-occurrence scores
    public static final Double SIM_AUG_THRESHOLD = 0.8;

    public static final int MAX_CACHE_SIZE = 20000;
}
