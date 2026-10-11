package com.roze.dbnavigator.ui.languages;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

import java.util.Map;

/**
 * Settings panel for Languages > XSLT.
 * Matches DataGrip screenshot media_1791719715605_3cbe350d.png.
 */
public class XsltSettingsPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public XsltSettingsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.XsltConfig cfg = settings.getXslt();

        VBox content = new VBox(14);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        CheckBox showAssociatedFilesCheck = new CheckBox("Show associated files in project view");
        showAssociatedFilesCheck.setSelected(cfg.isShowAssociatedFilesInProjectView());
        showAssociatedFilesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("languages_xslt_showAssociatedFiles", showAssociatedFilesCheck);

        content.getChildren().add(showAssociatedFilesCheck);
        return content;
    }
}
