package org.qamethod.datatesting.core.compare;

import org.eclipse.collections.impl.factory.Lists;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class SortMergeCompareIT {

    @Test
    void execute_classifies_rows_into_match_source_only_target_only_and_mismatch() throws Exception {
        String dbUrl = tempDbUrl();
        createInputTables(dbUrl);

        SortMergeCompare.Stats stats = new SortMergeCompare(dbUrl)
                .sourceTable("source_data")
                .targetTable("target_data")
                .keyColumns(Lists.mutable.of("ID"))
                .allColumns(Lists.mutable.of("ID", "NAME", "VALUE"))
                .execute();

        assertThat(stats.perfectMatches()).isEqualTo(2);
        assertThat(stats.sourceOnly()).isEqualTo(1);
        assertThat(stats.targetOnly()).isEqualTo(1);
        assertThat(stats.mismatches()).isEqualTo(1);
        assertThat(stats.totalRecords()).isEqualTo(5);

        assertThat(countRows(dbUrl, "missing_in_target")).isEqualTo(1);
        assertThat(countRows(dbUrl, "missing_in_source")).isEqualTo(1);
        assertThat(countRows(dbUrl, "mismatches")).isEqualTo(1);

        String comment = queryOne(dbUrl, "SELECT Comment FROM mismatches LIMIT 1");
        assertThat(comment).contains("VALUE");
    }

    @Test
    void execute_uses_custom_matcher_when_registered() throws Exception {
        String dbUrl = tempDbUrl();

        try (Connection conn = DriverManager.getConnection(dbUrl); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE source_data (ID TEXT, NAME TEXT, VALUE TEXT)");
            stmt.execute("CREATE TABLE target_data (ID TEXT, NAME TEXT, VALUE TEXT)");
            stmt.execute("INSERT INTO source_data VALUES ('1','A','100')");
            stmt.execute("INSERT INTO target_data VALUES ('1','A','103')");
        }

        FieldComparator tolerance = new FieldComparator() {
            @Override
            public boolean matches(String src, String tgt) {
                long s = Long.parseLong(src);
                long t = Long.parseLong(tgt);
                return Math.abs(s - t) <= 5;
            }

            @Override
            public String name() {
                return "tolerance5";
            }
        };

        SortMergeCompare.Stats stats = new SortMergeCompare(dbUrl)
                .sourceTable("source_data")
                .targetTable("target_data")
                .keyColumns(Lists.mutable.of("ID"))
                .allColumns(Lists.mutable.of("ID", "NAME", "VALUE"))
                .addCustomMatcher("VALUE", tolerance)
                .execute();

        assertThat(stats.perfectMatches()).isEqualTo(1);
        assertThat(stats.mismatches()).isZero();
    }

    @Test
    void execute_sorts_by_key_even_when_input_insertion_order_is_unsorted() throws Exception {
        String dbUrl = tempDbUrl();

        try (Connection conn = DriverManager.getConnection(dbUrl); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE source_data (ID TEXT, VALUE TEXT)");
            stmt.execute("CREATE TABLE target_data (ID TEXT, VALUE TEXT)");

            stmt.execute("INSERT INTO source_data VALUES ('2','B')");
            stmt.execute("INSERT INTO source_data VALUES ('1','A')");
            stmt.execute("INSERT INTO target_data VALUES ('1','A')");
            stmt.execute("INSERT INTO target_data VALUES ('2','B')");
        }

        SortMergeCompare.Stats stats = new SortMergeCompare(dbUrl)
                .sourceTable("source_data")
                .targetTable("target_data")
                .keyColumns(Lists.mutable.of("ID"))
                .allColumns(Lists.mutable.of("ID", "VALUE"))
                .execute();

        assertThat(stats.perfectMatches()).isEqualTo(2);
        assertThat(stats.sourceOnly()).isZero();
        assertThat(stats.targetOnly()).isZero();
        assertThat(stats.mismatches()).isZero();
    }

    @Test
    void execute_treats_null_and_empty_as_equal_after_normalization() throws Exception {
        String dbUrl = tempDbUrl();

        try (Connection conn = DriverManager.getConnection(dbUrl); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE source_data (ID TEXT, VALUE TEXT)");
            stmt.execute("CREATE TABLE target_data (ID TEXT, VALUE TEXT)");

            stmt.execute("INSERT INTO source_data VALUES ('1', NULL)");
            stmt.execute("INSERT INTO target_data VALUES ('1', '')");
        }

        SortMergeCompare.Stats stats = new SortMergeCompare(dbUrl)
                .sourceTable("source_data")
                .targetTable("target_data")
                .keyColumns(Lists.mutable.of("ID"))
                .allColumns(Lists.mutable.of("ID", "VALUE"))
                .execute();

        assertThat(stats.perfectMatches()).isEqualTo(1);
        assertThat(stats.mismatches()).isZero();
    }

    @Test
    void execute_handles_duplicate_keys_deterministically() throws Exception {
        String dbUrl = tempDbUrl();

        try (Connection conn = DriverManager.getConnection(dbUrl); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE source_data (ID TEXT, VALUE TEXT)");
            stmt.execute("CREATE TABLE target_data (ID TEXT, VALUE TEXT)");

            stmt.execute("INSERT INTO source_data VALUES ('1','A')");
            stmt.execute("INSERT INTO source_data VALUES ('1','B')");

            stmt.execute("INSERT INTO target_data VALUES ('1','A')");
            stmt.execute("INSERT INTO target_data VALUES ('1','C')");
        }

        SortMergeCompare.Stats stats = new SortMergeCompare(dbUrl)
                .sourceTable("source_data")
                .targetTable("target_data")
                .keyColumns(Lists.mutable.of("ID"))
                .allColumns(Lists.mutable.of("ID", "VALUE"))
                .execute();

        assertThat(stats.perfectMatches()).isEqualTo(1);
        assertThat(stats.mismatches()).isEqualTo(1);
        assertThat(stats.sourceOnly()).isZero();
        assertThat(stats.targetOnly()).isZero();
    }

    @Test
    void execute_performance_sanity_small_dataset() throws Exception {
        String dbUrl = tempDbUrl();

        try (Connection conn = DriverManager.getConnection(dbUrl); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE source_data (ID TEXT, VALUE TEXT)");
            stmt.execute("CREATE TABLE target_data (ID TEXT, VALUE TEXT)");
            for (int i = 0; i < 1000; i++) {
                stmt.execute("INSERT INTO source_data VALUES ('" + i + "','V" + i + "')");
                stmt.execute("INSERT INTO target_data VALUES ('" + i + "','V" + i + "')");
            }
        }

        long startNanos = System.nanoTime();
        SortMergeCompare.Stats stats = new SortMergeCompare(dbUrl)
                .sourceTable("source_data")
                .targetTable("target_data")
                .keyColumns(Lists.mutable.of("ID"))
                .allColumns(Lists.mutable.of("ID", "VALUE"))
                .execute();
        long durationMillis = (System.nanoTime() - startNanos) / 1_000_000;

        assertThat(stats.perfectMatches()).isEqualTo(1000);
        assertThat(durationMillis).isLessThan(10_000);
    }

    private void createInputTables(String dbUrl) throws Exception {
        try (Connection conn = DriverManager.getConnection(dbUrl); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE source_data (ID TEXT, NAME TEXT, VALUE TEXT)");
            stmt.execute("CREATE TABLE target_data (ID TEXT, NAME TEXT, VALUE TEXT)");

            stmt.execute("INSERT INTO source_data VALUES ('1','A','10')");
            stmt.execute("INSERT INTO source_data VALUES ('2','B','20')");
            stmt.execute("INSERT INTO source_data VALUES ('3','C','30')");
            stmt.execute("INSERT INTO source_data VALUES ('4','D','40')");

            stmt.execute("INSERT INTO target_data VALUES ('1','A','10')");
            stmt.execute("INSERT INTO target_data VALUES ('2','B','25')");
            stmt.execute("INSERT INTO target_data VALUES ('4','D','40')");
            stmt.execute("INSERT INTO target_data VALUES ('5','E','50')");
        }
    }

    private String tempDbUrl() throws Exception {
        Path file = Files.createTempFile("sort-merge-", ".db");
        file.toFile().deleteOnExit();
        return "jdbc:sqlite:" + file;
    }

    private long countRows(String dbUrl, String table) throws Exception {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM \"" + table + "\"")) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private String queryOne(String dbUrl, String sql) throws Exception {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }
}


