package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ParameterHistoryStore;
import com.roze.dbnavigator.util.SqlParameters;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
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

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setHgrow(Priority.NEVER);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.NEVER);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(col0, col1, col2);

        Map<String, TextField> fields = new LinkedHashMap<>();
        int row = 0;
        for (SqlParameters.Parameter param : params) {
            Label nameLabel = new Label(param.name());
            nameLabel.getStyleClass().add("parameter-name-label");
            nameLabel.setStyle("-fx-font-family: 'DBNavigator Mono', monospace; -fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -text;");
            nameLabel.setMinWidth(Region.USE_PREF_SIZE);
            nameLabel.setMaxWidth(Double.MAX_VALUE);

            Label snippetLabel = new Label();
            snippetLabel.getStyleClass().add("parameter-snippet-label");
            snippetLabel.setStyle("-fx-font-family: 'DBNavigator Mono', monospace; -fx-font-size: 12px; -fx-text-fill: -text-dim;");
            if (param.contextSnippet() != null && !param.contextSnippet().isBlank()) {
                snippetLabel.setText(param.contextSnippet());
            } else if (param.guessedColumn() != null) {
                snippetLabel.setText(param.guessedColumn());
            }
            snippetLabel.setMinWidth(Region.USE_PREF_SIZE);
            snippetLabel.setMaxWidth(160);
            snippetLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);

            TextField field = new TextField();
            field.setPrefWidth(240);
            field.setMinWidth(160);
            field.setPromptText("<null>");

            // Pre-fill previously entered value if available (DataGrip behavior)
            String previousValue = ParameterHistoryStore.get(param.name());
            if (previousValue != null && !previousValue.isBlank()) {
                field.setText(previousValue);
            }

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

        Button closeButton = new Button("Close");
        closeButton.getStyleClass().add("close-button");
        closeButton.setCancelButton(true);

        Button executeButton = new Button("Execute");
        executeButton.getStyleClass().addAll("run-button", "execute-button");
        executeButton.setDefaultButton(true);

        Runnable onExecute = () -> {
            Map<String, String> values = new LinkedHashMap<>();
            fields.forEach((name, field) -> {
                String text = field.getText();
                values.put(name, text);
                if (text != null && !text.isBlank()) {
                    ParameterHistoryStore.put(name, text);
                }
            });
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
        buttons.setPadding(new Insets(10, 20, 16, 20));
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(scroll, buttons);
        root.getStyleClass().addAll("app-root", "parameters-dialog");

        int maxNameLen = params.stream().mapToInt(p -> p.name().length()).max().orElse(12);
        int maxSnippetLen = params.stream().mapToInt(p -> {
            String s = p.contextSnippet() != null ? p.contextSnippet() : (p.guessedColumn() != null ? p.guessedColumn() : "");
            return s.length();
        }).max().orElse(6);

        double calculatedWidth = Math.max(540, Math.min(850, maxNameLen * 9.5 + Math.min(160, maxSnippetLen * 8.0) + 320));
        double calculatedHeight = Math.min(480, Math.max(180, params.size() * 48 + 110));

        Scene scene = new Scene(root, calculatedWidth, calculatedHeight);
        stage.setMinWidth(480);
        stage.setMinHeight(180);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);

        if (!fields.isEmpty()) {
            javafx.application.Platform.runLater(() -> {
                TextField first = fields.values().iterator().next();
                first.requestFocus();
                first.selectAll();
            });
        }

        stage.showAndWait();
        return Optional.ofNullable(result.get());
    }
}
