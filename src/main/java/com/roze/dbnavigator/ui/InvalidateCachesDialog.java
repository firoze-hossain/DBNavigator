package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.LocalHistoryStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * "Invalidate Caches" dialog, matching the reference IDE's wording and
 * layout. Honest simplification: "VCS Log caches" and "embedded browser
 * cache" don't apply to this app (no VCS integration, no embedded browser),
 * so those two checkboxes are shown for visual parity but disabled.
 *
 * Restart is deliberately NOT automated. An earlier version tried to
 * relaunch the same java command and then call Platform.exit() — but a
 * modular JavaFX app launched via an IDE run configuration (IntelliJ, in
 * particular) often can't be faithfully re-invoked from inside itself: the
 * classpath/module-path the IDE built isn't fully visible to the running
 * process, so the relaunch attempt would silently fail after the app had
 * already exited, leaving nothing running. Rather than risk closing the app
 * with no way back, both buttons here clear caches (if selected) and then
 * simply tell the person to restart manually — the app is never force-closed.
 */
public final class InvalidateCachesDialog {

    private InvalidateCachesDialog() {}

    public static void show(Window owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Invalidate Caches");

        Label message = new Label(
                "Remove caches and indexes for all projects. New caches will be built "
                + "when you reopen the projects.");
        message.setWrapText(true);

        Label optionalLabel = new Label("Optional:");
        optionalLabel.getStyleClass().add("panel-header");

        CheckBox clearFsCache = new CheckBox("Clear file system cache and Local History");
        CheckBox clearVcsLog = new CheckBox("Clear VCS Log caches and indexes");
        clearVcsLog.setDisable(true);
        clearVcsLog.setTooltip(new Tooltip("Not applicable — this app has no VCS integration"));
        CheckBox clearBrowserCache = new CheckBox("Delete embedded browser engine cache and cookies");
        clearBrowserCache.setDisable(true);
        clearBrowserCache.setTooltip(new Tooltip("Not applicable — this app has no embedded browser"));
        Label browserHint = new Label(
                "Affects components that use an embedded browser to render HTML-based content and web pages.");
        browserHint.getStyleClass().add("console-status");
        browserHint.setWrapText(true);

        VBox checks = new VBox(8, clearFsCache, clearVcsLog, clearBrowserCache, browserHint);
        checks.setPadding(new Insets(4, 0, 0, 0));

        VBox content = new VBox(14, message, optionalLabel, checks);
        content.setPadding(new Insets(20));

        Button justRestart = new Button("Just restart");
        justRestart.getStyleClass().add("link-button");
        justRestart.setOnAction(e -> {
            stage.close();
            tellPersonToRestart(owner, List.of());
        });

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> stage.close());

        Button invalidateAndRestart = new Button("Invalidate and Restart");
        invalidateAndRestart.getStyleClass().add("run-button");
        invalidateAndRestart.setDefaultButton(true);
        invalidateAndRestart.setOnAction(e -> {
            stage.close();
            List<String> cleared = new ArrayList<>();
            if (clearFsCache.isSelected()) {
                LocalHistoryStore.clearAll();
                CompletionService.clearAllCaches();
                cleared.add("file system cache and Local History");
            }
            tellPersonToRestart(owner, cleared);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10, justRestart, spacer, cancel, invalidateAndRestart);
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.setPadding(new Insets(0, 20, 16, 20));

        VBox root = new VBox(content, buttons);
        root.getStyleClass().add("app-root");
        Scene scene = new Scene(root, 560, 320);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Never closes the app automatically — see the class javadoc for why an
     * automated relaunch isn't safe to attempt here.
     */
    private static void tellPersonToRestart(Window owner, List<String> clearedItems) {
        Alert alert = (Alert) DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION));
        alert.initOwner(owner);
        alert.setHeaderText("Restart needed");
        String message = clearedItems.isEmpty()
                ? "Please restart DBNavigator Pro manually (e.g. rerun it from IntelliJ) "
                  + "for this to take effect."
                : "Cleared: " + String.join(", ", clearedItems) + ".\n\n"
                  + "Please restart DBNavigator Pro manually (e.g. rerun it from IntelliJ) "
                  + "to finish.";
        alert.setContentText(message);
        alert.showAndWait();
    }
}
