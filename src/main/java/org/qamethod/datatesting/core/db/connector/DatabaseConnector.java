package org.qamethod.datatesting.core.db.connector;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Universal interface for obtaining JDBC connections to any database.
 * <p>
 * Implementations handle cloud-specific connection strings, authentication,
 * driver loading, and connection pooling. Domain modules use this to query
 * reference data instead of loading CSV snapshots.
 *
 * <pre>
 * try (Connection conn = connector.getConnection()) {
 *     // query reference tables
 * }
 * </pre>
 */
public interface DatabaseConnector extends AutoCloseable {

    /**
     * Obtain a JDBC connection. Callers must close the connection when done.
     */
    Connection getConnection() throws SQLException;

    /**
     * Test whether the connector can reach the database.
     *
     * @return true if a connection can be established and a simple query succeeds
     */
    boolean testConnection();

    /**
     * A human-readable label for this connection (e.g. "Azure SQL - AnalyticsDB").
     */
    String label();

    /**
     * The underlying JDBC URL (for diagnostics/logging — may be masked).
     */
    String jdbcUrl();

    /**
     * Release any pooled connections and clean up resources.
     */
    @Override
    void close();
}

