package com.roze.dbnavigator.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the common subset of MongoDB shell syntax: {@code use dbname;} and
 * {@code db.collection.method(args);}. Arguments are written the way the
 * real Mongo shell accepts them — unquoted object keys, single or double
 * quoted strings — not strict JSON, so this also converts that lenient
 * syntax into real JSON that {@code org.bson.Document.parse()} can consume.
 */
public final class MongoShellParser {

    private MongoShellParser() {}

    public enum Kind { USE, COMMAND, UNKNOWN }

    /** One parsed statement. For USE, {@code database} is set; for COMMAND, everything else is. */
    public record Statement(Kind kind, String database, String collection, String method,
                            String argsJson, String raw) {}

    private static final Pattern USE_PATTERN =
            Pattern.compile("(?i)^use\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*;?\\s*$");
    private static final Pattern COMMAND_PATTERN =
            Pattern.compile("(?s)^db\\s*\\.\\s*([A-Za-z_][A-Za-z0-9_.]*)\\s*\\.\\s*"
                    + "([A-Za-z_][A-Za-z0-9_]*)\\s*\\((.*)\\)\\s*;?\\s*$");

    /** Splits a console's whole script into individual statements, respecting nested {}/[]/strings/comments. */
    public static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        if (script == null) return statements;
        int n = script.length();
        int depth = 0;
        int start = 0;
        boolean inSingle = false, inDouble = false, inLineComment = false, inBlockComment = false;

        int i = 0;
        while (i < n) {
            char c = script.charAt(i);
            char next = i + 1 < n ? script.charAt(i + 1) : '\0';

            if (inLineComment) { if (c == '\n') inLineComment = false; i++; continue; }
            if (inBlockComment) { if (c == '*' && next == '/') { inBlockComment = false; i += 2; continue; } i++; continue; }
            if (inSingle) { if (c == '\\') { i += 2; continue; } if (c == '\'') inSingle = false; i++; continue; }
            if (inDouble) { if (c == '\\') { i += 2; continue; } if (c == '"') inDouble = false; i++; continue; }

            if (c == '/' && next == '/') { inLineComment = true; i += 2; continue; }
            if (c == '/' && next == '*') { inBlockComment = true; i += 2; continue; }
            if (c == '\'') { inSingle = true; i++; continue; }
            if (c == '"') { inDouble = true; i++; continue; }
            if (c == '{' || c == '[' || c == '(') { depth++; i++; continue; }
            if (c == '}' || c == ']' || c == ')') { depth--; i++; continue; }

            if (c == ';' && depth == 0) {
                String stmt = script.substring(start, i).strip();
                if (!stmt.isEmpty()) statements.add(stmt);
                start = i + 1;
            }
            i++;
        }
        String tail = script.substring(start).strip();
        if (!tail.isEmpty()) statements.add(tail);
        return statements;
    }

    public static Statement parse(String statement) {
        String trimmed = statement.strip();

        Matcher useMatch = USE_PATTERN.matcher(trimmed);
        if (useMatch.matches()) {
            return new Statement(Kind.USE, useMatch.group(1), null, null, null, trimmed);
        }

        Matcher cmdMatch = COMMAND_PATTERN.matcher(trimmed);
        if (cmdMatch.matches()) {
            String collection = cmdMatch.group(1);
            String method = cmdMatch.group(2);
            String rawArgs = cmdMatch.group(3);
            return new Statement(Kind.COMMAND, null, collection, method, toStrictJsonArgs(rawArgs), trimmed);
        }

        return new Statement(Kind.UNKNOWN, null, null, null, null, trimmed);
    }

    /**
     * Converts the argument text between a method's parentheses — which may
     * be one document, several comma-separated documents/values, or an
     * array — from lenient shell syntax to strict JSON. Wraps the whole
     * thing in a JSON array so it always parses as one list of top-level
     * arguments, which the caller then splits back apart.
     */
    private static String toStrictJsonArgs(String rawArgs) {
        return "[" + toStrictJson(rawArgs) + "]";
    }

    /**
     * The actual lenient-to-strict conversion: walks the text once, copying
     * string literals through untouched (just normalizing the quote
     * character and escaping as needed) and quoting any bare identifier
     * that's immediately followed by a colon — the "object key" pattern —
     * wherever it occurs, regardless of nesting depth. Numbers, true/false/
     * null, and punctuation pass through unchanged.
     */
    public static String toStrictJson(String lenient) {
        StringBuilder out = new StringBuilder();
        int n = lenient.length();
        int i = 0;
        while (i < n) {
            char c = lenient.charAt(i);

            if (c == '\'' || c == '"') {
                char quote = c;
                StringBuilder content = new StringBuilder();
                int j = i + 1;
                while (j < n && lenient.charAt(j) != quote) {
                    if (lenient.charAt(j) == '\\' && j + 1 < n) {
                        content.append(lenient.charAt(j)).append(lenient.charAt(j + 1));
                        j += 2;
                    } else {
                        content.append(lenient.charAt(j));
                        j++;
                    }
                }
                out.append('"');
                out.append(quote == '\'' ? content.toString().replace("\"", "\\\"") : content.toString());
                out.append('"');
                i = Math.min(j + 1, n);
                continue;
            }

            if (Character.isLetter(c) || c == '_' || c == '$') {
                int j = i;
                while (j < n && (Character.isLetterOrDigit(lenient.charAt(j))
                        || lenient.charAt(j) == '_' || lenient.charAt(j) == '$')) {
                    j++;
                }
                String ident = lenient.substring(i, j);
                int k = j;
                while (k < n && Character.isWhitespace(lenient.charAt(k))) k++;
                boolean isKey = k < n && lenient.charAt(k) == ':';
                if (isKey) {
                    out.append('"').append(ident).append('"');
                } else {
                    out.append(ident);
                }
                i = j;
                continue;
            }

            out.append(c);
            i++;
        }
        return out.toString();
    }
}
