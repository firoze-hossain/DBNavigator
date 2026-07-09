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
 * keywords + table names, and columns after "tableName.".
 * Metadata is loaded once per (connection, database) in the background.
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

    /** cacheKey = profileId::catalog */
    private static final Map<String, List<String>> tableCache = new ConcurrentHashMap<>();
    /** cacheKey = profileId::catalog::table */
    private static final Map<String, List<String>> columnCache = new ConcurrentHashMap<>();

    private CompletionService() {}

    private static String key(ConnectionProfile profile, String catalog) {
        return profile.getId() + "::" + (catalog == null ? "" : catalog);
    }

    /** Kick off background loading of table names for a console's connection. */
    public static void preload(ConnectionProfile profile, String catalog) {
        String key = key(profile, catalog);
        if (tableCache.containsKey(key)) return;
        tableCache.put(key, List.of());   // marker so we only load once
        AppExecutor.run(() ->
                tableCache.put(key, MetadataService.listAllTables(profile, catalog)));
    }

    /**
     * Candidates for the token under the caret.
     * "use<caret>"        → keywords + tables starting with "use"
     * "users.na<caret>"   → columns of table users starting with "na"
     */
    public static List<String> suggest(ConnectionProfile profile, String catalog, String token) {
        if (token == null || token.isBlank()) return List.of();

        int dot = token.lastIndexOf('.');
        if (dot > 0) {
            String table = token.substring(0, dot);
            String prefix = token.substring(dot + 1).toLowerCase(Locale.ROOT);
            List<String> columns = columnsOf(profile, catalog, table);
            List<String> matches = new ArrayList<>();
            for (String col : columns) {
                if (col.toLowerCase(Locale.ROOT).startsWith(prefix)) matches.add(table + "." + col);
            }
            return matches.subList(0, Math.min(matches.size(), 15));
        }

        String prefix = token.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String table : tableCache.getOrDefault(key(profile, catalog), List.of())) {
            if (table.toLowerCase(Locale.ROOT).startsWith(prefix)) matches.add(table);
        }
        for (String keyword : KEYWORDS) {
            if (keyword.toLowerCase(Locale.ROOT).startsWith(prefix)) matches.add(keyword);
        }
        return matches.subList(0, Math.min(matches.size(), 15));
    }

    private static List<String> columnsOf(ConnectionProfile profile, String catalog, String table) {
        String cacheKey = key(profile, catalog) + "::" + table.toLowerCase(Locale.ROOT);
        List<String> cached = columnCache.get(cacheKey);
        if (cached != null) return cached;
        // Load synchronously the first time (metadata call is quick for one table),
        // then serve from cache.
        List<String> columns = MetadataService.listColumns(profile, catalog, table);
        columnCache.put(cacheKey, columns);
        return columns;
    }
}
