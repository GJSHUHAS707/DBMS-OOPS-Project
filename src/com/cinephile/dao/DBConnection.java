package com.cinephile.dao;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Plain JDBC Database Connection helper.
 * Demonstrates CS23333 Unit V: Accessing Databases with JDBC (DriverManager).
 */
public class DBConnection {

    private static final String PROPS_FILE = "db.properties";

    private static String url = "jdbc:mysql://localhost:3306/cinephile_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static String user = "root";
    private static String password = "root";

    static {
        loadProperties();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found on classpath: " + e.getMessage());
        }
    }

    public static void loadProperties() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(PROPS_FILE)) {
            props.load(fis);
            url = props.getProperty("db.url", url).trim();
            user = props.getProperty("db.user", user).trim();
            password = props.getProperty("db.password", password);
        } catch (IOException ignored) {
            // Fall back to defaults if db.properties is missing
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public static void updateCredentials(String newUrl, String newUser, String newPassword) throws IOException {
        url = newUrl.trim();
        user = newUser.trim();
        password = newPassword;
        Properties props = new Properties();
        props.setProperty("db.url", url);
        props.setProperty("db.user", user);
        props.setProperty("db.password", password);
        try (FileOutputStream fos = new FileOutputStream(PROPS_FILE)) {
            props.store(fos, "MySQL 8 Database Configuration for Cinephile");
        }
    }

    public static String getUrl() { return url; }
    public static String getUser() { return user; }
    public static String getPassword() { return password; }
}
