package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * DataGrip-style "Export with mysqldump…" dialog: full option grid with a
 * live-updating command preview, mirroring the reference dialog's layout
 * and default checkbox states exactly.
 */
public final class MySqlDumpDialog {

    private static final String PLACEHOLDER_HINT =
            "Allowed substitution patterns: {timestamp}, {data_source}, {database}";

    private final MainWindow mainWindow;
    private final ConnectionProfile profile;
    private final String database;
    private final Stage stage = new Stage();

    private final TextField pathField = new TextField("mysqldump");
    private final TextField outputField = new TextField();
    private final TextField databasesField = new TextField();
    private final TextField tablesField = new TextField();

    private final CheckBox addDropTable = new CheckBox("Add DROP TABLE before CREATE TABLE");
    private final CheckBox addDisableKeys = new CheckBox("Add DISABLE KEYS before each INSERT");
    private final CheckBox addLockTables = new CheckBox("Add LOCK TABLES before each table dump");
    private final CheckBox addDropTrigger = new CheckBox("Add DROP TRIGGER before CREATE TRIGGER");
    private final CheckBox schemaWithoutData = new CheckBox("Export schema without data");
    private final CheckBox schemaWithoutTablespaces = new CheckBox("Export schema without tablespaces");
    private final CheckBox withoutTableCreation = new CheckBox("Export without table creation");
    private final CheckBox completeInsert = new CheckBox("Include column names in each INSERT");
    private final CheckBox createOptions = new CheckBox("Include all table options in CREATE TABLE");
    private final CheckBox routines = new CheckBox("Include stored routines in the dump");
    private final CheckBox lockAllTables = new CheckBox("Lock all tables for the duration of export");
    private final CheckBox delayedInsert = new CheckBox("Use INSERT DELAYED (up to MySQL 5.5)");
    private final CheckBox extendedInsert = new CheckBox("Use single INSERT for multiple rows");

    private final TextArea commandPreview = new TextArea();

    private MySqlDumpDialog(MainWindow mainWindow, ConnectionProfile profile, String database) {
        this.mainWindow = mainWindow;
        this.profile = profile;
        this.database = database;
        buildUi(mainWindow.getOwnerWindow());
        wireLivePreview();
        refreshPreview();
    }

    public static void show(MainWindow mainWindow, ConnectionProfile profile, String database) {
        new MySqlDumpDialog(mainWindow, profile, database).stage.showAndWait();
    }

    // --------------------------------------------------------------- UI

