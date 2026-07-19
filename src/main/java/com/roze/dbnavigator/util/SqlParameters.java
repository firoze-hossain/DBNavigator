package com.roze.dbnavigator.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DataGrip-style named parameters: a query written with {@code :paramName}
 * placeholders can be executed after filling in each distinct parameter's
 * value once, rather than hand-editing literals into the SQL each time.
 *
 * Detection is quote/comment-aware so a token like {@code :=} inside a
 * string literal, or PostgreSQL's {@code ::type} cast (a double colon, not
 * a parameter), is never mistaken for a parameter.
 */
public final class SqlParameters {

    /** One distinct parameter, with a best-effort guess at the column it's compared against. */
    public record Parameter(String name, String guessedColumn) {}

    private static final Pattern COLUMN_COMPARISON = Pattern.compile(
            "([A-Za-z_][A-Za-z0-9_.]*)\\s*(?:=|<>|!=|>=|<=|>|<|IN)\\s*:([A-Za-z_][A-Za-z0-9_]*)",
            Pattern.CASE_INSENSITIVE);

    private SqlParameters() {}

    /** Distinct parameters in the order they first appear. */
    public static List<Parameter> detect(String sql) {
        Set<String> names = new LinkedHashSet<>();
        for (int[] range : findTokenRanges(sql)) {
            names.add(sql.substring(range[0] + 1, range[1]));
        }
        if (names.isEmpty()) return List.of();

        Map<String, String> guesses = new LinkedHashMap<>();
        Matcher matcher = COLUMN_COMPARISON.matcher(sql);
        while (matcher.find()) {
            guesses.putIfAbsent(matcher.group(2), matcher.group(1));
        }

        List<Parameter> result = new ArrayList<>();
        for (String name : names) {
            result.add(new Parameter(name, guesses.get(name)));
        }
        return result;
    }

    /** Replaces every {@code :paramName} with its typed value as a SQL literal (blank -> NULL). */
    public static String substitute(String sql, Map<String, String> values) {
        List<int[]> ranges = findTokenRanges(sql);
        if (ranges.isEmpty()) return sql;

        StringBuilder out = new StringBuilder();
        int lastEnd = 0;
        for (int[] range : ranges) {
            out.append(sql, lastEnd, range[0]);
            String name = sql.substring(range[0] + 1, range[1]);
            out.append(literal(values.get(name)));
            lastEnd = range[1];
        }
        out.append(sql.substring(lastEnd));
        return out.toString();
    }

    /** Start/end offsets of each ":paramName" token (start is the colon's index), outside quotes/comments. */
    private static List<int[]> findTokenRanges(String sql) {
        List<int[]> ranges = new ArrayList<>();
        int n = sql.length();
        boolean inSingleQuote = false, inDoubleQuote = false, inLineComment = false, inBlockComment = false;

        int i = 0;
        while (i < n) {
            char c = sql.charAt(i);
            char next = i + 1 < n ? sql.charAt(i + 1) : '\0';

            if (inLineComment) { if (c == '\n') inLineComment = false; i++; continue; }
            if (inBlockComment) { if (c == '*' && next == '/') { inBlockComment = false; i += 2; continue; } i++; continue; }
            if (inSingleQuote) { if (c == '\'' && next == '\'') { i += 2; continue; } if (c == '\'') inSingleQuote = false; i++; continue; }
            if (inDoubleQuote) { if (c == '"' && next == '"') { i += 2; continue; } if (c == '"') inDoubleQuote = false; i++; continue; }

            if (c == '-' && next == '-') { inLineComment = true; i += 2; continue; }
            if (c == '/' && next == '*') { inBlockComment = true; i += 2; continue; }
            if (c == '\'') { inSingleQuote = true; i++; continue; }
            if (c == '"') { inDoubleQuote = true; i++; continue; }

            if (c == ':' && next != ':' && (i == 0 || sql.charAt(i - 1) != ':')
                    && (Character.isLetter(next) || next == '_')) {
                int start = i;
                int j = i + 1;
                while (j < n && (Character.isLetterOrDigit(sql.charAt(j)) || sql.charAt(j) == '_')) j++;
                ranges.add(new int[]{start, j});
                i = j;
                continue;
            }
            i++;
        }
        return ranges;
    }

    private static String literal(String value) {
        if (value == null || value.isBlank()) return "NULL";
        if (value.matches("-?\\d+(\\.\\d+)?")) return value;
        return "'" + value.replace("'", "''") + "'";
    }
}
