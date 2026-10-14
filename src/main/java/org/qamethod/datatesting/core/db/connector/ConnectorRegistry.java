package org.qamethod.datatesting.core.db.connector;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Registry for managing multiple named database connections.
 * <p>
 * Domain modules can register connections by name and retrieve them later.
 * Typically populated from a YAML config section:
 *
 * <pre>
 * connections:
 *   analyticsDb:
 *     provider: azure-sql
 *     host: sqlserver.database.windows.net
 *     ...
 *   warehouseDb:
 *     provider: gcp-cloudsql
 *     host: /cloudsql/project:region:instance
 *     ...
 * </pre>
 *
 * Usage:
 * <pre>
 * ConnectorRegistry registry = ConnectorRegistry.fromConfigs(yamlMap);
 * try (Connection conn = registry.get("analyticsDb").getConnection()) {
 *     // query reference tables
 * }
 * </pre>
 */
public class ConnectorRegistry implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(ConnectorRegistry.class.getName());

    private final Map<String, DatabaseConnector> connectors = new LinkedHashMap<>();

    /**
     * Create a registry from a map of named connection configs.
     */
    public static ConnectorRegistry fromConfigs(Map<String, ConnectionConfig> configs) {
        ConnectorRegistry registry = new ConnectorRegistry();
        if (configs != null) {
            configs.forEach((name, config) -> {
                DatabaseConnector connector = ConnectorFactory.create(config);
                registry.register(name, connector);
                logger.info("Registered database connector: " + name + " -> " + connector.label());
            });
        }
        return registry;
    }

    /**
     * Register a connector by name.
     */
    public void register(String name, DatabaseConnector connector) {
        connectors.put(name, connector);
    }

    /**
     * Get a connector by name.
     *
     * @throws IllegalArgumentException if the name is not registered
     */
    public DatabaseConnector get(String name) {
        DatabaseConnector connector = connectors.get(name);
        if (connector == null) {
            throw new IllegalArgumentException("No database connector registered with name: " + name
                    + ". Available: " + connectors.keySet());
        }
        return connector;
    }

    /**
     * Check if a connector is registered.
     */
    public boolean has(String name) {
        return connectors.containsKey(name);
    }

    /**
     * All registered connection names.
     */
    public Set<String> names() {
        return Collections.unmodifiableSet(connectors.keySet());
    }

    /**
     * Test all connections and return a summary.
     */
    public Map<String, Boolean> testAll() {
        Map<String, Boolean> results = new LinkedHashMap<>();
        connectors.forEach((name, connector) -> {
            boolean ok = connector.testConnection();
            results.put(name, ok);
            logger.info("Connection test [" + name + "] " + connector.label() + ": " + (ok ? "OK" : "FAILED"));
        });
        return results;
    }

    /**
     * Close all connectors.
     */
    @Override
    public void close() {
        connectors.values().forEach(DatabaseConnector::close);
        connectors.clear();
    }
}

