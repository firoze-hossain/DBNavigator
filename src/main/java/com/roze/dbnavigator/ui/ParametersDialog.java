package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.util.SqlParameters;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * DataGrip-style "Parameters" dialog: one row per distinct {@code :name}
 * placeholder found in the query, a text field for its value (with the
 * best-effort guessed associated column shown as gray placeholder text,
 * exactly like the reference — it naturally disappears once you type),
 * then Close/Execute. Returns empty if closed/cancelled.
 */
public final class ParametersDialog {

    private ParametersDialog() {}

    public static Optional<Map<String, String>> show(Window owner, List<SqlParameters.Parameter> params) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Parameters");
        stage.setResizable(true);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        grid.setPadding(new Insets(16, 20, 10, 20));

        Map<String, TextField> fields = new LinkedHashMap<>();
        int row = 0;
        for (SqlParameters.Parameter param : params) {
            Label nameLabel = new Label(param.name());
            nameLabel.getStyleClass().add("connection-field-label");
            nameLabel.setStyle("-fx-font-family: 'DBNavigator Mono', monospace; -fx-font-weight: bold; -fx-font-size: 13px;");
            nameLabel.setMinWidth(110);

            Label snippetLabel = new Label();
            snippetLabel.setStyle("-fx-font-family: 'DBNavigator Mono', monospace; -fx-font-size: 12px; -fx-text-fill: -fg-muted;");
            if (param.contextSnippet() != null && !param.contextSnippet().isBlank()) {
                snippetLabel.setText(param.contextSnippet());
            } else if (param.guessedColumn() != null) {
                snippetLabel.setText(param.guessedColumn());
            }
            snippetLabel.setMinWidth(200);

            TextField field = new TextField();
            field.setPrefWidth(220);
            field.setPromptText("<null>");
            fields.put(param.name(), field);

            grid.add(nameLabel, 0, row);
            grid.add(snippetLabel, 1, row);
            grid.add(field, 2, row);
            GridPane.setHgrow(field, Priority.ALWAYS);
            row++;
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(Math.min(320, params.size() * 44 + 24));
        scroll.getStyleClass().add("diagram-scroll");

        AtomicReference<Map<String, String>> result = new AtomicReference<>();

        Button executeButton = new Button("Execute");
        executeButton.getStyleClass().add("run-button");
        executeButton.setDefaultButton(true);

        Button closeButton = new Button("Close");

        Runnable onExecute = () -> {
            Map<String, String> values = new LinkedHashMap<>();
            fields.forEach((name, field) -> values.put(name, field.getText()));
            result.set(values);
            stage.close();
        };
        executeButton.setOnAction(e -> onExecute.run());
        closeButton.setOnAction(e -> stage.close());
        stage.setOnCloseRequest(e -> result.set(null));

        // Enter in any field runs Execute, matching a typical "fill the form, hit Enter" flow
        for (TextField field : fields.values()) {
            field.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) onExecute.run();
                else if (e.getCode() == KeyCode.ESCAPE) stage.close();
            });
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10, spacer, executeButton, closeButton);
        buttons.setPadding(new Insets(10, 20, 16, 20));
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(scroll, buttons);
        root.getStyleClass().add("app-root");
        Scene scene = new Scene(root, 600, Math.min(380, params.size() * 46 + 110));
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);

        if (!fields.isEmpty()) {
            javafx.application.Platform.runLater(() -> fields.values().iterator().next().requestFocus());
        }

        stage.showAndWait();
        return Optional.ofNullable(result.get());
    }
}
