package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DataGrip-style "Modify Table…": add, drop, or rename columns, preview the
 * generated ALTER TABLE statements, then execute them all in one transaction.
 * Honest scope note: doesn't support changing a column's type or constraints
 * in this version — add/drop/rename covers the large majority of real-world
 * "Modify Table" usage without the added risk of an in-place type change.
 */
public final class ModifyTableDialog {

    /** One row of the editable column list. */
    public static class ColumnRow {
        final javafx.beans.property.SimpleStringProperty originalName;
        final javafx.beans.property.SimpleStringProperty name;
        final javafx.beans.property.SimpleStringProperty type;
        final javafx.beans.property.SimpleBooleanProperty nullable;
        final javafx.beans.property.SimpleBooleanProperty primaryKey;
        final javafx.beans.property.SimpleBooleanProperty markedForDrop =
                new javafx.beans.property.SimpleBooleanProperty(false);
        final boolean isNew;

        ColumnRow(String originalName, String name, String type, boolean nullable, boolean pk, boolean isNew) {
            this.originalName = new javafx.beans.property.SimpleStringProperty(originalName);
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.type = new javafx.beans.property.SimpleStringProperty(type);
            this.nullable = new javafx.beans.property.SimpleBooleanProperty(nullable);
            this.primaryKey = new javafx.beans.property.SimpleBooleanProperty(pk);
            this.isNew = isNew;
        }
    }

    private final MainWindow mainWindow;
    private final ConnectionProfile profile;
    private final DbObject table;
    private final Stage stage = new Stage();
    private final ObservableList<ColumnRow> rows = FXCollections.observableArrayList();
    private final TableView<ColumnRow> grid = new TableView<>();
    private final TextArea preview = new TextArea();

    private ModifyTableDialog(MainWindow mainWindow, ConnectionProfile profile, DbObject table) {
        this.mainWindow = mainWindow;
        this.profile = profile;
        this.table = table;
        buildUi();
        loadColumns();
    }

    public static void show(MainWindow mainWindow, ConnectionProfile profile, DbObject table) {
        new ModifyTableDialog(mainWindow, profile, table).stage.showAndWait();
    }

