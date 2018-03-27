package edu.umich.templar.main.settings;

public class Params {
    public static int MAX_TOP_CANDIDATES = 5;
    public static int MAX_CHAR_LENGTH = 50;
    public static int MIN_FULLTEXT_TOKEN_LENGTH = 3;

    /* Parameters for SQLizer */
    public static double SQLIZER_EPSILON = 0.0001;

    /* Minimum similarity for pruning */
    public static double MIN_SIM = 0.55;

    /* Harmonic mean minimum epsilon */
    public static double EPSILON = 0.01;

    /* Confidence for FW analysis (BW analysis is 1 - FW) */
    public static double CONF_FW = 0.8;
}
