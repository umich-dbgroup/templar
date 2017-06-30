package edu.umich.tbnalir.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by cjbaik on 6/20/17.
 */
public class Utils {
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValueDesc(Map<K, V> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public static String convertSQLTypetoConstant(String type) {
        switch (type) {
            case "varchar":
            case "varbinary":
            case "binary":
            case "sysname":
                return Constants.STR;
            case "real":
            case "float":
            case "int":
            case "bigint":
            case "smallint":
            case "tinyint":
            case "bit":
                return Constants.NUM;
            case "datetime":
                return Constants.DATETIME;
            case "time":
                return Constants.TIME;
            case "timestamp":
                return Constants.TIMESTAMP;
            case "date":
                return Constants.DATE;
            default:
                throw new IllegalArgumentException("Did not recognize function parameter type: <" + type + ">");
        }

    }
}
