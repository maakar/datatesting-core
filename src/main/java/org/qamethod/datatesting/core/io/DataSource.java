package org.qamethod.datatesting.core.io;

import org.qamethod.datatesting.core.model.Record;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Universal abstraction for any tabular data source.
 * <p>
 * Implementations: {@code CsvDataSource} (CSV files via ECDataFrame or BufferedReader),
 * and potentially {@code JdbcDataSource} (any JDBC database).
 */
public interface DataSource extends AutoCloseable {

    /**
     * Column names in order.
     */
    List<String> columns();

    /**
     * Stream records one at a time for memory efficiency.
     * The stream must be consumed before closing the data source.
     */
    Stream<Record> stream();

    /**
     * Total record count (may require a pre-scan for streaming sources).
     */
    long recordCount();

    /**
     * Metadata about the source (file name, path, row count, etc.)
     */
    Map<String, String> metadata();
}

