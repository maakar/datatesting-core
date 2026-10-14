package org.qamethod.datatesting.core.report;

import org.qamethod.datatesting.core.db.AnalysisStore;

import java.io.IOException;
import java.util.Map;

/**
 * Universal interface for generating analysis reports.
 * <p>
 * Core provides: {@code TextReportGenerator}, {@code ExcelReportGenerator}.
 * Domain modules can extend or customize report generation via {@code ReportSection}s.
 */
public interface ReportGenerator {

    /**
     * Generate a report from the analysis results stored in the database.
     *
     * @param store     the analysis store containing all results
     * @param metadata  pipeline metadata (counts, timestamps, configuration info)
     * @throws IOException if report file cannot be written
     */
    void generate(AnalysisStore store, Map<String, Object> metadata) throws IOException;

    /**
     * The output file path.
     */
    String outputPath();
}

