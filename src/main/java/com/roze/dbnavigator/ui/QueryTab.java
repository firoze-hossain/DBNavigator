package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.QueryResult;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DataGrip-style SQL console. Autocomplete (tables → columns → keywords,
 * Enter/Tab inserts), and simple single-table SELECT results are editable:
 * change cells / delete rows, then Submit commits everything in one transaction.
 */
public class QueryTab extends Tab {

    private final ConnectionProfile profile;
    private final String catalog;   // nullable: default database of the profile
    private final CodeArea editor = SqlHighlighter.createEditor();
    private final ResultGrid resultGrid = new ResultGrid();
    private final Label statusLabel = new Label("Ready");
    private final Spinner<Integer> limitSpinner = new Spinner<>(10, 100_000, 500, 100);
    private final Button runButton = new Button("Run");
    private final Button submitButton = new Button("Submit");
    private final Button revertButton = new Button("Revert");
    private final GridEditManager editManager;

    private String lastExecutedSql;

    // ---- autocomplete ----
    private final Popup completionPopup = new Popup();
    private final ListView<String> completionList = new ListView<>();
    private int tokenStart = -1;
    private boolean suppressCompletion = false;

    public QueryTab(ConnectionProfile profile, String catalog, String title) {
        this.profile = profile;
        this.catalog = catalog;
        setText(title + (catalog != null ? " [" + catalog + "]" : ""));
        setGraphic(Icons.of(FontAwesomeSolid.TERMINAL, "#6897bb", 11));

        // ---- Toolbar ----
        runButton.setGraphic(Icons.of(FontAwesomeSolid.PLAY, "#57965c", 11));
        runButton.getStyleClass().add("run-button");
        runButton.setTooltip(new Tooltip("Execute (Ctrl+Enter)"));
        runButton.setOnAction(e -> execute());

        submitButton.setGraphic(Icons.of(FontAwesomeSolid.CHECK, "#57965c", 11));
        submitButton.setTooltip(new Tooltip("Commit pending result edits/deletes"));
        revertButton.setGraphic(Icons.of(FontAwesomeSolid.UNDO, "#e05555", 11));
        revertButton.setTooltip(new Tooltip("Discard pending edits and re-run the query"));

        editManager = new GridEditManager(profile, catalog, resultGrid,
                submitButton, revertButton, this::rerunLastSql, statusLabel::setText);

        Button exportButton = new Button("Export CSV");
        exportButton.setGraphic(Icons.of(FontAwesomeSolid.FILE_CSV, "#e0a44c", 11));
        exportButton.setOnAction(e -> resultGrid.exportCsv());

        limitSpinner.setEditable(true);
        limitSpinner.setPrefWidth(95);

        Label connLabel = new Label(profile.getName()
                + (catalog != null ? " ▸ " + catalog : ""));
        connLabel.getStyleClass().add("console-connection-label");
        connLabel.setGraphic(Icons.of(FontAwesomeSolid.DATABASE, "#57965c", 10));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, runButton, submitButton, revertButton, exportButton,
                new Label("Limit:"), limitSpinner, spacer, connLabel);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.getStyleClass().add("console-toolbar");

        // ---- Editor ----
        editor.replaceText("-- " + profile.getType().getDisplayName()
                + " console. Ctrl+Enter runs, Ctrl+Space completes.\n");
        VirtualizedScrollPane<CodeArea> editorScroll = new VirtualizedScrollPane<>(editor);

