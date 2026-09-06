package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * StratosDB's own real backup/restore - closes the one real, StratosDB-
 * specific gap in this project's own dump/restore support (Postgres and
 * MySQL already have real, working pg_dump/pg_restore and mysqldump/mysql
 * integration - StratosDB's own menu item existed but did nothing at all).
 *
 * This is a direct, faithful port of stratosdump's own real, already-
 * verified algorithm (see StratosDump.java in the stratosdb repo itself
 * for the complete account of its own real design and honestly-stated
 * limitations, which this port shares exactly): {@code SHOW CATALOG} to
 * read back the exact, original CREATE statement text this engine
 * already persists for every schema object, then {@code SELECT *}
 * against every table to serialize its data as real INSERT statements,
 * in the same real, hand-worked-out dependency order stratosdump itself
 * uses (sequences before tables, tables before their data, data before
 * indexes, functions/procedures before triggers, extensions before any
 * native function that references one).
 *
 * A genuine, real improvement over stratosdump itself for this specific
 * use: stratosdump is a separate tool that opens its own raw socket and
 * does its own SCRAM handshake from scratch, needing a real, separate
 * jar located and invoked as an external process (matching pg_dump/
 * mysqldump's own shape) - this instead reuses the SAME, already-open,
 * already-authenticated JDBC connection DBNavigator itself already has
 * for this profile, so no separate binary needs to be installed or
 * located on the user's own machine at all.
 *
 * Restoring is symmetric and needs no separate tool either, matching
 * stratosdump's own real, honest claim that its own dump output is
 * "ordinary, valid SQL": split the file into individual statements and
 * run each one over the same connection.
 */
public final class StratosDumpService {

    private StratosDumpService() {}

    /**
     * The real dependency order a restore needs - identical to
     * stratosdump's own real DEPENDENCY_ORDER, kept in sync deliberately
     * rather than derived some other way, since this is StratosDB's own
     * real object-relationship ordering, not something DBNavigator gets
     * to redefine independently.
     */
    private static final List<String> DEPENDENCY_ORDER = List.of(
        "EXTENSION", "SEQUENCE", "TABLE", "__DATA__", "INDEX", "FUNCTION", "NATIVEFUNCTION", "PROCEDURE", "VIEW", "TRIGGER"
    );

    private static final Set<String> NUMERIC_TYPES = Set.of(
        "INT", "INTEGER", "BIGINT", "SMALLINT", "TINYINT", "SERIAL", "BIGSERIAL",
        "DECIMAL", "NUMERIC", "DOUBLE", "FLOAT", "BOOLEAN", "BOOL"
    );

    // ------------------------------------------------------------- dump

    public static void dumpDatabase(MainWindow mainWindow, ConnectionProfile profile, String database) {
        Window owner = mainWindow.getOwnerWindow();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save dump as\u2026");
        chooser.setInitialFileName(database + ".sql");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL files", "*.sql"));
        File file = chooser.showSaveDialog(owner);
        if (file == null) return;

        mainWindow.setStatus("Dumping " + database + "\u2026");
        AppExecutor.run(() -> {
            try {
                Connection conn = ClientRegistry.jdbc(profile, database).getConnection();
                try (PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                    dump(conn, writer);
                }
                Platform.runLater(() -> {
                    mainWindow.setStatus("Ready");
                    info(owner, "Dumped " + database + " to " + file.getName());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    mainWindow.setStatus("Ready");
                    error(owner, "Dump failed: " + e.getMessage());
                });
            }
        });
    }

    private static void dump(Connection conn, PrintWriter writer) throws SQLException {
        Map<String, List<String[]>> byType = new LinkedHashMap<>();
        List<String> tableNames = new ArrayList<>();
        Map<String, String> tableDdl = new LinkedHashMap<>();

        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SHOW CATALOG")) {
            while (rs.next()) {
                String objectType = rs.getString(1);
                String objectName = rs.getString(2);
                String ddlSql = rs.getString(3);
                byType.computeIfAbsent(objectType, k -> new ArrayList<>()).add(new String[]{objectName, ddlSql});
                if (objectType.equals("TABLE")) {
                    tableNames.add(objectName);
                    tableDdl.put(objectName, ddlSql);
                }
            }
        }

        writer.println("-- StratosDB dump - generated by DBNavigator");
        writer.println("-- Restore with DBNavigator's own \"Restore .sql into This Database...\", or any StratosDB SQL client");
        writer.println();

        for (String type : DEPENDENCY_ORDER) {
            if (type.equals("__DATA__")) {
                for (String tableName : tableNames) {
                    dumpTableData(conn, writer, tableName, tableDdl.get(tableName));
                }
                continue;
            }
            List<String[]> objects = byType.get(type);
            if (objects == null) continue;
            for (String[] object : objects) {
                writer.println(ensureTrailingSemicolon(object[1]));
            }
            if (!objects.isEmpty()) writer.println();
        }
        writer.flush();
    }

    private static String ensureTrailingSemicolon(String sql) {
        String trimmed = sql.trim();
        return trimmed.endsWith(";") ? trimmed : trimmed + ";";
    }

    private static void dumpTableData(Connection conn, PrintWriter writer, String tableName, String createTableDdl) {
        List<Map.Entry<String, String>> columns = parseColumnTypes(createTableDdl);
        List<String> columnNames = columns.stream().map(Map.Entry::getKey).toList();

        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT " + String.join(", ", columnNames) + " FROM " + tableName)) {
            boolean any = false;
            while (rs.next()) {
                any = true;
                StringBuilder line = new StringBuilder("INSERT INTO ").append(tableName)
                    .append(" (").append(String.join(", ", columnNames)).append(") VALUES (");
                for (int i = 0; i < columnNames.size(); i++) {
                    if (i > 0) line.append(", ");
                    String value = rs.getString(i + 1);
                    line.append(rs.wasNull() ? "NULL" : formatValue(value, columns.get(i).getValue()));
                }
                line.append(");");
                writer.println(line);
            }
            if (any) writer.println();
        } catch (SQLException e) {
            writer.println("-- WARNING: could not dump data for " + tableName + ": " + e.getMessage());
        }
    }

    /** Identical logic to StratosDump.formatValue - numeric/boolean types are written unquoted, everything else is quoted as a SQL string literal. */
    private static String formatValue(String value, String declaredType) {
        String baseType = declaredType.split("\\(")[0].replace("[]", "").trim().toUpperCase(Locale.ROOT);
        if (NUMERIC_TYPES.contains(baseType)) {
            return value;
        }
        return "'" + value.replace("'", "''") + "'";
    }

    /** Identical logic to StratosDump.parseColumnTypes - extracts (columnName -> declaredType) pairs, in order, from a real CREATE TABLE statement's own column list, tracking paren depth so a type like DECIMAL(10, 2)'s own internal comma is never mistaken for a column separator. */
    static List<Map.Entry<String, String>> parseColumnTypes(String createTableSql) {
        int openParen = createTableSql.indexOf('(');
        int closeParen = createTableSql.lastIndexOf(')');
        String columnList = createTableSql.substring(openParen + 1, closeParen);

        List<String> rawColumns = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < columnList.length(); i++) {
            char c = columnList.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) {
                rawColumns.add(columnList.substring(start, i));
                start = i + 1;
            }
        }
        rawColumns.add(columnList.substring(start));

        List<Map.Entry<String, String>> result = new ArrayList<>();
        for (String raw : rawColumns) {
            String trimmed = raw.trim();
            int spaceIdx = trimmed.indexOf(' ');
            if (spaceIdx < 0) continue; // defensive - a malformed/unexpected column definition, skip rather than crash the whole dump
            String columnName = trimmed.substring(0, spaceIdx);
            String rest = trimmed.substring(spaceIdx + 1).trim();
            result.add(Map.entry(columnName, rest));
        }
        return result;
    }

    // ---------------------------------------------------------- restore

    public static void restoreDatabase(MainWindow mainWindow, ConnectionProfile profile, String database) {
        Window owner = mainWindow.getOwnerWindow();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Restore from\u2026");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL files", "*.sql"));
        File file = chooser.showOpenDialog(owner);
        if (file == null) return;

        mainWindow.setStatus("Restoring into " + database + "\u2026");
        AppExecutor.run(() -> {
            try {
                Connection conn = ClientRegistry.jdbc(profile, database).getConnection();
                String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                List<String> statements = splitStatements(content);
                int executed = 0;
                try (Statement s = conn.createStatement()) {
                    for (String stmt : statements) {
                        String trimmed = stripComments(stmt).trim();
                        if (trimmed.isEmpty()) continue;
                        s.execute(trimmed);
                        executed++;
                    }
                }
                int finalExecuted = executed;
                Platform.runLater(() -> {
                    mainWindow.setStatus("Ready");
                    info(owner, "Restored " + finalExecuted + " statement(s) into " + database);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    mainWindow.setStatus("Ready");
                    error(owner, "Restore failed: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Splits a real, multi-statement SQL script into individual
     * statements by semicolon - correctly treating a semicolon inside a
     * real string literal (e.g. {@code 'it''s a test;'}, a real,
     * genuine value stratosdump's own dump output can produce) or a
     * line comment as ordinary text, not a statement separator.
     */
    static List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        boolean inLineComment = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (inLineComment) {
                current.append(c);
                if (c == '\n') inLineComment = false;
                continue;
            }
            if (inString) {
                current.append(c);
                if (c == '\'') {
                    if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                        current.append('\'');
                        i++;
                    } else {
                        inString = false;
                    }
                }
                continue;
            }
            if (c == '\'') {
                inString = true;
                current.append(c);
            } else if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                inLineComment = true;
                current.append(c);
            } else if (c == ';') {
                statements.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (!current.toString().isBlank()) {
            statements.add(current.toString());
        }
        return statements;
    }

    /** Removes real line comments from an already-split statement - a statement that is ENTIRELY a comment (dump's own header lines) must end up blank so it's skipped, not sent to the server as a real, empty-bodied statement. */
    private static String stripComments(String statement) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < statement.length(); i++) {
            char c = statement.charAt(i);
            if (!inString && c == '-' && i + 1 < statement.length() && statement.charAt(i + 1) == '-') {
                int newline = statement.indexOf('\n', i);
                if (newline < 0) break;
                i = newline;
                result.append('\n');
                continue;
            }
            if (c == '\'') inString = !inString;
            result.append(c);
        }
        return result.toString();
    }

    private static void info(Window owner, String message) {
        Alert alert = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION, message));
        alert.initOwner(owner);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private static void error(Window owner, String message) {
        Alert alert = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.ERROR, message));
        alert.initOwner(owner);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