    private void buildUi() {
        stage.initOwner(mainWindow.getOwnerWindow());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Modify Table \u2014 " + table.getName());
        stage.setMinWidth(760);
        stage.setMinHeight(560);

        grid.setEditable(true);
        grid.setItems(rows);

        TableColumn<ColumnRow, String> nameCol = new TableColumn<>("Column");
        nameCol.setCellValueFactory(d -> d.getValue().name);
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(e -> { e.getRowValue().name.set(e.getNewValue()); refreshPreview(); });
        nameCol.setPrefWidth(200);

        TableColumn<ColumnRow, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(d -> d.getValue().type);
        typeCol.setCellFactory(TextFieldTableCell.forTableColumn());
        typeCol.setOnEditCommit(e -> { e.getRowValue().type.set(e.getNewValue()); refreshPreview(); });
        typeCol.setPrefWidth(160);

        TableColumn<ColumnRow, Boolean> nullableCol = new TableColumn<>("Nullable");
        nullableCol.setCellValueFactory(d -> d.getValue().nullable);
        nullableCol.setCellFactory(CheckBoxTableCell.forTableColumn(nullableCol));
        nullableCol.setEditable(true);
        nullableCol.setPrefWidth(70);

        TableColumn<ColumnRow, Boolean> pkCol = new TableColumn<>("PK");
        pkCol.setCellValueFactory(d -> d.getValue().primaryKey);
        pkCol.setEditable(false);
        pkCol.setPrefWidth(40);

        TableColumn<ColumnRow, Boolean> dropCol = new TableColumn<>("Drop");
        dropCol.setCellValueFactory(d -> d.getValue().markedForDrop);
        dropCol.setCellFactory(CheckBoxTableCell.forTableColumn(dropCol));
        dropCol.setEditable(true);
        dropCol.setPrefWidth(50);

        grid.getColumns().addAll(nameCol, typeCol, nullableCol, pkCol, dropCol);
        VBox.setVgrow(grid, Priority.ALWAYS);

        Button addColumn = new Button("Add Column");
        addColumn.setGraphic(Icons.of(FontAwesomeSolid.PLUS, "#57965c", 11));
        addColumn.setOnAction(e -> {
            ColumnRow newRow = new ColumnRow(null, "new_column", "varchar(255)", true, false, true);
            wireLiveRefresh(newRow);
            rows.add(newRow);
            refreshPreview();
        });
        Button refreshHint = new Button("Refresh Preview");
        refreshHint.setGraphic(Icons.of(FontAwesomeSolid.SYNC_ALT, "#6897bb", 11));
        refreshHint.setOnAction(e -> refreshPreview());

        HBox toolbar = new HBox(8, addColumn, refreshHint);
        toolbar.setPadding(new Insets(0, 0, 8, 0));

        Label previewLabel = new Label("Generated DDL (edit above, then Execute):");
        previewLabel.getStyleClass().add("connection-field-label");
        preview.setEditable(false);
        preview.setPrefRowCount(6);
        preview.getStyleClass().add("process-output");

        VBox content = new VBox(8, toolbar, grid, previewLabel, preview);
        content.setPadding(new Insets(16));
        VBox.setVgrow(grid, Priority.ALWAYS);

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> stage.close());
        Button execute = new Button("Execute");
        execute.getStyleClass().add("run-button");
        execute.setDefaultButton(true);
        execute.setOnAction(e -> executeChanges());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10, spacer, cancel, execute);
        buttons.setPadding(new Insets(0, 16, 16, 16));

        VBox root = new VBox(content, buttons);
        root.getStyleClass().add("app-root");
        VBox.setVgrow(content, Priority.ALWAYS);

        Scene scene = new Scene(root, 820, 640);
        Window owner = mainWindow.getOwnerWindow();
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
    }

    private void wireLiveRefresh(ColumnRow row) {
        row.nullable.addListener((obs, o, n) -> refreshPreview());
        row.markedForDrop.addListener((obs, o, n) -> refreshPreview());
    }

    private void loadColumns() {
        AppExecutor.run(() -> {
            try {
                List<MetadataService.ColumnInfo> columns = MetadataService.loadColumnInfo(profile, table);
                List<ColumnRow> loaded = new ArrayList<>();
                for (MetadataService.ColumnInfo c : columns) {
                    ColumnRow row = new ColumnRow(c.name(), c.name(), c.typeName(), c.nullable(), c.primaryKey(), false);
                    wireLiveRefresh(row);
                    loaded.add(row);
                }
                Platform.runLater(() -> {
                    rows.setAll(loaded);
                    refreshPreview();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> preview.setText("-- Error loading columns: " + ex.getMessage()));
            }
        });
    }

    private List<String> buildAlterStatements() {
        List<String> statements = new ArrayList<>();
        String qualifiedTable = table.qualifiedName();

        for (ColumnRow row : rows) {
            if (row.isNew) {
                if (row.markedForDrop.get()) continue;   // added then removed — no-op
                statements.add("ALTER TABLE " + qualifiedTable + " ADD COLUMN "
                        + DbObject.quote(row.name.get()) + " " + row.type.get()
                        + (row.nullable.get() ? "" : " NOT NULL") + ";");
            } else if (row.markedForDrop.get()) {
                statements.add("ALTER TABLE " + qualifiedTable + " DROP COLUMN "
                        + DbObject.quote(row.originalName.get()) + ";");
            } else if (!row.originalName.get().equals(row.name.get())) {
                statements.add("ALTER TABLE " + qualifiedTable + " RENAME COLUMN "
                        + DbObject.quote(row.originalName.get()) + " TO " + DbObject.quote(row.name.get()) + ";");
            }
        }
        return statements;
    }

    private void refreshPreview() {
        List<String> statements = buildAlterStatements();
        preview.setText(statements.isEmpty()
                ? "-- No changes yet — edit a name/type, check Drop, or Add Column"
                : String.join("\n", statements));
    }

    private void executeChanges() {
        List<String> statements = buildAlterStatements();
        if (statements.isEmpty()) {
            stage.close();
            return;
        }

        Alert confirm = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.CONFIRMATION,
                "Run " + statements.size() + " ALTER TABLE statement(s) on " + table.getName() + "?",
                ButtonType.YES, ButtonType.NO));
        confirm.initOwner(stage);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        stage.close();
        RunPanel.RunHandle handle = mainWindow.getRunPanel().startRun("Modify Table \u2014 " + table.getName());
        mainWindow.showRunPanel();

        AppExecutor.run(() -> {
            try (Connection conn = ClientRegistry.jdbc(profile, table.getCatalog()).getConnection()) {
                conn.setAutoCommit(false);
                try (Statement stmt = conn.createStatement()) {
                    for (String sql : statements) {
                        handle.appendLine(sql);
                        stmt.execute(sql);
                    }
                    conn.commit();
                } catch (Exception inner) {
                    conn.rollback();
                    throw inner;
                }
                handle.appendLine("\u2713 Applied " + statements.size() + " change(s)");
                handle.markFinished(0);
            } catch (Exception ex) {
                handle.markFailed(ex.getMessage() == null ? ex.toString() : ex.getMessage());
            }
        });
    }
}
