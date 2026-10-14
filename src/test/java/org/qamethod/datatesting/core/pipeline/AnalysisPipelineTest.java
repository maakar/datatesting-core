package org.qamethod.datatesting.core.pipeline;

import org.junit.jupiter.api.Test;
import org.qamethod.datatesting.core.db.AnalysisStore;
import org.qamethod.datatesting.core.report.ReportGenerator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalysisPipelineTest {

    @Test
    void execute_runs_stages_in_order_and_generates_reports() throws Exception {
        List<String> actionOrder = new ArrayList<>();
        List<String> started = new ArrayList<>();
        List<String> completed = new ArrayList<>();

        ReportGenerator reportGenerator = new ReportGenerator() {
            @Override
            public void generate(AnalysisStore store, Map<String, Object> metadata) {
                actionOrder.add("REPORTER");
                assertThat(store.getDbUrl()).isEqualTo("jdbc:sqlite:test.db");
                assertThat(metadata).containsEntry("runId", "42");
            }

            @Override
            public String outputPath() {
                return "report.txt";
            }
        };

        PipelineListener listener = new PipelineListener() {
            @Override
            public void onStageStart(PipelineStage stage) {
                started.add(stage.name());
            }

            @Override
            public void onStageComplete(PipelineStage stage, long durationMs) {
                completed.add(stage.name());
            }
        };

        new AnalysisPipeline("test-pipeline")
                .store(new NoOpStore("jdbc:sqlite:test.db"))
                .putMetadata("runId", "42")
                .addStage(PipelineStage.LOAD, () -> actionOrder.add("LOAD"))
                .addStage(PipelineStage.FILTER, () -> actionOrder.add("FILTER"))
                .addReportGenerator(reportGenerator)
                .addListener(listener)
                .execute();

        assertThat(actionOrder).containsExactly("LOAD", "FILTER", "REPORTER");
        assertThat(started).containsExactly("LOAD", "FILTER", "REPORT");
        assertThat(completed).containsExactly("LOAD", "FILTER", "REPORT");
    }

    @Test
    void execute_propagates_stage_errors_and_notifies_listener() {
        List<String> started = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        PipelineListener listener = new PipelineListener() {
            @Override
            public void onStageStart(PipelineStage stage) {
                started.add(stage.name());
            }

            @Override
            public void onStageError(PipelineStage stage, Exception error) {
                errors.add(stage.name() + ":" + error.getMessage());
            }
        };

        AnalysisPipeline pipeline = new AnalysisPipeline("failure-pipeline")
                .store(new NoOpStore("jdbc:sqlite:test.db"))
                .addStage(PipelineStage.LOAD, () -> {})
                .addStage(PipelineStage.COMPARE, () -> {
                    throw new IllegalStateException("boom");
                })
                .addListener(listener);

        assertThatThrownBy(pipeline::execute)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boom");

        assertThat(started).containsExactly("LOAD", "COMPARE");
        assertThat(errors).containsExactly("COMPARE:boom");
    }

    private static class NoOpStore implements AnalysisStore {
        private final String dbUrl;

        private NoOpStore(String dbUrl) {
            this.dbUrl = dbUrl;
        }

        @Override
        public void initialize(boolean recreateTables) throws SQLException {
            // no-op for unit test
        }

        @Override
        public void execute(String sql) throws SQLException {
            // no-op for unit test
        }

        @Override
        public String getDbUrl() {
            return dbUrl;
        }
    }
}


