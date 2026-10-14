package org.qamethod.datatesting.core.db.connector.aws;

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
 * Connector for AWS RDS (MySQL, PostgreSQL, SQL Server, Aurora).
 * <p>
 * Supports:
 * <ul>
 *   <li>Username/password authentication</li>
 *   <li>AWS IAM authentication (short-lived token generated via AWS SDK)</li>
 * </ul>
 * <p>
 * Default assumes MySQL. Set {@code options.dbType=postgresql} or {@code options.dbType=sqlserver}
 * to change the JDBC driver prefix.
 * <p>
 * For IAM auth, SSL is required. The connector automatically appends {@code useSSL=true}
 * for MySQL or {@code sslmode=require} for PostgreSQL.
 */
public class AwsRdsConnector implements DatabaseConnector {

    private static final Logger logger = Logger.getLogger(AwsRdsConnector.class.getName());

    private final ConnectionConfig config;
    private final AuthStrategy authStrategy;
    private final String baseJdbcUrl;

    public AwsRdsConnector(ConnectionConfig config) {
        this.config = config;
        this.authStrategy = AuthStrategyFactory.create(config);

        String dbType = config.getOptions().getOrDefault("dbType", "mysql");
        int port = config.effectivePort();
        if (port == 0) {
            port = switch (dbType.toLowerCase()) {
                case "postgresql" -> 5432;
                case "sqlserver" -> 1433;
                default -> 3306; // mysql/aurora-mysql
            };
        }

        String driverPrefix = switch (dbType.toLowerCase()) {
            case "postgresql" -> "jdbc:postgresql://";
            case "sqlserver" -> "jdbc:sqlserver://";
            default -> "jdbc:mysql://";
        };

        StringBuilder url = new StringBuilder(driverPrefix)
                .append(config.getHost())
                .append(":").append(port)
                .append("/").append(config.getDatabase());

        // For IAM auth, enforce SSL
        boolean isIam = "iam".equalsIgnoreCase(config.getAuth().getType());
        if (isIam && "mysql".equalsIgnoreCase(dbType)) {
            url.append("?useSSL=true&requireSSL=true");
        } else if (isIam && "postgresql".equalsIgnoreCase(dbType)) {
            url.append("?sslmode=require");
        }

        this.baseJdbcUrl = url.toString();
    }

    @Override
    public Connection getConnection() throws SQLException {
        Properties props = new Properties();
        String finalUrl = authStrategy.configureConnection(baseJdbcUrl, props);
        logger.fine("Connecting to AWS RDS: " + finalUrl);
        return DriverManager.getConnection(finalUrl, props);
    }

    @Override
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            logger.warning("AWS RDS connection test failed: " + e.getMessage());
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

