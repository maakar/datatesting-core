package org.qamethod.datatesting.core.pipeline;

/**
 * Listener interface for pipeline lifecycle events.
 * Useful for logging, monitoring, progress reporting, and custom hooks.
 */
public interface PipelineListener {

    /**
     * Called when a pipeline stage starts.
     */
    default void onStageStart(PipelineStage stage) {}

    /**
     * Called when a pipeline stage completes successfully.
     *
     * @param stage      the completed stage
     * @param durationMs time taken in milliseconds
     */
    default void onStageComplete(PipelineStage stage, long durationMs) {}

    /**
     * Called when a pipeline stage fails.
     */
    default void onStageError(PipelineStage stage, Exception error) {}

    /**
     * Called periodically during long-running stages with progress info.
     *
     * @param stage           current stage
     * @param recordsProcessed number of records processed so far
     * @param message          human-readable progress message
     */
    default void onProgress(PipelineStage stage, long recordsProcessed, String message) {}
}

