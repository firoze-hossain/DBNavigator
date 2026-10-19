package com.roze.dbnavigator.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.QueryResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds text for each of the database data extractor export formats from a
 * QueryResult. Delimited formats (CSV/TSV/pipe/semicolon) share one
 * implementation; SQL Inserts/Updates, multi-row, JSON, HTML, Markdown, XML,
 * and pretty tabular formats generate runnable, well-formed output.
 */
public final class DataExporters {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public enum FormatCategory {
        BUILT_IN, CSV, SCRIPTED, BINARY
    }

    public enum Format {
        // Built-in
        SQL_INSERTS("SQL Inserts", FormatCategory.BUILT_IN, ".sql"),
        SQL_UPDATES("SQL Updates", FormatCategory.BUILT_IN, ".sql"),
        WHERE_CLAUSE("Where Clause", FormatCategory.BUILT_IN, ".sql"),

        // Binary
        EXCEL("Excel (xlsx)", FormatCategory.BINARY, ".xlsx"),

        // CSV
        CSV("CSV", FormatCategory.CSV, ".csv"),
        TSV("TSV", FormatCategory.CSV, ".tsv"),
        PIPE("Pipe-separated", FormatCategory.CSV, ".txt"),
        SEMICOLON("Semicolon-separated", FormatCategory.CSV, ".txt"),

        // Scripted
        CSV_GROOVY("CSV-Groovy", FormatCategory.SCRIPTED, ".csv"),
        HTML_GROOVY("HTML (groovy)", FormatCategory.SCRIPTED, ".html"),
        HTML_JS("HTML (js)", FormatCategory.SCRIPTED, ".html"),
        JSON("JSON", FormatCategory.SCRIPTED, ".json"),
        MARKDOWN("Markdown", FormatCategory.SCRIPTED, ".md"),
        ONE_ROW("One-row", FormatCategory.SCRIPTED, ".sql"),
        PRETTY("Pretty", FormatCategory.SCRIPTED, ".txt"),
        PYTHON_DATAFRAME("Python-DataFrame", FormatCategory.SCRIPTED, ".py"),
        SQL_INSERT_MULTIROW("SQL-Insert-Multirow", FormatCategory.SCRIPTED, ".sql"),
        SQL_INSERT_STATEMENTS("SQL-Insert-Statements", FormatCategory.SCRIPTED, ".sql"),
        XML("XML", FormatCategory.SCRIPTED, ".xml");

        private final String label;
        private final FormatCategory category;
        private final String defaultExtension;

        Format(String label, FormatCategory category, String defaultExtension) {
            this.label = label;
            this.category = category;
            this.defaultExtension = defaultExtension;
        }

        public FormatCategory getCategory() { return category; }
        public String getDefaultExtension() { return defaultExtension; }

        @Override
        public String toString() { return label; }

        public static Format fromLabel(String label) {
            if (label == null) return CSV;
            String trimmed = label.trim();
            for (Format f : values()) {
                if (f.label.equalsIgnoreCase(trimmed) || f.name().equalsIgnoreCase(trimmed)) {
                    return f;
                }
            }
            if (trimmed.equalsIgnoreCase("CSV (groovy)") || trimmed.equalsIgnoreCase("CSV-Groovy")
                    || trimmed.equalsIgnoreCase("csv.groovy") || trimmed.equalsIgnoreCase("CSV (Groovy)")) {
                return CSV_GROOVY;
            }
            return CSV;
        }
    }

    public static class ExportOptions {
        private boolean transpose = false;
        private boolean addColumnHeader = true;
        private boolean addRowHeader = false;
        private boolean skipComputedColumns = true;
        private boolean skipGeneratedColumns = false;
        private boolean addComputedColumns = false;
        private boolean addGeneratedColumns = true;
        private boolean addTableDefinition = false;
        private boolean outputQueryToSeparateSheet = true;

        public ExportOptions() {}

