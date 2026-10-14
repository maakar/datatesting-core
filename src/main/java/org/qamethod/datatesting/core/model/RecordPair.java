package org.qamethod.datatesting.core.model;

/**
 * A matched pair of records from source and target datasets.
 * Used during comparison to determine match/mismatch status.
 */
public record RecordPair(Record source, Record target) {

    /**
     * Create a pair representing a record only in the source (missing from target).
     */
    public static RecordPair sourceOnly(Record source) {
        return new RecordPair(source, null);
    }

    /**
     * Create a pair representing a record only in the target (missing from source).
     */
    public static RecordPair targetOnly(Record target) {
        return new RecordPair(null, target);
    }

    public boolean isMatched() {
        return source != null && target != null;
    }

    public boolean isSourceOnly() {
        return source != null && target == null;
    }

    public boolean isTargetOnly() {
        return source == null && target != null;
    }
}

