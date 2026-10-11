package com.roze.dbnavigator.ui.tools;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Map;

/**
 * Settings panel for Tools > Debugger > Data Views.
 * Matches DataGrip screenshot media_1791725739349_d0bbcb45.png.
 */
public class DebuggerDataViewsPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public DebuggerDataViewsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.DebuggerDataViewsConfig cfg = settings.getDebuggerDataViews();

        VBox content = new VBox(14);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        // Top checkboxes
        CheckBox sortValues = new CheckBox("Sort values alphabetically");
        sortValues.setSelected(cfg.isSortValuesAlphabetically());
        sortValues.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_debugger_sortValuesAlphabetically", sortValues);

        CheckBox enableAutoExpr = new CheckBox("Enable auto expressions in Variables view");
        enableAutoExpr.setSelected(cfg.isEnableAutoExpressions());
        enableAutoExpr.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_debugger_enableAutoExpressions", enableAutoExpr);

        VBox topBox = new VBox(10, sortValues, enableAutoExpr);

        // Editor Section
        Label editorLabel = new Label("Editor");
        editorLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px;");

        Separator sep = new Separator();
        HBox header = new HBox(10, editorLabel, sep);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(sep, Priority.ALWAYS);

        CheckBox showInline = new CheckBox("Show values inline");
        showInline.setSelected(cfg.isShowValuesInline());
        showInline.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_debugger_showValuesInline", showInline);

        CheckBox showTooltip = new CheckBox("Show value tooltip. Value tooltip delay (ms):");
        showTooltip.setSelected(cfg.isShowValueTooltip());
        showTooltip.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_debugger_showValueTooltip", showTooltip);

        Spinner<Integer> delaySpinner = new Spinner<>(0, 10000, cfg.getValueTooltipDelayMs(), 50);
        delaySpinner.setEditable(true);
        delaySpinner.setPrefWidth(90);
        delaySpinner.disableProperty().bind(showTooltip.selectedProperty().not());
        inputs.put("tools_debugger_valueTooltipDelayMs", delaySpinner);

        HBox tooltipRow = new HBox(8, showTooltip, delaySpinner);
        tooltipRow.setAlignment(Pos.CENTER_LEFT);

        Label altHint = new Label("If disabled, use \"alt\" to show/hide tooltips");
        altHint.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px; -fx-padding: 0 0 0 24;");

        VBox tooltipBox = new VBox(4, tooltipRow, altHint);

        CheckBox showOnSelect = new CheckBox("Show value tooltip on code selection");
        showOnSelect.setSelected(cfg.isShowValueTooltipOnCodeSelection());
        showOnSelect.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_debugger_showValueTooltipOnCodeSelection", showOnSelect);

        VBox editorBox = new VBox(10, header, showInline, tooltipBox, showOnSelect);

        content.getChildren().addAll(topBox, editorBox);
        return content;
    }
}
