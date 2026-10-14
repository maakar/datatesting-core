package org.qamethod.datatesting.core.filter;

import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.qamethod.datatesting.core.io.CsvUtils;
import org.qamethod.datatesting.core.model.Record;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Loads exclusion values from CSV files and filters out matching records.
 */
public class CsvExclusionFilter implements RecordFilter {

    private static final Logger logger = Logger.getLogger(CsvExclusionFilter.class.getName());

    private final String columnName;
    private final MutableSet<String> excludedValues;

    public CsvExclusionFilter(String columnName, Path filterDir, Set<String> recognisedHeaders) throws IOException {
        this.columnName = columnName;
        this.excludedValues = loadExclusions(filterDir, recognisedHeaders);
    }

    @Override
    public boolean test(Record record) {
        String value = record.get(columnName);
        return value == null || !excludedValues.contains(value);
    }

    @Override
    public String name() {
        return "CsvExclusionFilter(" + excludedValues.size() + " values)";
    }

    public int excludedCount() {
        return excludedValues.size();
    }

    private MutableSet<String> loadExclusions(Path folderPath, Set<String> recognisedHeaders) throws IOException {
        MutableSet<String> values = Sets.mutable.empty();
        if (!Files.exists(folderPath)) {
            logger.warning("Filter directory not found: " + folderPath.toAbsolutePath());
            return values;
        }

        Set<String> normalizedHeaders = Sets.mutable.withAll(recognisedHeaders)
                .collect(h -> CsvUtils.clean(h).toLowerCase(Locale.ROOT))
                .toSet();

        try (Stream<Path> paths = Files.walk(folderPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".csv"))
                    .forEach(path -> readCsv(path, normalizedHeaders, values));
        }

        logger.info("Loaded " + values.size() + " exclusions from " + folderPath.toAbsolutePath());
        return values;
    }

    private void readCsv(Path path, Set<String> normalizedHeaders, MutableSet<String> values) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String firstLine = reader.readLine();
            if (firstLine == null) return;

            String[] firstCols = CsvUtils.parseLine(firstLine);
            int headerIndex = firstMatchingHeaderIndex(firstCols, normalizedHeaders);
            boolean hasHeader = headerIndex >= 0;
            int valueIndex = hasHeader ? headerIndex : 0;

            if (!hasHeader) {
                addValue(CsvUtils.getField(firstCols, valueIndex), values);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] cols = CsvUtils.parseLine(line);
                addValue(CsvUtils.getField(cols, valueIndex), values);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to read filter file " + path, e);
        }
    }

    private int firstMatchingHeaderIndex(String[] headers, Set<String> normalizedHeaders) {
        for (int i = 0; i < headers.length; i++) {
            String normalized = CsvUtils.clean(headers[i]).toLowerCase(Locale.ROOT);
            if (normalizedHeaders.contains(normalized)) {
                return i;
            }
        }
        return -1;
    }

    private void addValue(String rawValue, MutableSet<String> values) {
        String cleaned = CsvUtils.clean(rawValue);
        if (cleaned != null && !cleaned.isEmpty()) {
            values.add(cleaned);
        }
    }
}

