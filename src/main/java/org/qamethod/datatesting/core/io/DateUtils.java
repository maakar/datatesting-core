package org.qamethod.datatesting.core.io;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Logger;

/**
 * Shared date parsing utilities.
 */
public final class DateUtils {

    private static final Logger logger = Logger.getLogger(DateUtils.class.getName());

    public static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateUtils() {}

    /**
     * Parse a date string, returning null for blank/null/placeholder values.
     */
    public static LocalDate parse(String dateString, DateTimeFormatter formatter) {
        if (dateString == null || dateString.isBlank()
                || "NULL".equalsIgnoreCase(dateString)
                || "null".equalsIgnoreCase(dateString)
                || "00000000".equals(dateString.trim())
                || "0".equals(dateString.trim())) {
            return null;
        }
        try {
            return LocalDate.parse(dateString.trim(), formatter);
        } catch (DateTimeParseException e) {
            logger.warning("Could not parse date '" + dateString + "' with formatter " + formatter);
            return null;
        }
    }

    /**
     * Parse YYYYMMDD format, returning null for invalid/blank values.
     */
    public static LocalDate parseYYYYMMDD(String s) {
        return parse(s, YYYYMMDD);
    }

    /**
     * Parse yyyy-MM-dd format, truncating any timestamp suffix.
     */
    public static LocalDate parseIsoDate(String s) {
        if (s == null || s.isBlank() || "NULL".equalsIgnoreCase(s) || "0".equals(s)) {
            return null;
        }
        String trimmed = s.trim();
        if (trimmed.length() >= 10) {
            trimmed = trimmed.substring(0, 10);
        }
        return parse(trimmed, YYYY_MM_DD);
    }
}

