package com.roze.dbnavigator.ui.tools;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Map;

/**
 * Settings panel for Tools > Coverage.
 * Matches DataGrip screenshot media_1791723242649_c0fe0b88.png.
 */
public class CoveragePanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public CoveragePanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.CoverageConfig cfg = settings.getCoverage();

        VBox content = new VBox(14);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        // Section: When New Coverage Is Gathered
        Label sectionTitle = new Label("When New Coverage Is Gathered");
        sectionTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px;");

        Separator sep = new Separator();
        HBox header = new HBox(10, sectionTitle, sep);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(sep, Priority.ALWAYS);

        ToggleGroup group = new ToggleGroup();
        inputs.put("tools_coverage_whenGatheredGroup", group);

        RadioButton rb1 = new RadioButton("Show options before applying coverage to the editor");
        rb1.setToggleGroup(group);
        rb1.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        RadioButton rb2 = new RadioButton("Do not apply collected coverage");
        rb2.setToggleGroup(group);
        rb2.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        RadioButton rb3 = new RadioButton("Replace active suites with the new one");
        rb3.setToggleGroup(group);
        rb3.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        RadioButton rb4 = new RadioButton("Add to the active suites");
        rb4.setToggleGroup(group);
        rb4.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        String currentWhen = cfg.getWhenNewCoverageGathered();
        if (currentWhen.contains("Do not apply")) {
            rb2.setSelected(true);
        } else if (currentWhen.contains("Replace active")) {
            rb3.setSelected(true);
        } else if (currentWhen.contains("Add to")) {
            rb4.setSelected(true);
        } else {
            rb1.setSelected(true);
        }

        VBox radioBox = new VBox(8, rb1, rb2, rb3, rb4);
        radioBox.setPadding(new Insets(0, 0, 8, 0));

        // Checkboxes section
        CheckBox activateViewCheck = new CheckBox("Activate Coverage View");
        activateViewCheck.setSelected(cfg.isActivateCoverageView());
        activateViewCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_coverage_activateView", activateViewCheck);

        CheckBox showInProjectCheck = new CheckBox("Show coverage in the project view");
        showInProjectCheck.setSelected(cfg.isShowCoverageInProjectView());
        showInProjectCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_coverage_showInProjectView", showInProjectCheck);

        VBox checksBox = new VBox(8, activateViewCheck, showInProjectCheck);

        content.getChildren().addAll(header, radioBox, checksBox);
        return content;
    }
}
