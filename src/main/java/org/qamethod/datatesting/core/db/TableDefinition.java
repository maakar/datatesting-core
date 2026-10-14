package org.qamethod.datatesting.core.db;
import java.util.List;

public record TableDefinition(String tableName, List<String> columnNames, List<String> columnTypes) {
    public String toCreateTableSql() {
        StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS \"").append(tableName).append("\" (");
        for (int i = 0; i < columnNames.size(); i++) { if (i > 0) sb.append(", "); sb.append("\"").append(columnNames.get(i)).append("\" ").append(columnTypes.get(i)); }
        sb.append(")"); return sb.toString();
    }
    public String toInsertSql() {
        StringBuilder sb = new StringBuilder("INSERT INTO \"").append(tableName).append("\" (");
        for (int i = 0; i < columnNames.size(); i++) { if (i > 0) sb.append(", "); sb.append("\"").append(columnNames.get(i)).append("\""); }
        sb.append(") VALUES ("); for (int i = 0; i < columnNames.size(); i++) { if (i > 0) sb.append(", "); sb.append("?"); }
        sb.append(")"); return sb.toString();
    }
}
