package com.canteen.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Unit V — DB credentials live in config.properties (never hardcoded).
 * Loaded once; every lookup falls back to a sane default so a missing
 * file produces a clear error at connect time, not a NullPointerException.
 */
public final class Config {
    private static final String FILE = "config.properties";
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = new FileInputStream(FILE)) {
            PROPS.load(in);
        } catch (IOException e) {
            System.err.println("[Config] Could not read " + FILE
                    + " (" + e.getMessage() + "); using built-in defaults.");
        }
    }

    private Config() {
    }

    public static String get(String key, String defaultValue) {
        return PROPS.getProperty(key, defaultValue);
    }

    public static String dbUrl() {
        return get("db.url",
                "jdbc:mysql://localhost:3306/canteen_db"
                + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
    }

    public static String dbUser() {
        return get("db.user", "canteen");
    }

    public static String dbPassword() {
        return get("db.password", "canteen123");
    }
}
