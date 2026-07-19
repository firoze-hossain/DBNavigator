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
import java.util.ArrayList;
import java.util.List;

/**
 * DataGrip-style "Create" dialog for a new database: Name, Comment,
 * Template/Allow Connections toggles, Tablespace, Owner, and a live SQL
 * preview — matching the reference dialog's layout and default state.
 *
 * Honest scope note: the Grants section is shown for visual parity
 * ("Nothing to show" + toolbar) but isn't wired to real grant-editing yet —
 * add roles/privileges afterward from a console instead.
 */
public final class CreateDatabaseDialog {

    private CreateDatabaseDialog() {}

    public static void show(MainWindow mainWindow, ConnectionProfile profile) {
        if (profile.getType() != ConnectionProfile.DatabaseType.POSTGRESQL) {
            DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION,
                    "Creating a database from this dialog is only available for PostgreSQL right now — "
                    + "for other engines, run CREATE DATABASE from a console."))
                    .showAndWait();
            return;
        }

        Stage stage = new Stage();
        Window owner = mainWindow.getOwnerWindow();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Create");
        stage.setResizable(true);
        stage.setMinWidth(560);
        stage.setMinHeight(520);

        TextField nameField = new TextField();
        nameField.setPromptText("database_name");
        TextField commentField = new TextField();
        CheckBox templateCheck = new CheckBox();
        CheckBox allowConnectionsCheck = new CheckBox();
        allowConnectionsCheck.setSelected(true);
        ComboBox<String> tablespaceCombo = new ComboBox<>();
        tablespaceCombo.setEditable(true);
        ComboBox<String> ownerCombo = new ComboBox<>();
        ownerCombo.setEditable(true);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 8, 20));
        int row = 0;
        grid.add(fieldLabel("Name"), 0, row);
        grid.add(withGrow(nameField), 1, row++);
        grid.add(fieldLabel("Comment"), 0, row);
        grid.add(withGrow(commentField), 1, row++);
        grid.add(fieldLabel("Template"), 0, row);
        grid.add(templateCheck, 1, row++);
        grid.add(fieldLabel("Allow Connections"), 0, row);
        grid.add(allowConnectionsCheck, 1, row++);
        grid.add(fieldLabel("Tablespace"), 0, row);
        tablespaceCombo.setPrefWidth(300);
        grid.add(tablespaceCombo, 1, row++);
        grid.add(fieldLabel("Owner"), 0, row);
        ownerCombo.setPrefWidth(300);
        grid.add(ownerCombo, 1, row++);

        ColumnConstraints labelCol = new ColumnConstraints(110);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        // ---- Grants (visual parity only — see class javadoc) ----
        Label grantsLabel = new Label("Grants");
        grantsLabel.getStyleClass().add("connection-field-label");
        Button addGrant = new Button();
        addGrant.setGraphic(Icons.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.PLUS, "#57965c", 10));
        addGrant.setDisable(true);
        addGrant.setTooltip(new Tooltip("Grant editing isn't supported here yet — use a console"));
        Button removeGrant = new Button();
        removeGrant.setGraphic(Icons.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.MINUS, "#a9b7c6", 10));
        removeGrant.setDisable(true);
        HBox grantsToolbar = new HBox(4, addGrant, removeGrant);
        Label grantsPlaceholder = new Label("Nothing to show");
        grantsPlaceholder.getStyleClass().add("console-status");
        VBox grantsBox = new VBox(4, grantsToolbar,
                wrapInBorder(centered(grantsPlaceholder), 80));

        // ---- Preview ----
        TextArea preview = new TextArea();
        preview.setEditable(false);
        preview.setPrefRowCount(6);
        preview.getStyleClass().add("process-output");
        Label previewLabel = new Label("Preview");
        previewLabel.getStyleClass().add("connection-field-label");

        VBox content = new VBox(14, grid, new Separator(), grantsLabel, grantsBox,
                new Separator(), previewLabel, preview);
        content.setPadding(new Insets(0, 20, 8, 20));
        VBox.setVgrow(preview, Priority.ALWAYS);

        Runnable refreshPreview = () -> preview.setText(
                buildSql(nameField.getText(), commentField.getText(), templateCheck.isSelected(),
                        allowConnectionsCheck.isSelected(), tablespaceCombo.getEditor().getText(),
                        ownerCombo.getEditor().getText()));

        nameField.textProperty().addListener((o, a, b) -> refreshPreview.run());
        commentField.textProperty().addListener((o, a, b) -> refreshPreview.run());
        templateCheck.selectedProperty().addListener((o, a, b) -> refreshPreview.run());
        allowConnectionsCheck.selectedProperty().addListener((o, a, b) -> refreshPreview.run());
        tablespaceCombo.getEditor().textProperty().addListener((o, a, b) -> refreshPreview.run());
        ownerCombo.getEditor().textProperty().addListener((o, a, b) -> refreshPreview.run());
        refreshPreview.run();

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> stage.close());
        Button ok = new Button("OK");
        ok.getStyleClass().add("run-button");
        ok.setDefaultButton(true);
        ok.setOnAction(e -> create(mainWindow, profile, nameField.getText().trim(),
                commentField.getText(), templateCheck.isSelected(), allowConnectionsCheck.isSelected(),
                tablespaceCombo.getEditor().getText().trim(), ownerCombo.getEditor().getText().trim(), stage));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10, spacer, ok, cancel);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(0, 20, 16, 20));

        VBox root = new VBox(content, buttons);
        root.getStyleClass().add("app-root");
        VBox.setVgrow(content, Priority.ALWAYS);
        Scene scene = new Scene(root, 600, 560);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
        stage.show();

        // Populate Owner/Tablespace suggestions in the background
        AppExecutor.run(() -> {
            List<String> owners = queryList(profile, "SELECT rolname FROM pg_roles ORDER BY rolname");
            List<String> tablespaces = queryList(profile, "SELECT spcname FROM pg_tablespace ORDER BY spcname");
            Platform.runLater(() -> {
                ownerCombo.getItems().setAll(owners);
                tablespaceCombo.getItems().setAll(tablespaces);
            });
        });
    }

    private static List<String> queryList(ConnectionProfile profile, String sql) {
        List<String> result = new ArrayList<>();
        try (Connection conn = ClientRegistry.jdbc(profile, null).getConnection();
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {
            while (rs.next()) result.add(rs.getString(1));
        } catch (Exception ignored) {
            // best-effort suggestions only — an empty list just means free typing
        }
        return result;
    }

    private static Label fieldLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("connection-field-label");
        return l;
    }

    private static TextField withGrow(TextField field) {
        HBox.setHgrow(field, Priority.ALWAYS);
        GridPane.setHgrow(field, Priority.ALWAYS);
        return field;
    }

    private static Region wrapInBorder(Region content, double height) {
        content.setPrefHeight(height);
        content.setStyle("-fx-border-color: #43454a; -fx-border-radius: 4; -fx-background-color: #1e1f22;");
        return content;
    }

    private static VBox centered(Label label) {
        VBox box = new VBox(label);
        box.setAlignment(Pos.CENTER);
        VBox.setVgrow(label, Priority.ALWAYS);
        return box;
    }

    private static String buildSql(String name, String comment, boolean template, boolean allowConnections,
                                   String tablespace, String owner) {
        String dbName = name == null || name.isBlank() ? "database_name" : name.trim();
        StringBuilder sql = new StringBuilder("create database ").append(quote(dbName));

        List<String> clauses = new ArrayList<>();
        if (owner != null && !owner.isBlank()) clauses.add("owner = " + quote(owner));
        if (tablespace != null && !tablespace.isBlank()) clauses.add("tablespace = " + quote(tablespace));
        if (!allowConnections) clauses.add("allow_connections = false");
        if (template) clauses.add("is_template = true");

        if (!clauses.isEmpty()) {
            sql.append("\n    ").append(String.join("\n    ", clauses));
        }
        sql.append(";");

        if (comment != null && !comment.isBlank()) {
            sql.append("\ncomment on database ").append(quote(dbName))
                    .append(" is '").append(comment.replace("'", "''")).append("';");
        }
        return sql.toString();
    }

    private static String quote(String ident) {
        if (ident.matches("[A-Za-z_][A-Za-z0-9_]*")) return ident;
        return "\"" + ident.replace("\"", "\"\"") + "\"";
    }

    private static void create(MainWindow mainWindow, ConnectionProfile profile, String name, String comment,
                               boolean template, boolean allowConnections, String tablespace, String owner,
                               Stage stage) {
        if (name.isBlank()) {
            DialogTheme.apply(new Alert(Alert.AlertType.WARNING, "Enter a database name.")).showAndWait();
            return;
        }
        String sql = buildSql(name, comment, template, allowConnections, tablespace, owner);
        stage.close();

        AppExecutor.run(() -> {
            try (Connection conn = ClientRegistry.jdbc(profile, null).getConnection();
                 Statement stmt = conn.createStatement()) {
                for (String statement : sql.split(";")) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty()) stmt.execute(trimmed);
                }
                Platform.runLater(() -> {
                    mainWindow.setStatus("Created database " + name);
                    mainWindow.refreshSchemaExplorer();
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                        "Could not create database: " + msg)).showAndWait());
            }
        });
    }
}