        public ExportOptions(boolean transpose, boolean addColumnHeader, boolean addRowHeader) {
            this.transpose = transpose;
            this.addColumnHeader = addColumnHeader;
            this.addRowHeader = addRowHeader;
        }

        public boolean isTranspose() { return transpose; }
        public void setTranspose(boolean transpose) { this.transpose = transpose; }

        public boolean isAddColumnHeader() { return addColumnHeader; }
        public void setAddColumnHeader(boolean addColumnHeader) { this.addColumnHeader = addColumnHeader; }

        public boolean isAddRowHeader() { return addRowHeader; }
        public void setAddRowHeader(boolean addRowHeader) { this.addRowHeader = addRowHeader; }

        public boolean isSkipComputedColumns() { return skipComputedColumns; }
        public void setSkipComputedColumns(boolean skipComputedColumns) { this.skipComputedColumns = skipComputedColumns; }

        public boolean isSkipGeneratedColumns() { return skipGeneratedColumns; }
        public void setSkipGeneratedColumns(boolean skipGeneratedColumns) { this.skipGeneratedColumns = skipGeneratedColumns; }

        public boolean isAddComputedColumns() { return addComputedColumns; }
        public void setAddComputedColumns(boolean addComputedColumns) { this.addComputedColumns = addComputedColumns; }

        public boolean isAddGeneratedColumns() { return addGeneratedColumns; }
        public void setAddGeneratedColumns(boolean addGeneratedColumns) { this.addGeneratedColumns = addGeneratedColumns; }

        public boolean isAddTableDefinition() { return addTableDefinition; }
        public void setAddTableDefinition(boolean addTableDefinition) { this.addTableDefinition = addTableDefinition; }

        public boolean isOutputQueryToSeparateSheet() { return outputQueryToSeparateSheet; }
        public void setOutputQueryToSeparateSheet(boolean outputQueryToSeparateSheet) {
            this.outputQueryToSeparateSheet = outputQueryToSeparateSheet;
        }
    }

    private DataExporters() {}

    public static String defaultExtension(Format format) {
        return format != null ? format.getDefaultExtension() : ".csv";
    }

    public static boolean isBinary(Format format) {
        return format == Format.EXCEL;
    }

    public static String render(Format format, QueryResult result, String qualifiedTable, int rowLimit) {
        return render(format, result, qualifiedTable, rowLimit, new ExportOptions());
    }

