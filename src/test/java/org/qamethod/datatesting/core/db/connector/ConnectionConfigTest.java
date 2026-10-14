package org.qamethod.datatesting.core.db.connector;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectionConfigTest {

    @Test
    void effective_port_uses_provider_defaults_when_port_not_set() {
        ConnectionConfig azure = new ConnectionConfig();
        azure.setProvider("azure-sql");

        ConnectionConfig gcp = new ConnectionConfig();
        gcp.setProvider("gcp-cloudsql");

        ConnectionConfig aws = new ConnectionConfig();
        aws.setProvider("aws-rds");

        assertThat(azure.effectivePort()).isEqualTo(1433);
        assertThat(gcp.effectivePort()).isEqualTo(5432);
        assertThat(aws.effectivePort()).isEqualTo(3306);
    }

    @Test
    void effective_port_prefers_explicit_port() {
        ConnectionConfig config = new ConnectionConfig();
        config.setProvider("azure-sql");
        config.setPort(1500);

        assertThat(config.effectivePort()).isEqualTo(1500);
    }

    @Test
    void effective_label_auto_derives_when_not_set() {
        ConnectionConfig config = new ConnectionConfig();
        config.setProvider("generic");
        config.setDatabase("db");
        config.setHost("localhost");

        assertThat(config.effectiveLabel()).isEqualTo("generic - db@localhost");

        config.setLabel("Custom Label");
        assertThat(config.effectiveLabel()).isEqualTo("Custom Label");
    }
}

