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
import org.fxmisc.richtext.LineNumberFactory;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
 * DataGrip-style SQL console. Autocomplete (tables → columns → keywords,
 * Enter/Tab inserts), and simple single-table SELECT results are editable:
 * change cells / delete rows, then Submit commits everything in one transaction.
 */
public class QueryTab extends Tab {

    private final MainWindow mainWindow;
    private final ConnectionProfile profile;
    private final String catalog;   // nullable: default database of the profile
    private final CodeArea editor = SqlHighlighter.createEditor();
    private final VirtualizedScrollPane<CodeArea> editorScroll;
    private final VBox editorContainer = new VBox();
    private final HBox breadcrumbsBar = new HBox(6);
    private final Label breadcrumbsFileLabel = new Label("console.sql");
    private final Label breadcrumbsSepLabel = new Label("›");
    private final Label breadcrumbsStmtLabel = new Label("SELECT");
    private final ResultGrid resultGrid = new ResultGrid();
    private final TabPane resultsTabPane = new TabPane();
    private int resultTabCounter = 1;
    private QueryExecutionChooserPopup activeChooserPopup;
    private final SplitPane editorResultSplit = new SplitPane();
    private final Label statusLabel = new Label("Ready");
    private final Spinner<Integer> limitSpinner = new Spinner<>(10, 100_000, 500, 100);
    private final Button runButton = new Button("Run");
    private final Button cancelButton = new Button("Cancel");
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
    private final ResultPager pager = new ResultPager();
    private com.roze.dbnavigator.db.PagedResultCursor activeCursor;
    private int currentPageStart = 0;
    private String currentEditableTable;
    private List<String> currentPkColumns = List.of();
    private Map<String, Integer> currentColumnTypes = Map.of();

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

        historyButton.setGraphic(Icons.of(FontAwesomeSolid.HISTORY, "#a9b7c6", 11));
        historyButton.setTooltip(new Tooltip("Query History"));
        historyButton.setOnAction(e -> showHistory());

        statementsButton.setGraphic(Icons.of(FontAwesomeSolid.LIST_UL, "#a9b7c6", 11));
        statementsButton.setTooltip(new Tooltip("Choose Statement to Run"));
        statementsButton.setOnAction(e -> showStatementsPopup());

        submitButton.setGraphic(Icons.of(FontAwesomeSolid.CHECK, "#57965c", 11));
        submitButton.setTooltip(new Tooltip("Commit pending result edits/deletes"));
        revertButton.setGraphic(Icons.of(FontAwesomeSolid.UNDO, "#e05555", 11));
        revertButton.setTooltip(new Tooltip("Discard pending edits and re-run the query"));

        editManager = new GridEditManager(profile, catalog, resultGrid,
                submitButton, revertButton, this::rerunLastSql, statusLabel::setText);

        Button exportButton = new Button("Export CSV");
        exportButton.setGraphic(Icons.of(FontAwesomeSolid.FILE_CSV, "#e0a44c", 11));
        exportButton.setOnAction(e -> activeResultGrid().exportCsv());

        limitSpinner.setEditable(true);
        limitSpinner.setPrefWidth(95);