    private void buildUi(Window owner) {
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Export with mysqldump… (" + profile.getName() + ")");
        stage.setMinWidth(700);

        RadioButton locallyRadio = new RadioButton("Locally");
        locallyRadio.setSelected(true);
        locallyRadio.setToggleGroup(new ToggleGroup());
        Tooltip.install(locallyRadio, new Tooltip("Running mysqldump over an SSH tunnel isn't supported yet"));

        pathField.setPrefWidth(420);
        Button browsePath = browseButton();
        browsePath.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Locate mysqldump executable");
            File file = chooser.showOpenDialog(stage);
            if (file != null) pathField.setText(file.getAbsolutePath());
        });

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

        databasesField.setText(database);
        databasesField.setPrefWidth(420);
        tablesField.setPromptText("(all tables)");
        tablesField.setPrefWidth(420);

        // Defaults matching the reference dialog exactly
        addDropTable.setSelected(true);
        addDisableKeys.setSelected(true);
        addLockTables.setSelected(true);
        createOptions.setSelected(true);
        lockAllTables.setSelected(true);
        extendedInsert.setSelected(true);

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

        form.add(label("Databases to dump:"), 0, row);
        form.add(databasesField, 1, row++);
        form.add(label("Tables to dump:"), 0, row);
        form.add(tablesField, 1, row++);

        GridPane checkGrid = new GridPane();
        checkGrid.setHgap(28);
        checkGrid.setVgap(8);
        checkGrid.add(addDropTable, 0, 0);
        checkGrid.add(completeInsert, 1, 0);
        checkGrid.add(addDisableKeys, 0, 1);
        checkGrid.add(createOptions, 1, 1);
        checkGrid.add(addLockTables, 0, 2);
        checkGrid.add(routines, 1, 2);
        checkGrid.add(addDropTrigger, 0, 3);
        checkGrid.add(lockAllTables, 1, 3);
        checkGrid.add(schemaWithoutData, 0, 4);
        checkGrid.add(delayedInsert, 1, 4);
        checkGrid.add(schemaWithoutTablespaces, 0, 5);
        checkGrid.add(extendedInsert, 1, 5);
        checkGrid.add(withoutTableCreation, 0, 6);

        form.add(checkGrid, 0, row, 2, 1);
        row++;

        commandPreview.setEditable(false);
        commandPreview.setWrapText(true);
        commandPreview.setPrefRowCount(4);
        commandPreview.getStyleClass().add("process-output");

        VBox previewBox = new VBox(4, commandPreview);
        previewBox.setPadding(new Insets(4, 20, 0, 20));
        VBox.setVgrow(commandPreview, Priority.ALWAYS);

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

        Scene scene = new Scene(root, 720, 640);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
    }

    private static Label label(String text) {
        Label l = new Label(text);
        l.setMinWidth(140);
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
        for (TextField f : List.of(pathField, outputField, databasesField, tablesField)) {
            f.textProperty().addListener((obs, o, n) -> refreshPreview());
        }
        for (CheckBox c : List.of(addDropTable, addDisableKeys, addLockTables, addDropTrigger,
                schemaWithoutData, schemaWithoutTablespaces, withoutTableCreation, completeInsert,
                createOptions, routines, lockAllTables, delayedInsert, extendedInsert)) {
            c.selectedProperty().addListener((obs, o, n) -> refreshPreview());
        }
    }

    /** Builds the option flags (used for both the preview text and the real run). */
    private List<String> buildOptionArgs() {
        List<String> args = new ArrayList<>();

        args.add(addDropTable.isSelected() ? "--add-drop-table" : "--skip-add-drop-table");
        args.add(addDisableKeys.isSelected() ? "--disable-keys" : "--skip-disable-keys");
        args.add(addLockTables.isSelected() ? "--lock-tables" : "--skip-lock-tables");
        if (addDropTrigger.isSelected()) args.add("--add-drop-trigger");
        if (schemaWithoutData.isSelected()) args.add("--no-data");
        if (schemaWithoutTablespaces.isSelected()) args.add("--no-tablespaces");
        if (withoutTableCreation.isSelected()) args.add("--no-create-info");
        if (completeInsert.isSelected()) args.add("--complete-insert");
        if (createOptions.isSelected()) args.add("--create-options"); else args.add("--skip-create-options");
        if (routines.isSelected()) args.add("--routines");
        if (lockAllTables.isSelected()) args.add("--lock-all-tables");
        if (delayedInsert.isSelected()) args.add("--delayed-insert");
        args.add(extendedInsert.isSelected() ? "--extended-insert" : "--skip-extended-insert");

        for (String table : splitList(tablesField.getText())) {
            args.add(table);
        }
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
        List<String> args = new ArrayList<>();
        args.add(databasesField.getText());
        args.addAll(buildOptionArgs());
        args.add("--result-file=\"" + outputField.getText() + "\"");
        commandPreview.setText(String.join(" ", args));
    }

    // ------------------------------------------------------------- run

    private void runDump() {
        if (pathField.getText().isBlank()) {
            DialogTheme.apply(new Alert(Alert.AlertType.WARNING, "Enter the path to the mysqldump executable."))
                    .showAndWait();
            return;
        }
        if (outputField.getText().isBlank()) {
            DialogTheme.apply(new Alert(Alert.AlertType.WARNING, "Enter an output file path.")).showAndWait();
            return;
        }

        String resolvedOutput = substitutePlaceholders(outputField.getText());

        List<String> command = new ArrayList<>();
        command.add(pathField.getText());
        command.add("-h");
        command.add(profile.getHost());
        command.add("-P");
        command.add(String.valueOf(profile.getPort()));
        command.add("-u");
        command.add(profile.getUsername());
        command.add("--result-file=" + resolvedOutput);
        command.add(databasesField.getText());
        command.addAll(buildOptionArgs());

        stage.close();
        String breadcrumb = "Database  \u203a  " + profile.getName() + "  \u203a  " + databasesField.getText();
        String taskLabel = "Dumping with mysqldump\u2026 (" + profile.getName() + ")";
        DumpRestoreService.runProcess(mainWindow, "mysqldump (" + profile.getName() + ")",
                command, profile, null, breadcrumb, taskLabel);
    }

    private String substitutePlaceholders(String path) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String dataSource = profile.getName().replaceAll("[^A-Za-z0-9_-]", "_");
        return path
                .replace("{timestamp}", timestamp)
                .replace("{data_source}", dataSource)
                .replace("{database}", databasesField.getText());
    }
}
