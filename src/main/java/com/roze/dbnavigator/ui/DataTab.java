package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.db.MetadataService;
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

import java.util.ArrayList;
import java.util.List;

/**
 * DataGrip-style table data view: breadcrumb showing exactly which
 * connection ▸ database ▸ schema ▸ table is open, paged grid with WHERE/ORDER BY,
 * inline editing (incl. date picker), multi-row delete, transactional Submit.
 */
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
    private final Button submitButton = new Button("Submit");
    private final Button revertButton = new Button("Revert");
    private final GridEditManager editManager;

    private final List<String> pkColumns = new ArrayList<>();
    private boolean useCtid = false;
    private int page = 0;
    private long totalRows = -1;

    public DataTab(ConnectionProfile profile, DbObject table) {
        this.profile = profile;
        this.table = table;

        setText(table.getName());
        setGraphic(Icons.of(FontAwesomeSolid.TABLE, "#4a88c7", 11));
        setTooltip(new Tooltip(breadcrumbText()));

        submitButton.setGraphic(Icons.of(FontAwesomeSolid.CHECK, "#57965c", 11));
        submitButton.setTooltip(new Tooltip("Write pending edits/deletes to the database"));
        revertButton.setGraphic(Icons.of(FontAwesomeSolid.UNDO, "#e05555", 11));
        revertButton.setTooltip(new Tooltip("Discard pending edits and reload"));

        editManager = new GridEditManager(profile, table.getCatalog(), grid,
                submitButton, revertButton, this::loadPage, statusLabel::setText);

        filterField.setPromptText("WHERE …  (e.g. status = 'active')");
        filterField.setPrefWidth(240);
        filterField.setOnAction(e -> reloadFromStart());

        orderField.setPromptText("ORDER BY …  (e.g. id DESC)");
        orderField.setPrefWidth(170);
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
                submitButton, revertButton, exportButton, spacer, prevButton, pageLabel, nextButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.getStyleClass().add("console-toolbar");

        // Breadcrumb bar: connection ▸ database ▸ schema ▸ table (DataGrip style)
        Label crumb = new Label(breadcrumbText());
        crumb.getStyleClass().add("breadcrumb");
        crumb.setGraphic(Icons.of(FontAwesomeSolid.MAP_MARKER_ALT, "#868a91", 10));
        HBox crumbBar = new HBox(crumb);
        crumbBar.setPadding(new Insets(4, 10, 4, 10));
        crumbBar.getStyleClass().add("breadcrumb-bar");

        statusLabel.getStyleClass().add("console-status");
        HBox statusBar = new HBox(statusLabel);
        statusBar.setPadding(new Insets(4, 10, 4, 10));
        statusBar.getStyleClass().add("console-status-bar");

        VBox root = new VBox(crumbBar, toolbar, grid, statusBar);
        VBox.setVgrow(grid, Priority.ALWAYS);
        setContent(root);

        detectPrimaryKeyThenLoad();
    }

    private String breadcrumbText() {
        StringBuilder sb = new StringBuilder(profile.getName());
        if (table.getCatalog() != null && !table.getCatalog().isBlank()) {
            sb.append("  ▸  ").append(table.getCatalog());
        }
        if (table.getSchema() != null && !table.getSchema().isBlank()) {
            sb.append("  ▸  ").append(table.getSchema());
        }
        sb.append("  ▸  ").append(table.getName());
        return sb.toString();
    }

    private void detectPrimaryKeyThenLoad() {
        AppExecutor.run(() -> {
            try {
                pkColumns.addAll(MetadataService.loadPrimaryKeys(profile, table));
            } catch (Exception ignored) {}
            // PostgreSQL tables without a PK (typically partitions) are still
            // editable through the physical row id (ctid) — DataGrip does the same
            if (pkColumns.isEmpty()
                    && profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL) {
                useCtid = true;
                pkColumns.add("ctid");
            }
            Platform.runLater(this::loadPage);
        });
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
                var client = ClientRegistry.jdbc(profile, table.getCatalog());
                QueryResult result = client.fetchTablePage(
                        table.qualifiedName(), currentPage * PAGE_SIZE, PAGE_SIZE,
                        where, order, useCtid);
                if (totalRows < 0) {
                    try {
                        totalRows = client.countRows(table.qualifiedName(), where);
                    } catch (Exception ignore) {
                        totalRows = -1;
                    }
                }
                Platform.runLater(() -> {
                    editManager.configure(table.qualifiedName(), pkColumns, result);
                    grid.showResult(result);
                    long from = (long) currentPage * PAGE_SIZE + 1;
                    long to = from + result.getRows().size() - 1;
                    pageLabel.setText(result.getRows().isEmpty()
                            ? "0 rows"
                            : from + "–" + to + (totalRows >= 0 ? " of " + totalRows : ""));
                    statusLabel.setText(result.getRows().size() + " row(s) in "
                            + result.getExecutionMillis() + " ms"
                            + (editManager.isEditable()
                                ? (useCtid
                                    ? "  ·  editable via row id (no primary key)"
                                    : "  ·  double-click a cell to edit, select rows + Delete to remove")
                                : "  ·  read-only (no primary key)"));
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
