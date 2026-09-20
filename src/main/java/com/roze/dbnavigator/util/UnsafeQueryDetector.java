package com.roze.dbnavigator.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects potentially destructive or unsafe SQL queries (e.g. DELETE or UPDATE
 * without a WHERE clause, or DROP / TRUNCATE operations), allowing DBNavigator
 * to prompt for user confirmation before execution matching DataGrip's
 * "Show warning before running potentially unsafe queries" setting.
 */
public final class UnsafeQueryDetector {

    public record UnsafeWarning(String title, String description) {}

    private static final Pattern DELETE_WITHOUT_WHERE = Pattern.compile(
            "^\\s*DELETE\\s+FROM\\s+([A-Za-z0-9_.\"]+)(?!.*\\bWHERE\\b)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern UPDATE_WITHOUT_WHERE = Pattern.compile(
            "^\\s*UPDATE\\s+([A-Za-z0-9_.\"]+)\\s+SET\\s+(?!.*\\bWHERE\\b)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern DROP_OPERATION = Pattern.compile(
            "^\\s*DROP\\s+(TABLE|DATABASE|SCHEMA|VIEW)\\s+([A-Za-z0-9_.\"]+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TRUNCATE_OPERATION = Pattern.compile(
            "^\\s*TRUNCATE\\s+(?:TABLE\\s+)?([A-Za-z0-9_.\"]+)",
            Pattern.CASE_INSENSITIVE);

    private UnsafeQueryDetector() {}

    /**
     * Checks if the given SQL statement is potentially unsafe.
     * Returns an {@link UnsafeWarning} describing the danger, or null if safe.
     */
    public static UnsafeWarning check(String sql) {
        if (sql == null || sql.isBlank()) return null;

        // Strip comments and string literals to prevent false matches/misses
        String sanitized = stripCommentsAndStrings(sql).strip();

        Matcher delMatch = DELETE_WITHOUT_WHERE.matcher(sanitized);
        if (delMatch.find()) {
            String table = delMatch.group(1);
            return new UnsafeWarning(
                    "DELETE Without WHERE Clause",
                    "This statement will delete ALL rows in table '" + table + "'.\nAre you sure you want to execute it?");
        }

        Matcher updMatch = UPDATE_WITHOUT_WHERE.matcher(sanitized);
        if (updMatch.find()) {
            String table = updMatch.group(1);
            return new UnsafeWarning(
                    "UPDATE Without WHERE Clause",
                    "This statement will update ALL rows in table '" + table + "'.\nAre you sure you want to execute it?");
        }

        Matcher dropMatch = DROP_OPERATION.matcher(sanitized);
        if (dropMatch.find()) {
            String objType = dropMatch.group(1).toUpperCase(Locale.ROOT);
            String name = dropMatch.group(2);
            return new UnsafeWarning(
                    "DROP " + objType,
                    "This statement will drop the " + objType.toLowerCase(Locale.ROOT) + " '" + name + "'.\nThis action cannot be undone. Are you sure?");
        }

        Matcher truncMatch = TRUNCATE_OPERATION.matcher(sanitized);
        if (truncMatch.find()) {
            String table = truncMatch.group(1);
            return new UnsafeWarning(
                    "TRUNCATE TABLE",
                    "This statement will remove all rows in table '" + table + "'.\nAre you sure you want to execute it?");
        }

        return null;
    }

    public static List<String> detectUnsafeQueries(List<String> statements) {
        List<String> unsafe = new java.util.ArrayList<>();
        if (statements == null) return unsafe;
        for (String stmt : statements) {
            UnsafeWarning w = check(stmt);
            if (w != null) {
                unsafe.add(stmt);
            }
        }
        return unsafe;
    }

    public static List<UnsafeWarning> detectUnsafe(List<String> statements) {
        List<UnsafeWarning> warnings = new java.util.ArrayList<>();
        if (statements == null) return warnings;
        for (String stmt : statements) {
            UnsafeWarning w = check(stmt);
            if (w != null) {
                warnings.add(w);
            }
        }
        return warnings;
    }

    private static String stripCommentsAndStrings(String sql) {
        StringBuilder sb = new StringBuilder();
        int n = sql.length();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        int i = 0;
        while (i < n) {
            char c = sql.charAt(i);
            char next = i + 1 < n ? sql.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (c == '\n') { inLineComment = false; sb.append('\n'); }
                i++;
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && next == '/') { inBlockComment = false; i += 2; continue; }
                i++;
                continue;
            }
            if (inSingleQuote) {
                if (c == '\'' && next == '\'') { i += 2; continue; }
                if (c == '\'') inSingleQuote = false;
                i++;
                continue;
            }
            if (inDoubleQuote) {
                if (c == '"' && next == '"') { i += 2; continue; }
                if (c == '"') inDoubleQuote = false;
                i++;
                continue;
            }

            if (c == '-' && next == '-') { inLineComment = true; i += 2; continue; }
            if (c == '/' && next == '*') { inBlockComment = true; i += 2; continue; }
            if (c == '\'') { inSingleQuote = true; i++; continue; }
            if (c == '"') { inDoubleQuote = true; i++; continue; }

            sb.append(c);
            i++;
        }
        return sb.toString();
    }
}
