package org.qamethod.datatesting.core.filter;

import io.github.vmzakharov.ecdataframe.dataframe.DataFrame;
import org.qamethod.datatesting.core.model.FilterStats;
import org.qamethod.datatesting.core.model.Record;

import java.util.*;
import java.util.logging.Logger;

/**
 * Applies a chain of {@link RecordFilter}s to a DataFrame, producing a filtered
 * DataFrame and detailed statistics.
 * <p>
 * Each filter in the chain is applied in order. Rows rejected by earlier filters
 * are not passed to later filters. Side-output DataFrames (e.g., "non-merch" rows)
 * can be captured via callbacks.
 *
 * <pre>
 * DataFrameFilterChain chain = new DataFrameFilterChain()
 *     .add("exclusion_filter", exclusionFilter)
 *     .add("status_filter", statusFilter)
 *     .add("low_value", lowValueFilter);
 *
 * DataFrameFilterChain.Result result = chain.apply(df, columns, "Source");
 * DataFrame filtered = result.kept();
 * FilterStats stats = result.stats();
 * </pre>
 */
public class DataFrameFilterChain {

    private static final Logger logger = Logger.getLogger(DataFrameFilterChain.class.getName());

    private final List<NamedFilter> filters = new ArrayList<>();

    /**
     * Add a filter to the chain.
     *
     * @param name   human-readable name (used in stats and logging)
     * @param filter the filter predicate
     * @return this chain (for fluent builder pattern)
     */
    public DataFrameFilterChain add(String name, RecordFilter filter) {
        filters.add(new NamedFilter(name, filter));
        return this;
    }

    /**
     * Apply all filters to a DataFrame.
     *
     * @param df         the input DataFrame
     * @param columns    column names to extract for each Record
     * @param sourceName the source label (e.g., "Source", "Target", or legacy system name)
     * @return result containing the kept DataFrame and per-filter statistics
     */
    public Result apply(DataFrame df, List<String> columns, String sourceName) {
        long totalRows = df.rowCount();
        Map<String, Long> filteredByCategory = new LinkedHashMap<>();
        Map<String, List<Map<String, String>>> sideOutputs = new LinkedHashMap<>();

        // Track which rows survive: true = kept
        boolean[] kept = new boolean[df.rowCount()];
        Arrays.fill(kept, true);

        for (NamedFilter nf : filters) {
            long filteredCount = 0;
            List<Map<String, String>> sideOutput = new ArrayList<>();

            for (int row = 0; row < df.rowCount(); row++) {
                if (!kept[row]) continue;  // already filtered by earlier stage

                Map<String, String> fields = new LinkedHashMap<>();
                for (String col : columns) {
                    fields.put(col, df.getString(col, row));
                }
                Record record = new Record(fields, sourceName);

                if (!nf.filter.test(record)) {
                    kept[row] = false;
                    filteredCount++;
                    sideOutput.add(fields);
                }
            }

            filteredByCategory.put(nf.name, filteredCount);
            if (!sideOutput.isEmpty()) {
                sideOutputs.put(nf.name, sideOutput);
            }
            logger.info(sourceName + " filter [" + nf.name + "]: removed " + filteredCount + " rows");
        }

        // Build the kept DataFrame
        long keptCount = 0;
        for (boolean b : kept) if (b) keptCount++;

        FilterStats stats = new FilterStats(totalRows, keptCount, filteredByCategory);
        return new Result(df, kept, stats, sideOutputs);
    }

    /**
     * Result of applying the filter chain.
     */
    public static class Result {
        private final DataFrame originalDf;
        private final boolean[] keptMask;
        private final FilterStats stats;
        private final Map<String, List<Map<String, String>>> sideOutputs;

        Result(DataFrame originalDf, boolean[] keptMask, FilterStats stats,
               Map<String, List<Map<String, String>>> sideOutputs) {
            this.originalDf = originalDf;
            this.keptMask = keptMask;
            this.stats = stats;
            this.sideOutputs = sideOutputs;
        }

        /** Filter statistics with per-category breakdown. */
        public FilterStats stats() { return stats; }

        /** The boolean mask — true for kept rows, false for filtered. */
        public boolean[] keptMask() { return keptMask; }

        /**
         * Side output rows captured by a specific filter (by filter name).
         * Useful for writing filtered rows to separate tables.
         */
        public List<Map<String, String>> sideOutput(String filterName) {
            return sideOutputs.getOrDefault(filterName, List.of());
        }

        /** All side outputs by filter name. */
        public Map<String, List<Map<String, String>>> allSideOutputs() {
            return Collections.unmodifiableMap(sideOutputs);
        }
    }

    private record NamedFilter(String name, RecordFilter filter) {}
}

