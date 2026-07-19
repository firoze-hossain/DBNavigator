package com.roze.dbnavigator.ui;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Regex-based SQL syntax highlighting for a RichTextFX CodeArea. */
public final class SqlHighlighter {

    private static final String[] KEYWORDS = {
            "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
            "DELETE", "CREATE", "ALTER", "DROP", "TABLE", "VIEW", "INDEX", "DATABASE",
            "SCHEMA", "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "OUTER", "CROSS", "ON",
            "GROUP", "BY", "ORDER", "HAVING", "LIMIT", "OFFSET", "UNION", "ALL",
            "DISTINCT", "AS", "AND", "OR", "NOT", "NULL", "IS", "IN", "BETWEEN",
            "LIKE", "EXISTS", "CASE", "WHEN", "THEN", "ELSE", "END", "PRIMARY", "KEY",
            "FOREIGN", "REFERENCES", "DEFAULT", "UNIQUE", "CONSTRAINT", "ADD", "COLUMN",
            "TRUNCATE", "BEGIN", "COMMIT", "ROLLBACK", "TRANSACTION", "GRANT", "REVOKE",
            "WITH", "RETURNING", "IF", "REPLACE", "SHOW", "DESCRIBE", "EXPLAIN", "USE",
            "ASC", "DESC"
    };

    /** Data-type names get their own color, distinct from control-flow keywords. */
    private static final String[] TYPES = {
            "INT", "INTEGER", "BIGINT", "SMALLINT", "TINYINT", "VARCHAR", "CHAR", "TEXT",
            "DATE", "TIME", "TIMESTAMP", "BOOLEAN", "BOOL", "DECIMAL", "NUMERIC",
            "FLOAT", "DOUBLE", "REAL", "SERIAL", "BIGSERIAL", "UUID", "JSON", "JSONB", "BYTEA"
    };

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>\\b(?i:" + String.join("|", KEYWORDS) + ")\\b)"
            + "|(?<TYPE>\\b(?i:" + String.join("|", TYPES) + ")\\b)"
            // :paramName (DataGrip-style parameter), but never the two colons of a ::cast
            + "|(?<PARAMETER>(?<!:):(?!:)[A-Za-z_][A-Za-z0-9_]*)"
            // any identifier immediately followed by an open parenthesis is a
            // function call — generic on purpose, so it also colors
            // user-defined functions, not just a fixed built-in list
            + "|(?<FUNCTION>\\b[A-Za-z_][A-Za-z0-9_]*\\b(?=\\s*\\())"
            // any identifier immediately followed by a dot is a table
            // alias/reference — e.g. the "b" in "b.quantity" — colored
            // distinctly from the column name after the dot, matching the
            // reference IDE's semantic highlighting without needing to
            // actually resolve whether it's a real alias
            + "|(?<QUALIFIER>\\b[A-Za-z_][A-Za-z0-9_]*\\b(?=\\.))"
            + "|(?<STRING>'[^']*')"
            + "|(?<NUMBER>\\b\\d+(\\.\\d+)?\\b)"
            + "|(?<COMMENT>--[^\\n]*|/\\*(.|\\R)*?\\*/)"
    );

    private SqlHighlighter() {}

    /** Creates a fully configured SQL editor. */
    public static CodeArea createEditor() {
        CodeArea editor = new CodeArea();
        editor.setParagraphGraphicFactory(LineNumberFactory.get(editor));
        editor.getStyleClass().add("sql-editor");
        editor.multiPlainChanges()
                .successionEnds(Duration.ofMillis(120))
                .subscribe(ignore -> editor.setStyleSpans(0, computeHighlighting(editor.getText())));
        return editor;
    }

    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastEnd = 0;
        StyleSpansBuilder<Collection<String>> spans = new StyleSpansBuilder<>();
        while (matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD")   != null ? "sql-keyword" :
                    matcher.group("TYPE")      != null ? "sql-type" :
                    matcher.group("PARAMETER") != null ? "sql-parameter" :
                    matcher.group("FUNCTION")  != null ? "sql-function" :
                    matcher.group("QUALIFIER") != null ? "sql-qualifier" :
                    matcher.group("STRING")    != null ? "sql-string"  :
                    matcher.group("NUMBER")    != null ? "sql-number"  :
                    matcher.group("COMMENT")   != null ? "sql-comment" : null;
            spans.add(Collections.emptyList(), matcher.start() - lastEnd);
            spans.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }
        spans.add(Collections.emptyList(), text.length() - lastEnd);
        return spans.create();
    }
}
