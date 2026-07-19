package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Dump a database to .sql and restore it, using the native client tools:
 * PostgreSQL → pg_dump / psql, MySQL & MariaDB → mysqldump / mysql.
 * The tools must be installed and on PATH (or give the full path in the dialog).
 *
 * Output streams into the app's docked Run panel (like DataGrip's Run tool
 * window) instead of a separate popup, with a matching status-bar progress
 * indicator while the process is alive.
 */
public final class DumpRestoreService {

    private DumpRestoreService() {}

    // ------------------------------------------------------------- dump

    public static void dumpDatabase(MainWindow mainWindow, ConnectionProfile profile, String database) {
        String defaultTool = switch (profile.getType()) {
            case POSTGRESQL -> "pg_dump";
            case MYSQL, MARIADB -> "mysqldump";
            default -> null;
        };
        if (defaultTool == null) {
            info(mainWindow.getOwnerWindow(), "Dump is supported for PostgreSQL, MySQL and MariaDB connections.");
            return;
        }

        Window owner = mainWindow.getOwnerWindow();
        var tool = toolPathDialog(owner, defaultTool,
                "Dump " + database, "Path to " + defaultTool + " (keep default if it is on PATH):").showAndWait();
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
        runProcess(mainWindow, defaultTool + " (" + profile.getName() + ")", command, profile, null,
                breadcrumb(profile, database), "Dumping with " + defaultTool + "… (" + profile.getName() + ")");
    }

    // ---------------------------------------------------------- restore

    public static void restoreDatabase(MainWindow mainWindow, ConnectionProfile profile, String database) {
        String defaultTool = switch (profile.getType()) {
            case POSTGRESQL -> "psql";
            case MYSQL, MARIADB -> "mysql";
            default -> null;
        };
        if (defaultTool == null) {
            info(mainWindow.getOwnerWindow(), "Restore is supported for PostgreSQL, MySQL and MariaDB connections.");
            return;
        }

        Window owner = mainWindow.getOwnerWindow();
        var tool = toolPathDialog(owner, defaultTool,
                "Restore into " + database, "Path to " + defaultTool + " (keep default if it is on PATH):").showAndWait();
        if (tool.isEmpty()) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose .sql dump to restore");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL files", "*.sql"));
        File file = chooser.showOpenDialog(owner);
        if (file == null) return;

        Alert confirm = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.CONFIRMATION,
                "Restore \"" + file.getName() + "\" into database \"" + database
                        + "\"?\nExisting objects with the same names may be overwritten or conflict.",
                ButtonType.YES, ButtonType.NO));
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
        runProcess(mainWindow, defaultTool + " (" + profile.getName() + ")", command, profile, stdinFile,
                breadcrumb(profile, database), "Restoring with " + defaultTool + "… (" + profile.getName() + ")");
    }

    // ----------------------------------------------------------- process

    private static String breadcrumb(ConnectionProfile profile, String database) {
        return "Database  ›  " + profile.getName() + "  ›  " + database;
    }

    /**
     * Runs a command with live streaming output in the docked Run panel, and
     * shows a matching progress indicator in the status bar. Package-visible
     * so richer dialogs (e.g. PgDumpDialog) can reuse the same execution path
     * instead of duplicating it.
     */
    static void runProcess(MainWindow mainWindow, String tabTitle, List<String> command,
                           ConnectionProfile profile, File stdinFile, String breadcrumb, String taskLabel) {
        RunPanel.RunHandle handle = mainWindow.getRunPanel().startRun(tabTitle);
        mainWindow.showRunPanel();

        Runnable execution = () -> execute(mainWindow, handle, command, profile, stdinFile, breadcrumb, taskLabel);
        handle.setRerunAction(execution);
        execution.run();
    }

    private static void execute(MainWindow mainWindow, RunPanel.RunHandle handle, List<String> command,
                                ConnectionProfile profile, File stdinFile, String breadcrumb, String taskLabel) {
        handle.appendLine("$ " + String.join(" ", command));
        AtomicReference<Process> processHolder = new AtomicReference<>();
        mainWindow.showTask(breadcrumb, taskLabel, () -> {
            Process p = processHolder.get();
            if (p != null && p.isAlive()) {
                p.destroy();
                handle.appendLine("[cancelled by user]");
            }
        });

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
                processHolder.set(process);
                handle.setProcess(process);

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        handle.appendLine(line);
                    }
                }
                int exit = process.waitFor();
                handle.appendLine(exit == 0 ? "✓ Finished successfully" : "✗ Exited with code " + exit);
                handle.markFinished(exit);
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                handle.markFailed(msg + "  — is the client tool installed and on PATH? "
                        + "On Windows, point the dialog at e.g. "
                        + "C:\\Program Files\\PostgreSQL\\16\\bin\\pg_dump.exe");
            } finally {
                Platform.runLater(mainWindow::hideTask);
            }
        });
    }

    private static TextInputDialog toolPathDialog(Window owner, String defaultTool,
                                                  String title, String prompt) {
        TextInputDialog dialog = (TextInputDialog) DialogTheme.apply(new TextInputDialog(defaultTool));
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(prompt);
        dialog.getDialogPane().setPrefWidth(520);
        return dialog;
    }

    private static void info(Window owner, String message) {
        Alert alert = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION, message));
        alert.initOwner(owner);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
