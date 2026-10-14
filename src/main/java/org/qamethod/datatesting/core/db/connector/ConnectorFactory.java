package org.qamethod.datatesting.core.db.connector;

import org.qamethod.datatesting.core.db.connector.aws.AwsRdsConnector;
import org.qamethod.datatesting.core.db.connector.azure.AzureSqlConnector;
import org.qamethod.datatesting.core.db.connector.gcp.GcpCloudSqlConnector;

/**
 * Factory that creates the appropriate {@link DatabaseConnector} based on the
 * {@code provider} field in {@link ConnectionConfig}.
 */
public final class ConnectorFactory {

    private ConnectorFactory() {}

    /**
     * Create a connector from configuration.
     *
     * @param config connection configuration with provider, host, auth, etc.
     * @return a ready-to-use connector (call {@link DatabaseConnector#getConnection()})
     * @throws IllegalArgumentException if the provider is not recognized
     */
    public static DatabaseConnector create(ConnectionConfig config) {
        if (config.getProvider() == null) {
            throw new IllegalArgumentException("Connection provider must be specified");
        }

        return switch (config.getProvider().toLowerCase()) {
            case "azure-sql", "azure", "mssql" -> new AzureSqlConnector(config);
            case "gcp-cloudsql", "gcp", "cloudsql" -> new GcpCloudSqlConnector(config);
            case "aws-rds", "aws", "rds" -> new AwsRdsConnector(config);
            case "generic" -> new GenericJdbcConnector(config);
            default -> throw new IllegalArgumentException(
                    "Unknown database provider: " + config.getProvider()
                    + ". Supported: azure-sql, gcp-cloudsql, aws-rds, generic");
        };
    }
}

