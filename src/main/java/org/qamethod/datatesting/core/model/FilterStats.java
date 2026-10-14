package org.qamethod.datatesting.core.model;

/**
 * Counts from a filtering stage.
 *
 * @param totalRecords       Total records before filtering
 * @param keptRecords        Records that passed all filters
 * @param filteredByCategory Breakdown of filtered records by filter name
 */
public record FilterStats(
        long totalRecords,
        long keptRecords,
        java.util.Map<String, Long> filteredByCategory
) {
    public long totalFiltered() {
        return totalRecords - keptRecords;
    }
}

