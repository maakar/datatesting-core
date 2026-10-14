package org.qamethod.datatesting.core.compare;

import org.eclipse.collections.api.list.ListIterable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Sort-merge comparison engine for two sorted result sets.
 * <p>
 * Compares rows from a source and target table (both in the same SQLite database)
 * by a composite key, classifying each row as:
 * <ul>
 *   <li>Perfect match — all columns identical</li>
 *   <li>Source only (missing from target)</li>
 *   <li>Target only (missing from source)</li>
 *   <li>Mismatch — keys match but data columns differ</li>
 * </ul>
 * <p>
 * Results are written directly to output tables in the same database.
 * This is a concrete, reusable engine — domain modules configure it
 * with their column lists, comparison keys, and optional custom matchers.
 *
 * <pre>
 * SortMergeCompare compare = new SortMergeCompare(dbUrl)
 *     .sourceTable("source_data")
 *     .targetTable("target_data")
 *     .keyColumns(Lists.mutable.of("ID"))
 *     .allColumns(Lists.mutable.of("ID", "NAME", "VALUE"));
 *
 * SortMergeCompare.Stats stats = compare.execute();
 * // stats.perfectMatches(), stats.sourceOnly(), stats.targetOnly(), stats.mismatches()
 * </pre>
 */
public class SortMergeCompare {

    private static final Logger logger = Logger.getLogger(SortMergeCompare.class.getName());

    private final String dbUrl;
    private String sourceTable;
    private String targetTable;
    private ListIterable<String> keyColumns;
    private ListIterable<String> allColumns;
    private String sourceOnlyTable = "missing_in_target";
    private String targetOnlyTable = "missing_in_source";
    private String mismatchTable = "mismatches";

    // Optional custom comparators for specific columns
    private final List<ColumnMatcher> customMatchers = new ArrayList<>();

    public SortMergeCompare(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public SortMergeCompare sourceTable(String t) { this.sourceTable = t; return this; }
    public SortMergeCompare targetTable(String t) { this.targetTable = t; return this; }
    public SortMergeCompare keyColumns(ListIterable<String> k) { this.keyColumns = k; return this; }
    public SortMergeCompare allColumns(ListIterable<String> c) { this.allColumns = c; return this; }
    public SortMergeCompare sourceOnlyTable(String t) { this.sourceOnlyTable = t; return this; }
    public SortMergeCompare targetOnlyTable(String t) { this.targetOnlyTable = t; return this; }
    public SortMergeCompare mismatchTable(String t) { this.mismatchTable = t; return this; }

    /**
     * Register a custom matcher for a specific column.
     * If not set, the default is exact string equality.
     */
    public SortMergeCompare addCustomMatcher(String column, FieldComparator comparator) {
        customMatchers.add(new ColumnMatcher(column, comparator));
        return this;
    }

    /**
     * Execute the sort-merge comparison.
     *
     * @return statistics with counts for each category
     */
    public Stats execute() throws SQLException {
        long perfectMatches = 0, sourceOnly = 0, targetOnly = 0, mismatches = 0;

        String orderBy = keyColumns.collect(c -> "\"" + c + "\"").makeString(", ");
        String sourceQuery = "SELECT * FROM \"" + sourceTable + "\" ORDER BY " + orderBy;
        String targetQuery = "SELECT * FROM \"" + targetTable + "\" ORDER BY " + orderBy;

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);

            // Create output tables
            createOutputTable(conn, sourceOnlyTable, false);
            createOutputTable(conn, targetOnlyTable, false);
            createOutputTable(conn, mismatchTable, true);

            String insertSourceOnly = buildInsertSql(sourceOnlyTable, false);
            String insertTargetOnly = buildInsertSql(targetOnlyTable, false);
            String insertMismatch   = buildInsertSql(mismatchTable, true);

            try (Statement srcStmt = conn.createStatement();
                 Statement tgtStmt = conn.createStatement();
                 ResultSet srcRs = srcStmt.executeQuery(sourceQuery);
                 ResultSet tgtRs = tgtStmt.executeQuery(targetQuery);
                 PreparedStatement psSrcOnly = conn.prepareStatement(insertSourceOnly);
                 PreparedStatement psTgtOnly = conn.prepareStatement(insertTargetOnly);
                 PreparedStatement psMismatch = conn.prepareStatement(insertMismatch)) {

                boolean hasSrc = srcRs.next();
                boolean hasTgt = tgtRs.next();
                int batchCount = 0;

                while (hasSrc && hasTgt) {
                    int cmp = compareByKey(srcRs, tgtRs);

                    if (cmp < 0) {
                        // Source only
                        writeRow(psSrcOnly, srcRs, null, false);
                        sourceOnly++;
                        hasSrc = srcRs.next();
                    } else if (cmp > 0) {
                        // Target only
                        writeRow(psTgtOnly, tgtRs, null, false);
                        targetOnly++;
                        hasTgt = tgtRs.next();
                    } else {
                        // Keys match — check data columns
                        String diff = findDifferences(srcRs, tgtRs);
                        if (diff == null) {
                            perfectMatches++;
                        } else {
                            writeRow(psMismatch, srcRs, tgtRs, true);
                            mismatches++;
                        }
                        hasSrc = srcRs.next();
                        hasTgt = tgtRs.next();
                    }

                    batchCount++;
                    if (batchCount >= 5000) {
                        psSrcOnly.executeBatch(); psTgtOnly.executeBatch(); psMismatch.executeBatch();
                        conn.commit();
                        batchCount = 0;
                        logger.info("Comparison progress: matches=" + perfectMatches
                                + ", sourceOnly=" + sourceOnly + ", targetOnly=" + targetOnly
                                + ", mismatches=" + mismatches);
                    }
                }

                // Drain remaining source rows
                while (hasSrc) {
                    writeRow(psSrcOnly, srcRs, null, false);
                    sourceOnly++;
                    hasSrc = srcRs.next();
                }
                // Drain remaining target rows
                while (hasTgt) {
                    writeRow(psTgtOnly, tgtRs, null, false);
                    targetOnly++;
                    hasTgt = tgtRs.next();
                }

                psSrcOnly.executeBatch(); psTgtOnly.executeBatch(); psMismatch.executeBatch();
                conn.commit();
            }
        }

