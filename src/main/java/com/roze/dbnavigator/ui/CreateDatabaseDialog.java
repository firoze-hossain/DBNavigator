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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DataGrip-style "Create" dialog for a new database. Field set is engine-
 * specific, since "create a database" means genuinely different things per
 * engine: PostgreSQL gets Comment/Template/Allow Connections/Tablespace/
 * Owner (matching the reference dialog); MySQL/MariaDB get Character Set/
 * Collation (the properties that actually exist for a MySQL database).
 * Every path ends in the same live-updating SQL preview + Run pattern.
 *
 * Honest scope note: the PostgreSQL Grants section is shown for visual
 * parity ("Nothing to show" + toolbar) but isn't wired to real grant-
 * editing yet — add roles/privileges afterward from a console instead.
 * SQL Server/Oracle/SQLite still show the "use a console" message.
 */
public final class CreateDatabaseDialog {

    private CreateDatabaseDialog() {}

    /**
     * Whether "New > Database…" does something real for this engine.
     * PostgreSQL and MySQL/MariaDB have an actual CREATE DATABASE workflow
     * with meaningful options (encoding, owner, collation, etc). SQLite has
     * no such concept — a SQLite connection already *is* one database, tied
     * to exactly one file, with no server-side "create another one" step
     * (this mirrors DataGrip, which likewise doesn't offer it for SQLite).
     * SQL Server/Oracle are included here for the same reason: {@link #show}
     * only ever shows a "use a console" message for them today.
     */
    public static boolean supportsCreate(ConnectionProfile.DatabaseType type) {
        return switch (type) {
            case POSTGRESQL, MYSQL, MARIADB -> true;
            default -> false;
        };
    }

