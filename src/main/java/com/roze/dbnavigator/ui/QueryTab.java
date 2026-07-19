package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.db.LocalHistoryStore;
import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.db.QueryHistoryStore;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.QueryResult;
import com.roze.dbnavigator.util.AppExecutor;
import com.roze.dbnavigator.util.SqlReformatter;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DataGrip-style SQL console. Autocomplete (tables → columns → keywords,
 * Enter/Tab inserts), and simple single-table SELECT results are editable:
 * change cells / delete rows, then Submit commits everything in one transaction.
 */
public class QueryTab extends Tab {

    private final MainWindow mainWindow;
    private final ConnectionProfile profile;
    private final String catalog;   // nullable: default database of the profile
    private final CodeArea editor = SqlHighlighter.createEditor();
    private final ResultGrid resultGrid = new ResultGrid();
    private final Label statusLabel = new Label("Ready");
    private final Spinner<Integer> limitSpinner = new Spinner<>(10, 100_000, 500, 100);
    private final Button runButton = new Button("Run");
    private final Button cancelButton = new Button("Cancel");
    private final Button historyButton = new Button();
    private final Button submitButton = new Button("Submit");
    private final Button revertButton = new Button("Revert");
    private final GridEditManager editManager;

    private final AtomicReference<java.sql.Statement> runningStatement = new AtomicReference<>();
    private final Popup historyPopup = new Popup();
    private final ListView<QueryHistoryStore.Entry> historyList = new ListView<>();
    private String lastExecutedSql;

    /** Stable identity for this console's Local History (independent of catalog suffix in the tab label). */
    private final String fileId;

    // ---- autocomplete ----
    private final Popup completionPopup = new Popup();
    private final ListView<CompletionService.Suggestion> completionList = new ListView<>();
    private int tokenStart = -1;
    private boolean suppressCompletion = false;

