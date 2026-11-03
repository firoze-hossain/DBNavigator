package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.util.SqlAliases;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Semantic SQL inspection and schema validation for SQL consoles, matching DataGrip:
 *  - Introspects table names against the active database schema
 *  - Resolves aliases (e.g. "FROM Bbill b" -> "b" maps to "Bbill" / "bbill")
 *  - Flags unresolved tables (e.g. "HmsAdmission" -> "Unable to resolve table 'HmsAdmission'")
 *  - Flags unresolved columns on aliases or tables (e.g. "b.episodeTypeID" -> "Unable to resolve column 'episodeTypeID'")
 *  - Preserves valid columns (e.g. "b.status")
 *  - Provides hover documentation / DDL preview for resolved tables
 */
public final class SqlSchemaValidator {

    public enum DiagnosticKind {
        UNRESOLVED_TABLE,
        UNRESOLVED_COLUMN,
        RESOLVED_TABLE
    }

    public record Diagnostic(
            int start,
            int end,
            String token,
            DiagnosticKind kind,
            String message,
            String details
    ) {}

    // Pattern to find table references in FROM, JOIN, UPDATE, INTO, TABLE clauses
    // E.g. "FROM Bbill b", "JOIN HmsAdmission a", "FROM public.users", "UPDATE bbill"
    private static final Pattern TABLE_REF_PATTERN = Pattern.compile(
            "(?i)\\b(?:FROM|JOIN|UPDATE|INTO|TABLE)\\s+([\"`\\[]?[A-Za-z_][A-Za-z0-9_.]*[\"`\\]]?)"
    );

    // Pattern to find qualified column references: alias.column or table.column
    // E.g. "b.episodeTypeID", "b.status", "b.createdBy", "b.createdAt", "a.ehrEpisodeTypeId"
    private static final Pattern QUALIFIED_COL_PATTERN = Pattern.compile(
            "\\b([A-Za-z_][A-Za-z0-9_]*)\\.([A-Za-z_][A-Za-z0-9_]*)\\b"
    );

    // Keywords that can follow FROM/JOIN that are not tables (e.g., subquery "(")
    private static final Set<String> SQL_KEYWORDS = Set.of(
            "select", "where", "group", "order", "having", "limit", "offset",
            "values", "set", "on", "using", "as", "and", "or", "not"
    );

    private SqlSchemaValidator() {}

    public static List<Diagnostic> validate(String sql, ConnectionProfile profile, String catalog) {
        return validate(sql, profile, catalog, null);
    }

