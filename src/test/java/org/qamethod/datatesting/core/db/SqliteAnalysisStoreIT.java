package org.qamethod.datatesting.core.db;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.jupiter.api.Test;
import org.qamethod.datatesting.core.io.CsvFileDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqliteAnalysisStoreIT {

    @Test
    void table_operations_and_queries_work_end_to_end() throws Exception {
        SqliteAnalysisStore store = new SqliteAnalysisStore(tempDbUrl());
        ListIterable<String> columns = Lists.mutable.of("ID", "NAME");

        store.initialize(true);
        store.recreateTable("records", columns, "TEXT");
        store.execute("INSERT INTO records (ID, NAME, Comment) VALUES ('1', 'Alice', 'seed')");
        store.execute("INSERT INTO records (ID, NAME, Comment) VALUES ('2', 'Bob', 'seed')");

        assertThat(store.count("records")).isEqualTo(2);
        assertThat(store.queryString("SELECT NAME FROM records WHERE ID = ?", "1")).isEqualTo("Alice");
        assertThat(store.queryLong("SELECT COUNT(*) FROM records WHERE NAME LIKE ?", "%o%"))
                .isEqualTo(1);

        store.initMetadataTable();
        store.putMetadata("sourceRecordCount", 123L);
        assertThat(store.getMetadataLong("sourceRecordCount", 0)).isEqualTo(123L);
        assertThat(store.getMetadataLong("missing", 99L)).isEqualTo(99L);
    }

    @Test
    void batch_insert_from_dataframe_inserts_rows_and_comment() throws Exception {
        SqliteAnalysisStore store = new SqliteAnalysisStore(tempDbUrl());
        ListIterable<String> columns = Lists.mutable.of("ID", "VALUE", "TYPE");
        Path csvPath = Files.createTempFile("store-fixture-", ".csv");
        Files.writeString(csvPath, "ID,VALUE,TYPE\n"
                + "a,10,A\n"
                + "b,50,B\n"
                + "c,150,C\n"
                + "d,250,D\n");
        csvPath.toFile().deleteOnExit();
        CsvFileDataSource ds = new CsvFileDataSource(csvPath, "Fixture", List.of("ID", "VALUE", "TYPE"));

        store.recreateTable("records", columns, "TEXT");
        store.batchInsertFromDataFrame("records", ds.dataFrame(), columns, "loaded", 1);

        assertThat(store.count("records")).isEqualTo(4);
        assertThat(store.queryString("SELECT Comment FROM records WHERE ID = ?", "a")).isEqualTo("loaded");
        assertThat(store.queryString("SELECT VALUE FROM records WHERE ID = ?", "c")).isEqualTo("150");
    }

    @Test
    void execute_throws_for_invalid_sql() throws Exception {
        SqliteAnalysisStore store = new SqliteAnalysisStore(tempDbUrl());

        assertThatThrownBy(() -> store.execute("THIS IS NOT SQL"))
                .isInstanceOf(SQLException.class);
    }

    @Test
    void get_connection_can_be_closed_cleanly() throws Exception {
        SqliteAnalysisStore store = new SqliteAnalysisStore(tempDbUrl());

        Connection conn = store.getConnection();
        assertThat(conn.isClosed()).isFalse();
        conn.close();
        assertThat(conn.isClosed()).isTrue();
    }

    private String tempDbUrl() throws Exception {
        Path file = Files.createTempFile("sqlite-store-", ".db");
        file.toFile().deleteOnExit();
        return "jdbc:sqlite:" + file;
    }

}



