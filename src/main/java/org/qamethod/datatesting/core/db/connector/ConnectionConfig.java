package org.qamethod.datatesting.core.db.connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * YAML-friendly configuration for a single database connection.
 *
 * <pre>
 * connections:
 *   analyticsDb:
 *     provider: azure-sql            # azure-sql | gcp-cloudsql | aws-rds | generic
 *     host: myserver.database.windows.net
 *     port: 1433
 *     database: AnalyticsDB
 *     auth:
 *       type: spn                    # windows | password | spn | keyvault
 *       tenantId: "..."
 *       clientId: "..."
 *       clientSecret: "..."          # or use keyvault reference
 *     options:
 *       encrypt: "true"
 *       trustServerCertificate: "false"
 *       loginTimeout: "30"
 *       connectTimeout: "30"
 *     label: "Azure SQL - AnalyticsDB"
 *
 *   warehouseDb:
 *     provider: gcp-cloudsql
 *     host: /cloudsql/project:region:instance
 *     database: warehouse
 *     auth:
 *       type: password
 *       username: "reader"
 *       password: "..."
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectionConfig {

    /** Cloud/DB provider: azure-sql, gcp-cloudsql, aws-rds, generic */
    private String provider = "generic";

    private String host;
    private int port = 0; // 0 = use provider default
    private String database;
    private String label;

    /** Authentication configuration */
    private AuthConfig auth = new AuthConfig();

    /** Additional JDBC connection properties (encrypt, timeout, etc.) */
    private Map<String, String> options = Map.of();

    // -- Getters / Setters --

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public AuthConfig getAuth() { return auth; }
    public void setAuth(AuthConfig auth) { this.auth = auth; }

    public Map<String, String> getOptions() { return options; }
    public void setOptions(Map<String, String> options) { this.options = options; }

    /**
     * Resolve the effective port (provider default if not set).
     */
    public int effectivePort() {
        if (port > 0) return port;
        return switch (provider.toLowerCase()) {
            case "azure-sql" -> 1433;
            case "gcp-cloudsql" -> 5432;
            case "aws-rds" -> 3306;
            default -> 0;
        };
    }

    /**
     * Resolve the effective label (auto-derive if not set).
     */
    public String effectiveLabel() {
        if (label != null && !label.isBlank()) return label;
        return provider + " - " + database + "@" + host;
    }

    // =====================================================================

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthConfig {

        /**
         * Authentication type:
         * <ul>
         *   <li>{@code windows} - Windows Integrated Authentication (Kerberos/NTLM)</li>
         *   <li>{@code password} - Username + password</li>
         *   <li>{@code spn} - Azure AD Service Principal (client credentials)</li>
         *   <li>{@code managed-identity} - Azure Managed Identity</li>
         *   <li>{@code keyvault} - Retrieve credentials from Azure Key Vault, then use them</li>
         *   <li>{@code iam} - AWS IAM authentication token</li>
         * </ul>
         */
        private String type = "password";

        // -- password auth --
        private String username;
        private String password;

        // -- Azure AD SPN auth --
        private String tenantId;
        private String clientId;
        private String clientSecret;

        // -- Azure Key Vault --
        @JsonProperty("keyvault")
        private KeyVaultConfig keyvault;

        // -- AWS IAM --
        private String awsRegion;
        private String awsProfile;

        // -- GCP --
        private String gcpProject;
        private String gcpServiceAccountKeyFile;

        // -- Getters / Setters --

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

        public KeyVaultConfig getKeyvault() { return keyvault; }
        public void setKeyvault(KeyVaultConfig keyvault) { this.keyvault = keyvault; }

        public String getAwsRegion() { return awsRegion; }
        public void setAwsRegion(String awsRegion) { this.awsRegion = awsRegion; }

        public String getAwsProfile() { return awsProfile; }
        public void setAwsProfile(String awsProfile) { this.awsProfile = awsProfile; }

        public String getGcpProject() { return gcpProject; }
        public void setGcpProject(String gcpProject) { this.gcpProject = gcpProject; }

        public String getGcpServiceAccountKeyFile() { return gcpServiceAccountKeyFile; }
        public void setGcpServiceAccountKeyFile(String f) { this.gcpServiceAccountKeyFile = f; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KeyVaultConfig {
        /** Azure Key Vault URL, e.g. https://myvault.vault.azure.net */
        private String vaultUrl;

        /** Secret names in Key Vault for each credential field */
        private String usernameSecret;
        private String passwordSecret;
        private String clientSecretName;

        public String getVaultUrl() { return vaultUrl; }
        public void setVaultUrl(String vaultUrl) { this.vaultUrl = vaultUrl; }

        public String getUsernameSecret() { return usernameSecret; }
        public void setUsernameSecret(String usernameSecret) { this.usernameSecret = usernameSecret; }

        public String getPasswordSecret() { return passwordSecret; }
        public void setPasswordSecret(String passwordSecret) { this.passwordSecret = passwordSecret; }

        public String getClientSecretName() { return clientSecretName; }
        public void setClientSecretName(String clientSecretName) { this.clientSecretName = clientSecretName; }
    }
}

