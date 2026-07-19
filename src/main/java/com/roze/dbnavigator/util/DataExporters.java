package com.roze.dbnavigator.util;

import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.QueryResult;

import java.util.List;
import java.util.Locale;

/**
 * Builds text for each of DataGrip's "Extractor" export formats from a
 * QueryResult. Delimited formats (CSV/TSV/pipe/semicolon) share one
 * implementation; SQL Inserts/Updates and Where Clause build real,
 * runnable SQL text.
 */
public final class DataExporters {

    public enum Format {
        SQL_INSERTS("SQL Inserts"),
        SQL_UPDATES("SQL Updates"),
        WHERE_CLAUSE("Where Clause"),
        EXCEL("Excel (xlsx)"),
        CSV("CSV"),
        TSV("TSV"),
        PIPE("Pipe-separated"),
        SEMICOLON("Semicolon-separated");

        private final String label;
        Format(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    private DataExporters() {}

    public static String render(Format format, QueryResult result, String qualifiedTable, int rowLimit) {
        List<String> columns = result.getColumns();
        List<List<String>> rows = result.getRows();
        int limit = rowLimit <= 0 ? rows.size() : Math.min(rowLimit, rows.size());

        return switch (format) {
            case CSV -> delimited(columns, rows, limit, ',');
            case TSV -> delimited(columns, rows, limit, '\t');
            case PIPE -> delimited(columns, rows, limit, '|');
            case SEMICOLON -> delimited(columns, rows, limit, ';');
            case SQL_INSERTS -> sqlInserts(columns, rows, limit, qualifiedTable);
            case SQL_UPDATES -> sqlUpdates(columns, rows, limit, qualifiedTable);
            case WHERE_CLAUSE -> whereClauses(columns, rows, limit);
            case EXCEL -> "(binary format — use Export to File, not Copy to Clipboard)";
        };
    }

    // ------------------------------------------------------------ delimited

    private static String delimited(List<String> columns, List<List<String>> rows, int limit, char delim) {
        StringBuilder sb = new StringBuilder();
        sb.append(joinRow(columns, delim)).append('\n');
        for (int i = 0; i < limit; i++) {
            sb.append(joinRow(rows.get(i), delim)).append('\n');
        }
        return sb.toString();
    }

    private static String joinRow(List<String> values, char delim) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(delim);
            sb.append(escapeDelimited(values.get(i), delim));
        }
        return sb.toString();
    }

    private static String escapeDelimited(String value, char delim) {
        if (value == null) return "";
        boolean needsQuote = value.indexOf(delim) >= 0 || value.contains("\"") || value.contains("\n");
        if (!needsQuote) return value;
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    // ------------------------------------------------------------ SQL

    private static String sqlInserts(List<String> columns, List<List<String>> rows, int limit, String table) {
        StringBuilder sb = new StringBuilder();
        String columnList = String.join(", ", columns.stream().map(DbObject::quote).toList());
        for (int i = 0; i < limit; i++) {
            List<String> row = rows.get(i);
            sb.append("INSERT INTO ").append(table).append(" (").append(columnList).append(") VALUES (");
            for (int c = 0; c < row.size(); c++) {
                if (c > 0) sb.append(", ");
                sb.append(literal(row.get(c)));
            }
            sb.append(");\n");
        }
        return sb.toString();
    }

    private static String sqlUpdates(List<String> columns, List<List<String>> rows, int limit, String table) {
        StringBuilder sb = new StringBuilder();
        // No guaranteed primary key at this layer — key on every column, which
        // is always correct (if verbose) as an UPDATE ... WHERE identifying clause.
        for (int i = 0; i < limit; i++) {
            List<String> row = rows.get(i);
            sb.append("UPDATE ").append(table).append(" SET ");
            for (int c = 0; c < columns.size(); c++) {
                if (c > 0) sb.append(", ");
                sb.append(DbObject.quote(columns.get(c))).append(" = ").append(literal(row.get(c)));
            }
            sb.append(" WHERE ");
            for (int c = 0; c < columns.size(); c++) {
                if (c > 0) sb.append(" AND ");
                sb.append(DbObject.quote(columns.get(c))).append(" = ").append(literal(row.get(c)));
            }
            sb.append(";\n");
        }
        return sb.toString();
    }

    private static String whereClauses(List<String> columns, List<List<String>> rows, int limit) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            List<String> row = rows.get(i);
            sb.append("WHERE ");
            for (int c = 0; c < columns.size(); c++) {
                if (c > 0) sb.append(" AND ");
                sb.append(DbObject.quote(columns.get(c))).append(" = ").append(literal(row.get(c)));
            }
            sb.append(";\n");
        }
        return sb.toString();
    }

    private static String literal(String value) {
        if (value == null || value.equals("NULL")) return "NULL";
        if (value.matches("-?\\d+(\\.\\d+)?")) return value;
        return "'" + value.replace("'", "''") + "'";
    }

    public static boolean isBinary(Format format) {
        return format == Format.EXCEL;
    }
}
