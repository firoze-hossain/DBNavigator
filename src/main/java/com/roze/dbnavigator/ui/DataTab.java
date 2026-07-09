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

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DataGrip-style table data view: paged grid with WHERE filter and ORDER BY.
 * Cells are editable when the table has a primary key — edits are collected
 * as pending changes and written back with Submit (like DataGrip's DML preview).
 */
public class DataTab extends Tab {

    private static final int PAGE_SIZE = 500;

    /** One pending cell change, keyed by the row's primary-key values at edit time. */
    private record PendingChange(Map<String, String> pkValues, String column, String newValue) {}

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

    private final List<String> pkColumns = new ArrayList<>();
    private final List<PendingChange> pending = new ArrayList<>();
    private QueryResult currentResult;

    private int page = 0;
    private long totalRows = -1;

    public DataTab(ConnectionProfile profile, DbObject table) {
        this.profile = profile;
        this.table = table;

        setText(table.getName());
        setGraphic(Icons.of(FontAwesomeSolid.TABLE, "#4a88c7", 11));

        filterField.setPromptText("WHERE …  (e.g. status = 'active')");
        filterField.setPrefWidth(260);
        filterField.setOnAction(e -> reloadFromStart());

        orderField.setPromptText("ORDER BY …  (e.g. id DESC)");
        orderField.setPrefWidth(180);
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

        submitButton.setGraphic(Icons.of(FontAwesomeSolid.CHECK, "#57965c", 11));
        submitButton.setTooltip(new Tooltip("Write pending edits to the database"));
        submitButton.setDisable(true);
        submitButton.setOnAction(e -> submitChanges());

        revertButton.setGraphic(Icons.of(FontAwesomeSolid.UNDO, "#e05555", 11));
        revertButton.setTooltip(new Tooltip("Discard pending edits and reload"));
        revertButton.setDisable(true);
        revertButton.setOnAction(e -> {
            pending.clear();
            updatePendingState();
            loadPage();
        });

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

        statusLabel.getStyleClass().add("console-status");
        HBox statusBar = new HBox(statusLabel);
        statusBar.setPadding(new Insets(4, 10, 4, 10));
        statusBar.getStyleClass().add("console-status-bar");

        VBox root = new VBox(toolbar, grid, statusBar);
        VBox.setVgrow(grid, Priority.ALWAYS);
        setContent(root);

        detectPrimaryKeyThenLoad();
    }

    // ---------------------------------------------------------------- load

    private void detectPrimaryKeyThenLoad() {
        AppExecutor.run(() -> {
            try {
                pkColumns.addAll(MetadataService.loadPrimaryKeys(profile, table));
            } catch (Exception ignored) {}
            Platform.runLater(() -> {
                if (!pkColumns.isEmpty()) {
                    grid.enableEditing(this::onCellEdited);
                }
                loadPage();
            });
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
                        table.qualifiedName(), currentPage * PAGE_SIZE, PAGE_SIZE, where, order);
                if (totalRows < 0) {
                    try {
                        totalRows = client.countRows(table.qualifiedName(), where);
                    } catch (Exception ignore) {
                        totalRows = -1;
                    }
                }
                Platform.runLater(() -> {
                    currentResult = result;
                    grid.showResult(result);
                    long from = (long) currentPage * PAGE_SIZE + 1;
                    long to = from + result.getRows().size() - 1;
                    pageLabel.setText(result.getRows().isEmpty()
                            ? "0 rows"
                            : from + "–" + to + (totalRows >= 0 ? " of " + totalRows : ""));
                    statusLabel.setText(result.getRows().size() + " row(s) in "
                            + result.getExecutionMillis() + " ms"
                            + (pkColumns.isEmpty()
                                ? "  ·  read-only (no primary key)"
                                : "  ·  double-click a cell to edit"));
                    prevButton.setDisable(currentPage == 0);
                    nextButton.setDisable(result.getRows().size() < PAGE_SIZE);
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> statusLabel.setText("Error: " + msg));
            }
        });
    }

    // ---------------------------------------------------------------- edit

    private void onCellEdited(int rowIndex, int columnIndex, String oldValue, String newValue) {
        if (currentResult == null) return;
        List<String> columns = currentResult.getColumns();
        List<String> row = grid.getItems().get(rowIndex);

        // Snapshot the primary-key values that identify this row.
        // If the edited column IS a pk column, use its pre-edit value.
        Map<String, String> pkValues = new LinkedHashMap<>();
        for (String pk : pkColumns) {
            int pkIndex = indexOfColumn(columns, pk);
            if (pkIndex < 0) {
                statusLabel.setText("Cannot edit: primary key column " + pk + " is not in the result");
                return;
            }
            pkValues.put(pk, pkIndex == columnIndex ? oldValue : row.get(pkIndex));
        }

        pending.add(new PendingChange(pkValues, columns.get(columnIndex), newValue));
        updatePendingState();
    }

    private static int indexOfColumn(List<String> columns, String name) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).equalsIgnoreCase(name)) return i;
        }
        return -1;
    }

    private void updatePendingState() {
        boolean has = !pending.isEmpty();
        submitButton.setDisable(!has);
        revertButton.setDisable(!has);
        submitButton.setText(has ? "Submit (" + pending.size() + ")" : "Submit");
        if (has) statusLabel.setText(pending.size() + " pending change(s) — press Submit to save");
    }

    private void submitChanges() {
        if (pending.isEmpty()) return;
        List<PendingChange> toApply = new ArrayList<>(pending);
        submitButton.setDisable(true);
        statusLabel.setText("Submitting " + toApply.size() + " change(s)…");

        AppExecutor.run(() -> {
            try (Connection conn = ClientRegistry.jdbc(profile, table.getCatalog()).getConnection()) {
                conn.setAutoCommit(false);
                try (Statement stmt = conn.createStatement()) {
                    for (PendingChange change : toApply) {
                        stmt.executeUpdate(buildUpdateSql(change));
                    }
                    conn.commit();
                } catch (Exception inner) {
                    conn.rollback();
                    throw inner;
                }
                Platform.runLater(() -> {
                    pending.clear();
                    updatePendingState();
                    statusLabel.setText("✓ " + toApply.size() + " change(s) committed");
                    loadPage();
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> {
                    statusLabel.setText("Commit failed (rolled back): " + msg);
                    submitButton.setDisable(false);
                });
            }
        });
    }

    private String buildUpdateSql(PendingChange change) {
        StringBuilder sql = new StringBuilder("UPDATE ")
                .append(table.qualifiedName())
                .append(" SET ").append(DbObject.quote(change.column()))
                .append(" = ").append(literal(change.newValue()))
                .append(" WHERE ");
        boolean first = true;
        for (var pk : change.pkValues().entrySet()) {
            if (!first) sql.append(" AND ");
            sql.append(DbObject.quote(pk.getKey()));
            if (pk.getValue() == null) {
                sql.append(" IS NULL");
            } else {
                sql.append(" = ").append(literal(pk.getValue()));
            }
            first = false;
        }
        return sql.toString();
    }

    /**
     * Converts a grid string back into an SQL literal:
     * "NULL" → NULL, numbers unquoted, everything else a quoted string.
     */
    private static String literal(String value) {
        if (value == null || value.equals("NULL")) return "NULL";
        if (value.matches("-?\\d+(\\.\\d+)?")) return value;
        return "'" + value.replace("'", "''") + "'";
    }
}
