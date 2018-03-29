package edu.umich.templar.main.settings;

public class Params {
    public static int MAX_TOP_CANDIDATES = 5;
    public static int MAX_CHAR_LENGTH = 50;
    public static int MIN_FULLTEXT_TOKEN_LENGTH = 3;

    /* Parameters for SQLizer */
    public static double SQLIZER_EPSILON = 0.0001;

    /** For our epsilons: word2vec_oov > lemmatizer > special_chars **/

    /* Lemmatizer epsilon */
    public static double LEMMATIZER_EPSILON = 0.0000001;

    /* Special chars epsilon */
    public static double SPECIAL_CHARS_EPSILON = 0.00000001;

    /* Minimum similarity for pruning */
    public static double MIN_SIM = 0.55;

    /* "Exact score" threshold for pruning */
    public static double EXACT_SCORE = 1 - LEMMATIZER_EPSILON;

    /* Harmonic mean minimum epsilon */
    public static double EPSILON = 0.01;

    /* Confidence for FW analysis (BW analysis is 1 - FW) */
    public static double CONF_FW = 0.8;

    /* Cache save interval (how many tasks we should wait) */
    public static int CACHE_SAVE_INTERVAL = 5;
}
