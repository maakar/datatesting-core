package org.qamethod.datatesting.core.report;

import org.qamethod.datatesting.core.db.AnalysisStore;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Base text report writer with utility methods for generating structured
 * analysis reports.
 * <p>
 * Provides helpers for printing headers, sections, count tables, and
 * querying the analysis database. Domain modules extend this to add
 * their specific report sections.
 *
 * <pre>
 * TextReportWriter report = new TextReportWriter("analysis_report.txt", "My Analysis");
 * report.generate(store, metadata);
 * </pre>
 */
public class TextReportWriter implements ReportGenerator {

    private static final Logger logger = Logger.getLogger(TextReportWriter.class.getName());

    private final String outputFile;
    private final String title;

    public TextReportWriter(String outputFile, String title) {
        this.outputFile = outputFile;
        this.title = title;
    }

    @Override
    public void generate(AnalysisStore store, Map<String, Object> metadata) throws IOException {
        logger.info("Generating text report: " + outputFile);

        try (PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)))) {
            writeHeader(w, metadata);
            writeBody(w, store, metadata);
            writeFooter(w);
        }

        logger.info("Text report complete: " + outputFile);
    }

    @Override
    public String outputPath() {
        return outputFile;
    }

    // =====================================================================
    //  Override points for domain modules
    // =====================================================================

    /**
     * Override to write custom report body sections.
     * The default implementation writes table row counts.
     */
    protected void writeBody(PrintWriter w, AnalysisStore store, Map<String, Object> metadata) throws IOException {
        // Default: list metadata
        w.println("Pipeline Metadata:");
        metadata.forEach((k, v) -> w.println("  " + k + ": " + v));
        w.println();
    }

    // =====================================================================
    //  Utility methods for report writing
    // =====================================================================

    protected void writeHeader(PrintWriter w, Map<String, Object> metadata) {
        w.println("=".repeat(80));
        w.println(title);
        w.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        w.println("=".repeat(80));
        w.println();
    }

    protected void writeFooter(PrintWriter w) {
        w.println();
        w.println("=".repeat(80));
        w.println("End of Report");
        w.println("=".repeat(80));
    }

    /**
     * Write a section header.
     */
    protected void section(PrintWriter w, String sectionTitle) {
        w.println("-".repeat(80));
        w.println(sectionTitle);
        w.println("-".repeat(80));
    }

    /**
     * Write a labeled count line.
     */
    protected void countLine(PrintWriter w, String label, long count) {
        w.printf("  %-50s %,15d%n", label, count);
    }

    /**
     * Write a labeled count line with percentage.
     */
    protected void countLine(PrintWriter w, String label, long count, long total) {
        double pct = total > 0 ? (100.0 * count / total) : 0;
        w.printf("  %-50s %,15d  (%6.2f%%)%n", label, count, pct);
    }

    /**
     * Get row count of a table via the store's DB URL.
     */
    protected long tableCount(AnalysisStore store, String tableName) {
        try (Connection conn = DriverManager.getConnection(store.getDbUrl());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM \"" + tableName + "\"")) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            return 0; // table may not exist
        }
    }
}

