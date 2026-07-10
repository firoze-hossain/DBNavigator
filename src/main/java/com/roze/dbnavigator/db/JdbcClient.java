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

    /** @param catalogOverride connect to a different database on the same server (PostgreSQL). */
    public JdbcClient(ConnectionProfile profile, String catalogOverride) {
        this.profile = profile;
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(profile.getJdbcUrl(catalogOverride));
        if (profile.getType() != ConnectionProfile.DatabaseType.SQLITE) {
            config.setUsername(profile.getUsername());
            config.setPassword(profile.getPassword() == null ? "" : profile.getPassword());
        }
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000);
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
        QueryResult result = new QueryResult();
        long start = System.currentTimeMillis();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

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
        }

        result.setExecutionMillis(System.currentTimeMillis() - start);
        return result;
    }

    /** Paged read of a whole table. */
    public QueryResult fetchTablePage(String qualifiedTable, int offset, int pageSize,
                                      String whereClause, String orderBy) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(qualifiedTable);
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
