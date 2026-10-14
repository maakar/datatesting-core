package org.qamethod.datatesting.core.db.connector;

import org.qamethod.datatesting.core.db.connector.auth.AuthStrategy;
import org.qamethod.datatesting.core.db.connector.auth.AuthStrategyFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Generic JDBC connector for databases not covered by cloud-specific connectors.
 * <p>
 * The full JDBC URL must be provided in the {@code host} field of the config,
 * or constructed from host + port + database using a generic pattern.
 * <p>
 * Use {@code options.jdbcUrl} to provide a fully custom JDBC URL.
 */
public class GenericJdbcConnector implements DatabaseConnector {

    private static final Logger logger = Logger.getLogger(GenericJdbcConnector.class.getName());

    private final ConnectionConfig config;
    private final AuthStrategy authStrategy;
    private final String baseJdbcUrl;

    public GenericJdbcConnector(ConnectionConfig config) {
        this.config = config;
        this.authStrategy = AuthStrategyFactory.create(config);

        // Allow fully custom URL via options
        String customUrl = config.getOptions().get("jdbcUrl");
        if (customUrl != null && !customUrl.isBlank()) {
            this.baseJdbcUrl = customUrl;
        } else {
            // Build a generic URL
            int port = config.effectivePort();
            StringBuilder url = new StringBuilder("jdbc:")
                    .append(config.getHost());
            if (port > 0) url.append(":").append(port);
            if (config.getDatabase() != null) url.append("/").append(config.getDatabase());
            this.baseJdbcUrl = url.toString();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        Properties props = new Properties();
        String finalUrl = authStrategy.configureConnection(baseJdbcUrl, props);
        logger.fine("Connecting (generic): " + finalUrl);
        return DriverManager.getConnection(finalUrl, props);
    }

    @Override
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            logger.warning("Generic connection test failed: " + e.getMessage());
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

