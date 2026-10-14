package org.qamethod.datatesting.core.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqliteDbConsolidatorIT {

    @TempDir
    Path tempDir;

    @Test
    void consolidate_merges_tables_and_creates_consolidation_log() throws Exception {
        Path db1 = tempDir.resolve("chunk_2026-03-01.db");
        Path db2 = tempDir.resolve("chunk_2026-03-02.db");
        Path consolidatedDb = tempDir.resolve("consolidated.db");

        // Seed chunk 1
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + db1);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE results (ID TEXT, VALUE TEXT)");
            stmt.execute("INSERT INTO results VALUES ('1', '100')");
            stmt.execute("INSERT INTO results VALUES ('2', '200')");
        }

        // Seed chunk 2
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + db2);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE results (ID TEXT, VALUE TEXT)");
            stmt.execute("INSERT INTO results VALUES ('3', '300')");
        }

        SqliteDbConsolidator consolidator = new SqliteDbConsolidator();
        consolidator.consolidate(
                consolidatedDb,
                List.of(db1, db2),
                path -> path.getFileName().toString().replace("chunk_", "").replace(".db", "")
        );

        // Verify consolidated database
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + consolidatedDb);
             Statement stmt = conn.createStatement()) {

            // 1. Check total merged rows in results
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM results")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getLong(1)).isEqualTo(3L);
            }

            // 2. Check analysis_date column was populated accurately per chunk
            try (ResultSet rs = stmt.executeQuery("SELECT ID, VALUE, analysis_date FROM results ORDER BY ID")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("ID")).isEqualTo("1");
                assertThat(rs.getString("VALUE")).isEqualTo("100");
                assertThat(rs.getString("analysis_date")).isEqualTo("2026-03-01");

                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("ID")).isEqualTo("2");
                assertThat(rs.getString("VALUE")).isEqualTo("200");
                assertThat(rs.getString("analysis_date")).isEqualTo("2026-03-01");

                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("ID")).isEqualTo("3");
                assertThat(rs.getString("VALUE")).isEqualTo("300");
                assertThat(rs.getString("analysis_date")).isEqualTo("2026-03-02");
            }

            // 3. Check consolidation_log table entries
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM consolidation_log")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getLong(1)).isEqualTo(2L);
            }

            try (ResultSet rs = stmt.executeQuery("SELECT analysis_date FROM consolidation_log ORDER BY id")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("analysis_date")).isEqualTo("2026-03-01");
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("analysis_date")).isEqualTo("2026-03-02");
            }
        }
    }

    @Test
    void consolidate_validates_minimum_inputs() {
        SqliteDbConsolidator consolidator = new SqliteDbConsolidator();
        Path out = tempDir.resolve("out.db");
        Path single = tempDir.resolve("single.db");

        assertThatThrownBy(() -> consolidator.consolidate(out, List.of(single), p -> "date"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least 2 input databases are required");
    }
}
