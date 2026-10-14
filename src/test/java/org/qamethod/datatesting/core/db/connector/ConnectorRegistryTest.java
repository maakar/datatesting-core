package org.qamethod.datatesting.core.db.connector;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConnectorRegistryTest {

    @Test
    void register_get_has_names_and_close_work() {
        ConnectorRegistry registry = new ConnectorRegistry();
        StubConnector connector = new StubConnector(true);

        registry.register("primary", connector);

        assertThat(registry.has("primary")).isTrue();
        assertThat(registry.names()).containsExactly("primary");
        assertThat(registry.get("primary")).isSameAs(connector);

        registry.close();
        assertThat(connector.closed).isTrue();
        assertThat(registry.names()).isEmpty();
    }

    @Test
    void get_throws_for_unknown_name() {
        ConnectorRegistry registry = new ConnectorRegistry();
        assertThatThrownBy(() -> registry.get("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No database connector registered");
    }

    @Test
    void test_all_returns_per_connector_status() {
        ConnectorRegistry registry = new ConnectorRegistry();
        registry.register("ok", new StubConnector(true));
        registry.register("bad", new StubConnector(false));

        Map<String, Boolean> results = registry.testAll();

        assertThat(results).containsEntry("ok", true).containsEntry("bad", false);
    }

    private static class StubConnector implements DatabaseConnector {
        private final boolean ok;
        private boolean closed;

        private StubConnector(boolean ok) {
            this.ok = ok;
        }

        @Override
        public Connection getConnection() throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean testConnection() {
            return ok;
        }

        @Override
        public String label() {
            return "stub";
        }

        @Override
        public String jdbcUrl() {
            return "jdbc:stub";
        }

        @Override
        public void close() {
            this.closed = true;
        }
    }
}

