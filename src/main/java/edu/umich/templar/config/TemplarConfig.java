package edu.umich.templar.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TemplarConfig {
    private static Properties prop = new Properties();

    static {
        InputStream input = null;
        try {
            input = new FileInputStream("config.properties");

            // load a properties file
            prop.load(input);

            input.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int getIntegerProperty(String key) {
        return Integer.valueOf(prop.getProperty(key));
    }

    public static String getProperty(String key) {
        String result = prop.getProperty(key);
        if (result.equalsIgnoreCase("null")) {
            return null;
        } else {
            return result;
        }
    }
}
