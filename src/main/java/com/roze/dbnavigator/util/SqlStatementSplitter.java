package com.roze.dbnavigator.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a console's text into individual statements on top-level semicolons
 * -- the same "multiple statements in one editor" model DataGrip consoles
 * use. Semicolons inside single/double-quoted strings, line comments (--),
 * and block comments are not treated as separators.
 */
public final class SqlStatementSplitter {

    /** One statement and the character range in the ORIGINAL text it spans. */
    public record Statement(String text, int start, int end) {
        public boolean contains(int caretPosition) {
            return caretPosition >= start && caretPosition <= end;
        }
    }

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

            if (c == ';') {
                addIfNotBlank(statements, sql, stmtStart, i + 1);
                stmtStart = i + 1;
            }
            i++;
        }
        addIfNotBlank(statements, sql, stmtStart, n);
        return statements;
    }

    private static void addIfNotBlank(List<Statement> statements, String sql, int start, int end) {
        String text = sql.substring(start, end);
        if (text.strip().isEmpty()) return;
        statements.add(new Statement(text, start, end));
    }

    /** The statement whose range contains the caret, or null if the text is empty/whitespace-only there. */
    public static Statement statementAt(List<Statement> statements, int caretPosition) {
        for (Statement s : statements) {
            if (s.contains(caretPosition)) return s;
        }
        return null;
    }
}
