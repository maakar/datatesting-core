package org.qamethod.datatesting.core.report;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.qamethod.datatesting.core.db.AnalysisStore;
import org.qamethod.datatesting.core.db.SqliteAnalysisStore;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelReportWriterIT {

    @Test
    void generate_creates_summary_sheet_with_metadata() throws Exception {
        Path db = Files.createTempFile("excel-report-", ".db");
        Path out = Files.createTempFile("excel-report-", ".xlsx");
        db.toFile().deleteOnExit();
        out.toFile().deleteOnExit();

        SqliteAnalysisStore store = new SqliteAnalysisStore("jdbc:sqlite:" + db);
        Map<String, Object> metadata = Map.of("runId", "42", "env", "test");

        ExcelReportWriter writer = new ExcelReportWriter(out.toString(), "Excel Analysis");
        writer.generate(store, metadata);

        try (FileInputStream fis = new FileInputStream(out.toFile()); XSSFWorkbook wb = new XSSFWorkbook(fis)) {
            Sheet summary = wb.getSheet("Summary");
            assertThat(summary).isNotNull();
            assertThat(summary.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Excel Analysis");

            boolean foundRunId = false;
            for (int i = 0; i <= summary.getLastRowNum(); i++) {
                if (summary.getRow(i) == null || summary.getRow(i).getCell(0) == null) continue;
                if ("runId".equals(summary.getRow(i).getCell(0).getStringCellValue())) {
                    foundRunId = true;
                    assertThat(summary.getRow(i).getCell(1).getStringCellValue()).isEqualTo("42");
                }
            }
            assertThat(foundRunId).isTrue();
        }
    }

    @Test
    void generate_can_include_query_sheet_via_subclass() throws Exception {
        Path db = Files.createTempFile("excel-query-", ".db");
        Path out = Files.createTempFile("excel-query-", ".xlsx");
        db.toFile().deleteOnExit();
        out.toFile().deleteOnExit();

        SqliteAnalysisStore store = new SqliteAnalysisStore("jdbc:sqlite:" + db);
        store.execute("CREATE TABLE items (ID TEXT, VALUE TEXT)");
        store.execute("INSERT INTO items VALUES ('1','A')");
        store.execute("INSERT INTO items VALUES ('2','B')");

        ExcelReportWriter writer = new QuerySheetWriter(out.toString(), "Excel Analysis");
        writer.generate(store, Map.of("runId", "42"));

        try (FileInputStream fis = new FileInputStream(out.toFile()); XSSFWorkbook wb = new XSSFWorkbook(fis)) {
            Sheet data = wb.getSheet("Items");
            assertThat(data).isNotNull();
            assertThat(data.getRow(0).getCell(0).getStringCellValue()).isEqualTo("ID");
            assertThat(data.getRow(1).getCell(0).getStringCellValue()).isEqualTo("1");
            assertThat(data.getRow(2).getCell(0).getStringCellValue()).isEqualTo("2");
        }
    }

    private static class QuerySheetWriter extends ExcelReportWriter {
        QuerySheetWriter(String outputFile, String title) {
            super(outputFile, title);
        }

        @Override
        protected void writeWorkbook(XSSFWorkbook workbook, AnalysisStore store, Map<String, Object> metadata) throws java.io.IOException {
            super.writeWorkbook(workbook, store, metadata);
            writeQuerySheet(workbook, "Items", store, "SELECT ID, VALUE FROM items ORDER BY ID");
        }
    }
}

