package org.qamethod.datatesting.core.db.connector.auth;

import java.util.Properties;

/**
 * Azure AD Service Principal (SPN) authentication.
 * Uses client credentials flow (tenantId + clientId + clientSecret) to obtain
 * an access token for Azure SQL Database.
 * <p>
 * Requires the Microsoft JDBC Driver for SQL Server with Azure AD support.
 * The driver handles token acquisition internally when these properties are set.
 */
public class ServicePrincipalAuthStrategy implements AuthStrategy {

    private final String tenantId;
    private final String clientId;
    private final String clientSecret;

    public ServicePrincipalAuthStrategy(String tenantId, String clientId, String clientSecret) {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public String configureConnection(String jdbcUrl, Properties properties) {
        // Microsoft JDBC driver recognizes these properties for SPN auth
        properties.setProperty("authentication", "ActiveDirectoryServicePrincipal");
        properties.setProperty("AADSecurePrincipalId", clientId);
        properties.setProperty("AADSecurePrincipalSecret", clientSecret);

        // Tenant ID is typically embedded in the authority URL
        // Some driver versions accept it as a property
        if (tenantId != null && !tenantId.isBlank()) {
            properties.setProperty("tenantId", tenantId);
        }

        return jdbcUrl;
    }

    @Override
    public String description() {
        return "Azure AD Service Principal (clientId=" + clientId + ", tenant=" + tenantId + ")";
    }
}

