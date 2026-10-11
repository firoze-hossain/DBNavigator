package com.roze.dbnavigator.ui.tools;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;

/**
 * Settings panel for Tools > Diff & Merge > External Diff Tools.
 * Matches DataGrip screenshot media_1791726256433_0372d2bb.png.
 */
public class ExternalDiffToolsPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public ExternalDiffToolsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.ExternalDiffToolsConfig cfg = settings.getExternalDiffTools();

        ObservableList<AppSettingsStore.ExternalToolDefinition> customTools =
                FXCollections.observableArrayList(new ArrayList<>(cfg.getCustomTools()));
        inputs.put("tools_external_diff_customTools", customTools);

        ObservableList<AppSettingsStore.FileTypeToolAssociation> associations =
                FXCollections.observableArrayList(new ArrayList<>(cfg.getAssociations()));
        inputs.put("tools_external_diff_associations", associations);

        VBox content = new VBox(12);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");

        // Top Checkbox
        CheckBox enableExternalCheck = new CheckBox("Enable external tools");
        enableExternalCheck.setSelected(cfg.isEnableExternalTools());
        enableExternalCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_external_diff_enable", enableExternalCheck);

        // Body container that gets disabled when enableExternalCheck is unchecked
        VBox body = new VBox(14);
        body.disableProperty().bind(enableExternalCheck.selectedProperty().not());
        VBox.setVgrow(body, Priority.ALWAYS);

        // ---------------------------------------------------------------------
        // Section 1: Configure external tools:
        // ---------------------------------------------------------------------
        Label sec1Label = new Label("Configure external tools:");
        sec1Label.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px;");

        ToolBar tb1 = new ToolBar();
        tb1.setStyle("-fx-background-color: -surface; -fx-border-color: -border-subtle; -fx-border-width: 1 1 0 1; -fx-border-radius: 4 4 0 0; -fx-background-radius: 4 4 0 0;");

        Button addToolBtn = new Button("+");
        addToolBtn.setTooltip(new Tooltip("Add External Diff Tool"));

        Button removeToolBtn = new Button("-");
        removeToolBtn.setTooltip(new Tooltip("Remove"));

        Button editToolBtn = new Button("✏");
        editToolBtn.setTooltip(new Tooltip("Edit"));

        tb1.getItems().addAll(addToolBtn, removeToolBtn, editToolBtn);

        TableView<AppSettingsStore.ExternalToolDefinition> toolsTable = new TableView<>(customTools);
        toolsTable.setPrefHeight(150);
        toolsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        toolsTable.setStyle("-fx-background-color: -surface; -fx-border-color: -border-subtle; -fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 4 4; -fx-background-radius: 0 0 4 4;");

        Label emptyLabel1 = new Label("Nothing to show");
        emptyLabel1.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px;");
        toolsTable.setPlaceholder(emptyLabel1);

        TableColumn<AppSettingsStore.ExternalToolDefinition, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(140);

        TableColumn<AppSettingsStore.ExternalToolDefinition, String> pathCol = new TableColumn<>("Program Path");
        pathCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProgramPath()));
        pathCol.setPrefWidth(220);

        TableColumn<AppSettingsStore.ExternalToolDefinition, String> diffParamsCol = new TableColumn<>("Diff Parameters");
        diffParamsCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDiffParameters()));
        diffParamsCol.setPrefWidth(120);

        TableColumn<AppSettingsStore.ExternalToolDefinition, String> mergeParamsCol = new TableColumn<>("Merge Parameters");
        mergeParamsCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMergeParameters()));
        mergeParamsCol.setPrefWidth(140);

        toolsTable.getColumns().addAll(nameCol, pathCol, diffParamsCol, mergeParamsCol);

        // Selection handling
        toolsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean hasSel = newV != null;
            removeToolBtn.setDisable(!hasSel);
            editToolBtn.setDisable(!hasSel);
        });
        removeToolBtn.setDisable(true);
        editToolBtn.setDisable(true);

        // Available tool names for section 2
        ObservableList<String> availableToolNames = FXCollections.observableArrayList("Built-in");
        Runnable updateAvailableNames = () -> {
            availableToolNames.clear();
            availableToolNames.add("Built-in");
            for (AppSettingsStore.ExternalToolDefinition td : customTools) {
                if (td.getName() != null && !td.getName().isBlank()) {
                    availableToolNames.add(td.getName());
                }
            }
        };
        updateAvailableNames.run();

        // Add tool action
        addToolBtn.setOnAction(e -> {
            Stage stage = (Stage) content.getScene().getWindow();
            showEditExternalToolDialog(stage, null).ifPresent(tool -> {
                customTools.add(tool);
                updateAvailableNames.run();
            });
        });

        // Edit tool action
        editToolBtn.setOnAction(e -> {
            AppSettingsStore.ExternalToolDefinition sel = toolsTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                Stage stage = (Stage) content.getScene().getWindow();
                showEditExternalToolDialog(stage, sel.copy()).ifPresent(updated -> {
                    int idx = customTools.indexOf(sel);
                    if (idx >= 0) {
                        customTools.set(idx, updated);
                        updateAvailableNames.run();
                    }
                });
            }
        });

        removeToolBtn.setOnAction(e -> {
            AppSettingsStore.ExternalToolDefinition sel = toolsTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                customTools.remove(sel);
                updateAvailableNames.run();
            }
        });

        VBox sec1 = new VBox(4, sec1Label, tb1, toolsTable);

        // ---------------------------------------------------------------------
        // Section 2: Configure external diff/merge tools associated with a file type:
        // ---------------------------------------------------------------------
        Label sec2Label = new Label("Configure external diff/merge tools associated with a file type:");
        sec2Label.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px; -fx-padding: 8 0 0 0;");

        ToolBar tb2 = new ToolBar();
        tb2.setStyle("-fx-background-color: -surface; -fx-border-color: -border-subtle; -fx-border-width: 1 1 0 1; -fx-border-radius: 4 4 0 0; -fx-background-radius: 4 4 0 0;");

        Button addAssocBtn = new Button("+");
        addAssocBtn.setTooltip(new Tooltip("Add Association"));

        Button removeAssocBtn = new Button("-");
        removeAssocBtn.setTooltip(new Tooltip("Remove Association"));

        tb2.getItems().addAll(addAssocBtn, removeAssocBtn);

        TableView<AppSettingsStore.FileTypeToolAssociation> assocTable = new TableView<>(associations);
        assocTable.setEditable(true);
        assocTable.setPrefHeight(180);
        assocTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        assocTable.setStyle("-fx-background-color: -surface; -fx-border-color: -border-subtle; -fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 4 4; -fx-background-radius: 0 0 4 4;");
        VBox.setVgrow(assocTable, Priority.ALWAYS);

        TableColumn<AppSettingsStore.FileTypeToolAssociation, String> fileTypeCol = new TableColumn<>("File Type");
        fileTypeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFileType()));
        fileTypeCol.setCellFactory(TextFieldTableCell.forTableColumn());
        fileTypeCol.setOnEditCommit(evt -> evt.getRowValue().setFileType(evt.getNewValue()));
        fileTypeCol.setPrefWidth(220);

        TableColumn<AppSettingsStore.FileTypeToolAssociation, String> diffToolCol = new TableColumn<>("Diff Tool");
        diffToolCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDiffTool()));
        diffToolCol.setCellFactory(ComboBoxTableCell.forTableColumn(availableToolNames));
        diffToolCol.setOnEditCommit(evt -> evt.getRowValue().setDiffTool(evt.getNewValue()));
        diffToolCol.setPrefWidth(200);

        TableColumn<AppSettingsStore.FileTypeToolAssociation, String> mergeToolCol = new TableColumn<>("Merge Tool");
        mergeToolCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMergeTool()));
        mergeToolCol.setCellFactory(ComboBoxTableCell.forTableColumn(availableToolNames));
        mergeToolCol.setOnEditCommit(evt -> evt.getRowValue().setMergeTool(evt.getNewValue()));
        mergeToolCol.setPrefWidth(200);

        assocTable.getColumns().addAll(fileTypeCol, diffToolCol, mergeToolCol);

        assocTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            // Cannot remove "Default"
            boolean canRemove = newV != null && !"Default".equalsIgnoreCase(newV.getFileType());
            removeAssocBtn.setDisable(!canRemove);
        });
        removeAssocBtn.setDisable(true);

        addAssocBtn.setOnAction(e -> {
            TextInputDialog tid = new TextInputDialog("*.sql");
            tid.setTitle("New File Type Association");
            tid.setHeaderText("Enter file type pattern (e.g. *.sql, *.json, XML):");
            tid.showAndWait().ifPresent(ft -> {
                if (!ft.trim().isEmpty()) {
                    associations.add(new AppSettingsStore.FileTypeToolAssociation(ft.trim(), "Built-in", "Built-in"));
                }
            });
        });

        removeAssocBtn.setOnAction(e -> {
            AppSettingsStore.FileTypeToolAssociation sel = assocTable.getSelectionModel().getSelectedItem();
            if (sel != null && !"Default".equalsIgnoreCase(sel.getFileType())) {
                associations.remove(sel);
            }
        });

        VBox sec2 = new VBox(4, sec2Label, tb2, assocTable);
        VBox.setVgrow(sec2, Priority.ALWAYS);

        body.getChildren().addAll(sec1, sec2);

        content.getChildren().addAll(enableExternalCheck, body);
        return content;
    }

    private Optional<AppSettingsStore.ExternalToolDefinition> showEditExternalToolDialog(
            Stage owner, AppSettingsStore.ExternalToolDefinition existing) {
        Dialog<AppSettingsStore.ExternalToolDefinition> d = new Dialog<>();
        d.initOwner(owner);
        d.setTitle(existing != null ? "Edit Diff Tool" : "Add Diff Tool");

        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        TextField pathField = new TextField(existing != null ? existing.getProgramPath() : "");
        TextField diffField = new TextField(existing != null ? existing.getDiffParameters() : "%1 %2");
        TextField mergeField = new TextField(existing != null ? existing.getMergeParameters() : "%1 %2 %3 %4");

        Button browseBtn = new Button("Browse...");
        browseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Diff/Merge Executable");
            File f = fc.showOpenDialog(d.getOwner());
            if (f != null) pathField.setText(f.getAbsolutePath());
        });

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(14));

        g.add(new Label("Tool Name:"), 0, 0);
        g.add(nameField, 1, 0);

        g.add(new Label("Executable Path:"), 0, 1);
        HBox pathBox = new HBox(6, pathField, browseBtn);
        HBox.setHgrow(pathField, Priority.ALWAYS);
        g.add(pathBox, 1, 1);

        g.add(new Label("Diff Parameters:"), 0, 2);
        g.add(diffField, 1, 2);

        g.add(new Label("Merge Parameters:"), 0, 3);
        g.add(mergeField, 1, 3);

        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        d.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !nameField.getText().trim().isEmpty()) {
                return new AppSettingsStore.ExternalToolDefinition(
                        nameField.getText().trim(),
                        pathField.getText().trim(),
                        diffField.getText().trim(),
                        mergeField.getText().trim()
                );
            }
            return null;
        });

        return d.showAndWait();
    }
}
