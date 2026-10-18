package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.db.LocalHistoryStore;
import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.db.QueryHistoryStore;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.QueryResult;
import com.roze.dbnavigator.util.AppExecutor;
import com.roze.dbnavigator.util.QueryExecutionResolver;
import com.roze.dbnavigator.util.SqlParameters;
import com.roze.dbnavigator.util.SqlReformatter;
import com.roze.dbnavigator.util.SqlStatementSplitter;
import com.roze.dbnavigator.util.UnsafeQueryDetector;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
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
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Window;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.roze.dbnavigator.db.ConnectionStore;
import java.sql.Connection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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
 * SQL console. Autocomplete (tables → columns → keywords,
 * Enter/Tab inserts), and simple single-table SELECT results are editable:
 * change cells / delete rows, then Submit commits everything in one transaction.
 */
public class QueryTab extends Tab {

    private final MainWindow mainWindow;
    private final ConnectionProfile profile;
    private String catalog;   // nullable: active database of the console
    private MenuButton dbSelectorButton;
    private HBox introspectionBanner;
    private Label bannerLabel;
    private Hyperlink bannerLink;
    private List<String> cachedDatabases;
    private final CodeArea editor = SqlHighlighter.createEditor();
    private final VirtualizedScrollPane<CodeArea> editorScroll;
    private final StackPane editorStack = new StackPane();
    private final Pane statementOverlay = new Pane();
    private final javafx.scene.shape.Rectangle statementBoundingBox = new javafx.scene.shape.Rectangle();
    private final Set<Integer> executedLines = new java.util.TreeSet<>();
    private int lastCaretParagraph = -1;
    private final VBox editorContainer = new VBox();
    private final HBox breadcrumbsBar = new HBox(6);
    private final Label breadcrumbsFileLabel = new Label("console.sql");
    private final Label breadcrumbsSepLabel = new Label("›");
    private final Label breadcrumbsStmtLabel = new Label("SELECT");
    private final ResultGrid resultGrid = new ResultGrid();
    private final TabPane resultsTabPane = new TabPane();
    private final VBox dataPanelContainer = new VBox();
    private Button hideDataPanelButton;
    private double lastDataPanelDivider = 0.45;
    private int resultTabCounter = 1;
    private QueryExecutionChooserPopup activeChooserPopup;
    private final SplitPane editorResultSplit = new SplitPane();
    private final Label statusLabel = new Label("Ready");
    private final Spinner<Integer> limitSpinner = new Spinner<>(10, 100_000, 500, 100);
    private final Button runButton = new Button("Run");
    private final Button cancelButton = new Button("Cancel");
    private final Button reloadButton = new Button();
    private final Button historyButton = new Button();
    private final Button statementsButton = new Button();
    private final Button submitButton = new Button("Submit");
    private final Button revertButton = new Button("Revert");
    private GridEditManager editManager;

    private final AtomicReference<java.sql.Statement> runningStatement = new AtomicReference<>();
    private final Popup historyPopup = new Popup();
    private final ListView<QueryHistoryStore.Entry> historyList = new ListView<>();
    private String lastExecutedSql;
    // The true original statement a result grid's rows came from — kept
    // separate from lastExecutedSql so re-sorting always wraps the real
    // base query, never an already-sort-wrapped one (which would otherwise
    // nest deeper on every click).
    private String baseSqlForSort;
    private String currentSortColumn;
    private String currentSortDirection;
    private double currentEditorFontSize;
    private String currentEditorFontFamily;

    // ---- tab context menu: Bookmarks / Override File Type / Open In ----
    private final Set<Integer> bookmarkedLines = new java.util.TreeSet<>();
    private boolean plainTextMode = false;
    private File savedFile;

    // ---- pagination state (see PagedResultCursor) ----
    private final TablePagingWidget pagingWidget = new TablePagingWidget();
    private BorderPane statusBar;
    private final Label positionLabel = new Label();
    private com.roze.dbnavigator.db.PagedResultCursor activeCursor;
    private int currentPageStart = 0;
    private String currentEditableTable;
    private List<String> currentPkColumns = List.of();
    private Map<String, Integer> currentColumnTypes = Map.of();
    private boolean currentViaCtid = false;

    /** Stable identity for this console's Local History (independent of catalog suffix in the tab label). */
    private final String fileId;

    // ---- autocomplete ----
    private final Popup completionPopup = new Popup();
    private final ListView<CompletionService.Suggestion> completionList = new ListView<>();
    private int tokenStart = -1;
    private boolean suppressCompletion = false;

    public QueryTab(ConnectionProfile profile, String catalog, String title) {
        this(null, profile, catalog, title);
    }

    public QueryTab(MainWindow mainWindow, ConnectionProfile profile, String catalog, String title) {
        FontLoader.loadBundledFonts();
        this.mainWindow = mainWindow;
        this.profile = profile;
        this.catalog = catalog;
        this.fileId = normalizeTitle(title, catalog);
        setText(fileId + (catalog != null ? " [" + catalog + "]" : ""));
        setGraphic(Icons.of(FontAwesomeSolid.TERMINAL, "#6897bb", 11));

        // ---- Toolbar ----
        runButton.setGraphic(Icons.of(FontAwesomeSolid.PLAY, "#57965c", 11));
        runButton.getStyleClass().add("run-button");
        runButton.setTooltip(new Tooltip("Execute (Ctrl+Enter) — Right-click for options"));
        runButton.setOnAction(e -> executeAction(0));

        ContextMenu runMenu = new ContextMenu();
        MenuItem exec1 = new MenuItem("Execute (Ctrl+Enter)");
        exec1.setOnAction(e -> executeAction(0));
        MenuItem exec2 = new MenuItem("Execute (2) (Ctrl+Shift+Enter)");
        exec2.setOnAction(e -> executeAction(1));
        MenuItem exec3 = new MenuItem("Execute (3) (Ctrl+Alt+Enter)");
        exec3.setOnAction(e -> executeAction(2));
        MenuItem toFile = new MenuItem("Execute to File…");
        toFile.setOnAction(e -> executeToFile());
        MenuItem explain = new MenuItem("Explain Plan");
        explain.setOnAction(e -> showExecutionPlan());
        runMenu.getItems().addAll(exec1, exec2, exec3, new SeparatorMenuItem(), toFile, explain);
        runButton.setContextMenu(runMenu);

        cancelButton.setGraphic(Icons.of(FontAwesomeSolid.STOP_CIRCLE, "#e05555", 11));
        cancelButton.setTooltip(new Tooltip("Cancel the running query"));
        cancelButton.setVisible(false);
        cancelButton.setManaged(false);
        cancelButton.setOnAction(e -> cancelRunningQuery());

        boolean isMac = System.getProperty("os.name", "").toLowerCase().contains("mac");
        String historyShortcut = isMac ? "⌥⌘E" : "Ctrl+Alt+E";
        historyButton.setGraphic(Icons.of(FontAwesomeSolid.HISTORY, "#a9b7c6", 11));
        historyButton.setTooltip(new Tooltip("Browse Query History (" + historyShortcut + ")"));
        historyButton.setOnAction(e -> showHistory());

        statementsButton.setGraphic(Icons.of(FontAwesomeSolid.LIST_UL, "#a9b7c6", 11));
        statementsButton.setTooltip(new Tooltip("Choose Statement to Run"));
        statementsButton.setOnAction(e -> showStatementsPopup());

        reloadButton.setGraphic(Icons.of(FontAwesomeSolid.SYNC_ALT, "#6897bb", 11));
        reloadButton.setTooltip(new Tooltip("Reload Page  Ctrl+F5"));
        reloadButton.setOnAction(e -> handleReloadQuery());


        submitButton.setGraphic(Icons.of(FontAwesomeSolid.CHECK, "#57965c", 11));
        submitButton.setTooltip(new Tooltip("Commit pending result edits/deletes"));
        revertButton.setGraphic(Icons.of(FontAwesomeSolid.UNDO, "#e05555", 11));
        revertButton.setTooltip(new Tooltip("Discard pending edits and re-run the query"));

        editManager = new GridEditManager(profile, catalog, resultGrid,
                submitButton, revertButton, this::rerunLastSql, statusLabel::setText);


        limitSpinner.setEditable(true);
        limitSpinner.setPrefWidth(95);

        Node dbSelectorNode = setupDbSelector();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, runButton, cancelButton, historyButton, statementsButton, spacer, dbSelectorNode);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.getStyleClass().add("console-toolbar");

        editor.replaceText("-- " + profile.getType().getDisplayName()
                + " console. Ctrl+Enter runs, Ctrl+Space completes.\n");
        editor.moveTo(0);
        editorScroll = new VirtualizedScrollPane<>(editor);
        VBox.setVgrow(editorScroll, Priority.ALWAYS);
        initStatementOverlay();
        initBreadcrumbsBar();
        applyEditorFontFromSettings();
        applyEditorAppearanceFromSettings();
        setupCtrlScrollFontZoom(editorScroll);
        setupKeyboardFontZoom();
        resultGrid.setSortRequestListener(this::sortByColumn);

        // Local History: first snapshot is the "Create" entry; further edits are
        // auto-captured after a pause (avoids saving a snapshot per keystroke).
        LocalHistoryStore.record(fileId, editor.getText());
        editor.plainTextChanges()
                .successionEnds(Duration.ofSeconds(3))
                .subscribe(change -> LocalHistoryStore.record(fileId, editor.getText()));

        editor.selectedTextProperty().addListener((obs, oldSel, newSel) -> {
            MainWindow mw = mainWindow != null ? mainWindow : MainWindow.getInstance();
            if (mw != null) {
                com.roze.dbnavigator.ui.action.ActionManager.getInstance().updateActions(mw);
            }
        });
        editor.undoAvailableProperty().addListener((obs, oldVal, newVal) -> {
            MainWindow mw = mainWindow != null ? mainWindow : MainWindow.getInstance();
            if (mw != null) {
                com.roze.dbnavigator.ui.action.ActionManager.getInstance().updateActions(mw);
            }
        });
        editor.redoAvailableProperty().addListener((obs, oldVal, newVal) -> {
            MainWindow mw = mainWindow != null ? mainWindow : MainWindow.getInstance();
            if (mw != null) {
                com.roze.dbnavigator.ui.action.ActionManager.getInstance().updateActions(mw);
            }
        });

        setOnClosed(e -> {
            LocalHistoryStore.record(fileId, editor.getText());
            closeActiveCursor();
        });

        pagingWidget.setActivePageSize(currentPageSize());
        pagingWidget.setOnFirst(() -> {
            if (editManager != null && !editManager.confirmAndClearPendingIfDiscarded(getOwnerWindow())) return;
            currentPageStart = 0; displayCurrentPage();
        });
        pagingWidget.setOnPrev(() -> {
            if (editManager != null && !editManager.confirmAndClearPendingIfDiscarded(getOwnerWindow())) return;
            currentPageStart = Math.max(0, currentPageStart - currentPageSize());
            displayCurrentPage();
        });
        pagingWidget.setOnNext(() -> {
            if (editManager != null && !editManager.confirmAndClearPendingIfDiscarded(getOwnerWindow())) return;
            pageForward();
        });
        pagingWidget.setOnLast(() -> {
            if (editManager != null && !editManager.confirmAndClearPendingIfDiscarded(getOwnerWindow())) return;
            pageToLast();
        });
        pagingWidget.setOnCountRows(this::countCursorRows);
        pagingWidget.setOnPageSizeChanged(newSize -> {
            if (limitSpinner != null && limitSpinner.getValueFactory() != null) {
                limitSpinner.getValueFactory().setValue(newSize);
            }
            if (activeCursor != null) {
                activeCursor.setPageSize(newSize);
            }
            rerunLastSql();
        });
        pagingWidget.setOnPlacementChanged(this::applyPagingPlacement);
        pagingWidget.setOnReload(this::handleReloadQuery);

