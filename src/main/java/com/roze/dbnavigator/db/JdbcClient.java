package com.roze.dbnavigator.db;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.QueryResult;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** One pooled JDBC client per relational connection profile. */
public class JdbcClient implements AutoCloseable {

    private final ConnectionProfile profile;
    private final HikariDataSource dataSource;

    public JdbcClient(ConnectionProfile profile) {
        this(profile, null);
    }

    /** @param catalogOverride connect to a different database on the same server. */
    public JdbcClient(ConnectionProfile profile, String catalogOverride) {
        this.profile = profile;
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(profile.getJdbcUrl(catalogOverride));
        if (profile.getType() != ConnectionProfile.DatabaseType.SQLITE) {
            config.setUsername(profile.getUsername());
            config.setPassword(profile.getPassword() == null ? "" : profile.getPassword());
        }
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10_000);
        config.setIdleTimeout(60_000);
        config.setMaxLifetime(600_000);
        config.setPoolName("DBNav-" + profile.getName()
                + (catalogOverride == null ? "" : "-" + catalogOverride));
        this.dataSource = new HikariDataSource(config);
    }

    public ConnectionProfile getProfile() { return profile; }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /** Executes any SQL statement; returns rows for SELECTs, update count otherwise. */
    public QueryResult execute(String sql, int maxRows) throws SQLException {
        return execute(sql, maxRows, null);
    }

    /**
     * Executes any SQL statement; returns rows for SELECTs, update count otherwise.
     *
     * @param statementHolder if non-null, the live Statement is published here the
     *                        instant it's created so a "Cancel" button on another
     *                        thread can call statement.cancel() to abort the query.
     */
    private static final int DEFAULT_QUERY_TIMEOUT_SECONDS = 120;

    public QueryResult execute(String sql, int maxRows,
                               java.util.concurrent.atomic.AtomicReference<Statement> statementHolder)
            throws SQLException {
        QueryResult result = new QueryResult();
        long start = System.currentTimeMillis();

        try {
            executeOnce(sql, maxRows, statementHolder, result, true);
        } catch (SQLException ex) {
            if (isTransactionBlockError(ex)) {
                // Some statements (CREATE DATABASE, DROP DATABASE, VACUUM,
                // ALTER SYSTEM, etc.) are only valid outside any transaction
                // on PostgreSQL — the autocommit(false) used below for
                // cursor-based fetching puts every statement inside an
                // implicit one. Retry once without it; none of these
                // statements return large result sets anyway.
                executeOnce(sql, maxRows, statementHolder, result, false);
            } else {
                throw ex;
            }
        }

        result.setExecutionMillis(System.currentTimeMillis() - start);
        return result;
    }

    private static boolean isTransactionBlockError(SQLException ex) {
        String msg = ex.getMessage();
        return msg != null
                && msg.toLowerCase(java.util.Locale.ROOT).contains("cannot run inside a transaction block");
    }

    private void executeOnce(String sql, int maxRows,
                             java.util.concurrent.atomic.AtomicReference<Statement> statementHolder,
                             QueryResult result, boolean useCursor) throws SQLException {
        try (Connection conn = getConnection()) {
            // Same fix as PagedResultCursor: without this, PostgreSQL's driver
            // ignores setFetchSize and eagerly buffers the ENTIRE result set
            // client-side during execute() — for an unbounded query (maxRows=0,
            // e.g. "Execute to File" on a large table) that risks the same
            // memory-pressure freeze/crash regardless of how the rows are
            // consumed afterward.
            boolean supportsCursor = useCursor && trySetAutoCommitFalse(conn);
            try (Statement stmt = conn.createStatement()) {
                if (supportsCursor) {
                    stmt.setFetchSize(maxRows > 0 ? Math.min(maxRows, 1000) : 1000);
                }
                try {
                    stmt.setQueryTimeout(DEFAULT_QUERY_TIMEOUT_SECONDS);
                } catch (SQLException ignored) {
                    // Some drivers may not support query timeout; best-effort only.
                }
                if (statementHolder != null) statementHolder.set(stmt);
                stmt.setMaxRows(maxRows);
                boolean hasResultSet = stmt.execute(sql);

                if (hasResultSet) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        readResultSet(rs, result, maxRows);
                    }
                } else {
                    int count = stmt.getUpdateCount();
                    result.setUpdateCount(Math.max(count, 0));
                    result.setMessage(count >= 0
                            ? count + " row(s) affected"
                            : "Statement executed");
                }
            } finally {
                if (statementHolder != null) statementHolder.set(null);
                if (supportsCursor) {
                    // commit() is the normal path (also persists any writes the
                    // statement made); it only fails when the transaction was
                    // already left in an aborted state by a thrown exception,
                    // in which case rollback is the only thing that can recover
                    // the connection cleanly before it's returned to the pool.
                    try {
                        conn.commit();
                    } catch (SQLException commitFailed) {
                        try { conn.rollback(); } catch (SQLException ignored) {}
                    }
                    try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                }
            }
        }
    }

    private static boolean trySetAutoCommitFalse(Connection conn) {
        try {
            conn.setAutoCommit(false);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /** Paged read of a whole table. */
    public QueryResult fetchTablePage(String qualifiedTable, int offset, int pageSize,
                                      String whereClause, String orderBy) throws SQLException {
        return fetchTablePage(qualifiedTable, offset, pageSize, whereClause, orderBy, false);
    }

    /**
     * @param includeCtid PostgreSQL only: select the physical row identity
     *                    (tableoid + ctid) so tables WITHOUT a primary key —
     *                    e.g. partitions — can still be edited safely.
     */
    public QueryResult fetchTablePage(String qualifiedTable, int offset, int pageSize,
                                      String whereClause, String orderBy,
                                      boolean includeCtid) throws SQLException {
        StringBuilder sql = new StringBuilder(includeCtid
                ? "SELECT tableoid::text AS tableoid, ctid, * FROM "
                : "SELECT * FROM ").append(qualifiedTable);
        if (whereClause != null && !whereClause.isBlank()) sql.append(" WHERE ").append(whereClause);
        if (orderBy != null && !orderBy.isBlank()) sql.append(" ORDER BY ").append(orderBy);

        sql.append(switch (profile.getType()) {
            case SQLSERVER -> {
                // SQL Server requires ORDER BY for OFFSET/FETCH
                String prefix = (orderBy == null || orderBy.isBlank()) ? " ORDER BY (SELECT NULL)" : "";
                yield prefix + " OFFSET " + offset + " ROWS FETCH NEXT " + pageSize + " ROWS ONLY";
            }
            case ORACLE -> " OFFSET " + offset + " ROWS FETCH NEXT " + pageSize + " ROWS ONLY";
            default -> " LIMIT " + pageSize + " OFFSET " + offset;
        });

        return execute(sql.toString(), pageSize);
    }

    public long countRows(String qualifiedTable, String whereClause) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + qualifiedTable
                + (whereClause != null && !whereClause.isBlank() ? " WHERE " + whereClause : "");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    static void readResultSet(ResultSet rs, QueryResult result, int maxRows) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        for (int i = 1; i <= colCount; i++) {
            result.getColumns().add(meta.getColumnLabel(i));
            String typeName;
            try {
                typeName = meta.getColumnTypeName(i);
            } catch (SQLException e) {
                typeName = "";
            }
            result.getColumnTypes().add(typeName == null ? "" : typeName);
        }
        int count = 0;
        while (rs.next() && (maxRows <= 0 || count < maxRows)) {
            List<String> row = new ArrayList<>(colCount);
            for (int i = 1; i <= colCount; i++) {
                Object value = rs.getObject(i);
                row.add(value == null ? null : String.valueOf(value));
            }
            result.getRows().add(row);
            count++;
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
