package org.qamethod.datatesting.core.io;

import io.github.vmzakharov.ecdataframe.dataframe.DataFrame;
import io.github.vmzakharov.ecdataframe.dataset.CsvDataSet;
import io.github.vmzakharov.ecdataframe.dataset.CsvSchema;
import io.github.vmzakharov.ecdataframe.dsl.value.ValueType;
import org.qamethod.datatesting.core.model.Record;

import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Loads a CSV file into an ECDataFrame {@link DataFrame} and exposes it
 * via the {@link DataSource} interface.
 * <p>
 * All columns are loaded as STRING type. The resulting DataFrame can also
 * be retrieved directly via {@link #dataFrame()} for efficient in-memory
 * operations (filtering, sorting, joining) without going through the
 * per-row {@link Record} abstraction.
 *
 * <pre>
 * CsvFileDataSource ds = new CsvFileDataSource(Path.of("records.csv"), "Records");
 * DataFrame df = ds.dataFrame();                 // direct DataFrame access
 * ds.stream().forEach(record -&gt; ...);            // or iterate as Records
 * </pre>
 */
public class CsvFileDataSource implements DataSource {

    private static final Logger logger = Logger.getLogger(CsvFileDataSource.class.getName());

    private final Path path;
    private final String sourceName;
    private final List<String> columns;
    private DataFrame dataFrame;

    /**
     * Load a CSV with all columns as STRING.
     *
     * @param path       path to the CSV file
     * @param sourceName label for this data source (used in logs and Record.sourceName)
     */
    public CsvFileDataSource(Path path, String sourceName) {
        this(path, sourceName, null);
    }

    /**
     * Load a CSV with explicit column names. If {@code columnNames} is null,
     * columns are inferred from the CSV header.
     */
    public CsvFileDataSource(Path path, String sourceName, List<String> columnNames) {
        this.path = path;
        this.sourceName = sourceName;

        CsvSchema schema = new CsvSchema();
        if (columnNames != null) {
            columnNames.forEach(col -> schema.addColumn(col, ValueType.STRING));
        }

        CsvDataSet csvDataSet = columnNames != null
                ? new CsvDataSet(path, sourceName, schema)
                : new CsvDataSet(path, sourceName);
        this.dataFrame = csvDataSet.loadAsDataFrame();

        // Capture column names from the loaded DataFrame
        List<String> cols = new ArrayList<>();
        this.dataFrame.getColumns().forEach(c -> cols.add(c.getName()));
        this.columns = Collections.unmodifiableList(cols);

        logger.info("Loaded " + sourceName + ": " + dataFrame.rowCount() + " rows, "
                + columns.size() + " columns from " + path.getFileName());
    }

    /**
     * Direct access to the underlying DataFrame for efficient bulk operations.
     */
    public DataFrame dataFrame() {
        return dataFrame;
    }

    /**
     * Replace the underlying DataFrame (e.g. after filtering).
     */
    public void setDataFrame(DataFrame df) {
        this.dataFrame = df;
    }

    @Override
    public List<String> columns() {
        return columns;
    }

    @Override
    public Stream<Record> stream() {
        return IntStream.range(0, dataFrame.rowCount())
                .mapToObj(row -> {
                    Map<String, String> fields = new LinkedHashMap<>();
                    for (String col : columns) {
                        fields.put(col, dataFrame.getString(col, row));
                    }
                    return new Record(fields, sourceName);
                });
    }

    @Override
    public long recordCount() {
        return dataFrame.rowCount();
    }

    @Override
    public Map<String, String> metadata() {
        return Map.of(
                "sourceName", sourceName,
                "filePath", path.toString(),
                "rowCount", String.valueOf(dataFrame.rowCount()),
                "columnCount", String.valueOf(columns.size())
        );
    }

    @Override
    public void close() {
        // DataFrame is in-memory, nothing to close
    }
}

