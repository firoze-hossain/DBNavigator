package com.roze.dbnavigator.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves table aliases from a query's FROM/JOIN clauses — e.g. given
 * {@code FROM bcharge b JOIN bbill bb ON bb.id = b.invoice_id}, this maps
 * {@code b -> bcharge} and {@code bb -> bbill}, so autocomplete can offer
 * the real table's columns after typing {@code b.} or {@code bb.}, not just
 * after typing a full table name.
 */
public final class SqlAliases {

    private static final Pattern FROM_OR_JOIN = Pattern.compile(
            "(?i)\\b(?:FROM|JOIN)\\s+([A-Za-z_][A-Za-z0-9_.]*)\\s+(?:AS\\s+)?([A-Za-z_][A-Za-z0-9_]*)\\b");

    private static final Pattern FROM_OR_JOIN_TABLE = Pattern.compile(
            "(?i)\\b(?:FROM|JOIN)\\s+([A-Za-z_][A-Za-z0-9_.\\\"]*)(?:\\s+(?:AS\\s+)?([A-Za-z_][A-Za-z0-9_]*))?\\b");

    // Words that can legally follow a table name without being an alias
    // (e.g. "FROM users WHERE ..." must not treat WHERE as an alias for users).
    private static final Set<String> NOT_AN_ALIAS = Set.of(
            "where", "on", "group", "order", "having", "limit", "offset",
            "left", "right", "inner", "outer", "full", "cross", "join",
            "union", "set", "values", "as", "and", "or");

    private SqlAliases() {}

    /** alias (lowercased) -> real table's simple name (schema/catalog prefix stripped). */
    public static Map<String, String> resolve(String sql) {
        Map<String, String> aliases = new LinkedHashMap<>();
        if (sql == null || sql.isBlank()) return aliases;

        Matcher matcher = FROM_OR_JOIN.matcher(sql);
        while (matcher.find()) {
            String table = matcher.group(1);
            String alias = matcher.group(2);
            if (NOT_AN_ALIAS.contains(alias.toLowerCase(Locale.ROOT))) continue;

            String simpleName = table.contains(".") ? table.substring(table.lastIndexOf('.') + 1) : table;
            aliases.putIfAbsent(alias.toLowerCase(Locale.ROOT), simpleName);
        }
        return aliases;
    }

    /**
     * Returns the list of referenced table names from FROM/JOIN clauses, useful
     * for scoped autocomplete in queries that already reference one or more tables.
     */
    public static List<String> referencedTables(String sql) {
        Set<String> tables = new LinkedHashSet<>();
        if (sql == null || sql.isBlank()) return List.of();

        Matcher matcher = FROM_OR_JOIN_TABLE.matcher(sql);
        while (matcher.find()) {
            String table = matcher.group(1);
            String simpleName = table.contains(".") ? table.substring(table.lastIndexOf('.') + 1) : table;
            simpleName = simpleName.replace("\"", "");
            tables.add(simpleName);
        }
        return new ArrayList<>(tables);
    }
}
