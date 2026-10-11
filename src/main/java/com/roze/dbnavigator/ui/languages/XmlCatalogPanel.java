package com.roze.dbnavigator.ui.languages;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.Map;

/**
 * Settings panel for Languages > Schemas and DTDs > XML Catalog.
 * Matches DataGrip screenshot media_1791719668070_d33648fc.png.
 */
public class XmlCatalogPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public XmlCatalogPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.XmlCatalogConfig cfg = settings.getXmlCatalog();

        VBox content = new VBox(14);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        Label label = new Label("Catalog property file:");
        label.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-min-width: 140px;");

        TextField field = new TextField(cfg.getCatalogPropertyFile());
        field.setPromptText("Path to catalog.xml or catalog.properties");
        field.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        HBox.setHgrow(field, Priority.ALWAYS);
        inputs.put("languages_xmlCatalog_propertyFile", field);

        Button browseBtn = new Button("📁");
        browseBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-min-width: 30px;");
        browseBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose XML Catalog or Properties File");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("XML Catalog / Properties Files (*.xml, *.properties)", "*.xml", "*.properties"),
                    new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
            );
            Window win = browseBtn.getScene() != null ? browseBtn.getScene().getWindow() : null;
            File f = chooser.showOpenDialog(win);
            if (f != null) {
                field.setText(f.getAbsolutePath());
            }
        });

        HBox row = new HBox(8, label, field, browseBtn);
        row.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().add(row);
        return content;
    }
}
