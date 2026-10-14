package org.qamethod.datatesting.core.io;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

/**
 * CSV parsing utilities shared across analysis modules.
 */
public final class CsvUtils {

    private CsvUtils() {}

    /**
     * Parse a single CSV line respecting quoted fields (RFC 4180).
     */
    public static String[] parseLine(String line) {
        if (line == null) return new String[0];
        MutableList<String> fields = Lists.mutable.empty();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }

    /**
     * Get a field from a parsed CSV row, returning null for out-of-bounds indices.
     */
    public static String getField(String[] cols, int idx) {
        if (idx < 0 || idx >= cols.length) return null;
        return cols[idx];
    }

    /**
     * Clean a CSV field value: remove quotes and trim whitespace.
     */
    public static String clean(String s) {
        if (s == null) return null;
        return s.replace("\"", "").trim();
    }

    /**
     * Find a header column index by name (case-insensitive, quote-aware).
     */
    public static int findHeader(String[] headers, String name) {
        for (int i = 0; i < headers.length; i++) {
            if (name.equalsIgnoreCase(clean(headers[i]))) return i;
        }
        return -1;
    }

    /**
     * Strip BOM (Byte Order Mark) from the beginning of a string if present.
     */
    public static String stripBom(String s) {
        if (s != null && s.startsWith("\uFEFF")) {
            return s.substring(1);
        }
        return s;
    }
}

