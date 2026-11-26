package com.rentproof.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DBConnection provides a centralized, reusable database connection manager
 * for the entire RentProof application.
 * 
 * Shared across all four team modules:
 * - Aditi Sharma  : Landlords, Properties, Rooms
 * - Shweta        : Tenants
 * - Aanvi         : Payments
 * - Gungun        : Reminders
 * 
 * Concepts used (Syllabus up to Lecture 42):
 * - Static initialization block
 * - Static factory method
 * - Exception handling (try-catch, SQLException)
 * - Properties configuration file loading
 * - DriverManager & Connection interfaces
 */
public class DBConnection {

    // Default configuration values
    private static String driver = "com.mysql.cj.jdbc.Driver";
    private static String url = "jdbc:mysql://localhost:3306/rentproof?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static String serverUrl;
    private static String username;
    private static String password;

    // Static block executed once when the class is loaded into memory
    static {
        loadConfiguration();
        try {
            // Load the MySQL JDBC Driver class
            Class.forName(driver);
            System.out.println("[DBConnection] MySQL JDBC Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection] ERROR: MySQL JDBC Driver not found!");
            System.err.println("[DBConnection] Please ensure 'mysql-connector-j.jar' is added to your project classpath.");
            e.printStackTrace();
        }
    }

    // Private constructor to prevent direct instantiation (Singleton pattern style utility class)
    private DBConnection() {
    }

    /**
     * Loads database credentials from db.properties or environment variables.
     * Prevents hardcoding passwords in source code.
     */
    private static void loadConfiguration() {
        Properties props = new Properties();
        InputStream is = null;

        try {
            // 1. First attempt: Load from classpath (e.g. resources folder)
            is = DBConnection.class.getClassLoader().getResourceAsStream("db.properties");

            // 2. Second attempt: Check local root directory
            if (is == null) {
                File localFile = new File("db.properties");
                if (localFile.exists()) {
                    is = new FileInputStream(localFile);
                }
            }

            // 3. Third attempt: Check backend/src/main/resources/
            if (is == null) {
                File resFile = new File("backend/src/main/resources/db.properties");
                if (resFile.exists()) {
                    is = new FileInputStream(resFile);
                }
            }

            if (is != null) {
                try (InputStream input = is) {
                    props.load(input);
                }
                if (props.getProperty("db.driver") != null) {
                    driver = props.getProperty("db.driver").trim();
                }
                if (props.getProperty("db.url") != null) {
                    url = props.getProperty("db.url").trim();
                }
                if (props.getProperty("db.user") != null) {
                    username = props.getProperty("db.user").trim();
                }
                if (props.getProperty("db.password") != null) {
                    password = props.getProperty("db.password").trim();
                }
                if (props.getProperty("db.serverUrl") != null) {
                    serverUrl = props.getProperty("db.serverUrl").trim();
                }
                System.out.println("[DBConnection] Loaded database settings from db.properties.");
            } else {
                System.err.println("[DBConnection] db.properties not found. Configure database access before connecting.");
            }
        } catch (java.io.IOException e) {
            System.err.println("[DBConnection] Could not read db.properties: " + e.getMessage());
        }

        // Allow system environment variable overrides (common in enterprise setups)
        String envUrl = System.getenv("RENTPROOF_DB_URL");
        if (envUrl != null && !envUrl.isEmpty()) {
            url = envUrl;
        }

        String envServerUrl = System.getenv("RENTPROOF_DB_SERVER_URL");
        if (envServerUrl != null && !envServerUrl.isEmpty()) {
            serverUrl = envServerUrl;
        }

        String envUser = System.getenv("RENTPROOF_DB_USER");
        if (envUser != null && !envUser.isEmpty()) {
            username = envUser;
        }

        String envPass = System.getenv("RENTPROOF_DB_PASSWORD");
        if (envPass != null) {
            password = envPass;
        }
    }

    /**
     * Establishes and returns a live Connection to the rentproof database.
     * Callers should manage the connection using try-with-resources.
     * 
     * @return active java.sql.Connection object
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        validateCredentials();
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            System.err.println("[DBConnection] Error code: " + e.getErrorCode() + " | SQLState: " + e.getSQLState());
            throw e;
        }
    }

    /**
     * Opens a connection to the MySQL server without selecting a database.
     * The optional db.serverUrl property can be used when the server URL cannot
     * be derived from db.url.
     *
     * @return active connection to the MySQL server
     * @throws SQLException if the server connection fails or credentials are missing
     */
    public static Connection getServerConnection() throws SQLException {
        validateCredentials();
        String connectionUrl = serverUrl;
        if (connectionUrl == null || connectionUrl.isEmpty()) {
            connectionUrl = getServerUrl(url);
        }

        try {
            return DriverManager.getConnection(connectionUrl, username, password);
        } catch (SQLException e) {
            System.err.println("[DBConnection] Server connection failed. Error code: "
                    + e.getErrorCode() + " | SQLState: " + e.getSQLState());
            throw e;
        }
    }

    private static void validateCredentials() throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            throw new SQLException("Database user is missing. Set db.user in db.properties.");
        }
        if (password == null) {
            throw new SQLException("Database password is missing. Set db.password in db.properties.");
        }
    }

    private static String getServerUrl(String databaseUrl) throws SQLException {
        int protocolEnd = databaseUrl.indexOf("://");
        if (protocolEnd < 0) {
            throw new SQLException("Invalid MySQL URL. Set db.serverUrl in db.properties.");
        }

        int authorityStart = protocolEnd + 3;
        int pathStart = databaseUrl.indexOf('/', authorityStart);
        int queryStart = databaseUrl.indexOf('?', authorityStart);
        if (pathStart < 0 || (queryStart >= 0 && queryStart < pathStart)) {
            return databaseUrl;
        }

        String query = queryStart >= 0 ? databaseUrl.substring(queryStart) : "";
        return databaseUrl.substring(0, pathStart) + query;
    }

    /**
     * Helper method to verify database connectivity.
     * Useful for startup health checks and test scripts.
     * 
     * @return true if connection is successful, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // Getters for connection metadata (read-only)
    public static String getUrl() {
        return url;
    }

    public static String getUsername() {
        return username;
    }
}
