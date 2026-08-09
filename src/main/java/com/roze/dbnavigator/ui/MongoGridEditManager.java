package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.db.MongoDbClient;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.QueryResult;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.scene.control.Button;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Shared editing controller for a {@link ResultGrid} showing MongoDB
 * documents — same Submit/Revert pattern as {@link GridEditManager} for SQL
 * grids: edits are collected as pending (grouped by document {@code _id}, so
 * several field edits on the same document collapse into a single
 * {@code updateOne($set)}) rather than written immediately, and only
 * actually saved when Submit is pressed. {@code _id} itself can't be edited
 * this way (MongoDB doesn't support changing a document's {@code _id} via
 * update — that needs a delete + re-insert instead).
 */
public class MongoGridEditManager {

    private final ConnectionProfile profile;
    private final ResultGrid grid;
    private final Button submitButton;
    private final Button revertButton;
    private final Runnable reloader;
    private final Consumer<String> status;

    private String database;
    private String collection;
    private QueryResult currentResult;
    private boolean editable;

    /** _id (displayed text form) -> (field name -> new value), so repeat edits on one document merge. */
    private final Map<String, Map<String, String>> pendingByDoc = new LinkedHashMap<>();

    public MongoGridEditManager(ConnectionProfile profile, ResultGrid grid,
                                Button submitButton, Button revertButton,
                                Runnable reloader, Consumer<String> status) {
        this.profile = profile;
        this.grid = grid;
        this.submitButton = submitButton;
        this.revertButton = revertButton;
        this.reloader = reloader;
        this.status = status;

        grid.enableEditing(this::onCellEdited);
        submitButton.setOnAction(e -> submit());
        revertButton.setOnAction(e -> {
            clearPending();
            reloader.run();
        });
        updateButtons();
    }

    /** Binds the manager to a fresh result. Call BEFORE grid.showResult(result). */
    public void configure(String database, String collection, QueryResult result) {
        this.database = database;
        this.collection = collection;
        this.currentResult = result;
        clearPending();
        editable = database != null && collection != null && result != null
                && result.getColumns().contains("_id");
        grid.setEditable(editable);
    }

    public boolean hasPending() { return !pendingByDoc.isEmpty(); }

    // ------------------------------------------------------------- editing

    private void onCellEdited(int rowIndex, int columnIndex, String oldValue, String newValue) {
        if (!editable || currentResult == null) return;
        List<String> columns = currentResult.getColumns();
        if (columnIndex < 0 || columnIndex >= columns.size()) return;
        String columnName = columns.get(columnIndex);

        if ("_id".equals(columnName)) {
            status.accept("_id can't be edited here \u2014 delete and re-insert the document instead");
            grid.refresh();
            return;
        }
        int idIndex = columns.indexOf("_id");
        if (idIndex < 0 || rowIndex < 0 || rowIndex >= grid.getItems().size()) return;
        String idValue = grid.getItems().get(rowIndex).get(idIndex);

        pendingByDoc.computeIfAbsent(idValue, k -> new LinkedHashMap<>()).put(columnName, newValue);
        updateButtons();
    }

    private void clearPending() {
        pendingByDoc.clear();
        updateButtons();
    }

    private void updateButtons() {
        boolean has = hasPending();
        submitButton.setDisable(!has);
        revertButton.setDisable(!has);
        int totalFields = pendingByDoc.values().stream().mapToInt(Map::size).sum();
        submitButton.setText(has ? "Submit (" + pendingByDoc.size() + ")" : "Submit");
        if (has) {
            status.accept(pendingByDoc.size() + " document(s), " + totalFields
                    + " field edit(s) pending \u2014 press Submit to save");
        }
    }

    // -------------------------------------------------------------- submit

    private void submit() {
        if (!hasPending()) return;
        Map<String, Map<String, String>> toApply = new LinkedHashMap<>(pendingByDoc);
        submitButton.setDisable(true);
        status.accept("Submitting\u2026");

        AppExecutor.run(() -> {
            int succeeded = 0;
            List<String> failures = new ArrayList<>();
            MongoDbClient client = ClientRegistry.mongo(profile);
            for (Map.Entry<String, Map<String, String>> entry : toApply.entrySet()) {
                try {
                    client.updateOne(database, collection,
                            idFilterJson(entry.getKey()), buildSetJson(entry.getValue()));
                    succeeded++;
                } catch (Exception ex) {
                    String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                    failures.add(entry.getKey() + ": " + msg);
                }
            }
            int finalSucceeded = succeeded;
            Platform.runLater(() -> {
                clearPending();
                status.accept(failures.isEmpty()
                        ? "\u2713 " + finalSucceeded + " document(s) updated"
                        : "\u2713 " + finalSucceeded + " updated, " + failures.size()
                                + " failed: " + String.join("; ", failures));
                reloader.run();
            });
        });
    }

    // ---------------------------------------------------------- JSON build

    private static String buildSetJson(Map<String, String> fieldEdits) {
        StringBuilder sb = new StringBuilder("{\"$set\": {");
        boolean first = true;
        for (Map.Entry<String, String> e : fieldEdits.entrySet()) {
            if (!first) sb.append(", ");
            sb.append('"').append(e.getKey().replace("\"", "\\\"")).append("\": ")
                    .append(toJsonValue(e.getValue()));
            first = false;
        }
        return sb.append("}}").toString();
    }

    /**
     * Builds a filter matching one document by its displayed {@code _id}
     * text. The grid only has the flattened string form to work with, so
     * this recognizes the overwhelmingly common case — a 24-hex-character
     * ObjectId — and falls back to a plain number or string otherwise,
     * covering collections with their own natural-key {@code _id}.
     */
    static String idFilterJson(String idValue) {
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
     * everything else is stored as a string.
     */
    static String toJsonValue(String newValue) {
        if (newValue == null || newValue.equalsIgnoreCase("null")) return "null";
        if (newValue.equalsIgnoreCase("true") || newValue.equalsIgnoreCase("false")) {
            return newValue.toLowerCase(Locale.ROOT);
        }
        if (newValue.matches("-?\\d+")) return newValue;
        if (newValue.matches("-?\\d+\\.\\d+")) return newValue;
        return "\"" + newValue.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