    public QueryTab(MainWindow mainWindow, ConnectionProfile profile, String catalog, String title) {
        this.mainWindow = mainWindow;
        this.profile = profile;
        this.catalog = catalog;
        this.fileId = title;
        setText(title + (catalog != null ? " [" + catalog + "]" : ""));
        setGraphic(Icons.of(FontAwesomeSolid.TERMINAL, "#6897bb", 11));

        // ---- Toolbar ----
        runButton.setGraphic(Icons.of(FontAwesomeSolid.PLAY, "#57965c", 11));
        runButton.getStyleClass().add("run-button");
        runButton.setTooltip(new Tooltip("Execute (Ctrl+Enter)"));
        runButton.setOnAction(e -> execute());

        cancelButton.setGraphic(Icons.of(FontAwesomeSolid.STOP_CIRCLE, "#e05555", 11));
        cancelButton.setTooltip(new Tooltip("Cancel the running query"));
        cancelButton.setVisible(false);
        cancelButton.setManaged(false);
        cancelButton.setOnAction(e -> cancelRunningQuery());

        historyButton.setGraphic(Icons.of(FontAwesomeSolid.HISTORY, "#a9b7c6", 11));
        historyButton.setTooltip(new Tooltip("Query History"));
        historyButton.setOnAction(e -> showHistory());

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
        HBox toolbar = new HBox(8, runButton, cancelButton, historyButton,
                submitButton, revertButton, exportButton,
                new Label("Limit:"), limitSpinner, spacer, connLabel);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.getStyleClass().add("console-toolbar");

        // ---- Editor ----
        editor.replaceText("-- " + profile.getType().getDisplayName()
                + " console. Ctrl+Enter runs, Ctrl+Space completes.\n");
        VirtualizedScrollPane<CodeArea> editorScroll = new VirtualizedScrollPane<>(editor);

        // Local History: first snapshot is the "Create" entry; further edits are
        // auto-captured after a pause (avoids saving a snapshot per keystroke).
        LocalHistoryStore.record(fileId, editor.getText());
        editor.plainTextChanges()
                .successionEnds(Duration.ofSeconds(3))
                .subscribe(change -> LocalHistoryStore.record(fileId, editor.getText()));
        setOnClosed(e -> LocalHistoryStore.record(fileId, editor.getText()));

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
        setupHistory();
        setupEditorContextMenu();
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
        completionList.setPrefSize(430, 210);
        completionList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(CompletionService.Suggestion item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label name = new Label(item.text());
                name.getStyleClass().addAll("completion-name",
                        "completion-" + item.kind().name().toLowerCase());
                Label detail = new Label(item.detail());
                detail.getStyleClass().add("completion-detail");
                Region gap = new Region();
                HBox.setHgrow(gap, Priority.ALWAYS);
                HBox box = new HBox(10, name, gap, detail);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
            }
        });
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
            String inserted = change.getInserted();
            if (inserted.length() == 1 && inserted.matches("[A-Za-z0-9_.]")) {
                showCompletions();
            } else if (inserted.equals(" ")
                    && contextAt(editor.getCaretPosition()) != CompletionService.Context.ANY) {
                showCompletions();   // DataGrip behavior: popup right after FROM/SELECT/…
            } else if (completionPopup.isShowing()) {
                showCompletions();   // refresh after backspace etc.
            }
        });
    }

    private void showCompletions() {
        String token = currentToken();
        CompletionService.Context context = contextAt(tokenStart);
        if (token.isBlank() && context == CompletionService.Context.ANY) {
            completionPopup.hide();
            return;
        }
        List<CompletionService.Suggestion> suggestions =
                CompletionService.suggest(profile, catalog, token, context);
        // Nothing useful, or the token is already the only completion → hide
        if (suggestions.isEmpty()
                || (suggestions.size() == 1 && suggestions.get(0).text().equalsIgnoreCase(token))) {
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
        CompletionService.Suggestion selected =
                completionList.getSelectionModel().getSelectedItem();
        if (selected == null || tokenStart < 0) {
            completionPopup.hide();
            return;
        }
        suppressCompletion = true;
        editor.replaceText(tokenStart, editor.getCaretPosition(), selected.text());
        suppressCompletion = false;
        completionPopup.hide();
        editor.requestFocus();
    }

    /** Context from the previous significant word: FROM → tables, WHERE → columns … */
    private CompletionService.Context contextAt(int position) {
        String text = editor.getText();
        int i = Math.min(position, text.length()) - 1;
        while (i >= 0 && Character.isWhitespace(text.charAt(i))) i--;
        int end = i + 1;
        while (i >= 0 && (Character.isLetterOrDigit(text.charAt(i)) || text.charAt(i) == '_')) i--;
        String word = text.substring(i + 1, end).toLowerCase(Locale.ROOT);
        return switch (word) {
            case "from", "join", "update", "into", "table" -> CompletionService.Context.TABLES;
            case "select", "where", "on", "and", "or", "by", "set",
                 "having", "between", "like", "when", "then" -> CompletionService.Context.COLUMNS;
            default -> CompletionService.Context.ANY;
        };
    }

    /** Pre-filled console text accessor (used by File → Save Console As…). */
    public String getSqlText() {
        return editor.getText();
    }

    // -------------------------------------------------------- local history

    public String getFileId() { return fileId; }

    public String getDisplayFileName() { return fileId.replace(' ', '_') + ".sql"; }

    /** File → Local History → Show History… / Show History for Selection… */
    public void showLocalHistory(javafx.stage.Window owner) {
        LocalHistoryDialog.showForFile(owner, fileId, getDisplayFileName(), editor::getText);
    }

    /** File → Local History → Put Label… */
    public void putLocalHistoryLabel(String label) {
        LocalHistoryStore.putLabel(fileId, editor.getText(), label);
    }

    // ------------------------------------------------------- editor context menu

    /**
     * DataGrip-style right-click menu on the editor, with a different item
     * set depending on whether there's a selection — matching the reference
     * IDE's own behavior. Items that are genuinely implemented here run real
     * actions; items that would need a full SQL parser, an AI backend, or
     * IDE-level refactoring/indexing (AI Actions, Column Selection Mode,
     * Find in Files/Usages, Folding, Save as Live Template, Rename, Refactor,
     * Generate, Open In, Edit as Table) are shown disabled — visibly
     * unavailable rather than silently doing nothing, matching how the
     * reference IDE itself grays out inapplicable items (e.g. Rename there).
     */
    private void setupEditorContextMenu() {
        editor.setOnContextMenuRequested(this::showEditorContextMenu);
    }

    private void showEditorContextMenu(ContextMenuEvent event) {
        boolean hasSelection = editor.getSelectedText() != null && !editor.getSelectedText().isBlank();
        ContextMenu menu = new ContextMenu();

        menu.getItems().add(disabled("Show Context Actions",
                new KeyCodeCombination(KeyCode.ENTER, KeyCombination.ALT_DOWN)));
        menu.getItems().add(disabledMenu("AI Actions"));
        menu.getItems().add(new SeparatorMenuItem());

        if (hasSelection) {
            menu.getItems().add(action("Cut",
                    new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN), editor::cut));
            menu.getItems().add(action("Copy",
                    new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN), editor::copy));
        }
        menu.getItems().add(action("Paste",
                new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN), editor::paste));
        menu.getItems().add(disabledMenu("Copy / Paste Special"));
        menu.getItems().add(disabled("Column Selection Mode",
                new KeyCodeCombination(KeyCode.DIGIT8, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)));
        menu.getItems().add(new SeparatorMenuItem());

        if (hasSelection) {
            menu.getItems().add(disabled("Find in Files",
                    new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)));
            menu.getItems().add(disabled("Find Usages", new KeyCodeCombination(KeyCode.F7, KeyCombination.ALT_DOWN)));
        }
        Menu goTo = new Menu("Go To");
        goTo.getItems().add(action("Line\u2026", null, this::goToLine));
        menu.getItems().add(goTo);
        menu.getItems().add(disabledMenu("Folding"));
        menu.getItems().add(disabled("Save as Live Template\u2026", null));
        menu.getItems().add(action("Reformat Code",
                new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN),
                this::reformatCode));
        if (hasSelection) {
            menu.getItems().add(disabled("Edit as Table", null));
            menu.getItems().add(action("Search with Google", null, this::searchSelectionWithGoogle));
        }
        menu.getItems().add(new SeparatorMenuItem());

        menu.getItems().add(disabled("Rename\u2026", new KeyCodeCombination(KeyCode.F6, KeyCombination.SHIFT_DOWN)));
        menu.getItems().add(disabledMenu("Refactor"));
        menu.getItems().add(disabled("Generate\u2026", new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN)));
        menu.getItems().add(new SeparatorMenuItem());

        Menu explainPlan = new Menu("Explain Plan");
        explainPlan.getItems().add(action("Show Execution Plan", null, this::showExecutionPlan));
        menu.getItems().add(explainPlan);
        menu.getItems().add(action("Execute",
                new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHORTCUT_DOWN), this::execute));
        if (hasSelection) {
            menu.getItems().add(action("Execute to File\u2026", null, this::executeToFile));
        }
        menu.getItems().add(disabledMenu("Open In"));
        menu.getItems().add(new SeparatorMenuItem());

        Menu localHistory = new Menu("Local History");
        localHistory.getItems().add(action("Show History\u2026", null,
                () -> showLocalHistory(mainWindow.getOwnerWindow())));
        menu.getItems().add(localHistory);
        menu.getItems().add(action("Compare with Clipboard", null, this::compareWithClipboard));

        Menu diagrams = new Menu("Diagrams");
        diagrams.getItems().add(action("Show Diagram of Referenced Tables\u2026", null,
                this::showDiagramOfReferencedTables));
        menu.getItems().add(diagrams);

        menu.show(editor, event.getScreenX(), event.getScreenY());
        event.consume();
    }

    private static MenuItem action(String text, KeyCombination accelerator, Runnable action) {
        MenuItem item = new MenuItem(text);
        if (accelerator != null) item.setAccelerator(accelerator);
        item.setOnAction(e -> action.run());
        return item;
    }

    private static MenuItem disabled(String text, KeyCombination accelerator) {
        MenuItem item = new MenuItem(text);
        if (accelerator != null) item.setAccelerator(accelerator);
        item.setDisable(true);
        return item;
    }

    private static Menu disabledMenu(String text) {
        Menu menu = new Menu(text);
        menu.setDisable(true);
        return menu;
    }

    private String selectedOrEditorText() {
        String selected = editor.getSelectedText();
        return (selected != null && !selected.isBlank()) ? selected : editor.getText();
    }

    /** Reformats the selection if there is one, otherwise the whole editor. */
    private void reformatCode() {
        String selected = editor.getSelectedText();
        if (selected != null && !selected.isBlank()) {
            editor.replaceSelection(SqlReformatter.reformat(selected));
        } else {
            int caret = editor.getCaretPosition();
            editor.replaceText(SqlReformatter.reformat(editor.getText()));
            editor.moveTo(Math.min(caret, editor.getLength()));
        }
    }

    /** Runs the query and writes the result straight to a file instead of the grid. */
    private void executeToFile() {
        String sql = selectedOrEditorText();
        if (sql.isBlank()) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Execute to File");
        chooser.setInitialFileName("result.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showSaveDialog(editor.getScene() == null ? null : editor.getScene().getWindow());
        if (file == null) return;

        statusLabel.setText("Executing to file\u2026");
        AppExecutor.run(() -> {
            try {
                QueryResult result = ClientRegistry.jdbc(profile, catalog).execute(sql, 0);
                StringBuilder csv = new StringBuilder();
                csv.append(String.join(",", result.getColumns())).append('\n');
                for (List<String> row : result.getRows()) {
                    csv.append(String.join(",", row.stream().map(v -> v == null ? "" : v).toList())).append('\n');
                }
                java.nio.file.Files.writeString(file.toPath(), csv.toString(), StandardCharsets.UTF_8);
                Platform.runLater(() -> statusLabel.setText(
                        "\u2713 Wrote " + result.getRows().size() + " row(s) to " + file.getName()));
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> statusLabel.setText("Execute to File failed: " + msg));
            }
        });
    }

    /** Runs EXPLAIN on the current statement and shows the plan in the result grid. */
    private void showExecutionPlan() {
        String sql = selectedOrEditorText();
        if (sql.isBlank()) return;
        String explainSql = "EXPLAIN " + sql.replaceAll(";\\s*$", "");
        executeSql(explainSql);
    }

    private void compareWithClipboard() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        String clipboardText = clipboard.hasString() ? clipboard.getString() : "";
        ClipboardCompareDialog.show(mainWindow.getOwnerWindow(), clipboardText, selectedOrEditorText());
    }

    private void goToLine() {
        TextInputDialog dialog = DialogTheme.apply(new TextInputDialog());
        dialog.initOwner(mainWindow.getOwnerWindow());
        dialog.setTitle("Go to Line");
        dialog.setHeaderText(null);
        dialog.setContentText("Line number:");
        dialog.showAndWait().ifPresent(text -> {
            try {
                int line = Math.max(1, Integer.parseInt(text.trim()));
                int target = Math.min(line - 1, editor.getParagraphs().size() - 1);
                editor.moveTo(target, 0);
                editor.requestFollowCaret();
                editor.requestFocus();
            } catch (NumberFormatException ignored) {
                // not a number — silently ignore rather than error on a trivial typo
            }
        });
    }

    private void searchSelectionWithGoogle() {
        String selected = editor.getSelectedText();
        if (selected == null || selected.isBlank()) return;
        try {
            String query = URLEncoder.encode(selected.strip(), StandardCharsets.UTF_8);
            java.awt.Desktop.getDesktop().browse(new URI("https://www.google.com/search?q=" + query));
        } catch (Exception ex) {
            statusLabel.setText("Could not open browser: " + ex.getMessage());
        }
    }

    /** Scans the query text for FROM/JOIN table names and opens a diagram for one of them. */
    private void showDiagramOfReferencedTables() {
        Set<String> tables = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("(?i)\\b(?:FROM|JOIN)\\s+([A-Za-z_][A-Za-z0-9_.]*)")
                .matcher(editor.getText());
        while (matcher.find()) {
            String token = matcher.group(1);
            String simpleName = token.contains(".") ? token.substring(token.lastIndexOf('.') + 1) : token;
            tables.add(simpleName);
        }
        if (tables.isEmpty()) {
            statusLabel.setText("No table names found in this console's SQL");
            return;
        }

        List<String> options = new ArrayList<>(tables);
        String chosen = options.get(0);
        if (options.size() > 1) {
            ChoiceDialog<String> dialog = DialogTheme.apply(new ChoiceDialog<>(chosen, options));
            dialog.initOwner(mainWindow.getOwnerWindow());
            dialog.setTitle("Show Diagram");
            dialog.setHeaderText(null);
            dialog.setContentText("Diagram which table?");
            Optional<String> picked = dialog.showAndWait();
            if (picked.isEmpty()) return;
            chosen = picked.get();
        }
        DbObject tableRef = new DbObject(chosen, DbObject.Kind.TABLE, catalog, null);
        mainWindow.openDiagramTab(profile, tableRef);
    }

    // ------------------------------------------------------------- history

    private void setupHistory() {
        historyList.getStyleClass().add("completion-list");
        historyList.setPrefSize(560, 260);
        java.time.format.DateTimeFormatter fmt =
                java.time.format.DateTimeFormatter.ofPattern("MMM d, HH:mm");
        historyList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(QueryHistoryStore.Entry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                String when = java.time.Instant.ofEpochMilli(item.executedAtEpochMillis())
                        .atZone(java.time.ZoneId.systemDefault()).format(fmt);
                String sqlPreview = item.sql().replaceAll("\\s+", " ").strip();
                if (sqlPreview.length() > 90) sqlPreview = sqlPreview.substring(0, 90) + "…";
                Label sql = new Label(sqlPreview);
                sql.getStyleClass().addAll("completion-name", "completion-table");
                Label time = new Label(when);
                time.getStyleClass().add("completion-detail");
                Region gap = new Region();
                HBox.setHgrow(gap, Priority.ALWAYS);
                HBox box = new HBox(10, sql, gap, time);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        historyPopup.getContent().add(historyList);
        historyPopup.setAutoHide(true);
        historyList.setOnMouseClicked(e -> { if (e.getClickCount() == 2) insertSelectedHistory(); });
        historyList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) insertSelectedHistory();
            else if (e.getCode() == KeyCode.ESCAPE) historyPopup.hide();
        });
    }

    private void showHistory() {
        List<QueryHistoryStore.Entry> entries = QueryHistoryStore.forConnection(profile.getId());
        if (entries.isEmpty()) {
            statusLabel.setText("No query history yet for this connection");
            return;
        }
        historyList.getItems().setAll(entries);
        historyList.getSelectionModel().selectFirst();
        historyPopup.show(historyButton, historyButton.localToScreen(0, 0).getX(),
                historyButton.localToScreen(0, 0).getY() + historyButton.getHeight() + 2);
        historyList.requestFocus();
    }

    private void insertSelectedHistory() {
        QueryHistoryStore.Entry selected = historyList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        setSql(selected.sql());
        historyPopup.hide();
        editor.requestFocus();
    }

    // ------------------------------------------------------------- cancel

    private void cancelRunningQuery() {
        java.sql.Statement stmt = runningStatement.get();
        if (stmt == null) return;
        statusLabel.setText("Cancelling…");
        AppExecutor.run(() -> {
            try {
                stmt.cancel();
            } catch (Exception ignored) {
                // driver may not support cancel — the query will just run to completion
            }
        });
    }

    private void setRunningState(boolean running) {
        runButton.setDisable(running);
        cancelButton.setVisible(running);
        cancelButton.setManaged(running);
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
        setRunningState(true);
        statusLabel.setText("Executing…");
        int limit = limitSpinner.getValue();
        QueryHistoryStore.record(profile.getId(), sql);

        AppExecutor.run(() -> {
            try {
                // Editable when this is a simple single-table SELECT with a usable PK
                String editableTable = detectEditableTable(sql);
                List<String> pkColumns = List.of();
                Map<String, Integer> columnTypes = Map.of();
                boolean viaCtid = false;
                String sqlToRun = sql;

                if (editableTable != null) {
                    DbObject ref = tableRef(editableTable);
                    try {
                        pkColumns = MetadataService.loadPrimaryKeys(profile, ref);
                    } catch (Exception ignored) {
                        pkColumns = List.of();
                    }
                    // PostgreSQL table without a PK (e.g. a partition) + plain
                    // "SELECT *": silently select ctid too so edits still work.
                    // The ctid column stays hidden in the grid.
                    if (pkColumns.isEmpty()
                            && profile.getType() == DatabaseType.POSTGRESQL
                            && sql.matches("(?is)\\s*select\\s+\\*\\s+from\\s+.*")) {
                        sqlToRun = sql.replaceFirst("(?is)^(\\s*select\\s+)\\*", "$1ctid, *");
                        pkColumns = List.of("ctid");
                        viaCtid = true;
                    }
                    if (!pkColumns.isEmpty()) {
                        try {
                            columnTypes = MetadataService.loadColumnTypes(profile, ref);
                        } catch (Exception ignored) {
                            columnTypes = Map.of();
                        }
                    }
                }

                QueryResult execResult;
                try {
                    execResult = ClientRegistry.jdbc(profile, catalog)
                            .execute(sqlToRun, limit, runningStatement);
                } catch (Exception rewriteFailure) {
                    if (!viaCtid) throw rewriteFailure;
                    // views have no ctid — run the original query, read-only
                    execResult = ClientRegistry.jdbc(profile, catalog)
                            .execute(sql, limit, runningStatement);
                    pkColumns = List.of();
                }

                final QueryResult result = execResult;
                final String targetTable = editableTable;
                final List<String> pk = pkColumns;
                final Map<String, Integer> types = columnTypes;

                Platform.runLater(() -> {
                    if (result.isResultSet()) {
                        if (targetTable != null && !pk.isEmpty()) {
                            editManager.configure(targetTable, pk, types, result);
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
                    setRunningState(false);
                });
            } catch (Exception ex) {
                boolean cancelled = ex.getMessage() != null
                        && ex.getMessage().toLowerCase(Locale.ROOT).contains("cancel");
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> {
                    editManager.configureReadOnly(null);
                    resultGrid.showResult(null);
                    statusLabel.setText(cancelled ? "Query cancelled" : "Error: " + msg);
                    setRunningState(false);
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
