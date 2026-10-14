package org.qamethod.datatesting.core.db.connector.azure;

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
 * Connector for Azure SQL Database (and Azure SQL Managed Instance).
 * <p>
 * Builds a JDBC URL of the form:
 * {@code jdbc:sqlserver://host:port;databaseName=DB;encrypt=true;...}
 * <p>
 * Supports all Azure auth types: password, Windows integrated,
 * Azure AD Service Principal, Managed Identity, and Key Vault retrieval.
 * <p>
 * Requires the Microsoft JDBC Driver for SQL Server ({@code com.microsoft.sqlserver:mssql-jdbc})
 * on the classpath at runtime.
 */
public class AzureSqlConnector implements DatabaseConnector {

    private static final Logger logger = Logger.getLogger(AzureSqlConnector.class.getName());

    private final ConnectionConfig config;
    private final AuthStrategy authStrategy;
    private final String baseJdbcUrl;

    public AzureSqlConnector(ConnectionConfig config) {
        this.config = config;
        this.authStrategy = AuthStrategyFactory.create(config);

        // Build base JDBC URL
        int port = config.effectivePort();
        StringBuilder url = new StringBuilder("jdbc:sqlserver://")
                .append(config.getHost());
        if (port > 0) url.append(":").append(port);
        url.append(";databaseName=").append(config.getDatabase());

        // Apply additional options (encrypt, trustServerCertificate, etc.)
        config.getOptions().forEach((key, value) ->
                url.append(";").append(key).append("=").append(value));

        // Azure SQL defaults
        if (!config.getOptions().containsKey("encrypt")) {
            url.append(";encrypt=true");
        }
        if (!config.getOptions().containsKey("trustServerCertificate")) {
            url.append(";trustServerCertificate=false");
        }

        this.baseJdbcUrl = url.toString();
    }

    @Override
    public Connection getConnection() throws SQLException {
        Properties props = new Properties();
        String finalUrl = authStrategy.configureConnection(baseJdbcUrl, props);

        logger.fine("Connecting to Azure SQL: " + maskedUrl(finalUrl));
        return DriverManager.getConnection(finalUrl, props);
    }

    @Override
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            logger.warning("Azure SQL connection test failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String label() {
        return config.effectiveLabel();
    }

    @Override
    public String jdbcUrl() {
        return maskedUrl(baseJdbcUrl);
    }

    @Override
    public void close() {
        // No pool to close in basic implementation.
        // Future: integrate HikariCP connection pooling here.
    }

    private String maskedUrl(String url) {
        // Mask password if embedded in URL
        return url.replaceAll("(password=)[^;]+", "$1***");
    }
}

