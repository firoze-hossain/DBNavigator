package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Dump a database to .sql and restore it, using the native client tools:
 * PostgreSQL → pg_dump / psql, MySQL & MariaDB → mysqldump / mysql.
 * The tools must be installed and on PATH (or give the full path in the dialog).
 */
public final class DumpRestoreService {

    private DumpRestoreService() {}

    // ------------------------------------------------------------- dump

    public static void dumpDatabase(Window owner, ConnectionProfile profile, String database) {
        String defaultTool = switch (profile.getType()) {
            case POSTGRESQL -> "pg_dump";
            case MYSQL, MARIADB -> "mysqldump";
            default -> null;
        };
        if (defaultTool == null) {
            info(owner, "Dump is supported for PostgreSQL, MySQL and MariaDB connections.");
            return;
        }

        TextInputDialog toolDialog = toolPathDialog(owner, defaultTool,
                "Dump " + database, "Path to " + defaultTool + " (keep default if it is on PATH):");
        var tool = toolDialog.showAndWait();
        if (tool.isEmpty()) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save dump as…");
        chooser.setInitialFileName(database + ".sql");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL files", "*.sql"));
        File file = chooser.showSaveDialog(owner);
        if (file == null) return;

        List<String> command = new ArrayList<>();
        if (profile.getType() == DatabaseType.POSTGRESQL) {
            command.addAll(List.of(tool.get(),
                    "--host", profile.getHost(),
                    "--port", String.valueOf(profile.getPort()),
                    "--username", profile.getUsername(),
                    "--dbname", database,
                    "--no-password",
                    "--file", file.getAbsolutePath()));
        } else {
            command.addAll(List.of(tool.get(),
                    "-h", profile.getHost(),
                    "-P", String.valueOf(profile.getPort()),
                    "-u", profile.getUsername(),
                    "--result-file=" + file.getAbsolutePath(),
                    database));
        }
        runProcess(owner, "Dumping " + database + " → " + file.getName(), command, profile, null);
    }

    // ---------------------------------------------------------- restore

    public static void restoreDatabase(Window owner, ConnectionProfile profile, String database) {
        String defaultTool = switch (profile.getType()) {
            case POSTGRESQL -> "psql";
            case MYSQL, MARIADB -> "mysql";
            default -> null;
        };
        if (defaultTool == null) {
            info(owner, "Restore is supported for PostgreSQL, MySQL and MariaDB connections.");
            return;
        }

        TextInputDialog toolDialog = toolPathDialog(owner, defaultTool,
                "Restore into " + database, "Path to " + defaultTool + " (keep default if it is on PATH):");
        var tool = toolDialog.showAndWait();
        if (tool.isEmpty()) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose .sql dump to restore");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL files", "*.sql"));
        File file = chooser.showOpenDialog(owner);
        if (file == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Restore \"" + file.getName() + "\" into database \"" + database
                        + "\"?\nExisting objects with the same names may be overwritten or conflict.",
                ButtonType.YES, ButtonType.NO);
        confirm.initOwner(owner);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        List<String> command = new ArrayList<>();
        File stdinFile = null;
        if (profile.getType() == DatabaseType.POSTGRESQL) {
            command.addAll(List.of(tool.get(),
                    "--host", profile.getHost(),
                    "--port", String.valueOf(profile.getPort()),
                    "--username", profile.getUsername(),
                    "--dbname", database,
                    "--no-password",
                    "--file", file.getAbsolutePath()));
        } else {
            command.addAll(List.of(tool.get(),
                    "-h", profile.getHost(),
                    "-P", String.valueOf(profile.getPort()),
                    "-u", profile.getUsername(),
                    database));
            stdinFile = file;   // mysql reads the script from stdin
        }
        runProcess(owner, "Restoring " + file.getName() + " → " + database, command, profile, stdinFile);
    }

    // ----------------------------------------------------------- process

    private static void runProcess(Window owner, String title, List<String> command,
                                   ConnectionProfile profile, File stdinFile) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.NONE);
        stage.setTitle(title);

        TextArea output = new TextArea();
        output.setEditable(false);
        output.getStyleClass().add("process-output");
        VBox.setVgrow(output, Priority.ALWAYS);

        Label status = new Label("Running…");
        Button closeButton = new Button("Close");
        closeButton.setDisable(true);
        closeButton.setOnAction(e -> stage.close());
        HBox bottom = new HBox(10, status, closeButton);
        bottom.setPadding(new Insets(8));

        VBox root = new VBox(output, bottom);
        Scene scene = new Scene(root, 720, 420);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
        stage.show();

        appendLine(output, "$ " + String.join(" ", command));

        AppExecutor.run(() -> {
            try {
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.redirectErrorStream(true);
                if (stdinFile != null) builder.redirectInput(stdinFile);

                // Pass the password through the environment — never on the command line
                String password = profile.getPassword() == null ? "" : profile.getPassword();
                if (profile.getType() == DatabaseType.POSTGRESQL) {
                    builder.environment().put("PGPASSWORD", password);
                } else {
                    builder.environment().put("MYSQL_PWD", password);
                }

                Process process = builder.start();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        appendLine(output, line);
                    }
                }
                int exit = process.waitFor();
                Platform.runLater(() -> {
                    status.setText(exit == 0 ? "✓ Finished successfully" : "✗ Exited with code " + exit);
                    status.setStyle(exit == 0 ? "-fx-text-fill: #57965c;" : "-fx-text-fill: #e05555;");
                    closeButton.setDisable(false);
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                appendLine(output, "ERROR: " + msg);
                appendLine(output, "Is the client tool installed and on PATH? "
                        + "On Windows, point the dialog at e.g. "
                        + "C:\\Program Files\\PostgreSQL\\16\\bin\\pg_dump.exe");
                Platform.runLater(() -> {
                    status.setText("✗ Failed to start process");
                    status.setStyle("-fx-text-fill: #e05555;");
                    closeButton.setDisable(false);
                });
            }
        });
    }

    private static void appendLine(TextArea area, String line) {
        Platform.runLater(() -> area.appendText(line + System.lineSeparator()));
    }

    private static TextInputDialog toolPathDialog(Window owner, String defaultTool,
                                                  String title, String prompt) {
        TextInputDialog dialog = new TextInputDialog(defaultTool);
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(prompt);
        dialog.getDialogPane().setPrefWidth(520);
        return dialog;
    }

    private static void info(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.initOwner(owner);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
