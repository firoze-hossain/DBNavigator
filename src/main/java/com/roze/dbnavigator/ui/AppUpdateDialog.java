package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.update.AppUpdate;
import com.roze.dbnavigator.update.AppUpdateService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.nio.file.Path;

public final class AppUpdateDialog {
    private AppUpdateDialog() {}

    public static void check(Window owner, boolean silentIfCurrent) {
        AppUpdateService.checkForUpdate().whenComplete((update, error) -> Platform.runLater(() -> {
            if (error != null) {
                show(owner, null, error, false, false);
            } else if (update == null || !update.available || update.release == null) {
                if (!silentIfCurrent) show(owner, update, null, false, false);
            } else {
                show(owner, update, null, AppSettingsAutoDownload(), false);
            }
        }));
    }

    public static void show(Window owner, AppUpdate update, Throwable error, boolean autoDownload, boolean silentIfCurrent) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("DBNavigator Updates");
        dialog.setMinWidth(560);

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        Label title = new Label("DBNavigator Updates");
        title.getStyleClass().add("panel-header");
        Label status = new Label();
        status.setWrapText(true);
        TextArea notes = new TextArea();
        notes.setEditable(false);
        notes.setWrapText(true);
        notes.setPrefRowCount(8);
        notes.setVisible(false);
        notes.setManaged(false);
        ProgressBar progress = new ProgressBar(0);
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.setVisible(false);
        progress.setManaged(false);

        Button close = new Button("Close");
        Button download = new Button("Download update");
        Button install = new Button("Install & Restart");
        download.setDisable(true);
        install.setDisable(true);
        install.setVisible(false);
        install.setManaged(false);

        final AppUpdate.Release[] releaseHolder = new AppUpdate.Release[1];
        final Path[] fileHolder = new Path[1];

        download.setOnAction(e -> {
            AppUpdate.Release release = releaseHolder[0];
            if (release == null) return;
            download.setDisable(true);
            progress.setVisible(true);
            progress.setManaged(true);
            status.setText("Downloading " + release.fileName + "…");
            AppUpdateService.download(release, value -> Platform.runLater(() -> progress.setProgress(value)))
                    .whenComplete((file, downloadError) -> Platform.runLater(() -> {
                        if (downloadError != null) {
                            status.setText("Download failed: " + rootMessage(downloadError));
                            download.setDisable(false);
                            return;
                        }
                        fileHolder[0] = file;
                        status.setText("Download complete and SHA-256 verified.");
                        install.setVisible(true);
                        install.setManaged(true);
                        install.setDisable(false);
                    }));
        });

        install.setOnAction(e -> {
            try {
                AppUpdateService.installAndRestart(fileHolder[0]);
                dialog.close();
                Platform.exit();
            } catch (Exception ex) {
                status.setText("Could not start the installer: " + ex.getMessage());
            }
        });
        close.setOnAction(e -> dialog.close());

        HBox buttons = new HBox(10, new Region(), download, install, close);
        HBox.setHgrow(buttons.getChildren().get(0), Priority.ALWAYS);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        root.getChildren().addAll(title, status, notes, progress, buttons);
        Scene scene = new Scene(root, 620, 420);
        if (owner != null && owner.getScene() != null) scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        dialog.setScene(scene);

        if (error != null) {
            title.setText("Update check failed");
            status.setText(rootMessage(error));
            dialog.show();
            return;
        }

        if (update == null || !update.available || update.release == null) {
            title.setText("You're up to date");
            status.setText("DBNavigator Pro " + AppUpdateService.currentVersion() + " is the latest published version for " + AppUpdateService.platform() + " / " + AppUpdateService.architecture() + ".");
            if (!silentIfCurrent) dialog.show();
            return;
        }

        AppUpdate.Release release = update.release;
        releaseHolder[0] = release;
        title.setText("DBNavigator Pro " + release.version + " is available");
        status.setText("Current version: " + AppUpdateService.currentVersion() + "  •  " + release.fileName + (release.mandatory ? "  •  Mandatory update" : ""));
        notes.setText(release.displayNotes());
        notes.setVisible(true);
        notes.setManaged(true);
        dialog.show();

        if (release.mandatory) close.setDisable(true);

        if (autoDownload) {
            download.fire();
        } else {
            download.setDisable(false);
        }
    }

    private static boolean AppSettingsAutoDownload() {
        return com.roze.dbnavigator.db.AppSettingsStore.load().isAutoDownloadUpdates();
    }

    private static String rootMessage(Throwable error) {
        Throwable t = error;
        while (t.getCause() != null && (t.getMessage() == null || t.getMessage().isBlank())) t = t.getCause();
        return t.getMessage() == null ? t.toString() : t.getMessage();
    }
}
