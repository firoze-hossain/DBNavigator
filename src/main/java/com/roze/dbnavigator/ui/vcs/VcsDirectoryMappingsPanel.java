package com.roze.dbnavigator.ui.vcs;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.ui.SettingsDialog;
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
 * Settings panel for Version Control > Directory Mappings,
 * matching DataGrip screenshot media_1791678961836_498ba393.png.
 */
public class VcsDirectoryMappingsPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public VcsDirectoryMappingsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.VcsDirectoryMappingConfig cfg = settings.getVcsDirectoryMappings();

        VBox content = new VBox(10);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        // Working list of mappings
        ObservableList<AppSettingsStore.VcsDirectoryMappingEntry> mappingsList = FXCollections.observableArrayList();
        if (cfg.getMappings() != null) {
            for (AppSettingsStore.VcsDirectoryMappingEntry m : cfg.getMappings()) {
                mappingsList.add(m.copy());
            }
        }
        if (mappingsList.isEmpty()) {
            mappingsList.addAll(AppSettingsStore.VcsDirectoryMappingConfig.defaultMappings());
        }
        inputs.put("vcs_directoryMappings", mappingsList);

        // Action Toolbar
        Button addBtn = new Button("+");
        addBtn.setTooltip(new Tooltip("Add VCS Mapping"));
        addBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-font-weight: bold; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-min-width: 28px; -fx-cursor: hand;");

        Button removeBtn = new Button("-");
        removeBtn.setTooltip(new Tooltip("Remove VCS Mapping"));
        removeBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-font-weight: bold; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-min-width: 28px; -fx-cursor: hand;");
        removeBtn.setDisable(true);

        Button editBtn = new Button("✎");
        editBtn.setTooltip(new Tooltip("Edit VCS Mapping"));
        editBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-font-weight: bold; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-min-width: 28px; -fx-cursor: hand;");
        editBtn.setDisable(true);

        HBox toolbar = new HBox(4, addBtn, removeBtn, editBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // Table
        TableView<AppSettingsStore.VcsDirectoryMappingEntry> table = new TableView<>(mappingsList);
        table.setPrefHeight(200);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");

        TableColumn<AppSettingsStore.VcsDirectoryMappingEntry, String> dirCol = new TableColumn<>("Directory");
        dirCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDirectory()));
        dirCol.setPrefWidth(320);

        TableColumn<AppSettingsStore.VcsDirectoryMappingEntry, String> vcsCol = new TableColumn<>("VCS");
        vcsCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getVcs()));
        vcsCol.setPrefWidth(120);

        table.getColumns().addAll(dirCol, vcsCol);

        // Selection listener to enable/disable remove and edit
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            boolean hasSel = selected != null;
            editBtn.setDisable(!hasSel);
            // In DataGrip, removing <Project> root is disabled or discouraged
            removeBtn.setDisable(!hasSel || "<Project>".equalsIgnoreCase(selected.getDirectory()));
        });

        // Add action
        addBtn.setOnAction(e -> {
            Window owner = table.getScene() != null ? table.getScene().getWindow() : null;
            DirectoryMappingDialog dialog = new DirectoryMappingDialog(owner, null);
            AppSettingsStore.VcsDirectoryMappingEntry newEntry = dialog.showAndWait();
            if (newEntry != null) {
                mappingsList.add(newEntry);
                table.getSelectionModel().select(newEntry);
            }
        });

        // Edit action
        Runnable editAction = () -> {
            AppSettingsStore.VcsDirectoryMappingEntry selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            Window owner = table.getScene() != null ? table.getScene().getWindow() : null;
            DirectoryMappingDialog dialog = new DirectoryMappingDialog(owner, selected);
            AppSettingsStore.VcsDirectoryMappingEntry updated = dialog.showAndWait();
            if (updated != null) {
                int idx = mappingsList.indexOf(selected);
                if (idx >= 0) {
                    mappingsList.set(idx, updated);
                    table.getSelectionModel().select(idx);
                }
            }
        };

        editBtn.setOnAction(e -> editAction.run());

        // Double click to edit
        table.setRowFactory(tv -> {
            TableRow<AppSettingsStore.VcsDirectoryMappingEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editAction.run();
                }
            });
            return row;
        });

        // Remove action
        removeBtn.setOnAction(e -> {
            AppSettingsStore.VcsDirectoryMappingEntry selected = table.getSelectionModel().getSelectedItem();
            if (selected != null && !"<Project>".equalsIgnoreCase(selected.getDirectory())) {
                mappingsList.remove(selected);
            }
        });

        // Explanatory label below table
        Label descLabel = new Label("<Project> - Content roots of all modules, all immediate descendants of project base directory, and .idea directory contents");
        descLabel.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px; -fx-wrap-text: true;");

        // Bottom checkbox with tooltip
        CheckBox autoDetectCheck = new CheckBox("Enable automatic mapping detection");
        autoDetectCheck.setSelected(cfg.isEnableAutomaticMappingDetection());
        autoDetectCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("vcs_enableAutomaticMappingDetection", autoDetectCheck);

        Label helpIcon = SettingsDialog.createHelpTooltip("Automatically detect VCS repositories in project directories and suggest registering directory mappings.");

        HBox autoDetectRow = new HBox(6, autoDetectCheck, helpIcon);
        autoDetectRow.setAlignment(Pos.CENTER_LEFT);
        autoDetectRow.setPadding(new Insets(6, 0, 0, 0));

        content.getChildren().addAll(toolbar, table, descLabel, autoDetectRow);
        return content;
    }
}
