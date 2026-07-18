package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * DataGrip-style "Restore with pg_restore/psql…" dialog: a segmented
 * pg_restore / psql tool switcher (pg_restore restores archive formats,
 * psql runs a plain-SQL dump script), each with its own option set and live
 * command preview, mirroring the real IDE dialog.
 */
public final class RestoreDialog {

    private enum Tool { PG_RESTORE, PSQL }

    private final MainWindow mainWindow;
    private final ConnectionProfile profile;
    private final Stage stage = new Stage();

    private Tool tool = Tool.PG_RESTORE;
    private final ToggleButton pgRestoreTab = new ToggleButton("pg_restore");
    private final ToggleButton psqlTab = new ToggleButton("psql");

    private final TextField pathField = new TextField();
    private final TextField databaseField = new TextField();
    private final TextField dumpPathField = new TextField();

    // pg_restore-only controls
    private final TextField schemasField = new TextField();
    private final TextField tablesField = new TextField();
    private final ComboBox<String> formatCombo = new ComboBox<>();
    private final CheckBox dropBeforeCreate = new CheckBox("Add DROP before CREATE");
    private final CheckBox createDatabase = new CheckBox("Add CREATE DATABASE and reconnect to the created one");
    private final CheckBox useIfExists = new CheckBox("Use DROP … IF EXISTS before CREATE");
    private final CheckBox dataOnly = new CheckBox("Export data without schema");
    private final CheckBox singleTxPgRestore = new CheckBox("Wrap the restore in BEGIN … COMMIT");

    // psql-only controls
    private final CheckBox singleTxPsql = new CheckBox("Wrap the restore in BEGIN … COMMIT");

    private final VBox pgRestoreOptions = new VBox(10);
    private final VBox psqlOptions = new VBox(10);
    private final TextArea commandPreview = new TextArea();

    private RestoreDialog(MainWindow mainWindow, ConnectionProfile profile, String database) {
        this.mainWindow = mainWindow;
        this.profile = profile;
        databaseField.setText(database);
        buildUi(mainWindow.getOwnerWindow());
        wireLivePreview();
        selectTool(Tool.PG_RESTORE);
    }

    public static void show(MainWindow mainWindow, ConnectionProfile profile, String database) {
        new RestoreDialog(mainWindow, profile, database).stage.showAndWait();
    }

    // --------------------------------------------------------------- UI

