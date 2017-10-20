package edu.umich.templar.rdbms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by cjbaik on 10/20/17.
 */
public class MappingCache {
    static Map<String, List<String>> textCache = new HashMap<>();
    static Map<String, List<String>> numCache = new HashMap<>();

    private static String hashText(String relName, String attrName, String value) {
        return (relName + attrName + value).intern();
    }

    public static List<String> getText(String relName, String attrName, String value) {
        String key = hashText(relName, attrName, value);
        return textCache.get(key);
    }

    public static List<String> putText(String relName, String attrName, String value, List<String> text) {
        String key = hashText(relName, attrName, value);
        return textCache.put(key, text);
    }

    private static String hashNum(String relName, String attrName, String op, String value) {
        return (relName + attrName + op + value).intern();
    }

    public static List<String> getNum(String relName, String attrName, String op, String value) {
        String key = hashNum(relName, attrName, op, value);
        return numCache.get(key);
    }

    public static List<String> putNum(String relName, String attrName, String op, String value, List<String> num) {
        String key = hashNum(relName, attrName, op, value);
        return numCache.put(key, num);
    }
}