        logger.info("Sort-merge comparison complete: perfect=" + perfectMatches
                + ", sourceOnly=" + sourceOnly + ", targetOnly=" + targetOnly
                + ", mismatches=" + mismatches);
        return new Stats(perfectMatches, sourceOnly, targetOnly, mismatches);
    }

    // =====================================================================
    //  Private helpers
    // =====================================================================

    private int compareByKey(ResultSet src, ResultSet tgt) throws SQLException {
        for (int i = 0; i < keyColumns.size(); i++) {
            String col = keyColumns.get(i);
            String sv = normalize(src.getString(col));
            String tv = normalize(tgt.getString(col));
            int cmp = sv.compareTo(tv);
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    /**
     * Compare non-key columns. Returns null if all match, or a description of differences.
     */
    private String findDifferences(ResultSet src, ResultSet tgt) throws SQLException {
        StringBuilder diffs = null;
        for (int i = 0; i < allColumns.size(); i++) {
            String col = allColumns.get(i);
            if (keyColumns.contains(col)) continue; // skip key columns

            String sv = normalize(src.getString(col));
            String tv = normalize(tgt.getString(col));

            // Check custom matchers first
            boolean matched = false;
            for (ColumnMatcher cm : customMatchers) {
                if (cm.column.equals(col)) {
                    matched = cm.comparator.matches(sv, tv);
                    break;
                }
            }
            if (!matched && !sv.equals(tv)) {
                if (diffs == null) diffs = new StringBuilder();
                else diffs.append("; ");
                diffs.append(col).append(": [").append(sv).append("] vs [").append(tv).append("]");
            }
        }
        return diffs != null ? diffs.toString() : null;
    }

    private void writeRow(PreparedStatement ps, ResultSet src, ResultSet tgt, boolean isMismatch) throws SQLException {
        int idx = 1;
        // Source columns (or "source" side of mismatch)
        for (int i = 0; i < allColumns.size(); i++) {
            ps.setString(idx++, src.getString(allColumns.get(i)));
        }
        if (isMismatch && tgt != null) {
            // Comment column (difference description)
            ps.setString(idx++, findDifferences(src, tgt));
            // Target columns
            for (int i = 0; i < allColumns.size(); i++) {
                ps.setString(idx++, tgt.getString(allColumns.get(i)));
            }
        }
        ps.addBatch();
    }

    private void createOutputTable(Connection conn, String tableName, boolean isMismatch) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS \"" + tableName + "\"");
            StringBuilder ddl = new StringBuilder("CREATE TABLE \"").append(tableName).append("\" (");
            if (isMismatch) {
                // Source columns prefixed
                allColumns.forEach(col -> ddl.append("\"SRC_").append(col).append("\" TEXT, "));
                ddl.append("\"Comment\" TEXT, ");
                // Target columns prefixed
                allColumns.forEach(col -> ddl.append("\"TGT_").append(col).append("\" TEXT, "));
            } else {
                allColumns.forEach(col -> ddl.append("\"").append(col).append("\" TEXT, "));
            }
            ddl.setLength(ddl.length() - 2); // remove trailing ", "
            ddl.append(")");
            stmt.execute(ddl.toString());
        }
    }

    private String buildInsertSql(String tableName, boolean isMismatch) {
        int paramCount;
        if (isMismatch) {
            paramCount = allColumns.size() * 2 + 1; // src + comment + tgt
        } else {
            paramCount = allColumns.size();
        }
        StringBuilder sb = new StringBuilder("INSERT INTO \"").append(tableName).append("\" VALUES (");
        for (int i = 0; i < paramCount; i++) {
            if (i > 0) sb.append(",");
            sb.append("?");
        }
        sb.append(")");
        return sb.toString();
    }

    private static String normalize(String s) {
        return s != null ? s.trim() : "";
    }

    // =====================================================================
    //  Results
    // =====================================================================

    /**
     * Comparison statistics.
     */
    public record Stats(long perfectMatches, long sourceOnly, long targetOnly, long mismatches) {
        public long totalRecords() {
            return perfectMatches + sourceOnly + targetOnly + mismatches;
        }
    }

    private record ColumnMatcher(String column, FieldComparator comparator) {}
}

