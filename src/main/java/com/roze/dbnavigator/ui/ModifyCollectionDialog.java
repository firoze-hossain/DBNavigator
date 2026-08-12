package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.db.MongoDbClient;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple "Modify" dialog for a MongoDB collection: rename it, see its
 * fields (read-only, for reference — same data as the schema tree's
 * "fields" folder, including flattened nested fields like
 * "address.city"), and manage its indexes for real (list, drop, create).
 *
 * Deliberately plain rows (Label/CheckBox in a VBox), not a TableView —
 * simpler controls, less internal machinery, less that can go wrong.
 * Feedback on Execute — success or failure, with the real message either
 * way — is a status line inside this same still-open dialog, not a
 * separate popup shown after closing it.
 *
 * Scope note: no virtual columns/foreign keys — those are the reference
 * IDE's own metadata-only annotations, not real MongoDB operations, so
 * they're not implemented here. This dialog only offers changes that are
 * genuinely executable against the database.
 */
public final class ModifyCollectionDialog {

    private ModifyCollectionDialog() {}

    private record IndexRow(String name, CheckBox dropCheck) {}

    public static void show(MainWindow mainWindow, ConnectionProfile profile, DbObject collection) {
        Stage stage = new Stage();
        Window owner = mainWindow.getOwnerWindow();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Modify Collection \u2014 " + collection.getName());
        stage.setMinWidth(580);
        stage.setMinHeight(620);

        TextField nameField = new TextField(collection.getName());

        GridPane nameGrid = new GridPane();
        nameGrid.setHgap(12);
        nameGrid.setVgap(10);
        nameGrid.setPadding(new Insets(16, 16, 8, 16));
        Label nameLabel = new Label("Name");
        nameLabel.getStyleClass().add("connection-field-label");
        nameGrid.add(nameLabel, 0, 0);
        GridPane.setHgrow(nameField, Priority.ALWAYS);
        nameGrid.add(nameField, 1, 0);
        ColumnConstraints labelCol = new ColumnConstraints(80);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        nameGrid.getColumnConstraints().addAll(labelCol, fieldCol);

        // ---- fields (read-only, for reference) ----
        Label fieldsLabel = new Label("Fields");
        fieldsLabel.getStyleClass().add("connection-field-label");
        VBox fieldsBox = new VBox(2);
        fieldsBox.setPadding(new Insets(4));
        ScrollPane fieldsScroll = new ScrollPane(fieldsBox);
        fieldsScroll.setFitToWidth(true);
        fieldsScroll.setPrefHeight(150);
        Label loadingFields = new Label("Loading\u2026");
        loadingFields.getStyleClass().add("console-status");
        fieldsBox.getChildren().add(loadingFields);

        // ---- indexes ----
        Label indexesLabel = new Label("Indexes");
        indexesLabel.getStyleClass().add("connection-field-label");

        List<IndexRow> indexRows = new ArrayList<>();
        VBox indexList = new VBox(4);
        indexList.setPadding(new Insets(4));
        ScrollPane indexScroll = new ScrollPane(indexList);
        indexScroll.setFitToWidth(true);
        indexScroll.setPrefHeight(110);
        Label loadingIndexes = new Label("Loading\u2026");
        loadingIndexes.getStyleClass().add("console-status");
        indexList.getChildren().add(loadingIndexes);

        // ---- add-index form ----
        TextField fieldNameField = new TextField();
        fieldNameField.setPromptText("field name");
        ComboBox<String> directionCombo = new ComboBox<>(FXCollections.observableArrayList("Ascending", "Descending"));
        directionCombo.getSelectionModel().selectFirst();
        CheckBox uniqueCheck = new CheckBox("Unique");
        Button addIndexButton = new Button("Add Index");
        addIndexButton.setGraphic(Icons.of(FontAwesomeSolid.PLUS, "#57965c", 11));

        List<String[]> pendingNewIndexes = new ArrayList<>();   // [fieldName, direction, unique]
        ObservableList<String> pendingIndexLabels = FXCollections.observableArrayList();
        ListView<String> pendingIndexList = new ListView<>(pendingIndexLabels);
        pendingIndexList.setPrefHeight(60);

        addIndexButton.setOnAction(e -> {
            String field = fieldNameField.getText().strip();
            if (field.isBlank()) return;
            boolean descending = "Descending".equals(directionCombo.getValue());
            boolean unique = uniqueCheck.isSelected();
            pendingNewIndexes.add(new String[]{field, String.valueOf(descending), String.valueOf(unique)});
            pendingIndexLabels.add("(" + field + " " + (descending ? "-1" : "1") + ")" + (unique ? "  UNIQUE" : "")
                    + "  \u2014 new");
            fieldNameField.clear();
            uniqueCheck.setSelected(false);
        });

        HBox addIndexRow = new HBox(8, fieldNameField, directionCombo, uniqueCheck, addIndexButton);
        addIndexRow.setAlignment(Pos.CENTER_LEFT);

        // ---- inline status line — result shows here, no separate popup ----
        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setManaged(false);
        statusLabel.setVisible(false);

        VBox content = new VBox(10, nameGrid, new Separator(),
                fieldsLabel, fieldsScroll,
                new Separator(), indexesLabel, indexScroll,
                new Label("New indexes to create:"), pendingIndexList, addIndexRow, statusLabel);
        content.setPadding(new Insets(0, 16, 8, 16));

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> stage.close());
        Button execute = new Button("Execute");
        execute.getStyleClass().add("run-button");
        execute.setDefaultButton(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10, spacer, execute, cancel);
        buttons.setPadding(new Insets(0, 16, 16, 16));

