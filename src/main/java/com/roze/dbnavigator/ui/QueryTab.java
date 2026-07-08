package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.QueryResult;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/** A DataGrip-style SQL console: editor on top, results below, Ctrl+Enter to run. */
public class QueryTab extends Tab {

    private final ConnectionProfile profile;
    private final CodeArea editor = SqlHighlighter.createEditor();
    private final ResultGrid resultGrid = new ResultGrid();
    private final Label statusLabel = new Label("Ready");
    private final Spinner<Integer> limitSpinner =
            new Spinner<>(10, 100_000, 500, 100);
    private final Button runButton = new Button("Run");

    public QueryTab(ConnectionProfile profile, String title) {
        this.profile = profile;
        setText(title);
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

        Label connLabel = new Label(profile.getName());
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
                + " console. Ctrl+Enter runs the statement.\n");
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

        root.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
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

    private void execute() {
        String sql = selectedOrAllText();
        if (sql.isBlank()) return;

        runButton.setDisable(true);
        statusLabel.setText("Executing…");
        int limit = limitSpinner.getValue();

        AppExecutor.run(() -> {
            try {
                QueryResult result = ClientRegistry.jdbc(profile).execute(sql, limit);
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
