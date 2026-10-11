package com.roze.dbnavigator.ui.languages;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Map;

/**
 * Settings panel for Languages > Schemas and DTDs > Default XML Schemas.
 * Matches DataGrip screenshot media_1791719497531_58de0677.png.
 */
public class DefaultXmlSchemasPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public DefaultXmlSchemasPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.DefaultXmlSchemasConfig cfg = settings.getDefaultXmlSchemas();

        VBox content = new VBox(20);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        // Section 1: Default HTML language level
        Label htmlTitle = new Label("Default HTML language level");
        htmlTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px;");

        Separator htmlSep = new Separator();
        HBox htmlHeader = new HBox(10, htmlTitle, htmlSep);
        htmlHeader.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(htmlSep, Priority.ALWAYS);

        ToggleGroup htmlGroup = new ToggleGroup();
        inputs.put("languages_xml_htmlLanguageGroup", htmlGroup);

        RadioButton html4Radio = new RadioButton("HTML 4 (\"http://www.w3.org/TR/html4/loose.dtd\")");
        html4Radio.setToggleGroup(htmlGroup);
        html4Radio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        RadioButton html5Radio = new RadioButton("HTML 5");
        html5Radio.setToggleGroup(htmlGroup);
        html5Radio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        RadioButton otherRadio = new RadioButton("Other doctype:");
        otherRadio.setToggleGroup(htmlGroup);
        otherRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        TextField otherDoctypeField = new TextField(cfg.getHtmlOtherDoctype());
        otherDoctypeField.setPromptText("<!DOCTYPE html ... >");
        otherDoctypeField.setPrefWidth(320);
        otherDoctypeField.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        otherDoctypeField.disableProperty().bind(otherRadio.selectedProperty().not());
        inputs.put("languages_xml_htmlOtherDoctype", otherDoctypeField);

        HBox otherRow = new HBox(8, otherRadio, otherDoctypeField);
        otherRow.setAlignment(Pos.CENTER_LEFT);

        // Select initial
        String currentHtml = cfg.getHtmlLanguageLevel();
        if ("HTML 4".equalsIgnoreCase(currentHtml) || currentHtml.contains("HTML 4")) {
            html4Radio.setSelected(true);
        } else if ("Other".equalsIgnoreCase(currentHtml) || currentHtml.startsWith("Other")) {
            otherRadio.setSelected(true);
        } else {
            html5Radio.setSelected(true);
        }

        VBox htmlBox = new VBox(10, htmlHeader, html4Radio, html5Radio, otherRow);

        // Section 2: XML Schema version
        Label xmlTitle = new Label("XML Schema version");
        xmlTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px;");

        Separator xmlSep = new Separator();
        HBox xmlHeader = new HBox(10, xmlTitle, xmlSep);
        xmlHeader.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(xmlSep, Priority.ALWAYS);

        ToggleGroup xmlGroup = new ToggleGroup();
        inputs.put("languages_xml_schemaVersionGroup", xmlGroup);

        RadioButton xml10Radio = new RadioButton("XML Schema 1.0");
        xml10Radio.setToggleGroup(xmlGroup);
        xml10Radio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        RadioButton xml11Radio = new RadioButton("XML Schema 1.1");
        xml11Radio.setToggleGroup(xmlGroup);
        xml11Radio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        if ("1.1".equals(cfg.getXmlSchemaVersion()) || cfg.getXmlSchemaVersion().contains("1.1")) {
            xml11Radio.setSelected(true);
        } else {
            xml10Radio.setSelected(true);
        }

        VBox xmlBox = new VBox(10, xmlHeader, xml10Radio, xml11Radio);

        content.getChildren().addAll(htmlBox, xmlBox);
        return content;
    }
}
