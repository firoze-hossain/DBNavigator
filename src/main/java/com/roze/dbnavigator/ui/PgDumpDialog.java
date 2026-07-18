package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * DataGrip-style "Export with pg_dump…" dialog: full option set (statements
 * style, schema/table filters, format, DROP/CREATE/data-only switches) with a
 * live-updating command preview, mirroring the real IDE dialog layout.
 */
public final class PgDumpDialog {

    private static final String PLACEHOLDER_HINT =
            "Allowed substitution patterns: {timestamp}, {data_source}, {database}";

    private final ConnectionProfile profile;
    private final String database;
    private final Stage stage = new Stage();

    private final TextField pathField = new TextField("pg_dump");
    private final TextField outputField = new TextField();
    private final ComboBox<String> statementsCombo = new ComboBox<>();
    private final TextField databaseField = new TextField();
    private final TextField schemasField = new TextField();
    private final TextField tablesField = new TextField();
    private final ComboBox<String> formatCombo = new ComboBox<>();
    private final CheckBox dropBeforeCreate = new CheckBox("Add DROP before CREATE");
    private final CheckBox createDatabase = new CheckBox("Add CREATE DATABASE and reconnect to the created one");
    private final CheckBox useIfExists = new CheckBox("Use DROP … IF EXISTS before CREATE");
    private final CheckBox dataOnly = new CheckBox("Export data without schema");
    private final TextArea commandPreview = new TextArea();

    private PgDumpDialog(Window owner, ConnectionProfile profile, String database) {
        this.profile = profile;
        this.database = database;
        buildUi(owner);
        wireLivePreview();
        refreshPreview();
    }

    public static void show(Window owner, ConnectionProfile profile, String database) {
        new PgDumpDialog(owner, profile, database).stage.showAndWait();
    }

    // --------------------------------------------------------------- UI