    private void buildUi(Window owner) {
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Restore with pg_restore/psql… (" + profile.getName() + ")");
        stage.setMinWidth(700);

        ToggleGroup toolGroup = new ToggleGroup();
        pgRestoreTab.setToggleGroup(toolGroup);
        psqlTab.setToggleGroup(toolGroup);
        pgRestoreTab.getStyleClass().add("tool-tab");
        psqlTab.getStyleClass().add("tool-tab");
        pgRestoreTab.setOnAction(e -> selectTool(Tool.PG_RESTORE));
        psqlTab.setOnAction(e -> selectTool(Tool.PSQL));
        HBox toolTabs = new HBox(4, pgRestoreTab, psqlTab);
        toolTabs.setPadding(new Insets(12, 20, 4, 20));

        RadioButton locallyRadio = new RadioButton("Locally");
        locallyRadio.setSelected(true);
        locallyRadio.setToggleGroup(new ToggleGroup());
        Tooltip.install(locallyRadio, new Tooltip("Running over an SSH tunnel isn't supported yet"));

        pathField.setPrefWidth(420);
        Button browsePath = browseButton();
        browsePath.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Locate " + (tool == Tool.PG_RESTORE ? "pg_restore" : "psql") + " executable");
            File file = chooser.showOpenDialog(stage);
            if (file != null) pathField.setText(file.getAbsolutePath());
        });

        databaseField.setPrefWidth(260);

        dumpPathField.setPromptText("Choose the dump file to restore");
        dumpPathField.setPrefWidth(420);
        Button browseDump = browseButton();
        browseDump.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose dump file");
            File file = chooser.showOpenDialog(stage);
            if (file != null) dumpPathField.setText(file.getAbsolutePath());
        });

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.setPadding(new Insets(8, 20, 8, 20));

        int row = 0;
        form.add(label("Where to run:"), 0, row);
        form.add(locallyRadio, 1, row++);
        form.add(label("Path to executable:"), 0, row);
        form.add(fieldWithBrowse(pathField, browsePath), 1, row++);

        form.add(new Separator(), 0, row, 2, 1);
        row++;
        Label optionsHeader = new Label("Options");
        optionsHeader.getStyleClass().add("panel-header");
        form.add(optionsHeader, 0, row, 2, 1);
        row++;

        form.add(label("Database:"), 0, row);
        form.add(databaseField, 1, row++);

        // ---- pg_restore-only fields ----
        schemasField.setPromptText("(all schemas)");
        schemasField.setPrefWidth(260);
        tablesField.setPromptText("(all tables)");
        tablesField.setPrefWidth(260);
        formatCombo.getItems().addAll("Auto", "Custom", "Directory", "Tar");
        formatCombo.getSelectionModel().selectFirst();
        formatCombo.setPrefWidth(260);

        GridPane pgRestoreForm = new GridPane();
        pgRestoreForm.setHgap(12);
        pgRestoreForm.setVgap(10);
        int r = 0;
        pgRestoreForm.add(label("Schemas:"), 0, r);
        pgRestoreForm.add(schemasField, 1, r++);
        pgRestoreForm.add(label("Tables to dump:"), 0, r);
        pgRestoreForm.add(tablesField, 1, r++);
        pgRestoreForm.add(label("Format:"), 0, r);
        pgRestoreForm.add(formatCombo, 1, r++);
        pgRestoreForm.add(label("Path to dump:"), 0, r);
        pgRestoreForm.add(fieldWithBrowse(dumpPathField, browseDump), 1, r++);

        GridPane pgCheckGrid = new GridPane();
        pgCheckGrid.setHgap(24);
        pgCheckGrid.setVgap(8);
        pgCheckGrid.add(dropBeforeCreate, 0, 0);
        pgCheckGrid.add(createDatabase, 1, 0);
        pgCheckGrid.add(useIfExists, 0, 1);
        pgCheckGrid.add(dataOnly, 1, 1);
        pgCheckGrid.add(singleTxPgRestore, 0, 2);

        useIfExists.setDisable(true);
        dropBeforeCreate.selectedProperty().addListener((obs, was, checked) -> {
            useIfExists.setDisable(!checked);
            if (!checked) useIfExists.setSelected(false);
        });

        pgRestoreOptions.getChildren().addAll(pgRestoreForm, pgCheckGrid);

        // ---- psql-only fields ----
        GridPane psqlForm = new GridPane();
        psqlForm.setHgap(12);
        psqlForm.setVgap(10);
        psqlForm.add(label("Path to dump:"), 0, 0);
        psqlForm.add(fieldWithBrowse(dumpPathField, browseDump), 1, 0);
        psqlOptions.getChildren().addAll(psqlForm, singleTxPsql);

        StackPane toolOptionsStack = new StackPane(pgRestoreOptions, psqlOptions);

        form.add(toolOptionsStack, 0, row, 2, 1);
        row++;

        // ---- Command preview ----
        commandPreview.setEditable(false);
        commandPreview.setWrapText(true);
        commandPreview.setPrefRowCount(5);
        commandPreview.getStyleClass().add("process-output");
        VBox previewBox = new VBox(commandPreview);
        previewBox.setPadding(new Insets(4, 20, 0, 20));
        VBox.setVgrow(commandPreview, Priority.ALWAYS);

        // ---- Buttons ----
        Button runButton = new Button("Run");
        runButton.getStyleClass().add("run-button");
        runButton.setDefaultButton(true);
        runButton.setOnAction(e -> runRestore());

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> stage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10, spacer, runButton, cancelButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(12, 20, 16, 20));

        VBox root = new VBox(toolTabs, form, previewBox, buttons);
        VBox.setVgrow(previewBox, Priority.ALWAYS);
        root.getStyleClass().add("app-root");

        Scene scene = new Scene(root, 720, 640);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
    }

    private void selectTool(Tool newTool) {
        this.tool = newTool;
        pgRestoreTab.setSelected(newTool == Tool.PG_RESTORE);
        psqlTab.setSelected(newTool == Tool.PSQL);
        pgRestoreOptions.setVisible(newTool == Tool.PG_RESTORE);
        pgRestoreOptions.setManaged(newTool == Tool.PG_RESTORE);
        psqlOptions.setVisible(newTool == Tool.PSQL);
        psqlOptions.setManaged(newTool == Tool.PSQL);

        // Refresh the executable path default unless the user typed something
        // that doesn't match either tool's default (in which case leave it alone).
        String current = pathField.getText();
        boolean isDefault = current.isBlank() || current.endsWith("/pg_restore") || current.endsWith("/psql")
                || current.equals("pg_restore") || current.equals("psql");
        if (isDefault) {
            pathField.setText(newTool == Tool.PG_RESTORE ? "/usr/bin/pg_restore" : "/usr/bin/psql");
        }
        refreshPreview();
    }

    private static Label label(String text) {
        Label l = new Label(text);
        l.setMinWidth(150);
        return l;
    }

    private static HBox fieldWithBrowse(TextField field, Button browse) {
        HBox.setHgrow(field, Priority.ALWAYS);
        HBox box = new HBox(6, field, browse);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static Button browseButton() {
        Button b = new Button();
        b.setGraphic(Icons.of(FontAwesomeSolid.FOLDER_OPEN, "#e0a44c", 12));
        return b;
    }

    // ------------------------------------------------------- live preview

    private void wireLivePreview() {
        for (TextField f : List.of(databaseField, dumpPathField, schemasField, tablesField)) {
            f.textProperty().addListener((obs, o, n) -> refreshPreview());
        }
        formatCombo.valueProperty().addListener((obs, o, n) -> refreshPreview());
        for (CheckBox c : List.of(dropBeforeCreate, createDatabase, useIfExists, dataOnly,
                singleTxPgRestore, singleTxPsql)) {
            c.selectedProperty().addListener((obs, o, n) -> refreshPreview());
        }
    }

    private List<String> buildPgRestoreOptionArgs() {
        List<String> args = new ArrayList<>();
        switch (formatCombo.getValue()) {
            case "Custom" -> args.add("--format=custom");
            case "Directory" -> args.add("--format=directory");
            case "Tar" -> args.add("--format=tar");
            default -> { /* "Auto" — pg_restore detects the format itself */ }
        }
        for (String schema : splitList(schemasField.getText())) args.add("--schema=" + schema);
        for (String table : splitList(tablesField.getText())) args.add("--table=" + table);
        if (dropBeforeCreate.isSelected()) args.add("--clean");
        if (createDatabase.isSelected()) args.add("--create");
        if (useIfExists.isSelected()) args.add("--if-exists");
        if (dataOnly.isSelected()) args.add("--data-only");
        if (singleTxPgRestore.isSelected()) args.add("--single-transaction");
        return args;
    }

    private static List<String> splitList(String csv) {
        List<String> out = new ArrayList<>();
        if (csv == null || csv.isBlank()) return out;
        for (String part : csv.split(",")) {
            String trimmed = part.strip();
            if (!trimmed.isEmpty()) out.add(trimmed);
        }
        return out;
    }

    private void refreshPreview() {
        List<String> preview = new ArrayList<>();
        if (tool == Tool.PG_RESTORE) {
            preview.addAll(buildPgRestoreOptionArgs());
            preview.add("--dbname=" + databaseField.getText());
            preview.add("\"" + dumpPathField.getText() + "\"");
        } else {
            preview.add(databaseField.getText());
            preview.add("--file=\"" + dumpPathField.getText() + "\"");
            if (singleTxPsql.isSelected()) preview.add("--single-transaction");
        }
        commandPreview.setText(String.join(" ", preview));
    }

    // ------------------------------------------------------------- run

    private void runRestore() {
        if (pathField.getText().isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Enter the path to the executable.").showAndWait();
            return;
        }
        if (dumpPathField.getText().isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Choose a dump file to restore.").showAndWait();
            return;
        }

        String database = databaseField.getText();
        String dumpPath = dumpPathField.getText();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Restore \"" + new File(dumpPath).getName() + "\" into database \"" + database
                        + "\"?\nExisting objects with the same names may be overwritten or conflict.",
                ButtonType.YES, ButtonType.NO);
        confirm.initOwner(stage);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        List<String> command = new ArrayList<>();
        command.add(pathField.getText());

        String toolName;
        if (tool == Tool.PG_RESTORE) {
            toolName = "pg_restore";
            command.add("--host");
            command.add(profile.getHost());
            command.add("--port");
            command.add(String.valueOf(profile.getPort()));
            command.add("--username");
            command.add(profile.getUsername());
            command.add("--no-password");
            command.addAll(buildPgRestoreOptionArgs());
            command.add("--dbname=" + database);
            command.add(dumpPath);   // pg_restore reads the archive as a positional argument
        } else {
            toolName = "psql";
            command.add("--file=" + dumpPath);
            if (singleTxPsql.isSelected()) command.add("--single-transaction");
            command.add("--username");
            command.add(profile.getUsername());
            command.add("--host");
            command.add(profile.getHost());
            command.add("--port");
            command.add(String.valueOf(profile.getPort()));
            command.add(database);   // psql takes the target database as a positional argument
        }

        stage.close();
        String breadcrumb = "Database  ›  " + profile.getName() + "  ›  " + database;
        String taskLabel = "Restoring with " + toolName + "… (" + profile.getName() + ")";
        DumpRestoreService.runProcess(mainWindow, toolName + " (" + profile.getName() + ")",
                command, profile, null, breadcrumb, taskLabel);
    }
}
