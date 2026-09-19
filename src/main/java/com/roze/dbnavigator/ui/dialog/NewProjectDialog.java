package com.roze.dbnavigator.ui.dialog;

import com.roze.dbnavigator.db.ProjectStore;
import com.roze.dbnavigator.model.Project;
import com.roze.dbnavigator.ui.DialogTheme;
import com.roze.dbnavigator.ui.Icons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;
import java.nio.file.Path;

/**
 * DataGrip-aligned "New Project" dialog:
 * Prompts user for project name or directory path, with directory chooser browse button.
 */
public class NewProjectDialog {

    private final Window owner;
    private Project createdProject = null;

    public NewProjectDialog(Window owner) {
        this.owner = owner;
    }

    public Project showAndWait() {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("New Project");
        stage.setResizable(false);

        // Circular Question Mark Icon
        StackPane iconPane = new StackPane();
        Circle circle = new Circle(14, Color.web("#3574F0"));
        Label qMark = new Label("?");
        qMark.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        iconPane.getChildren().addAll(circle, qMark);
        iconPane.setAlignment(Pos.TOP_LEFT);
        iconPane.setPadding(new Insets(2, 6, 0, 0));

        // Prompt & Input
        Label promptLabel = new Label("Enter new project name or path:");
        promptLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        String defaultDirPath = Path.of(System.getProperty("user.home"), "DBNavigatorProjects", "untitled").toString();
        TextField pathField = new TextField(defaultDirPath);
        pathField.setPrefWidth(340);
        pathField.setStyle("-fx-font-size: 13px;");

        Button browseBtn = new Button();
        browseBtn.setGraphic(Icons.of(FontAwesomeSolid.FOLDER_OPEN, "#a9b7c6", 13));
        browseBtn.setTooltip(new Tooltip("Browse directory…"));
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Project Directory");
            File initialDir = new File(pathField.getText().trim());
            if (initialDir.exists() && initialDir.isDirectory()) {
                dc.setInitialDirectory(initialDir);
            } else if (initialDir.getParentFile() != null && initialDir.getParentFile().isDirectory()) {
                dc.setInitialDirectory(initialDir.getParentFile());
            } else {
                dc.setInitialDirectory(new File(System.getProperty("user.home")));
            }
            File chosen = dc.showDialog(stage);
            if (chosen != null) {
                pathField.setText(chosen.getAbsolutePath());
            }
        });

        HBox inputRow = new HBox(6, pathField, browseBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(pathField, Priority.ALWAYS);

        VBox contentBox = new VBox(8, promptLabel, inputRow);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        HBox topBox = new HBox(12, iconPane, contentBox);
        topBox.setAlignment(Pos.TOP_LEFT);

        // Buttons: OK and Cancel
        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 75px;");
        okBtn.setOnAction(e -> {
            String input = pathField.getText().trim();
            if (!input.isBlank()) {
                createdProject = ProjectStore.createOrOpenProject(input);
                stage.close();
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle("-fx-min-width: 75px;");
        cancelBtn.setOnAction(e -> stage.close());

        HBox buttonBar = new HBox(10, okBtn, cancelBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(12, 0, 0, 0));

        VBox root = new VBox(16, topBox, buttonBar);
        root.setPadding(new Insets(16, 18, 16, 18));
        root.setStyle("-fx-background-color: #1e1f22;");
        root.getStyleClass().add("app-root");

        Scene scene = new Scene(root, 480, 150);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
        stage.showAndWait();

        return createdProject;
    }
}
