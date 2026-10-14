package org.qamethod.datatesting.core.db.connector.auth;

import org.junit.jupiter.api.Test;
import org.qamethod.datatesting.core.db.connector.ConnectionConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthStrategyFactoryTest {

    @Test
    void create_supports_expected_auth_types() {
        assertThat(AuthStrategyFactory.create(configWithAuth("windows"))).isInstanceOf(WindowsAuthStrategy.class);
        assertThat(AuthStrategyFactory.create(configWithAuth("password"))).isInstanceOf(UsernamePasswordAuthStrategy.class);
        assertThat(AuthStrategyFactory.create(configWithAuth("spn"))).isInstanceOf(ServicePrincipalAuthStrategy.class);
        assertThat(AuthStrategyFactory.create(configWithAuth("managed-identity"))).isInstanceOf(ManagedIdentityAuthStrategy.class);
        assertThat(AuthStrategyFactory.create(configWithAuth("iam"))).isInstanceOf(AwsIamAuthStrategy.class);

        ConnectionConfig keyVault = configWithAuth("keyvault");
        ConnectionConfig.KeyVaultConfig kv = new ConnectionConfig.KeyVaultConfig();
        kv.setVaultUrl("https://vault.example.net");
        keyVault.getAuth().setKeyvault(kv);
        assertThat(AuthStrategyFactory.create(keyVault)).isInstanceOf(KeyVaultAuthStrategy.class);
    }

    @Test
    void create_validates_missing_or_invalid_auth_config() {
        ConnectionConfig noAuth = new ConnectionConfig();
        noAuth.setAuth(null);

        assertThatThrownBy(() -> AuthStrategyFactory.create(noAuth))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Auth configuration is required");

        ConnectionConfig keyVaultNoConfig = configWithAuth("keyvault");
        keyVaultNoConfig.getAuth().setKeyvault(null);
        assertThatThrownBy(() -> AuthStrategyFactory.create(keyVaultNoConfig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("keyvault config section required");

        assertThatThrownBy(() -> AuthStrategyFactory.create(configWithAuth("unknown")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown auth type");
    }

    private ConnectionConfig configWithAuth(String authType) {
        ConnectionConfig c = new ConnectionConfig();
        c.setHost("localhost");
        c.setPort(5432);
        c.setDatabase("db");

        ConnectionConfig.AuthConfig auth = new ConnectionConfig.AuthConfig();
        auth.setType(authType);
        auth.setUsername("user");
        auth.setPassword("pass");
        auth.setAwsRegion("us-east-1");
        auth.setTenantId("tenant");
        auth.setClientId("client");
        auth.setClientSecret("secret");
        c.setAuth(auth);

        return c;
    }
}

