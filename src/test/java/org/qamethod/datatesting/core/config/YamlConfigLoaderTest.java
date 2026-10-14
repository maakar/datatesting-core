package org.qamethod.datatesting.core.config;

import org.junit.jupiter.api.Test;
import org.qamethod.datatesting.core.db.connector.ConnectionConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YamlConfigLoaderTest {

    @Test
    void load_reads_explicit_path_and_maps_provider_and_auth() throws Exception {
        Path yaml = Files.createTempFile("connections-", ".yaml");
        yaml.toFile().deleteOnExit();
        Files.writeString(yaml,
                "connections:\n"
                        + "  primary:\n"
                        + "    provider: azure-sql\n"
                        + "    host: db.example.com\n"
                        + "    database: appdb\n"
                        + "    auth:\n"
                        + "      type: password\n"
                        + "      username: app\n"
                        + "      password: secret\n"
                        + "  replica:\n"
                        + "    provider: aws-rds\n"
                        + "    host: replica.example.com\n"
                        + "    database: appdb\n"
                        + "    auth:\n"
                        + "      type: iam\n"
                        + "      username: app\n"
                        + "      awsRegion: us-east-1\n");

        ConnectorConfigRoot root = YamlConfigLoader.load(yaml.toString(), "unused.yaml", ConnectorConfigRoot.class);

        assertThat(root.connections).containsKeys("primary", "replica");
        ConnectionConfig primary = root.connections.get("primary");
        assertThat(primary.getProvider()).isEqualTo("azure-sql");
        assertThat(primary.getHost()).isEqualTo("db.example.com");
        assertThat(primary.getAuth().getType()).isEqualTo("password");
        assertThat(primary.getAuth().getUsername()).isEqualTo("app");

        ConnectionConfig replica = root.connections.get("replica");
        assertThat(replica.getProvider()).isEqualTo("aws-rds");
        assertThat(replica.getAuth().getType()).isEqualTo("iam");
        assertThat(replica.getAuth().getAwsRegion()).isEqualTo("us-east-1");
    }

    @Test
    void load_throws_when_explicit_path_does_not_exist() {
        assertThatThrownBy(() -> YamlConfigLoader.load("/no/such/file.yaml", "ignored.yaml", ConnectorConfigRoot.class))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("Config file not found");
    }

    static class ConnectorConfigRoot {
        public Map<String, ConnectionConfig> connections;
    }
}

