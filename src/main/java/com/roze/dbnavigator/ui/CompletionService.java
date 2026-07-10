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
 * Supplies completion candidates for the SQL console:
 * table names → column names → keywords, ranked in that order.
 * "tableName." suggests that table's columns.
 * PostgreSQL suggests parent tables only, never partition children.
 */
public final class CompletionService {

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
     * Candidates for the token under the caret:
     * "app<caret>"      → tables, then columns, then keywords starting with "app"
     * "users.na<caret>" → columns of table users starting with "na"
     */
    public static List<String> suggest(ConnectionProfile profile, String catalog, String token) {
        if (token == null || token.isBlank()) return List.of();

        int dot = token.lastIndexOf('.');
        if (dot > 0) {
            String table = token.substring(0, dot);
            String prefix = token.substring(dot + 1).toLowerCase(Locale.ROOT);
            List<String> matches = new ArrayList<>();
            for (String col : columnsOf(profile, catalog, table)) {
                if (col.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    matches.add(table + "." + col);
                    if (matches.size() >= MAX_SUGGESTIONS) break;
                }
            }
            return matches;
        }

        String prefix = token.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();

        // 1. tables first (most useful after FROM/UPDATE/JOIN)
        for (String table : tableCache.getOrDefault(key(profile, catalog), List.of())) {
            if (table.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(table);
                if (matches.size() >= MAX_SUGGESTIONS) return matches;
            }
        }
        // 2. then column names from every table (SELECT/WHERE clauses)
        for (String column : allColumnsCache.getOrDefault(key(profile, catalog), List.of())) {
            if (column.toLowerCase(Locale.ROOT).startsWith(prefix) && !matches.contains(column)) {
                matches.add(column);
                if (matches.size() >= MAX_SUGGESTIONS) return matches;
            }
        }
        // 3. keywords last
        for (String keyword : KEYWORDS) {
            if (keyword.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(keyword);
                if (matches.size() >= MAX_SUGGESTIONS) return matches;
            }
        }
        return matches;
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
