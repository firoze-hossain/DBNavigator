package com.roze.dbnavigator.model;

import java.util.ArrayList;
import java.util.List;

/** Tabular result of a query — works for SQL rows and flattened Mongo documents alike. */
public class QueryResult {

    private final List<String> columns = new ArrayList<>();
    private final List<String> columnTypes = new ArrayList<>();   // e.g. varchar, timestamp
    private final List<List<String>> rows = new ArrayList<>();
    private long executionMillis;
    private int updateCount = -1;   // >= 0 when the statement was an UPDATE/INSERT/DELETE/DDL
    private String message;

    public List<String> getColumns() { return columns; }
    public List<String> getColumnTypes() { return columnTypes; }
    public List<List<String>> getRows() { return rows; }

    public long getExecutionMillis() { return executionMillis; }
    public void setExecutionMillis(long executionMillis) { this.executionMillis = executionMillis; }

    public int getUpdateCount() { return updateCount; }
    public void setUpdateCount(int updateCount) { this.updateCount = updateCount; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isResultSet() { return !columns.isEmpty(); }
}
