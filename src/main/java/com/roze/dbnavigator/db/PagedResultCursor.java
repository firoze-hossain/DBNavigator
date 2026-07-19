package com.roze.dbnavigator.db;

import com.roze.dbnavigator.model.ConnectionProfile;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * A query's live, forward-only JDBC cursor, kept open across pager clicks so
 * paging through console results never re-runs the query (which would be
 * unsafe without a stable ORDER BY, since a plain SELECT's row order across
 * separate executions isn't guaranteed) and never needs a separate COUNT(*).
 *
 * Rows already read are cached in memory, so paging backward ("Previous",
 * "First") is always instant; paging forward past what's cached reads a new
 * batch from the still-open ResultSet. A safety cap bounds total memory use
 * for pathologically large result sets.
 */
public class PagedResultCursor implements AutoCloseable {

    private static final int SAFETY_CAP = 50_000;

    private final int pageSize;
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;

    private final List<String> columns = new ArrayList<>();
    private final List<String> columnTypes = new ArrayList<>();
    private final List<List<String>> cachedRows = new ArrayList<>();
    private boolean exhausted = false;
    private boolean isQueryResult = false;
    private int updateCount = -1;
    private String message;
    private long executionMillis;

    public PagedResultCursor(int pageSize) {
        this.pageSize = Math.max(1, pageSize);
    }

    /** Runs the statement and reads the first page (+1 row, to know if there's more). Call off the FX thread. */
    public void open(ConnectionProfile profile, String catalog, String sql) throws SQLException {
        open(profile, catalog, sql, null);
    }

    /**
     * @param statementHolder if non-null, the live Statement is published here the
     *                        instant it's created so a "Cancel" button on another
     *                        thread can call statement.cancel() to abort the query —
     *                        mirrors {@link JdbcClient}'s own cancellable execute.
     */
    public void open(ConnectionProfile profile, String catalog, String sql,
                     java.util.concurrent.atomic.AtomicReference<Statement> statementHolder) throws SQLException {
        close();
        long start = System.currentTimeMillis();

        connection = ClientRegistry.jdbc(profile, catalog).getConnection();
        statement = connection.createStatement();
        if (statementHolder != null) statementHolder.set(statement);
        try {
            isQueryResult = statement.execute(sql);
        } finally {
            if (statementHolder != null) statementHolder.set(null);
        }

        if (isQueryResult) {
            resultSet = statement.getResultSet();
            ResultSetMetaData meta = resultSet.getMetaData();
            int colCount = meta.getColumnCount();
            for (int i = 1; i <= colCount; i++) {
                columns.add(meta.getColumnLabel(i));
                columnTypes.add(safeTypeName(meta, i));
            }
            fetchMore(pageSize + 1);
        } else {
            int count = statement.getUpdateCount();
            updateCount = Math.max(count, 0);
            message = count >= 0 ? count + " row(s) affected" : "Statement executed";
        }
        executionMillis = System.currentTimeMillis() - start;
    }

    private static String safeTypeName(ResultSetMetaData meta, int i) {
        try {
            return meta.getColumnTypeName(i);
        } catch (SQLException e) {
            return "";
        }
    }

    /** Reads up to n more rows from the still-open ResultSet into the cache; returns how many were actually read. */
    public int fetchMore(int n) throws SQLException {
        if (resultSet == null || exhausted) return 0;
        int read = 0;
        while (read < n && cachedRows.size() < SAFETY_CAP && resultSet.next()) {
            List<String> row = new ArrayList<>(columns.size());
            for (int i = 1; i <= columns.size(); i++) {
                Object value = resultSet.getObject(i);
                row.add(value == null ? null : String.valueOf(value));
            }
            cachedRows.add(row);
            read++;
        }
        if (read < n || cachedRows.size() >= SAFETY_CAP) exhausted = true;
        return read;
    }

    /** Ensures at least (pageStart + pageSize + 1) rows are cached, fetching more if needed and not exhausted. */
    public void ensureFetchedThrough(int pageStart) throws SQLException {
        int needed = pageStart + pageSize + 1;
        if (cachedRows.size() < needed && !exhausted) {
            fetchMore(needed - cachedRows.size());
        }
    }

    public boolean isQueryResult() { return isQueryResult; }
    public List<String> getColumns() { return columns; }
    public List<String> getColumnTypes() { return columnTypes; }
    public List<List<String>> getCachedRows() { return cachedRows; }
    public boolean isExhausted() { return exhausted; }
    public int getPageSize() { return pageSize; }
    public int getUpdateCount() { return updateCount; }
    public String getMessage() { return message; }
    public long getExecutionMillis() { return executionMillis; }

    @Override
    public void close() {
        try { if (resultSet != null) resultSet.close(); } catch (SQLException ignored) {}
        try { if (statement != null) statement.close(); } catch (SQLException ignored) {}
        try { if (connection != null) connection.close(); } catch (SQLException ignored) {}
        resultSet = null;
        statement = null;
        connection = null;
        columns.clear();
        columnTypes.clear();
        cachedRows.clear();
        exhausted = false;
        isQueryResult = false;
        updateCount = -1;
        message = null;
    }
}
