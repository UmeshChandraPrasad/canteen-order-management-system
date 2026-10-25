package com.canteen.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Unit V — JDBC: central connection factory built on DriverManager.
 * Every DAO gets connections from here; callers use try-with-resources
 * so connections always return to the pool / close (Unit II).
 */
public final class Db {

    static {
        try {
            // Explicit driver registration (also works via ServiceLoader).
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                    "MySQL Connector/J not found on the classpath. "
                    + "Add lib/mysql-connector-j.jar. Cause: " + e.getMessage());
        }
    }

    private Db() {
    }

    /** Open a new JDBC connection using config.properties credentials. */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                Config.dbUrl(), Config.dbUser(), Config.dbPassword());
    }

    /** Quiet rollback helper for catch blocks (never masks the real error). */
    public static void rollbackQuietly(Connection c) {
        if (c != null) {
            try {
                c.rollback();
            } catch (SQLException ignored) {
                // best effort only
            }
        }
    }

    /** Quiet close helper for hand-rolled finally blocks. */
    public static void closeQuietly(AutoCloseable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception ignored) {
                // best effort only
            }
        }
    }
}
