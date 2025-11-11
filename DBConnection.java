import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database connection helper.
 *
 * Behavior:
 * - Reads DB URL, user, and password from environment variables (DB_URL, DB_USER, DB_PASSWORD)
 *   and falls back to sensible defaults defined below.
 * - Appends recommended JDBC parameters when not present (useSSL=false, serverTimezone=UTC,
 *   allowPublicKeyRetrieval=true).
 * - Attempts to load the MySQL driver class for older JVMs, but continues if the driver is
 *   auto-registered (JDBC 4+).
 */
public class DBConnection {
    // Default values (change to match your local setup). Consider setting DB_URL/DB_USER/DB_PASSWORD
    // as environment variables instead of committing secrets here.
    // Default database name is `petshop` (see PetShop/setup-mysql.sql). Use env var DB_URL to override.
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/petshop";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "#T@nureet124";

    private static String getenvOrDefault(String key, String def) {
        String v = System.getenv(key);
        return (v != null && !v.isEmpty()) ? v : def;
    }

    public static Connection getConnection() throws SQLException {
        String url = getenvOrDefault("DB_URL", DEFAULT_URL);
        String user = getenvOrDefault("DB_USER", DEFAULT_USER);
        String password = getenvOrDefault("DB_PASSWORD", DEFAULT_PASSWORD);

        // If no query string present, append recommended params. If the URL already contains
        // parameters, add missing ones only.
        if (!url.contains("?")) {
            url = url + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        } else {
            if (!url.contains("useSSL=")) url = url + "&useSSL=false";
            if (!url.contains("serverTimezone=")) url = url + "&serverTimezone=UTC";
            if (!url.contains("allowPublicKeyRetrieval=")) url = url + "&allowPublicKeyRetrieval=true";
        }

        try {
            // Optional: explicit driver load for older JVMs/drivers.
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // Driver not found on classpath — log and continue; DriverManager may still find it if
            // the driver jar is present and properly registered. We'll rethrow any SQLException below.
            System.err.println("Warning: MySQL JDBC Driver not found on classpath: " + e.getMessage());
        }

        // Let SQLException propagate to caller with useful message
        return DriverManager.getConnection(url, user, password);
    }
}