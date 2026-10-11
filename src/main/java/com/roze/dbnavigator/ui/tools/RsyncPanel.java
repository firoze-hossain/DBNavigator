package com.roze.dbnavigator.ui.tools;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Settings panel for Tools > Rsync.
 * Matches DataGrip screenshot media_1791727596382_682ee432.png.
 */
public class RsyncPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public RsyncPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.RsyncConfig cfg = settings.getRsync();

        VBox content = new VBox(16);
        content.setPadding(new Insets(12, 16, 16, 16));
        content.setStyle("-fx-background-color: transparent;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setAlignment(Pos.TOP_LEFT);

        // 1. Rsync executable path
        Label rsyncLabel = new Label("Rsync executable path:");
        rsyncLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        TextField rsyncPathField = new TextField(cfg.getRsyncExecutablePath());
        rsyncPathField.setPrefWidth(350);
        rsyncPathField.setStyle("-fx-background-color: -control-bg; -fx-text-fill: -text; -fx-border-color: -border-color; -fx-border-radius: 4px;");
        inputs.put("tools_rsync_rsyncExecutablePath", rsyncPathField);

        Button rsyncBrowseBtn = new Button("📁");
        rsyncBrowseBtn.setTooltip(new Tooltip("Browse for rsync executable"));
        rsyncBrowseBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Rsync Executable");
            File file = chooser.showOpenDialog(rsyncBrowseBtn.getScene().getWindow());
            if (file != null) {
                rsyncPathField.setText(file.getAbsolutePath());
            }
        });

        HBox rsyncBox = new HBox(6, rsyncPathField, rsyncBrowseBtn);
        HBox.setHgrow(rsyncPathField, Priority.ALWAYS);

        grid.add(rsyncLabel, 0, 0);
        grid.add(rsyncBox, 1, 0);

        // 2. Rsync options
        Label optionsLabel = new Label("Rsync options:");
        optionsLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        TextField optionsField = new TextField(cfg.getRsyncOptions());
        optionsField.setPrefWidth(350);
        optionsField.setStyle("-fx-background-color: -control-bg; -fx-text-fill: -text; -fx-border-color: -border-color; -fx-border-radius: 4px;");
        inputs.put("tools_rsync_rsyncOptions", optionsField);

        Button expandBtn = new Button("⤢");
        expandBtn.setTooltip(new Tooltip("Edit Rsync options in separate window"));
        expandBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(optionsField.getText());
            dialog.setTitle("Rsync Options");
            dialog.setHeaderText("Specify command-line flags passed to rsync:");
            dialog.setContentText("Options:");
            dialog.showAndWait().ifPresent(optionsField::setText);
        });

        Hyperlink restoreDefaultsLink = new Hyperlink("Restore Defaults");
        restoreDefaultsLink.setStyle("-fx-text-fill: -accent; -fx-underline: false; -fx-font-size: 12px;");
        restoreDefaultsLink.setOnAction(e -> optionsField.setText("-zar"));

        HBox optionsControlsBox = new HBox(8, optionsField, expandBtn, restoreDefaultsLink);
        optionsControlsBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(optionsField, Priority.ALWAYS);

        Hyperlink optionsDocLink = new Hyperlink("Rsync Options List ↗");
        optionsDocLink.setStyle("-fx-text-fill: -accent; -fx-underline: false; -fx-font-size: 12px;");
        optionsDocLink.setOnAction(e -> {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Rsync Options Reference");
            info.setHeaderText("Common Rsync Command Line Options");
            info.setContentText("-z, --compress: Compress file data during the transfer\n"
                    + "-a, --archive: Archive mode; equals -rlptgoD (preserves attributes)\n"
                    + "-r, --recursive: Recurse into directories\n"
                    + "-v, --verbose: Increase verbosity\n"
                    + "-u, --update: Skip files that are newer on receiver\n"
                    + "-P: Equivalent to --partial --progress\n"
                    + "-e: Specify the remote shell to use (e.g. ssh)");
            info.showAndWait();
        });

        VBox optionsContainer = new VBox(4, optionsControlsBox, optionsDocLink);

        grid.add(optionsLabel, 0, 1);
        grid.add(optionsContainer, 1, 1);

        // 3. Shell executable path
        Label shellLabel = new Label("Shell executable path:");
        shellLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        TextField shellPathField = new TextField(cfg.getShellExecutablePath());
        shellPathField.setPrefWidth(350);
        shellPathField.setStyle("-fx-background-color: -control-bg; -fx-text-fill: -text; -fx-border-color: -border-color; -fx-border-radius: 4px;");
        inputs.put("tools_rsync_shellExecutablePath", shellPathField);

        Button shellBrowseBtn = new Button("📁");
        shellBrowseBtn.setTooltip(new Tooltip("Browse for SSH executable"));
        shellBrowseBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Shell / SSH Executable");
            File file = chooser.showOpenDialog(shellBrowseBtn.getScene().getWindow());
            if (file != null) {
                shellPathField.setText(file.getAbsolutePath());
            }
        });

        HBox shellBox = new HBox(6, shellPathField, shellBrowseBtn);
        HBox.setHgrow(shellPathField, Priority.ALWAYS);

        Label shellNote = new Label("Rsync will use this executable when connecting via SSH");
        shellNote.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");

        VBox shellContainer = new VBox(4, shellBox, shellNote);

        grid.add(shellLabel, 0, 2);
        grid.add(shellContainer, 1, 2);

        // 4. Test Connection button
        Button testConnBtn = new Button("Test Connection");
        testConnBtn.setStyle("-fx-font-size: 12px; -fx-padding: 5 14 5 14;");
        testConnBtn.setOnAction(e -> testExecutables(rsyncPathField.getText().trim(), shellPathField.getText().trim()));

        HBox buttonBar = new HBox(testConnBtn);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));

        content.getChildren().addAll(grid, buttonBar);
        return content;
    }

    private void testExecutables(String rsyncPath, String shellPath) {
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            boolean rsyncOk = false;
            boolean shellOk = false;

            // Test rsync
            try {
                Process p = new ProcessBuilder(rsyncPath.isBlank() ? "rsync" : rsyncPath, "--version").start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null) {
                        sb.append("Rsync: ").append(line).append("\n");
                        rsyncOk = true;
                    }
                }
                p.waitFor();
            } catch (Exception ex) {
                sb.append("Rsync failed: ").append(ex.getMessage()).append("\n");
            }

            // Test shell/ssh
            try {
                Process p = new ProcessBuilder(shellPath.isBlank() ? "ssh" : shellPath, "-V").redirectErrorStream(true).start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null) {
                        sb.append("Shell (SSH): ").append(line).append("\n");
                        shellOk = true;
                    }
                }
                p.waitFor();
            } catch (Exception ex) {
                sb.append("Shell failed: ").append(ex.getMessage()).append("\n");
            }

            final boolean success = rsyncOk && shellOk;
            final String message = sb.toString();

            Platform.runLater(() -> {
                Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
                alert.setTitle("Rsync & Shell Connection Test");
                alert.setHeaderText(success ? "Connection Test Successful" : "Executable Verification Warning");
                alert.setContentText(message.isBlank() ? "No output received." : message);
                alert.showAndWait();
            });
        }).start();
    }
}
