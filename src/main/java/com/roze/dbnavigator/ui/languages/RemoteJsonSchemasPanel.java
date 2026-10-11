package com.roze.dbnavigator.ui.languages;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.util.Map;

/**
 * Settings panel for Languages > Schemas and DTDs > Remote JSON Schemas.
 * Matches DataGrip screenshot media_1791719653093_ea40501b.png.
 */
public class RemoteJsonSchemasPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public RemoteJsonSchemasPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.RemoteJsonSchemasConfig cfg = settings.getRemoteJsonSchemas();

        VBox content = new VBox(14);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        // Top level checkbox
        CheckBox allowDownloadCheck = new CheckBox("Allow downloading JSON Schemas from remote sources");
        allowDownloadCheck.setSelected(cfg.isAllowDownloadRemoteSchemas());
        allowDownloadCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("languages_remoteJson_allowDownload", allowDownloadCheck);

        // Indented section
        VBox indentedBox = new VBox(12);
        indentedBox.setPadding(new Insets(0, 0, 0, 24));
        indentedBox.disableProperty().bind(allowDownloadCheck.selectedProperty().not());

        // Sub-option 1: Use schemastore.org
        CheckBox useSchemaStoreCheck = new CheckBox("Use schemastore.org JSON Schema catalog");
        useSchemaStoreCheck.setSelected(cfg.isUseSchemaStoreCatalog());
        useSchemaStoreCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("languages_remoteJson_useSchemaStore", useSchemaStoreCheck);

        Label desc1Pre = new Label("Schemas will be downloaded and assigned using the");
        desc1Pre.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");

        Hyperlink apiLink = new Hyperlink("SchemaStore API ↗");
        apiLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 11px; -fx-padding: 0;");
        apiLink.setOnAction(e -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("https://www.schemastore.org/api/json/catalog.json"));
                }
            } catch (Exception ignored) {}
        });

        HBox desc1Box = new HBox(4, desc1Pre, apiLink);
        desc1Box.setAlignment(Pos.CENTER_LEFT);
        desc1Box.setPadding(new Insets(0, 0, 0, 24));

        VBox option1Box = new VBox(4, useSchemaStoreCheck, desc1Box);

        // Sub-option 2: Always download the most recent version of schemas
        CheckBox alwaysDownloadCheck = new CheckBox("Always download the most recent version of schemas");
        alwaysDownloadCheck.setSelected(cfg.isAlwaysDownloadMostRecent());
        alwaysDownloadCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("languages_remoteJson_alwaysDownloadMostRecent", alwaysDownloadCheck);

        Label desc2 = new Label("Schemas will always be downloaded from the SchemaStore, even if some of them are bundled with the IDE");
        desc2.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px; -fx-padding: 0 0 0 24;");

        VBox option2Box = new VBox(4, alwaysDownloadCheck, desc2);

        indentedBox.getChildren().addAll(option1Box, option2Box);
        content.getChildren().addAll(allowDownloadCheck, indentedBox);

        return content;
    }
}
