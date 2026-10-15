package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.db.MongoDbClient;
import com.roze.dbnavigator.db.QueryHistoryStore;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.QueryResult;
import com.roze.dbnavigator.util.AppExecutor;
import com.roze.dbnavigator.util.MongoShellParser;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import org.bson.Document;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A real MongoDB shell console: {@code use dbname;} and
 * {@code db.collection.method(args);} statements, run against the actual
 * driver — not just a read-only filter box like {@link MongoCollectionTab}.
 *
 * Matches the reference IDE's console behavior: Run and Ctrl+Enter execute
 * only the statement the caret is currently in (or the whole thing if
 * there's just one), which is highlighted with the same character-range
 * background used by the SQL console — not the entire script every time.
 * Statements don't need trailing semicolons; a newline at bracket depth 0
 * ends one, the same way the real Mongo shell (and JavaScript's automatic
 * semicolon insertion) treats them.
 *
 * Supported methods: findOne, insertOne, insertMany, find, updateOne,
 * updateMany, deleteOne, deleteMany, countDocuments, drop. Arguments are
 * written the way the real Mongo shell accepts them (unquoted keys, single
 * or double quoted strings) via {@link MongoShellParser}, not strict JSON.
 *
 * {@code find()}/{@code findOne()} results support the same real column
 * sorting, Submit/Revert cell editing, and CSV/JSON export as
 * {@link MongoCollectionTab} — sorting and editing both act against
 * whichever collection/filter the most recent query used. Running one also
 * switches to the Result tab automatically.
 *
 * Honest scope note: this covers the common CRUD operations, not the full
 * shell language — no variables, no JS expressions/functions beyond a
 * literal document/array argument, no aggregation pipeline helpers. That
 * covers what "insert/update/delete/query data" actually needs without
 * building a full JavaScript engine.
 */
public class MongoConsoleTab extends Tab {

    private final ConnectionProfile profile;
    private final CodeArea editor = MongoShellHighlighter.createEditor();
    private final TextArea outputArea = new TextArea();
    private final ResultGrid resultGrid = new ResultGrid();
    private final MongoGridEditManager editManager;
    private final Label dbLabel = new Label();
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Button submitButton = new Button("Submit");
    private final Button revertButton = new Button("Revert");
    private final TabPane bottomTabs;
    private final Tab resultTab;
    private final StackPane dataPanelContainer = new StackPane();
    private final SplitPane split;
    private double lastDividerPosition = 0.55;

    private String currentDatabase;
    private String lastFindDatabase;
    private String lastFindCollection;
    private String lastFindFilter = "{}";
    private boolean lastFindWasSingle;
    private String sortField;
    private String sortDirection;   // "ASC", "DESC", or null

    // ---- find() pagination ----
    private int findPageSize = 200;
    private int findPage = 0;
    private long findTotalDocs = -1;
    private final TablePagingWidget pagingWidget = new TablePagingWidget();
    private BorderPane resultPagingBar;
    private final Label statusLabel = new Label();
    private final Label positionLabel = new Label();
    private HBox dataToolbar;

    // ---- autocomplete ----
    private final Popup completionPopup = new Popup();
    private final ListView<MongoCompletionService.Suggestion> completionList = new ListView<>();
    private int tokenStart = -1;
    private boolean suppressCompletion = false;
    private List<String> cachedCollectionNames = List.of();
    private String cachedCollectionsDatabase;

    // ---- history ----
    private final Button historyButton = new Button();
    private final Popup historyPopup = new Popup();
    private final ListView<QueryHistoryStore.Entry> historyList = new ListView<>();

    public MongoConsoleTab(ConnectionProfile profile, String initialDatabase, String title) {
        this.profile = profile;
        this.currentDatabase = initialDatabase;

        setText(title);
        setGraphic(Icons.of(FontAwesomeSolid.LEAF, "#57965c", 11));

        editor.replaceText("// MongoDB console. Run/Ctrl+Enter execute the statement at the caret.\n"
                + "// Example: use " + (initialDatabase != null ? initialDatabase : "mydb") + ";\n"
                + "// db.collection.insertOne({ name: \"Ada\", age: 30 });\n\n");
        editor.moveTo(0);
        VirtualizedScrollPane<CodeArea> editorScroll = new VirtualizedScrollPane<>(editor);

        Button runButton = new Button("Run");
        runButton.setGraphic(Icons.of(FontAwesomeSolid.PLAY, "#57965c", 12));
        runButton.getStyleClass().add("run-button");
        runButton.setOnAction(e -> runCurrentStatement());

        boolean isMac = System.getProperty("os.name", "").toLowerCase().contains("mac");
        String historyShortcut = isMac ? "⌥⌘E" : "Ctrl+Alt+E";
        historyButton.setGraphic(Icons.of(FontAwesomeSolid.HISTORY, "#a9b7c6", 11));
        historyButton.setTooltip(new Tooltip("Browse Query History (" + historyShortcut + ")"));
        historyButton.setOnAction(e -> showHistory());

        submitButton.getStyleClass().add("run-button");
        submitButton.setGraphic(Icons.of(FontAwesomeSolid.CHECK, "#ffffff", 11));
        revertButton.setGraphic(Icons.of(FontAwesomeSolid.UNDO, "#a9b7c6", 11));
        editManager = new MongoGridEditManager(profile, resultGrid,
                submitButton, revertButton, this::rerunLastFind, this::log);


        resultGrid.setSortRequestListener((columnName, direction) -> {
            sortField = direction == null ? null : columnName;
            sortDirection = direction;
            resultGrid.setCurrentSort(columnName, direction);
            findPage = 0;
            rerunLastFind();
        });

        dbLabel.getStyleClass().add("console-connection-label");
        updateDbLabel();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, runButton, historyButton, spacer, dbLabel);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.getStyleClass().add("console-toolbar");

        outputArea.setEditable(false);
        outputArea.getStyleClass().add("process-output");
        pagingWidget.setActivePageSize(findPageSize);
        pagingWidget.setOnCountRows(() -> {
            if (lastFindCollection == null) return;
            AppExecutor.run(() -> {
                try {
                    MongoDbClient client = ClientRegistry.mongo(profile);
                    long count = client.countDocuments(lastFindDatabase, lastFindCollection, lastFindFilter);
                    findTotalDocs = count;
                    Platform.runLater(() -> {
                        int rows = resultGrid.getItems().size();
                        long from = rows == 0 ? 0 : (long) findPage * findPageSize + 1;
                        long to = from + rows - 1;
                        pagingWidget.update(from, to, findTotalDocs, true);
                        statusLabel.setText("Counted " + count + " document(s)");
                        log("Counted " + count + " document(s) in " + lastFindCollection);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Error counting documents: " + ex.getMessage());
                        log("Error counting documents: " + ex.getMessage());
                    });
                }
            });
        });
        pagingWidget.setOnPageSizeChanged(newSize -> {
            findPageSize = newSize;
            findPage = 0;
            rerunLastFind();
        });
        pagingWidget.setOnPlacementChanged(this::applyPagingPlacement);
        pagingWidget.setOnReload(this::rerunLastFind);
        pagingWidget.setOnFirst(() -> {
            if (findPage != 0) {
                findPage = 0;
                rerunLastFind();
            }
        });
        pagingWidget.setOnPrev(() -> {
            if (findPage > 0) {
                findPage--;
                rerunLastFind();
            }
        });
        pagingWidget.setOnNext(() -> {
            findPage++;
            rerunLastFind();
        });

        statusLabel.getStyleClass().add("console-status");
        positionLabel.getStyleClass().add("console-status");
        positionLabel.textProperty().bind(resultGrid.selectionStatusProperty());
        resultPagingBar = new BorderPane();
        resultPagingBar.setPadding(new Insets(4, 8, 4, 8));
        resultPagingBar.getStyleClass().add("console-status-bar");
        applyPagingPlacement(TablePagingWidget.Placement.BOTTOM_CENTER);

        resultGrid.setReloadAction(this::rerunLastFind);

        Button addRowBtn = new Button();
        addRowBtn.setGraphic(Icons.of(FontAwesomeSolid.PLUS, "#57965c", 11));
        addRowBtn.setTooltip(new Tooltip("Add Document / Row"));
        addRowBtn.disableProperty().bind(resultGrid.editableProperty().not());
        addRowBtn.setOnAction(e -> {
            if (resultGrid.getAddRowAction() != null) {
                resultGrid.getAddRowAction().run();
            }
        });

        Button deleteRowsBtn = new Button();
        deleteRowsBtn.setGraphic(Icons.of(FontAwesomeSolid.MINUS, "#e05555", 11));
        deleteRowsBtn.setTooltip(new Tooltip("Delete Selected Document(s)"));
        deleteRowsBtn.disableProperty().bind(resultGrid.editableProperty().not()
                .or(javafx.beans.binding.Bindings.isEmpty(resultGrid.getSelectionModel().getSelectedCells())));
        deleteRowsBtn.setOnAction(e -> {
            if (resultGrid.getDeleteRowsAction() != null) {
                resultGrid.getDeleteRowsAction().run();
            }
        });

        Button tabSubmitBtn = new Button("Submit");
        tabSubmitBtn.setGraphic(Icons.of(FontAwesomeSolid.CHECK, "#57965c", 11));
        tabSubmitBtn.setTooltip(new Tooltip("Commit pending edits/deletes to MongoDB"));
        tabSubmitBtn.disableProperty().bind(submitButton.disableProperty());
        tabSubmitBtn.setOnAction(e -> submitButton.fire());

        Button tabRevertBtn = new Button("Revert");
        tabRevertBtn.setGraphic(Icons.of(FontAwesomeSolid.UNDO, "#e05555", 11));
        tabRevertBtn.setTooltip(new Tooltip("Discard pending edits and re-run query"));
        tabRevertBtn.disableProperty().bind(revertButton.disableProperty());
        tabRevertBtn.setOnAction(e -> revertButton.fire());

        Button tabReloadBtn = new Button();
        tabReloadBtn.setGraphic(Icons.of(FontAwesomeSolid.SYNC_ALT, "#6897bb", 11));
        tabReloadBtn.setTooltip(new Tooltip("Reload Page / Re-run query  Ctrl+F5"));
        tabReloadBtn.setOnAction(e -> rerunLastFind());

        DataExtractorsToolbar extractorsToolbar = new DataExtractorsToolbar(
                ConnectionProfile.DatabaseType.MONGODB,
                resultGrid::toQueryResult,
                () -> (lastFindDatabase != null && lastFindCollection != null ? lastFindDatabase + "." + lastFindCollection : "collection"),
                () -> null,
                () -> profile,
                () -> getTabPane() != null && getTabPane().getScene() != null ? getTabPane().getScene().getWindow() : null
        );
        extractorsToolbar.addFormatListener(resultGrid::setActiveExtractorFormat);
        resultGrid.setActiveExtractorFormat(extractorsToolbar.getActiveFormat());
        resultGrid.setExportDataAction(extractorsToolbar::openExportDialog);

        Region tbSpacer = new Region();
        HBox.setHgrow(tbSpacer, Priority.ALWAYS);

        dataToolbar = new HBox(6, addRowBtn, deleteRowsBtn, tabSubmitBtn, tabRevertBtn, tabReloadBtn, tbSpacer, extractorsToolbar);
        dataToolbar.setAlignment(Pos.CENTER_LEFT);
        dataToolbar.setPadding(new Insets(4, 8, 4, 8));
        dataToolbar.getStyleClass().add("result-data-toolbar");

        VBox resultContent = new VBox(dataToolbar, resultGrid, resultPagingBar);
        VBox.setVgrow(resultGrid, Priority.ALWAYS);

        Tab outputTab = new Tab("Output", outputArea);
        outputTab.setClosable(false);
        resultTab = new Tab("Result", resultContent);
        resultTab.setClosable(false);
        bottomTabs = new TabPane(outputTab, resultTab);

        String hideShortcut = isMac ? "⇧⎋" : "Shift+Escape";

        Button hideDataPanelButton = new Button();
        hideDataPanelButton.setGraphic(Icons.of(FontAwesomeSolid.MINUS, "#a9b7c6", 11));
        hideDataPanelButton.setTooltip(new Tooltip("Hide  " + hideShortcut));
        hideDataPanelButton.getStyleClass().add("run-panel-chrome-button");
        hideDataPanelButton.setOnAction(e -> hideDataPanel());

        Button moreOptionsButton = new Button();
        moreOptionsButton.setGraphic(Icons.of(FontAwesomeSolid.ELLIPSIS_V, "#a9b7c6", 11));
        moreOptionsButton.setTooltip(new Tooltip("More Options"));
        moreOptionsButton.getStyleClass().add("run-panel-chrome-button");

        HBox chromeButtons = new HBox(2, moreOptionsButton, hideDataPanelButton);
        chromeButtons.setAlignment(Pos.CENTER_RIGHT);
        chromeButtons.setPickOnBounds(false);
        chromeButtons.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(chromeButtons, Pos.TOP_RIGHT);
        StackPane.setMargin(chromeButtons, new Insets(3, 6, 0, 0));

        dataPanelContainer.getChildren().addAll(bottomTabs, chromeButtons);

        this.split = new SplitPane(editorScroll, dataPanelContainer);
        split.setOrientation(javafx.geometry.Orientation.VERTICAL);
        split.setDividerPositions(0.55);

        VBox root = new VBox(toolbar, split);
        VBox.setVgrow(split, Priority.ALWAYS);
        setContent(root);

        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (new KeyCodeCombination(KeyCode.ESCAPE, KeyCombination.SHIFT_DOWN).match(e)) {
                if (isDataPanelVisible()) {
                    hideDataPanel();
                    e.consume();
                }
            } else if (new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN).match(e)
                    || (e.isControlDown() && e.isAltDown() && e.getCode() == KeyCode.E)) {
                showHistory();
                e.consume();
            } else if (e.isControlDown() && e.getCode() == KeyCode.ENTER) {
                runCurrentStatement();
                e.consume();
            }
        });

        setupStatementHighlighting();
        setupCompletion();
        setupHistory();
    }

    private void updateDbLabel() {
        dbLabel.setText(profile.getName() + (currentDatabase != null ? "  \u203a  " + currentDatabase : ""));
    }

    /** Used by MainWindow's Split Right/Down and Reopen Closed Tab. */
    public ConnectionProfile getProfileForReopen() { return profile; }
    public ConnectionProfile getProfile() { return profile; }
    public String getCurrentDatabase() { return currentDatabase; }
    public String getScriptText() { return editor.getText(); }
    public void setScriptText(String text) {
        suppressCompletion = true;
        editor.replaceText(text != null ? text : "");
        editor.moveTo(Math.min(editor.getCaretPosition(), editor.getLength()));
        suppressCompletion = false;
    }
    public CodeArea getEditor() { return editor; }
    public void focusEditor() { if (editor != null) editor.requestFocus(); }
    public void execute() { runCurrentStatement(); }

    public void hideDataPanel() {
        if (split.getItems().contains(dataPanelContainer)) {
            if (split.getDividerPositions().length > 0) {
                lastDividerPosition = split.getDividerPositions()[0];
            }
            split.getItems().remove(dataPanelContainer);
            if (editor != null) editor.requestFocus();
        }
    }

    public void showDataPanel() {
        if (!split.getItems().contains(dataPanelContainer)) {
            split.getItems().add(dataPanelContainer);
            split.setDividerPositions(lastDividerPosition);
        }
    }

    public void toggleDataPanel() {
        if (isDataPanelVisible()) hideDataPanel(); else showDataPanel();
    }

    public boolean isDataPanelVisible() {
        return split.getItems().contains(dataPanelContainer);
    }

    private void log(String line) {
        outputArea.appendText("[" + LocalTime.now().format(timeFormat) + "] " + line + "\n");
    }

    // ----------------------------------------------------- current-statement

    /**
     * Highlights the statement the caret is currently inside — same
     * character-range technique as the SQL console (an overlay merged with
     * the syntax-highlighting spans, so the highlighted text keeps its own
     * keyword/string colors underneath).
     */
    private void setupStatementHighlighting() {
        editor.multiPlainChanges()
                .successionEnds(Duration.ofMillis(120))
                .subscribe(ignore -> refreshEditorStyling());
        editor.caretPositionProperty().addListener((obs, o, n) -> refreshEditorStyling());
        Platform.runLater(this::refreshEditorStyling);
    }

    private void refreshEditorStyling() {
        try {
            String text = editor.getText();
            var syntax = MongoShellHighlighter.computeHighlighting(text);
            int[] range = currentStatementCharRange(text);

            if (range == null) {
                editor.setStyleSpans(0, syntax);
                return;
            }
            var spansBuilder = new org.fxmisc.richtext.model.StyleSpansBuilder<java.util.Collection<String>>();
            if (range[0] > 0) spansBuilder.add(Collections.emptyList(), range[0]);
            spansBuilder.add(Collections.singleton("current-statement-line"), range[1] - range[0]);
            if (range[1] < text.length()) spansBuilder.add(Collections.emptyList(), text.length() - range[1]);
            var highlight = spansBuilder.create();

            var merged = syntax.overlay(highlight, (base, extra) -> {
                if (extra.isEmpty()) return base;
                var combined = new java.util.LinkedHashSet<>(base);
                combined.addAll(extra);
                return combined;
            });
            editor.setStyleSpans(0, merged);
        } catch (Exception ignored) {
            // Never let a highlight-computation edge case leave stale/broken styling behind.
        }
    }

    /** Start/end offsets of the statement at the caret — or null if there's only one statement to begin with. */
    private int[] currentStatementCharRange(String text) {
        List<MongoShellParser.RawStatement> statements = MongoShellParser.splitStatements(text);
        if (statements.size() < 2) return null;
        MongoShellParser.RawStatement stmt = statementAtCaret(statements);
        if (stmt == null) return null;
        if (stmt.contentStart() >= stmt.end()) return null;
        return new int[]{stmt.contentStart(), stmt.end()};
    }

    private MongoShellParser.RawStatement statementAtCaret(List<MongoShellParser.RawStatement> statements) {
        return MongoShellParser.statementAt(statements, editor.getCaretPosition());
    }

    // ------------------------------------------------------------- completion

    /**
     * Autocomplete after {@code db}/{@code use}, {@code db.} (collection
     * names plus database-level methods), and {@code db.collection.}
     * (collection-level methods) — same popup interaction pattern as the
     * SQL console: Ctrl+Space to open, auto-opens while typing an
     * identifier, Up/Down/Enter/Tab/Escape to navigate and accept.
     */
    private void setupCompletion() {
        completionList.getStyleClass().add("completion-list");
        completionList.setPrefSize(360, 200);
        completionList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(MongoCompletionService.Suggestion item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                boolean isMethod = item.kind() == MongoCompletionService.Kind.METHOD;
                Label name = new Label(isMethod ? item.text() + "()" : item.text());
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
            } else if (completionPopup.isShowing()) {
                showCompletions();   // refresh after backspace etc.
            }
        });
    }

    private void showCompletions() {
        String token = currentToken();
        String textBeforeCaret = editor.getText().substring(0, tokenStart);
        MongoCompletionService.Context context = MongoCompletionService.contextAt(textBeforeCaret);

        if (context == MongoCompletionService.Context.DB_MEMBER) {
            ensureCollectionNamesCached();
        }

        List<MongoCompletionService.Suggestion> suggestions =
                MongoCompletionService.suggest(context, token, cachedCollectionNames);
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

    /** Fetches collection names once per database, in the background, then silently refreshes an open popup. */
    private void ensureCollectionNamesCached() {
        if (currentDatabase == null || currentDatabase.equals(cachedCollectionsDatabase)) return;
        String database = currentDatabase;
        cachedCollectionsDatabase = database;   // claim it immediately so a fast retype doesn't refetch repeatedly
        AppExecutor.run(() -> {
            try {
                List<String> names = ClientRegistry.mongo(profile).listCollectionNames(database);
                Platform.runLater(() -> {
                    cachedCollectionNames = names;
                    if (completionPopup.isShowing()) showCompletions();
                });
            } catch (Exception ignored) {
                // best-effort suggestions only — a fetch failure just means no collection names offered yet
            }
        });
    }

    /** Extracts the identifier token immediately before the caret (dots included, so "db.stu" is one token). */
    private String currentToken() {
        int caret = editor.getCaretPosition();
        String text = editor.getText();
        int start = caret;
        while (start > 0) {
            char c = text.charAt(start - 1);
            if (Character.isLetterOrDigit(c) || c == '_') start--;
            else break;
        }
        tokenStart = start;
        return text.substring(start, caret);
    }

    private void insertSelectedCompletion() {
        MongoCompletionService.Suggestion selected = completionList.getSelectionModel().getSelectedItem();
        if (selected == null || tokenStart < 0) {
            completionPopup.hide();
            return;
        }
        suppressCompletion = true;
        boolean isMethod = selected.kind() == MongoCompletionService.Kind.METHOD;
        String insertText = isMethod ? selected.text() + "()" : selected.text();
        editor.replaceText(tokenStart, editor.getCaretPosition(), insertText);
        if (isMethod) {
            // Land the caret between the parens, ready to type arguments —
            // matching the reference IDE's method-completion behavior.
            editor.moveTo(tokenStart + selected.text().length() + 1);
        }
        suppressCompletion = false;
        completionPopup.hide();
        editor.requestFocus();
    }

    // ------------------------------------------------------------- history

    private void setupHistory() {
        historyList.getStyleClass().add("completion-list");
        historyList.setPrefSize(560, 260);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d, HH:mm");
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

    public void showHistory() {
        List<QueryHistoryStore.Entry> entries = QueryHistoryStore.forConnection(profile.getId());
        if (entries.isEmpty()) {
            log("No query history yet for this connection");
            return;
        }
        historyList.getItems().setAll(entries);
        historyList.getSelectionModel().selectFirst();
        if (historyButton.getScene() != null && historyButton.getScene().getWindow() != null) {
            javafx.geometry.Point2D screenCoords = historyButton.localToScreen(0, 0);
            if (screenCoords != null) {
                historyPopup.show(historyButton, screenCoords.getX(),
                        screenCoords.getY() + historyButton.getHeight() + 2);
            } else {
                historyPopup.show(historyButton.getScene().getWindow());
            }
            historyList.requestFocus();
        }
    }

    private void insertSelectedHistory() {
        QueryHistoryStore.Entry selected = historyList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        suppressCompletion = true;
        String query = selected.sql();
        IndexRange selection = editor.getSelection();
        if (selection != null && selection.getLength() > 0) {
            editor.replaceText(selection, query);
        } else if (editor.getText().isBlank() || editor.getText().trim().startsWith("// MongoDB console.")) {
            editor.replaceText(query);
        } else {
            editor.replaceText(query);
        }
        editor.moveTo(Math.min(editor.getCaretPosition(), editor.getLength()));
        suppressCompletion = false;
        historyPopup.hide();
        editor.requestFocus();
    }

    public Button getHistoryButton() {
        return historyButton;
    }

    // --------------------------------------------------------------- run

    /** Run / Ctrl+Enter: the statement at the caret, or the only statement if there's just one. */
    public void runCurrentStatement() {
        String text = editor.getText();
        List<MongoShellParser.RawStatement> statements = MongoShellParser.splitStatements(text);
        if (statements.isEmpty()) return;

        MongoShellParser.RawStatement target = statements.size() == 1
                ? statements.get(0) : statementAtCaret(statements);
        if (target == null) return;

        String rawQuery = target.text().trim();
        QueryHistoryStore.record(profile.getId(), rawQuery);

        MongoShellParser.Statement stmt = MongoShellParser.parse(target.text());
        Platform.runLater(this::showDataPanel);
        AppExecutor.run(() -> {
            try {
                runOne(stmt);
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> log("Error: " + msg));
            }
        });
    }

    private void runOne(MongoShellParser.Statement stmt) {
        switch (stmt.kind()) {
            case USE -> {
                currentDatabase = stmt.database();
                Platform.runLater(() -> {
                    updateDbLabel();
                    log("use " + stmt.database());
                });
            }
            case COMMAND -> runCommand(stmt);
            case UNKNOWN -> Platform.runLater(() ->
                    log("Could not understand: " + shorten(stmt.raw())
                            + "  (expected \"use db;\" or \"db.collection.method(args);\")"));
        }
    }

    private void runCommand(MongoShellParser.Statement stmt) {
        if (currentDatabase == null) {
            Platform.runLater(() -> log("No database selected \u2014 run \"use <database>;\" first"));
            return;
        }
        MongoDbClient client = ClientRegistry.mongo(profile);
        String collection = stmt.collection();
        long start = System.currentTimeMillis();

        try {
            List<Document> args = parseArgs(stmt.argsJson());
            switch (stmt.method()) {
                case "insertOne" -> {
                    var result = client.insertOne(currentDatabase, collection, toJson(args, 0));
                    reportCommand(stmt, result.message(), start);
                }
                case "insertMany" -> {
                    List<Document> docs = args.size() == 1 && args.get(0).get("__array__") != null
                            ? castList(args.get(0).get("__array__")) : args;
                    var result = client.insertMany(currentDatabase, collection, docs);
                    reportCommand(stmt, result.message(), start);
                }
                case "updateOne" -> {
                    var result = client.updateOne(currentDatabase, collection, toJson(args, 0), toJson(args, 1));
                    reportCommand(stmt, result.message(), start);
                }
                case "updateMany" -> {
                    var result = client.updateMany(currentDatabase, collection, toJson(args, 0), toJson(args, 1));
                    reportCommand(stmt, result.message(), start);
                }
                case "deleteOne" -> {
                    var result = client.deleteOne(currentDatabase, collection, toJson(args, 0));
                    reportCommand(stmt, result.message(), start);
                }
                case "deleteMany" -> {
                    var result = client.deleteMany(currentDatabase, collection, toJson(args, 0));
                    reportCommand(stmt, result.message(), start);
                }
                case "countDocuments" -> {
                    long count = client.countDocuments(currentDatabase, collection, toJson(args, 0));
                    reportCommand(stmt, count + " document(s)", start);
                }
                case "drop" -> {
                    var result = client.drop(currentDatabase, collection);
                    reportCommand(stmt, result.message(), start);
                }
                case "find" -> {
                    lastFindDatabase = currentDatabase;
                    lastFindCollection = collection;
                    lastFindFilter = args.isEmpty() ? "{}" : toJson(args, 0);
                    lastFindWasSingle = false;
                    sortField = null;
                    sortDirection = null;
                    findPage = 0;
                    findTotalDocs = -1;
                    showFindResult(start);
                }
                case "findOne" -> {
                    lastFindDatabase = currentDatabase;
                    lastFindCollection = collection;
                    lastFindFilter = args.isEmpty() ? "{}" : toJson(args, 0);
                    lastFindWasSingle = true;
                    sortField = null;
                    sortDirection = null;
                    findPage = 0;
                    findTotalDocs = -1;
                    showFindResult(start);
                }
                default -> Platform.runLater(() -> log("Unsupported method: " + stmt.method()
                        + "  (supported: findOne, insertOne, insertMany, find, updateOne, updateMany, "
                        + "deleteOne, deleteMany, countDocuments, drop)"));
            }
        } catch (Exception ex) {
            String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            Platform.runLater(() -> log("Error in " + shorten(stmt.raw()) + ": " + msg));
        }
    }

    /** Re-runs the most recent find()/findOne() — used by both the sort icon and Revert, so both act on fresh data. */
    private void rerunLastFind() {
        if (lastFindCollection == null) return;
        AppExecutor.run(() -> showFindResult(System.currentTimeMillis()));
    }

    private void showFindResult(long start) {
        MongoDbClient client = ClientRegistry.mongo(profile);
        try {
            QueryResult result;
            if (lastFindWasSingle) {
                result = client.findOne(lastFindDatabase, lastFindCollection, lastFindFilter);
            } else {
                result = client.find(lastFindDatabase, lastFindCollection, lastFindFilter,
                        sortField, "DESC".equals(sortDirection), findPage * findPageSize, findPageSize);
                if (findTotalDocs < 0) {
                    try {
                        findTotalDocs = client.countDocuments(lastFindDatabase, lastFindCollection, lastFindFilter);
                    } catch (Exception ignore) {
                        findTotalDocs = -1;
                    }
                }
            }
            QueryResult finalResult = result;
            Platform.runLater(() -> {
                if (lastFindCollection != null && !lastFindCollection.isBlank()) {
                    String tabTitle = (lastFindDatabase != null ? lastFindDatabase + "." : "") + lastFindCollection;
                    resultTab.setText(tabTitle);
                    resultTab.setGraphic(Icons.of(FontAwesomeSolid.TABLE, "#4a88c7", 11));
                }
                resultGrid.setCurrentSort(sortField, sortDirection);
                editManager.configure(lastFindDatabase, lastFindCollection, finalResult);
                resultGrid.showResult(finalResult);
                bottomTabs.getSelectionModel().select(resultTab);
                updateFindPagingUi(finalResult);
                String statusText = finalResult.getRows().size() + " document(s) in " + (System.currentTimeMillis() - start) + " ms";
                statusLabel.setText(statusText);
                log("db." + lastFindCollection + "." + (lastFindWasSingle ? "findOne" : "find")
                        + "(...) \u2192 " + statusText);
            });
        } catch (Exception ex) {
            String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            Platform.runLater(() -> {
                statusLabel.setText("Error: " + msg);
                log("Error running find(): " + msg);
            });
        }
    }

    private void updateFindPagingUi(QueryResult result) {
        if (lastFindWasSingle) {
            pagingWidget.update(1, 1, 1, true);
            return;
        }
        int rows = result.getRows().size();
        long from = rows == 0 ? 0 : (long) findPage * findPageSize + 1;
        long to = from + rows - 1;
        pagingWidget.update(from, to, findTotalDocs, findTotalDocs >= 0);
    }

    private void applyPagingPlacement(TablePagingWidget.Placement placement) {
        if (pagingWidget.getParent() instanceof Pane parent) {
            parent.getChildren().remove(pagingWidget);
        }
        if (dataToolbar != null) {
            dataToolbar.getChildren().remove(pagingWidget);
        }
        if (resultPagingBar != null) {
            resultPagingBar.setLeft(null);
            resultPagingBar.setCenter(null);
            resultPagingBar.setRight(null);
        }
        pagingWidget.setPlacement(placement);
        if (resultPagingBar == null) return;

        switch (placement) {
            case BOTTOM_CENTER -> {
                resultPagingBar.setLeft(statusLabel);
                BorderPane.setAlignment(statusLabel, Pos.CENTER_LEFT);
                resultPagingBar.setCenter(pagingWidget);
                BorderPane.setAlignment(pagingWidget, Pos.CENTER);
                resultPagingBar.setRight(positionLabel);
                BorderPane.setAlignment(positionLabel, Pos.CENTER_RIGHT);
            }
            case BOTTOM_LEFT -> {
                HBox leftBox = new HBox(10, pagingWidget, statusLabel);
                leftBox.setAlignment(Pos.CENTER_LEFT);
                resultPagingBar.setLeft(leftBox);
                resultPagingBar.setCenter(null);
                resultPagingBar.setRight(positionLabel);
                BorderPane.setAlignment(positionLabel, Pos.CENTER_RIGHT);
            }
            case BOTTOM_RIGHT -> {
                resultPagingBar.setLeft(statusLabel);
                BorderPane.setAlignment(statusLabel, Pos.CENTER_LEFT);
                resultPagingBar.setCenter(null);
                HBox rightBox = new HBox(10, positionLabel, pagingWidget);
                rightBox.setAlignment(Pos.CENTER_RIGHT);
                resultPagingBar.setRight(rightBox);
            }
            case TOOLBAR -> {
                resultPagingBar.setLeft(statusLabel);
                BorderPane.setAlignment(statusLabel, Pos.CENTER_LEFT);
                resultPagingBar.setCenter(null);
                resultPagingBar.setRight(positionLabel);
                BorderPane.setAlignment(positionLabel, Pos.CENTER_RIGHT);
                if (dataToolbar != null && !dataToolbar.getChildren().contains(pagingWidget)) {
                    dataToolbar.getChildren().add(pagingWidget);
                }
            }
        }
    }

    public TablePagingWidget getPagingWidget() {
        return pagingWidget;
    }

    private void reportCommand(MongoShellParser.Statement stmt, String message, long start) {
        Platform.runLater(() -> log(stmt.raw().replaceAll("\\s+", " ") + "  \u2192  " + message
                + "  (" + (System.currentTimeMillis() - start) + " ms)"));
    }

    /** Parses the JSON-array wrapper MongoShellParser produces back into individual documents. */
    private List<Document> parseArgs(String argsJsonArray) {
        if (argsJsonArray == null || argsJsonArray.isBlank()) return List.of();
        Object parsed = Document.parse("{\"__wrap__\":" + argsJsonArray + "}").get("__wrap__");
        List<Document> result = new ArrayList<>();
        if (parsed instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Document doc) {
                    result.add(doc);
                } else if (item instanceof List<?> arr) {
                    // a bare array argument (e.g. insertMany's [doc, doc]) — wrap so
                    // callers can unwrap it back out via the __array__ sentinel below
                    Document wrapper = new Document();
                    wrapper.put("__array__", arr);
                    result.add(wrapper);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Document> castList(Object raw) {
        List<Document> result = new ArrayList<>();
        for (Object item : (List<Object>) raw) {
            if (item instanceof Document doc) result.add(doc);
        }
        return result;
    }

    private static String toJson(List<Document> args, int index) {
        return index < args.size() ? args.get(index).toJson() : "{}";
    }

    private static String shorten(String s) {
        String flat = s.replaceAll("\\s+", " ").strip();
        return flat.length() > 80 ? flat.substring(0, 80) + "\u2026" : flat;
    }
}
