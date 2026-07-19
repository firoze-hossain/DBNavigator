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
        stage.setResizable(false);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        grid.setPadding(new Insets(16, 20, 8, 20));

        Map<String, TextField> fields = new LinkedHashMap<>();
        int row = 0;
        for (SqlParameters.Parameter param : params) {
            Label nameLabel = new Label(param.name());
            nameLabel.getStyleClass().add("connection-field-label");
            nameLabel.setMinWidth(140);

            TextField field = new TextField();
            field.setPrefWidth(260);
            String hint = param.guessedColumn() != null ? param.guessedColumn() + "  <null>" : "<null>";
            field.setPromptText(hint);
            fields.put(param.name(), field);

            grid.add(nameLabel, 0, row);
            grid.add(field, 1, row);
            row++;
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(Math.min(280, params.size() * 42 + 20));
        scroll.getStyleClass().add("diagram-scroll");

        AtomicReference<Map<String, String>> result = new AtomicReference<>();

        Button closeButton = new Button("Close");
        Button executeButton = new Button("Execute");
        executeButton.getStyleClass().add("run-button");
        executeButton.setDefaultButton(true);

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
        HBox buttons = new HBox(10, spacer, closeButton, executeButton);
        buttons.setPadding(new Insets(8, 20, 16, 20));
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(scroll, buttons);
        root.getStyleClass().add("app-root");
        Scene scene = new Scene(root, 440, Math.min(360, params.size() * 46 + 100));
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