    private void buildUi(Window owner) {
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Export with pg_dump… (" + profile.getName() + ")");
        stage.setMinWidth(680);

        // ---- Where to run ----
        RadioButton locallyRadio = new RadioButton("Locally");
        locallyRadio.setSelected(true);
        locallyRadio.setToggleGroup(new ToggleGroup());
        Tooltip.install(locallyRadio, new Tooltip("Running pg_dump over an SSH tunnel isn't supported yet"));

        // ---- Path to executable ----
        pathField.setPrefWidth(420);
        Button browsePath = browseButton();
        browsePath.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Locate pg_dump executable");
            File file = chooser.showOpenDialog(stage);
            if (file != null) pathField.setText(file.getAbsolutePath());
        });

        // ---- Output result to ----
        outputField.setText(defaultOutputPath());
        outputField.setPrefWidth(420);
        Button browseOutput = browseButton();
        browseOutput.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Choose output folder");
            File dir = chooser.showDialog(stage);
            if (dir != null) {
                outputField.setText(new File(dir, "{data_source}-{timestamp}-dump.sql").getAbsolutePath());
            }
        });

        Label hint = new Label(PLACEHOLDER_HINT);
        hint.getStyleClass().add("console-status");

        // ---- Options ----
        statementsCombo.getItems().addAll("Copy", "Insert", "Insert (with column names)");
        statementsCombo.getSelectionModel().selectFirst();
        statementsCombo.setPrefWidth(260);

        databaseField.setText(database);
        databaseField.setPrefWidth(260);

        schemasField.setPromptText("(all schemas)");
        schemasField.setPrefWidth(260);

        tablesField.setPromptText("(all tables)");
        tablesField.setPrefWidth(260);

        formatCombo.getItems().addAll("File", "Directory", "Custom", "Tar");
        formatCombo.getSelectionModel().selectFirst();
        formatCombo.setPrefWidth(260);

        // "IF EXISTS" only makes sense together with "DROP before CREATE"
        useIfExists.setDisable(true);
        dropBeforeCreate.selectedProperty().addListener((obs, was, checked) -> {
            useIfExists.setDisable(!checked);
            if (!checked) useIfExists.setSelected(false);
        });

        GridPane checkGrid = new GridPane();
        checkGrid.setHgap(24);
        checkGrid.setVgap(8);
        checkGrid.add(dropBeforeCreate, 0, 0);
        checkGrid.add(createDatabase, 1, 0);
        checkGrid.add(useIfExists, 0, 1);
        checkGrid.add(dataOnly, 1, 1);

        Label optionsHeader = new Label("Options");
        optionsHeader.getStyleClass().add("panel-header");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.setPadding(new Insets(16, 20, 8, 20));

        int row = 0;
        form.add(label("Where to run:"), 0, row);
        form.add(locallyRadio, 1, row++);

        form.add(label("Path to executable:"), 0, row);
        form.add(fieldWithBrowse(pathField, browsePath), 1, row++);

        form.add(label("Output result to:"), 0, row);
        form.add(fieldWithBrowse(outputField, browseOutput), 1, row++);

        form.add(new Region(), 0, row);
        form.add(hint, 1, row++);

        form.add(new Separator(), 0, row, 2, 1);
        row++;
        form.add(optionsHeader, 0, row, 2, 1);
        row++;

        form.add(label("Statements:"), 0, row);
        form.add(statementsCombo, 1, row++);

        form.add(label("Database:"), 0, row);
        form.add(databaseField, 1, row++);

        form.add(label("Schemas:"), 0, row);
        form.add(schemasField, 1, row++);

        form.add(label("Tables to dump:"), 0, row);
        form.add(tablesField, 1, row++);

        form.add(label("Format:"), 0, row);
        form.add(formatCombo, 1, row++);

        form.add(checkGrid, 0, row, 2, 1);
        row++;

        // ---- Command preview ----
        commandPreview.setEditable(false);
        commandPreview.setWrapText(true);
        commandPreview.setPrefRowCount(4);
        commandPreview.getStyleClass().add("process-output");

        VBox previewBox = new VBox(4, commandPreview);
        previewBox.setPadding(new Insets(4, 20, 0, 20));
        VBox.setVgrow(commandPreview, Priority.ALWAYS);

        // ---- Buttons ----
        Button runButton = new Button("Run");
        runButton.getStyleClass().add("run-button");
        runButton.setDefaultButton(true);
        runButton.setOnAction(e -> runDump());

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> stage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10, spacer, runButton, cancelButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(12, 20, 16, 20));

        VBox root = new VBox(form, previewBox, buttons);
        VBox.setVgrow(previewBox, Priority.ALWAYS);
        root.getStyleClass().add("app-root");

        Scene scene = new Scene(root, 700, 620);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
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

    private String defaultOutputPath() {
        String home = System.getProperty("user.home", "");
        return new File(home, "{data_source}-{timestamp}-dump.sql").getAbsolutePath();
    }

    // ------------------------------------------------------- live preview

    private void wireLivePreview() {
        for (TextField f : List.of(pathField, outputField, databaseField, schemasField, tablesField)) {
            f.textProperty().addListener((obs, o, n) -> refreshPreview());
        }
        statementsCombo.valueProperty().addListener((obs, o, n) -> refreshPreview());
        formatCombo.valueProperty().addListener((obs, o, n) -> refreshPreview());
        for (CheckBox c : List.of(dropBeforeCreate, createDatabase, useIfExists, dataOnly)) {
            c.selectedProperty().addListener((obs, o, n) -> refreshPreview());
        }
    }

    /** Builds the option flags (used for both the preview text and the real run). */
    private List<String> buildOptionArgs() {
        List<String> args = new ArrayList<>();

        switch (formatCombo.getValue()) {
            case "Directory" -> args.add("--format=directory");
            case "Custom" -> args.add("--format=custom");
            case "Tar" -> args.add("--format=tar");
            default -> { /* "File" = plain text, pg_dump's default — no flag needed */ }
        }

        switch (statementsCombo.getValue()) {
            case "Insert" -> args.add("--inserts");
            case "Insert (with column names)" -> args.add("--column-inserts");
            default -> { /* "Copy" is pg_dump's default */ }
        }

        for (String schema : splitList(schemasField.getText())) {
            args.add("--schema=" + schema);
        }
        for (String table : splitList(tablesField.getText())) {
            args.add("--table=" + table);
        }

        if (dropBeforeCreate.isSelected()) args.add("--clean");
        if (createDatabase.isSelected()) args.add("--create");
        if (useIfExists.isSelected()) args.add("--if-exists");
        if (dataOnly.isSelected()) args.add("--data-only");

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
        List<String> args = new ArrayList<>(buildOptionArgs());
        args.add("--dbname=" + databaseField.getText());
        args.add("--file=\"" + outputField.getText() + "\"");
        commandPreview.setText(String.join(" ", args));
    }

    // ------------------------------------------------------------- run

    private void runDump() {
        if (pathField.getText().isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Enter the path to the pg_dump executable.").showAndWait();
            return;
        }
        if (outputField.getText().isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Enter an output file path.").showAndWait();
            return;
        }

        String resolvedOutput = substitutePlaceholders(outputField.getText());

        List<String> command = new ArrayList<>();
        command.add(pathField.getText());
        command.add("--host");
        command.add(profile.getHost());
        command.add("--port");
        command.add(String.valueOf(profile.getPort()));
        command.add("--username");
        command.add(profile.getUsername());
        command.add("--no-password");
        command.addAll(buildOptionArgs());
        command.add("--dbname=" + databaseField.getText());
        command.add("--file=" + resolvedOutput);

        stage.close();
        DumpRestoreService.runProcess(stage.getOwner(),
                "Dumping " + databaseField.getText() + " → " + new File(resolvedOutput).getName(),
                command, profile, null);
    }

    private String substitutePlaceholders(String path) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String dataSource = profile.getName().replaceAll("[^A-Za-z0-9_-]", "_");
        return path
                .replace("{timestamp}", timestamp)
                .replace("{data_source}", dataSource)
                .replace("{database}", databaseField.getText());
    }
}
