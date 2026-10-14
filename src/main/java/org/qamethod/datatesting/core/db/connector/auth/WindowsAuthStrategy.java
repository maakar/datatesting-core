package org.qamethod.datatesting.core.db.connector.auth;

import java.util.Properties;

/**
 * Windows Integrated Authentication (Kerberos/NTLM).
 * Uses the current Windows user's credentials.
 * <p>
 * For Azure SQL: appends {@code authentication=ActiveDirectoryIntegrated} or
 * {@code integratedSecurity=true} depending on the driver.
 */
public class WindowsAuthStrategy implements AuthStrategy {

    @Override
    public String configureConnection(String jdbcUrl, Properties properties) {
        // For Microsoft JDBC driver: use integrated security
        properties.setProperty("integratedSecurity", "true");
        // For Azure AD integrated: also set authentication mode
        properties.setProperty("authentication", "ActiveDirectoryIntegrated");
        return jdbcUrl;
    }

    @Override
    public String description() {
        return "Windows Integrated Authentication (current user)";
    }
}

