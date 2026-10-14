package org.qamethod.datatesting.core.db;

import io.github.vmzakharov.ecdataframe.dataframe.DataFrame;
import org.eclipse.collections.api.list.ListIterable;

import java.sql.*;
import java.util.logging.Logger;

/**
 * Ready-to-use SQLite-backed analysis store.
 * <p>
 * Provides concrete implementations for common database operations:
 * table creation, batch inserts from DataFrames, row counting,
 * metadata storage, and query execution.
 * <p>
 * Domain modules can use this directly or extend it for custom table schemas.
 *
 * <pre>
 * SqliteAnalysisStore store = new SqliteAnalysisStore("jdbc:sqlite:results.db");
 * store.initialize(true);
 * store.createTable("source_data", columns, "TEXT");
 * store.batchInsertFromDataFrame("source_data", df, columns);
 * long count = store.count("source_data");
 * </pre>
 */
public class SqliteAnalysisStore implements AnalysisStore {

    private static final Logger logger = Logger.getLogger(SqliteAnalysisStore.class.getName());

    private final String dbUrl;

    public SqliteAnalysisStore(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    @Override
    public void initialize(boolean recreateTables) throws SQLException {
        // Base initialization — subclasses override to create their specific tables
        logger.info("Initializing SQLite store: " + dbUrl + " (recreate=" + recreateTables + ")");
        // Ensure the database file exists by opening a connection
        try (Connection conn = getConnection()) {
            conn.createStatement().execute("SELECT 1");
        }
    }

    @Override
    public void execute(String sql) throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    @Override
    public String getDbUrl() {
        return dbUrl;
    }

    /**
     * Get a new JDBC connection. Caller must close it.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    // =====================================================================
    //  Table operations
    // =====================================================================

    /**
     * Create a table with the given columns, all of the specified SQL type.
     * Adds a Comment column automatically.
     */
    public void createTable(String tableName, ListIterable<String> columns, String sqlType) throws SQLException {
        StringBuilder ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS \"")
                .append(tableName).append("\" (");
        columns.forEach(col -> ddl.append("\"").append(col).append("\" ").append(sqlType).append(", "));
        ddl.append("\"Comment\" TEXT)");

        execute(ddl.toString());
        logger.fine("Created table: " + tableName);
    }

    /**
     * Drop and recreate a table.
     */
    public void recreateTable(String tableName, ListIterable<String> columns, String sqlType) throws SQLException {
        execute("DROP TABLE IF EXISTS \"" + tableName + "\"");
        createTable(tableName, columns, sqlType);
    }

    /**
     * Get the row count of a table.
     */
    public long count(String tableName) throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM \"" + tableName + "\"")) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    /**
     * Query for a single string value.
     */
    public String queryString(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /**
     * Query for a single long value.
     */
    public long queryLong(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    // =====================================================================
    //  DataFrame batch operations
    // =====================================================================

    /**
     * Batch-insert all rows from a DataFrame into a table.
     *
     * @param tableName target table
     * @param df        source DataFrame
     * @param columns   column names to insert (must exist in both df and table)
     */
    public void batchInsertFromDataFrame(String tableName, DataFrame df,
                                          ListIterable<String> columns) throws SQLException {
        batchInsertFromDataFrame(tableName, df, columns, null, 5000);
    }

    /**
     * Batch-insert rows with an optional Comment value per row.
     *
     * @param tableName  target table
     * @param df         source DataFrame
     * @param columns    column names to insert
     * @param comment    static comment for all rows (null = no comment)
     * @param batchSize  commit interval
     */
    public void batchInsertFromDataFrame(String tableName, DataFrame df,
                                          ListIterable<String> columns,
                                          String comment, int batchSize) throws SQLException {
        String sql = buildInsertSql(tableName, columns, comment != null);
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            int batch = 0;

            for (int row = 0; row < df.rowCount(); row++) {
                for (int c = 0; c < columns.size(); c++) {
                    ps.setString(c + 1, df.getString(columns.get(c), row));
                }
                if (comment != null) {
                    ps.setString(columns.size() + 1, comment);
                }
                ps.addBatch();
                batch++;

                if (batch >= batchSize) {
                    ps.executeBatch();
                    conn.commit();
                    batch = 0;
                }
            }

            if (batch > 0) {
                ps.executeBatch();
                conn.commit();
            }

            logger.info("Inserted " + df.rowCount() + " rows into " + tableName);
        }
    }

    /**
     * Build a parameterized INSERT SQL statement.
     */
    public String buildInsertSql(String tableName, ListIterable<String> columns, boolean hasComment) {
        StringBuilder sb = new StringBuilder("INSERT INTO \"").append(tableName).append("\" (");
        columns.forEach(col -> sb.append("\"").append(col).append("\", "));
        if (hasComment) sb.append("\"Comment\"");
        else sb.setLength(sb.length() - 2); // remove trailing ", "
        sb.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) sb.append("?,");
        if (hasComment) sb.append("?");
        else sb.setLength(sb.length() - 1); // remove trailing ","
        sb.append(")");
        return sb.toString();
    }

    // =====================================================================
    //  Metadata store (key-value pairs for pipeline state)
    // =====================================================================

    /**
     * Create the metadata table if it doesn't exist.
     */
    public void initMetadataTable() throws SQLException {
        execute("CREATE TABLE IF NOT EXISTS _metadata (key TEXT PRIMARY KEY, value TEXT)");
    }

    /**
     * Store a metadata key-value pair.
     */
    public void putMetadata(String key, String value) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR REPLACE INTO _metadata (key, value) VALUES (?, ?)")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }

    /**
     * Retrieve a metadata value by key.
     */
    public String getMetadata(String key) throws SQLException {
        return queryString("SELECT value FROM _metadata WHERE key = ?", key);
    }

    /**
     * Store a long value as metadata.
     */
    public void putMetadata(String key, long value) throws SQLException {
        putMetadata(key, String.valueOf(value));
    }

    /**
     * Retrieve a long metadata value.
     */
    public long getMetadataLong(String key, long defaultValue) throws SQLException {
        String val = getMetadata(key);
        return val != null ? Long.parseLong(val) : defaultValue;
    }
}

