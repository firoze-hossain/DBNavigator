package com.roze.dbnavigator.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * DataGrip-style "Confirm Exit" modal dialog matching the reference IDE.
 * Displays when exiting the application (via menu or window close button),
 * prompting the user with "Are you sure you want to exit?" and a "Don't ask again" option.
 */
public final class ConfirmExitDialog {

    public record Result(boolean confirmed, boolean dontAskAgain) {
        public boolean isConfirmed() { return confirmed; }
        public boolean isDontAskAgain() { return dontAskAgain; }
    }

    private static double xOffset = 0;
    private static double yOffset = 0;

    private ConfirmExitDialog() {}

    public static Result show(Window owner) {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            return new Result(true, false);
        }
        if (!javafx.application.Platform.isFxApplicationThread()) {
            final Result[] res = new Result[1];
            try {
                java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                javafx.application.Platform.runLater(() -> {
                    try {
                        res[0] = show(owner);
                    } finally {
                        latch.countDown();
                    }
                });
                latch.await();
                return res[0] != null ? res[0] : new Result(false, false);
            } catch (Exception ex) {
                return new Result(false, false);
            }
        }

        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("Confirm Exit");

        // 1. Title bar header: Blue ? badge, "Confirm Exit", and close button
        Circle badgeBg = new Circle(9, Color.web("#388bfd"));
        Label badgeQuestion = new Label("?");
        badgeQuestion.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
        StackPane iconBadge = new StackPane(badgeBg, badgeQuestion);
        iconBadge.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Confirm Exit");
        titleLabel.setStyle("-fx-text-fill: #dfe1e5; -fx-font-size: 13px; -fx-font-weight: bold;");

        HBox titleGroup = new HBox(8, iconBadge, titleLabel);
        titleGroup.setAlignment(Pos.CENTER_LEFT);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868a91; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 0 4 0 4;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #dfe1e5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 0 4 0 4;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868a91; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 0 4 0 4;"));

        HBox header = new HBox(8, titleGroup, headerSpacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 14, 6, 14));

        // Enable dragging the dialog by the header
        header.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        header.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        // 2. Content: "Are you sure you want to exit?" & "Don't ask again"
        Label messageLabel = new Label("Are you sure you want to exit?");
        messageLabel.setStyle("-fx-text-fill: #dfe1e5; -fx-font-size: 13px;");

        CheckBox dontAskAgain = new CheckBox("Don't ask again");
        dontAskAgain.setStyle("-fx-text-fill: #dfe1e5; -fx-font-size: 12px;");

        VBox contentBox = new VBox(10, messageLabel, dontAskAgain);
        contentBox.setPadding(new Insets(10, 16, 14, 38)); // Indented to align with header title

        // 3. Buttons: [ Exit ]   [ Cancel ]
        final Result[] dialogResult = new Result[]{new Result(false, false)};

        Button exitBtn = new Button("Exit");
        exitBtn.setDefaultButton(true);
        exitBtn.setStyle("-fx-background-color: #388bfd; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 4; -fx-padding: 6 22; -fx-cursor: hand;");
        exitBtn.setOnMouseEntered(e -> exitBtn.setStyle("-fx-background-color: #4796ff; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 4; -fx-padding: 6 22; -fx-cursor: hand;"));
        exitBtn.setOnMouseExited(e -> exitBtn.setStyle("-fx-background-color: #388bfd; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 4; -fx-padding: 6 22; -fx-cursor: hand;"));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle("-fx-background-color: #393b40; -fx-text-fill: #dfe1e5; -fx-font-size: 12px; -fx-background-radius: 4; -fx-border-color: #4e5157; -fx-border-radius: 4; -fx-padding: 6 18; -fx-cursor: hand;");
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle("-fx-background-color: #43454a; -fx-text-fill: #dfe1e5; -fx-font-size: 12px; -fx-background-radius: 4; -fx-border-color: #5c6168; -fx-border-radius: 4; -fx-padding: 6 18; -fx-cursor: hand;"));
        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle("-fx-background-color: #393b40; -fx-text-fill: #dfe1e5; -fx-font-size: 12px; -fx-background-radius: 4; -fx-border-color: #4e5157; -fx-border-radius: 4; -fx-padding: 6 18; -fx-cursor: hand;"));

        exitBtn.setOnAction(e -> {
            dialogResult[0] = new Result(true, dontAskAgain.isSelected());
            stage.close();
        });

        cancelBtn.setOnAction(e -> {
            dialogResult[0] = new Result(false, false);
            stage.close();
        });

        closeBtn.setOnAction(e -> {
            dialogResult[0] = new Result(false, false);
            stage.close();
        });

        HBox buttonBar = new HBox(12, exitBtn, cancelBtn);
        buttonBar.setAlignment(Pos.CENTER);
        buttonBar.setPadding(new Insets(6, 16, 16, 16));

        // 4. Root layout with rounded dark theme border and shadow
        VBox root = new VBox(header, contentBox, buttonBar);
        root.setStyle(
                "-fx-background-color: #2b2d30; " +
                "-fx-background-radius: 8; " +
                "-fx-border-color: #43454a; " +
                "-fx-border-radius: 8; " +
                "-fx-border-width: 1; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 18, 0, 0, 6);"
        );
        root.setMinWidth(380);
        root.setMaxWidth(420);

        StackPane container = new StackPane(root);
        container.setPadding(new Insets(10)); // Breathing room for drop shadow
        container.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(container);
        scene.setFill(Color.TRANSPARENT);
        DialogTheme.applyToScene(scene);

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                dialogResult[0] = new Result(false, false);
                stage.close();
            }
        });

        stage.setScene(scene);

        if (owner != null && owner.isShowing()) {
            stage.setOnShown(e -> {
                double x = owner.getX() + (owner.getWidth() - stage.getWidth()) / 2;
                double y = owner.getY() + (owner.getHeight() - stage.getHeight()) / 2;
                stage.setX(x);
                stage.setY(y);
            });
        }

        stage.showAndWait();
        return dialogResult[0];
    }
}
