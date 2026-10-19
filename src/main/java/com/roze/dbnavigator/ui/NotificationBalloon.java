package com.roze.dbnavigator.ui;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import com.roze.dbnavigator.ui.Icons;
import com.roze.dbnavigator.update.AppUpdate;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.HashSet;
import java.util.Set;

/**
 * DataGrip-style floating notification balloon for updates and system alerts.
 * Floats gracefully at the bottom-right corner of the IDE window.
 */
public final class NotificationBalloon {

    private static final Set<String> ignoredVersions = new HashSet<>();
    private static Popup currentPopup;

    private NotificationBalloon() {}

    public static void showUpdateNotification(Stage stage, AppUpdate update, MainWindow mainWindow) {
        if (stage == null || update == null || !update.available || update.release == null) return;
        String version = update.latestVersion != null ? update.latestVersion : update.release.version;
        if (version == null || ignoredVersions.contains(version)) return;

        Platform.runLater(() -> {
            if (currentPopup != null && currentPopup.isShowing()) {
                currentPopup.hide();
            }

            Popup popup = new Popup();
            popup.setAutoHide(false);

            VBox card = new VBox(8);
            card.getStyleClass().add("notification-balloon-card");
            card.setMinWidth(380);
            card.setMaxWidth(440);
            card.setPadding(new Insets(12, 14, 12, 14));

            // Header row: icon + title + spacer + close button
            Label icon = new Label();
            icon.setGraphic(Icons.of(FontAwesomeSolid.DOWNLOAD, "#3574f0", 14));

            Label title = new Label("DBNavigator Pro " + version + " is available");
            title.getStyleClass().add("notification-balloon-title");

            Button closeBtn = new Button("✕");
            closeBtn.getStyleClass().add("notification-balloon-close");
            closeBtn.setOnAction(e -> {
                ignoredVersions.add(version);
                popup.hide();
            });

            HBox header = new HBox(8, icon, title, new Region(), closeBtn);
            HBox.setHgrow(header.getChildren().get(2), Priority.ALWAYS);
            header.setAlignment(Pos.CENTER_LEFT);

            // Body description
            String sourceDesc = "github".equalsIgnoreCase(update.source) ? "GitHub Releases" : "RozeHub Website";
            Label desc = new Label("A new version is available for download (" + sourceDesc + ").");
            desc.getStyleClass().add("notification-balloon-desc");
            desc.setWrapText(true);

            // Action buttons row: Download & Install (primary), What's New, Ignore
            Button downloadBtn = new Button("Download and Install");
            downloadBtn.getStyleClass().add("notification-balloon-action-btn");
            downloadBtn.setOnAction(e -> {
                popup.hide();
                AppUpdateDialog.show(stage, update, null, true, false);
            });

            Hyperlink whatsNew = new Hyperlink("What's New");
            whatsNew.getStyleClass().add("notification-balloon-link");
            whatsNew.setOnAction(e -> {
                popup.hide();
                AppUpdateDialog.show(stage, update, null, false, false);
            });

            Hyperlink ignoreLink = new Hyperlink("Ignore");
            ignoreLink.getStyleClass().add("notification-balloon-link-dim");
            ignoreLink.setOnAction(e -> {
                ignoredVersions.add(version);
                popup.hide();
            });

            HBox actions = new HBox(10, downloadBtn, whatsNew, ignoreLink);
            actions.setAlignment(Pos.CENTER_LEFT);

            card.getChildren().addAll(header, desc, actions);
            popup.getContent().add(card);
            currentPopup = popup;

            // Apply active scene stylesheets
            if (stage.getScene() != null) {
                card.getStylesheets().addAll(stage.getScene().getStylesheets());
            }

            // Calculate bottom-right positioning relative to IDE window
            double x = stage.getX() + stage.getWidth() - 460;
            double y = stage.getY() + stage.getHeight() - 140;
            popup.show(stage, Math.max(10, x), Math.max(10, y));

            // Smooth fade-in
            FadeTransition ft = new FadeTransition(Duration.millis(300), card);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();

            // Auto-hide after 35 seconds unless hovered
            PauseTransition pause = new PauseTransition(Duration.seconds(35));
            pause.setOnFinished(e -> {
                if (!card.isHover()) {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(300), card);
                    fadeOut.setFromValue(1.0);
                    fadeOut.setToValue(0.0);
                    fadeOut.setOnFinished(fe -> popup.hide());
                    fadeOut.play();
                }
            });
            pause.play();
            card.setOnMouseEntered(e -> pause.stop());
        });
    }

    public static void hide() {
        if (currentPopup != null && currentPopup.isShowing()) {
            currentPopup.hide();
        }
    }
}
