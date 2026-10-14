package org.qamethod.datatesting.core.report;

import org.junit.jupiter.api.Test;
import org.qamethod.datatesting.core.db.SqliteAnalysisStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TextReportWriterIT {

    @Test
    void generate_writes_title_metadata_and_footer() throws Exception {
        Path db = Files.createTempFile("text-report-", ".db");
        Path out = Files.createTempFile("text-report-", ".txt");
        db.toFile().deleteOnExit();
        out.toFile().deleteOnExit();

        SqliteAnalysisStore store = new SqliteAnalysisStore("jdbc:sqlite:" + db);
        Map<String, Object> metadata = Map.of("runId", "42", "env", "test");

        TextReportWriter writer = new TextReportWriter(out.toString(), "Analysis Report");
        writer.generate(store, metadata);

        String content = Files.readString(out);
        assertThat(content).contains("Analysis Report");
        assertThat(content).contains("Pipeline Metadata:");
        assertThat(content).contains("runId: 42");
        assertThat(content).contains("env: test");
        assertThat(content).contains("End of Report");
    }
}

