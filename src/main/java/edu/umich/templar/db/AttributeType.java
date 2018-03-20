package edu.umich.templar.db;

import java.io.Serializable;

/**
 * Created by cjbaik on 1/31/18.
 */
public enum AttributeType implements Serializable {
    TEXT, INT, DOUBLE, TIME, TIMESTAMP, ALL_COLUMNS;
}
