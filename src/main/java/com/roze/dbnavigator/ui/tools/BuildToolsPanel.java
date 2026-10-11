package com.roze.dbnavigator.ui.tools;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import java.util.Map;

/**
 * Settings panel for Tools > Build Tools.
 * Matches DataGrip screenshot media_1791723192757_b320a898.png.
 */
public class BuildToolsPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public BuildToolsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.BuildToolsConfig cfg = settings.getBuildTools();

        VBox content = new VBox(12);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        CheckBox syncProjectCheck = new CheckBox("Sync project after changes in the build scripts:");
        syncProjectCheck.setSelected(cfg.isSyncProjectAfterChanges());
        syncProjectCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_build_syncProjectAfterChanges", syncProjectCheck);

        ToggleGroup triggerGroup = new ToggleGroup();
        inputs.put("tools_build_syncTriggerGroup", triggerGroup);

        RadioButton anyChangesRadio = new RadioButton("Any changes");
        anyChangesRadio.setToggleGroup(triggerGroup);
        anyChangesRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        RadioButton externalChangesRadio = new RadioButton("External changes");
        externalChangesRadio.setToggleGroup(triggerGroup);
        externalChangesRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        Label desc = new Label("Changes outside the IDE or from version control.");
        desc.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px; -fx-padding: 0 0 0 24;");

        if ("Any changes".equalsIgnoreCase(cfg.getSyncTrigger())) {
            anyChangesRadio.setSelected(true);
        } else {
            externalChangesRadio.setSelected(true);
        }

        VBox externalBox = new VBox(4, externalChangesRadio, desc);

        VBox subOptions = new VBox(10, anyChangesRadio, externalBox);
        subOptions.setPadding(new Insets(0, 0, 0, 24));
        subOptions.disableProperty().bind(syncProjectCheck.selectedProperty().not());

        content.getChildren().addAll(syncProjectCheck, subOptions);
        return content;
    }
}
