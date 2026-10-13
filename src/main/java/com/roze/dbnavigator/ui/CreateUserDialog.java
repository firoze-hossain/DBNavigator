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
 * DataGrip-style "Create User" dialog with live SQL preview.
 * Supports PostgreSQL and MySQL/MariaDB syntax dynamically.
 */
public final class CreateUserDialog {

    private CreateUserDialog() {}

    public static void show(MainWindow mainWindow, ConnectionProfile profile) {
        Stage stage = new Stage();
        Window owner = mainWindow.getOwnerWindow();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Create User");
        stage.setResizable(true);
        stage.setMinWidth(480);
        stage.setMinHeight(360);

        boolean isPg = profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL;
        boolean isSqlServer = profile.getType() == ConnectionProfile.DatabaseType.SQLSERVER;

        stage.setTitle(isSqlServer ? "Create Server Principal" : "Create User");

        TextField usernameField = new TextField();
        usernameField.setPromptText(isSqlServer ? "login_name" : "username");

        TextField hostField = new TextField("%");
        hostField.setPromptText("%");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("password");

        CheckBox canLoginCheck = new CheckBox("Can login");
        canLoginCheck.setSelected(true);

        CheckBox superuserCheck = new CheckBox("Superuser");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 8, 20));

        int row = 0;
        grid.add(fieldLabel(isSqlServer ? "Login name" : "User name"), 0, row);
        grid.add(usernameField, 1, row++);

        if (!isPg && !isSqlServer) {
            grid.add(fieldLabel("Host"), 0, row);
            grid.add(hostField, 1, row++);
        }

        grid.add(fieldLabel("Password"), 0, row);
        grid.add(passwordField, 1, row++);

        if (isPg) {
            HBox flags = new HBox(12, canLoginCheck, superuserCheck);
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
            String u = usernameField.getText().trim();
            String p = passwordField.getText();
            String h = hostField.getText().trim();
            if (isPg) {
                StringBuilder sb = new StringBuilder("CREATE USER ");
                sb.append(u.isBlank() ? "<username>" : "\"" + u + "\"");
                if (!p.isBlank()) {
                    sb.append(" WITH PASSWORD '").append(p.replace("'", "''")).append("'");
                }
                if (superuserCheck.isSelected()) {
                    sb.append(" SUPERUSER");
                }
                if (!canLoginCheck.isSelected()) {
                    sb.append(" NOLOGIN");
                }
                sb.append(";");
                preview.setText(sb.toString());
            } else if (isSqlServer) {
                StringBuilder sb = new StringBuilder("CREATE LOGIN ");
                sb.append(u.isBlank() ? "<login_name>" : "[" + u + "]");
                if (!p.isBlank()) {
                    sb.append(" WITH PASSWORD = '").append(p.replace("'", "''")).append("'");
                }
                sb.append(";");
                preview.setText(sb.toString());
            } else {
                StringBuilder sb = new StringBuilder("CREATE USER ");
                sb.append(u.isBlank() ? "<username>" : "'" + u + "'");
                sb.append("@").append(h.isBlank() ? "'%'" : "'" + h + "'");
                if (!p.isBlank()) {
                    sb.append(" IDENTIFIED BY '").append(p.replace("'", "''")).append("'");
                }
                sb.append(";");
                preview.setText(sb.toString());
            }
        };

        usernameField.textProperty().addListener((o, a, b) -> refreshPreview.run());
        hostField.textProperty().addListener((o, a, b) -> refreshPreview.run());
        passwordField.textProperty().addListener((o, a, b) -> refreshPreview.run());
        canLoginCheck.selectedProperty().addListener((o, a, b) -> refreshPreview.run());
        superuserCheck.selectedProperty().addListener((o, a, b) -> refreshPreview.run());
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
            String u = usernameField.getText().trim();
            if (u.isBlank()) {
                DialogTheme.apply(new Alert(Alert.AlertType.WARNING,
                        isSqlServer ? "Enter a login name." : "Enter a user name.")).showAndWait();
                return;
            }
            String sql = preview.getText();
            stage.close();
            executeSql(mainWindow, profile, u, sql);
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
        boolean isSqlServer = profile.getType() == ConnectionProfile.DatabaseType.SQLSERVER;
        String entityNoun = isSqlServer ? "server principal " : "user ";
        mainWindow.setStatus("Creating " + entityNoun + name + "…");
        AppExecutor.run(() -> {
            try (Connection conn = ClientRegistry.jdbc(profile, null).getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                Platform.runLater(() -> {
                    mainWindow.setStatus((isSqlServer ? "Server principal " : "User ") + name + " created successfully.");
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
                Platform.runLater(() -> {
                    mainWindow.setStatus("Failed to create user: " + msg);
                    DialogTheme.apply(new Alert(Alert.AlertType.ERROR, "Error creating user:\n" + msg)).showAndWait();
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