    public static String render(Format format, QueryResult result, String qualifiedTable, int rowLimit, ExportOptions options) {
        if (result == null) return "";
        if (options == null) options = new ExportOptions();

        List<String> rawColumns = result.getColumns() != null ? new ArrayList<>(result.getColumns()) : new ArrayList<>();
        List<List<String>> rawRows = result.getRows() != null ? new ArrayList<>(result.getRows()) : new ArrayList<>();

        List<Integer> validIndices = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        for (int i = 0; i < rawColumns.size(); i++) {
            String col = rawColumns.get(i);
            if ("tableoid".equalsIgnoreCase(col) || "ctid".equalsIgnoreCase(col)) {
                continue;
            }
            validIndices.add(i);
            columns.add(col);
        }

        int limit = rowLimit <= 0 ? rawRows.size() : Math.min(rowLimit, rawRows.size());
        List<List<String>> limitedRows = new ArrayList<>();
        for (int r = 0; r < limit; r++) {
            List<String> originalRow = rawRows.get(r);
            List<String> row = new ArrayList<>(validIndices.size());
            for (int idx : validIndices) {
                row.add(idx < originalRow.size() ? originalRow.get(idx) : "");
            }
            limitedRows.add(row);
        }

        String tableName = (qualifiedTable != null && !qualifiedTable.isBlank()) ? qualifiedTable : "table";

        if (format == Format.EXCEL) {
            return "[Binary Excel Spreadsheet (.xlsx)]\n"
                    + "Target Table: " + tableName + "\n"
                    + "Columns: " + String.join(", ", columns) + "\n"
                    + "Rows: " + limitedRows.size() + " row(s)\n\n"
                    + "(Binary format \u2014 live text preview not applicable; use Export to File)";
        }

        // Apply transposition and headers if needed for tabular formats
        TransposedData transposed = applyTransformations(columns, limitedRows, options);

        AppSettingsStore.Settings settings = AppSettingsStore.load();

        return switch (format) {
            case CSV -> renderDelimited(transposed, ",", options);
            case CSV_GROOVY -> renderCsvGroovy(transposed, options);
            case TSV -> renderDelimited(transposed, "\t", options);
            case PIPE -> renderDelimited(transposed, "|", options);
            case SEMICOLON -> renderDelimited(transposed, ";", options);
            case SQL_INSERTS -> sqlInserts(columns, limitedRows, tableName, options);
            case SQL_UPDATES -> sqlUpdates(columns, limitedRows, tableName, options);
            case WHERE_CLAUSE -> whereClauses(columns, limitedRows, options);
            case SQL_INSERT_MULTIROW -> sqlInsertMultirow(columns, limitedRows, tableName, options);
            case SQL_INSERT_STATEMENTS -> sqlInsertStatements(columns, limitedRows, tableName, options);
            case JSON -> renderJson(transposed.headers, transposed.rows, options);
            case MARKDOWN -> renderMarkdown(transposed.headers, transposed.rows, options);
            case PRETTY -> renderPretty(transposed.headers, transposed.rows, options);
            case PYTHON_DATAFRAME -> renderPythonDataFrame(transposed.headers, transposed.rows, options);
            case HTML_GROOVY -> renderHtmlGroovy(transposed.headers, transposed.rows, options);
            case HTML_JS -> renderHtmlJs(transposed.headers, transposed.rows, tableName, options);
            case ONE_ROW -> renderOneRow(transposed.headers, transposed.rows, options);
            case XML -> renderXml(transposed.headers, transposed.rows, tableName, options);
            case EXCEL -> "";
        };
    }

    private record TransposedData(List<String> headers, List<List<String>> rows) {}

    private static TransposedData applyTransformations(List<String> columns, List<List<String>> rows, ExportOptions options) {
        if (!options.isTranspose()) {
            List<String> headers = new ArrayList<>(columns);
            List<List<String>> transformedRows = new ArrayList<>();
            for (int r = 0; r < rows.size(); r++) {
                List<String> original = rows.get(r);
                List<String> row = new ArrayList<>(original);
                if (options.isAddRowHeader()) {
                    row.add(0, String.valueOf(r + 1));
                }
                transformedRows.add(row);
            }
            if (options.isAddRowHeader()) {
                headers.add(0, "#");
            }
            return new TransposedData(headers, transformedRows);
        }

        // Transpose: columns become rows, rows become columns
        int origColCount = columns.size();
        int origRowCount = rows.size();

        List<String> newHeaders = new ArrayList<>();
        if (options.isAddRowHeader()) {
            newHeaders.add("Column");
        }
        for (int r = 0; r < origRowCount; r++) {
            newHeaders.add(String.valueOf(r + 1));
        }

        List<List<String>> newRows = new ArrayList<>();
        for (int c = 0; c < origColCount; c++) {
            List<String> newRow = new ArrayList<>();
            if (options.isAddRowHeader() || true) {
                newRow.add(columns.get(c));
            }
            for (int r = 0; r < origRowCount; r++) {
                List<String> row = rows.get(r);
                newRow.add(c < row.size() ? row.get(c) : "");
            }
            newRows.add(newRow);
        }

        return new TransposedData(newHeaders, newRows);
    }

    // ------------------------------------------------------------ Delimited (CSV, TSV, Pipe, Semicolon)

