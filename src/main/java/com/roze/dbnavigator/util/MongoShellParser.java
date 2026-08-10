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

    /**
     * One statement as split out of the script.
     *
     * @param start        offset of the first character belonging to this
     *                     statement's own boundary (may include leading
     *                     blank lines carried over from the previous split
     *                     point)
     * @param end          offset just past the statement (at the splitting
     *                     {@code ;} or newline)
     * @param contentStart offset of the statement's actual first non-
     *                     comment, non-blank character — the right start
     *                     point for highlighting just the command itself,
     *                     not any comment line above it
     * @param text         the statement's text with comments stripped
     */
    public record RawStatement(int start, int end, int contentStart, String text) {}

    private static final Pattern USE_PATTERN =
            Pattern.compile("(?i)^use\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*;?\\s*$");
    // The optional trailing ".pretty()" is a purely cosmetic shell convenience
    // (just formats the printed output) — matched here so it can be silently
    // discarded rather than breaking the parse the way any unrecognized
    // trailing method chain would.
    private static final Pattern COMMAND_PATTERN =
            Pattern.compile("(?s)^db\\s*\\.\\s*([A-Za-z_][A-Za-z0-9_.]*)\\s*\\.\\s*"
                    + "([A-Za-z_][A-Za-z0-9_]*)\\s*\\((.*?)\\)"
                    + "(?:\\s*\\.\\s*pretty\\s*\\(\\s*\\))?\\s*;?\\s*$");

    /**
     * Splits a console's whole script into individual statements. Real Mongo
     * shell scripts routinely omit semicolons entirely, relying on newlines
     * as implicit statement boundaries the same way JavaScript's automatic
     * semicolon insertion does — so this splits on {@code ;} AND on
     * newlines, whenever either occurs at bracket depth 0 (a statement that
     * spans multiple lines, like a multi-line document literal, keeps depth
     * above 0 the whole time it's open, so a newline in the middle of one
     * doesn't split it). A comment ending mid-way through such an open
     * multi-line literal doesn't trigger a split either, for the same
     * reason. Comment text is stripped from each returned statement's
     * {@code text} entirely, not just skipped over while scanning — leaving
     * it in place would break the "use"/"db.collection.method(...)" pattern
     * match later, since a statement starting with a comment no longer
     * starts with "use" or "db" once the comment isn't (correctly) removed.
     */
    public static List<RawStatement> splitStatements(String script) {
        List<RawStatement> statements = new ArrayList<>();
        if (script == null) return statements;
        int n = script.length();
        int depth = 0;
        boolean inSingle = false, inDouble = false, inLineComment = false, inBlockComment = false;
        StringBuilder current = new StringBuilder();
        int rawStart = 0;
        int contentStart = -1;

        int i = 0;
        while (i < n) {
            char c = script.charAt(i);
            char next = i + 1 < n ? script.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                    if (depth == 0) {
                        String stmt = current.toString().strip();
                        if (!stmt.isEmpty()) {
                            statements.add(new RawStatement(rawStart, i,
                                    contentStart < 0 ? i : contentStart, stmt));
                        }
                        current.setLength(0);
                        rawStart = i + 1;
                        contentStart = -1;
                    } else {
                        current.append(c);
                    }
                    i++;
                    continue;
                }
                i++;
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && next == '/') { inBlockComment = false; i += 2; continue; }
                i++;
                continue;
            }
            if (inSingle) {
                if (c == '\\') { current.append(c).append(next); i += 2; continue; }
                if (c == '\'') inSingle = false;
                current.append(c);
                i++;
                continue;
            }
            if (inDouble) {
                if (c == '\\') { current.append(c).append(next); i += 2; continue; }
                if (c == '"') inDouble = false;
                current.append(c);
                i++;
                continue;
            }

            if (c == '/' && next == '/') { inLineComment = true; i += 2; continue; }
            if (c == '/' && next == '*') { inBlockComment = true; i += 2; continue; }
            if (Character.isWhitespace(c)) {
                if (c == '\n' && depth == 0) {
                    String stmt = current.toString().strip();
                    if (!stmt.isEmpty()) {
                        statements.add(new RawStatement(rawStart, i, contentStart < 0 ? i : contentStart, stmt));
                    }
                    current.setLength(0);
                    rawStart = i + 1;
                    contentStart = -1;
                    i++;
                    continue;
                }
                current.append(c);
                i++;
                continue;
            }

            if (contentStart < 0) contentStart = i;
            if (c == '\'') { inSingle = true; current.append(c); i++; continue; }
            if (c == '"') { inDouble = true; current.append(c); i++; continue; }
            if (c == '{' || c == '[' || c == '(') { depth++; current.append(c); i++; continue; }
            if (c == '}' || c == ']' || c == ')') { depth--; current.append(c); i++; continue; }

            if (c == ';' && depth == 0) {
                String stmt = current.toString().strip();
                if (!stmt.isEmpty()) statements.add(new RawStatement(rawStart, i, contentStart < 0 ? i : contentStart, stmt));
                current.setLength(0);
                rawStart = i + 1;
                contentStart = -1;
                i++;
                continue;
            }

            current.append(c);
            i++;
        }
        String tail = current.toString().strip();
        if (!tail.isEmpty()) statements.add(new RawStatement(rawStart, n, contentStart < 0 ? n : contentStart, tail));
        return statements;
    }

    /** The statement whose span contains the given caret offset, or null if none does. */
    public static RawStatement statementAt(List<RawStatement> statements, int caretOffset) {
        for (RawStatement s : statements) {
            if (caretOffset >= s.start() && caretOffset <= s.end()) return s;
        }
        return null;
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
