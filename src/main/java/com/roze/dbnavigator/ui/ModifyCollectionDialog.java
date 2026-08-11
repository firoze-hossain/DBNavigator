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
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.ArrayList;
import java.util.List;

/**
 * DataGrip-style "Modify" dialog for a MongoDB collection. MongoDB has no
 * fixed schema to alter the way a SQL table does, so this offers the real
 * operations that actually apply to a collection: renaming it, and
 * managing its indexes (list, drop, create).
 *
 * Honest scope note: the reference IDE's virtual columns / virtual foreign
 * keys are metadata-only annotations layered on top of the IDE's own
 * inferred schema view — they don't correspond to any real MongoDB
 * operation and don't change stored documents at all. Building that would
 * mean a whole separate annotation-persistence layer for the IDE's own use,
 * not a database capability, which isn't implemented here — this dialog
 * focuses on the two things that are real, executable database changes.
 */
public final class ModifyCollectionDialog {

    private ModifyCollectionDialog() {}

    private record IndexRow(String name, String detail, javafx.beans.property.SimpleBooleanProperty drop) {}

    public static void show(MainWindow mainWindow, ConnectionProfile profile, DbObject collection) {
        Stage stage = new Stage();
        Window owner = mainWindow.getOwnerWindow();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Modify Collection \u2014 " + collection.getName());
        stage.setMinWidth(620);
        stage.setMinHeight(520);

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

        Label indexesLabel = new Label("Indexes");
        indexesLabel.getStyleClass().add("connection-field-label");

        ObservableList<IndexRow> indexRows = FXCollections.observableArrayList();
        TableView<IndexRow> indexTable = new TableView<>(indexRows);
        indexTable.setEditable(true);
        indexTable.setPrefHeight(160);

        TableColumn<IndexRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().name()));
        nameCol.setPrefWidth(160);
        TableColumn<IndexRow, String> detailCol = new TableColumn<>("Keys");
        detailCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().detail()));
        detailCol.setPrefWidth(220);
        TableColumn<IndexRow, Boolean> dropCol = new TableColumn<>("Drop");
        dropCol.setCellValueFactory(d -> d.getValue().drop());
        dropCol.setCellFactory(CheckBoxTableCell.forTableColumn(dropCol));
        dropCol.setEditable(true);
        dropCol.setPrefWidth(60);
        indexTable.getColumns().addAll(nameCol, detailCol, dropCol);

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
        pendingIndexList.setPrefHeight(70);

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

        VBox content = new VBox(10, nameGrid, new Separator(), indexesLabel, indexTable,
                new Label("New indexes to create:"), pendingIndexList, addIndexRow);
        content.setPadding(new Insets(0, 16, 8, 16));
        VBox.setVgrow(indexTable, Priority.SOMETIMES);

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> stage.close());
        Button execute = new Button("Execute");
        execute.getStyleClass().add("run-button");
        execute.setDefaultButton(true);
        execute.setOnAction(e -> {
            try {
                String newName = nameField.getText().strip();
                List<String> toDropIndexes = new ArrayList<>();
                for (IndexRow row : indexRows) {
                    if (row.drop().get()) toDropIndexes.add(row.name());
                }
                stage.close();
                applyChanges(mainWindow, profile, collection, newName, toDropIndexes, pendingNewIndexes);
            } catch (Exception ex) {
                // Nothing between clicking Execute and the actual database work
                // should ever be able to fail invisibly — show it directly
                // rather than letting JavaFX's default handler swallow it.
                showAlert(mainWindow, Alert.AlertType.ERROR,
                        "Could not start Modify Collection: " + describeError(ex));
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10, spacer, execute, cancel);
        buttons.setPadding(new Insets(0, 16, 16, 16));

        VBox root = new VBox(content, buttons);
        root.getStyleClass().add("app-root");
        VBox.setVgrow(content, Priority.ALWAYS);

        Scene scene = new Scene(root, 640, 560);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
        stage.show();

        AppExecutor.run(() -> {
            try {
                List<MongoDbClient.IndexInfo> indexes = ClientRegistry.mongo(profile)
                        .listIndexes(collection.getCatalog(), collection.getName());
                Platform.runLater(() -> {
                    for (MongoDbClient.IndexInfo idx : indexes) {
                        indexRows.add(new IndexRow(idx.name(), idx.detail(),
                                new javafx.beans.property.SimpleBooleanProperty(false)));
                    }
                });
            } catch (Exception ignored) {
                // best-effort listing only
            }
        });
    }

    /**
     * Does the real database work first, entirely off the FX thread and
     * completely independent of the Run panel, then shows a direct,
     * unmissable Alert with the outcome — success or failure — before
     * attempting anything else. Run panel logging is attempted afterward,
     * in its own try/catch, specifically so that even if something about
     * that panel's own state has a problem, it can never prevent the user
     * from seeing the actual result of the operation they asked for.
     */
    private static void applyChanges(MainWindow mainWindow, ConnectionProfile profile, DbObject collection,
                                     String newName, List<String> toDropIndexes, List<String[]> newIndexes) {
        String currentName = collection.getName();
        if (toDropIndexes.isEmpty() && newIndexes.isEmpty()
                && (newName.equals(currentName) || newName.isBlank())) {
            showAlert(mainWindow, Alert.AlertType.INFORMATION,
                    "No changes to apply \u2014 nothing was renamed, and no indexes were added or checked for drop.");
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
                    showAlert(mainWindow, Alert.AlertType.ERROR,
                            "Modify Collection failed: " + describeError(finalFailure));
                } else {
                    mainWindow.refreshSchemaExplorer();
                    String summary = finalApplied.isEmpty() ? "(nothing applied)" : String.join("\n", finalApplied);
                    showAlert(mainWindow, Alert.AlertType.INFORMATION,
                            "Collection modified successfully:\n\n" + summary);
                }
                logToRunPanelBestEffort(mainWindow, collection, finalApplied, finalFailure);
            });
        });
    }

    private static void logToRunPanelBestEffort(MainWindow mainWindow, DbObject collection,
                                                List<String> applied, Exception failure) {
        try {
            RunPanel.RunHandle handle = mainWindow.getRunPanel()
                    .startRun("Modify Collection \u2014 " + collection.getName());
            mainWindow.showRunPanel();
            for (String line : applied) handle.appendLine(line);
            if (failure != null) {
                handle.markFailed(describeError(failure));
            } else {
                handle.appendLine("\u2713 Done");
                handle.markFinished(0);
            }
        } catch (Exception ignored) {
            // The user already has the real answer from the alert shown above.
        }
    }

    private static void showAlert(MainWindow mainWindow, Alert.AlertType type, String message) {
        Alert alert = DialogTheme.apply(new Alert(type, message));
        Window owner = mainWindow.getOwnerWindow();
        if (owner != null) alert.initOwner(owner);
        alert.showAndWait();
    }

    private static String describeError(Exception ex) {
        return ex.getMessage() == null ? ex.toString() : ex.getMessage();
    }
}
