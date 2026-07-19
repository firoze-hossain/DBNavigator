package com.roze.dbnavigator.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A basic, real (not decorative) SQL reformatter: puts each major clause on
 * its own line with consistent indentation. This is a heuristic line-breaker,
 * not a full parser-driven pretty-printer — it won't perfectly handle every
 * nested subquery or exotic construct, but for typical SELECT/UPDATE/INSERT/
 * DELETE statements it produces a genuinely more readable layout.
 */
public final class SqlReformatter {

    private static final List<String> MAJOR_CLAUSES = List.of(
            "SELECT", "FROM", "WHERE", "GROUP BY", "HAVING", "ORDER BY", "LIMIT", "OFFSET",
            "INNER JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN", "JOIN",
            "UNION ALL", "UNION", "VALUES", "SET", "ON CONFLICT");

    private static final Pattern CLAUSE_PATTERN;
    static {
        // Longest clauses first so e.g. "LEFT JOIN" matches before bare "JOIN"
        List<String> sorted = new java.util.ArrayList<>(MAJOR_CLAUSES);
        sorted.sort((a, b) -> b.length() - a.length());
        StringBuilder alternation = new StringBuilder();
        for (String clause : sorted) {
            if (alternation.length() > 0) alternation.append('|');
            alternation.append(clause.replace(" ", "\\s+"));
        }
        CLAUSE_PATTERN = Pattern.compile("(?i)\\b(" + alternation + ")\\b");
    }

    private SqlReformatter() {}

    public static String reformat(String sql) {
        if (sql == null || sql.isBlank()) return sql;

        // Normalize whitespace first so the clause matcher sees clean text
        String collapsed = sql.replaceAll("\\s+", " ").strip();

        StringBuilder out = new StringBuilder();
        Matcher matcher = CLAUSE_PATTERN.matcher(collapsed);
        int lastEnd = 0;
        boolean first = true;
        while (matcher.find()) {
            String before = collapsed.substring(lastEnd, matcher.start()).strip();
            if (!before.isEmpty()) {
                out.append(first ? "" : "  ").append(before);
            }
            if (!first) out.append('\n');
            String clause = normalizeClauseCasing(matcher.group(1));
            out.append(clause).append(' ');
            lastEnd = matcher.end();
            first = false;
        }
        String tail = collapsed.substring(lastEnd).strip();
        if (!tail.isEmpty()) out.append(tail);

        return breakOnCommaLists(out.toString());
    }

    /** Long comma-separated SELECT lists and VALUES lists get one item per line. */
    private static String breakOnCommaLists(String text) {
        String[] lines = text.split("\n");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (i > 0) result.append('\n');
            if (line.length() > 100 && line.contains(",")) {
                String[] parts = line.split(",");
                for (int p = 0; p < parts.length; p++) {
                    if (p > 0) result.append(",\n    ");
                    result.append(parts[p].strip());
                }
            } else {
                result.append(line);
            }
        }
        return result.toString();
    }

    private static String normalizeClauseCasing(String clause) {
        return clause.toUpperCase(java.util.Locale.ROOT).replaceAll("\\s+", " ");
    }
}
