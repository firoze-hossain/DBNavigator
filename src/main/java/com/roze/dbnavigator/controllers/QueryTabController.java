package com.roze.dbnavigator.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryTabController {
    @FXML
    private CodeArea sqlEditor;
    @FXML
    private TextArea messagesArea;
    private String initialContent = "";
    private boolean isModified = false;

    private static final String[] KEYWORDS = new String[]{
            "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "JOIN",
            "INNER", "OUTER", "LEFT", "RIGHT", "GROUP BY", "ORDER BY", "HAVING"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'";
    private static final String COMMENT_PATTERN = "--[^\n]*|/\\*(.|\\R)*?\\*/";

    private static final Pattern SQL_PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
    );

    @FXML
    public void initialize() {
        setupSqlEditor();
        // Track changes to determine if content is modified
        sqlEditor.textProperty().addListener((obs, oldText, newText) -> {
            isModified = !newText.equals(initialContent);
        });
    }

    @FXML
    private void executeQuery() {
        String query = sqlEditor.getText();
        messagesArea.appendText("Executing query:\n" + query + "\n");
        // TODO: Add actual query execution logic
    }

    private void setupSqlEditor() {
        sqlEditor.setParagraphGraphicFactory(LineNumberFactory.get(sqlEditor));
        // Initialize with default content
        sqlEditor.replaceText("-- Enter your SQL here\n");
        initialContent = sqlEditor.getText();
        sqlEditor.textProperty().addListener((obs, oldText, newText) -> {
            sqlEditor.setStyleSpans(0, computeHighlighting(newText));
        });

        // Add context menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem formatSql = new MenuItem("Format SQL");
        formatSql.setOnAction(e -> formatSql());
        contextMenu.getItems().add(formatSql);
        sqlEditor.setContextMenu(contextMenu);
    }

    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = SQL_PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        while (matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                            matcher.group("STRING") != null ? "string" :
                                    matcher.group("COMMENT") != null ? "comment" :
                                            null;

            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }

        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    // Add this method to check for unsaved changes
    public boolean hasUnsavedChanges() {
        return isModified;
    }

    // Add this method to mark content as saved
    public void markAsSaved() {
        initialContent = sqlEditor.getText();
        isModified = false;
    }

    // Add this method to get the current SQL content
    public String getSqlContent() {
        return sqlEditor.getText();
    }

    @FXML
    private void formatSql() {
        messagesArea.appendText("Formatting SQL...\n");
        // Implement SQL formatting logic
    }

    @FXML
    private void saveQuery() {
        messagesArea.appendText("Query saved.\n");
        markAsSaved();
        // TODO: Implement actual saving
    }
}