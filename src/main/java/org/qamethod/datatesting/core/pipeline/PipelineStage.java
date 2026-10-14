package org.qamethod.datatesting.core.pipeline;

/**
 * The stages of a data testing pipeline.
 */
public enum PipelineStage {
    LOAD("Loading data sources"),
    FILTER("Applying filters"),
    COMPARE("Comparing records"),
    CLASSIFY("Classifying and splitting results"),
    ENRICH("Enriching mismatches with reference data"),
    REPORT("Generating reports");

    private final String description;

    PipelineStage(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}