    public static List<Diagnostic> validate(String sql, ConnectionProfile profile, String catalog, Runnable onColumnsLoaded) {
        if (sql == null || sql.isBlank() || profile == null) {
            return List.of();
        }

        // Only validate if metadata has actually been loaded for this connection
        if (!CompletionService.isMetadataLoaded(profile, catalog)) {
            return List.of();
        }

        List<Diagnostic> diagnostics = new ArrayList<>();
        Map<String, String> aliases = SqlAliases.resolve(sql); // e.g. b -> Bbill, a -> HmsAdmission

        // 1. Validate Table References
        Matcher tableMatcher = TABLE_REF_PATTERN.matcher(sql);
        while (tableMatcher.find()) {
            int start = tableMatcher.start(1);
            int end = tableMatcher.end(1);
            String rawTableToken = tableMatcher.group(1);
            String cleanToken = cleanIdentifier(rawTableToken);

            if (cleanToken.isBlank() || SQL_KEYWORDS.contains(cleanToken.toLowerCase(Locale.ROOT))) {
                continue;
            }

            String canonicalTable = CompletionService.canonicalTableName(profile, catalog, cleanToken);
            if (canonicalTable == null) {
                // Table is NOT found in database schema!
                diagnostics.add(new Diagnostic(
                        start, end, rawTableToken,
                        DiagnosticKind.UNRESOLVED_TABLE,
                        "Unable to resolve table '" + cleanToken + "'",
                        "Table '" + cleanToken + "' cannot be found in schema " + (catalog != null ? catalog : "current")
                ));
            } else {
                // Table IS resolved! Provide DDL / definition details on hover
                diagnostics.add(new Diagnostic(
                        start, end, rawTableToken,
                        DiagnosticKind.RESOLVED_TABLE,
                        "Table: " + canonicalTable,
                        buildTableSummary(profile, catalog, canonicalTable)
                ));
                // Ensure columns of this table are preloaded in background
                CompletionService.getCachedColumns(profile, catalog, canonicalTable, onColumnsLoaded);
            }
        }

        // 2. Validate Qualified Column References (<alias>.<column> or <table>.<column>)
        Matcher colMatcher = QUALIFIED_COL_PATTERN.matcher(sql);
        while (colMatcher.find()) {
            String qualifier = colMatcher.group(1);
            String colToken = colMatcher.group(2);
            int colStart = colMatcher.start(2);
            int colEnd = colMatcher.end(2);

            // Ignore numeric qualifiers (like 12.34) or comments
            if (Character.isDigit(qualifier.charAt(0))) continue;

            // Resolve qualifier to table name (check aliases first, then check if qualifier itself is table name)
            String targetTable = aliases.get(qualifier.toLowerCase(Locale.ROOT));
            if (targetTable == null) {
                targetTable = qualifier;
            }

            String cleanTarget = cleanIdentifier(targetTable);
            String canonicalTable = CompletionService.canonicalTableName(profile, catalog, cleanTarget);

            if (canonicalTable != null) {
                // The table exists in database. Now check if the column exists in that table!
                List<String> columns = CompletionService.getCachedColumns(profile, catalog, canonicalTable, onColumnsLoaded);
                if (columns != null && !columns.isEmpty()) {
                    boolean colFound = false;
                    for (String col : columns) {
                        if (col.equalsIgnoreCase(colToken)) {
                            colFound = true;
                            break;
                        }
                    }
                    if (!colFound) {
                        diagnostics.add(new Diagnostic(
                                colStart, colEnd, colToken,
                                DiagnosticKind.UNRESOLVED_COLUMN,
                                "Unable to resolve column '" + colToken + "'",
                                "Column '" + colToken + "' does not exist in table '" + canonicalTable + "'"
                        ));
                    }
                }
            } else {
                // The qualifier refers to an alias of an unresolved table (e.g. `a` in `FROM HmsAdmission a`)
                if (aliases.containsKey(qualifier.toLowerCase(Locale.ROOT))) {
                    diagnostics.add(new Diagnostic(
                            colStart, colEnd, colToken,
                            DiagnosticKind.UNRESOLVED_COLUMN,
                            "Unable to resolve column '" + colToken + "'",
                            "Table for alias '" + qualifier + "' ('" + cleanTarget + "') is unresolved"
                    ));
                }
            }
        }

        return diagnostics;
    }

    private static String cleanIdentifier(String raw) {
        if (raw == null) return "";
        String s = raw.trim().replace("\"", "").replace("`", "").replace("[", "").replace("]", "");
        if (s.contains(".")) {
            s = s.substring(s.lastIndexOf('.') + 1);
        }
        return s;
    }

    private static String buildTableSummary(ConnectionProfile profile, String catalog, String canonicalTable) {
        StringBuilder sb = new StringBuilder();
        sb.append("Data Source: ").append(profile.getName() != null && !profile.getName().isBlank() ? profile.getName() : profile.getHost()).append("\n");
        if (catalog != null && !catalog.isBlank()) {
            sb.append("Database: ").append(catalog).append("\n");
        }
        sb.append("Schema: public\n");
        sb.append("Table: ").append(canonicalTable).append("\n\n");
        sb.append("-- auto-generated definition\n");
        sb.append("create table ").append(canonicalTable).append("\n(\n");

        List<String> cols = CompletionService.getCachedColumns(profile, catalog, canonicalTable, null);
        if (cols != null && !cols.isEmpty()) {
            for (int i = 0; i < cols.size(); i++) {
                sb.append("    ").append(cols.get(i));
                if (i < cols.size() - 1) sb.append(",");
                sb.append("\n");
            }
        } else {
            sb.append("    -- columns loading...\n");
        }
        sb.append(");");
        return sb.toString();
    }
}