        Label connLabel = new Label(profile.getName()
                + (catalog != null ? " ▸ " + catalog : ""));
        connLabel.getStyleClass().add("console-connection-label");
        connLabel.setGraphic(Icons.of(FontAwesomeSolid.DATABASE, "#57965c", 10));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, runButton, cancelButton, historyButton, statementsButton,
                submitButton, revertButton, exportButton,
                new Label("Limit:"), limitSpinner, spacer, connLabel);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.getStyleClass().add("console-toolbar");

        // ---- Editor ----
        editor.replaceText("-- " + profile.getType().getDisplayName()
                + " console. Ctrl+Enter runs, Ctrl+Space completes.\n");
        editorScroll = new VirtualizedScrollPane<>(editor);
        VBox.setVgrow(editorScroll, Priority.ALWAYS);
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
        setOnClosed(e -> {
            LocalHistoryStore.record(fileId, editor.getText());
            closeActiveCursor();
        });

        pager.setOnFirst(() -> { currentPageStart = 0; displayCurrentPage(); });
        pager.setOnPrev(() -> {
            currentPageStart = Math.max(0, currentPageStart - currentPageSize());
            displayCurrentPage();
        });
        pager.setOnNext(this::pageForward);
        pager.setOnLast(this::pageToLast);
        pager.addOverflowItem("Refresh (re-run query)", this::rerunLastSql);

        // ---- Layout ----
        Tab initialResultTab = new Tab("Result 1", resultGrid);
        initialResultTab.setClosable(false);
        resultsTabPane.getTabs().add(initialResultTab);
        resultsTabPane.setStyle("-fx-tab-min-width: 80px;");
        resultsTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null && newTab.getContent() instanceof ResultGrid grid) {
                editManager = new GridEditManager(profile, catalog, grid,
                        submitButton, revertButton, this::rerunLastSql, statusLabel::setText);
            }
        });
        editorResultSplit.getItems().addAll(editorContainer, resultsTabPane);
        editorResultSplit.setOrientation(Orientation.VERTICAL);
        editorResultSplit.setDividerPositions(0.45);

        statusLabel.getStyleClass().add("console-status");
        Region statusSpacer = new Region();
        HBox.setHgrow(statusSpacer, Priority.ALWAYS);
        HBox statusBar = new HBox(statusLabel, statusSpacer, pager);
        statusBar.setPadding(new Insets(4, 10, 4, 10));
        statusBar.getStyleClass().add("console-status-bar");

        VBox root = new VBox(toolbar, editorResultSplit, statusBar);
        VBox.setVgrow(editorResultSplit, Priority.ALWAYS);
        setContent(root);

        setupCompletion();
        setupHistory();
        setupEditorContextMenu();
        setupStatementHighlighting();
        setupOutsideClickDismiss();
        CompletionService.preload(profile, catalog);

        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN).match(e)) {
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
        suppressCompletion = false;
    }

    public CodeArea getEditor() {
        return editor;
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
    private void setupStatementHighlighting() {
        editor.multiPlainChanges()
                .successionEnds(Duration.ofMillis(120))
                .subscribe(ignore -> {
                    refreshEditorStyling();
                    updateBreadcrumbsText();
                });
        editor.caretPositionProperty().addListener((obs, o, n) -> {
            refreshEditorStyling();
            updateBreadcrumbsText();
        });
        // Recompute after every mouse press (including right-clicks) too — a
        // right-click's ContextMenuEvent can fire before the caret-position
        // property listener has settled, which is why the wrong statement was
        // sometimes still shown highlighted when the menu opened.
        editor.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                e -> Platform.runLater(() -> {
                    refreshEditorStyling();
                    updateBreadcrumbsText();
                }));
        Platform.runLater(() -> {
            refreshEditorStyling();
            updateBreadcrumbsText();
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

    /** File → Settings → Editor → General → Appearance & Breadcrumbs, applied here and re-applied after Apply/OK. */
    public void applyEditorAppearanceFromSettings() {
        var settings = com.roze.dbnavigator.db.AppSettingsStore.load();

        // 1. Line numbers
        if (!settings.isShowLineNumbers()) {
            editor.setParagraphGraphicFactory(null);
        } else {
            String mode = settings.getLineNumbersMode();
            if ("Relative".equalsIgnoreCase(mode)) {
                editor.setParagraphGraphicFactory(LineNumberFactory.get(editor, lineIdx -> {
                    int cur = editor.getCurrentParagraph();
                    return String.valueOf(Math.abs(lineIdx - cur));
                }));
            } else if ("Hybrid".equalsIgnoreCase(mode)) {
                editor.setParagraphGraphicFactory(LineNumberFactory.get(editor, lineIdx -> {
                    int cur = editor.getCurrentParagraph();
                    return (lineIdx == cur) ? String.valueOf(lineIdx + 1) : String.valueOf(Math.abs(lineIdx - cur));
                }));
            } else {
                editor.setParagraphGraphicFactory(LineNumberFactory.get(editor));
            }
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
        if (editorContainer != null && editorScroll != null) {
            boolean show = settings.isShowBreadcrumbs();
            if (!show) {
                editorContainer.getChildren().setAll(editorScroll);
            } else {
                String placement = settings.getBreadcrumbsPlacement();
                if ("Top".equalsIgnoreCase(placement)) {
                    editorContainer.getChildren().setAll(breadcrumbsBar, editorScroll);
                } else {
                    editorContainer.getChildren().setAll(editorScroll, breadcrumbsBar);
                }
                updateBreadcrumbsText();
            }
        }
    }

    /** File → Settings → Editor → Font, applied here and re-applied after Apply/OK. */
    public void applyEditorFontFromSettings() {
        var settings = com.roze.dbnavigator.db.AppSettingsStore.load();
        currentEditorFontFamily = settings.getEditorFontFamily();
        currentEditorFontSize = settings.getEditorFontSize();
        updateEditorFontStyle();
    }

    private void updateEditorFontStyle() {
        editor.setStyle("-fx-font-family: '" + currentEditorFontFamily + "'; -fx-font-size: "
                + currentEditorFontSize + "px;");
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

    /** Recomputes syntax highlighting and overlays the current-statement highlight in one combined pass. */
    private void refreshEditorStyling() {
        try {
            String text = editor.getText();
            org.fxmisc.richtext.model.StyleSpans<java.util.Collection<String>> syntax =
                    plainTextMode ? emptySpans(text.length()) : SqlHighlighter.computeHighlighting(text);
            int[] range = currentStatementCharRange(text);

            if (range == null) {
                editor.setStyleSpans(0, syntax);
            } else {
                var spansBuilder = new org.fxmisc.richtext.model.StyleSpansBuilder<java.util.Collection<String>>();
                if (range[0] > 0) spansBuilder.add(java.util.Collections.emptyList(), range[0]);
                spansBuilder.add(java.util.Collections.singleton("current-statement-line"), range[1] - range[0]);
                if (range[1] < text.length()) {
                    spansBuilder.add(java.util.Collections.emptyList(), text.length() - range[1]);
                }
                var highlight = spansBuilder.create();

                var merged = syntax.overlay(highlight, (base, extra) -> {
                    if (extra.isEmpty()) return base;
                    LinkedHashSet<String> combined = new LinkedHashSet<>(base);
                    combined.addAll(extra);
                    return combined;
                });
                editor.setStyleSpans(0, merged);
            }
            refreshBookmarkParagraphStyles();
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

    /** Whole-line markers (paragraph-level, independent of the character-range statement highlight above). */
    private void refreshBookmarkParagraphStyles() {
        int count = editor.getParagraphs().size();
        for (int i = 0; i < count; i++) {
            boolean bookmarked = bookmarkedLines.contains(i);
            editor.setParagraphStyle(i, bookmarked ? List.of("bookmarked-line") : List.of());
        }
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
        if (statements.size() < 2) return null;

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
        Tab tab = new Tab(title, newGrid);
        tab.setClosable(true);
        resultsTabPane.getTabs().add(tab);
        resultsTabPane.getSelectionModel().select(tab);
        editManager = new GridEditManager(profile, catalog, newGrid,
                submitButton, revertButton, this::rerunLastSql, statusLabel::setText);
    }

    private ResultGrid activeResultGrid() {
        Tab selected = resultsTabPane.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getContent() instanceof ResultGrid grid) {
            return grid;
        }
        return resultGrid;
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

                Platform.runLater(() -> {
                    closeActiveCursor();   // now that the new one opened successfully
                    activeCursor = finalCursor;
                    currentPageStart = 0;
                    currentEditableTable = targetTable;
                    currentPkColumns = pk;
                    currentColumnTypes = types;

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
                        pager.update(0, 0, -1, true);
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
        return sql.stripLeading().matches("(?is)^(select|with|values|show|describe|explain)\\b.*");
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

    /** Adds/removes the actual grid from the split so commands never leave a blank data area. */
    private void showDataPanel(boolean show) {
        boolean isShown = editorResultSplit.getItems().contains(resultsTabPane);
        if (show && !isShown) {
            editorResultSplit.getItems().add(resultsTabPane);
            editorResultSplit.setDividerPositions(0.45);
        } else if (!show && isShown) {
            editorResultSplit.getItems().remove(resultsTabPane);
        }
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
        return SCHEMA_CHANGING_STATEMENT.matcher(sql).find();
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
     * DataGrip-style column-header sort: wraps the *original* base query
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

        long fromDisplay = pageRows.isEmpty() ? 0 : currentPageStart + 1L;
        long toDisplay = currentPageStart + pageRows.size();
        long total = cached.size();
        boolean exact = activeCursor.isExhausted();
        pager.update(fromDisplay, toDisplay, total, exact);

        statusLabel.setText(pageRows.size() + " row(s) in " + activeCursor.getExecutionMillis() + " ms"
                + (editManager.isEditable()
                    ? "  ·  editable — double-click cells, Delete removes rows"
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

    // ------------------------------------------- editable-target detection

    private static final Pattern SIMPLE_SELECT = Pattern.compile(
            "^\\s*select\\s+(.+?)\\s+from\\s+([A-Za-z0-9_.\"]+)(.*)$",
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
        String selectList = matcher.group(1);
        if (AGGREGATE_CALL.matcher(selectList).find()) return null;
        String rest = matcher.group(3).stripLeading();
        if (rest.startsWith(",")) return null;   // multi-table FROM a, b
        return matcher.group(2);
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

    private static String addMissingPrimaryKeyColumns(String sql, List<String> pkColumns) {
        String lower = sql.toLowerCase(Locale.ROOT);
        if (!lower.matches("(?is)^\\s*select\\s+.+?\\s+from\\s+.+")) return sql;
        int fromIndex = lower.indexOf(" from ");
        if (fromIndex < 0) return sql;
        String selectPart = sql.substring(0, fromIndex);
        String rest = sql.substring(fromIndex);

        // A bare "*" (optionally table-qualified, e.g. "t.*") already
        // includes every column, the primary key included — inserting an
        // explicit PK name in front of it produces invalid syntax on
        // MySQL specifically (an unqualified "*" generally can't be mixed
        // with other column names in the same select list there), so a
        // wildcard select is left completely untouched.
        if (selectPart.strip().matches("(?i)select\\s+([A-Za-z_][A-Za-z0-9_]*\\.)?\\*\\s*")) {
            return sql;
        }

        for (String pk : pkColumns) {
            if (!selectPart.toLowerCase(Locale.ROOT).contains(pk.toLowerCase(Locale.ROOT))) {
                selectPart = selectPart.replaceFirst("(?i)select\\s+", "$0" + pk + ", ");
            }
        }
        return selectPart + rest;
    }

    private static boolean isSimpleSingleTableSelect(String sql) {
        // Console queries normally end in a semicolon. Treat it as syntax
        // decoration rather than evidence that this is a complex query, so a
        // plain "SELECT * FROM partition;" remains editable.
        return sql.matches("(?is)^\\s*select\\s+.+?\\s+from\\s+[A-Za-z0-9_.\\\"]+"
                + "\\s*(where\\b.*)?\\s*;?\\s*$");
    }

    private static String addRowIdentityToSelect(String sql) {
        return sql.replaceFirst("(?is)^(\\s*select\\s+)",
                "$1tableoid::text AS tableoid, ctid, ");
    }

}
