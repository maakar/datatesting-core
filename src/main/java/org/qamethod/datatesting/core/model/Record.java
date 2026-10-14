package org.qamethod.datatesting.core.model;

import java.util.Map;

/**
 * Represents a single data record from any tabular source (CSV row, DB row, etc.).
 * This is the universal unit of data flowing through the pipeline.
 */
public class Record {
    private final Map<String, String> fields;
    private final String sourceName; // e.g., "Source", "Target", or legacy system name

    public Record(Map<String, String> fields, String sourceName) {
        this.fields = Map.copyOf(fields);
        this.sourceName = sourceName;
    }

    public String get(String columnName) {
        return fields.get(columnName);
    }

    public boolean hasField(String columnName) {
        return fields.containsKey(columnName);
    }

    public Map<String, String> allFields() {
        return fields;
    }

    public String sourceName() {
        return sourceName;
    }

    @Override
    public String toString() {
        return "Record{source=" + sourceName + ", fields=" + fields + "}";
    }
}