        // ---- Layout ----
        SplitPane split = new SplitPane(editorScroll, resultGrid);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.45);

        statusLabel.getStyleClass().add("console-status");
        HBox statusBar = new HBox(statusLabel);
        statusBar.setPadding(new Insets(4, 10, 4, 10));
        statusBar.getStyleClass().add("console-status-bar");

        VBox root = new VBox(toolbar, split, statusBar);
        VBox.setVgrow(split, Priority.ALWAYS);
        setContent(root);

        setupCompletion();
        CompletionService.preload(profile, catalog);

        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN).match(e)) {
                execute();
                e.consume();
            }
        });
    }

    /** Pre-fills the editor, e.g. from "New Query on table". */
    public void setSql(String sql) {
        suppressCompletion = true;
        editor.replaceText(sql);
        suppressCompletion = false;
    }

    // ---------------------------------------------------------- completion

    private void setupCompletion() {
        completionList.getStyleClass().add("completion-list");
        completionList.setPrefSize(340, 200);
        completionPopup.getContent().add(completionList);
        completionPopup.setAutoHide(true);

        completionList.setOnMouseClicked(e -> insertSelectedCompletion());

        // Enter/Tab also work when the list itself has focus (after a click)
        completionList.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER, TAB -> { insertSelectedCompletion(); e.consume(); }
                case ESCAPE -> { completionPopup.hide(); e.consume(); }
                default -> {}
            }
        });

        editor.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (new KeyCodeCombination(KeyCode.SPACE, KeyCombination.CONTROL_DOWN).match(e)) {
                showCompletions();
                e.consume();
                return;
            }
            if (!completionPopup.isShowing()) return;
            switch (e.getCode()) {
                case DOWN -> { completionList.getSelectionModel().selectNext(); e.consume(); }
                case UP -> { completionList.getSelectionModel().selectPrevious(); e.consume(); }
                case ENTER, TAB -> { insertSelectedCompletion(); e.consume(); }
                case ESCAPE -> { completionPopup.hide(); e.consume(); }
                default -> {}
            }
        });

        editor.plainTextChanges().subscribe(change -> {
            if (suppressCompletion) return;
            if (change.getInserted().length() == 1
                    && change.getInserted().matches("[A-Za-z0-9_.]")) {
                showCompletions();
            } else if (completionPopup.isShowing()) {
                showCompletions();   // refresh after backspace etc.
            }
        });
    }

    private void showCompletions() {
        String token = currentToken();
        if (token.isBlank()) {
            completionPopup.hide();
            return;
        }
        List<String> suggestions = CompletionService.suggest(profile, catalog, token);
        // Nothing useful, or the token is already the only completion → hide
        if (suggestions.isEmpty()
                || (suggestions.size() == 1 && suggestions.get(0).equalsIgnoreCase(token))) {
            completionPopup.hide();
            return;
        }
        completionList.getItems().setAll(suggestions);
        completionList.getSelectionModel().selectFirst();

        Optional<Bounds> caret = editor.getCaretBounds();
        if (caret.isPresent()) {
            completionPopup.show(editor, caret.get().getMinX(), caret.get().getMaxY() + 2);
        }
    }

    /** Extracts the identifier token immediately before the caret. */
    private String currentToken() {
        int caret = editor.getCaretPosition();
        String text = editor.getText();
        int start = caret;
        while (start > 0) {
            char c = text.charAt(start - 1);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '.') start--;
            else break;
        }
        tokenStart = start;
        return text.substring(start, caret);
    }

    private void insertSelectedCompletion() {
        String selected = completionList.getSelectionModel().getSelectedItem();
        if (selected == null || tokenStart < 0) {
            completionPopup.hide();
            return;
        }
        suppressCompletion = true;
        editor.replaceText(tokenStart, editor.getCaretPosition(), selected);
        suppressCompletion = false;
        completionPopup.hide();
        editor.requestFocus();
    }

    // ------------------------------------------------------------- execute

    private void execute() {
        completionPopup.hide();
        String sql = selectedOrAllText();
        if (sql.isBlank()) return;
        executeSql(sql);
    }

    private void rerunLastSql() {
        if (lastExecutedSql != null) executeSql(lastExecutedSql);
    }

    private void executeSql(String sql) {
        lastExecutedSql = sql;
        runButton.setDisable(true);
        statusLabel.setText("Executing…");
        int limit = limitSpinner.getValue();

        AppExecutor.run(() -> {
            try {
                QueryResult result = ClientRegistry.jdbc(profile, catalog).execute(sql, limit);

                // Editable when this is a simple single-table SELECT with a usable PK
                String editableTable = null;
                List<String> pkColumns = List.of();
                if (result.isResultSet()) {
                    editableTable = detectEditableTable(sql);
                    if (editableTable != null) {
                        try {
                            pkColumns = MetadataService.loadPrimaryKeys(
                                    profile, tableRef(editableTable));
                        } catch (Exception ignored) {
                            pkColumns = List.of();
                        }
                    }
                }
                final String targetTable = editableTable;
                final List<String> pk = pkColumns;

                Platform.runLater(() -> {
                    if (result.isResultSet()) {
                        if (targetTable != null && !pk.isEmpty()) {
                            editManager.configure(targetTable, pk, result);
                        } else {
                            editManager.configureReadOnly(result);
                        }
                        resultGrid.showResult(result);
                        statusLabel.setText(result.getRows().size() + " row(s) in "
                                + result.getExecutionMillis() + " ms"
                                + (result.getRows().size() >= limit ? "  (limited to " + limit + ")" : "")
                                + (editManager.isEditable()
                                    ? "  ·  editable — double-click cells, Delete removes rows"
                                    : ""));
                    } else {
                        editManager.configureReadOnly(null);
                        resultGrid.showResult(null);
                        statusLabel.setText(result.getMessage() + " in " + result.getExecutionMillis() + " ms");
                    }
                    runButton.setDisable(false);
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> {
                    editManager.configureReadOnly(null);
                    resultGrid.showResult(null);
                    statusLabel.setText("Error: " + msg);
                    runButton.setDisable(false);
                });
            }
        });
    }

    // ------------------------------------------- editable-target detection

    private static final Pattern SIMPLE_SELECT = Pattern.compile(
            "^\\s*select\\s+.+?\\s+from\\s+([A-Za-z0-9_.\"]+)(.*)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * Returns the target table token when the query is a plain single-table
     * SELECT (no joins/aggregation/unions), otherwise null.
     */
    private static String detectEditableTable(String sql) {
        String lower = sql.toLowerCase(Locale.ROOT);
        if (lower.contains(" join ") || lower.contains("group by") || lower.contains(" union ")
                || lower.contains("distinct") || lower.contains(" having ")) {
            return null;
        }
        Matcher matcher = SIMPLE_SELECT.matcher(sql.trim());
        if (!matcher.matches()) return null;
        String rest = matcher.group(2).stripLeading();
        if (rest.startsWith(",")) return null;   // multi-table FROM a, b
        return matcher.group(1);
    }

    /** Builds a DbObject reference from a (possibly qualified) table token. */
    private DbObject tableRef(String token) {
        String[] parts = token.replace("\"", "").split("\\.");
        String tableName = parts[parts.length - 1];
        String prefix = parts.length > 1 ? parts[parts.length - 2] : null;

        String cat = catalog;
        String schema = null;
        if (prefix != null) {
            if (profile.getType() == DatabaseType.MYSQL
                    || profile.getType() == DatabaseType.MARIADB) {
                cat = prefix;      // MySQL qualifies by database
            } else {
                schema = prefix;   // everyone else qualifies by schema
            }
        }
        return new DbObject(tableName, DbObject.Kind.TABLE, cat, schema);
    }

    private String selectedOrAllText() {
        String selected = editor.getSelectedText();
        return (selected != null && !selected.isBlank()) ? selected : editor.getText();
    }
}