        // ---- Layout ----
        Pane initialResultContent = wrapResultGridWithToolbar(resultGrid);
        Tab initialResultTab = new Tab("Result 1", initialResultContent);
        initialResultTab.setClosable(false);
        initialResultTab.setUserData(editManager);
        resultsTabPane.getTabs().add(initialResultTab);
        resultsTabPane.setStyle("-fx-tab-min-width: 80px;");
        resultsTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                ResultGrid activeGrid = null;
                if (newTab.getUserData() instanceof GridEditManager mgr) {
                    editManager = mgr;
                    editManager.updateButtons();
                    activeGrid = extractResultGrid(newTab);
                } else {
                    ResultGrid grid = extractResultGrid(newTab);
                    if (grid != null) {
                        editManager = new GridEditManager(profile, catalog, grid,
                                submitButton, revertButton, this::rerunLastSql, statusLabel::setText);
                        newTab.setUserData(editManager);
                        activeGrid = grid;
                    }
                }
                if (activeGrid != null) {
                    positionLabel.textProperty().unbind();
                    positionLabel.textProperty().bind(activeGrid.selectionStatusProperty());
                }
                if (pagingWidget.getPlacement() == TablePagingWidget.Placement.TOOLBAR) {
                    applyPagingPlacement(TablePagingWidget.Placement.TOOLBAR);
                }
            }
        });

        StackPane tabPaneStack = new StackPane();
        VBox.setVgrow(tabPaneStack, Priority.ALWAYS);
        tabPaneStack.getChildren().add(resultsTabPane);

        String hideShortcut = isMac ? "⇧⎋" : "Shift+Escape";

        this.hideDataPanelButton = new Button();
        this.hideDataPanelButton.setGraphic(Icons.of(FontAwesomeSolid.MINUS, "#a9b7c6", 11));
        this.hideDataPanelButton.setTooltip(new Tooltip("Hide  " + hideShortcut));
        this.hideDataPanelButton.getStyleClass().add("run-panel-chrome-button");
        this.hideDataPanelButton.setOnAction(e -> hideDataPanel());

        Button moreOptionsButton = new Button();
        moreOptionsButton.setGraphic(Icons.of(FontAwesomeSolid.ELLIPSIS_V, "#a9b7c6", 11));
        moreOptionsButton.setTooltip(new Tooltip("More Options"));
        moreOptionsButton.getStyleClass().add("run-panel-chrome-button");
        ContextMenu moreMenu = new ContextMenu();
        MenuItem hideItem = new MenuItem("Hide (" + hideShortcut + ")");
        hideItem.setOnAction(e -> hideDataPanel());
        MenuItem closeTabsItem = new MenuItem("Close Result Tabs");
        closeTabsItem.setOnAction(e -> {
            while (resultsTabPane.getTabs().size() > 1) {
                resultsTabPane.getTabs().remove(resultsTabPane.getTabs().size() - 1);
            }
        });
        MenuItem exportCsvItem = new MenuItem("Export CSV");
        exportCsvItem.setOnAction(e -> activeResultGrid().exportCsv());
        moreMenu.getItems().addAll(hideItem, new SeparatorMenuItem(), closeTabsItem, exportCsvItem);
        moreOptionsButton.setOnAction(e -> moreMenu.show(moreOptionsButton, javafx.geometry.Side.BOTTOM, 0, 4));

        HBox chromeButtons = new HBox(2, moreOptionsButton, hideDataPanelButton);
        chromeButtons.setAlignment(Pos.CENTER_RIGHT);
        chromeButtons.setPickOnBounds(false);
        chromeButtons.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(chromeButtons, Pos.TOP_RIGHT);
        StackPane.setMargin(chromeButtons, new Insets(3, 6, 0, 0));

        tabPaneStack.getChildren().add(chromeButtons);

        statusLabel.getStyleClass().add("console-status");
        positionLabel.getStyleClass().add("console-status");
        positionLabel.textProperty().bind(resultGrid.selectionStatusProperty());
        statusBar = new BorderPane();
        statusBar.setPadding(new Insets(4, 10, 4, 10));
        statusBar.getStyleClass().add("console-status-bar");
        applyPagingPlacement(TablePagingWidget.Placement.BOTTOM_CENTER);

        dataPanelContainer.getChildren().addAll(tabPaneStack, statusBar);
        VBox.setVgrow(tabPaneStack, Priority.ALWAYS);
        dataPanelContainer.setMinHeight(100);

        editorResultSplit.getItems().addAll(editorContainer, dataPanelContainer);
        editorResultSplit.setOrientation(Orientation.VERTICAL);
        editorResultSplit.setDividerPositions(0.45);

        setupIntrospectionBanner();
        VBox root = new VBox(introspectionBanner, toolbar, editorResultSplit);
        VBox.setVgrow(editorResultSplit, Priority.ALWAYS);
        setContent(root);

        setupCompletion();
        setupHistory();
        setupEditorContextMenu();
        setupStatementHighlighting();
        setupOutsideClickDismiss();
        CompletionService.preload(profile, catalog);

        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if ((e.isControlDown() && e.getCode() == KeyCode.F5) || e.getCode() == KeyCode.F5) {
                handleReloadQuery();
                e.consume();
            } else if (new KeyCodeCombination(KeyCode.ESCAPE, KeyCombination.SHIFT_DOWN).match(e)) {
                if (isDataPanelVisible()) {
                    hideDataPanel();
                    e.consume();
                }
            } else if (new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN).match(e)
                    || (e.isControlDown() && e.isAltDown() && e.getCode() == KeyCode.E)) {
                showHistory();
                e.consume();
            } else if (new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN).match(e)) {
                executeAction(1);
                e.consume();
            } else if (new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN).match(e)) {
                executeAction(2);
                e.consume();
            } else if (new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN).match(e)) {
                executeAction(0);
                e.consume();
            }
        });
    }

    /** Pre-fills the editor, e.g. from "New Query on table". */
    public void setSql(String sql) {
        suppressCompletion = true;
        editor.replaceText(sql);
        editor.moveTo(0);
        suppressCompletion = false;
    }

    public String getLastExecutedSql() {
        return lastExecutedSql;
    }

    public void setLastExecutedSql(String sql) {
        this.lastExecutedSql = sql;
    }

    public CodeArea getEditor() {
        return editor;
    }

    public void focusEditor() {
        if (editor != null) {
            editor.requestFocus();
        }
    }

    public void formatSql() {
        String selected = editor.getSelectedText();
        if (selected != null && !selected.isBlank()) {
            String formatted = SqlFormatter.format(selected);
            editor.replaceSelection(formatted);
        } else {
            String text = editor.getText();
            if (text != null && !text.isBlank()) {
                int caret = editor.getCaretPosition();
                String formatted = SqlFormatter.format(text);
                editor.replaceText(formatted);
                editor.moveTo(Math.min(caret, editor.getLength()));
            }
        }
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
            if (!completionPopup.isShowing()) {
                if (tryExpandPostfixCompletion(e.getCode())) {
                    e.consume();
                    return;
                }

                AppSettingsStore.Settings st = AppSettingsStore.load();
                // Smart Keys: Jump outside bracket/quote on Tab
                if (e.getCode() == KeyCode.TAB && !e.isShiftDown() && !e.isControlDown() && !e.isAltDown() && !e.isMetaDown()) {
                    if (st.isSmartKeysJumpOutsideBracketWithTab()) {
                        int caret = editor.getCaretPosition();
                        if (caret < editor.getLength()) {
                            char next = editor.getText().charAt(caret);
                            if (next == ')' || next == ']' || next == '}' || next == '>' || next == '\'' || next == '"') {
                                editor.moveTo(caret + 1);
                                e.consume();
                                return;
                            }
                        }
                    }
                }

                // Smart Keys: Smart indent and SQL Smart Keys on Enter
                if (e.getCode() == KeyCode.ENTER && !e.isShiftDown() && !e.isControlDown() && !e.isAltDown() && !e.isMetaDown()) {
                    int caret = editor.getCaretPosition();
                    String text = editor.getText();
                    int lineStart = text.lastIndexOf('\n', caret - 1);
                    lineStart = lineStart == -1 ? 0 : lineStart + 1;
                    String lineBefore = text.substring(lineStart, caret);
                    StringBuilder indent = new StringBuilder();
                    for (int i = 0; i < lineBefore.length(); i++) {
                        char ch = lineBefore.charAt(i);
                        if (ch == ' ' || ch == '\t') indent.append(ch);
                        else break;
                    }

                    // SQL Smart Key: Close code blocks on Enter (CASE ... END, BEGIN ... END)
                    if (st.isSmartKeysSqlCloseCodeBlocksOnEnter()) {
                        String trimmed = lineBefore.trim().toUpperCase();
                        if (trimmed.endsWith("CASE") || trimmed.endsWith("BEGIN")) {
                            editor.insertText(caret, "\n" + indent + "    \n" + indent + "END");
                            editor.moveTo(caret + 1 + indent.length() + 4);
                            e.consume();
                            return;
                        }
                    }

                    // SQL Smart Key: Insert string concatenation on Enter inside string literal
                    if (st.isSmartKeysSqlInsertStringConcatOnEnter()) {
                        int quotesBefore = 0;
                        for (int i = lineStart; i < caret; i++) {
                            if (text.charAt(i) == '\'') quotesBefore++;
                        }
                        if (quotesBefore % 2 == 1) {
                            editor.insertText(caret, "' ||\n" + indent + "'");
                            editor.moveTo(caret + 5 + indent.length());
                            e.consume();
                            return;
                        }
                    }

                    if (st.isSmartKeysEnterSmartIndent()) {
                        if (st.isSmartKeysEnterInsertPairBrace() && lineBefore.trim().endsWith("{")) {
                            editor.insertText(caret, "\n" + indent + "    \n" + indent + "}");
                            editor.moveTo(caret + 1 + indent.length() + 4);
                            e.consume();
                            return;
                        } else if (indent.length() > 0) {
                            editor.insertText(caret, "\n" + indent);
                            editor.moveTo(caret + 1 + indent.length());
                            e.consume();
                            return;
                        }
                    }
                }
                return;
            }

            AppSettingsStore.Settings s = AppSettingsStore.load();
            boolean acceptByKey = false;
            String extraAccept = s.getAdditionalAcceptCharacters();
            if (extraAccept != null && !extraAccept.isEmpty() && e.getText() != null && !e.getText().isEmpty()) {
                if (extraAccept.contains(e.getText())) {
                    acceptByKey = true;
                }
            }
            if (s.isInsertSelectedSuggestionByContextKeys()) {
                if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.PERIOD) {
                    acceptByKey = true;
                }
            }
            if (acceptByKey) {
                insertSelectedCompletion();
                e.consume();
                return;
            }

            switch (e.getCode()) {
                case DOWN -> { completionList.getSelectionModel().selectNext(); e.consume(); }
                case UP -> { completionList.getSelectionModel().selectPrevious(); e.consume(); }
                case ENTER, TAB -> { insertSelectedCompletion(); e.consume(); }
                case ESCAPE -> { completionPopup.hide(); e.consume(); }
                default -> {}
            }
        });

        editor.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            String typed = e.getCharacter();
            if (typed == null || typed.isEmpty()) return;
            char c = typed.charAt(0);
            AppSettingsStore.Settings s = AppSettingsStore.load();

            IndexRange sel = editor.getSelection();
            boolean hasSel = sel != null && sel.getLength() > 0;

            if (s.isSmartKeysSurroundSelectionOnQuoteOrBrace() && hasSel) {
                if (c == '(' || c == '[' || c == '{' || c == '<' || c == '\'' || c == '"') {
                    char close = c == '(' ? ')' : (c == '[' ? ']' : (c == '{' ? '}' : (c == '<' ? '>' : c)));
                    String selText = editor.getSelectedText();
                    int start = sel.getStart();
                    editor.replaceText(start, sel.getEnd(), c + selText + close);
                    editor.selectRange(start + 1, start + 1 + selText.length());
                    e.consume();
                    return;
                }
            }

            if (s.isSmartKeysInsertPairedBrackets()) {
                if (c == '(') {
                    int caret = editor.getCaretPosition();
                    editor.insertText(caret, "()");
                    editor.moveTo(caret + 1);
                    e.consume();
                    return;
                } else if (c == '[') {
                    int caret = editor.getCaretPosition();
                    editor.insertText(caret, "[]");
                    editor.moveTo(caret + 1);
                    e.consume();
                    return;
                } else if (c == '{') {
                    int caret = editor.getCaretPosition();
                    editor.insertText(caret, "{}");
                    editor.moveTo(caret + 1);
                    e.consume();
                    return;
                }
                if (c == ')' || c == ']' || c == '}') {
                    int caret = editor.getCaretPosition();
                    if (caret < editor.getLength() && editor.getText().charAt(caret) == c) {
                        editor.moveTo(caret + 1);
                        e.consume();
                        return;
                    }
                }
            }

            if (s.isSmartKeysInsertPairQuote()) {
                if (c == '\'' || c == '"') {
                    int caret = editor.getCaretPosition();
                    if (caret < editor.getLength() && editor.getText().charAt(caret) == c) {
                        editor.moveTo(caret + 1);
                        e.consume();
                        return;
                    }
                    editor.insertText(caret, "" + c + c);
                    editor.moveTo(caret + 1);
                    e.consume();
                    return;
                }
            }
        });

        editor.plainTextChanges().subscribe(change -> {
            if (suppressCompletion) return;
            if (!AppSettingsStore.load().isShowSuggestionsAsYouType()) {
                if (completionPopup.isShowing()) {
                    completionPopup.hide();
                }
                return;
            }
            String inserted = change.getInserted();
            if (inserted.length() == 1 && inserted.matches("[A-Za-z0-9_.]")) {
                showCompletions();
            } else if (inserted.equals(" ")
                    && contextAt(editor.getCaretPosition()) != CompletionService.Context.ANY) {
                showCompletions();   // popup right after FROM/SELECT/…
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
                CompletionService.suggest(profile, catalog, editor.getText(), token, context);

        AppSettingsStore.Settings s = AppSettingsStore.load();
        if (s.isMatchCase() && !token.isEmpty()) {
            String mode = s.getMatchCaseMode();
            suggestions = suggestions.stream().filter(sug -> {
                String t = sug.text();
                if (t.isEmpty()) return false;
                if ("First letter only".equalsIgnoreCase(mode)) {
                    return t.charAt(0) == token.charAt(0);
                } else if ("All letters".equalsIgnoreCase(mode)) {
                    return t.startsWith(token);
                }
                return true;
            }).toList();
        }

        if (s.isSortSuggestionsAlphabetically()) {
            suggestions = new ArrayList<>(suggestions);
            suggestions.sort(Comparator.comparing(CompletionService.Suggestion::text, String.CASE_INSENSITIVE_ORDER));
        }

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

    private static final Pattern COMPLETION_CONTEXT_KEYWORD = Pattern.compile(
        "(?i)\\b(select|from|join|update|into|table|sequence|where|on|and|or|by|set|having|between|like|when|then)\\b");

    private CompletionService.Context contextAt(int position) {
        String text = editor.getText();
        int pos = Math.min(position, text.length());
        String before = text.substring(0, pos).toLowerCase(Locale.ROOT);
        Matcher matcher = COMPLETION_CONTEXT_KEYWORD.matcher(before);
        String lastKeyword = null;
        while (matcher.find()) {
            lastKeyword = matcher.group(1);
        }
        if (lastKeyword == null) return CompletionService.Context.ANY;
        return switch (lastKeyword) {
            case "from", "join", "update", "into", "table" -> CompletionService.Context.TABLES;
            case "sequence" -> CompletionService.Context.SEQUENCES;
            case "select", "where", "on", "and", "or", "by", "set",
                 "having", "between", "like", "when", "then" -> CompletionService.Context.COLUMNS;
            default -> CompletionService.Context.ANY;
        };
    }

    private void insertSelectedCompletion() {
        CompletionService.Suggestion selected =
                completionList.getSelectionModel().getSelectedItem();
        if (selected == null || tokenStart < 0) {
            completionPopup.hide();
            return;
        }
        suppressCompletion = true;
        AppSettingsStore.Settings s = AppSettingsStore.load();
        String replacement = selected.text();

        // Automatically add parentheses if applicable
        if (s.isInsertParenthesesAutomatically()) {
            if (replacement.endsWith("(")) {
                replacement = replacement + ")";
            }
        }

        // Table alias auto-add
        if (selected.kind() == CompletionService.Kind.TABLE && s.isTableAliasesAutoAdd()) {
            String alias = null;
            if (s.getCustomTableAliases() != null) {
                for (AppSettingsStore.TableAliasConfig tac : s.getCustomTableAliases()) {
                    if (tac.getTableName() != null && tac.getTableName().equalsIgnoreCase(selected.text())
                            && tac.getCustomAlias() != null && !tac.getCustomAlias().isBlank()) {
                        alias = tac.getCustomAlias();
                        break;
                    }
                }
            }
            if (alias == null || alias.isBlank()) {
                StringBuilder sb = new StringBuilder();
                for (String part : selected.text().split("_")) {
                    if (!part.isEmpty()) sb.append(Character.toLowerCase(part.charAt(0)));
                }
                alias = sb.toString();
                if (alias.isEmpty()) alias = "t";
            }
            replacement = replacement + " " + alias;
        }

        editor.replaceText(tokenStart, editor.getCaretPosition(), replacement);
        suppressCompletion = false;
        completionPopup.hide();
        editor.requestFocus();
    }

    private boolean tryExpandPostfixCompletion(KeyCode code) {
        AppSettingsStore.Settings s = AppSettingsStore.load();
        if (!s.isPostfixCompletionEnabled()) return false;

        String expandWith = s.getPostfixCompletionExpandWith();
        boolean matchesKey = false;
        if ("Tab".equalsIgnoreCase(expandWith) && code == KeyCode.TAB) matchesKey = true;
        else if ("Space".equalsIgnoreCase(expandWith) && code == KeyCode.SPACE) matchesKey = true;
        else if ("Enter".equalsIgnoreCase(expandWith) && code == KeyCode.ENTER) matchesKey = true;
        if (!matchesKey) return false;

        int caret = editor.getCaretPosition();
        if (caret <= 0) return false;
        String text = editor.getText();
        if (caret > text.length()) caret = text.length();

        int lineStart = text.lastIndexOf('\n', caret - 1);
        lineStart = lineStart == -1 ? 0 : lineStart + 1;
        String line = text.substring(lineStart, caret);

        int dotIndex = line.lastIndexOf('.');
        if (dotIndex <= 0) return false;

        String key = line.substring(dotIndex + 1).trim();
        if (key.isEmpty()) return false;

        int exprEnd = dotIndex;
        int exprStart = exprEnd;
        while (exprStart > 0) {
            char ch = line.charAt(exprStart - 1);
            if (Character.isWhitespace(ch) || ch == '(' || ch == ',' || ch == ';') {
                break;
            }
            exprStart--;
        }
        String expr = line.substring(exprStart, exprEnd).trim();
        if (expr.isEmpty()) return false;

        List<AppSettingsStore.PostfixTemplateConfig> templates = s.getPostfixTemplates();
        if (templates == null) return false;

        for (AppSettingsStore.PostfixTemplateConfig t : templates) {
            if (t.isEnabled() && key.equalsIgnoreCase(t.getKey())) {
                int absoluteStart = lineStart + exprStart;
                int absoluteEnd = caret;

                String templateExpr = t.getExpression();
                if (templateExpr == null || templateExpr.isEmpty()) {
                    templateExpr = t.getDescription();
                }

                String expanded;
                if (templateExpr.contains("$EXPR$")) {
                    expanded = templateExpr.replace("$EXPR$", expr);
                } else if (templateExpr.toLowerCase().contains("authors")) {
                    expanded = templateExpr.replaceAll("(?i)\\bauthors\\b", expr);
                } else {
                    expanded = templateExpr + " " + expr;
                }

                if (expanded.contains("|")) {
                    int pipeIdx = expanded.indexOf('|');
                    expanded = expanded.replaceFirst("\\|", "");
                    editor.replaceText(absoluteStart, absoluteEnd, expanded);
                    editor.moveTo(absoluteStart + pipeIdx);
                } else {
                    editor.replaceText(absoluteStart, absoluteEnd, expanded);
                    editor.moveTo(absoluteStart + expanded.length());
                }
                return true;
            }
        }
        return false;
    }

    /** Pre-filled console text accessor (used by File → Save Console As…). */
    public String getSqlText() {
        return editor.getText();
    }

    // -------------------------------------------------------- local history

    public String getFileId() { return fileId; }

    /**
     * Real, previously-shipped bug this heals: a title already carrying
     * one or more accumulated " [catalog]" suffixes (from a
     * session.json saved before the fix for this existed - see
     * MainWindow.saveSession's own real account of that bug) would
     * otherwise get yet another suffix appended on top the very next
     * time it's loaded, since this constructor always appends
     * unconditionally regardless of what the title already ends with.
     * Strips every trailing occurrence of the exact suffix this
     * constructor is about to append, in one pass, so an already-
     * corrupted title (potentially several suffixes deep from repeated
     * restarts before the fix existed) is normalized back to its real,
     * original base title immediately, rather than merely growing more
     * slowly than before.
     */
    private static String normalizeTitle(String title, String catalog) {
        if (catalog == null) return title;
        String suffix = " [" + catalog + "]";
        while (title.endsWith(suffix)) {
            title = title.substring(0, title.length() - suffix.length());
        }
        return title;
    }
    public ConnectionProfile getProfile() { return profile; }
    public String getCatalog() { return catalog; }

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
     * Context menu on the editor, with a different item
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

    // ---------------------------------------------------- statement highlight

    /**
     * Highlights the character range of the statement the caret is inside —
     * not the whole line(s) it's on — so two statements sharing one line
     * (e.g. "SELECT ...; SELECT ...;" typed on a single line) each get their
     * own precise highlight instead of the shared line lighting up for both,
     * matching the reference IDE. Implemented as an overlay merged with the
     * syntax-highlighting style spans (RichTextFX combines the style-class
     * sets per character via {@code StyleSpans.overlay}), rather than a
     * paragraph-level style — that's what makes sub-line precision possible.
     *
     * This recomputes and re-applies syntax highlighting itself rather than
     * relying solely on {@link SqlHighlighter}'s own auto-highlighting
     * subscription (still active, since {@code StructureTab}'s read-only DDL
     * viewer also uses {@code SqlHighlighter.createEditor()} and needs it) —
     * registered after that one, this pass's result is simply what's
     * actually shown once both have run for a given change.
     */
    private void initStatementOverlay() {
        statementOverlay.setPickOnBounds(false);
        statementOverlay.setMouseTransparent(true);
        statementBoundingBox.setStroke(javafx.scene.paint.Color.web("#385e43"));
        statementBoundingBox.setStrokeWidth(1.0);
        statementBoundingBox.setFill(javafx.scene.paint.Color.web("#365847", 0.04));
        statementBoundingBox.setArcWidth(4.0);
        statementBoundingBox.setArcHeight(4.0);
        statementBoundingBox.setVisible(false);
        statementOverlay.getChildren().add(statementBoundingBox);

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(editorScroll.widthProperty());
        clip.heightProperty().bind(editorScroll.heightProperty());
        statementOverlay.setClip(clip);

        editorStack.getChildren().setAll(editorScroll, statementOverlay);
        VBox.setVgrow(editorStack, Priority.ALWAYS);

        editorScroll.estimatedScrollYProperty().addListener((obs, o, n) -> Platform.runLater(this::updateStatementOverlay));
        editorScroll.estimatedScrollXProperty().addListener((obs, o, n) -> Platform.runLater(this::updateStatementOverlay));
        editor.widthProperty().addListener((obs, o, n) -> Platform.runLater(this::updateStatementOverlay));
        editor.heightProperty().addListener((obs, o, n) -> Platform.runLater(this::updateStatementOverlay));
    }

    private void updateStatementOverlay() {
        try {
            String text = editor.getText();
            int[] range = currentStatementCharRange(text);
            if (range == null || range[1] <= range[0] || statementOverlay.getScene() == null) {
                statementBoundingBox.setVisible(false);
                return;
            }

            int startPara = editor.offsetToPosition(range[0], org.fxmisc.richtext.model.TwoDimensional.Bias.Forward).getMajor();
            int endPara = editor.offsetToPosition(range[1], org.fxmisc.richtext.model.TwoDimensional.Bias.Backward).getMajor();

            int firstVisible = editor.firstVisibleParToAllParIndex();
            int lastVisible = editor.lastVisibleParToAllParIndex();
            if (firstVisible >= 0 && lastVisible >= 0) {
                if (endPara < firstVisible || startPara > lastVisible) {
                    statementBoundingBox.setVisible(false);
                    return;
                }
            }

            // 1. Determine gutter right boundary in statementOverlay coordinates
            double gutterRightX = -1;
            var pText = editor.lookup(".paragraph-box .paragraph-text");
            if (pText != null) {
                Point2D p = statementOverlay.screenToLocal(pText.localToScreen(0, 0));
                if (p != null && p.getX() > 10.0) {
                    gutterRightX = p.getX();
                }
            }
            if (gutterRightX < 0) {
                var pBox = editor.lookup(".paragraph-box");
                if (pBox != null) {
                    Point2D pb = statementOverlay.screenToLocal(pBox.localToScreen(0, 0));
                    if (pb != null && pb.getX() >= 0) {
                        gutterRightX = pb.getX() + 46.5;
                    }
                }
            }
            if (gutterRightX < 0) {
                gutterRightX = 46.5;
            }

            // Left edge is anchored immediately after gutter with a clean 2px breathing margin
            double boxX = gutterRightX + 2.0;

            // 2. Compute vertical bounds (minY, maxY) and character maxX
            double minFoundY = Double.MAX_VALUE;
            double maxFoundY = Double.MIN_VALUE;
            double maxCharX = boxX + 60.0;
            boolean anyBoundsFound = false;

            for (int p = startPara; p <= endPara; p++) {
                int pStart = editor.getAbsolutePosition(p, 0);
                int pLen = editor.getParagraph(p).length();
                if (pLen == 0) {
                    var optBounds = editor.getParagraphBoundsOnScreen(p);
                    if (optBounds.isPresent()) {
                        Bounds pb = optBounds.get();
                        Point2D pTop = statementOverlay.screenToLocal(pb.getMinX(), pb.getMinY());
                        Point2D pBottom = statementOverlay.screenToLocal(pb.getMinX(), pb.getMaxY());
                        if (pTop != null && pBottom != null) {
                            minFoundY = Math.min(minFoundY, Math.min(pTop.getY(), pBottom.getY()));
                            maxFoundY = Math.max(maxFoundY, Math.max(pTop.getY(), pBottom.getY()));
                            anyBoundsFound = true;
                        }
                    }
                    continue;
                }
                int pEnd = pStart + pLen;
                int charFrom = Math.max(range[0], pStart);
                int charTo = Math.min(range[1], pEnd);
                if (charTo > charFrom) {
                    Optional<Bounds> optBounds = editor.getCharacterBoundsOnScreen(charFrom, charTo);
                    if (optBounds.isPresent()) {
                        Bounds sb = optBounds.get();
                        Point2D p1 = statementOverlay.screenToLocal(sb.getMinX(), sb.getMinY());
                        Point2D p2 = statementOverlay.screenToLocal(sb.getMaxX(), sb.getMaxY());
                        if (p1 != null && p2 != null) {
                            maxCharX = Math.max(maxCharX, Math.max(p1.getX(), p2.getX()));
                            minFoundY = Math.min(minFoundY, Math.min(p1.getY(), p2.getY()));
                            maxFoundY = Math.max(maxFoundY, Math.max(p1.getY(), p2.getY()));
                            anyBoundsFound = true;
                        }
                    } else {
                        var optPb = editor.getParagraphBoundsOnScreen(p);
                        if (optPb.isPresent()) {
                            Bounds pb = optPb.get();
                            Point2D pTop = statementOverlay.screenToLocal(pb.getMinX(), pb.getMinY());
                            Point2D pBottom = statementOverlay.screenToLocal(pb.getMinX(), pb.getMaxY());
                            if (pTop != null && pBottom != null) {
                                minFoundY = Math.min(minFoundY, Math.min(pTop.getY(), pBottom.getY()));
                                maxFoundY = Math.max(maxFoundY, Math.max(pTop.getY(), pBottom.getY()));
                                anyBoundsFound = true;
                            }
                        }
                    }
                }
            }

            // Estimate width from longest line in statement so maxX never collapses when lines are virtualized
            double charWidth = 8.2;
            int maxLineLen = 0;
            for (int p = startPara; p <= endPara; p++) {
                maxLineLen = Math.max(maxLineLen, editor.getParagraph(p).length());
            }
            double estimatedMaxX = boxX + (maxLineLen * charWidth);
            double boxMaxX = Math.max(maxCharX, estimatedMaxX);

            // Compute robust Y bounds using visible reference paragraph if needed
            double computedMinY = minFoundY;
            double computedMaxY = maxFoundY;
            if (!anyBoundsFound || minFoundY >= maxFoundY) {
                int refPara = Math.max(startPara, Math.min(firstVisible >= 0 ? firstVisible : startPara, endPara));
                var optPb = editor.getParagraphBoundsOnScreen(refPara);
                double lineH = 21.6;
                double refY = 0;
                if (optPb.isPresent()) {
                    lineH = Math.max(16.0, optPb.get().getHeight());
                    Point2D pt = statementOverlay.screenToLocal(0, optPb.get().getMinY());
                    if (pt != null) refY = pt.getY();
                }
                computedMinY = refY - (refPara - startPara) * lineH;
                computedMaxY = computedMinY + (endPara - startPara + 1) * lineH;
            }

            boolean isLight = com.roze.dbnavigator.db.AppSettingsStore.load().getTheme() == com.roze.dbnavigator.db.AppSettingsStore.Theme.LIGHT;
            if (isLight) {
                statementBoundingBox.setStroke(javafx.scene.paint.Color.web("#57965c"));
                statementBoundingBox.setFill(javafx.scene.paint.Color.web("#57965c", 0.04));
            } else {
                statementBoundingBox.setStroke(javafx.scene.paint.Color.web("#385e43"));
                statementBoundingBox.setFill(javafx.scene.paint.Color.web("#365847", 0.04));
            }
            statementBoundingBox.setStrokeWidth(1.2);
            statementBoundingBox.setArcWidth(6.0);
            statementBoundingBox.setArcHeight(6.0);

            double x = boxX;
            double y = computedMinY - 1.5;
            double w = Math.max(60, (boxMaxX - boxX) + 18.0);
            double h = Math.max(18, (computedMaxY - computedMinY) + 3.0);

            statementBoundingBox.setX(x);
            statementBoundingBox.setY(y);
            statementBoundingBox.setWidth(w);
            statementBoundingBox.setHeight(h);
            statementBoundingBox.setVisible(true);
        } catch (Exception ignored) {
            statementBoundingBox.setVisible(false);
        }
    }

    javafx.scene.shape.Rectangle getStatementBoundingBox() {
        return statementBoundingBox;
    }

    void refreshEditorStylingPublic() {
        refreshEditorStyling();
    }

    private void setupStatementHighlighting() {
        editor.multiPlainChanges()
                .successionEnds(Duration.ofMillis(120))
                .subscribe(ignore -> {
                    refreshEditorStyling();
                    updateBreadcrumbsText();
                    Platform.runLater(this::updateStatementOverlay);
                });
        editor.caretPositionProperty().addListener((obs, o, n) -> {
            int prevCur = lastCaretParagraph;
            int newCur = editor.getCurrentParagraph();
            lastCaretParagraph = newCur;
            refreshEditorStyling();
            updateBreadcrumbsText();
            Platform.runLater(this::updateStatementOverlay);
            if (prevCur != newCur && prevCur >= 0 && prevCur < editor.getParagraphs().size()) {
                try {
                    editor.recreateParagraphGraphic(prevCur);
                    editor.recreateParagraphGraphic(newCur);
                } catch (Exception ignored) {}
            }
        });
        editor.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                e -> Platform.runLater(() -> {
                    refreshEditorStyling();
                    updateBreadcrumbsText();
                    updateStatementOverlay();
                }));
        Platform.runLater(() -> {
            refreshEditorStyling();
            updateBreadcrumbsText();
            updateStatementOverlay();
        });
    }

    private void initBreadcrumbsBar() {
        breadcrumbsBar.setAlignment(Pos.CENTER_LEFT);
        breadcrumbsBar.setPadding(new Insets(3, 10, 3, 10));
        breadcrumbsBar.setStyle("-fx-background-color: -bg-darkest; -fx-border-color: -border-subtle; -fx-border-width: 1 0 1 0;");
        breadcrumbsFileLabel.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        breadcrumbsSepLabel.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        breadcrumbsStmtLabel.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 11px; -fx-font-weight: bold;");
        breadcrumbsBar.getChildren().addAll(breadcrumbsFileLabel, breadcrumbsSepLabel, breadcrumbsStmtLabel);
    }

    private void updateBreadcrumbsText() {
        try {
            String text = editor.getText();
            int[] range = currentStatementCharRange(text);
            String file = (catalog != null ? catalog : profile.getName()) + ".sql";
            breadcrumbsFileLabel.setText(file);
            if (range != null && range[1] > range[0]) {
                String stmt = text.substring(range[0], range[1]).trim();
                String firstWord = stmt.split("\\s+")[0].toUpperCase();
                breadcrumbsStmtLabel.setText(firstWord.isBlank() ? "Statement" : firstWord);
            } else {
                int line = editor.getCurrentParagraph() + 1;
                int col = editor.getCaretColumn() + 1;
                breadcrumbsStmtLabel.setText("Line " + line + ":" + col);
            }
        } catch (Exception ignored) {}
    }

    private java.util.function.IntFunction<Node> createDataGripGutterFactory() {
        return lineIdx -> {
            HBox gutter = new HBox(4);
            gutter.setAlignment(Pos.CENTER_RIGHT);
            gutter.setPadding(new Insets(0, 4, 0, 8));
            gutter.getStyleClass().add("lineno");

            Label lineNum = new Label();
            lineNum.getStyleClass().add("lineno-text");
            lineNum.setAlignment(Pos.CENTER_RIGHT);

            int totalLines = Math.max(1, editor.getParagraphs().size());
            int digits = Math.max(2, String.valueOf(totalLines).length());
            double numWidth = Math.max(18.0, digits * 8.5);
            lineNum.setMinWidth(numWidth);
            lineNum.setPrefWidth(numWidth);

            var settings = com.roze.dbnavigator.db.AppSettingsStore.load();
            String mode = settings.getLineNumbersMode();
            int cur = editor.getCurrentParagraph();
            if ("Relative".equalsIgnoreCase(mode)) {
                lineNum.setText(String.valueOf(Math.abs(lineIdx - cur)));
            } else if ("Hybrid".equalsIgnoreCase(mode)) {
                lineNum.setText((lineIdx == cur) ? String.valueOf(lineIdx + 1) : String.valueOf(Math.abs(lineIdx - cur)));
            } else {
                lineNum.setText(String.valueOf(lineIdx + 1));
            }

            Label icon = new Label();
            icon.setMinWidth(14);
            icon.setPrefWidth(14);
            icon.setMaxWidth(14);
            icon.setAlignment(Pos.CENTER);
            if (executedLines.contains(lineIdx)) {
                icon.setText("✓");
                icon.setStyle("-fx-text-fill: #57965c; -fx-font-size: 11px; -fx-font-weight: bold;");
                icon.setTooltip(new Tooltip("Statement executed successfully"));
            } else if (bookmarkedLines.contains(lineIdx)) {
                icon.setText("★");
                icon.setStyle("-fx-text-fill: #3574f0; -fx-font-size: 11px;");
                icon.setTooltip(new Tooltip("Bookmarked line"));
            } else if (lineIdx == cur && isInsideExecutableStatement()) {
                icon.setText("💡");
                icon.setStyle("-fx-font-size: 10px;");
            } else {
                icon.setText("");
            }

            gutter.getChildren().addAll(lineNum, icon);
            return gutter;
        };
    }

    private boolean isInsideExecutableStatement() {
        try {
            int[] range = currentStatementCharRange(editor.getText());
            return range != null && range[1] > range[0];
        } catch (Exception ignored) {
            return false;
        }
    }

    /** File → Settings → Editor → General → Appearance & Breadcrumbs, applied here and re-applied after Apply/OK. */
    public void applyEditorAppearanceFromSettings() {
        var settings = com.roze.dbnavigator.db.AppSettingsStore.load();
        syncEditorStylesheet(settings.getTheme());

        // 1. Line numbers (with DataGrip execution checkmarks & gutter styling)
        if (!settings.isShowLineNumbers()) {
            editor.setParagraphGraphicFactory(null);
        } else {
            editor.setParagraphGraphicFactory(createDataGripGutterFactory());
        }

        // 2. Caret blinking
        try {
            var caret = editor.getCaretSelectionBind().getUnderlyingCaret();
            if (caret != null) {
                if (settings.isCaretBlinking()) {
                    caret.setBlinkRate(javafx.util.Duration.millis(settings.getCaretBlinkingMs()));
                } else {
                    caret.setBlinkRate(javafx.util.Duration.ZERO);
                }
            }
        } catch (Exception ignored) {}

        // 3. Breadcrumbs
        if (editorContainer != null && editorStack != null) {
            boolean show = settings.isShowBreadcrumbs();
            if (!show) {
                editorContainer.getChildren().setAll(editorStack);
            } else {
                String placement = settings.getBreadcrumbsPlacement();
                if ("Top".equalsIgnoreCase(placement)) {
                    editorContainer.getChildren().setAll(breadcrumbsBar, editorStack);
                } else {
                    editorContainer.getChildren().setAll(editorStack, breadcrumbsBar);
                }
                updateBreadcrumbsText();
            }
        }
    }

    /** File → Settings → Editor → Font, applied here and re-applied after Apply/OK. */
    public void applyEditorFontFromSettings() {
        var settings = com.roze.dbnavigator.db.AppSettingsStore.load();
        currentEditorFontFamily = settings.getEditorFontFamily();
        if ("DBNavigator Mono".equalsIgnoreCase(currentEditorFontFamily)) {
            currentEditorFontFamily = "DBNavigator Mono";
        }
        currentEditorFontSize = settings.getEditorFontSize();
        updateEditorFontStyle();
    }

    private void syncEditorStylesheet(com.roze.dbnavigator.db.AppSettingsStore.Theme theme) {
        try {
            String themeUrl = ThemeManager.stylesheetUrl(theme);
            if (!editor.getStylesheets().contains(themeUrl)) {
                editor.getStylesheets().removeIf(s -> s.contains("app.css") || s.contains("app-light.css"));
                editor.getStylesheets().add(themeUrl);
            }
        } catch (Exception ignored) {}
    }

    private void updateEditorFontStyle() {
        var settings = com.roze.dbnavigator.db.AppSettingsStore.load();
        syncEditorStylesheet(settings.getTheme());

        String weight = settings.getEditorFontMainWeight();
        String fxWeight = "normal";
        if ("Bold".equalsIgnoreCase(weight) || "ExtraBold".equalsIgnoreCase(weight) || "Bold Recommended".equalsIgnoreCase(weight)) {
            fxWeight = "bold";
        } else if ("Light".equalsIgnoreCase(weight) || "ExtraLight".equalsIgnoreCase(weight) || "Thin".equalsIgnoreCase(weight)) {
            fxWeight = "100";
        }
        double effectiveFontSize = currentEditorFontSize <= 13.0 ? 13.5 : currentEditorFontSize;
        String fam = (currentEditorFontFamily != null && !currentEditorFontFamily.isBlank())
                ? currentEditorFontFamily : "DBNavigator Mono";
        if ("DBNavigator Mono".equalsIgnoreCase(fam)) {
            fam = "DBNavigator Mono";
        }
        editor.setStyle("-fx-font-family: '" + fam + "', 'DBNavigator Mono', 'Menlo', 'Monaco', 'Consolas', monospace; "
                + "-fx-font-size: " + effectiveFontSize + "px; "
                + "-fx-font-weight: " + fxWeight + ";");
    }

    /**
     * Ctrl+= / Ctrl+Plus (numpad or main keyboard) makes the font bigger;
     * Ctrl+- / Ctrl+Minus makes it smaller. A keyboard shortcut has none of
     * the scroll-direction ambiguity the wheel-based zoom can run into (OS-
     * level gesture handling can intercept Ctrl+Scroll before it ever
     * reaches the app, particularly upward scrolls on some platforms) — this
     * is the reliable fallback if that's happening.
     */
    private void setupKeyboardFontZoom() {
        editor.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (!e.isControlDown()) return;
            boolean zoomIn = e.getCode() == KeyCode.PLUS || e.getCode() == KeyCode.EQUALS || e.getCode() == KeyCode.ADD;
            boolean zoomOut = e.getCode() == KeyCode.MINUS || e.getCode() == KeyCode.SUBTRACT;
            if (!zoomIn && !zoomOut) return;
            currentEditorFontSize = Math.max(8, Math.min(48, currentEditorFontSize + (zoomIn ? 1 : -1)));
            updateEditorFontStyle();
            e.consume();
        });
    }

    /**
     * File → Settings → Editor → General → "Change font size with Ctrl+Mouse Wheel".
     *
     * Registered on the wrapping VirtualizedScrollPane rather than the CodeArea
     * itself: filters fire top-down during the capture phase, so a filter on
     * the parent runs before the event ever reaches the CodeArea's own
     * internal scroll-panning handling. That handling appears to special-case
     * scrolling up at the top of a short document (nothing left to pan to),
     * which was swallowing the upward gesture before the CodeArea-level
     * filter's zoom-in logic ever got a reliable chance to run — registering
     * one level higher sidesteps that regardless of scroll direction.
     */
    private void setupCtrlScrollFontZoom(VirtualizedScrollPane<CodeArea> scrollPane) {
        scrollPane.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
            if (!e.isControlDown()) return;
            if (!com.roze.dbnavigator.db.AppSettingsStore.load().isCtrlScrollZoomEnabled()) return;
            currentEditorFontSize = Math.max(8, Math.min(48, currentEditorFontSize + (e.getDeltaY() > 0 ? 1 : -1)));
            updateEditorFontStyle();
            e.consume();
        });
    }

    /**
     * A click anywhere in the main window should dismiss any of this
     * console's own popups/menus that happen to be open — completion,
     * history, the Statements picker, and the editor's right-click menu.
     * {@code Popup.setAutoHide(true)} / {@code ContextMenu} normally handle
     * this on their own, but RichTextFX's editor appears to consume mouse
     * presses at a level those built-in listeners never see, which is why
     * Escape worked but clicking elsewhere didn't. This adds an explicit,
     * reliable backstop: any press reaching the *main* scene is by
     * definition outside these popups, since each one lives in its own
     * separate window/scene.
     */
    private void setupOutsideClickDismiss() {
        editor.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> dismissTransientPopups());
            }
        });
        if (editor.getScene() != null) {
            editor.getScene().addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                    e -> dismissTransientPopups());
        }
    }

    private void dismissTransientPopups() {
        if (editorMenu.isShowing()) editorMenu.hide();
        if (completionPopup.isShowing()) completionPopup.hide();
        if (historyPopup.isShowing()) historyPopup.hide();
        if (statementsPopup.isShowing()) statementsPopup.hide();
        if (activeChooserPopup != null && activeChooserPopup.isShowing()) activeChooserPopup.hide();
    }

    /** Recomputes syntax highlighting and overlays the current-statement and active row styling in DataGrip fashion. */
    private void refreshEditorStyling() {
        try {
            String text = editor.getText();
            org.fxmisc.richtext.model.StyleSpans<java.util.Collection<String>> syntax =
                    plainTextMode ? emptySpans(text.length()) : SqlHighlighter.computeHighlighting(text);
            editor.setStyleSpans(0, syntax);

            int currentPara = editor.getCurrentParagraph();
            int count = editor.getParagraphs().size();
            for (int i = 0; i < count; i++) {
                List<String> paraStyles = new ArrayList<>();
                if (i == currentPara) {
                    paraStyles.add("current-line-highlight");
                }
                if (bookmarkedLines.contains(i)) {
                    paraStyles.add("bookmarked-line");
                }
                editor.setParagraphStyle(i, paraStyles);
            }
            updateStatementOverlay();
        } catch (Exception ignored) {
            // Never let a highlight-computation edge case leave stale/broken styling behind.
        }
    }

    private static org.fxmisc.richtext.model.StyleSpans<java.util.Collection<String>> emptySpans(int length) {
        var builder = new org.fxmisc.richtext.model.StyleSpansBuilder<java.util.Collection<String>>();
        builder.add(java.util.Collections.emptyList(), length);
        return builder.create();
    }

    // ----------------------------------------------------------- bookmarks

    /** Whole-line markers (paragraph-level, combined with statement highlight above). */
    private void refreshBookmarkParagraphStyles() {
        refreshEditorStyling();
    }

    /** Tab context menu → Bookmarks → Toggle Bookmark. */
    public void toggleBookmarkAtCaret() {
        int line = editor.getCurrentParagraph();
        if (!bookmarkedLines.remove(line)) {
            bookmarkedLines.add(line);
        }
        refreshEditorStyling();
    }

    public boolean isBookmarkedAtCaret() {
        return bookmarkedLines.contains(editor.getCurrentParagraph());
    }

    /** Used by MainWindow's "Show Bookmarks…" to build the cross-console list. */
    public List<Integer> getBookmarkedLines() {
        return new ArrayList<>(bookmarkedLines);
    }

    public String getLineText(int line) {
        return line >= 0 && line < editor.getParagraphs().size() ? editor.getParagraph(line).getText() : "";
    }

    public void jumpToLine(int line) {
        int clamped = Math.max(0, Math.min(line, editor.getParagraphs().size() - 1));
        editor.moveTo(clamped, 0);
        editor.requestFollowCaret();
        editor.requestFocus();
    }

    // ----------------------------------------------------- override file type

    public boolean isPlainTextMode() { return plainTextMode; }

    /** Tab context menu → Override File Type → SQL / Plain Text. */
    public void setPlainTextMode(boolean plainText) {
        this.plainTextMode = plainText;
        refreshEditorStyling();
    }

    // ----------------------------------------------------------- open in

    public File getSavedFile() { return savedFile; }

    /** Set by MainWindow once "Save Console As…" succeeds, so "Open In" has a real file to act on. */
    public void setSavedFile(File file) { this.savedFile = file; }

    /**
     * Start/end character offsets of the statement at the caret, trimmed of
     * surrounding whitespace — or null if there's nothing ambiguous to call out (a
     * single-statement console, or the caret isn't inside any statement).
     * A statement's raw range includes any blank/whitespace lines separating
     * it from the previous one (that gap has to belong to *someone's*
     * range) — trimming to the first/last non-whitespace character keeps the
     * highlight on the statement's own text only.
     */
    private int[] currentStatementCharRange(String text) {
        List<SqlStatementSplitter.Statement> statements = SqlStatementSplitter.split(text);
        if (statements.isEmpty()) return null;

        SqlStatementSplitter.Statement stmt =
                SqlStatementSplitter.statementAt(statements, editor.getCaretPosition());
        if (stmt == null) return null;

        String stmtText = stmt.text();
        int leadingWs = 0;
        while (leadingWs < stmtText.length() && Character.isWhitespace(stmtText.charAt(leadingWs))) leadingWs++;
        int trailingWs = 0;
        while (trailingWs < stmtText.length() - leadingWs
                && Character.isWhitespace(stmtText.charAt(stmtText.length() - 1 - trailingWs))) trailingWs++;
        int trimmedStart = stmt.start() + leadingWs;
        int trimmedEnd = stmt.end() - trailingWs;
        if (trimmedStart >= trimmedEnd) return null;
        return new int[]{trimmedStart, trimmedEnd};
    }

    /** Reused across right-clicks so a stale instance never lingers behind a new one. */
    private final ContextMenu editorMenu = new ContextMenu();

    private void showEditorContextMenu(ContextMenuEvent event) {
        editorMenu.hide();   // in case a previous one is still up for any reason
        boolean hasSelection = editor.getSelectedText() != null && !editor.getSelectedText().isBlank();
        editorMenu.getItems().clear();
        editorMenu.setAutoHide(true);
        editorMenu.setHideOnEscape(true);
        ContextMenu menu = editorMenu;

        AppSettingsStore.Settings settings = AppSettingsStore.load();
        AppSettingsStore.MenuItemConfig popupConfig = settings.getMenuConfig("root.editor.popup");
        if (popupConfig != null && !popupConfig.getChildren().isEmpty()) {
            buildConfiguredEditorContextMenu(menu, popupConfig, hasSelection);
        } else {
            buildDefaultEditorContextMenu(menu, hasSelection);
        }

        menu.show(editor, event.getScreenX(), event.getScreenY());
        event.consume();
    }

    private void buildConfiguredEditorContextMenu(ContextMenu menu, AppSettingsStore.MenuItemConfig popupConfig, boolean hasSelection) {
        if (popupConfig.getChildren() != null) {
            for (AppSettingsStore.MenuItemConfig child : popupConfig.getChildren()) {
                appendConfigItemToEditorContextMenu(child, menu, hasSelection);
            }
        }
        cleanTrailingSeparators(menu);
    }

    private void appendConfigItemToEditorContextMenu(AppSettingsStore.MenuItemConfig child, ContextMenu menu, boolean hasSelection) {
        if (child.getType() == AppSettingsStore.MenuItemConfig.Type.SEPARATOR) {
            addSeparatorToContextMenu(menu);
        } else if (child.getType() == AppSettingsStore.MenuItemConfig.Type.GROUP) {
            if (child.isPopup()) {
                Menu sub = new Menu(child.getText());
                buildConfiguredSubMenu(sub, child, hasSelection);
                menu.getItems().add(sub);
            } else if (child.getChildren() != null) {
                for (AppSettingsStore.MenuItemConfig grandChild : child.getChildren()) {
                    appendConfigItemToEditorContextMenu(grandChild, menu, hasSelection);
                }
            }
        } else {
            MenuItem item = createEditorActionMenuItem(child, hasSelection);
            if (item != null) {
                menu.getItems().add(item);
            }
        }
    }

    private void buildConfiguredSubMenu(Menu menu, AppSettingsStore.MenuItemConfig groupConfig, boolean hasSelection) {
        if (groupConfig.getChildren() != null) {
            for (AppSettingsStore.MenuItemConfig child : groupConfig.getChildren()) {
                appendConfigItemToSubMenu(child, menu, hasSelection);
            }
        }
        cleanTrailingSeparators(menu);
    }

    private void appendConfigItemToSubMenu(AppSettingsStore.MenuItemConfig child, Menu menu, boolean hasSelection) {
        if (child.getType() == AppSettingsStore.MenuItemConfig.Type.SEPARATOR) {
            addSeparatorToMenu(menu);
        } else if (child.getType() == AppSettingsStore.MenuItemConfig.Type.GROUP) {
            if (child.isPopup()) {
                Menu sub = new Menu(child.getText());
                buildConfiguredSubMenu(sub, child, hasSelection);
                menu.getItems().add(sub);
            } else if (child.getChildren() != null) {
                for (AppSettingsStore.MenuItemConfig grandChild : child.getChildren()) {
                    appendConfigItemToSubMenu(grandChild, menu, hasSelection);
                }
            }
        } else {
            MenuItem item = createEditorActionMenuItem(child, hasSelection);
            if (item != null) {
                menu.getItems().add(item);
            }
        }
    }

    private void addSeparatorToContextMenu(ContextMenu menu) {
        if (!menu.getItems().isEmpty() && !(menu.getItems().get(menu.getItems().size() - 1) instanceof SeparatorMenuItem)) {
            menu.getItems().add(new SeparatorMenuItem());
        }
    }

    private void addSeparatorToMenu(Menu menu) {
        if (!menu.getItems().isEmpty() && !(menu.getItems().get(menu.getItems().size() - 1) instanceof SeparatorMenuItem)) {
            menu.getItems().add(new SeparatorMenuItem());
        }
    }

    private void cleanTrailingSeparators(ContextMenu menu) {
        while (!menu.getItems().isEmpty() && menu.getItems().get(menu.getItems().size() - 1) instanceof SeparatorMenuItem) {
            menu.getItems().remove(menu.getItems().size() - 1);
        }
    }

    private void cleanTrailingSeparators(Menu menu) {
        while (!menu.getItems().isEmpty() && menu.getItems().get(menu.getItems().size() - 1) instanceof SeparatorMenuItem) {
            menu.getItems().remove(menu.getItems().size() - 1);
        }
    }

    private MenuItem createEditorActionMenuItem(AppSettingsStore.MenuItemConfig child, boolean hasSelection) {
        String id = child.getId();
        String text = child.getText();
        switch (id) {
            case "edit.cut": {
                MenuItem mi = action(text, new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN), editor::cut);
                mi.setDisable(!hasSelection);
                return mi;
            }
            case "edit.copy": {
                MenuItem mi = action(text, new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN), editor::copy);
                mi.setDisable(!hasSelection);
                return mi;
            }
            case "edit.copy.plain": {
                MenuItem mi = action(text, null, () -> mainWindow.copyAsPlainTextCurrentEditor());
                mi.setDisable(!hasSelection);
                return mi;
            }
            case "edit.copy.reference": {
                MenuItem mi = action(text, null, () -> mainWindow.copyPathOrReferenceCurrentEditor());
                mi.setDisable(!hasSelection);
                return mi;
            }
            case "edit.copy.rich": {
                MenuItem mi = action(text, null, editor::copy);
                mi.setDisable(!hasSelection);
                return mi;
            }
            case "edit.paste":
            case "edit.paste.plain":
                return action(text, new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN), editor::paste);
            case "middle.run":
            case "run.run":
                return action(text, new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHORTCUT_DOWN), () -> executeAction(0));
            case "editor.explain.plan":
                return action(text, null, this::showExecutionPlan);
            case "code.reformat":
                return action(text, new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN), this::reformatCode);
            case "editor.popup.show.context.actions":
                return action(text, new KeyCodeCombination(KeyCode.ENTER, KeyCombination.ALT_DOWN), () -> mainWindow.setStatus(text));
            case "diff.compare.clipboard":
                return action(text, null, () -> mainWindow.setStatus("Compare with Clipboard"));
            case "editor.search.with.google":
                return action(text, null, () -> mainWindow.setStatus("Search with Google"));
            case "editor.jump.ddl":
                return action(text, null, () -> mainWindow.setStatus("DDL"));
            default: {
                com.roze.dbnavigator.ui.action.AnAction regAction = com.roze.dbnavigator.ui.action.ActionManager.getInstance().getAction(id);
                if (regAction != null) {
                    MenuItem mi = regAction.createMenuItem(mainWindow);
                    if (text != null && !text.isBlank()) {
                        mi.setText(text);
                    }
                    return mi;
                }
                return action(text, null, () -> mainWindow.setStatus(text));
            }
        }
    }

    private void buildDefaultEditorContextMenu(ContextMenu menu, boolean hasSelection) {

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
        menu.getItems().add(action("Execute (Ctrl+Enter)",
                new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHORTCUT_DOWN), () -> executeAction(0)));
        menu.getItems().add(action("Execute (2) (Ctrl+Shift+Enter)",
                new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), () -> executeAction(1)));
        menu.getItems().add(action("Execute (3) (Ctrl+Alt+Enter)",
                new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN), () -> executeAction(2)));
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

    /**
     * Resolves what "the current query" means for an action with no
     * explicit target: an active text selection wins; otherwise the
     * statement the caret is currently inside (multi-statement consoles);
     * otherwise the whole editor as a last resort.
     *
     * The result has its own trailing ";" stripped, UNLESS it's a single
     * PL/SQL block, whose own trailing ";" (right after its final END) is
     * mandatory grammar rather than a separator — see
     * SqlStatementSplitter's class doc for why blocks need this distinct
     * handling in the first place. This has to be resolved here, before
     * execute()'s own re-split/strip runs, because the single most common
     * way to run a block is simply placing the caret inside it with
     * nothing selected — that always comes through this method first, and
     * if this method already corrupted the block's closing ";", execute()
     * re-splitting the (already-broken) result afterward can't recover it.
     */
    private String selectedOrEditorText() {
        String selected = editor.getSelectedText();
        String raw = (selected != null && !selected.isBlank()) ? selected : null;
        if (raw == null) {
            SqlStatementSplitter.Statement stmt = currentStatement();
            raw = stmt != null ? stmt.text() : editor.getText();
        }
        return smartStripTrailingSemicolon(raw);
    }

    private static String smartStripTrailingSemicolon(String sql) {
        List<SqlStatementSplitter.Statement> parts = SqlStatementSplitter.split(sql);
        if (parts.size() == 1 && parts.get(0).plsqlBlock()) return sql.strip();
        return stripTrailingSemicolon(sql);
    }

    private static String stripTrailingSemicolon(String sql) {
        String trimmed = sql.stripTrailing();
        return trimmed.endsWith(";") ? trimmed.substring(0, trimmed.length() - 1) : sql;
    }

    private SqlStatementSplitter.Statement currentStatement() {
        List<SqlStatementSplitter.Statement> statements = SqlStatementSplitter.split(editor.getText());
        return SqlStatementSplitter.statementAt(statements, editor.getCaretPosition());
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
        resolveParametersThenRun(sql, this::runExecuteToFile);
    }

    private void runExecuteToFile(String sql) {
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
        resolveParametersThenRun(sql, resolved ->
                executeSql("EXPLAIN " + resolved.replaceAll(";\\s*$", ""), false));
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

    public void showHistory() {
        List<QueryHistoryStore.Entry> entries = QueryHistoryStore.forConnection(profile.getId());
        if (entries.isEmpty()) {
            statusLabel.setText("No query history yet for this connection");
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
        setSql(selected.sql());
        historyPopup.hide();
        editor.requestFocus();
    }

    // ------------------------------------------------------- statements popup

    /**
     * Matches the reference IDE's "Statements" quick-pick: lists every
     * statement currently in the console (not run history — this console's
     * own text, split the same way Ctrl+Enter's caret-detection does) so an
     * ambiguous or hard-to-click statement can still be chosen and run
     * directly. Auto-hides on any outside click (default ContextMenu/Popup
     * behavior — see also the editor context menu's own auto-hide).
     */
    /** Reused across invocations, and reachable by the app-wide dismiss-on-outside-click filter. */
    private final Popup statementsPopup = new Popup();

    private void showStatementsPopup() {
        statementsPopup.hide();
        statementsPopup.getContent().clear();

        List<SqlStatementSplitter.Statement> statements = SqlStatementSplitter.split(editor.getText());
        if (statements.isEmpty()) {
            statusLabel.setText("No statements to choose from");
            return;
        }

        ListView<SqlStatementSplitter.Statement> list = new ListView<>();
        list.getStyleClass().add("completion-list");
        list.setPrefWidth(420);
        list.setPrefHeight(Math.min(220, 36.0 * statements.size() + 40));
        list.getItems().setAll(statements);
        list.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(SqlStatementSplitter.Statement stmt, boolean empty) {
                super.updateItem(stmt, empty);
                setText(empty || stmt == null ? null : previewOf(stmt.text()));
            }
        });
        SqlStatementSplitter.Statement current =
                SqlStatementSplitter.statementAt(statements, editor.getCaretPosition());
        list.getSelectionModel().select(current != null ? current : statements.get(0));

        Label title = new Label("Statements");
        title.getStyleClass().add("panel-header");
        Hyperlink customize = new Hyperlink("Customize");
        customize.setOnAction(e -> statusLabel.setText(
                "Customizing which statements are listed isn't supported yet"));

        VBox box = new VBox(6, title, list, customize);
        box.setPadding(new Insets(10));
        box.getStyleClass().add("statements-popup");

        Popup popup = statementsPopup;
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        popup.getContent().add(box);

        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) runChosenStatement(list, popup);
        });
        list.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) runChosenStatement(list, popup);
            else if (e.getCode() == KeyCode.ESCAPE) popup.hide();
        });

        var bounds = statementsButton.localToScreen(statementsButton.getBoundsInLocal());
        popup.show(statementsButton, bounds.getMinX(), bounds.getMaxY() + 2);
        list.requestFocus();
    }

    private void runChosenStatement(ListView<SqlStatementSplitter.Statement> list, Popup popup) {
        SqlStatementSplitter.Statement chosen = list.getSelectionModel().getSelectedItem();
        popup.hide();
        if (chosen == null) return;
        editor.moveTo(Math.min(chosen.start(), editor.getLength()));
        editor.requestFollowCaret();
        String text = chosen.plsqlBlock() ? chosen.text() : stripTrailingSemicolon(chosen.text());
        resolveParametersThenRun(text, this::executeSql);
    }

    private static String previewOf(String sql) {
        String flat = sql.replaceAll("\\s+", " ").strip();
        return flat.length() > 70 ? flat.substring(0, 70) + "\u2026" : flat;
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

    public void execute() {
        executeAction(0);
    }

    public void executeAction(int actionIndex) {
        completionPopup.hide();
        dismissTransientPopups();

        AppSettingsStore.Settings settings = AppSettingsStore.load();
        List<AppSettingsStore.ExecuteActionConfig> actions = settings.getExecuteActions();
        AppSettingsStore.ExecuteActionConfig actionConfig;
        if (actions != null && actionIndex >= 0 && actionIndex < actions.size()) {
            actionConfig = actions.get(actionIndex);
        } else {
            actionConfig = new AppSettingsStore.ExecuteActionConfig(
                    "Execute", "Ctrl+Enter",
                    "Ask what to execute", "Nothing", "Exactly as separate statements", false);
        }

        String fullText = editor.getText();
        if (fullText == null || fullText.isBlank()) {
            return;
        }

        IndexRange selection = editor.getSelection();
        int caretPosition = editor.getCaretPosition();
        String scriptSplittingMode = settings.getScriptSplitting();

        QueryExecutionResolver.ResolvedExecution resolved = QueryExecutionResolver.resolve(
                fullText, selection, caretPosition, actionConfig, scriptSplittingMode);

        switch (resolved.getActionType()) {
            case NO_OP -> statusLabel.setText(resolved.getSummary());
            case ASK_CHOOSER -> showChooserPopup(resolved.getCandidates(), actionConfig);
            case EXECUTE -> proceedWithExecution(resolved.getStatements(), actionConfig);
        }
    }

    private void showChooserPopup(List<QueryExecutionResolver.CandidateOption> candidates,
                                  AppSettingsStore.ExecuteActionConfig actionConfig) {
        if (candidates == null || candidates.isEmpty()) return;
        dismissTransientPopups();

        activeChooserPopup = new QueryExecutionChooserPopup(candidates, chosen -> {
            activeChooserPopup = null;
            proceedWithExecution(chosen.statements(), actionConfig);
        });

        Optional<Bounds> caretBounds = editor.getCaretBounds();
        if (caretBounds.isPresent()) {
            Bounds b = caretBounds.get();
            activeChooserPopup.showAtNode(editor, b.getMinX(), b.getMaxY() + 4);
        } else {
            Point2D p = editor.localToScreen(20, 40);
            if (p != null) {
                activeChooserPopup.showAtNode(editor, p.getX(), p.getY());
            } else {
                activeChooserPopup.showAtNode(editor, 100, 100);
            }
        }
    }

    private void proceedWithExecution(List<String> rawStatements, AppSettingsStore.ExecuteActionConfig actionConfig) {
        if (rawStatements == null || rawStatements.isEmpty()) return;

        List<String> cleaned = rawStatements.stream()
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .toList();
        if (cleaned.isEmpty()) return;

        AppSettingsStore.Settings settings = AppSettingsStore.load();

        // 1. Warn before running potentially unsafe queries if enabled
        if (settings.isWarnUnsafeQueries()) {
            List<String> unsafe = UnsafeQueryDetector.detectUnsafeQueries(cleaned);
            if (!unsafe.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.initOwner(mainWindow.getOwnerWindow());
                alert.setTitle("Potentially Unsafe Query");
                alert.setHeaderText("The statement(s) you are about to execute may modify or delete data without a WHERE clause or drop schema objects:");
                StringBuilder sb = new StringBuilder();
                for (String s : unsafe) {
                    sb.append("• ").append(compactSql(s)).append("\n");
                }
                sb.append("\nDo you want to proceed with execution?");
                alert.setContentText(sb.toString());
                ButtonType executeAnywayBtn = new ButtonType("Execute Anyway", ButtonBar.ButtonData.OK_DONE);
                alert.getButtonTypes().setAll(executeAnywayBtn, ButtonType.CANCEL);
                Optional<ButtonType> resp = alert.showAndWait();
                if (resp.isEmpty() || resp.get() != executeAnywayBtn) {
                    statusLabel.setText("Execution cancelled by user");
                    return;
                }
            }
        }

        // 2. Review parameters before execution if enabled
        if (settings.isReviewParametersBeforeExecution()) {
            resolveBatchParametersThenRun(cleaned, resolved -> executeResolvedStatements(resolved, actionConfig));
        } else {
            executeResolvedStatements(cleaned, actionConfig);
        }
    }

    private void executeResolvedStatements(List<String> statements, AppSettingsStore.ExecuteActionConfig actionConfig) {
        if (statements == null || statements.isEmpty()) return;

        if (statements.size() > 1) {
            executeStatementsSequentially(statements);
        } else {
            String singleSql = statements.get(0);
            AppSettingsStore.Settings settings = AppSettingsStore.load();
            String commentTitle = null;
            if (settings.isCreateTitleFromComment()) {
                commentTitle = SqlParameters.extractPrecedingCommentTitle(singleSql, settings.getTitleAfterCommentText());
            }

            if (actionConfig.isOpenResultsInNewTab()) {
                prepareNewResultTab(singleSql, commentTitle);
            } else if (commentTitle != null) {
                Tab selected = resultsTabPane.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    selected.setText(commentTitle);
                }
            }
            executeSql(singleSql);
        }
    }

    private void prepareNewResultTab(String sql) {
        prepareNewResultTab(sql, null);
    }

    private void prepareNewResultTab(String sql, String customTitle) {
        resultTabCounter++;
        String title = (customTitle != null && !customTitle.isBlank())
                ? customTitle : ("Result " + resultTabCounter);
        ResultGrid newGrid = new ResultGrid();
        newGrid.setSortRequestListener(this::sortByColumn);
        Pane content = wrapResultGridWithToolbar(newGrid);
        Tab tab = new Tab(title, content);
        tab.setClosable(true);
        resultsTabPane.getTabs().add(tab);
        resultsTabPane.getSelectionModel().select(tab);
        editManager = new GridEditManager(profile, catalog, newGrid,
                submitButton, revertButton, this::rerunLastSql, statusLabel::setText);
        tab.setUserData(editManager);
    }

    private ResultGrid activeResultGrid() {
        Tab selected = resultsTabPane.getSelectionModel().getSelectedItem();
        ResultGrid grid = extractResultGrid(selected);
        return grid != null ? grid : resultGrid;
    }

    private ResultGrid extractResultGrid(Tab tab) {
        if (tab == null) return null;
        if (tab.getContent() instanceof ResultGrid grid) return grid;
        if (tab.getContent() instanceof Pane pane) {
            for (javafx.scene.Node node : pane.getChildren()) {
                if (node instanceof ResultGrid grid) return grid;
            }
        }
        return null;
    }

    private Pane wrapResultGridWithToolbar(ResultGrid grid) {
        grid.setReloadAction(this::handleReloadQuery);

        Button addRowBtn = new Button();
        addRowBtn.setGraphic(Icons.of(FontAwesomeSolid.PLUS, "#57965c", 11));
        addRowBtn.setTooltip(new Tooltip("Add Row"));
        addRowBtn.disableProperty().bind(grid.editableProperty().not());
        addRowBtn.setOnAction(e -> {
            if (grid.getAddRowAction() != null) {
                grid.getAddRowAction().run();
            }
        });

        Button deleteRowsBtn = new Button();
        deleteRowsBtn.setGraphic(Icons.of(FontAwesomeSolid.MINUS, "#e05555", 11));
        deleteRowsBtn.setTooltip(new Tooltip("Delete Selected Row(s)"));
        deleteRowsBtn.disableProperty().bind(grid.editableProperty().not()
                .or(javafx.beans.binding.Bindings.isEmpty(grid.getSelectionModel().getSelectedItems())));
        deleteRowsBtn.setOnAction(e -> {
            if (grid.getDeleteRowsAction() != null) {
                grid.getDeleteRowsAction().run();
            }
        });

        Button tabSubmitBtn = new Button("Submit");
        tabSubmitBtn.setGraphic(Icons.of(FontAwesomeSolid.CHECK, "#57965c", 11));
        tabSubmitBtn.setTooltip(new Tooltip("Commit pending result edits/deletes"));
        tabSubmitBtn.disableProperty().bind(submitButton.disableProperty());
        tabSubmitBtn.textProperty().bind(submitButton.textProperty());
        tabSubmitBtn.setOnAction(e -> submitButton.fire());

        Button tabRevertBtn = new Button("Revert");
        tabRevertBtn.setGraphic(Icons.of(FontAwesomeSolid.UNDO, "#e05555", 11));
        tabRevertBtn.setTooltip(new Tooltip("Discard pending edits and re-run the query"));
        tabRevertBtn.disableProperty().bind(revertButton.disableProperty());
        tabRevertBtn.setOnAction(e -> revertButton.fire());

        Button tabReloadBtn = new Button();
        tabReloadBtn.setGraphic(Icons.of(FontAwesomeSolid.SYNC_ALT, "#6897bb", 11));
        tabReloadBtn.setTooltip(new Tooltip("Reload Page  Ctrl+F5"));
        tabReloadBtn.setOnAction(e -> handleReloadQuery());

        DataExtractorsToolbar extractorsToolbar = new DataExtractorsToolbar(
                profile != null ? profile.getType() : ConnectionProfile.DatabaseType.POSTGRESQL,
                grid::toQueryResult,
                () -> {
                    Tab selected = resultsTabPane.getSelectionModel().getSelectedItem();
                    if (selected != null && selected.getText() != null && !selected.getText().isBlank()) {
                        return selected.getText();
                    }
                    return "query_result";
                },
                () -> null,
                () -> profile,
                () -> mainWindow != null ? mainWindow.getOwnerWindow() : null
        );
        extractorsToolbar.addFormatListener(grid::setActiveExtractorFormat);
        grid.setActiveExtractorFormat(extractorsToolbar.getActiveFormat());
        grid.setExportDataAction(extractorsToolbar::openExportDialog);

        Region tbSpacer = new Region();
        HBox.setHgrow(tbSpacer, Priority.ALWAYS);
        List<Node> tbNodes = new ArrayList<>();
        tbNodes.add(addRowBtn);
        tbNodes.add(deleteRowsBtn);
        tbNodes.add(tabSubmitBtn);
        tbNodes.add(tabRevertBtn);
        tbNodes.add(tabReloadBtn);

        if (profile != null && profile.getType() != DatabaseType.MONGODB) {
            Button ddlBtn = new Button("DDL");
            ddlBtn.getStyleClass().add("ddl-button");
            ddlBtn.setTooltip(new Tooltip("Go to DDL"));
            ddlBtn.setOnAction(e -> {
                DbObject tableObj = resolveCurrentTableObject();
                if (tableObj != null && mainWindow != null) {
                    mainWindow.openDdlTab(profile, tableObj);
                } else if (tableObj != null && MainWindow.getInstance() != null) {
                    MainWindow.getInstance().openDdlTab(profile, tableObj);
                } else {
                    statusLabel.setText("No table detected for DDL");
                }
            });
            tbNodes.add(ddlBtn);
        }

        tbNodes.add(tbSpacer);
        tbNodes.add(extractorsToolbar);

        HBox dataToolbar = new HBox(6);
        dataToolbar.getChildren().addAll(tbNodes);
        dataToolbar.setAlignment(Pos.CENTER_LEFT);
        dataToolbar.setPadding(new Insets(4, 8, 4, 8));
        dataToolbar.getStyleClass().add("result-data-toolbar");

        VBox box = new VBox(dataToolbar, grid);
        VBox.setVgrow(grid, Priority.ALWAYS);
        return box;
    }

    private void executeStatementsSequentially(List<String> statements) {
        setRunningState(true);
        showDataPanel(false);
        RunPanel.RunHandle output = mainWindow.getRunPanel().openConsoleOutput(fileId, getText());
        AppSettingsStore.Settings settings = AppSettingsStore.load();
        String showServices = settings.getShowServicesOutput();
        if (!"Never".equals(showServices)) {
            mainWindow.showRunPanel();
        }
        statusLabel.setText("Executing " + statements.size() + " statements\u2026");

        AppExecutor.run(() -> {
            int completed = 0;
            boolean schemaChanged = false;
            try {
                for (String stmt : statements) {
                    output.appendLine(connectionLabel() + "> " + compactSql(stmt));
                    QueryHistoryStore.record(profile.getId(), stmt);
                    QueryResult result = ClientRegistry.jdbc(profile, catalog).execute(stmt, 0);
                    if (!result.getDbmsOutput().isEmpty()) {
                        for (String dbms : result.getDbmsOutput()) {
                            output.appendLine("[DBMS_OUTPUT] " + dbms);
                        }
                    }
                    completed++;
                    String line = result.isResultSet()
                            ? "Completed successfully: " + result.getRows().size()
                                    + " row(s) returned in " + result.getExecutionMillis() + " ms."
                            : "Completed successfully: " + result.getMessage()
                                    + " in " + result.getExecutionMillis() + " ms.";
                    output.appendLine(line);
                    if (isSchemaChangingStatement(stmt)) schemaChanged = true;
                }
                int finalCompleted = completed;
                boolean finalSchemaChanged = schemaChanged;
                Platform.runLater(() -> {
                    statusLabel.setText(finalCompleted + " of " + statements.size() + " statement(s) completed");
                    output.markFinished(0);
                    setRunningState(false);
                    if (finalSchemaChanged) mainWindow.refreshSchemaExplorer(profile);
                });
            } catch (Exception ex) {
                String failedStatement = statements.get(Math.min(completed, statements.size() - 1));
                String msg = executionErrorMessage(ex, failedStatement);
                int finalCompleted = completed;
                boolean finalSchemaChanged = schemaChanged;
                output.appendLine("ERROR: " + msg);
                Platform.runLater(() -> {
                    statusLabel.setText("Error after " + finalCompleted + " of " + statements.size()
                            + " statement(s): " + msg);
                    output.markFinished(-1);
                    setRunningState(false);
                    if (finalSchemaChanged) mainWindow.refreshSchemaExplorer(profile);
                });
            }
        });
    }

    private void resolveBatchParametersThenRun(List<String> statements, java.util.function.Consumer<List<String>> onReady) {
        List<com.roze.dbnavigator.util.SqlParameters.Parameter> allParams = new ArrayList<>();
        for (String stmt : statements) {
            for (com.roze.dbnavigator.util.SqlParameters.Parameter p : com.roze.dbnavigator.util.SqlParameters.detect(stmt)) {
                if (allParams.stream().noneMatch(existing -> existing.name().equalsIgnoreCase(p.name()))) {
                    allParams.add(p);
                }
            }
        }
        if (allParams.isEmpty()) {
            onReady.accept(statements);
            return;
        }
        ParametersDialog.show(mainWindow.getOwnerWindow(), allParams).ifPresent(values -> {
            List<String> resolved = new ArrayList<>();
            for (String stmt : statements) {
                resolved.add(com.roze.dbnavigator.util.SqlParameters.substitute(stmt, values));
            }
            onReady.accept(resolved);
        });
    }

    private void resolveParametersThenRun(String sql, java.util.function.Consumer<String> onReady) {
        resolveBatchParametersThenRun(List.of(sql), list -> {
            if (!list.isEmpty()) onReady.accept(list.get(0));
        });
    }

    public void handleReloadQuery() {
        if (editManager != null && !editManager.confirmAndClearPendingIfDiscarded(getOwnerWindow())) {
            return;
        }
        if (lastExecutedSql != null && !lastExecutedSql.isBlank()) {
            rerunLastSql();
        } else {
            executeAction(0);
        }
    }

    private Window getOwnerWindow() {
        if (mainWindow != null && mainWindow.getOwnerWindow() != null) {
            return mainWindow.getOwnerWindow();
        }
        if (getTabPane() != null && getTabPane().getScene() != null) {
            return getTabPane().getScene().getWindow();
        }
        return null;
    }

    private void rerunLastSql() {
        if (lastExecutedSql != null) executeSql(lastExecutedSql);
    }

    private void executeSql(String sql) {
        executeSql(sql, true);
    }

    /**
     * @param isNewBaseQuery false for internal re-executions (sort-by-column,
     *                        EXPLAIN) that shouldn't become the "base" the
     *                        next sort click wraps — only a genuinely new
     *                        statement the user ran should reset that.
     */
    private void executeSql(String sql, boolean isNewBaseQuery) {
        if (editManager != null && !editManager.confirmAndClearPendingIfDiscarded(getOwnerWindow())) {
            return;
        }
        lastExecutedSql = sql;
        if (isNewBaseQuery) {
            baseSqlForSort = sql;
            currentSortColumn = null;
            currentSortDirection = null;
        }
        setRunningState(true);
        statusLabel.setText("Executing…");
        int pageSize = limitSpinner.getValue();
        QueryHistoryStore.record(profile.getId(), sql);
        boolean likelyDataQuery = isLikelyDataQuery(sql);
        RunPanel.RunHandle output = likelyDataQuery ? null : openRunOutput(sql);
        if (likelyDataQuery) mainWindow.hideRunPanel();

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
                    if (!pkColumns.isEmpty()) {
                        sqlToRun = addMissingPrimaryKeyColumns(sqlToRun, pkColumns);
                        try {
                            columnTypes = MetadataService.loadColumnTypes(profile, ref);
                        } catch (Exception ignored) {
                            columnTypes = Map.of();
                        }
                    }
                    // PostgreSQL table without a PK (e.g. a partition) + plain
                    // single-table SELECT: silently select its physical row
                    // identity so edits target the correct child partition.
                    // These columns stay hidden in the grid.
                    else if (profile.getType() == DatabaseType.POSTGRESQL
                            && isSimpleSingleTableSelect(sql)) {
                        sqlToRun = addRowIdentityToSelect(sqlToRun);
                        pkColumns = List.of("tableoid", "ctid");
                        viaCtid = true;
                        try {
                            // The hidden physical identity has no JDBC metadata,
                            // but edited table columns still need their actual
                            // types (e.g. bigint rather than varchar) on commit.
                            columnTypes = MetadataService.loadColumnTypes(profile, ref);
                        } catch (Exception ignored) {
                            columnTypes = Map.of();
                        }
                    }
                }

                com.roze.dbnavigator.db.PagedResultCursor cursor =
                        new com.roze.dbnavigator.db.PagedResultCursor(pageSize);
                try {
                    cursor.open(profile, catalog, sqlToRun, runningStatement);
                } catch (Exception rewriteFailure) {
                    if (!viaCtid) throw rewriteFailure;
                    // views have no ctid — run the original query, read-only
                    cursor = new com.roze.dbnavigator.db.PagedResultCursor(pageSize);
                    cursor.open(profile, catalog, sql, runningStatement);
                    pkColumns = List.of();
                }

                final com.roze.dbnavigator.db.PagedResultCursor finalCursor = cursor;
                final String targetTable = editableTable;
                final List<String> pk = pkColumns;
                final Map<String, Integer> types = columnTypes;
                final boolean wasViaCtid = viaCtid;

                Platform.runLater(() -> {
                    try {
                        int[] range = currentStatementCharRange(editor.getText());
                        int targetPara = (range != null && range[0] < editor.getText().length())
                                ? editor.offsetToPosition(range[0], org.fxmisc.richtext.model.TwoDimensional.Bias.Forward).getMajor()
                                : editor.getCurrentParagraph();
                        executedLines.add(targetPara);
                        editor.recreateParagraphGraphic(targetPara);
                    } catch (Exception ignored) {}

                    closeActiveCursor();   // now that the new one opened successfully
                    activeCursor = finalCursor;
                    currentPageStart = 0;
                    currentEditableTable = targetTable;
                    currentPkColumns = pk;
                    currentColumnTypes = types;
                    currentViaCtid = wasViaCtid;

                    if (finalCursor.isQueryResult()) {
                        showDataPanel(true);
                        mainWindow.hideRunPanel();
                        displayCurrentPage();
                    } else {
                        showDataPanel(false);
                        editManager.configureReadOnly(null);
                        activeResultGrid().showResult(null);
                        String completion = "Completed successfully: " + finalCursor.getMessage()
                                + " in " + finalCursor.getExecutionMillis() + " ms.";
                        statusLabel.setText(completion);
                        pagingWidget.update(0, 0, -1, true);
                        output.appendLine(completion);
                        // A CREATE/ALTER/DROP/TRUNCATE just succeeded — without
                        // this, the explorer keeps showing whatever it last
                        // loaded (e.g. "Tables (empty)") until the user
                        // remembers to hit Refresh by hand, even though the
                        // table now genuinely exists on disk.
                        if (isSchemaChangingStatement(sql)) {
                            mainWindow.refreshSchemaExplorer(profile);
                        }
                    }
                    if (output != null) output.markFinished(0);
                    setRunningState(false);
                });
            } catch (Exception ex) {
                boolean cancelled = isCancellation(ex);
                boolean timedOut = isTimeout(ex);
                String msg = executionErrorMessage(ex, sql);
                Platform.runLater(() -> {
                    showDataPanel(false);
                    editManager.configureReadOnly(null);
                    activeResultGrid().showResult(null);
                    if (cancelled) {
                        statusLabel.setText("Query cancelled by user");
                    } else if (timedOut) {
                        statusLabel.setText("Query timed out after 120 seconds");
                    } else {
                        statusLabel.setText("Error: " + msg);
                    }
                    RunPanel.RunHandle errorOutput = output == null ? openRunOutput(sql) : output;
                    errorOutput.appendLine(cancelled ? "Query cancelled by user." : timedOut ? "Query timed out after 120 seconds." : "ERROR: " + msg);
                    errorOutput.markFinished(-1);
                    setRunningState(false);
                });
            }
        });
    }

    /** Row-producing statements use the result grid; commands use the Run console. */
    private static boolean isLikelyDataQuery(String sql) {
        return stripComments(sql).matches("(?is)^(select|with|values|show|describe|explain)\\b.*");
    }

    private RunPanel.RunHandle openRunOutput(String sql) {
        RunPanel.RunHandle output = mainWindow.getRunPanel().openConsoleOutput(fileId, getText());
        AppSettingsStore.Settings settings = AppSettingsStore.load();
        String showServices = settings.getShowServicesOutput();
        if (!"Never".equals(showServices)) {
            mainWindow.showRunPanel();
        }
        output.appendLine(connectionLabel() + "> " + compactSql(sql));
        return output;
    }

    /** Adds/removes the data panel from the split so commands never leave a blank data area. */
    public void showDataPanel(boolean show) {
        boolean isShown = editorResultSplit.getItems().contains(dataPanelContainer);
        if (show && !isShown) {
            editorResultSplit.getItems().add(dataPanelContainer);
            editorResultSplit.setDividerPositions(lastDataPanelDivider);
        } else if (!show && isShown) {
            if (editorResultSplit.getDividerPositions().length > 0) {
                lastDataPanelDivider = editorResultSplit.getDividerPositions()[0];
            }
            editorResultSplit.getItems().remove(dataPanelContainer);
        }
    }

    public void hideDataPanel() {
        showDataPanel(false);
        editor.requestFocus();
    }

    public void showDataPanel() {
        showDataPanel(true);
    }

    public void toggleDataPanel() {
        if (isDataPanelVisible()) {
            hideDataPanel();
        } else {
            showDataPanel();
        }
    }

    public boolean isDataPanelVisible() {
        return editorResultSplit.getItems().contains(dataPanelContainer);
    }

    public Button getHideDataPanelButton() {
        return hideDataPanelButton;
    }

    public Button getHistoryButton() {
        return historyButton;
    }

    public VBox getDataPanelContainer() {
        return dataPanelContainer;
    }

    private String connectionLabel() {
        String database = catalog == null || catalog.isBlank() ? profile.getDatabase() : catalog;
        String target = database == null || database.isBlank() ? profile.getName() : database;
        return profile.getType() == DatabaseType.POSTGRESQL ? target + ".public" : target;
    }

    private static String compactSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private static final Pattern SCHEMA_CHANGING_STATEMENT = Pattern.compile(
            "^\\s*(CREATE|ALTER|DROP|TRUNCATE)\\b", Pattern.CASE_INSENSITIVE);

    /**
     * True for statements that add, remove, or restructure a schema object —
     * the kind of change the explorer's cached folders (Tables, Views, ...)
     * need to actually re-fetch to reflect. Deliberately excludes plain
     * INSERT/UPDATE/DELETE: those change row data, not what shows up in the
     * tree, so refreshing for them would just be tree flicker with nothing
     * new to show.
     */
    private static boolean isSchemaChangingStatement(String sql) {
        return SCHEMA_CHANGING_STATEMENT.matcher(stripComments(sql)).find();
    }

    /** Formats database errors compactly while retaining the SQLSTATE when the driver provides it. */
    private static String executionErrorMessage(Exception ex, String sql) {
        if (isTimeout(ex)) {
            return "Query timed out after 120 seconds.";
        }
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof java.sql.SQLException sqlException) {
                String state = sqlException.getSQLState();
                String message = sqlException.getMessage();
                String detail = message == null ? sqlException.toString() : message;
                if ("42P01".equals(state)) {
                    Matcher object = DDL_OBJECT.matcher(sql);
                    if (object.find()) {
                        String kind = object.group(1).equalsIgnoreCase("sequence") ? "Sequence" : "Table";
                        detail = kind + " " + object.group(2) + " does not exist.";
                    }
                } else if ("42601".equals(state)) {
                    Matcher duplicateKeyword = DUPLICATE_DDL_KEYWORD.matcher(sql);
                    if (duplicateKeyword.find()) {
                        String kind = duplicateKeyword.group(1).toUpperCase(Locale.ROOT);
                        String objectName = duplicateKeyword.group(2).trim();
                        detail = "Invalid DROP " + kind + " statement: " + kind
                                + " is repeated. Use: DROP " + kind + " " + objectName + ";";
                    }
                }
                return (state == null || state.isBlank() ? "" : "[" + state + "] ") + detail;
            }
            cause = cause.getCause();
        }
        return ex.getMessage() == null ? ex.toString() : ex.getMessage();
    }

    private static boolean isTimeout(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof java.sql.SQLTimeoutException) return true;
            if (cause instanceof java.sql.SQLException sqlEx) {
                String state = sqlEx.getSQLState();
                if ("57014".equals(state) || "40001".equals(state)) return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static boolean isCancellation(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof java.sql.SQLTimeoutException) return false;
            if (cause instanceof java.sql.SQLException sqlEx) {
                String message = sqlEx.getMessage();
                if (message != null && message.toLowerCase(Locale.ROOT).contains("cancel")) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }

    // ---------------------------------------------------------------- paging

    private int currentPageSize() {
        return activeCursor != null ? activeCursor.getPageSize() : limitSpinner.getValue();
    }

    /** Slices the cursor's in-memory cache to the current page and renders it — no fetch needed. */
    /**
     * Column-header sort: wraps the *original* base query
     * (not whatever's currently displayed, which might already be a
     * previous sort's wrapped version) in a subquery with an ORDER BY, and
     * re-executes it through the normal paged-cursor path — this sorts the
     * complete result set on the server, not just whatever rows happen to
     * be loaded in memory already. Works regardless of how complex the
     * original query is (joins, subqueries, CTEs) since it never has to
     * parse or modify the original SQL, only wrap it.
     */
    private void sortByColumn(String columnName, String direction) {
        if (baseSqlForSort == null || baseSqlForSort.isBlank()) return;
        if (editManager != null && !editManager.confirmAndClearPendingIfDiscarded(getOwnerWindow())) {
            return;
        }
        currentSortColumn = columnName;
        currentSortDirection = direction;

        if (direction == null) {
            executeSql(baseSqlForSort, false);
            return;
        }
        String trimmedBase = baseSqlForSort.strip().replaceAll(";\\s*$", "");
        String wrapped = "SELECT * FROM (" + trimmedBase + ") AS sort_wrapper ORDER BY "
                + DbObject.quote(columnName) + " " + direction;
        executeSql(wrapped, false);
    }

    private void displayCurrentPage() {
        if (activeCursor == null) return;
        List<List<String>> cached = activeCursor.getCachedRows();
        int pageSize = activeCursor.getPageSize();
        int from = Math.min(currentPageStart, cached.size());
        int to = Math.min(from + pageSize, cached.size());
        List<List<String>> pageRows = new ArrayList<>(cached.subList(from, to));

        QueryResult pageResult = new QueryResult();
        pageResult.getColumns().addAll(activeCursor.getColumns());
        pageResult.getColumnTypes().addAll(activeCursor.getColumnTypes());
        pageResult.getRows().addAll(pageRows);
        pageResult.setExecutionMillis(activeCursor.getExecutionMillis());

        if (currentEditableTable != null && !currentPkColumns.isEmpty()) {
            editManager.configure(currentEditableTable, currentPkColumns, currentColumnTypes, pageResult);
        } else {
            editManager.configureReadOnly(pageResult);
        }
        activeResultGrid().setRowNumberOffset(currentPageStart);
        activeResultGrid().setCurrentSort(currentSortColumn, currentSortDirection);
        activeResultGrid().showResult(pageResult);

        Tab activeTab = resultsTabPane.getSelectionModel().getSelectedItem();
        if (activeTab != null && currentEditableTable != null && !currentEditableTable.isBlank()) {
            String tabTitle = currentEditableTable;
            if (catalog != null && !catalog.isBlank() && !tabTitle.startsWith(catalog + ".")) {
                tabTitle = catalog + "." + tabTitle;
            }
            activeTab.setText(tabTitle);
            activeTab.setGraphic(Icons.of(FontAwesomeSolid.TABLE, "#4a88c7", 11));
        }

        long fromDisplay = pageRows.isEmpty() ? 0 : currentPageStart + 1L;
        long toDisplay = currentPageStart + pageRows.size();
        long total = cached.size();
        boolean exact = activeCursor.isExhausted();
        pagingWidget.update(fromDisplay, toDisplay, total, exact);

        statusLabel.setText(pageRows.size() + " row(s) in " + activeCursor.getExecutionMillis() + " ms"
                + (editManager.isEditable()
                    ? (currentViaCtid
                        ? "  ·  editable via row id (no primary key)"
                        : "  ·  editable — double-click cells, Delete removes rows")
                    : ""));
    }

    private void pageForward() {
        if (activeCursor == null) return;
        int newStart = currentPageStart + currentPageSize();
        AppExecutor.run(() -> {
            try {
                activeCursor.ensureFetchedThrough(newStart);
                Platform.runLater(() -> {
                    currentPageStart = newStart;
                    displayCurrentPage();
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> statusLabel.setText("Could not fetch next page: " + msg));
            }
        });
    }

    /** Reads all remaining rows (bounded by the cursor's own safety cap) then jumps to the final page. */
    private void pageToLast() {
        if (activeCursor == null) return;
        statusLabel.setText("Fetching remaining rows\u2026");
        AppExecutor.run(() -> {
            try {
                while (!activeCursor.isExhausted()) {
                    activeCursor.fetchMore(activeCursor.getPageSize());
                }
                Platform.runLater(() -> {
                    int total = activeCursor.getCachedRows().size();
                    int pageSize = activeCursor.getPageSize();
                    currentPageStart = total == 0 ? 0 : ((total - 1) / pageSize) * pageSize;
                    displayCurrentPage();
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> statusLabel.setText("Could not fetch remaining rows: " + msg));
            }
        });
    }

    private void closeActiveCursor() {
        if (activeCursor != null) {
            activeCursor.close();
            activeCursor = null;
        }
    }

    private void countCursorRows() {
        if (activeCursor == null) return;
        statusLabel.setText("Counting rows\u2026");
        AppExecutor.run(() -> {
            try {
                while (!activeCursor.isExhausted()) {
                    activeCursor.fetchMore(1000);
                }
                Platform.runLater(() -> {
                    int total = activeCursor.getCachedRows().size();
                    long fromDisplay = total == 0 ? 0 : currentPageStart + 1L;
                    long toDisplay = Math.min(currentPageStart + currentPageSize(), total);
                    pagingWidget.update(fromDisplay, toDisplay, total, true);
                    statusLabel.setText("Exact count: " + total + " row(s)");
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> statusLabel.setText("Could not count rows: " + msg));
            }
        });
    }

    private Pane activeResultPane() {
        Tab selected = resultsTabPane.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getContent() instanceof Pane p) {
            return p;
        }
        return null;
    }

    private void applyPagingPlacement(TablePagingWidget.Placement placement) {
        if (pagingWidget.getParent() instanceof Pane parent) {
            parent.getChildren().remove(pagingWidget);
        }
        if (statusBar != null) {
            statusBar.setLeft(null);
            statusBar.setCenter(null);
            statusBar.setRight(null);
        }
        pagingWidget.setPlacement(placement);
        if (statusBar == null) return;

        switch (placement) {
            case BOTTOM_CENTER -> {
                statusBar.setLeft(statusLabel);
                BorderPane.setAlignment(statusLabel, Pos.CENTER_LEFT);
                statusBar.setCenter(pagingWidget);
                BorderPane.setAlignment(pagingWidget, Pos.CENTER);
                statusBar.setRight(positionLabel);
                BorderPane.setAlignment(positionLabel, Pos.CENTER_RIGHT);
            }
            case BOTTOM_LEFT -> {
                HBox leftBox = new HBox(10, pagingWidget, statusLabel);
                leftBox.setAlignment(Pos.CENTER_LEFT);
                statusBar.setLeft(leftBox);
                statusBar.setCenter(null);
                statusBar.setRight(positionLabel);
                BorderPane.setAlignment(positionLabel, Pos.CENTER_RIGHT);
            }
            case BOTTOM_RIGHT -> {
                statusBar.setLeft(statusLabel);
                BorderPane.setAlignment(statusLabel, Pos.CENTER_LEFT);
                statusBar.setCenter(null);
                HBox rightBox = new HBox(10, positionLabel, pagingWidget);
                rightBox.setAlignment(Pos.CENTER_RIGHT);
                statusBar.setRight(rightBox);
            }
            case TOOLBAR -> {
                statusBar.setLeft(statusLabel);
                BorderPane.setAlignment(statusLabel, Pos.CENTER_LEFT);
                statusBar.setCenter(null);
                statusBar.setRight(positionLabel);
                BorderPane.setAlignment(positionLabel, Pos.CENTER_RIGHT);
                Pane activeContent = activeResultPane();
                if (activeContent != null) {
                    for (Node n : activeContent.getChildren()) {
                        if (n instanceof HBox tb && tb.getStyleClass().contains("result-data-toolbar")) {
                            if (!tb.getChildren().contains(pagingWidget)) {
                                tb.getChildren().add(pagingWidget);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    public TablePagingWidget getPagingWidget() {
        return pagingWidget;
    }

    // ------------------------------------------- editable-target detection

    private static final Pattern SIMPLE_SELECT = Pattern.compile(
            "^\\s*select\\s+(.+?)\\s+from\\s+([A-Za-z0-9_.\\\"`\\[\\]]+)(.*)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    /**
     * An aggregate function anywhere in the SELECT list means the result no
     * longer has a 1:1 row-to-table-row mapping — e.g. "SELECT COUNT(*)
     * FROM t" returns one summary row, not editable table data. Queries
     * like this still otherwise match SIMPLE_SELECT (no JOIN/GROUP BY/etc),
     * so without this check addMissingPrimaryKeyColumns would insert the
     * table's PK column into the select list to make it "editable" —
     * turning "SELECT COUNT(*) FROM t" into "SELECT pk_col, COUNT(*) FROM
     * t", an aggregate mixed with an ungrouped column, which every engine
     * rejects (Oracle reports it as ORA-00937, "not a single-group group
     * function").
     */
    private static final Pattern AGGREGATE_CALL = Pattern.compile(
            "(?i)\\b(COUNT|SUM|AVG|MIN|MAX|LISTAGG|STRING_AGG|ARRAY_AGG|GROUP_CONCAT|VARIANCE|STDDEV)\\s*\\(");
    /** Identifies the object in a DDL statement for friendly PostgreSQL missing-object errors. */
    private static final Pattern DDL_OBJECT = Pattern.compile(
            "(?is)^\\s*(?:drop|alter|truncate)\\s+(table|sequence)\\s+"
                    + "(?:if\\s+exists\\s+)?((?:\"[^\"]+\"|[A-Za-z_][A-Za-z0-9_$]*)"
                    + "(?:\\s*\\.\\s*(?:\"[^\"]+\"|[A-Za-z_][A-Za-z0-9_$]*))?)");
    /** Catches accidental completion/input such as {@code DROP TABLE table orders}. */
    private static final Pattern DUPLICATE_DDL_KEYWORD = Pattern.compile(
            "(?is)^\\s*drop\\s+(table|sequence)\\s+\\1\\s+(.+?)\\s*;?\\s*$");

    /**
     * Strips single-line (--) and multi-line (/* *\/) comments from SQL,
     * preserving string literals ('...') and quoted identifiers ("...").
     */
    public static String stripComments(String sql) {
        if (sql == null || sql.isBlank()) return "";
        StringBuilder sb = new StringBuilder(sql.length());
        int n = sql.length();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < n; i++) {
            char c = sql.charAt(i);
            char next = (i + 1 < n) ? sql.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                    sb.append('\n');
                }
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }
            if (inSingleQuote) {
                sb.append(c);
                if (c == '\'' && next == '\'') {
                    sb.append(next);
                    i++;
                    continue;
                }
                if (c == '\'') inSingleQuote = false;
                continue;
            }
            if (inDoubleQuote) {
                sb.append(c);
                if (c == '"' && next == '"') {
                    sb.append(next);
                    i++;
                    continue;
                }
                if (c == '"') inDoubleQuote = false;
                continue;
            }

            if (c == '-' && next == '-') {
                inLineComment = true;
                i++;
                continue;
            }
            if (c == '/' && next == '*') {
                inBlockComment = true;
                i++;
                continue;
            }
            if (c == '\'') {
                inSingleQuote = true;
                sb.append(c);
                continue;
            }
            if (c == '"') {
                inDoubleQuote = true;
                sb.append(c);
                continue;
            }

            sb.append(c);
        }
        return sb.toString().trim();
    }

    /**
     * Finds the index immediately following the top-level SELECT keyword
     * in the given SQL, ignoring any comments and string literals that precede it.
     */
    public static int findSelectKeywordEnd(String sql) {
        if (sql == null) return -1;
        int n = sql.length();
        boolean inSingleQuote = false, inDoubleQuote = false, inLineComment = false, inBlockComment = false;

        for (int i = 0; i < n; i++) {
            char c = sql.charAt(i);
            char next = (i + 1 < n) ? sql.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (c == '\n') inLineComment = false;
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }
            if (inSingleQuote) {
                if (c == '\'' && next == '\'') { i++; continue; }
                if (c == '\'') inSingleQuote = false;
                continue;
            }
            if (inDoubleQuote) {
                if (c == '"' && next == '"') { i++; continue; }
                if (c == '"') inDoubleQuote = false;
                continue;
            }

            if (c == '-' && next == '-') {
                inLineComment = true;
                i++;
                continue;
            }
            if (c == '/' && next == '*') {
                inBlockComment = true;
                i++;
                continue;
            }
            if (c == '\'') {
                inSingleQuote = true;
                continue;
            }
            if (c == '"') {
                inDoubleQuote = true;
                continue;
            }

            if (Character.isWhitespace(c)) continue;

            if (i + 6 <= n && sql.regionMatches(true, i, "select", 0, 6)) {
                if (i + 6 == n || Character.isWhitespace(sql.charAt(i + 6)) || sql.charAt(i + 6) == '/' || sql.charAt(i + 6) == '-') {
                    return i + 6;
                }
            }
            return -1;
        }
        return -1;
    }

    /**
     * Returns the target table token when the query is a plain single-table
     * SELECT (no joins/aggregation/unions), otherwise null.
     */
    public static String detectEditableTable(String sql) {
        if (sql == null || sql.isBlank()) return null;
        String cleaned = stripComments(sql);
        while (cleaned.endsWith(";")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).stripTrailing();
        }
        String lower = cleaned.toLowerCase(Locale.ROOT);
        if (lower.contains(" join ") || lower.contains("group by") || lower.contains(" union ")
                || lower.contains("distinct") || lower.contains(" having ")) {
            return null;
        }
        Matcher matcher = SIMPLE_SELECT.matcher(cleaned.trim());
        if (!matcher.matches()) return null;
        String selectList = matcher.group(1);
        if (AGGREGATE_CALL.matcher(selectList).find()) return null;
        String rest = matcher.group(3).stripLeading();
        if (rest.startsWith(",")) return null;   // multi-table FROM a, b
        return matcher.group(2);
    }

    private static final Pattern FROM_TABLE_PATTERN = Pattern.compile(
            "(?i)\\bfrom\\s+([A-Za-z0-9_.\\\"`\\[\\]]+)");

    public DbObject resolveCurrentTableObject() {
        String token = currentEditableTable;
        if (token == null || token.isBlank()) {
            token = detectEditableTable(lastExecutedSql);
        }
        if (token == null || token.isBlank()) {
            if (lastExecutedSql != null) {
                Matcher m = FROM_TABLE_PATTERN.matcher(stripComments(lastExecutedSql));
                if (m.find()) {
                    token = m.group(1);
                }
            }
        }
        if (token == null || token.isBlank()) {
            Tab sel = resultsTabPane != null ? resultsTabPane.getSelectionModel().getSelectedItem() : null;
            if (sel != null && sel.getText() != null && !sel.getText().startsWith("Result ")) {
                token = sel.getText();
            }
        }
        if (token == null || token.isBlank()) {
            token = detectEditableTable(editor != null ? editor.getText() : null);
        }
        if (token != null && !token.isBlank()) {
            return tableRef(token);
        }
        return null;
    }

    /** Builds a DbObject reference from a (possibly qualified) table token. */
    private DbObject tableRef(String token) {
        if (token == null || token.isBlank()) return null;
        boolean quoted = token.contains("\"") || token.contains("[") || token.contains("`");
        String[] parts = token.replace("\"", "").replace("[", "").replace("]", "").replace("`", "").split("\\.");
        String rawTable = parts[parts.length - 1];
        String prefix = parts.length > 1 ? parts[parts.length - 2] : null;

        String tableName = rawTable;
        if (!quoted) {
            if (profile != null && profile.getType() == DatabaseType.POSTGRESQL) {
                tableName = rawTable.toLowerCase(Locale.ROOT);
            } else if (profile != null && profile.getType() == DatabaseType.ORACLE) {
                tableName = rawTable.toUpperCase(Locale.ROOT);
            }
        }

        String cat = catalog;
        String schema = null;
        if (parts.length >= 3) {
            cat = parts[parts.length - 3];
            schema = parts[parts.length - 2];
        } else if (prefix != null) {
            if (profile != null && (profile.getType() == DatabaseType.MYSQL
                    || profile.getType() == DatabaseType.MARIADB)) {
                cat = prefix;      // MySQL qualifies by database
            } else {
                schema = (!quoted && profile != null && profile.getType() == DatabaseType.POSTGRESQL)
                        ? prefix.toLowerCase(Locale.ROOT)
                        : (!quoted && profile != null && profile.getType() == DatabaseType.ORACLE)
                        ? prefix.toUpperCase(Locale.ROOT)
                        : prefix;
            }
        }
        return new DbObject(tableName, DbObject.Kind.TABLE, cat, schema);
    }

    public static String addMissingPrimaryKeyColumns(String sql, List<String> pkColumns) {
        if (sql == null || pkColumns == null || pkColumns.isEmpty()) return sql;
        String cleaned = stripComments(sql);
        if (!cleaned.toLowerCase(Locale.ROOT).matches("(?is)^select\\s+.+?\\s+from\\s+.+")) return sql;

        int fromIndex = -1;
        int n = sql.length();
        boolean inSingleQuote = false, inDoubleQuote = false, inLineComment = false, inBlockComment = false;
        for (int i = 0; i < n; i++) {
            char c = sql.charAt(i);
            char next = (i + 1 < n) ? sql.charAt(i + 1) : '\0';
            if (inLineComment) { if (c == '\n') inLineComment = false; continue; }
            if (inBlockComment) { if (c == '*' && next == '/') { inBlockComment = false; i++; } continue; }
            if (inSingleQuote) { if (c == '\'' && next == '\'') { i++; continue; } if (c == '\'') inSingleQuote = false; continue; }
            if (inDoubleQuote) { if (c == '"' && next == '"') { i++; continue; } if (c == '"') inDoubleQuote = false; continue; }
            if (c == '-' && next == '-') { inLineComment = true; i++; continue; }
            if (c == '/' && next == '*') { inBlockComment = true; i++; continue; }
            if (c == '\'') { inSingleQuote = true; continue; }
            if (c == '"') { inDoubleQuote = true; continue; }

            if (i + 4 <= n && sql.regionMatches(true, i, "from", 0, 4)
                    && (i == 0 || Character.isWhitespace(sql.charAt(i - 1)))
                    && (i + 4 == n || Character.isWhitespace(sql.charAt(i + 4)))) {
                fromIndex = i;
                break;
            }
        }
        if (fromIndex < 0) return sql;

        String selectPart = sql.substring(0, fromIndex);
        String selectPartCleaned = stripComments(selectPart).strip();

        // A bare "*" (optionally table-qualified, e.g. "t.*") already includes every column
        if (selectPartCleaned.matches("(?i)^select\\s+([A-Za-z_][A-Za-z0-9_]*\\.)?\\*\\s*$")) {
            return sql;
        }

        int selectEnd = findSelectKeywordEnd(sql);
        if (selectEnd < 0 || selectEnd >= fromIndex) return sql;

        StringBuilder missingPks = new StringBuilder();
        String selectLower = selectPartCleaned.toLowerCase(Locale.ROOT);
        for (String pk : pkColumns) {
            if (!selectLower.contains(pk.toLowerCase(Locale.ROOT))) {
                missingPks.append(" ").append(pk).append(",");
            }
        }
        if (missingPks.isEmpty()) return sql;

        return sql.substring(0, selectEnd) + missingPks + sql.substring(selectEnd);
    }

    public static boolean isSimpleSingleTableSelect(String sql) {
        if (sql == null || sql.isBlank()) return false;
        String cleaned = stripComments(sql);
        while (cleaned.endsWith(";")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).stripTrailing();
        }
        return cleaned.matches("(?is)^\\s*select\\s+.+?\\s+from\\s+[A-Za-z0-9_.\\\"`\\[\\]]+"
                + "\\s*(where\\b.*)?\\s*$");
    }

    public static String addRowIdentityToSelect(String sql) {
        int idx = findSelectKeywordEnd(sql);
        if (idx < 0) return sql;
        return sql.substring(0, idx) + " tableoid::text AS tableoid, ctid," + sql.substring(idx);
    }

    private void setupIntrospectionBanner() {
        introspectionBanner = new HBox(8);
        introspectionBanner.getStyleClass().add("console-introspection-banner");
        introspectionBanner.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label();
        iconLabel.setGraphic(Icons.of(FontAwesomeSolid.EXCLAMATION_TRIANGLE, "#d9a74a", 12));

        bannerLabel = new Label("Current database is not introspected.");
        Region bannerSpacer = new Region();
        HBox.setHgrow(bannerSpacer, Priority.ALWAYS);

        bannerLink = new Hyperlink("Introspect database");
        bannerLink.setOnAction(e -> {
            if (catalog != null && !catalog.isBlank()) {
                List<String> visible = profile.getVisibleDatabases();
                List<String> updated = visible != null ? new ArrayList<>(visible) : new ArrayList<>();
                if (!updated.contains(catalog)) {
                    updated.add(catalog);
                }
                profile.setVisibleDatabases(updated);
                ConnectionStore.saveOrUpdate(profile);
                if (mainWindow != null && mainWindow.getSchemaPane() != null) {
                    mainWindow.getSchemaPane().refreshConnection(profile);
                }
                updateIntrospectionBanner();
            }
        });

        introspectionBanner.getChildren().addAll(iconLabel, bannerLabel, bannerSpacer, bannerLink);
        updateIntrospectionBanner();
    }

    public void updateIntrospectionBanner() {
        if (introspectionBanner == null) return;
        if (profile.getType() == DatabaseType.SQLITE) {
            introspectionBanner.setVisible(false);
            introspectionBanner.setManaged(false);
            return;
        }
        List<String> visible = profile.getVisibleDatabases();
        boolean hasFilter = visible != null && !visible.isEmpty();
        boolean notIntrospected = hasFilter && catalog != null && !visible.contains(catalog);

        if (notIntrospected) {
            boolean isOracle = profile.getType() == DatabaseType.ORACLE;
            bannerLabel.setText(isOracle ? "Current schema is not introspected." : "Current database is not introspected.");
            bannerLink.setText(isOracle ? "Introspect schema" : "Introspect database");
            introspectionBanner.setVisible(true);
            introspectionBanner.setManaged(true);
        } else {
            introspectionBanner.setVisible(false);
            introspectionBanner.setManaged(false);
        }
    }

    private javafx.scene.Node setupDbSelector() {
        if (profile.getType() == DatabaseType.SQLITE) {
            Label label = new Label("main");
            label.getStyleClass().add("console-connection-label");
            label.setGraphic(Icons.of(FontAwesomeSolid.DATABASE, "#57965c", 10));
            return label;
        }

        this.dbSelectorButton = new MenuButton(formatSelectorText(catalog));
        this.dbSelectorButton.getStyleClass().add("console-db-selector");
        this.dbSelectorButton.setGraphic(Icons.of(FontAwesomeSolid.DATABASE, "#57965c", 10));
        this.dbSelectorButton.setTooltip(new Tooltip("Select Database / Schema"));

        this.dbSelectorButton.setOnShowing(e -> populateDbSelectorMenu());

        AppExecutor.run(() -> {
            try {
                cachedDatabases = MetadataService.listDatabaseNames(profile);
            } catch (Exception ignored) {}
        });

        return this.dbSelectorButton;
    }

    public String formatSelectorText(String cat) {
        if (profile.getType() == DatabaseType.SQLITE) {
            return "main";
        }
        if (cat == null || cat.isBlank()) {
            cat = profile.getDatabase() != null && !profile.getDatabase().isBlank()
                    ? profile.getDatabase() : "default";
        }
        if (profile.getType() == DatabaseType.POSTGRESQL || profile.getType() == DatabaseType.SQLSERVER) {
            return cat + ".<schema>";
        }
        if (profile.getType() == DatabaseType.ORACLE) {
            return cat.toUpperCase();
        }
        return cat;
    }

    private void populateDbSelectorMenu() {
        if (dbSelectorButton == null) return;
        dbSelectorButton.getItems().clear();

        List<String> allDbs = cachedDatabases;
        if (allDbs == null || allDbs.isEmpty()) {
            try {
                allDbs = MetadataService.listDatabaseNames(profile);
                cachedDatabases = allDbs;
            } catch (Exception ignored) {
                allDbs = List.of();
            }
        }

        if (allDbs.isEmpty() && catalog != null) {
            allDbs = List.of(catalog);
        }

        List<String> visible = profile.getVisibleDatabases();
        boolean hasFilter = visible != null && !visible.isEmpty();

        List<String> introspected = hasFilter
                ? allDbs.stream().filter(visible::contains).toList()
                : allDbs;
        List<String> nonIntrospected = hasFilter
                ? allDbs.stream().filter(d -> !visible.contains(d)).toList()
                : List.of();

        // 1. Introspected databases section
        for (String db : introspected) {
            Menu dbMenu = new Menu(db);
            dbMenu.setGraphic(Icons.of(FontAwesomeSolid.DATABASE, "#3592c4", 10));

            MenuItem defaultItem = new MenuItem(db + (profile.getType() == DatabaseType.POSTGRESQL || profile.getType() == DatabaseType.SQLSERVER ? ".<schema>" : ""));
            defaultItem.setOnAction(e -> switchDatabase(db));
            dbMenu.getItems().add(defaultItem);

            if (profile.getType() == DatabaseType.POSTGRESQL || profile.getType() == DatabaseType.SQLSERVER) {
                MenuItem publicItem = new MenuItem("public");
                publicItem.setOnAction(e -> switchDatabase(db));
                dbMenu.getItems().add(publicItem);
            }

            dbSelectorButton.getItems().add(dbMenu);
        }

        // 2. Non-Introspected section
        if (!nonIntrospected.isEmpty()) {
            dbSelectorButton.getItems().add(new SeparatorMenuItem());

            Label headerLabel = new Label("Non-Introspected");
            headerLabel.setStyle("-fx-text-fill: #787878; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8 3 8;");
            CustomMenuItem headerItem = new CustomMenuItem(headerLabel);
            headerItem.setHideOnClick(false);
            dbSelectorButton.getItems().add(headerItem);

            for (String db : nonIntrospected) {
                MenuItem item = new MenuItem(db);
                item.setGraphic(Icons.of(FontAwesomeSolid.DATABASE, "#787878", 10));
                item.setOnAction(e -> switchDatabase(db));
                dbSelectorButton.getItems().add(item);
            }
        }

        // 3. Footer hint
        dbSelectorButton.getItems().add(new SeparatorMenuItem());
        Label footerLabel = new Label("Connect to database (Enter)");
        footerLabel.setStyle("-fx-text-fill: #606060; -fx-font-size: 10px; -fx-padding: 2 8 2 8;");
        CustomMenuItem footerItem = new CustomMenuItem(footerLabel);
        footerItem.setHideOnClick(false);
        dbSelectorButton.getItems().add(footerItem);
    }

    public void switchDatabase(String newCatalog) {
        if (newCatalog == null || newCatalog.equals(this.catalog)) {
            return;
        }
        this.catalog = newCatalog;
        if (dbSelectorButton != null) {
            dbSelectorButton.setText(formatSelectorText(newCatalog));
        }
        setText(fileId + (newCatalog != null ? " [" + newCatalog + "]" : ""));
        if (breadcrumbsFileLabel != null) {
            breadcrumbsFileLabel.setText((newCatalog != null ? newCatalog : "console") + ".sql");
        }
        ResultGrid grid = activeResultGrid();
        if (grid != null) {
            this.editManager = new GridEditManager(profile, newCatalog, grid,
                    submitButton, revertButton, this::rerunLastSql, statusLabel::setText);
        }
        updateIntrospectionBanner();
        statusLabel.setText("Connected to " + newCatalog);

        AppExecutor.run(() -> CompletionService.preload(profile, newCatalog));

        long start = System.currentTimeMillis();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timeStr = LocalDateTime.now().format(formatter);
        if (mainWindow != null && mainWindow.getRunPanel() != null) {
            RunPanel.RunHandle output = mainWindow.getRunPanel().openConsoleOutput(fileId, getText());
            AppSettingsStore.Settings settings = AppSettingsStore.load();
            if (settings != null && !"Never".equals(settings.getShowServicesOutput())) {
                mainWindow.showRunPanel();
            }
            output.appendLine("[" + timeStr + "] Connected to " + newCatalog);

            AppExecutor.run(() -> {
                try (Connection conn = ClientRegistry.jdbc(profile, newCatalog).getConnection()) {
                    long elapsed = System.currentTimeMillis() - start;
                    String doneTime = LocalDateTime.now().format(formatter);
                    Platform.runLater(() -> output.appendLine("[" + doneTime + "] completed in " + elapsed + " ms"));
                } catch (Exception ex) {
                    String errMsg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
                    Platform.runLater(() -> output.appendLine("[Error connecting to " + newCatalog + "] " + errMsg));
                }
            });
        }
    }

    public MenuButton getDbSelectorButton() {
        return dbSelectorButton;
    }

    public HBox getIntrospectionBanner() {
        return introspectionBanner;
    }

}
