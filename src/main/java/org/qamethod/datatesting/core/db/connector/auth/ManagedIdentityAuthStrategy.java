package org.qamethod.datatesting.core.db.connector.auth;

import java.util.Properties;

/**
 * Azure Managed Identity authentication.
 * Uses the system-assigned or user-assigned managed identity of the
 * Azure resource (VM, App Service, AKS pod, etc.) running the application.
 * <p>
 * No credentials needed -- the Azure platform handles token acquisition.
 * Requires the Microsoft JDBC Driver for SQL Server with Azure AD support.
 */
public class ManagedIdentityAuthStrategy implements AuthStrategy {

    @Override
    public String configureConnection(String jdbcUrl, Properties properties) {
        properties.setProperty("authentication", "ActiveDirectoryManagedIdentity");
        return jdbcUrl;
    }

    @Override
    public String description() {
        return "Azure Managed Identity";
    }
}