    public static void show(MainWindow mainWindow, ConnectionProfile profile) {
        switch (profile.getType()) {
            case POSTGRESQL -> showPostgres(mainWindow, profile);
            case MYSQL, MARIADB -> showMySql(mainWindow, profile);
            default -> DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION,
                    "Creating a database from this dialog is only available for PostgreSQL and "
                    + "MySQL/MariaDB right now — for other engines, run CREATE DATABASE from a console."))
                    .showAndWait();
        }
    }

    // =========================================================== PostgreSQL

    private static void showPostgres(MainWindow mainWindow, ConnectionProfile profile) {
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
                buildPostgresSql(nameField.getText(), commentField.getText(), templateCheck.isSelected(),
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
        ok.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isBlank()) {
                DialogTheme.apply(new Alert(Alert.AlertType.WARNING, "Enter a database name.")).showAndWait();
                return;
            }
            String sql = buildPostgresSql(name, commentField.getText(), templateCheck.isSelected(),
                    allowConnectionsCheck.isSelected(), tablespaceCombo.getEditor().getText().trim(),
                    ownerCombo.getEditor().getText().trim());
            stage.close();
            runCreate(mainWindow, profile, name, sql);
        });

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

        AppExecutor.run(() -> {
            List<String> owners = queryList(profile, "SELECT rolname FROM pg_roles ORDER BY rolname");
            List<String> tablespaces = queryList(profile, "SELECT spcname FROM pg_tablespace ORDER BY spcname");
            Platform.runLater(() -> {
                ownerCombo.getItems().setAll(owners);
                tablespaceCombo.getItems().setAll(tablespaces);
            });
        });
    }

    private static String buildPostgresSql(String name, String comment, boolean template, boolean allowConnections,
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

    // ============================================================== MySQL

    private static void showMySql(MainWindow mainWindow, ConnectionProfile profile) {
        Stage stage = new Stage();
        Window owner = mainWindow.getOwnerWindow();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Create");
        stage.setResizable(true);
        stage.setMinWidth(520);
        stage.setMinHeight(340);

        TextField nameField = new TextField();
        nameField.setPromptText("database_name");
        ComboBox<String> charsetCombo = new ComboBox<>();
        charsetCombo.setEditable(true);
        charsetCombo.getItems().add("utf8mb4");
        charsetCombo.getSelectionModel().selectFirst();
        ComboBox<String> collationCombo = new ComboBox<>();
        collationCombo.setEditable(true);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 8, 20));
        int row = 0;
        grid.add(fieldLabel("Name"), 0, row);
        grid.add(withGrow(nameField), 1, row++);
        grid.add(fieldLabel("Character Set"), 0, row);
        charsetCombo.setPrefWidth(300);
        grid.add(charsetCombo, 1, row++);
        grid.add(fieldLabel("Collation"), 0, row);
        collationCombo.setPrefWidth(300);
        grid.add(collationCombo, 1, row++);

        ColumnConstraints labelCol = new ColumnConstraints(110);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        TextArea preview = new TextArea();
        preview.setEditable(false);
        preview.setPrefRowCount(4);
        preview.getStyleClass().add("process-output");
        Label previewLabel = new Label("Preview");
        previewLabel.getStyleClass().add("connection-field-label");

        VBox content = new VBox(14, grid, new Separator(), previewLabel, preview);
        content.setPadding(new Insets(0, 20, 8, 20));
        VBox.setVgrow(preview, Priority.ALWAYS);

        // Collations are specific to a character set — reload the list whenever
        // the charset changes, keeping whatever the user already typed if it's
        // still valid for the new charset.
        Map<String, List<String>> collationsByCharset = new LinkedHashMap<>();
        Runnable reloadCollations = () -> {
            String charset = charsetCombo.getEditor().getText().trim();
            List<String> collations = collationsByCharset.get(charset);
            if (collations != null) {
                String current = collationCombo.getEditor().getText();
                collationCombo.getItems().setAll(collations);
                collationCombo.getSelectionModel().select(
                        collations.contains(current) ? current : (collations.isEmpty() ? "" : collations.get(0)));
            }
        };

        Runnable refreshPreview = () -> preview.setText(buildMySqlSql(
                nameField.getText(), charsetCombo.getEditor().getText(), collationCombo.getEditor().getText()));

        nameField.textProperty().addListener((o, a, b) -> refreshPreview.run());
        charsetCombo.getEditor().textProperty().addListener((o, a, b) -> refreshPreview.run());
        collationCombo.getEditor().textProperty().addListener((o, a, b) -> refreshPreview.run());
        charsetCombo.valueProperty().addListener((o, a, b) -> { reloadCollations.run(); refreshPreview.run(); });
        refreshPreview.run();

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> stage.close());
        Button ok = new Button("OK");
        ok.getStyleClass().add("run-button");
        ok.setDefaultButton(true);
        ok.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isBlank()) {
                DialogTheme.apply(new Alert(Alert.AlertType.WARNING, "Enter a database name.")).showAndWait();
                return;
            }
            String sql = buildMySqlSql(name, charsetCombo.getEditor().getText().trim(),
                    collationCombo.getEditor().getText().trim());
            stage.close();
            runCreate(mainWindow, profile, name, sql);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10, spacer, ok, cancel);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(0, 20, 16, 20));

        VBox root = new VBox(content, buttons);
        root.getStyleClass().add("app-root");
        VBox.setVgrow(content, Priority.ALWAYS);
        Scene scene = new Scene(root, 560, 380);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
        stage.show();

        // Populate real character sets / collations from the server in the background
        AppExecutor.run(() -> {
            List<String> charsets = new ArrayList<>();
            Map<String, List<String>> byCharset = new LinkedHashMap<>();
            try (Connection conn = ClientRegistry.jdbc(profile, null).getConnection();
                 Statement stmt = conn.createStatement()) {
                try (var rs = stmt.executeQuery("SHOW CHARACTER SET")) {
                    while (rs.next()) charsets.add(rs.getString("Charset"));
                }
                try (var rs = stmt.executeQuery("SHOW COLLATION")) {
                    while (rs.next()) {
                        byCharset.computeIfAbsent(rs.getString("Charset"), k -> new ArrayList<>())
                                .add(rs.getString("Collation"));
                    }
                }
            } catch (Exception ignored) {
                // best-effort suggestions only — an empty list just means free typing
            }
            Platform.runLater(() -> {
                collationsByCharset.putAll(byCharset);
                if (!charsets.isEmpty()) {
                    String preferred = charsets.contains("utf8mb4") ? "utf8mb4" : charsets.get(0);
                    charsetCombo.getItems().setAll(charsets);
                    charsetCombo.getSelectionModel().select(preferred);
                }
                reloadCollations.run();
            });
        });
    }

    private static String buildMySqlSql(String name, String charset, String collation) {
        String dbName = name == null || name.isBlank() ? "database_name" : name.trim();
        StringBuilder sql = new StringBuilder("CREATE DATABASE ").append(backtick(dbName));
        if (charset != null && !charset.isBlank()) {
            sql.append(" CHARACTER SET ").append(charset.trim());
        }
        if (collation != null && !collation.isBlank()) {
            sql.append(" COLLATE ").append(collation.trim());
        }
        sql.append(";");
        return sql.toString();
    }

    private static String backtick(String ident) {
        return "`" + ident.replace("`", "``") + "`";
    }

    // ============================================================== shared

    private static void runCreate(MainWindow mainWindow, ConnectionProfile profile, String name, String sql) {
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

    private static String quote(String ident) {
        if (ident.matches("[A-Za-z_][A-Za-z0-9_]*")) return ident;
        return "\"" + ident.replace("\"", "\"\"") + "\"";
    }
}
