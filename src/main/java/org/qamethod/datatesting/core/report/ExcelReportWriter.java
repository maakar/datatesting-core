package org.qamethod.datatesting.core.report;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.qamethod.datatesting.core.db.AnalysisStore;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Base Excel report writer with utility methods for generating structured
 * analysis workbooks.
 * <p>
 * Provides helpers for creating sheets, summary headers, data tables
 * from SQL queries, and styled cells. Domain modules extend this to add
 * their specific sheets and formatting.
 *
 * <pre>
 * ExcelReportWriter report = new ExcelReportWriter("report.xlsx", "My Analysis");
 * report.generate(store, metadata);
 * </pre>
 */
public class ExcelReportWriter implements ReportGenerator {

    private static final Logger logger = Logger.getLogger(ExcelReportWriter.class.getName());

    private final String outputFile;
    private final String title;

    public ExcelReportWriter(String outputFile, String title) {
        this.outputFile = outputFile;
        this.title = title;
    }

    @Override
    public void generate(AnalysisStore store, Map<String, Object> metadata) throws IOException {
        logger.info("Generating Excel report: " + outputFile);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            writeWorkbook(workbook, store, metadata);

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }
        }

        logger.info("Excel report complete: " + outputFile);
    }

    @Override
    public String outputPath() {
        return outputFile;
    }

    // =====================================================================
    //  Override point for domain modules
    // =====================================================================

    /**
     * Override to populate the workbook with custom sheets.
     * The default creates a Summary sheet with metadata.
     */
    protected void writeWorkbook(XSSFWorkbook workbook, AnalysisStore store,
                                  Map<String, Object> metadata) throws IOException {
        Sheet summary = workbook.createSheet("Summary");
        CellStyle headerStyle = createHeaderStyle(workbook);
        int rowIdx = 0;

        // Title
        Row titleRow = summary.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(headerStyle);
        summary.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        // Timestamp
        Row timeRow = summary.createRow(rowIdx++);
        timeRow.createCell(0).setCellValue("Generated:");
        timeRow.createCell(1).setCellValue(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        rowIdx++;

        // Metadata
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            Row r = summary.createRow(rowIdx++);
            r.createCell(0).setCellValue(entry.getKey());
            r.createCell(1).setCellValue(String.valueOf(entry.getValue()));
        }

        summary.autoSizeColumn(0);
        summary.autoSizeColumn(1);
    }

    // =====================================================================
    //  Utility methods for building Excel reports
    // =====================================================================

    /**
     * Write a SQL query result set as a sheet.
     *
     * @param workbook  the workbook
     * @param sheetName sheet name
     * @param store     the analysis store (for DB connection)
     * @param sql       the SELECT query
     * @return number of data rows written
     */
    protected int writeQuerySheet(XSSFWorkbook workbook, String sheetName,
                                   AnalysisStore store, String sql) throws IOException {
        Sheet sheet = workbook.createSheet(sheetName);
        CellStyle headerStyle = createHeaderStyle(workbook);
        int rowIdx = 0;

        try (Connection conn = DriverManager.getConnection(store.getDbUrl());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            // Header row
            Row headerRow = sheet.createRow(rowIdx++);
            for (int c = 1; c <= colCount; c++) {
                Cell cell = headerRow.createCell(c - 1);
                cell.setCellValue(meta.getColumnLabel(c));
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int dataRows = 0;
            while (rs.next()) {
                Row dataRow = sheet.createRow(rowIdx++);
                for (int c = 1; c <= colCount; c++) {
                    dataRow.createCell(c - 1).setCellValue(rs.getString(c));
                }
                dataRows++;
            }

            // Auto-size columns (up to 20)
            for (int c = 0; c < Math.min(colCount, 20); c++) {
                sheet.autoSizeColumn(c);
            }

            return dataRows;

        } catch (SQLException e) {
            logger.warning("Failed to write sheet '" + sheetName + "': " + e.getMessage());
            throw new IOException("Failed to write sheet " + sheetName, e);
        }
    }

    /**
     * Create a bold header cell style.
     */
    protected CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    /**
     * Get row count of a table via the store.
     */
    protected long tableCount(AnalysisStore store, String tableName) {
        try (Connection conn = DriverManager.getConnection(store.getDbUrl());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM \"" + tableName + "\"")) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }
}