    private static String renderDelimited(TransposedData data, String delim, ExportOptions options) {
        StringBuilder sb = new StringBuilder();
        if (options.isAddColumnHeader() && data.headers != null && !data.headers.isEmpty()) {
            sb.append(joinDelimited(data.headers, delim)).append('\n');
        }
        for (List<String> row : data.rows) {
            sb.append(joinDelimited(row, delim)).append('\n');
        }
        return sb.toString();
    }

    private static String renderCsvGroovy(TransposedData data, ExportOptions options) {
        StringBuilder sb = new StringBuilder();
        for (List<String> row : data.rows) {
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) sb.append(',');
                String val = row.get(i);
                if (val == null || "null".equalsIgnoreCase(val)) {
                    sb.append("null");
                } else {
                    sb.append(escapeDelimited(val, ","));
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String joinDelimited(List<String> values, String delim) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(delim);
            sb.append(escapeDelimited(values.get(i), delim));
        }
        return sb.toString();
    }

    private static String escapeDelimited(String value, String delim) {
        if (value == null) return "";
        boolean needsQuote = value.contains(delim) || value.contains("\"") || value.contains("\n") || value.contains("\r");
        if (!needsQuote) return value;
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    // ------------------------------------------------------------ SQL Formats

    public static String sqlTableName(String qualifiedTable) {
        if (qualifiedTable == null || qualifiedTable.isBlank()) return "table";
        String[] parts = qualifiedTable.split("\\.");
        if (parts.length >= 3) {
            return DbObject.quote(parts[parts.length - 2]) + "." + DbObject.quote(parts[parts.length - 1]);
        } else if (parts.length == 2) {
            return DbObject.quote(parts[0]) + "." + DbObject.quote(parts[1]);
        }
        return DbObject.quote(qualifiedTable);
    }

    private static String sqlInserts(List<String> columns, List<List<String>> rows, String table, ExportOptions options) {
        StringBuilder sb = new StringBuilder();
        String targetTable = sqlTableName(table);
        if (options != null && options.isAddTableDefinition()) {
            sb.append("CREATE TABLE ").append(targetTable).append(" (\n");
            for (int c = 0; c < columns.size(); c++) {
                sb.append("    ").append(DbObject.quote(columns.get(c))).append(" varchar");
                if (c < columns.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append(");\n\n");
        }
        String columnList = String.join(", ", columns.stream().map(DbObject::quote).toList());
        for (List<String> row : rows) {
            sb.append("INSERT INTO ").append(targetTable).append(" (").append(columnList).append(") VALUES (");
            for (int c = 0; c < row.size(); c++) {
                if (c > 0) sb.append(", ");
                sb.append(sqlLiteral(row.get(c)));
            }
            sb.append(");\n");
        }
        return sb.toString();
    }

    private static String sqlInsertMultirow(List<String> columns, List<List<String>> rows, String table, ExportOptions options) {
        if (rows.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        String targetTable = sqlTableName(table);
        if (options != null && options.isAddTableDefinition()) {
            sb.append("create table ").append(targetTable).append(" (\n");
            for (int c = 0; c < columns.size(); c++) {
                sb.append("    ").append(DbObject.quote(columns.get(c))).append(" varchar");
                if (c < columns.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append(");\n\n");
        }
        String columnList = String.join(", ", columns.stream().map(DbObject::quote).toList());
        sb.append("insert into ").append(targetTable).append(" (").append(columnList).append(")\n");
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (i == 0) {
                sb.append("values (");
            } else {
                sb.append("       (");
            }
            for (int c = 0; c < row.size(); c++) {
                if (c > 0) sb.append(", ");
                sb.append(sqlLiteralLower(row.get(c)));
            }
            sb.append(")");
            if (i < rows.size() - 1) {
                sb.append(",\n");
            } else {
                sb.append(";\n");
            }
        }
        return sb.toString();
    }

    private static String sqlInsertStatements(List<String> columns, List<List<String>> rows, String table, ExportOptions options) {
        StringBuilder sb = new StringBuilder();
        String targetTable = sqlTableName(table);
        if (options != null && options.isAddTableDefinition()) {
            sb.append("create table ").append(targetTable).append(" (\n");
            for (int c = 0; c < columns.size(); c++) {
                sb.append("    ").append(DbObject.quote(columns.get(c))).append(" varchar");
                if (c < columns.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append(");\n\n");
        }
        String columnList = String.join(", ", columns.stream().map(DbObject::quote).toList());
        for (List<String> row : rows) {
            sb.append("insert into ").append(targetTable).append(" (").append(columnList).append(") values (");
            for (int c = 0; c < row.size(); c++) {
                if (c > 0) sb.append(", ");
                sb.append(sqlLiteralLower(row.get(c)));
            }
            sb.append(");\n");
        }
        return sb.toString();
    }

    private static String sqlUpdates(List<String> columns, List<List<String>> rows, String table, ExportOptions options) {
        StringBuilder sb = new StringBuilder();
        String targetTable = sqlTableName(table);
        for (List<String> row : rows) {
            sb.append("UPDATE ").append(targetTable).append(" SET ");
            for (int c = 0; c < columns.size(); c++) {
                if (c > 0) sb.append(", ");
                sb.append(DbObject.quote(columns.get(c))).append(" = ").append(sqlLiteral(c < row.size() ? row.get(c) : null));
            }
            sb.append(";\n");
        }
        return sb.toString();
    }

    private static String whereClauses(List<String> columns, List<List<String>> rows, ExportOptions options) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            if (r > 0) {
                sb.append("OR ");
            }
            for (int c = 0; c < columns.size(); c++) {
                if (c > 0) sb.append(" AND ");
                String col = columns.get(c);
                String val = c < row.size() ? row.get(c) : null;
                if (val == null || val.equalsIgnoreCase("null") || val.equals("<null>")) {
                    sb.append(DbObject.quote(col)).append(" IS NULL");
                } else {
                    sb.append(DbObject.quote(col)).append(" = ").append(sqlLiteral(val));
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private static String sqlLiteral(String value) {
        if (value == null || value.equalsIgnoreCase("null") || value.equals("<null>")) return "NULL";
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) return value.toUpperCase();
        if (value.matches("-?\\d+(\\.\\d+)?")) return value;
        return "'" + value.replace("'", "''") + "'";
    }

    private static String sqlLiteralLower(String value) {
        if (value == null || value.equalsIgnoreCase("null") || value.equals("<null>")) return "null";
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) return value.toLowerCase();
        if (value.matches("-?\\d+(\\.\\d+)?")) return value;
        return "'" + value.replace("'", "''") + "'";
    }

    // ------------------------------------------------------------ JSON

    private static String renderJson(List<String> headers, List<List<String>> rows, ExportOptions options) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int r = 0; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            sb.append("  {\n");
            for (int c = 0; c < headers.size(); c++) {
                String header = headers.get(c);
                String val = c < row.size() ? row.get(c) : null;
                sb.append("    \"").append(jsonEscape(header)).append("\": ").append(formatJsonValue(val));
                if (c < headers.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  }");
            if (r < rows.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String formatJsonValue(String value) {
        if (value == null || value.equals("NULL")) return "null";
        if (value.equalsIgnoreCase("true")) return "true";
        if (value.equalsIgnoreCase("false")) return "false";
        if (value.matches("-?\\d+(\\.\\d+)?")) return value;

        String trimmed = value.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            try {
                // Check if it is valid json
                OBJECT_MAPPER.readTree(trimmed);
                return trimmed;
            } catch (Exception ignore) {
                // Not valid JSON, fall through to string escape
            }
        }
        return "\"" + jsonEscape(value) + "\"";
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch <= '\u001F') {
                        sb.append(String.format(Locale.ROOT, "\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ Markdown

    private static String renderMarkdown(List<String> headers, List<List<String>> rows, ExportOptions options) {
        if (headers == null || headers.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("|");
        for (String h : headers) {
            sb.append(" ").append(mdEscape(h)).append(" |");
        }
        sb.append("\n|");
        for (int i = 0; i < headers.size(); i++) {
            sb.append(":---|");
        }
        sb.append("\n");

        for (List<String> row : rows) {
            sb.append("|");
            for (int i = 0; i < headers.size(); i++) {
                String val = i < row.size() ? row.get(i) : null;
                if (val == null || val.equalsIgnoreCase("null") || val.equals("<null>")) {
                    sb.append(" null |");
                } else {
                    sb.append(" ").append(mdEscape(val)).append(" |");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private static String mdEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("_", "\\_")
                .replace("|", "\\|")
                .replace("\n", " ");
    }

    // ------------------------------------------------------------ Pretty (ASCII Border)

    private static String renderPretty(List<String> headers, List<List<String>> rows, ExportOptions options) {
        int colCount = headers != null ? headers.size() : 0;
        if (colCount == 0) return "";

        int[] widths = new int[colCount];
        for (int c = 0; c < colCount; c++) {
            widths[c] = headers.get(c) != null ? headers.get(c).length() : 0;
        }
        for (List<String> row : rows) {
            for (int c = 0; c < colCount; c++) {
                String raw = c < row.size() ? row.get(c) : null;
                String v = formatPrettyValue(raw);
                if (v.length() > widths[c]) widths[c] = v.length();
            }
        }

        StringBuilder sb = new StringBuilder();
        String separator = buildPrettySeparator(widths);

        sb.append(separator).append('\n');
        if (options.isAddColumnHeader()) {
            sb.append("|");
            for (int c = 0; c < colCount; c++) {
                sb.append(padRight(headers.get(c), widths[c])).append("|");
            }
            sb.append('\n');
            sb.append(separator).append('\n');
        }

        for (List<String> row : rows) {
            sb.append("|");
            for (int c = 0; c < colCount; c++) {
                String raw = c < row.size() ? row.get(c) : null;
                String v = formatPrettyValue(raw);
                sb.append(padRight(v, widths[c])).append("|");
            }
            sb.append('\n');
        }
        sb.append(separator).append('\n');
        return sb.toString();
    }

    private static String formatPrettyValue(String raw) {
        if (raw == null || raw.equalsIgnoreCase("null") || raw.equals("<null>")) {
            return "null";
        }
        return raw;
    }

    private static String buildPrettySeparator(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            sb.append("-".repeat(w)).append("+");
        }
        return sb.toString();
    }

    private static String padRight(String s, int n) {
        if (s == null) s = "";
        return String.format(Locale.ROOT, "%-" + n + "s", s);
    }

    // ------------------------------------------------------------ Python DataFrame

    private static String renderPythonDataFrame(List<String> headers, List<List<String>> rows, ExportOptions options) {
        StringBuilder sb = new StringBuilder();
        sb.append("import pandas as pd\n\n");
        sb.append("data = [\n");
        for (int r = 0; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            sb.append("    {");
            for (int c = 0; c < headers.size(); c++) {
                if (c > 0) sb.append(", ");
                String header = headers.get(c);
                String val = c < row.size() ? row.get(c) : null;
                sb.append("\"").append(jsonEscape(header)).append("\": ").append(formatPythonValue(val));
            }
            sb.append("}");
            if (r < rows.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]\n\n");
        sb.append("df = pd.DataFrame(data)\n");
        return sb.toString();
    }

    private static String formatPythonValue(String val) {
        if (val == null || val.equals("NULL")) return "None";
        if (val.equalsIgnoreCase("true")) return "True";
        if (val.equalsIgnoreCase("false")) return "False";
        if (val.matches("-?\\d+")) return val;
        if (val.matches("-?\\d+\\.\\d+")) return val;
        return "\"" + jsonEscape(val) + "\"";
    }

    // ------------------------------------------------------------ HTML Formats

    private static String renderHtmlGroovy(List<String> headers, List<List<String>> rows, ExportOptions options) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html>\n");
        sb.append("  <head>\n");
        sb.append("    <title></title>\n");
        sb.append("    <meta charset=\"UTF-8\">\n");
        sb.append("  </head>\n");
        sb.append("  <body>\n");
        sb.append("  <table border=\"1\" style=\"border-collapse:collapse\">\n");
        int colCount = headers != null ? headers.size() : 0;
        if (colCount > 0) {
            String colWidth = String.format(Locale.ROOT, "%.2f%%", 100.0 / colCount);
            sb.append("    <colgroup>\n");
            for (int i = 0; i < colCount; i++) {
                sb.append("      <col width=\"").append(colWidth).append("\"/>\n");
            }
            sb.append("    </colgroup>\n");
            sb.append("    <thead>\n");
            sb.append("      <tr>\n");
            for (String h : headers) {
                sb.append("        <th>").append(htmlEscape(h)).append("</th>\n");
            }
            sb.append("      </tr>\n");
            sb.append("    </thead>\n");
        }
        sb.append("    <tbody>\n");
        for (List<String> row : rows) {
            sb.append("      <tr>\n");
            for (int c = 0; c < colCount; c++) {
                String val = c < row.size() ? row.get(c) : "";
                if (val == null || val.equalsIgnoreCase("null") || val.equals("<null>")) {
                    sb.append("        <td></td>\n");
                } else {
                    sb.append("        <td>").append(htmlEscape(val)).append("</td>\n");
                }
            }
            sb.append("      </tr>\n");
        }
        sb.append("    </tbody>\n");
        sb.append("  </table>\n");
        sb.append("  </body>\n");
        sb.append("</html>\n");
        return sb.toString();
    }

    private static String renderHtmlJs(List<String> headers, List<List<String>> rows, String table, ExportOptions options) {
        return "";
    }

    private static String htmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // ------------------------------------------------------------ One Row

    private static String renderOneRow(List<String> headers, List<List<String>> rows, ExportOptions options) {
        if (rows == null || rows.isEmpty()) return "";
        List<String> firstRow = rows.get(0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < firstRow.size(); i++) {
            if (i > 0) sb.append(", ");
            String val = firstRow.get(i);
            if (val == null || val.equalsIgnoreCase("null") || val.equals("<null>")) {
                sb.append("null");
            } else if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false")) {
                sb.append(val.toLowerCase());
            } else if (val.matches("-?\\d+(\\.\\d+)?")) {
                sb.append(val);
            } else {
                sb.append("'").append(val.replace("'", "''")).append("'");
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ XML

    private static String renderXml(List<String> headers, List<List<String>> rows, String table, ExportOptions options) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<data>\n");
        for (List<String> row : rows) {
            sb.append("<row>\n");
            for (int c = 0; c < headers.size(); c++) {
                String tag = sanitizeXmlTag(headers.get(c));
                String raw = c < row.size() ? row.get(c) : null;
                String val = (raw == null || raw.equalsIgnoreCase("null") || raw.equals("<null>")) ? "null" : xmlEscape(raw);
                sb.append("    <").append(tag).append(">").append(val).append("</").append(tag).append(">\n");
            }
            sb.append("</row>\n");
        }
        sb.append("</data>\n");
        return sb.toString();
    }

    private static String sanitizeXmlTag(String tag) {
        if (tag == null || tag.isBlank()) return "column";
        String s = tag.replaceAll("[^a-zA-Z0-9_.-]", "_");
        if (!Character.isLetter(s.charAt(0)) && s.charAt(0) != '_') {
            s = "_" + s;
        }
        return s;
    }

    private static String xmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
