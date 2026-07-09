package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.model.ConnectionProfile;
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
import java.util.Optional;

/**
 * DataGrip-style SQL console: editor on top, results below, Ctrl+Enter to run,
 * autocomplete popup for keywords, table names and "table.column".
 * A console can be bound to a specific database of the connection (PostgreSQL).
 */
public class QueryTab extends Tab {

    private final ConnectionProfile profile;
    private final String catalog;   // nullable: default database of the profile
    private final CodeArea editor = SqlHighlighter.createEditor();
    private final ResultGrid resultGrid = new ResultGrid();
    private final Label statusLabel = new Label("Ready");
    private final Spinner<Integer> limitSpinner = new Spinner<>(10, 100_000, 500, 100);
    private final Button runButton = new Button("Run");

    // ---- autocomplete ----
    private final Popup completionPopup = new Popup();
    private final ListView<String> completionList = new ListView<>();
    private int tokenStart = -1;

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

        Button exportButton = new Button("Export CSV");
        exportButton.setGraphic(Icons.of(FontAwesomeSolid.FILE_CSV, "#e0a44c", 11));
        exportButton.setOnAction(e -> resultGrid.exportCsv());

        limitSpinner.setEditable(true);
        limitSpinner.setPrefWidth(95);

        Label connLabel = new Label(profile.getName()
                + (catalog != null ? " · " + catalog : ""));
        connLabel.getStyleClass().add("console-connection-label");
        connLabel.setGraphic(Icons.of(FontAwesomeSolid.DATABASE, "#57965c", 10));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, runButton, exportButton,
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
        editor.replaceText(sql);
    }

    // ---------------------------------------------------------- completion

    private void setupCompletion() {
        completionList.getStyleClass().add("completion-list");
        completionList.setPrefSize(320, 180);
        completionPopup.getContent().add(completionList);
        completionPopup.setAutoHide(true);

        completionList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) insertSelectedCompletion();
        });

        // Keyboard interaction while the popup is showing
        editor.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (new KeyCodeCombination(KeyCode.SPACE, KeyCombination.CONTROL_DOWN).match(e)) {
                showCompletions();
                e.consume();
                return;
            }
            if (!completionPopup.isShowing()) return;
            switch (e.getCode()) {
                case DOWN -> {
                    completionList.getSelectionModel().selectNext();
                    e.consume();
                }
                case UP -> {
                    completionList.getSelectionModel().selectPrevious();
                    e.consume();
                }
                case ENTER, TAB -> {
                    insertSelectedCompletion();
                    e.consume();
                }
                case ESCAPE -> {
                    completionPopup.hide();
                    e.consume();
                }
                default -> {}
            }
        });

        // Re-suggest as the user types
        editor.plainTextChanges().subscribe(change -> {
            if (change.getInserted().length() == 1
                    && change.getInserted().matches("[A-Za-z0-9_.]")) {
                showCompletions();
            } else if (completionPopup.isShowing()) {
                showCompletions();   // refresh (e.g. after backspace)
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
        if (suggestions.isEmpty()) {
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
        if (selected == null || tokenStart < 0) return;
        editor.replaceText(tokenStart, editor.getCaretPosition(), selected);
        completionPopup.hide();
    }

    // ------------------------------------------------------------- execute

    private void execute() {
        completionPopup.hide();
        String sql = selectedOrAllText();
        if (sql.isBlank()) return;

        runButton.setDisable(true);
        statusLabel.setText("Executing…");
        int limit = limitSpinner.getValue();

        AppExecutor.run(() -> {
            try {
                QueryResult result = ClientRegistry.jdbc(profile, catalog).execute(sql, limit);
                Platform.runLater(() -> {
                    if (result.isResultSet()) {
                        resultGrid.showResult(result);
                        statusLabel.setText(result.getRows().size() + " row(s) in "
                                + result.getExecutionMillis() + " ms"
                                + (result.getRows().size() >= limit ? "  (limited to " + limit + ")" : ""));
                    } else {
                        resultGrid.showResult(null);
                        statusLabel.setText(result.getMessage() + " in " + result.getExecutionMillis() + " ms");
                    }
                    runButton.setDisable(false);
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> {
                    resultGrid.showResult(null);
                    statusLabel.setText("Error: " + msg);
                    runButton.setDisable(false);
                });
            }
        });
    }

    private String selectedOrAllText() {
        String selected = editor.getSelectedText();
        return (selected != null && !selected.isBlank()) ? selected : editor.getText();
    }
}
