package org.qamethod.datatesting.core.db.connector.gcp;

import org.qamethod.datatesting.core.db.connector.ConnectionConfig;
import org.qamethod.datatesting.core.db.connector.DatabaseConnector;
import org.qamethod.datatesting.core.db.connector.auth.AuthStrategy;
import org.qamethod.datatesting.core.db.connector.auth.AuthStrategyFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Connector for Google Cloud SQL (PostgreSQL, MySQL, SQL Server).
 * <p>
 * Supports two connection modes:
 * <ul>
 *   <li><b>Cloud SQL Auth Proxy</b>: host is the instance connection name
 *       (e.g. {@code project:region:instance}), driver connects via Unix socket or proxy</li>
 *   <li><b>Public/Private IP</b>: host is a regular hostname or IP</li>
 * </ul>
 * <p>
 * Default assumes PostgreSQL. Set {@code options.dbType=mysql} or {@code options.dbType=sqlserver}
 * to change the JDBC driver prefix.
 * <p>
 * Requires the appropriate JDBC driver on the classpath:
 * {@code org.postgresql:postgresql}, {@code com.mysql:mysql-connector-j}, or
 * {@code com.microsoft.sqlserver:mssql-jdbc}.
 */
public class GcpCloudSqlConnector implements DatabaseConnector {

    private static final Logger logger = Logger.getLogger(GcpCloudSqlConnector.class.getName());

    private final ConnectionConfig config;
    private final AuthStrategy authStrategy;
    private final String baseJdbcUrl;

    public GcpCloudSqlConnector(ConnectionConfig config) {
        this.config = config;
        this.authStrategy = AuthStrategyFactory.create(config);

        String dbType = config.getOptions().getOrDefault("dbType", "postgresql");
        int port = config.effectivePort();
        if (port == 0) {
            port = switch (dbType.toLowerCase()) {
                case "mysql" -> 3306;
                case "sqlserver" -> 1433;
                default -> 5432; // postgresql
            };
        }

        String driverPrefix = switch (dbType.toLowerCase()) {
            case "mysql" -> "jdbc:mysql://";
            case "sqlserver" -> "jdbc:sqlserver://";
            default -> "jdbc:postgresql://";
        };

        StringBuilder url = new StringBuilder(driverPrefix)
                .append(config.getHost())
                .append(":").append(port)
                .append("/").append(config.getDatabase());

        // Append options as query params for PostgreSQL/MySQL
        if (!config.getOptions().isEmpty() && !"sqlserver".equalsIgnoreCase(dbType)) {
            url.append("?");
            config.getOptions().entrySet().stream()
                    .filter(e -> !"dbType".equalsIgnoreCase(e.getKey()))
                    .forEach(e -> url.append(e.getKey()).append("=").append(e.getValue()).append("&"));
            url.setLength(url.length() - 1); // remove trailing &
        }

        this.baseJdbcUrl = url.toString();
    }

    @Override
    public Connection getConnection() throws SQLException {
        Properties props = new Properties();
        String finalUrl = authStrategy.configureConnection(baseJdbcUrl, props);
        logger.fine("Connecting to GCP Cloud SQL: " + finalUrl);
        return DriverManager.getConnection(finalUrl, props);
    }

    @Override
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            logger.warning("GCP Cloud SQL connection test failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String label() { return config.effectiveLabel(); }

    @Override
    public String jdbcUrl() { return baseJdbcUrl; }

    @Override
    public void close() {}
}

