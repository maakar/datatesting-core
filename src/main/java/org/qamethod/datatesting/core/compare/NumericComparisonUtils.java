package org.qamethod.datatesting.core.compare;

/**
 * Generic numeric comparison helpers reused across domains.
 */
public final class NumericComparisonUtils {

    private NumericComparisonUtils() {}

    /**
     * Floating-point comparison within an absolute tolerance.
     */
    public static boolean valuesMatch(String value1, String value2, double tolerance) {
        if (value1 == null || value2 == null) return false;
        try {
            double v1 = Double.parseDouble(value1);
            double v2 = Double.parseDouble(value2);
            return Math.abs(v1 - v2) <= tolerance;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Drop the last {@code charsToTruncate} characters before comparing.
     */
    public static String truncateForComparison(String value, int charsToTruncate) {
        if (value == null || charsToTruncate <= 0 || value.length() <= charsToTruncate) {
            return value;
        }
        return value.substring(0, value.length() - charsToTruncate);
    }
}

