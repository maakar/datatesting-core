package org.qamethod.datatesting.core.db.connector;

import org.junit.jupiter.api.Test;
import org.qamethod.datatesting.core.db.connector.aws.AwsRdsConnector;
import org.qamethod.datatesting.core.db.connector.azure.AzureSqlConnector;
import org.qamethod.datatesting.core.db.connector.gcp.GcpCloudSqlConnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConnectorFactoryTest {

    @Test
    void create_selects_connector_by_provider_and_aliases() {
        assertThat(ConnectorFactory.create(config("azure-sql"))).isInstanceOf(AzureSqlConnector.class);
        assertThat(ConnectorFactory.create(config("azure"))).isInstanceOf(AzureSqlConnector.class);
        assertThat(ConnectorFactory.create(config("mssql"))).isInstanceOf(AzureSqlConnector.class);

        assertThat(ConnectorFactory.create(config("gcp-cloudsql"))).isInstanceOf(GcpCloudSqlConnector.class);
        assertThat(ConnectorFactory.create(config("gcp"))).isInstanceOf(GcpCloudSqlConnector.class);
        assertThat(ConnectorFactory.create(config("cloudsql"))).isInstanceOf(GcpCloudSqlConnector.class);

        assertThat(ConnectorFactory.create(config("aws-rds"))).isInstanceOf(AwsRdsConnector.class);
        assertThat(ConnectorFactory.create(config("aws"))).isInstanceOf(AwsRdsConnector.class);
        assertThat(ConnectorFactory.create(config("rds"))).isInstanceOf(AwsRdsConnector.class);

        assertThat(ConnectorFactory.create(config("generic"))).isInstanceOf(GenericJdbcConnector.class);
    }

    @Test
    void create_validates_provider_presence_and_value() {
        ConnectionConfig nullProvider = config(null);
        assertThatThrownBy(() -> ConnectorFactory.create(nullProvider))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider must be specified");

        assertThatThrownBy(() -> ConnectorFactory.create(config("unknown-provider")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown database provider");
    }

    private ConnectionConfig config(String provider) {
        ConnectionConfig c = new ConnectionConfig();
        c.setProvider(provider);
        c.setHost("localhost");
        c.setDatabase("testdb");
        ConnectionConfig.AuthConfig auth = new ConnectionConfig.AuthConfig();
        auth.setType("password");
        auth.setUsername("user");
        auth.setPassword("pass");
        c.setAuth(auth);
        return c;
    }
}

