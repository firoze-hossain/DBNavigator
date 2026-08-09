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

/** MongoDB collection viewer: JSON filter, pagination, flattened document grid. */
public class MongoCollectionTab extends Tab {

    private static final int PAGE_SIZE = 200;

    private final ConnectionProfile profile;
    private final DbObject collection;
    private final ResultGrid grid = new ResultGrid();
    private final TextField filterField = new TextField();
    private final Label pageLabel = new Label();
    private final Label statusLabel = new Label("Loading…");
    private final Button prevButton = new Button();
    private final Button nextButton = new Button();

    private int page = 0;
    private long totalDocs = -1;
    private String sortField;
    private String sortDirection;   // "ASC", "DESC", or null
    private QueryResult lastResult;

    public MongoCollectionTab(ConnectionProfile profile, DbObject collection) {
        this.profile = profile;
        this.collection = collection;

        setText(collection.getCatalog() + "." + collection.getName());
        setGraphic(Icons.of(FontAwesomeSolid.LEAF, "#57965c", 11));

        grid.setSortRequestListener((columnName, direction) -> {
            sortField = direction == null ? null : columnName;
            sortDirection = direction;
            grid.setCurrentSort(columnName, direction);
            reloadFromStart();
        });
        grid.setEditable(true);
        grid.enableEditing(this::onCellEdit);

        filterField.setPromptText("Filter JSON, e.g. {\"status\": \"active\", \"age\": {\"$gt\": 21}}");
        HBox.setHgrow(filterField, Priority.ALWAYS);
        filterField.setOnAction(e -> reloadFromStart());

        Button applyButton = new Button("Find");
        applyButton.setGraphic(Icons.of(FontAwesomeSolid.SEARCH, "#6897bb", 11));
        applyButton.setOnAction(e -> reloadFromStart());

        Button exportButton = new Button();
        exportButton.setGraphic(Icons.of(FontAwesomeSolid.FILE_CSV, "#e0a44c", 11));
        exportButton.setTooltip(new Tooltip("Export current page to CSV"));
        exportButton.setOnAction(e -> grid.exportCsv());

        prevButton.setGraphic(Icons.of(FontAwesomeSolid.CHEVRON_LEFT, "#a9b7c6", 11));
        prevButton.setOnAction(e -> { if (page > 0) { page--; loadPage(); } });
        nextButton.setGraphic(Icons.of(FontAwesomeSolid.CHEVRON_RIGHT, "#a9b7c6", 11));
        nextButton.setOnAction(e -> { page++; loadPage(); });

        Region spacer = new Region();
        HBox toolbar = new HBox(8, filterField, applyButton, exportButton,
                spacer, prevButton, pageLabel, nextButton);
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
        totalDocs = -1;
        loadPage();
    }

    private void loadPage() {
        statusLabel.setText("Loading…");
        String filter = filterField.getText().trim();
        int currentPage = page;

        AppExecutor.run(() -> {
            try {
                var client = ClientRegistry.mongo(profile);
                QueryResult result = client.find(collection.getCatalog(), collection.getName(),
                        filter, sortField, "DESC".equals(sortDirection), currentPage * PAGE_SIZE, PAGE_SIZE);
                if (totalDocs < 0) {
                    try {
                        totalDocs = client.countDocuments(
                                collection.getCatalog(), collection.getName(), filter);
                    } catch (Exception ignore) {
                        totalDocs = -1;
                    }
                }
                Platform.runLater(() -> {
                    lastResult = result;
                    grid.showResult(result);
                    long from = (long) currentPage * PAGE_SIZE + 1;
                    long to = from + result.getRows().size() - 1;
                    pageLabel.setText(result.getRows().isEmpty()
                            ? "0 docs"
                            : from + "–" + to + (totalDocs >= 0 ? " of " + totalDocs : ""));
                    statusLabel.setText(result.getRows().size() + " document(s) in "
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

    /**
     * Commits an inline cell edit as a real {@code updateOne({_id: ...}, {$set: {field: value}})}
     * against the collection — not just an in-memory grid change. {@code _id}
     * itself can't be edited this way (MongoDB doesn't allow modifying a
     * document's {@code _id} via update; that would need a delete + re-insert
     * instead), so that column stays read-only.
     */
    private void onCellEdit(int rowIndex, int columnIndex, String oldValue, String newValue) {
        if (lastResult == null || rowIndex < 0 || rowIndex >= lastResult.getRows().size()) return;
        java.util.List<String> columns = lastResult.getColumns();
        if (columnIndex < 0 || columnIndex >= columns.size()) return;
        String columnName = columns.get(columnIndex);

        if ("_id".equals(columnName)) {
            statusLabel.setText("_id can't be edited here \u2014 delete and re-insert the document instead");
            grid.refresh();
            return;
        }
        int idIndex = columns.indexOf("_id");
        if (idIndex < 0) {
            statusLabel.setText("Can't locate this document's _id \u2014 edit not saved");
            grid.refresh();
            return;
        }
        String idValue = lastResult.getRows().get(rowIndex).get(idIndex);
        if (java.util.Objects.equals(oldValue, newValue)) return;

        String filterJson = idFilterJson(idValue);
        String updateJson = "{\"$set\": {\"" + columnName.replace("\"", "\\\"")
                + "\": " + toJsonValue(newValue) + "}}";

        statusLabel.setText("Saving\u2026");
        AppExecutor.run(() -> {
            try {
                var result = ClientRegistry.mongo(profile).updateOne(
                        collection.getCatalog(), collection.getName(), filterJson, updateJson);
                Platform.runLater(() -> statusLabel.setText(result.message()));
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> {
                    statusLabel.setText("Update failed: " + msg);
                    grid.refresh();
                });
            }
        });
    }

    /**
     * Builds a filter matching one document by its displayed {@code _id}
     * text. The grid only has the flattened string form to work with (the
     * original typed BSON value isn't kept around after rendering), so this
     * recognizes the overwhelmingly common case — a 24-hex-character
     * ObjectId — and falls back to a plain number or string otherwise,
     * which covers collections that use their own natural-key {@code _id}.
     */
    private static String idFilterJson(String idValue) {
        if (idValue.matches("[0-9a-fA-F]{24}")) {
            return "{\"_id\": ObjectId(\"" + idValue + "\")}";
        }
        if (idValue.matches("-?\\d+(\\.\\d+)?")) {
            return "{\"_id\": " + idValue + "}";
        }
        return "{\"_id\": \"" + idValue.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
    }

    /**
     * Best-effort typed encoding of a typed-in cell value: recognizable
     * numbers and booleans are stored as such, "null" clears the field,
     * everything else is stored as a string. Scope note: this covers the
     * common scalar types — entering a date, ObjectId, array, or nested
     * object as text isn't supported here, only via the console's
     * {@code updateOne(...)} for now.
     */
    private static String toJsonValue(String newValue) {
        if (newValue == null || newValue.equalsIgnoreCase("null")) return "null";
        if (newValue.equalsIgnoreCase("true") || newValue.equalsIgnoreCase("false")) {
            return newValue.toLowerCase(java.util.Locale.ROOT);
        }
        if (newValue.matches("-?\\d+")) return newValue;
        if (newValue.matches("-?\\d+\\.\\d+")) return newValue;
        return "\"" + newValue.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
