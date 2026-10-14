package org.qamethod.datatesting.core.db.connector;

import org.qamethod.datatesting.core.io.DataSource;
import org.qamethod.datatesting.core.model.Record;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A {@link DataSource} backed by a SQL query against a {@link DatabaseConnector}.
 * <p>
 * Bridges the connector system with the core data pipeline — domain modules
 * can use this instead of CSV files to read reference data directly from
 * a live database.
 *
 * <pre>
 * DatabaseConnector connector = registry.get("analyticsDb");
 * DataSource source = new DatabaseDataSource(connector, "SELECT * FROM records", "Records");
 * source.stream().forEach(record -&gt; ...);
 * </pre>
 */
public class DatabaseDataSource implements DataSource {

    private static final Logger logger = Logger.getLogger(DatabaseDataSource.class.getName());

    private final DatabaseConnector connector;
    private final String sql;
    private final String sourceName;
    private List<String> columns;
    private long cachedCount = -1;

    /**
     * @param connector  the database connector
     * @param sql        the SELECT query to execute
     * @param sourceName label for this data source (e.g., "records")
     */
    public DatabaseDataSource(DatabaseConnector connector, String sql, String sourceName) {
        this.connector = connector;
        this.sql = sql;
        this.sourceName = sourceName;
    }

    @Override
    public List<String> columns() {
        if (columns == null) {
            // Execute query with LIMIT 0 to get metadata without fetching rows
            String metaSql = "SELECT * FROM (" + sql + ") AS _meta LIMIT 0";
            try (Connection conn = connector.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(metaSql)) {
                ResultSetMetaData meta = rs.getMetaData();
                List<String> colNames = new ArrayList<>(meta.getColumnCount());
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    colNames.add(meta.getColumnLabel(i));
                }
                columns = Collections.unmodifiableList(colNames);
            } catch (SQLException e) {
                logger.warning("Failed to load column metadata for " + sourceName
                        + ". Columns will be resolved on first stream() call.");
                columns = List.of();
            }
        }
        return columns;
    }

    @Override
    public Stream<Record> stream() {
        try {
            Connection conn = connector.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            ResultSetMetaData meta = rs.getMetaData();

            int colCount = meta.getColumnCount();
            List<String> colNames = new ArrayList<>(colCount);
            for (int i = 1; i <= colCount; i++) {
                colNames.add(meta.getColumnLabel(i));
            }
            this.columns = Collections.unmodifiableList(colNames);

            // Create a stream that reads ResultSet row by row
            Iterator<Record> iterator = new Iterator<>() {
                private boolean hasNext;
                {
                    try { hasNext = rs.next(); } catch (SQLException e) { hasNext = false; }
                }

                @Override
                public boolean hasNext() { return hasNext; }

                @Override
                public Record next() {
                    if (!hasNext) throw new NoSuchElementException();
                    try {
                        Map<String, String> fields = new LinkedHashMap<>();
                        for (int i = 1; i <= colCount; i++) {
                            fields.put(colNames.get(i - 1), rs.getString(i));
                        }
                        hasNext = rs.next();
                        return new Record(fields, sourceName);
                    } catch (SQLException e) {
                        throw new RuntimeException("Error reading result set", e);
                    }
                }
            };

            Spliterator<Record> spliterator = Spliterators.spliteratorUnknownSize(
                    iterator, Spliterator.ORDERED | Spliterator.NONNULL);

            // Close resources when stream is closed
            return StreamSupport.stream(spliterator, false)
                    .onClose(() -> {
                        try { rs.close(); } catch (SQLException ignored) {}
                        try { stmt.close(); } catch (SQLException ignored) {}
                        try { conn.close(); } catch (SQLException ignored) {}
                    });
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute query: " + sql, e);
        }
    }

    @Override
    public long recordCount() {
        if (cachedCount >= 0) return cachedCount;

        // Wrap in COUNT(*) subquery
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") AS _cnt";
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next()) {
                cachedCount = rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.warning("Failed to count records for " + sourceName + ": " + e.getMessage());
            cachedCount = 0;
        }
        return cachedCount;
    }

    @Override
    public Map<String, String> metadata() {
        return Map.of(
                "sourceName", sourceName,
                "connector", connector.label(),
                "sql", sql
        );
    }

    @Override
    public void close() {
        // Individual streams handle their own resource cleanup
    }
}

