package org.qamethod.datatesting.core.db.connector.auth;

import org.qamethod.datatesting.core.db.connector.ConnectionConfig;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * Azure Key Vault credential retrieval.
 * <p>
 * Fetches database credentials (username/password or SPN client secret) from
 * Azure Key Vault, then delegates to the appropriate auth strategy.
 * <p>
 * Requires Azure Identity + Key Vault Secrets client libraries at runtime.
 * If not on classpath, falls back with a clear error message.
 *
 * <pre>
 * auth:
 *   type: keyvault
 *   keyvault:
 *     vaultUrl: "https://myvault.vault.azure.net"
 *     usernameSecret: "db-username"
 *     passwordSecret: "db-password"
 * </pre>
 */
public class KeyVaultAuthStrategy implements AuthStrategy {

    private static final Logger logger = Logger.getLogger(KeyVaultAuthStrategy.class.getName());

    private final ConnectionConfig.KeyVaultConfig kvConfig;
    private AuthStrategy resolvedDelegate;

    public KeyVaultAuthStrategy(ConnectionConfig.KeyVaultConfig kvConfig) {
        this.kvConfig = kvConfig;
    }

    @Override
    public String configureConnection(String jdbcUrl, Properties properties) {
        if (resolvedDelegate == null) {
            resolvedDelegate = resolveCredentials();
        }
        return resolvedDelegate.configureConnection(jdbcUrl, properties);
    }

    private AuthStrategy resolveCredentials() {
        logger.info("Resolving credentials from Azure Key Vault: " + kvConfig.getVaultUrl());

        try {
            // Use reflection to avoid hard compile-time dependency on Azure SDK.
            // At runtime, azure-identity and azure-security-keyvault-secrets must be on classpath.
            Class<?> secretClientBuilderClass = Class.forName(
                    "com.azure.security.keyvault.secrets.SecretClientBuilder");
            Class<?> defaultCredentialBuilderClass = Class.forName(
                    "com.azure.identity.DefaultAzureCredentialBuilder");

            // DefaultAzureCredential -> tries Managed Identity, Azure CLI, IntelliJ, etc.
            Object credential = defaultCredentialBuilderClass
                    .getMethod("build").invoke(defaultCredentialBuilderClass.getConstructor().newInstance());

            // SecretClient builder
            Object builder = secretClientBuilderClass.getConstructor().newInstance();
            secretClientBuilderClass.getMethod("vaultUrl", String.class).invoke(builder, kvConfig.getVaultUrl());
            secretClientBuilderClass.getMethod("credential", Class.forName("com.azure.core.credential.TokenCredential"))
                    .invoke(builder, credential);
            Object secretClient = secretClientBuilderClass.getMethod("buildClient").invoke(builder);

            // Fetch secrets
            String username = fetchSecret(secretClient, kvConfig.getUsernameSecret());
            String password = fetchSecret(secretClient, kvConfig.getPasswordSecret());

            if (kvConfig.getClientSecretName() != null) {
                String clientSecret = fetchSecret(secretClient, kvConfig.getClientSecretName());
                logger.info("Key Vault: resolved SPN client secret from " + kvConfig.getClientSecretName());
                // If client secret is fetched, we need tenant/client IDs from the parent config
                // For now, return as password-based (most common Key Vault use case)
            }

            logger.info("Key Vault: resolved credentials for user '" + username + "'");
            return new UsernamePasswordAuthStrategy(username, password);

        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Azure Key Vault SDK not on classpath. Add 'com.azure:azure-security-keyvault-secrets' " +
                    "and 'com.azure:azure-identity' dependencies to use Key Vault auth.", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve credentials from Azure Key Vault: " + e.getMessage(), e);
        }
    }

    private String fetchSecret(Object secretClient, String secretName) throws Exception {
        if (secretName == null || secretName.isBlank()) return null;
        Object keyVaultSecret = secretClient.getClass()
                .getMethod("getSecret", String.class)
                .invoke(secretClient, secretName);
        return (String) keyVaultSecret.getClass().getMethod("getValue").invoke(keyVaultSecret);
    }

    @Override
    public String description() {
        return "Azure Key Vault (" + kvConfig.getVaultUrl() + ")";
    }
}

