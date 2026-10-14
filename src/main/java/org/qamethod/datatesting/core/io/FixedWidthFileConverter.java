package org.qamethod.datatesting.core.io;

import java.io.IOException;
import java.nio.file.Path;

/** Converts a fixed-width text file to CSV. */
public interface FixedWidthFileConverter {

    /**
     * Convert {@code inputFile} to CSV, writing to {@code outputCsv}.
     *
     * @return number of data rows written
     */
    long convert(Path inputFile, Path outputCsv) throws IOException;

    /** Human-readable name of the layout (for example, {@code "RECORDS_LAYOUT"}). */
    String layoutName();
}

