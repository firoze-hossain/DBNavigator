package com.roze.dbnavigator.ui;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight SQL formatter for reformatting queries in the SQL editor.
 * Formats statements with indentation and standard keyword capitalization.
 */
public final class SqlFormatter {

    private static final Set<String> MAJOR_CLAUSES = new HashSet<>(Arrays.asList(
            "SELECT", "FROM", "WHERE", "GROUP BY", "HAVING", "ORDER BY",
            "INSERT INTO", "VALUES", "UPDATE", "SET", "DELETE FROM", "DELETE",
            "LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "OUTER JOIN", "CROSS JOIN",
            "JOIN", "LIMIT", "OFFSET", "UNION ALL", "UNION", "EXCEPT", "INTERSECT",
            "CREATE TABLE", "ALTER TABLE", "DROP TABLE", "CREATE VIEW", "CREATE INDEX"
    ));

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
            "DELETE", "CREATE", "ALTER", "DROP", "TABLE", "VIEW", "INDEX", "SEQUENCE",
            "DATABASE", "SCHEMA", "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "OUTER",
            "CROSS", "ON", "GROUP", "BY", "ORDER", "HAVING", "LIMIT", "OFFSET", "UNION",
            "ALL", "DISTINCT", "AS", "AND", "OR", "NOT", "NULL", "IS", "IN", "BETWEEN",
            "LIKE", "EXISTS", "CASE", "WHEN", "THEN", "ELSE", "END", "PRIMARY", "KEY",
            "FOREIGN", "REFERENCES", "DEFAULT", "UNIQUE", "CONSTRAINT", "ADD", "COLUMN",
            "TRUNCATE", "BEGIN", "COMMIT", "ROLLBACK", "TRANSACTION", "GRANT", "REVOKE",
            "WITH", "RETURNING", "IF", "REPLACE", "SHOW", "DESCRIBE", "EXPLAIN", "USE",
            "ASC", "DESC", "COUNT", "SUM", "AVG", "MIN", "MAX", "COALESCE"
    ));

    private static final String INDENT = "    ";

    private SqlFormatter() {}

    /**
     * Formats the given SQL string with clean indentation and keyword casing.
     */
    public static String format(String sql) {
        if (sql == null || sql.isBlank()) return sql;

        // Tokenize while preserving strings, comments, and symbols
        Pattern tokenPattern = Pattern.compile(
                "(\"(?:[^\"]|\"\")*\")|" +          // Double-quoted identifier
                "('(?:[^']|'')*')|" +               // Single-quoted literal
                "(--[^\\r\\n]*)|" +                 // Line comment
                "(/\\*[\\s\\S]*?\\*/)|" +           // Block comment
                "([A-Za-z_][A-Za-z0-9_]*)|" +       // Word / Identifier
                "(\\S)"                             // Symbol / Operator
        );

        Matcher m = tokenPattern.matcher(sql);
        StringBuilder out = new StringBuilder();
        int indentLevel = 0;
        boolean newlinePending = false;
        String prevToken = "";

        while (m.find()) {
            String token = m.group();
            String upper = token.toUpperCase();

            // Line or block comment
            if (token.startsWith("--") || token.startsWith("/*")) {
                if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') {
                    out.append(" ");
                }
                out.append(token).append("\n");
                newlinePending = true;
                prevToken = token;
                continue;
            }

            // String literal
            if (token.startsWith("'") || token.startsWith("\"")) {
                if (newlinePending) {
                    appendIndent(out, indentLevel);
                    newlinePending = false;
                } else if (needsSpaceBefore(token, prevToken)) {
                    out.append(" ");
                }
                out.append(token);
                prevToken = token;
                continue;
            }

            // Semicolon starts fresh statement
            if (";".equals(token)) {
                out.append(";\n\n");
                indentLevel = 0;
                newlinePending = true;
                prevToken = ";";
                continue;
            }

            // Parentheses
            if ("(".equals(token)) {
                if (newlinePending) {
                    appendIndent(out, indentLevel);
                    newlinePending = false;
                } else if (needsSpaceBefore(token, prevToken)) {
                    out.append(" ");
                }
                out.append("(");
                indentLevel++;
                prevToken = "(";
                continue;
            }

            if (")".equals(token)) {
                indentLevel = Math.max(0, indentLevel - 1);
                out.append(")");
                prevToken = ")";
                continue;
            }

            // Comma
            if (",".equals(token)) {
                out.append(",");
                if (indentLevel <= 1) {
                    out.append("\n");
                    newlinePending = true;
                } else {
                    out.append(" ");
                }
                prevToken = ",";
                continue;
            }

            // Major SQL clause keywords
            if (MAJOR_CLAUSES.contains(upper) || isMajorClause(upper)) {
                if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') {
                    out.append("\n");
                }
                appendIndent(out, indentLevel);
                out.append(upper);
                newlinePending = false;
                prevToken = upper;
                continue;
            }

            // Standard keywords
            if (KEYWORDS.contains(upper)) {
                token = upper;
            }

            if (newlinePending) {
                appendIndent(out, indentLevel);
                newlinePending = false;
            } else if (needsSpaceBefore(token, prevToken)) {
                out.append(" ");
            }

            out.append(token);
            prevToken = token;
        }

        return out.toString().stripTrailing();
    }

    private static boolean isMajorClause(String upper) {
        return "GROUP".equals(upper) || "ORDER".equals(upper);
    }

    private static boolean needsSpaceBefore(String token, String prevToken) {
        if (prevToken.isEmpty() || "(".equals(prevToken)) return false;
        if (",".equals(prevToken)) return true;
        if (".".equals(token) || ".".equals(prevToken)) return false;
        if (":".equals(token) || ":".equals(prevToken)) return false;
        return true;
    }

    private static void appendIndent(StringBuilder sb, int level) {
        for (int i = 0; i < level; i++) {
            sb.append(INDENT);
        }
    }
}
