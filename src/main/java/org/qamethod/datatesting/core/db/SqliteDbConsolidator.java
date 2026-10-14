package org.qamethod.datatesting.core.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Merges multiple SQLite analysis databases into one consolidated database.
 */
public class SqliteDbConsolidator {

    private static final Logger logger = Logger.getLogger(SqliteDbConsolidator.class.getName());

    public void consolidate(Path outputDb, List<Path> inputDbs, Function<Path, String> dateFn)
            throws SQLException, IOException {
        consolidate(outputDb, inputDbs, dateFn, table -> true);
    }

    public void consolidate(Path outputDb,
                            List<Path> inputDbs,
                            Function<Path, String> dateFn,
                            Predicate<String> tablePredicate)
            throws SQLException, IOException {
        if (inputDbs.size() < 2) {
            throw new IllegalArgumentException("At least 2 input databases are required for consolidation.");
        }
        for (Path db : inputDbs) {
            if (!Files.exists(db)) {
                throw new IOException("Input database not found: " + db.toAbsolutePath());
            }
        }

        Files.deleteIfExists(outputDb);

        try (Connection out = connect(outputDb)) {
            out.setAutoCommit(false);
            createConsolidationLog(out);

            List<String> tablesToMerge;
            try (Connection first = connect(inputDbs.get(0))) {
                tablesToMerge = getTableNames(first, tablePredicate);
                createConsolidatedTables(out, first, tablesToMerge);
            }

            for (Path inputDb : inputDbs) {
                String analysisDate = dateFn.apply(inputDb);
                logger.info("Merging " + inputDb.getFileName() + " (analysis_date=" + analysisDate + ")...");

                try (Connection src = connect(inputDb)) {
                    for (String table : tablesToMerge) {
                        if (tableExists(src, table)) {
                            mergeTable(src, out, table, analysisDate);
                        } else {
                            logger.warning("Table '" + table + "' not found in " + inputDb.getFileName() + ", skipping.");
                        }
                    }
                }

                logSourceDb(out, inputDb, analysisDate);
                out.commit();
            }
        }
    }

    private void createConsolidationLog(Connection out) throws SQLException {
        try (Statement s = out.createStatement()) {
            s.execute("""
                    CREATE TABLE IF NOT EXISTS consolidation_log (
                        id              INTEGER PRIMARY KEY AUTOINCREMENT,
                        source_file     TEXT,
                        analysis_date   TEXT,
                        merged_at       TEXT
                    )""");
        }
    }

    private void createConsolidatedTables(Connection out, Connection src, List<String> tables) throws SQLException {
        try (Statement s = out.createStatement()) {
            for (String table : tables) {
                String colDef = getColumnDefinitions(src, table);
                String ddl = "CREATE TABLE IF NOT EXISTS " + table + " (" + colDef + ", analysis_date TEXT)";
                s.execute(ddl);
            }
        }
        out.commit();
    }

    private void mergeTable(Connection src, Connection out, String table, String analysisDate) throws SQLException {
        List<String> cols = getColumnNames(src, table);
        if (cols.isEmpty()) return;

        String selectSql = "SELECT " + quotedList(cols) + " FROM " + table;
        String insertSql = "INSERT INTO " + table + " (" + quotedList(cols) + ", analysis_date) VALUES ("
                + placeholders(cols.size()) + ", ?)";

        long count = 0;
        try (PreparedStatement ps = out.prepareStatement(insertSql);
             Statement ss = src.createStatement();
             ResultSet rs = ss.executeQuery(selectSql)) {

            while (rs.next()) {
                for (int i = 0; i < cols.size(); i++) {
                    ps.setString(i + 1, rs.getString(cols.get(i)));
                }
                ps.setString(cols.size() + 1, analysisDate);
                ps.addBatch();
                count++;

                if (count % 50_000 == 0) {
                    ps.executeBatch();
                    out.commit();
                    logger.info("  " + table + ": inserted " + count + " rows...");
                }
            }

            ps.executeBatch();
            out.commit();
        }

        logger.info("  " + table + ": " + count + " rows merged.");
    }

    private void logSourceDb(Connection out, Path inputDb, String analysisDate) throws SQLException {
        String mergedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        try (PreparedStatement ps = out.prepareStatement(
                "INSERT INTO consolidation_log (source_file, analysis_date, merged_at) VALUES (?, ?, ?)")) {
            ps.setString(1, inputDb.toAbsolutePath().toString());
            ps.setString(2, analysisDate);
            ps.setString(3, mergedAt);
            ps.executeUpdate();
        }
    }

    private static Connection connect(Path dbPath) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
    }

    private static boolean tableExists(Connection conn, String table) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static String getColumnDefinitions(Connection conn, String table) throws SQLException {
        List<String> defs = new ArrayList<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                String name = rs.getString("name");
                String type = rs.getString("type");
                if (type == null || type.isBlank()) type = "TEXT";
                defs.add("\"" + name + "\" " + type);
            }
        }
        return String.join(", ", defs);
    }

    private static List<String> getColumnNames(Connection conn, String table) throws SQLException {
        List<String> names = new ArrayList<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        }
        return names;
    }

    private static String quotedList(List<String> cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append('"').append(cols.get(i)).append('"');
        }
        return sb.toString();
    }

    private static String placeholders(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(", ");
            sb.append('?');
        }
        return sb.toString();
    }

    private static List<String> getTableNames(Connection conn, Predicate<String> predicate) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String table = rs.getString("name");
                if ("consolidation_log".equalsIgnoreCase(table)) continue;
                if (predicate.test(table)) {
                    tables.add(table);
                }
            }
        }
        return tables;
    }
}

