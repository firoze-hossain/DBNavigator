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

/** Regex-based syntax highlighting for the MongoDB shell console. */
public final class MongoShellHighlighter {

    private static final String[] KEYWORDS = {"use", "true", "false", "null", "undefined"};

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>\\b(?:" + String.join("|", KEYWORDS) + ")\\b)"
            + "|(?<DBFUNCTION>\\bdb\\b)"
            + "|(?<FUNCTION>\\b[A-Za-z_][A-Za-z0-9_]*\\b(?=\\s*\\())"
            + "|(?<STRING>\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*')"
            + "|(?<NUMBER>\\b-?\\d+(\\.\\d+)?\\b)"
            + "|(?<COMMENT>//[^\\n]*|/\\*(.|\\R)*?\\*/)"
    );

    private MongoShellHighlighter() {}

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
                    matcher.group("KEYWORD")    != null ? "sql-keyword" :
                    matcher.group("DBFUNCTION") != null ? "sql-qualifier" :
                    matcher.group("FUNCTION")   != null ? "sql-function" :
                    matcher.group("STRING")     != null ? "sql-string"  :
                    matcher.group("NUMBER")     != null ? "sql-number"  :
                    matcher.group("COMMENT")    != null ? "sql-comment" : null;
            spans.add(Collections.emptyList(), matcher.start() - lastEnd);
            spans.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }
        spans.add(Collections.emptyList(), text.length() - lastEnd);
        return spans.create();
    }
}
