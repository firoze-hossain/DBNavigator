package com.roze.dbnavigator.ui.languages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Modal dialog for adding or editing an ignored schema or DTD URI.
 * Matches DataGrip screenshot media_1791719471783_711d8da4.png.
 */
public class IgnoredResourceDialog extends Stage {

    private final TextField uriField;
    private String result = null;

    public IgnoredResourceDialog(Window owner, String initialUri) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("External Resource");
        setResizable(false);

        uriField = new TextField(initialUri != null ? initialUri : "");
        uriField.setPromptText("URI (e.g. http://example.com/ns)");
        uriField.setPrefWidth(380);
        uriField.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        HBox.setHgrow(uriField, Priority.ALWAYS);

        Label uriLabel = new Label("URI:");
        uriLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-min-width: 40px;");

        HBox formRow = new HBox(8, uriLabel, uriField);
        formRow.setAlignment(Pos.CENTER_LEFT);

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setStyle("-fx-background-color: #3574f0; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 75px; -fx-background-radius: 4;");
        okBtn.setOnAction(e -> {
            String val = uriField.getText().trim();
            if (val.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Please enter a URI to ignore.", ButtonType.OK);
                alert.initOwner(this);
                alert.showAndWait();
                return;
            }
            result = val;
            close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-min-width: 75px;");
        cancelBtn.setOnAction(e -> close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttonBar = new HBox(10, spacer, okBtn, cancelBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));

        VBox root = new VBox(14, formRow, buttonBar);
        root.setPadding(new Insets(16, 18, 16, 18));
        root.setStyle("-fx-background-color: #2b2d30;");

        Scene scene = new Scene(root, 440, 120);
        setScene(scene);
    }

    public static Optional<String> showDialog(Window owner, String initialUri) {
        IgnoredResourceDialog dialog = new IgnoredResourceDialog(owner, initialUri);
        dialog.showAndWait();
        return Optional.ofNullable(dialog.result);
    }
}
