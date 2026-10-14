package org.qamethod.datatesting.core.db.connector.auth;

import java.util.Properties;

/**
 * Strategy interface for database authentication.
 * <p>
 * Each implementation knows how to obtain credentials and configure
 * a JDBC connection for a specific auth mechanism.
 */
public interface AuthStrategy {

    /**
     * Apply authentication to the JDBC connection properties.
     * Called before the connection is opened.
     *
     * @param jdbcUrl    the base JDBC URL (may be modified by the strategy)
     * @param properties mutable JDBC properties to add credentials to
     * @return the (possibly modified) JDBC URL
     */
    String configureConnection(String jdbcUrl, Properties properties);

    /**
     * A human-readable description of this auth method.
     */
    String description();
}

