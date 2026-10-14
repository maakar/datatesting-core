package org.qamethod.datatesting.core.db;
import java.sql.SQLException;

public interface AnalysisStore {
    void initialize(boolean recreateTables) throws SQLException;
    void execute(String sql) throws SQLException;
    String getDbUrl();
}
