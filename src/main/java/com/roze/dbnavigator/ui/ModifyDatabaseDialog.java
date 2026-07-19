package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.DatabaseAdminService;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * DataGrip-style "Modify Database…" dialog. Honest scope note: PostgreSQL
 * exposes many more ALTER DATABASE properties (encoding, connection limit,
 * tablespace, etc.) than are editable here — this covers the two most
 * commonly changed ones, rename and owner, rather than every property.
 */
public final class ModifyDatabaseDialog {

    private ModifyDatabaseDialog() {}

    public static void show(MainWindow mainWindow, ConnectionProfile profile, String database) {
        Stage stage = new Stage();
        Window owner = mainWindow.getOwnerWindow();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Modify Database \u2014 " + database);
        stage.setResizable(false);

        TextField nameField = new TextField(database);
        TextField ownerField = new TextField();
        ownerField.setPromptText("(loading current owner\u2026)");
        ownerField.setDisable(true);

        boolean supportsRename = profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL;
        nameField.setDisable(!supportsRename);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 8, 20));
        Label nameLabel = new Label("Name:");
        nameLabel.getStyleClass().add("connection-field-label");
        Label ownerLabel = new Label("Owner:");
        ownerLabel.getStyleClass().add("connection-field-label");
        nameField.setPrefWidth(260);
        ownerField.setPrefWidth(260);
        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(ownerLabel, 0, 1);
        grid.add(ownerField, 1, 1);

        Label hint = new Label(supportsRename
                ? "Renaming or changing the owner requires no other session be using this database."
                : profile.getType().getDisplayName() + " doesn't support renaming a database directly here.");
        hint.getStyleClass().add("console-status");
        hint.setWrapText(true);
        hint.setMaxWidth(360);

        VBox content = new VBox(10, grid, hint);
        content.setPadding(new Insets(0, 20, 8, 20));

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> stage.close());
        Button apply = new Button("Apply");
        apply.getStyleClass().add("run-button");
        apply.setDefaultButton(true);
        apply.setOnAction(e -> applyChanges(mainWindow, profile, database, nameField.getText().trim(),
                ownerField.getText().trim(), stage));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10, spacer, cancel, apply);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(0, 20, 16, 20));

        VBox root = new VBox(content, buttons);
        root.getStyleClass().add("app-root");
        Scene scene = new Scene(root, 440, 220);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
        stage.show();

        AppExecutor.run(() -> {
            String currentOwner = DatabaseAdminService.currentOwner(profile, database);
            Platform.runLater(() -> {
                if (currentOwner != null) {
                    ownerField.setText(currentOwner);
                    ownerField.setDisable(!supportsRename);
                    ownerField.setPromptText("");
                } else {
                    ownerField.setPromptText("(unavailable)");
                }
            });
        });
    }

    private static void applyChanges(MainWindow mainWindow, ConnectionProfile profile, String originalName,
                                     String newName, String newOwner, Stage stage) {
        boolean rename = !newName.isBlank() && !newName.equals(originalName);
        boolean ownerChange = !newOwner.isBlank();

        if (!rename && !ownerChange) {
            stage.close();
            return;
        }

        stage.close();
        AppExecutor.run(() -> {
            try {
                String currentName = originalName;
                if (rename) {
                    DatabaseAdminService.renameDatabase(profile, currentName, newName);
                    currentName = newName;
                }
                if (ownerChange) {
                    DatabaseAdminService.changeOwner(profile, currentName, newOwner);
                }
                Platform.runLater(() -> {
                    mainWindow.setStatus("Modified database " + originalName);
                    mainWindow.refreshSchemaExplorer();
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                        "Could not modify database: " + msg)).showAndWait());
            }
        });
    }
}
