package org.qamethod.datatesting.core.db.connector.auth;

import org.qamethod.datatesting.core.db.connector.ConnectionConfig;

/**
 * Creates the appropriate {@link AuthStrategy} based on the configuration's
 * {@code auth.type} field.
 */
public final class AuthStrategyFactory {

    private AuthStrategyFactory() {}

    /**
     * Create an auth strategy from configuration.
     *
     * @param config the full connection config (for host/port needed by some strategies)
     */
    public static AuthStrategy create(ConnectionConfig config) {
        ConnectionConfig.AuthConfig auth = config.getAuth();
        if (auth == null) {
            throw new IllegalArgumentException("Auth configuration is required");
        }

        return switch (auth.getType().toLowerCase()) {
            case "windows" -> new WindowsAuthStrategy();
            case "password" -> new UsernamePasswordAuthStrategy(auth.getUsername(), auth.getPassword());
            case "spn" -> new ServicePrincipalAuthStrategy(auth.getTenantId(), auth.getClientId(), auth.getClientSecret());
            case "managed-identity" -> new ManagedIdentityAuthStrategy();
            case "keyvault" -> {
                if (auth.getKeyvault() == null) {
                    throw new IllegalArgumentException("keyvault config section required when auth.type=keyvault");
                }
                yield new KeyVaultAuthStrategy(auth.getKeyvault());
            }
            case "iam" -> new AwsIamAuthStrategy(
                    auth.getAwsRegion(), config.getHost(), config.effectivePort(), auth.getUsername());
            default -> throw new IllegalArgumentException("Unknown auth type: " + auth.getType()
                    + ". Supported: windows, password, spn, managed-identity, keyvault, iam");
        };
    }
}

