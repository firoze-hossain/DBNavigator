package com.roze.dbnavigator.ui.languages;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Settings panel for Languages > Schemas and DTDs.
 * Matches DataGrip screenshot media_1791719446179_7af815e0.png.
 */
public class SchemasDtdsPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public SchemasDtdsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.SchemasDtdsConfig cfg = settings.getSchemasDtds();

        VBox content = new VBox(14);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        // Section 1: External schemas and DTDs
        VBox extBox = buildExternalSchemasSection(cfg);
        VBox.setVgrow(extBox, Priority.ALWAYS);

        // Section 2: Ignored schemas and DTDs
        VBox ignoredBox = buildIgnoredSchemasSection(cfg);
        VBox.setVgrow(ignoredBox, Priority.ALWAYS);

        content.getChildren().addAll(extBox, ignoredBox);
        return content;
    }

    private VBox buildExternalSchemasSection(AppSettingsStore.SchemasDtdsConfig cfg) {
        Label title = new Label("External schemas and DTDs:");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px;");

        ObservableList<AppSettingsStore.ExternalResourceMapping> items = FXCollections.observableArrayList();
        for (AppSettingsStore.ExternalResourceMapping m : cfg.getExternalResources()) {
            if (m != null) items.add(m.copy());
        }
        inputs.put("languages_schemas_externalResources", items);

        TableView<AppSettingsStore.ExternalResourceMapping> table = new TableView<>(items);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(150);
        table.setStyle("-fx-background-color: #1e1f22; -fx-control-inner-background: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label placeholder = new Label("No external resources");
        placeholder.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px;");
        table.setPlaceholder(placeholder);

        TableColumn<AppSettingsStore.ExternalResourceMapping, String> uriCol = new TableColumn<>("URI");
        uriCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUri()));
        uriCol.setPrefWidth(260);

        TableColumn<AppSettingsStore.ExternalResourceMapping, String> locCol = new TableColumn<>("Location");
        locCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLocation()));
        locCol.setPrefWidth(320);

        table.getColumns().addAll(uriCol, locCol);

        // Toolbar: + - Edit
        Button addBtn = new Button("+");
        addBtn.setTooltip(new Tooltip("Add External Schema or DTD"));
        addBtn.setStyle("-fx-min-width: 28px; -fx-min-height: 24px; -fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");

        Button removeBtn = new Button("—");
        removeBtn.setTooltip(new Tooltip("Remove"));
        removeBtn.setStyle("-fx-min-width: 28px; -fx-min-height: 24px; -fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        removeBtn.setDisable(true);

        Button editBtn = new Button("✏");
        editBtn.setTooltip(new Tooltip("Edit"));
        editBtn.setStyle("-fx-min-width: 28px; -fx-min-height: 24px; -fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        editBtn.setDisable(true);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSel = newVal != null;
            removeBtn.setDisable(!hasSel);
            editBtn.setDisable(!hasSel);
        });

        addBtn.setOnAction(e -> {
            Window win = table.getScene() != null ? table.getScene().getWindow() : null;
            MapExternalResourceDialog.showDialog(win, null).ifPresent(mapping -> {
                items.add(mapping);
                table.getSelectionModel().select(mapping);
            });
        });

        editBtn.setOnAction(e -> {
            AppSettingsStore.ExternalResourceMapping sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                Window win = table.getScene() != null ? table.getScene().getWindow() : null;
                MapExternalResourceDialog.showDialog(win, sel).ifPresent(mapping -> {
                    int idx = items.indexOf(sel);
                    if (idx >= 0) {
                        items.set(idx, mapping);
                        table.getSelectionModel().select(mapping);
                    }
                });
            }
        });

        removeBtn.setOnAction(e -> {
            AppSettingsStore.ExternalResourceMapping sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                items.remove(sel);
            }
        });

        table.setRowFactory(tv -> {
            TableRow<AppSettingsStore.ExternalResourceMapping> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editBtn.fire();
                }
            });
            return row;
        });

        HBox toolbar = new HBox(4, addBtn, removeBtn, editBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(6, title, toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private VBox buildIgnoredSchemasSection(AppSettingsStore.SchemasDtdsConfig cfg) {
        Label title = new Label("Ignored schemas and DTDs:");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px; -fx-padding: 8 0 0 0;");

        ObservableList<String> items = FXCollections.observableArrayList();
        if (cfg.getIgnoredResources() != null) {
            items.addAll(cfg.getIgnoredResources());
        }
        inputs.put("languages_schemas_ignoredResources", items);

        ListView<String> listView = new ListView<>(items);
        listView.setPrefHeight(180);
        listView.setStyle("-fx-background-color: #1e1f22; -fx-control-inner-background: #1e1f22; -fx-font-family: monospace; -fx-font-size: 12px; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");

        Button addBtn = new Button("+");
        addBtn.setTooltip(new Tooltip("Add Ignored Schema or DTD"));
        addBtn.setStyle("-fx-min-width: 28px; -fx-min-height: 24px; -fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");

        Button removeBtn = new Button("—");
        removeBtn.setTooltip(new Tooltip("Remove"));
        removeBtn.setStyle("-fx-min-width: 28px; -fx-min-height: 24px; -fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        removeBtn.setDisable(true);

        Button editBtn = new Button("✏");
        editBtn.setTooltip(new Tooltip("Edit"));
        editBtn.setStyle("-fx-min-width: 28px; -fx-min-height: 24px; -fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        editBtn.setDisable(true);

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSel = newVal != null;
            removeBtn.setDisable(!hasSel);
            editBtn.setDisable(!hasSel);
        });

        addBtn.setOnAction(e -> {
            Window win = listView.getScene() != null ? listView.getScene().getWindow() : null;
            IgnoredResourceDialog.showDialog(win, null).ifPresent(uri -> {
                if (!items.contains(uri)) {
                    items.add(uri);
                    listView.getSelectionModel().select(uri);
                }
            });
        });

        editBtn.setOnAction(e -> {
            String sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                Window win = listView.getScene() != null ? listView.getScene().getWindow() : null;
                IgnoredResourceDialog.showDialog(win, sel).ifPresent(uri -> {
                    int idx = items.indexOf(sel);
                    if (idx >= 0) {
                        items.set(idx, uri);
                        listView.getSelectionModel().select(uri);
                    }
                });
            }
        });

        removeBtn.setOnAction(e -> {
            String sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                items.remove(sel);
            }
        });

        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && listView.getSelectionModel().getSelectedItem() != null) {
                editBtn.fire();
            }
        });

        HBox toolbar = new HBox(4, addBtn, removeBtn, editBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(6, title, toolbar, listView);
        VBox.setVgrow(listView, Priority.ALWAYS);
        return box;
    }
}