        execute.setOnAction(e -> {
            String newName = nameField.getText().strip();
            List<String> toDropIndexes = new ArrayList<>();
            for (IndexRow row : indexRows) {
                if (row.dropCheck().isSelected()) toDropIndexes.add(row.name());
            }

            execute.setDisable(true);
            nameField.setDisable(true);
            addIndexButton.setDisable(true);
            statusLabel.setManaged(true);
            statusLabel.setVisible(true);
            statusLabel.getStyleClass().setAll("console-status");
            statusLabel.setText("Working\u2026");

            applyChanges(profile, collection, newName, toDropIndexes, pendingNewIndexes,
                    result -> {
                        statusLabel.getStyleClass().setAll(result.success() ? "status-success" : "status-error");
                        statusLabel.setText(result.message());
                        execute.setDisable(true);
                        execute.setVisible(false);
                        execute.setManaged(false);
                        cancel.setText("Close");
                        cancel.setDefaultButton(true);
                        if (result.success()) mainWindow.refreshSchemaExplorer();
                    });
        });

        VBox root = new VBox(content, buttons);
        root.getStyleClass().add("app-root");
        VBox.setVgrow(content, Priority.ALWAYS);

        Scene scene = new Scene(root, 620, 660);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
        stage.show();

        loadFields(profile, collection, fieldsBox);
        loadIndexes(profile, collection, indexList, indexRows);
    }

    private static void loadFields(ConnectionProfile profile, DbObject collection, VBox fieldsBox) {
        AppExecutor.run(() -> {
            try {
                List<MongoDbClient.FieldInfo> fields = ClientRegistry.mongo(profile)
                        .inferFields(collection.getCatalog(), collection.getName(), 100);
                Platform.runLater(() -> {
                    fieldsBox.getChildren().clear();
                    for (MongoDbClient.FieldInfo field : fields) {
                        Label name = new Label(field.name());
                        name.setPrefWidth(220);
                        Label type = new Label(field.type());
                        type.getStyleClass().add("console-status");
                        HBox row = new HBox(10, name, type);
                        row.setAlignment(Pos.CENTER_LEFT);
                        fieldsBox.getChildren().add(row);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    fieldsBox.getChildren().clear();
                    Label error = new Label("Could not load fields: " + describeError(ex));
                    error.getStyleClass().add("status-error");
                    error.setWrapText(true);
                    fieldsBox.getChildren().add(error);
                });
            }
        });
    }

    private static void loadIndexes(ConnectionProfile profile, DbObject collection,
                                    VBox indexList, List<IndexRow> indexRows) {
        AppExecutor.run(() -> {
            try {
                List<MongoDbClient.IndexInfo> indexes = ClientRegistry.mongo(profile)
                        .listIndexes(collection.getCatalog(), collection.getName());
                Platform.runLater(() -> {
                    indexList.getChildren().clear();
                    if (indexes.isEmpty()) {
                        Label none = new Label("(no indexes)");
                        none.getStyleClass().add("console-status");
                        indexList.getChildren().add(none);
                        return;
                    }
                    for (MongoDbClient.IndexInfo idx : indexes) {
                        CheckBox drop = new CheckBox();
                        Label name = new Label(idx.name());
                        name.setPrefWidth(160);
                        Label detail = new Label(idx.detail());
                        detail.getStyleClass().add("console-status");
                        HBox.setHgrow(detail, Priority.ALWAYS);
                        HBox row = new HBox(10, name, detail, new Label("Drop"), drop);
                        row.setAlignment(Pos.CENTER_LEFT);
                        indexList.getChildren().add(row);
                        indexRows.add(new IndexRow(idx.name(), drop));
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    indexList.getChildren().clear();
                    Label error = new Label("Could not load indexes: " + describeError(ex));
                    error.getStyleClass().add("status-error");
                    error.setWrapText(true);
                    indexList.getChildren().add(error);
                });
            }
        });
    }

    private record Outcome(boolean success, String message) {}

    private static void applyChanges(ConnectionProfile profile, DbObject collection,
                                     String newName, List<String> toDropIndexes, List<String[]> newIndexes,
                                     java.util.function.Consumer<Outcome> onDone) {
        String currentName = collection.getName();
        if (toDropIndexes.isEmpty() && newIndexes.isEmpty()
                && (newName.equals(currentName) || newName.isBlank())) {
            onDone.accept(new Outcome(true,
                    "No changes to apply \u2014 nothing was renamed, and no indexes were added or checked for drop."));
            return;
        }

        AppExecutor.run(() -> {
            List<String> applied = new ArrayList<>();
            Exception failure = null;
            try {
                MongoDbClient client = ClientRegistry.mongo(profile);
                String database = collection.getCatalog();
                String workingName = currentName;

                for (String indexName : toDropIndexes) {
                    var result = client.dropIndex(database, workingName, indexName);
                    applied.add(result.message());
                }
                for (String[] spec : newIndexes) {
                    boolean descending = Boolean.parseBoolean(spec[1]);
                    boolean unique = Boolean.parseBoolean(spec[2]);
                    var result = client.createIndex(database, workingName, spec[0], descending, unique);
                    applied.add(result.message());
                }
                if (!newName.equals(workingName) && !newName.isBlank()) {
                    var result = client.renameCollection(database, workingName, newName);
                    applied.add(result.message());
                }
            } catch (Exception ex) {
                failure = ex;
            }

            Exception finalFailure = failure;
            List<String> finalApplied = applied;
            Platform.runLater(() -> {
                if (finalFailure != null) {
                    onDone.accept(new Outcome(false, "Failed: " + describeError(finalFailure)));
                } else {
                    String summary = finalApplied.isEmpty() ? "nothing to apply" : String.join("; ", finalApplied);
                    onDone.accept(new Outcome(true, "\u2713 " + summary));
                }
            });
        });
    }

    private static String describeError(Exception ex) {
        return ex.getMessage() == null ? ex.toString() : ex.getMessage();
    }
}
