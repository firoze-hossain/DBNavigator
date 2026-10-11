package com.roze.dbnavigator.ui.tools;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Map;

/**
 * Settings panel for Tools > XPath Viewer.
 * Matches DataGrip screenshot media_1791728466658_7a28784a.png.
 */
public class XPathViewerPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public XPathViewerPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.XPathViewerConfig cfg = settings.getXpathViewer();

        VBox content = new VBox(16);
        content.setPadding(new Insets(10, 16, 20, 16));
        content.setStyle("-fx-background-color: transparent;");

        // --- Section: Settings ---
        Label settingsSectionTitle = new Label("Settings");
        settingsSectionTitle.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-font-weight: bold;");

        Separator settingsSep = new Separator();
        HBox.setHgrow(settingsSep, Priority.ALWAYS);
        HBox settingsHeader = new HBox(8, settingsSectionTitle, settingsSep);
        settingsHeader.setAlignment(Pos.CENTER_LEFT);

        CheckBox scrollFirstHitCheck = new CheckBox("Scroll first hit into visible area");
        scrollFirstHitCheck.setSelected(cfg.isScrollFirstHitIntoVisibleArea());
        scrollFirstHitCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_xpath_scrollFirstHit", scrollFirstHitCheck);

        CheckBox useNodeAtCursorCheck = new CheckBox("Use node at cursor as context node");
        useNodeAtCursorCheck.setSelected(cfg.isUseNodeAtCursorAsContextNode());
        useNodeAtCursorCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_xpath_useNodeAtCursor", useNodeAtCursorCheck);

        CheckBox highlightStartTagCheck = new CheckBox("Highlight only start tag instead of whole tag content");
        highlightStartTagCheck.setSelected(cfg.isHighlightOnlyStartTag());
        highlightStartTagCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_xpath_highlightOnlyStartTag", highlightStartTagCheck);

        CheckBox errorStripeMarkersCheck = new CheckBox("Add error stripe markers for each result");
        errorStripeMarkersCheck.setSelected(cfg.isAddErrorStripeMarkers());
        errorStripeMarkersCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_xpath_addErrorStripeMarkers", errorStripeMarkersCheck);

        VBox settingsSection = new VBox(8,
                settingsHeader,
                scrollFirstHitCheck,
                useNodeAtCursorCheck,
                highlightStartTagCheck,
                errorStripeMarkersCheck
        );

        // --- Section: Colors ---
        Label colorsSectionTitle = new Label("Colors");
        colorsSectionTitle.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-font-weight: bold;");

        Separator colorsSep = new Separator();
        HBox.setHgrow(colorsSep, Priority.ALWAYS);
        HBox colorsHeader = new HBox(8, colorsSectionTitle, colorsSep);
        colorsHeader.setAlignment(Pos.CENTER_LEFT);

        GridPane colorsGrid = new GridPane();
        colorsGrid.setHgap(12);
        colorsGrid.setVgap(10);
        colorsGrid.setPadding(new Insets(4, 0, 0, 0));

        Label highlightColorLbl = new Label("Highlight color:");
        highlightColorLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        Color initialHighlight = parseHexColor(cfg.getHighlightColor(), Color.web("#FFD578"));
        ColorPicker highlightColorPicker = new ColorPicker(initialHighlight);
        highlightColorPicker.setStyle("-fx-color-label-visible: true;");
        inputs.put("tools_xpath_highlightColor", highlightColorPicker);

        Label contextColorLbl = new Label("Context node color:");
        contextColorLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        Color initialContext = parseHexColor(cfg.getContextNodeColor(), Color.web("#C2FFD4"));
        ColorPicker contextColorPicker = new ColorPicker(initialContext);
        contextColorPicker.setStyle("-fx-color-label-visible: true;");
        inputs.put("tools_xpath_contextNodeColor", contextColorPicker);

        colorsGrid.add(highlightColorLbl, 0, 0);
        colorsGrid.add(highlightColorPicker, 1, 0);
        colorsGrid.add(contextColorLbl, 0, 1);
        colorsGrid.add(contextColorPicker, 1, 1);

        VBox colorsSection = new VBox(8, colorsHeader, colorsGrid);

        content.getChildren().addAll(settingsSection, colorsSection);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private static Color parseHexColor(String hex, Color fallback) {
        if (hex == null || hex.isBlank()) return fallback;
        try {
            String clean = hex.startsWith("#") ? hex : "#" + hex;
            return Color.web(clean);
        } catch (Exception e) {
            return fallback;
        }
    }
}
