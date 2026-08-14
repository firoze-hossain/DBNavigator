package com.roze.dbnavigator.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits a console's text into individual statements — the same "multiple
 * statements in one editor" model DataGrip consoles use. Semicolons inside
 * single/double-quoted strings, line comments (--), and block comments are
 * not treated as separators.
 *
 * PL/SQL blocks (CREATE [OR REPLACE] PROCEDURE/FUNCTION/PACKAGE [BODY]/
 * TRIGGER/TYPE [BODY], and anonymous DECLARE/BEGIN blocks) are a second,
 * genuinely different case: they contain their own internal semicolons —
 * one after nearly every line inside the block body — which are part of the
 * PL/SQL language itself, not statement separators. Splitting on every
 * semicolon the ordinary way fragments a single CREATE PROCEDURE into a
 * dozen meaningless pieces and sends each one to the database on its own,
 * none of which are valid SQL by themselves. The standard fix every real
 * Oracle tool uses (SQL*Plus, SQLcl, DataGrip) is a delimiter that means
 * something different from a semicolon: a "/" alone on its own line marks
 * the actual end of a PL/SQL block. This splitter recognizes that same
 * convention — once a statement is detected as starting a PL/SQL block, its
 * internal semicolons are left alone, and only a standalone "/" line ends
 * it (the "/" itself isn't included in the statement text sent to the
 * database, matching what SQL*Plus does with it).
 */
public final class SqlStatementSplitter {

    /**
     * One statement and the character range in the ORIGINAL text it spans.
     * plsqlBlock is true when this was ended by a standalone "/" rather
     * than a ";" — callers need this because a PL/SQL block's own trailing
     * semicolon (the one right after its final END) is mandatory PL/SQL
     * grammar, not a statement separator, and must never be stripped the
     * way a plain statement's separator semicolon is before execution.
     */
    public record Statement(String text, int start, int end, boolean plsqlBlock) {
        public boolean contains(int caretPosition) {
            return caretPosition >= start && caretPosition <= end;
        }
    }

    private static final Pattern PLSQL_BLOCK_START = Pattern.compile(
            "(CREATE(\\s+OR\\s+REPLACE)?\\s+(PROCEDURE|FUNCTION|PACKAGE\\s+BODY|PACKAGE|TRIGGER|" +
            "TYPE\\s+BODY|TYPE)\\b|DECLARE\\b|BEGIN\\b)",
            Pattern.CASE_INSENSITIVE);

    private SqlStatementSplitter() {}

    public static List<Statement> split(String sql) {
        List<Statement> statements = new ArrayList<>();
        if (sql == null || sql.isEmpty()) return statements;

        int n = sql.length();
        int stmtStart = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean inPlsqlBlock = looksLikePlsqlBlockStart(sql, stmtStart);

        int i = 0;
        while (i < n) {
            char c = sql.charAt(i);
            char next = i + 1 < n ? sql.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (c == '\n') inLineComment = false;
                i++;
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && next == '/') { inBlockComment = false; i += 2; continue; }
                i++;
                continue;
            }
            if (inSingleQuote) {
                if (c == '\'' && next == '\'') { i += 2; continue; }   // escaped quote
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

            if (inPlsqlBlock) {
                if (c == '/' && isStandaloneSlashAt(sql, i)) {
                    addIfNotBlank(statements, sql, stmtStart, i, true);
                    stmtStart = i + 1;
                    inPlsqlBlock = looksLikePlsqlBlockStart(sql, stmtStart);
                }
                i++;
                continue;
            }

            if (c == ';') {
                addIfNotBlank(statements, sql, stmtStart, i + 1, false);
                stmtStart = i + 1;
                inPlsqlBlock = looksLikePlsqlBlockStart(sql, stmtStart);
            }
            i++;
        }
        addIfNotBlank(statements, sql, stmtStart, n, inPlsqlBlock);
        return statements;
    }

    /** Skips leading whitespace and comments, then checks for a PL/SQL block-starting keyword. */
    private static boolean looksLikePlsqlBlockStart(String sql, int from) {
        int pos = skipLeadingTrivia(sql, from);
        return pos < sql.length() && PLSQL_BLOCK_START.matcher(sql).region(pos, sql.length()).lookingAt();
    }

    private static int skipLeadingTrivia(String sql, int pos) {
        int n = sql.length();
        while (pos < n) {
            char c = sql.charAt(pos);
            if (Character.isWhitespace(c)) { pos++; continue; }
            if (c == '-' && pos + 1 < n && sql.charAt(pos + 1) == '-') {
                int nl = sql.indexOf('\n', pos);
                pos = (nl == -1) ? n : nl + 1;
                continue;
            }
            if (c == '/' && pos + 1 < n && sql.charAt(pos + 1) == '*') {
                int end = sql.indexOf("*/", pos + 2);
                pos = (end == -1) ? n : end + 2;
                continue;
            }
            break;
        }
        return pos;
    }

    /** True when the "/" at slashPos is the only non-whitespace character on its line. */
    private static boolean isStandaloneSlashAt(String sql, int slashPos) {
        int lineStart = sql.lastIndexOf('\n', slashPos - 1) + 1;   // 0 if no earlier newline
        for (int k = lineStart; k < slashPos; k++) {
            if (!Character.isWhitespace(sql.charAt(k))) return false;
        }
        int lineEnd = sql.indexOf('\n', slashPos + 1);
        if (lineEnd == -1) lineEnd = sql.length();
        for (int k = slashPos + 1; k < lineEnd; k++) {
            if (!Character.isWhitespace(sql.charAt(k))) return false;
        }
        return true;
    }

    private static void addIfNotBlank(List<Statement> statements, String sql, int start, int end,
                                      boolean plsqlBlock) {
        String text = sql.substring(start, end);
        if (text.strip().isEmpty()) return;
        statements.add(new Statement(text, start, end, plsqlBlock));
    }

    /** The statement whose range contains the caret, or null if the text is empty/whitespace-only there. */
    public static Statement statementAt(List<Statement> statements, int caretPosition) {
        for (Statement s : statements) {
            if (s.contains(caretPosition)) return s;
        }
        return null;
    }
}
