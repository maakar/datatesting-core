package org.qamethod.datatesting.core.filter;

import org.qamethod.datatesting.core.model.Record;

/**
 * Filters records where any configured value column is less than or equal to a given threshold.
 */
public class LowValueFilter implements RecordFilter {

    private final String[] valueColumns;
    private final long threshold;

    public LowValueFilter(long threshold, String... valueColumns) {
        this.threshold = threshold;
        this.valueColumns = valueColumns;
    }

    @Override
    public boolean test(Record record) {
        for (String col : valueColumns) {
            String value = record.get(col);
            if (value != null && !value.trim().isEmpty()) {
                try {
                    long parsed = Long.parseLong(value.trim());
                    if (parsed <= threshold) {
                        return false;
                    }
                } catch (NumberFormatException ignored) {
                    // Keep row when numeric parsing fails.
                }
            }
        }
        return true;
    }

    @Override
    public String name() {
        return "LowValueFilter(threshold=" + threshold + ", columns=" + String.join(",", valueColumns) + ")";
    }
}
