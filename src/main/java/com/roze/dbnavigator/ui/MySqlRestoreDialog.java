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
 * DataGrip-style "Restore with mysql…" dialog: matches the reference's
 * simpler layout (no options grid, just executable/database/dump path and
 * a live command preview) since {@code mysql} restoring a plain dump has
 * far fewer meaningful switches than {@code mysqldump} exporting one.
 */
public final class MySqlRestoreDialog {

    private final MainWindow mainWindow;
    private final ConnectionProfile profile;
    private final Stage stage = new Stage();

    private final TextField pathField = new TextField("mysql");
    private final TextField databaseField = new TextField();
    private final TextField dumpPathField = new TextField();
    private final TextArea commandPreview = new TextArea();

    private MySqlRestoreDialog(MainWindow mainWindow, ConnectionProfile profile, String database) {
        this.mainWindow = mainWindow;
        this.profile = profile;
        buildUi(mainWindow.getOwnerWindow(), database);
        wireLivePreview();
        refreshPreview();
    }

    public static void show(MainWindow mainWindow, ConnectionProfile profile, String database) {
        new MySqlRestoreDialog(mainWindow, profile, database).stage.showAndWait();
    }

    private void buildUi(Window owner, String database) {
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Restore with mysql… (" + profile.getName() + ")");
        stage.setMinWidth(680);

        RadioButton locallyRadio = new RadioButton("Locally");
        locallyRadio.setSelected(true);
        locallyRadio.setToggleGroup(new ToggleGroup());
        Tooltip.install(locallyRadio, new Tooltip("Running mysql over an SSH tunnel isn't supported yet"));

        pathField.setPrefWidth(420);
        Button browsePath = browseButton();
        browsePath.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Locate mysql executable");
            File file = chooser.showOpenDialog(stage);
            if (file != null) pathField.setText(file.getAbsolutePath());
        });

        databaseField.setText(database);
        databaseField.setPrefWidth(420);

        dumpPathField.setPrefWidth(420);
        Button browseDump = browseButton();
        browseDump.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose .sql dump to restore");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL files", "*.sql"));
            File file = chooser.showOpenDialog(stage);
            if (file != null) dumpPathField.setText(file.getAbsolutePath());
        });

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

        form.add(new Separator(), 0, row, 2, 1);
        row++;
        form.add(optionsHeader, 0, row, 2, 1);
        row++;

        form.add(label("Database:"), 0, row);
        form.add(databaseField, 1, row++);
        form.add(label("Path to dump:"), 0, row);
        form.add(fieldWithBrowse(dumpPathField, browseDump), 1, row++);

        commandPreview.setEditable(false);
        commandPreview.setWrapText(true);
        commandPreview.setPrefRowCount(3);
        commandPreview.getStyleClass().add("process-output");

        VBox previewBox = new VBox(4, commandPreview);
        previewBox.setPadding(new Insets(4, 20, 0, 20));
        VBox.setVgrow(commandPreview, Priority.ALWAYS);

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

        VBox root = new VBox(form, previewBox, buttons);
        VBox.setVgrow(previewBox, Priority.ALWAYS);
        root.getStyleClass().add("app-root");

        Scene scene = new Scene(root, 700, 460);
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

    private void wireLivePreview() {
        for (TextField f : List.of(pathField, databaseField, dumpPathField)) {
            f.textProperty().addListener((obs, o, n) -> refreshPreview());
        }
    }

    private void refreshPreview() {
        commandPreview.setText("--database=" + databaseField.getText()
                + " < \"" + dumpPathField.getText() + "\"");
    }

    private void runRestore() {
        if (pathField.getText().isBlank()) {
            DialogTheme.apply(new Alert(Alert.AlertType.WARNING, "Enter the path to the mysql executable."))
                    .showAndWait();
            return;
        }
        if (dumpPathField.getText().isBlank()) {
            DialogTheme.apply(new Alert(Alert.AlertType.WARNING, "Choose a dump file to restore.")).showAndWait();
            return;
        }
        File dumpFile = new File(dumpPathField.getText());

        Alert confirm = DialogTheme.apply(new Alert(Alert.AlertType.CONFIRMATION,
                "Restore \"" + dumpFile.getName() + "\" into database \"" + databaseField.getText()
                        + "\"?\nExisting objects with the same names may be overwritten or conflict.",
                ButtonType.YES, ButtonType.NO));
        confirm.initOwner(stage);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        List<String> command = new ArrayList<>();
        command.add(pathField.getText());
        command.add("-h");
        command.add(profile.getHost());
        command.add("-P");
        command.add(String.valueOf(profile.getPort()));
        command.add("-u");
        command.add(profile.getUsername());
        command.add(databaseField.getText());

        stage.close();
        String breadcrumb = "Database  \u203a  " + profile.getName() + "  \u203a  " + databaseField.getText();
        String taskLabel = "Restoring with mysql\u2026 (" + profile.getName() + ")";
        DumpRestoreService.runProcess(mainWindow, "mysql (" + profile.getName() + ")",
                command, profile, dumpFile, breadcrumb, taskLabel);
    }
}
