package com.roze.dbnavigator.ui.tools;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Map;

/**
 * Settings panel for Tools > Debugger > Stepping.
 * Matches DataGrip screenshot media_1791725748857_bda3060c.png.
 */
public class DebuggerSteppingPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public DebuggerSteppingPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.DebuggerSteppingConfig cfg = settings.getDebuggerStepping();

        VBox content = new VBox(14);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        // Oracle section
        Label oracleHeader = new Label("Oracle");
        oracleHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px;");

        Label steppingModeLabel = new Label("Stepping mode:");
        steppingModeLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        steppingModeLabel.setPrefWidth(120);

        ComboBox<String> steppingCombo = new ComboBox<>();
        steppingCombo.getItems().addAll("Graceful", "Force step", "Step into");
        steppingCombo.setValue(cfg.getSteppingMode());
        steppingCombo.setPrefWidth(200);
        inputs.put("tools_debugger_steppingMode", steppingCombo);

        HBox modeRow = new HBox(10, steppingModeLabel, steppingCombo);
        modeRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox pauseAtBegin = new CheckBox("Pause at begin");
        pauseAtBegin.setSelected(cfg.isPauseAtBegin());
        pauseAtBegin.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_debugger_pauseAtBegin", pauseAtBegin);

        VBox oracleBox = new VBox(10, oracleHeader, modeRow, pauseAtBegin);

        content.getChildren().add(oracleBox);
        return content;
    }
}
