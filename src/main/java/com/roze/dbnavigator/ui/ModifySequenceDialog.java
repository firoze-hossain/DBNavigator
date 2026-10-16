package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * "Modify Sequence…" dialog for PostgreSQL sequences:
 * allows configuring increment, min/max values, restart value, cache, and cycle.
 */
public final class ModifySequenceDialog {

    private ModifySequenceDialog() {}

    public static void show(MainWindow mainWindow, ConnectionProfile profile, DbObject sequenceObj) {
        Stage stage = new Stage();
        Window owner = mainWindow != null ? mainWindow.getOwnerWindow() : null;
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Modify Sequence \u2014 " + sequenceObj.getName());
        stage.setResizable(false);

        String schema = sequenceObj.getSchema() != null && !sequenceObj.getSchema().isBlank()
                ? sequenceObj.getSchema() : "public";

        TextField nameField = new TextField(sequenceObj.getName());
        TextField incrementField = new TextField("1");
        TextField minValueField = new TextField("1");
        TextField maxValueField = new TextField("9223372036854775807");
        TextField restartField = new TextField("");
        restartField.setPromptText("(leave blank to keep current)");
        TextField cacheField = new TextField("1");
        CheckBox cycleBox = new CheckBox("Cycle");

        // Load current properties in background
        AppExecutor.run(() -> {
            try (Connection conn = ClientRegistry.jdbc(profile, sequenceObj.getCatalog()).getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT increment, minimum_value, maximum_value, cycle_option "
                         + "FROM information_schema.sequences "
                         + "WHERE sequence_schema = '" + schema.replace("'", "''") + "' "
                         + "AND sequence_name = '" + sequenceObj.getName().replace("'", "''") + "'")) {
                if (rs.next()) {
                    String inc = rs.getString("increment");
                    String min = rs.getString("minimum_value");
                    String max = rs.getString("maximum_value");
                    String cycle = rs.getString("cycle_option");
                    Platform.runLater(() -> {
                        if (inc != null) incrementField.setText(inc);
                        if (min != null) minValueField.setText(min);
                        if (max != null) maxValueField.setText(max);
                        cycleBox.setSelected("YES".equalsIgnoreCase(cycle));
                    });
                }
            } catch (Exception ignored) {}
        });

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16, 20, 10, 20));

        int row = 0;
        grid.add(new Label("Name:"), 0, row);
        grid.add(nameField, 1, row++);
        grid.add(new Label("Increment:"), 0, row);
        grid.add(incrementField, 1, row++);
        grid.add(new Label("Min Value:"), 0, row);
        grid.add(minValueField, 1, row++);
        grid.add(new Label("Max Value:"), 0, row);
        grid.add(maxValueField, 1, row++);
        grid.add(new Label("Restart With:"), 0, row);
        grid.add(restartField, 1, row++);
        grid.add(new Label("Cache:"), 0, row);
        grid.add(cacheField, 1, row++);
        grid.add(new Label("Cycle:"), 0, row);
        grid.add(cycleBox, 1, row++);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> stage.close());

        Button applyBtn = new Button("Apply");
        applyBtn.getStyleClass().add("run-button");
        applyBtn.setDefaultButton(true);
        applyBtn.setOnAction(e -> {
            StringBuilder sql = new StringBuilder("ALTER SEQUENCE \"")
                    .append(schema.replace("\"", "\"\""))
                    .append("\".\"")
                    .append(sequenceObj.getName().replace("\"", "\"\""))
                    .append("\"");

            if (!incrementField.getText().isBlank()) {
                sql.append(" INCREMENT BY ").append(incrementField.getText().trim());
            }
            if (!minValueField.getText().isBlank()) {
                sql.append(" MINVALUE ").append(minValueField.getText().trim());
            }
            if (!maxValueField.getText().isBlank()) {
                sql.append(" MAXVALUE ").append(maxValueField.getText().trim());
            }
            if (!restartField.getText().isBlank()) {
                sql.append(" RESTART WITH ").append(restartField.getText().trim());
            }
            if (!cacheField.getText().isBlank()) {
                sql.append(" CACHE ").append(cacheField.getText().trim());
            }
            sql.append(cycleBox.isSelected() ? " CYCLE" : " NO CYCLE");

            String newName = nameField.getText().trim();
            String renameSql = (!newName.isEmpty() && !newName.equals(sequenceObj.getName()))
                    ? "ALTER SEQUENCE \"" + schema.replace("\"", "\"\"") + "\".\""
                        + sequenceObj.getName().replace("\"", "\"\"") + "\" RENAME TO \""
                        + newName.replace("\"", "\"\"") + "\";"
                    : null;

            AppExecutor.run(() -> {
                try (Connection conn = ClientRegistry.jdbc(profile, sequenceObj.getCatalog()).getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.execute(sql.toString());
                    if (renameSql != null) {
                        stmt.execute(renameSql);
                    }
                    Platform.runLater(() -> {
                        if (mainWindow != null) {
                            mainWindow.setStatus("Modified sequence " + sequenceObj.getName());
                            mainWindow.refreshSchemaExplorer();
                        }
                        stage.close();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                            "Could not modify sequence: " + ex.getMessage())).showAndWait());
                }
            });
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttonBar = new HBox(8, spacer, cancelBtn, applyBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 20, 16, 20));

        VBox root = new VBox(grid, buttonBar);
        Scene scene = new Scene(root);
        DialogTheme.applyToScene(scene);
        stage.setScene(scene);
        stage.show();
    }
}
