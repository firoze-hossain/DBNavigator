package com.roze.dbnavigator.ui.languages;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.util.Optional;

/**
 * Modal dialog for mapping external schemas and DTDs to local or standard schema files.
 * Matches DataGrip screenshot media_1791719457567_be4c9d40.png.
 */
public class MapExternalResourceDialog extends Stage {

    private final TextField uriField;
    private final TextField fileField;
    private AppSettingsStore.ExternalResourceMapping result = null;

    public static record SchemaItem(String name, String uri, String defaultPath) {
        @Override
        public String toString() {
            return name;
        }
    }

    public MapExternalResourceDialog(Window owner, AppSettingsStore.ExternalResourceMapping initial) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Map External Resource");
        setResizable(true);

        uriField = new TextField(initial != null ? initial.getUri() : "");
        uriField.setPromptText("http://example.com/schema.xsd");
        uriField.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        HBox.setHgrow(uriField, Priority.ALWAYS);

        fileField = new TextField(initial != null ? initial.getLocation() : "");
        fileField.setPromptText("Local file path or schema location");
        fileField.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        HBox.setHgrow(fileField, Priority.ALWAYS);

        Button browseBtn = new Button("...");
        browseBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-min-width: 32px;");
        browseBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Schema or DTD File");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Schema and DTD Files (*.xsd, *.dtd, *.xml, *.rng, *.json)", "*.xsd", "*.dtd", "*.xml", "*.rng", "*.json"),
                    new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
            );
            if (!fileField.getText().isBlank()) {
                File current = new File(fileField.getText().trim());
                if (current.exists()) {
                    if (current.isDirectory()) chooser.setInitialDirectory(current);
                    else if (current.getParentFile() != null && current.getParentFile().exists()) chooser.setInitialDirectory(current.getParentFile());
                }
            }
            File chosen = chooser.showOpenDialog(this);
            if (chosen != null) {
                fileField.setText(chosen.getAbsolutePath());
            }
        });

        // Top form grid
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);

        Label uriLabel = new Label("URI:");
        uriLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-min-width: 45px;");
        formGrid.add(uriLabel, 0, 0);
        formGrid.add(uriField, 1, 0);

        Label fileLabel = new Label("File:");
        fileLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-min-width: 45px;");
        HBox fileBox = new HBox(6, fileField, browseBtn);
        fileBox.setAlignment(Pos.CENTER_LEFT);
        formGrid.add(fileLabel, 0, 1);
        formGrid.add(fileBox, 1, 1);

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setMinWidth(45);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        formGrid.getColumnConstraints().addAll(col0, col1);

        // Project Schemas Tree
        Label projectSchemasLabel = new Label("Project Schemas");
        projectSchemasLabel.setStyle("-fx-text-fill: -text; -fx-font-weight: bold; -fx-font-size: 13px;");

        TreeItem<SchemaItem> rootItem = new TreeItem<>(new SchemaItem("Project Schemas", "", ""));
        rootItem.setExpanded(true);

        populateSchemaTree(rootItem);

        TreeView<SchemaItem> schemaTree = new TreeView<>(rootItem);
        schemaTree.setShowRoot(true);
        schemaTree.setPrefHeight(220);
        schemaTree.setStyle("-fx-background-color: #1e1f22; -fx-control-inner-background: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(schemaTree, Priority.ALWAYS);

        schemaTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                SchemaItem item = newVal.getValue();
                if (!item.uri().isBlank()) {
                    uriField.setText(item.uri());
                    if (fileField.getText().isBlank() && !item.defaultPath().isBlank()) {
                        fileField.setText(item.defaultPath());
                    }
                }
            }
        });

        // Bottom Bar: Help icon (?) on left, OK and Cancel on right
        Button helpBtn = new Button("?");
        helpBtn.setTooltip(new Tooltip("Map an external URI namespace to a local schema or DTD specification."));
        helpBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text-dim; -fx-border-color: #393b40; -fx-border-radius: 12; -fx-background-radius: 12; -fx-min-width: 24px; -fx-min-height: 24px;");

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setStyle("-fx-background-color: #3574f0; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 75px; -fx-background-radius: 4;");
        okBtn.setOnAction(e -> {
            String uri = uriField.getText().trim();
            String file = fileField.getText().trim();
            if (uri.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Please specify a valid URI.", ButtonType.OK);
                alert.initOwner(this);
                alert.showAndWait();
                return;
            }
            result = new AppSettingsStore.ExternalResourceMapping(uri, file);
            close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-min-width: 75px;");
        cancelBtn.setOnAction(e -> close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttonBar = new HBox(10, helpBtn, spacer, okBtn, cancelBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));

        VBox root = new VBox(12, formGrid, projectSchemasLabel, schemaTree, buttonBar);
        root.setPadding(new Insets(14, 16, 14, 16));
        root.setStyle("-fx-background-color: #2b2d30;");

        Scene scene = new Scene(root, 520, 440);
        setScene(scene);
    }

    private void populateSchemaTree(TreeItem<SchemaItem> root) {
        TreeItem<SchemaItem> htmlCat = new TreeItem<>(new SchemaItem("HTML & XHTML", "", ""));
        htmlCat.getChildren().add(new TreeItem<>(new SchemaItem("HTML 4.01 Strict", "http://www.w3.org/TR/html4/strict.dtd", "resources/schemas/html4-strict.dtd")));
        htmlCat.getChildren().add(new TreeItem<>(new SchemaItem("HTML 4.01 Transitional", "http://www.w3.org/TR/html4/loose.dtd", "resources/schemas/html4-loose.dtd")));
        htmlCat.getChildren().add(new TreeItem<>(new SchemaItem("HTML 4.01 Frameset", "http://www.w3.org/TR/html4/frameset.dtd", "resources/schemas/html4-frameset.dtd")));
        htmlCat.getChildren().add(new TreeItem<>(new SchemaItem("HTML 5", "http://www.w3.org/1999/xhtml", "resources/schemas/html5.xsd")));
        htmlCat.getChildren().add(new TreeItem<>(new SchemaItem("XHTML 1.0 Strict", "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd", "resources/schemas/xhtml1-strict.dtd")));
        htmlCat.getChildren().add(new TreeItem<>(new SchemaItem("XHTML 1.0 Transitional", "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd", "resources/schemas/xhtml1-transitional.dtd")));
        htmlCat.setExpanded(true);

        TreeItem<SchemaItem> xmlCat = new TreeItem<>(new SchemaItem("XML Schema & Namespaces", "", ""));
        xmlCat.getChildren().add(new TreeItem<>(new SchemaItem("XML Schema 1.0", "http://www.w3.org/2001/XMLSchema", "resources/schemas/XMLSchema.xsd")));
        xmlCat.getChildren().add(new TreeItem<>(new SchemaItem("XML Schema 1.1", "http://www.w3.org/2009/XMLSchema/XMLSchema.xsd", "resources/schemas/XMLSchema11.xsd")));
        xmlCat.getChildren().add(new TreeItem<>(new SchemaItem("XML Schema Instance", "http://www.w3.org/2001/XMLSchema-instance", "resources/schemas/XMLSchema-instance.xsd")));
        xmlCat.getChildren().add(new TreeItem<>(new SchemaItem("XML 1998 Namespace", "http://www.w3.org/XML/1998/namespace", "resources/schemas/xml.xsd")));

        TreeItem<SchemaItem> relaxCat = new TreeItem<>(new SchemaItem("Relax NG", "", ""));
        relaxCat.getChildren().add(new TreeItem<>(new SchemaItem("Relax NG 1.0", "http://relaxng.org/ns/structure/1.0", "resources/schemas/relaxng.rng")));
        relaxCat.getChildren().add(new TreeItem<>(new SchemaItem("Relax NG Annotations", "http://relaxng.org/ns/compatibility/annotations/1.0", "resources/schemas/compatibility-annotations.rng")));

        TreeItem<SchemaItem> xsltCat = new TreeItem<>(new SchemaItem("XSLT", "", ""));
        xsltCat.getChildren().add(new TreeItem<>(new SchemaItem("XSLT 1.0", "http://www.w3.org/1999/XSL/Transform", "resources/schemas/xslt-1.0.xsd")));
        xsltCat.getChildren().add(new TreeItem<>(new SchemaItem("XSLT 2.0", "http://www.w3.org/2007/schema-for-xslt20.xsd", "resources/schemas/xslt-2.0.xsd")));

        TreeItem<SchemaItem> standardsCat = new TreeItem<>(new SchemaItem("Web & Standards", "", ""));
        standardsCat.getChildren().add(new TreeItem<>(new SchemaItem("SVG 1.1", "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd", "resources/schemas/svg11.dtd")));
        standardsCat.getChildren().add(new TreeItem<>(new SchemaItem("MathML 2.0", "http://www.w3.org/TR/MathML2/dtd/mathml2.dtd", "resources/schemas/mathml2.dtd")));
        standardsCat.getChildren().add(new TreeItem<>(new SchemaItem("WSDL 1.1", "http://schemas.xmlsoap.org/wsdl/", "resources/schemas/wsdl.xsd")));
        standardsCat.getChildren().add(new TreeItem<>(new SchemaItem("SOAP 1.1", "http://schemas.xmlsoap.org/soap/envelope/", "resources/schemas/soap-envelope.xsd")));

        root.getChildren().addAll(htmlCat, xmlCat, relaxCat, xsltCat, standardsCat);
    }

    public static Optional<AppSettingsStore.ExternalResourceMapping> showDialog(Window owner, AppSettingsStore.ExternalResourceMapping initial) {
        MapExternalResourceDialog dialog = new MapExternalResourceDialog(owner, initial);
        dialog.showAndWait();
        return Optional.ofNullable(dialog.result);
    }
}
