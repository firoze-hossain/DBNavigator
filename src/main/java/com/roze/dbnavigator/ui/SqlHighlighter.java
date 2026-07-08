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
            "COUNT", "SUM", "AVG", "MIN", "MAX", "CAST", "COALESCE", "ASC", "DESC",
            "INT", "INTEGER", "BIGINT", "SMALLINT", "VARCHAR", "CHAR", "TEXT", "DATE",
            "TIMESTAMP", "BOOLEAN", "DECIMAL", "NUMERIC", "FLOAT", "DOUBLE", "SERIAL"
    };

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>\\b(?i:" + String.join("|", KEYWORDS) + ")\\b)"
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
                    matcher.group("KEYWORD") != null ? "sql-keyword" :
                    matcher.group("STRING")  != null ? "sql-string"  :
                    matcher.group("NUMBER")  != null ? "sql-number"  :
                    matcher.group("COMMENT") != null ? "sql-comment" : null;
            spans.add(Collections.emptyList(), matcher.start() - lastEnd);
            spans.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }
        spans.add(Collections.emptyList(), text.length() - lastEnd);
        return spans.create();
    }
}
