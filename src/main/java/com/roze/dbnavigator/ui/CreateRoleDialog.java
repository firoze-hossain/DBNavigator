package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.sql.Connection;
import java.sql.Statement;

/**
 * DataGrip-style "Create Role" dialog with live SQL preview.
 * Supports PostgreSQL and MySQL/MariaDB syntax dynamically.
 */
public final class CreateRoleDialog {

    private CreateRoleDialog() {}

    public static void show(MainWindow mainWindow, ConnectionProfile profile) {
        Stage stage = new Stage();
        Window owner = mainWindow.getOwnerWindow();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Create Role");
        stage.setResizable(true);
        stage.setMinWidth(480);
        stage.setMinHeight(340);

        boolean isPg = profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL;

        TextField roleNameField = new TextField();
        roleNameField.setPromptText("role_name");

        CheckBox inheritCheck = new CheckBox("Inherit");
        inheritCheck.setSelected(true);

        CheckBox canLoginCheck = new CheckBox("Can login");

        CheckBox createDbCheck = new CheckBox("Create DB");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 8, 20));

        int row = 0;
        grid.add(fieldLabel("Role name"), 0, row);
        grid.add(roleNameField, 1, row++);

        if (isPg) {
            HBox flags = new HBox(12, inheritCheck, canLoginCheck, createDbCheck);
            grid.add(fieldLabel("Options"), 0, row);
            grid.add(flags, 1, row++);
        }

        ColumnConstraints labelCol = new ColumnConstraints(100);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        TextArea preview = new TextArea();
        preview.setEditable(false);
        preview.setPrefRowCount(4);
        preview.getStyleClass().add("process-output");
        Label previewLabel = new Label("Preview");
        previewLabel.getStyleClass().add("connection-field-label");

        Runnable refreshPreview = () -> {
            String r = roleNameField.getText().trim();
            if (isPg) {
                StringBuilder sb = new StringBuilder("CREATE ROLE ");
                sb.append(r.isBlank() ? "<role_name>" : "\"" + r + "\"");
                if (inheritCheck.isSelected()) {
                    sb.append(" INHERIT");
                } else {
                    sb.append(" NOINHERIT");
                }
                if (canLoginCheck.isSelected()) {
                    sb.append(" LOGIN");
                }
                if (createDbCheck.isSelected()) {
                    sb.append(" CREATEDB");
                }
                sb.append(";");
                preview.setText(sb.toString());
            } else {
                StringBuilder sb = new StringBuilder("CREATE ROLE ");
                sb.append(r.isBlank() ? "<role_name>" : "'" + r + "'");
                sb.append(";");
                preview.setText(sb.toString());
            }
        };

        roleNameField.textProperty().addListener((o, a, b) -> refreshPreview.run());
        inheritCheck.selectedProperty().addListener((o, a, b) -> refreshPreview.run());
        canLoginCheck.selectedProperty().addListener((o, a, b) -> refreshPreview.run());
        createDbCheck.selectedProperty().addListener((o, a, b) -> refreshPreview.run());
        refreshPreview.run();

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> stage.close());

        Button openInConsole = new Button("Open in Console");
        openInConsole.setOnAction(e -> {
            stage.close();
            mainWindow.openQueryTab(profile, null, preview.getText());
        });

        Button execute = new Button("Execute");
        execute.getStyleClass().add("run-button");
        execute.setDefaultButton(true);
        execute.setOnAction(e -> {
            String r = roleNameField.getText().trim();
            if (r.isBlank()) {
                DialogTheme.apply(new Alert(Alert.AlertType.WARNING, "Enter a role name.")).showAndWait();
                return;
            }
            String sql = preview.getText();
            stage.close();
            executeSql(mainWindow, profile, r, sql);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10, openInConsole, spacer, execute, cancel);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(10, 20, 16, 20));

        VBox content = new VBox(10, grid, new Separator(), previewLabel, preview);
        content.setPadding(new Insets(0, 20, 0, 20));
        VBox.setVgrow(preview, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setCenter(content);
        root.setBottom(buttons);

        Scene scene = new Scene(root);
        DialogTheme.applyToScene(scene);
        stage.setScene(scene);
        stage.show();
    }

    private static void executeSql(MainWindow mainWindow, ConnectionProfile profile, String name, String sql) {
        mainWindow.setStatus("Creating role " + name + "…");
        AppExecutor.run(() -> {
            try (Connection conn = ClientRegistry.jdbc(profile, null).getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                Platform.runLater(() -> {
                    mainWindow.setStatus("Role " + name + " created successfully.");
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
                Platform.runLater(() -> {
                    mainWindow.setStatus("Failed to create role: " + msg);
                    DialogTheme.apply(new Alert(Alert.AlertType.ERROR, "Error creating role:\n" + msg)).showAndWait();
                });
            }
        });
    }

    private static Label fieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("connection-field-label");
        return lbl;
    }
}
