package com.roze.dbnavigator.ui.tools;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;

/**
 * Settings panel for Tools > External Tools.
 * Matches DataGrip screenshot media_1791726288403_ab27a7a8.png.
 */
public class ExternalToolsPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public ExternalToolsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.ExternalToolsConfig cfg = settings.getExternalTools();
        ObservableList<AppSettingsStore.ExternalToolItem> toolList =
                FXCollections.observableArrayList(new ArrayList<>(cfg.getTools()));
        inputs.put("tools_external_tools_list", toolList);

        VBox content = new VBox(0);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        // Toolbar
        ToolBar toolBar = new ToolBar();
        toolBar.setStyle("-fx-background-color: -surface; -fx-border-color: -border-subtle; -fx-border-width: 1 1 0 1; -fx-border-radius: 4 4 0 0; -fx-background-radius: 4 4 0 0;");

        Button addBtn = new Button("+");
        addBtn.setTooltip(new Tooltip("Add (Alt+Insert)"));

        Button removeBtn = new Button("-");
        removeBtn.setTooltip(new Tooltip("Remove (Delete)"));

        Button editBtn = new Button("✏");
        editBtn.setTooltip(new Tooltip("Edit (Enter)"));

        Button upBtn = new Button("▲");
        upBtn.setTooltip(new Tooltip("Move Up (Alt+Up)"));

        Button downBtn = new Button("▼");
        downBtn.setTooltip(new Tooltip("Move Down (Alt+Down)"));

        Button dupBtn = new Button("⧉");
        dupBtn.setTooltip(new Tooltip("Duplicate (Ctrl+D)"));

        toolBar.getItems().addAll(addBtn, removeBtn, editBtn, upBtn, downBtn, dupBtn);

        // TreeView or Center StackPane
        TreeItem<Object> root = new TreeItem<>("Root");
        root.setExpanded(true);

        TreeView<Object> treeView = new TreeView<>(root);
        treeView.setShowRoot(false);
        treeView.setStyle("-fx-background-color: -surface; -fx-border-color: -border-subtle; -fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 4 4; -fx-background-radius: 0 0 4 4;");
        VBox.setVgrow(treeView, Priority.ALWAYS);

        Label emptyLabel = new Label("No external tools configured");
        emptyLabel.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px;");

        StackPane centerContainer = new StackPane(treeView, emptyLabel);
        VBox.setVgrow(centerContainer, Priority.ALWAYS);

        // Rebuild tree
        Runnable refreshTree = () -> {
            root.getChildren().clear();
            if (toolList.isEmpty()) {
                emptyLabel.setVisible(true);
                treeView.setVisible(false);
                removeBtn.setDisable(true);
                editBtn.setDisable(true);
                upBtn.setDisable(true);
                downBtn.setDisable(true);
                dupBtn.setDisable(true);
            } else {
                emptyLabel.setVisible(false);
                treeView.setVisible(true);

                // Group tools
                Map<String, List<AppSettingsStore.ExternalToolItem>> grouped = new LinkedHashMap<>();
                for (AppSettingsStore.ExternalToolItem item : toolList) {
                    String g = item.getGroup() != null && !item.getGroup().isBlank() ? item.getGroup() : "External Tools";
                    grouped.computeIfAbsent(g, k -> new ArrayList<>()).add(item);
                }

                for (Map.Entry<String, List<AppSettingsStore.ExternalToolItem>> entry : grouped.entrySet()) {
                    TreeItem<Object> groupItem = new TreeItem<>(entry.getKey());
                    groupItem.setExpanded(true);
                    for (AppSettingsStore.ExternalToolItem tool : entry.getValue()) {
                        groupItem.getChildren().add(new TreeItem<>(tool));
                    }
                    root.getChildren().add(groupItem);
                }
            }
        };

        refreshTree.run();

        // Selection handling
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasToolSelected = newVal != null && newVal.getValue() instanceof AppSettingsStore.ExternalToolItem;
            removeBtn.setDisable(!hasToolSelected);
            editBtn.setDisable(!hasToolSelected);
            upBtn.setDisable(!hasToolSelected);
            downBtn.setDisable(!hasToolSelected);
            dupBtn.setDisable(!hasToolSelected);
        });

        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (item instanceof AppSettingsStore.ExternalToolItem ti) {
                    setText(ti.getName() + (ti.getDescription().isBlank() ? "" : " (" + ti.getDescription() + ")"));
                    setStyle("-fx-text-fill: -text;");
                } else {
                    setText(item.toString());
                    setStyle("-fx-text-fill: -text; -fx-font-weight: bold;");
                }
            }
        });

        // Add
        addBtn.setOnAction(e -> {
            Stage stage = (Stage) content.getScene().getWindow();
            new CreateToolDialog(null, false).showAndWait(stage).ifPresent(newTool -> {
                toolList.add(newTool);
                refreshTree.run();
            });
        });

        // Remove
        removeBtn.setOnAction(e -> {
            TreeItem<Object> sel = treeView.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getValue() instanceof AppSettingsStore.ExternalToolItem ti) {
                toolList.remove(ti);
                refreshTree.run();
            }
        });

        // Edit
        editBtn.setOnAction(e -> {
            TreeItem<Object> sel = treeView.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getValue() instanceof AppSettingsStore.ExternalToolItem ti) {
                Stage stage = (Stage) content.getScene().getWindow();
                new CreateToolDialog(ti.copy(), true).showAndWait(stage).ifPresent(updated -> {
                    int idx = toolList.indexOf(ti);
                    if (idx >= 0) {
                        toolList.set(idx, updated);
                        refreshTree.run();
                    }
                });
            }
        });

        // Double click to edit
        treeView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                editBtn.fire();
            }
        });

        // Up
        upBtn.setOnAction(e -> {
            TreeItem<Object> sel = treeView.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getValue() instanceof AppSettingsStore.ExternalToolItem ti) {
                int idx = toolList.indexOf(ti);
                if (idx > 0) {
                    toolList.remove(idx);
                    toolList.add(idx - 1, ti);
                    refreshTree.run();
                }
            }
        });

        // Down
        downBtn.setOnAction(e -> {
            TreeItem<Object> sel = treeView.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getValue() instanceof AppSettingsStore.ExternalToolItem ti) {
                int idx = toolList.indexOf(ti);
                if (idx >= 0 && idx < toolList.size() - 1) {
                    toolList.remove(idx);
                    toolList.add(idx + 1, ti);
                    refreshTree.run();
                }
            }
        });

        // Duplicate
        dupBtn.setOnAction(e -> {
            TreeItem<Object> sel = treeView.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getValue() instanceof AppSettingsStore.ExternalToolItem ti) {
                AppSettingsStore.ExternalToolItem copy = ti.copy();
                copy.setName(ti.getName() + " (Copy)");
                int idx = toolList.indexOf(ti);
                if (idx >= 0) toolList.add(idx + 1, copy);
                else toolList.add(copy);
                refreshTree.run();
            }
        });

        content.getChildren().addAll(toolBar, centerContainer);
        return content;
    }
}
