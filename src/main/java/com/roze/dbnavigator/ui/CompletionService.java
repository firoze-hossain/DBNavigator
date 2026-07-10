package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.util.AppExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DataGrip-style contextual completion:
 *   after FROM / JOIN / UPDATE / INTO  → tables
 *   after SELECT / WHERE / ON / SET …  → columns first
 *   "tableName."                       → that table's columns
 * Each suggestion carries a kind + description so the popup can color it.
 */
public final class CompletionService {

    public enum Kind { KEYWORD, TABLE, COLUMN }

    /** What the previous word tells us the user is about to type. */
    public enum Context { ANY, TABLES, COLUMNS }

    public record Suggestion(String text, Kind kind, String detail) {
        @Override public String toString() { return text; }
    }

    private static final List<String> KEYWORDS = List.of(
            "SELECT", "FROM", "WHERE", "INSERT INTO", "VALUES", "UPDATE", "SET",
            "DELETE FROM", "CREATE TABLE", "ALTER TABLE", "DROP TABLE", "CREATE INDEX",
            "JOIN", "INNER JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN", "ON",
            "GROUP BY", "ORDER BY", "HAVING", "LIMIT", "OFFSET", "UNION", "DISTINCT",
            "AND", "OR", "NOT", "NULL", "IS NULL", "IS NOT NULL", "IN", "BETWEEN",
            "LIKE", "EXISTS", "CASE", "WHEN", "THEN", "ELSE", "END", "AS",
            "COUNT(*)", "SUM(", "AVG(", "MIN(", "MAX(", "COALESCE(", "CAST(",
            "PRIMARY KEY", "FOREIGN KEY", "REFERENCES", "DEFAULT", "UNIQUE",
            "BEGIN", "COMMIT", "ROLLBACK", "TRUNCATE TABLE", "ASC", "DESC");

    private static final int MAX_SUGGESTIONS = 20;

    /** cacheKey = profileId::catalog */
    private static final Map<String, List<String>> tableCache = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> allColumnsCache = new ConcurrentHashMap<>();
    /** cacheKey = profileId::catalog::table */
    private static final Map<String, List<String>> columnCache = new ConcurrentHashMap<>();

    private CompletionService() {}

    private static String key(ConnectionProfile profile, String catalog) {
        return profile.getId() + "::" + (catalog == null ? "" : catalog);
    }

    /** Kick off background loading of table and column names for a console. */
    public static void preload(ConnectionProfile profile, String catalog) {
        String key = key(profile, catalog);
        if (tableCache.containsKey(key)) return;
        tableCache.put(key, List.of());        // marker so we only load once
        allColumnsCache.put(key, List.of());
        AppExecutor.run(() -> {
            tableCache.put(key, MetadataService.listAllTables(profile, catalog));
            allColumnsCache.put(key, MetadataService.listAllColumns(profile, catalog));
        });
    }

    /**
     * Ranked suggestions for the token under the caret.
     * Empty tokens are allowed when the context already narrows the answer
     * (e.g. right after "FROM " every table is a valid suggestion).
     */
    public static List<Suggestion> suggest(ConnectionProfile profile, String catalog,
                                           String token, Context context) {
        if (token == null) token = "";

        // "users.na" → columns of users
        int dot = token.lastIndexOf('.');
        if (dot > 0) {
            String table = token.substring(0, dot);
            String prefix = token.substring(dot + 1).toLowerCase(Locale.ROOT);
            List<Suggestion> matches = new ArrayList<>();
            for (String col : columnsOf(profile, catalog, table)) {
                if (col.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    matches.add(new Suggestion(table + "." + col, Kind.COLUMN, "column of " + table));
                    if (matches.size() >= MAX_SUGGESTIONS) break;
                }
            }
            return matches;
        }

        if (token.isBlank() && context == Context.ANY) return List.of();

        String prefix = token.toLowerCase(Locale.ROOT);
        List<Suggestion> matches = new ArrayList<>();

        switch (context) {
            case TABLES -> {
                addTables(profile, catalog, prefix, matches);
                addKeywords(prefix, matches);      // e.g. FROM (SELECT …
            }
            case COLUMNS -> {
                addColumns(profile, catalog, prefix, matches);
                addKeywords(prefix, matches);
                addTables(profile, catalog, prefix, matches);
            }
            case ANY -> {
                addTables(profile, catalog, prefix, matches);
                addColumns(profile, catalog, prefix, matches);
                addKeywords(prefix, matches);
            }
        }
        return matches;
    }

    private static void addTables(ConnectionProfile profile, String catalog,
                                  String prefix, List<Suggestion> matches) {
        for (String table : tableCache.getOrDefault(key(profile, catalog), List.of())) {
            if (matches.size() >= MAX_SUGGESTIONS) return;
            if (table.toLowerCase(Locale.ROOT).startsWith(prefix) && notPresent(matches, table)) {
                matches.add(new Suggestion(table, Kind.TABLE, "table"));
            }
        }
    }

    private static void addColumns(ConnectionProfile profile, String catalog,
                                   String prefix, List<Suggestion> matches) {
        for (String column : allColumnsCache.getOrDefault(key(profile, catalog), List.of())) {
            if (matches.size() >= MAX_SUGGESTIONS) return;
            if (column.toLowerCase(Locale.ROOT).startsWith(prefix) && notPresent(matches, column)) {
                matches.add(new Suggestion(column, Kind.COLUMN, "column"));
            }
        }
    }

    private static void addKeywords(String prefix, List<Suggestion> matches) {
        if (prefix.isBlank()) return;              // don't flood with all keywords
        for (String keyword : KEYWORDS) {
            if (matches.size() >= MAX_SUGGESTIONS) return;
            if (keyword.toLowerCase(Locale.ROOT).startsWith(prefix) && notPresent(matches, keyword)) {
                matches.add(new Suggestion(keyword, Kind.KEYWORD, "keyword"));
            }
        }
    }

    private static boolean notPresent(List<Suggestion> matches, String text) {
        return matches.stream().noneMatch(s -> s.text().equalsIgnoreCase(text));
    }

    private static List<String> columnsOf(ConnectionProfile profile, String catalog, String table) {
        String cacheKey = key(profile, catalog) + "::" + table.toLowerCase(Locale.ROOT);
        List<String> cached = columnCache.get(cacheKey);
        if (cached != null) return cached;
        List<String> columns = MetadataService.listColumns(profile, catalog, table);
        columnCache.put(cacheKey, columns);
        return columns;
    }
}
