package org.qamethod.datatesting.core.filter;

import org.qamethod.datatesting.core.model.Record;

import java.util.Set;

/**
 * Keeps records where a named column's value is in a specified allow-list.
 */
public class AllowlistFilter implements RecordFilter {

    private final String columnName;
    private final Set<String> allowedValues;

    public AllowlistFilter(String columnName, Set<String> allowedValues) {
        this.columnName = columnName;
        this.allowedValues = allowedValues;
    }

    @Override
    public boolean test(Record record) {
        String value = record.get(columnName);
        return value != null && allowedValues.contains(value);
    }

    @Override
    public String name() {
        return "AllowlistFilter(" + columnName + " in " + allowedValues + ")";
    }
}

