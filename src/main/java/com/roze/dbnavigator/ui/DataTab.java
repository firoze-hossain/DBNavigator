package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.QueryResult;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/** DataGrip-style table data view: paged grid with WHERE filter and ORDER BY. */
public class DataTab extends Tab {

    private static final int PAGE_SIZE = 500;

    private final ConnectionProfile profile;
    private final DbObject table;
    private final ResultGrid grid = new ResultGrid();
    private final TextField filterField = new TextField();
    private final TextField orderField = new TextField();
    private final Label pageLabel = new Label();
    private final Label statusLabel = new Label("Loading…");
    private final Button prevButton = new Button();
    private final Button nextButton = new Button();

    private int page = 0;
    private long totalRows = -1;

    public DataTab(ConnectionProfile profile, DbObject table) {
        this.profile = profile;
        this.table = table;

        setText(table.getName());
        setGraphic(Icons.of(FontAwesomeSolid.TABLE, "#4a88c7", 11));

        filterField.setPromptText("WHERE …  (e.g. status = 'active')");
        filterField.setPrefWidth(280);
        filterField.setOnAction(e -> reloadFromStart());

        orderField.setPromptText("ORDER BY …  (e.g. id DESC)");
        orderField.setPrefWidth(200);
        orderField.setOnAction(e -> reloadFromStart());

        Button applyButton = new Button("Apply");
        applyButton.setOnAction(e -> reloadFromStart());

        Button refreshButton = new Button();
        refreshButton.setGraphic(Icons.of(FontAwesomeSolid.SYNC_ALT, "#6897bb", 11));
        refreshButton.setTooltip(new Tooltip("Refresh"));
        refreshButton.setOnAction(e -> loadPage());

        Button exportButton = new Button();
        exportButton.setGraphic(Icons.of(FontAwesomeSolid.FILE_CSV, "#e0a44c", 11));
        exportButton.setTooltip(new Tooltip("Export current page to CSV"));
        exportButton.setOnAction(e -> grid.exportCsv());

        prevButton.setGraphic(Icons.of(FontAwesomeSolid.CHEVRON_LEFT, "#a9b7c6", 11));
        prevButton.setOnAction(e -> { if (page > 0) { page--; loadPage(); } });
        nextButton.setGraphic(Icons.of(FontAwesomeSolid.CHEVRON_RIGHT, "#a9b7c6", 11));
        nextButton.setOnAction(e -> { page++; loadPage(); });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, filterField, orderField, applyButton, refreshButton,
                exportButton, spacer, prevButton, pageLabel, nextButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.getStyleClass().add("console-toolbar");

        statusLabel.getStyleClass().add("console-status");
        HBox statusBar = new HBox(statusLabel);
        statusBar.setPadding(new Insets(4, 10, 4, 10));
        statusBar.getStyleClass().add("console-status-bar");

        VBox root = new VBox(toolbar, grid, statusBar);
        VBox.setVgrow(grid, Priority.ALWAYS);
        setContent(root);

        loadPage();
    }

    private void reloadFromStart() {
        page = 0;
        totalRows = -1;
        loadPage();
    }

    private void loadPage() {
        statusLabel.setText("Loading…");
        String where = filterField.getText().trim();
        String order = orderField.getText().trim();
        int currentPage = page;

        AppExecutor.run(() -> {
            try {
                var client = ClientRegistry.jdbc(profile);
                QueryResult result = client.fetchTablePage(
                        table.qualifiedName(), currentPage * PAGE_SIZE, PAGE_SIZE, where, order);
                if (totalRows < 0) {
                    try {
                        totalRows = client.countRows(table.qualifiedName(), where);
                    } catch (Exception ignore) {
                        totalRows = -1;
                    }
                }
                Platform.runLater(() -> {
                    grid.showResult(result);
                    long from = (long) currentPage * PAGE_SIZE + 1;
                    long to = from + result.getRows().size() - 1;
                    pageLabel.setText(result.getRows().isEmpty()
                            ? "0 rows"
                            : from + "–" + to + (totalRows >= 0 ? " of " + totalRows : ""));
                    statusLabel.setText(result.getRows().size() + " row(s) in "
                            + result.getExecutionMillis() + " ms");
                    prevButton.setDisable(currentPage == 0);
                    nextButton.setDisable(result.getRows().size() < PAGE_SIZE);
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> statusLabel.setText("Error: " + msg));
            }
        });
    }
}
