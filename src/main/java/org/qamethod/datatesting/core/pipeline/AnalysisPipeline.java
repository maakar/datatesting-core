package org.qamethod.datatesting.core.pipeline;

import org.qamethod.datatesting.core.db.AnalysisStore;
import org.qamethod.datatesting.core.report.ReportGenerator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Universal analysis pipeline that executes a configurable sequence of stages.
 * <p>
 * This is the main entry point for running a data testing analysis.
 * Domain modules configure the pipeline with their specific data sources,
 * filters, comparisons, enrichment, and report generators, then call
 * {@link #execute()}.
 *
 * <pre>
 * AnalysisPipeline pipeline = new AnalysisPipeline("Dataset Reconciliation")
 *     .store(sqliteStore)
 *     .addStage(PipelineStage.LOAD, () -&gt; { loadData(); })
 *     .addStage(PipelineStage.FILTER, () -&gt; { filterData(); })
 *     .addStage(PipelineStage.COMPARE, () -&gt; { compareData(); })
 *     .addStage(PipelineStage.ENRICH, () -&gt; { enrichMismatches(); })
 *     .addReportGenerator(textReport)
 *     .addReportGenerator(excelReport)
 *     .addListener(loggingListener);
 *
 * pipeline.execute();
 * </pre>
 */
public class AnalysisPipeline {

    private static final Logger logger = Logger.getLogger(AnalysisPipeline.class.getName());

    private final String name;
    private final List<StageEntry> stages = new ArrayList<>();
    private final List<ReportGenerator> reporters = new ArrayList<>();
    private final List<PipelineListener> listeners = new ArrayList<>();
    private final Map<String, Object> metadata = new LinkedHashMap<>();
    private AnalysisStore store;

    public AnalysisPipeline(String name) {
        this.name = name;
    }

    /** Set the analysis store (SQLite database). */
    public AnalysisPipeline store(AnalysisStore store) {
        this.store = store;
        return this;
    }

    /**
     * Add a processing stage.
     *
     * @param stage  the pipeline stage enum (for listeners and logging)
     * @param action the action to execute
     */
    public AnalysisPipeline addStage(PipelineStage stage, StageAction action) {
        stages.add(new StageEntry(stage, action));
        return this;
    }

    /** Add a report generator (executed after all stages). */
    public AnalysisPipeline addReportGenerator(ReportGenerator reporter) {
        reporters.add(reporter);
        return this;
    }

    /** Add a pipeline lifecycle listener. */
    public AnalysisPipeline addListener(PipelineListener listener) {
        listeners.add(listener);
        return this;
    }

    /** Store arbitrary metadata (accessible by stages and reporters). */
    public AnalysisPipeline putMetadata(String key, Object value) {
        metadata.put(key, value);
        return this;
    }

    /** Retrieve pipeline metadata. */
    public Map<String, Object> metadata() {
        return metadata;
    }

    /**
     * Execute the pipeline: run all stages, then generate reports.
     */
    public void execute() throws Exception {
        logger.info("=".repeat(80));
        logger.info("Pipeline: " + name);
        logger.info("=".repeat(80));

        long pipelineStart = System.currentTimeMillis();

        for (StageEntry entry : stages) {
            PipelineStage stage = entry.stage;
            logger.info(">>> Stage: " + stage.name() + " - " + stage.description());
            listeners.forEach(l -> l.onStageStart(stage));

            long stageStart = System.currentTimeMillis();
            try {
                entry.action.run();
                long duration = System.currentTimeMillis() - stageStart;
                logger.info("<<< Stage " + stage.name() + " completed in " + duration + " ms");
                listeners.forEach(l -> l.onStageComplete(stage, duration));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Stage " + stage.name() + " failed", e);
                listeners.forEach(l -> l.onStageError(stage, e));
                throw e;
            }
        }

        // Report generation
        if (!reporters.isEmpty()) {
            logger.info(">>> Generating reports...");
            listeners.forEach(l -> l.onStageStart(PipelineStage.REPORT));
            long reportStart = System.currentTimeMillis();

            for (ReportGenerator reporter : reporters) {
                try {
                    reporter.generate(store, metadata);
                    logger.info("Report generated: " + reporter.outputPath());
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Report generation failed: " + reporter.outputPath(), e);
                }
            }

            long reportDuration = System.currentTimeMillis() - reportStart;
            listeners.forEach(l -> l.onStageComplete(PipelineStage.REPORT, reportDuration));
        }

        long totalDuration = System.currentTimeMillis() - pipelineStart;
        logger.info("=".repeat(80));
        logger.info("Pipeline '" + name + "' completed in " + totalDuration + " ms");
        logger.info("=".repeat(80));
    }

    /**
     * Functional interface for a pipeline stage action.
     */
    @FunctionalInterface
    public interface StageAction {
        void run() throws Exception;
    }

    private record StageEntry(PipelineStage stage, StageAction action) {}
}

