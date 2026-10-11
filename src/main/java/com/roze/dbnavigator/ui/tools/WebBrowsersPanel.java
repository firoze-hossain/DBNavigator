package com.roze.dbnavigator.ui.tools;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Settings panel for Tools > Web Browsers and Preview.
 * Matches DataGrip screenshot media_1791728433184_a2263c3e.png.
 */
public class WebBrowsersPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;
    private final ObservableList<AppSettingsStore.WebBrowserItem> workingList = FXCollections.observableArrayList();

    public WebBrowsersPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.WebBrowsersConfig cfg = settings.getWebBrowsers();

        // Load working copy of browsers
        workingList.clear();
        for (AppSettingsStore.WebBrowserItem item : cfg.getBrowsers()) {
            workingList.add(item.copy());
        }
        inputs.put("tools_browsers_list", workingList);

        VBox content = new VBox(14);
        content.setPadding(new Insets(10, 16, 20, 16));
        content.setStyle("-fx-background-color: transparent;");

        // --- Toolbar ---
        Button addBtn = new Button("+");
        addBtn.setTooltip(new Tooltip("Add Browser"));
        addBtn.setStyle("-fx-font-weight: bold; -fx-padding: 3 8 3 8;");

        Button removeBtn = new Button("-");
        removeBtn.setTooltip(new Tooltip("Remove Selected Browser"));
        removeBtn.setStyle("-fx-font-weight: bold; -fx-padding: 3 8 3 8;");
        removeBtn.setDisable(true);

        Button editBtn = new Button("✏");
        editBtn.setTooltip(new Tooltip("Edit Browser"));
        editBtn.setStyle("-fx-padding: 3 7 3 7;");
        editBtn.setDisable(true);

        Button moveUpBtn = new Button("↑");
        moveUpBtn.setTooltip(new Tooltip("Move Up"));
        moveUpBtn.setStyle("-fx-padding: 3 7 3 7;");
        moveUpBtn.setDisable(true);

        Button moveDownBtn = new Button("↓");
        moveDownBtn.setTooltip(new Tooltip("Move Down"));
        moveDownBtn.setStyle("-fx-padding: 3 7 3 7;");
        moveDownBtn.setDisable(true);

        Button copyBtn = new Button("⤢");
        copyBtn.setTooltip(new Tooltip("Duplicate Browser"));
        copyBtn.setStyle("-fx-padding: 3 7 3 7;");
        copyBtn.setDisable(true);

        HBox toolbar = new HBox(4, addBtn, removeBtn, editBtn, moveUpBtn, moveDownBtn, copyBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // --- TableView ---
        TableView<AppSettingsStore.WebBrowserItem> table = new TableView<>(workingList);
        table.setEditable(true);
        table.setPrefHeight(190);
        table.setStyle("-fx-background-color: -control-bg; -fx-border-color: -border-color; -fx-border-radius: 4px;");

        // Column 1: Active CheckBox + Name
        TableColumn<AppSettingsStore.WebBrowserItem, Boolean> activeCol = new TableColumn<>("");
        activeCol.setPrefWidth(45);
        activeCol.setEditable(true);
        activeCol.setCellValueFactory(param -> {
            SimpleBooleanProperty prop = new SimpleBooleanProperty(param.getValue().isActive());
            prop.addListener((obs, oldV, newV) -> param.getValue().setActive(newV));
            return prop;
        });
        activeCol.setCellFactory(CheckBoxTableCell.forTableColumn(activeCol));

        TableColumn<AppSettingsStore.WebBrowserItem, String> nameCol = new TableColumn<>("Name");
        nameCol.setPrefWidth(160);
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<AppSettingsStore.WebBrowserItem, String> familyCol = new TableColumn<>("Family");
        familyCol.setPrefWidth(160);
        familyCol.setCellValueFactory(new PropertyValueFactory<>("family"));
        familyCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String family, boolean empty) {
                super.updateItem(family, empty);
                if (empty || family == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(family);
                    // Add badge/icon
                    Label icon = new Label(getFamilySymbol(family));
                    icon.setStyle(getFamilyIconStyle(family));
                    setGraphic(icon);
                    setStyle("-fx-text-fill: -text; -fx-font-size: 12px;");
                }
            }
        });

        TableColumn<AppSettingsStore.WebBrowserItem, String> pathCol = new TableColumn<>("Path");
        pathCol.setPrefWidth(320);
        pathCol.setCellValueFactory(new PropertyValueFactory<>("path"));
        pathCol.setStyle("-fx-text-fill: -text; -fx-font-size: 12px;");

        table.getColumns().addAll(activeCol, nameCol, familyCol, pathCol);
        VBox.setVgrow(table, Priority.NEVER);

        // Selection listener
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean hasSel = newV != null;
            removeBtn.setDisable(!hasSel);
            editBtn.setDisable(!hasSel);
            copyBtn.setDisable(!hasSel);

            int idx = table.getSelectionModel().getSelectedIndex();
            moveUpBtn.setDisable(!hasSel || idx <= 0);
            moveDownBtn.setDisable(!hasSel || idx >= workingList.size() - 1);
        });

        // Add action
        addBtn.setOnAction(e -> showEditBrowserDialog(null, table));

        // Edit action
        editBtn.setOnAction(e -> {
            AppSettingsStore.WebBrowserItem selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) showEditBrowserDialog(selected, table);
        });

        // Remove action
        removeBtn.setOnAction(e -> {
            AppSettingsStore.WebBrowserItem selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                int idx = workingList.indexOf(selected);
                workingList.remove(selected);
                if (!workingList.isEmpty()) {
                    table.getSelectionModel().select(Math.min(idx, workingList.size() - 1));
                }
            }
        });

        // Move Up
        moveUpBtn.setOnAction(e -> {
            int idx = table.getSelectionModel().getSelectedIndex();
            if (idx > 0) {
                AppSettingsStore.WebBrowserItem item = workingList.remove(idx);
                workingList.add(idx - 1, item);
                table.getSelectionModel().select(idx - 1);
            }
        });

        // Move Down
        moveDownBtn.setOnAction(e -> {
            int idx = table.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < workingList.size() - 1) {
                AppSettingsStore.WebBrowserItem item = workingList.remove(idx);
                workingList.add(idx + 1, item);
                table.getSelectionModel().select(idx + 1);
            }
        });

        // Duplicate
        copyBtn.setOnAction(e -> {
            AppSettingsStore.WebBrowserItem selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                AppSettingsStore.WebBrowserItem copy = selected.copy();
                copy.setId(UUID.randomUUID().toString());
                copy.setName(selected.getName() + " copy");
                workingList.add(copy);
                table.getSelectionModel().select(copy);
            }
        });

        // Select first item if available
        if (!workingList.isEmpty()) {
            table.getSelectionModel().select(0);
        }

        // --- Default Browser row ---
        Label defBrowserLbl = new Label("Default Browser:");
        defBrowserLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ObservableList<String> defaultBrowserOptions = FXCollections.observableArrayList(
                "System default", "First listed", "Custom path", "Chrome", "Firefox", "Safari", "Opera", "Internet Explorer", "Edge"
        );
        ComboBox<String> defBrowserCombo = new ComboBox<>(defaultBrowserOptions);
        defBrowserCombo.setValue(cfg.getDefaultBrowser());
        defBrowserCombo.setPrefWidth(160);
        inputs.put("tools_browsers_defaultBrowser", defBrowserCombo);

        TextField customPathField = new TextField(cfg.getCustomBrowserPath());
        customPathField.setPrefWidth(300);
        inputs.put("tools_browsers_customPath", customPathField);

        Button browseCustomPathBtn = new Button("📁");
        browseCustomPathBtn.setTooltip(new Tooltip("Browse for custom browser executable"));
        browseCustomPathBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Browser Executable");
            File file = chooser.showOpenDialog(browseCustomPathBtn.getScene().getWindow());
            if (file != null) customPathField.setText(file.getAbsolutePath());
        });

        HBox defaultBrowserRow = new HBox(8, defBrowserLbl, defBrowserCombo, customPathField, browseCustomPathBtn);
        defaultBrowserRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(customPathField, Priority.ALWAYS);

        // --- Section: Show browser popup in the editor ---
        Label popupSectionTitle = new Label("Show browser popup in the editor");
        popupSectionTitle.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-font-weight: bold;");

        CheckBox htmlPopupCheck = new CheckBox("For HTML files");
        htmlPopupCheck.setSelected(cfg.isShowPopupForHtml());
        htmlPopupCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_browsers_popupHtml", htmlPopupCheck);

        CheckBox xmlPopupCheck = new CheckBox("For XML files");
        xmlPopupCheck.setSelected(cfg.isShowPopupForXml());
        xmlPopupCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_browsers_popupXml", xmlPopupCheck);

        VBox popupSection = new VBox(6, popupSectionTitle, htmlPopupCheck, xmlPopupCheck);
        popupSection.setPadding(new Insets(6, 0, 0, 0));

        // --- Section: Reload behavior ---
        Label reloadSectionTitle = new Label("Reload behavior");
        reloadSectionTitle.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-font-weight: bold;");

        Separator reloadSep = new Separator();
        HBox.setHgrow(reloadSep, Priority.ALWAYS);
        HBox reloadHeader = new HBox(8, reloadSectionTitle, reloadSep);
        reloadHeader.setAlignment(Pos.CENTER_LEFT);

        GridPane reloadGrid = new GridPane();
        reloadGrid.setHgap(12);
        reloadGrid.setVgap(10);
        reloadGrid.setPadding(new Insets(4, 0, 0, 0));

        Label reloadBrowserLbl = new Label("Reload page in browser:");
        reloadBrowserLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ComboBox<String> reloadBrowserCombo = new ComboBox<>(FXCollections.observableArrayList("On Save", "On changes in IDE", "Disabled"));
        reloadBrowserCombo.setValue(cfg.getReloadPageInBrowser());
        reloadBrowserCombo.setPrefWidth(140);
        inputs.put("tools_browsers_reloadBrowser", reloadBrowserCombo);

        Label reloadPreviewLbl = new Label("Reload page in built-in preview:");
        reloadPreviewLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ComboBox<String> reloadPreviewCombo = new ComboBox<>(FXCollections.observableArrayList("On Save", "On changes in IDE", "Disabled"));
        reloadPreviewCombo.setValue(cfg.getReloadPageInBuiltInPreview());
        reloadPreviewCombo.setPrefWidth(140);
        inputs.put("tools_browsers_reloadPreview", reloadPreviewCombo);

        reloadGrid.add(reloadBrowserLbl, 0, 0);
        reloadGrid.add(reloadBrowserCombo, 1, 0);
        reloadGrid.add(reloadPreviewLbl, 0, 1);
        reloadGrid.add(reloadPreviewCombo, 1, 1);

        VBox reloadSection = new VBox(8, reloadHeader, reloadGrid);

        // --- Section: Built-in Server ---
        Label serverSectionTitle = new Label("Built-in Server");
        serverSectionTitle.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-font-weight: bold;");

        Separator serverSep = new Separator();
        HBox.setHgrow(serverSep, Priority.ALWAYS);
        HBox serverHeader = new HBox(8, serverSectionTitle, serverSep);
        serverHeader.setAlignment(Pos.CENTER_LEFT);

        Label portLbl = new Label("Port:");
        portLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        Spinner<Integer> portSpinner = new Spinner<>(1024, 65535, cfg.getBuiltInServerPort());
        portSpinner.setEditable(true);
        portSpinner.setPrefWidth(100);
        inputs.put("tools_browsers_serverPort", portSpinner);

        Label portHelp = new Label("❓");
        portHelp.setTooltip(new Tooltip("Local HTTP server port used to serve project files for debugging and browser previews."));

        HBox portBox = new HBox(8, portLbl, portSpinner, portHelp);
        portBox.setAlignment(Pos.CENTER_LEFT);

        CheckBox acceptExternalCheck = new CheckBox("Can accept external connections");
        acceptExternalCheck.setSelected(cfg.isAcceptExternalConnections());
        acceptExternalCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_browsers_acceptExternal", acceptExternalCheck);

        CheckBox allowUnsignedCheck = new CheckBox("Allow unsigned requests");
        allowUnsignedCheck.setSelected(cfg.isAllowUnsignedRequests());
        allowUnsignedCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_browsers_allowUnsigned", allowUnsignedCheck);

        VBox serverSection = new VBox(8, serverHeader, portBox, acceptExternalCheck, allowUnsignedCheck);

        content.getChildren().addAll(
                toolbar,
                table,
                defaultBrowserRow,
                popupSection,
                reloadSection,
                serverSection
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private void showEditBrowserDialog(AppSettingsStore.WebBrowserItem existing, TableView<AppSettingsStore.WebBrowserItem> table) {
        Dialog<AppSettingsStore.WebBrowserItem> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Web Browser" : "Edit Web Browser");
        dialog.setHeaderText(existing == null ? "Add a new browser configuration" : "Edit browser configuration");

        ButtonType saveBtnType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        ComboBox<String> familyCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Chrome", "Firefox", "Safari", "Internet Explorer"
        ));
        familyCombo.setValue(existing != null ? existing.getFamily() : "Chrome");

        TextField pathField = new TextField(existing != null ? existing.getPath() : "");
        Button browseBtn = new Button("📁");
        browseBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Browser Executable");
            File file = chooser.showOpenDialog(browseBtn.getScene().getWindow());
            if (file != null) pathField.setText(file.getAbsolutePath());
        });
        HBox pathBox = new HBox(6, pathField, browseBtn);
        HBox.setHgrow(pathField, Priority.ALWAYS);

        CheckBox activeCheck = new CheckBox("Active");
        activeCheck.setSelected(existing != null && existing.isActive());

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Family:"), 0, 1);
        grid.add(familyCombo, 1, 1);
        grid.add(new Label("Path:"), 0, 2);
        grid.add(pathBox, 1, 2);
        grid.add(activeCheck, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtnType) {
                String name = nameField.getText().trim();
                String family = familyCombo.getValue();
                String path = pathField.getText().trim();
                boolean active = activeCheck.isSelected();

                if (existing != null) {
                    existing.setName(name);
                    existing.setFamily(family);
                    existing.setPath(path);
                    existing.setActive(active);
                    return existing;
                } else {
                    return new AppSettingsStore.WebBrowserItem(active, name.isBlank() ? "Browser" : name, family, path);
                }
            }
            return null;
        });

        Optional<AppSettingsStore.WebBrowserItem> result = dialog.showAndWait();
        result.ifPresent(item -> {
            if (existing == null) {
                workingList.add(item);
                table.getSelectionModel().select(item);
            } else {
                table.refresh();
            }
        });
    }

    private static String getFamilySymbol(String family) {
        if ("Chrome".equalsIgnoreCase(family)) return "🌐";
        if ("Firefox".equalsIgnoreCase(family)) return "🦊";
        if ("Safari".equalsIgnoreCase(family)) return "🧭";
        if ("Internet Explorer".equalsIgnoreCase(family)) return "🌐";
        return "🌐";
    }

    private static String getFamilyIconStyle(String family) {
        if ("Chrome".equalsIgnoreCase(family)) return "-fx-text-fill: #4285F4; -fx-padding: 0 4 0 0;";
        if ("Firefox".equalsIgnoreCase(family)) return "-fx-text-fill: #FF7139; -fx-padding: 0 4 0 0;";
        if ("Safari".equalsIgnoreCase(family)) return "-fx-text-fill: #006CFF; -fx-padding: 0 4 0 0;";
        if ("Internet Explorer".equalsIgnoreCase(family)) return "-fx-text-fill: #0076D6; -fx-padding: 0 4 0 0;";
        return "-fx-text-fill: -text-dim; -fx-padding: 0 4 0 0;";
    }
}
