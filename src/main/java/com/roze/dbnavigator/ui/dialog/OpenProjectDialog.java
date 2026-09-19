package com.roze.dbnavigator.ui.dialog;

import com.roze.dbnavigator.model.Project;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * DataGrip-aligned "Open Project" switching dialog (matches screenshot 5):
 * Offers options to open in 'This Window', 'New Window', or 'Attach', with 'Don't ask again' checkbox.
 */
public class OpenProjectDialog {

    public enum OpenMode {
        THIS_WINDOW,
        NEW_WINDOW,
        ATTACH,
        CANCEL
    }

    public static class Result {
        private final OpenMode mode;
        private final boolean dontAskAgain;

        public Result(OpenMode mode, boolean dontAskAgain) {
            this.mode = mode;
            this.dontAskAgain = dontAskAgain;
        }

        public OpenMode getMode() { return mode; }
        public boolean isDontAskAgain() { return dontAskAgain; }
    }

    private final Window owner;
    private final Project project;
    private OpenMode selectedMode = OpenMode.CANCEL;

    public OpenProjectDialog(Window owner, Project project) {
        this.owner = owner;
        this.project = project;
    }

    public Result showAndWait() {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Open Project");
        stage.setResizable(false);

        // Circular Question Mark Icon
        StackPane iconPane = new StackPane();
        Circle circle = new Circle(14, Color.web("#3574F0"));
        Label qMark = new Label("?");
        qMark.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        iconPane.getChildren().addAll(circle, qMark);
        iconPane.setAlignment(Pos.TOP_LEFT);
        iconPane.setPadding(new Insets(2, 6, 0, 0));

        // Header & Description
        Label header = new Label("Open Project");
        header.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: -text;");

        String msg = "Projects can either be opened in a new window, or replace the project in the current window, or be attached to the already opened projects.\nHow would you like to open the project?";
        Label desc = new Label(msg);
        desc.setWrapText(true);
        desc.setMaxWidth(440);
        desc.setStyle("-fx-text-fill: -text; -fx-font-size: 12px; -fx-line-spacing: 2;");

        CheckBox dontAskCheck = new CheckBox("Don't ask again");
        dontAskCheck.setStyle("-fx-font-size: 12px; -fx-padding: 6 0 0 0;");

        VBox contentBox = new VBox(8, header, desc, dontAskCheck);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        HBox topBox = new HBox(14, iconPane, contentBox);
        topBox.setAlignment(Pos.TOP_LEFT);

        // Action Buttons: [ This Window ] [ New Window ] [ Attach ] [ Cancel ]
        Button thisWindowBtn = new Button("This Window");
        thisWindowBtn.setDefaultButton(true);
        thisWindowBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 95px;");
        thisWindowBtn.setOnAction(e -> {
            selectedMode = OpenMode.THIS_WINDOW;
            stage.close();
        });

        Button newWindowBtn = new Button("New Window");
        newWindowBtn.setStyle("-fx-min-width: 95px;");
        newWindowBtn.setOnAction(e -> {
            selectedMode = OpenMode.NEW_WINDOW;
            stage.close();
        });

        Button attachBtn = new Button("Attach");
        attachBtn.setStyle("-fx-min-width: 75px;");
        attachBtn.setOnAction(e -> {
            selectedMode = OpenMode.ATTACH;
            stage.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle("-fx-min-width: 75px;");
        cancelBtn.setOnAction(e -> {
            selectedMode = OpenMode.CANCEL;
            stage.close();
        });

        HBox buttonBar = new HBox(8, thisWindowBtn, newWindowBtn, attachBtn, cancelBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(14, 0, 0, 0));

        VBox root = new VBox(16, topBox, buttonBar);
        root.setPadding(new Insets(16, 18, 16, 18));
        root.setStyle("-fx-background-color: #1e1f22;");
        root.getStyleClass().add("app-root");

        Scene scene = new Scene(root, 520, 200);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
        stage.showAndWait();

        return new Result(selectedMode, dontAskCheck.isSelected());
    }
}
