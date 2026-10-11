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
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Settings panel for Languages > Schemas and DTDs > JSON Schema Mappings.
 * Matches DataGrip screenshots media_1791719601962_e3fe875b.png and media_1791719631790_c7a56122.png.
 */
public class JsonSchemaMappingsPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public JsonSchemaMappingsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.JsonSchemaMappingsConfig cfg = settings.getJsonSchemaMappings();

        ObservableList<AppSettingsStore.JsonSchemaMappingItem> schemaItems = FXCollections.observableArrayList();
        for (AppSettingsStore.JsonSchemaMappingItem item : cfg.getMappings()) {
            if (item != null) schemaItems.add(item.copy());
        }
        inputs.put("languages_json_schemaMappings", schemaItems);

        // --- Left Master List Pane ---
        Button addSchemaBtn = new Button("+");
        addSchemaBtn.setTooltip(new Tooltip("Add JSON Schema Mapping"));
        addSchemaBtn.setStyle("-fx-min-width: 28px; -fx-min-height: 24px; -fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");

        Button removeSchemaBtn = new Button("—");
        removeSchemaBtn.setTooltip(new Tooltip("Remove"));
        removeSchemaBtn.setStyle("-fx-min-width: 28px; -fx-min-height: 24px; -fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        removeSchemaBtn.setDisable(true);

        HBox leftToolbar = new HBox(4, addSchemaBtn, removeSchemaBtn);
        leftToolbar.setAlignment(Pos.CENTER_LEFT);
        leftToolbar.setPadding(new Insets(0, 0, 4, 0));

        ListView<AppSettingsStore.JsonSchemaMappingItem> schemaListView = new ListView<>(schemaItems);
        schemaListView.setStyle("-fx-background-color: #1e1f22; -fx-control-inner-background: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        schemaListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AppSettingsStore.JsonSchemaMappingItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName());
                    setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
                }
            }
        });

        Label emptyListPlaceholder = new Label("Nothing to show");
        emptyListPlaceholder.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px;");
        schemaListView.setPlaceholder(emptyListPlaceholder);
        VBox.setVgrow(schemaListView, Priority.ALWAYS);

        VBox leftPane = new VBox(6, leftToolbar, schemaListView);
        leftPane.setPrefWidth(240);
        leftPane.setMinWidth(180);
        leftPane.setMaxWidth(350);

        // --- Right Detail Pane ---
        StackPane rightPaneContainer = new StackPane();
        VBox.setVgrow(rightPaneContainer, Priority.ALWAYS);
        HBox.setHgrow(rightPaneContainer, Priority.ALWAYS);

        Label emptyDetailPlaceholder = new Label("Please add a JSON Schema file and configure its usage");
        emptyDetailPlaceholder.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px;");
        StackPane emptyDetailBox = new StackPane(emptyDetailPlaceholder);
        emptyDetailBox.setStyle("-fx-background-color: transparent;");

        rightPaneContainer.getChildren().add(emptyDetailBox);

        // Selection handling
        schemaListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            removeSchemaBtn.setDisable(!hasSelection);
            rightPaneContainer.getChildren().clear();
            if (hasSelection) {
                rightPaneContainer.getChildren().add(buildDetailEditor(newVal, schemaListView));
            } else {
                rightPaneContainer.getChildren().add(emptyDetailBox);
            }
        });

        addSchemaBtn.setOnAction(e -> {
            String defaultName = "New Schema";
            int count = 1;
            boolean exists;
            do {
                final String cand = count == 1 ? defaultName : defaultName + " (" + count + ")";
                exists = schemaItems.stream().anyMatch(item -> cand.equals(item.getName()));
                if (!exists) defaultName = cand;
                count++;
            } while (exists);

            AppSettingsStore.JsonSchemaMappingItem newItem = new AppSettingsStore.JsonSchemaMappingItem(defaultName, "", "JSON Schema v4");
            schemaItems.add(newItem);
            schemaListView.getSelectionModel().select(newItem);
        });

        removeSchemaBtn.setOnAction(e -> {
            AppSettingsStore.JsonSchemaMappingItem selected = schemaListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                schemaItems.remove(selected);
            }
        });

        if (!schemaItems.isEmpty()) {
            schemaListView.getSelectionModel().select(0);
        }

        SplitPane splitPane = new SplitPane(leftPane, rightPaneContainer);
        splitPane.setDividerPositions(0.28);
        splitPane.setStyle("-fx-background-color: transparent; -fx-box-border: transparent;");
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        VBox content = new VBox(splitPane);
        content.setPadding(new Insets(10, 16, 16, 14));
        content.setStyle("-fx-background-color: transparent;");
        return content;
    }

    private Node buildDetailEditor(AppSettingsStore.JsonSchemaMappingItem item, ListView<AppSettingsStore.JsonSchemaMappingItem> parentList) {
        VBox detailBox = new VBox(12);
        detailBox.setPadding(new Insets(4, 8, 8, 12));
        detailBox.setStyle("-fx-background-color: transparent;");

        // Row 1: Name
        Label nameLabel = new Label("Name:");
        nameLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-min-width: 140px;");
        TextField nameField = new TextField(item.getName());
        nameField.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        HBox.setHgrow(nameField, Priority.ALWAYS);
        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            item.setName(newVal != null ? newVal.trim() : "");
            parentList.refresh();
        });
        HBox nameRow = new HBox(8, nameLabel, nameField);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        // Row 2: Schema file or URL
        Label schemaUrlLabel = new Label("Schema file or URL:");
        schemaUrlLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-min-width: 140px;");

        TextField schemaUrlField = new TextField(item.getSchemaFileOrUrl());
        schemaUrlField.setPromptText("Local file path or URL");
        schemaUrlField.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        HBox.setHgrow(schemaUrlField, Priority.ALWAYS);
        schemaUrlField.textProperty().addListener((obs, oldVal, newVal) -> item.setSchemaFileOrUrl(newVal != null ? newVal.trim() : ""));

        Button browseFileBtn = new Button("📁");
        browseFileBtn.setTooltip(new Tooltip("Browse local JSON Schema file"));
        browseFileBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-min-width: 30px;");
        browseFileBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select JSON Schema File");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("JSON Schema Files (*.json, *.schema.json)", "*.json", "*.schema.json"),
                    new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
            );
            Window win = browseFileBtn.getScene() != null ? browseFileBtn.getScene().getWindow() : null;
            File f = chooser.showOpenDialog(win);
            if (f != null) {
                schemaUrlField.setText(f.getAbsolutePath());
            }
        });

        Button globeBtn = new Button("🌐");
        globeBtn.setTooltip(new Tooltip("Select standard SchemaStore schema URL"));
        globeBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-min-width: 30px;");
        globeBtn.setOnAction(e -> showSchemaStoreCatalogPopup(globeBtn, schemaUrlField));

        HBox schemaUrlRow = new HBox(8, schemaUrlLabel, schemaUrlField, browseFileBtn, globeBtn);
        schemaUrlRow.setAlignment(Pos.CENTER_LEFT);

        // Row 3: Schema version
        Label versionLabel = new Label("Schema version:");
        versionLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-min-width: 140px;");

        ComboBox<String> versionCombo = new ComboBox<>();
        versionCombo.getItems().addAll(
                "JSON Schema v4",
                "JSON Schema v6",
                "JSON Schema v7",
                "JSON Schema 2019-09",
                "JSON Schema 2020-12"
        );
        versionCombo.setValue(item.getSchemaVersion() != null && !item.getSchemaVersion().isBlank() ? item.getSchemaVersion() : "JSON Schema v4");
        versionCombo.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        versionCombo.setPrefWidth(220);
        versionCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) item.setSchemaVersion(newVal);
        });

        HBox versionRow = new HBox(8, versionLabel, versionCombo);
        versionRow.setAlignment(Pos.CENTER_LEFT);

        // Row 4: Pattern mappings toolbar & table
        ObservableList<AppSettingsStore.JsonSchemaPatternMapping> patterns = FXCollections.observableArrayList();
        if (item.getPatterns() != null) {
            patterns.addAll(item.getPatterns());
        }

        TableView<AppSettingsStore.JsonSchemaPatternMapping> patternTable = new TableView<>(patterns);
        patternTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        patternTable.setPrefHeight(220);
        patternTable.setStyle("-fx-background-color: #1e1f22; -fx-control-inner-background: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(patternTable, Priority.ALWAYS);

        TableColumn<AppSettingsStore.JsonSchemaPatternMapping, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(formatKind(c.getValue().getKind())));
        typeCol.setPrefWidth(120);

        TableColumn<AppSettingsStore.JsonSchemaPatternMapping, String> pathCol = new TableColumn<>("File Path / Pattern");
        pathCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPathOrPattern()));
        pathCol.setPrefWidth(340);

        patternTable.getColumns().addAll(typeCol, pathCol);

        // Empty placeholder with blue action links matching Screenshot 2
        Label emptyTitle = new Label("No schema mappings defined");
        emptyTitle.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px;");

        Hyperlink fileLink = new Hyperlink("file");
        fileLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 13px; -fx-padding: 0;");
        Hyperlink patternLink = new Hyperlink("file path pattern");
        patternLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 13px; -fx-padding: 0;");
        Hyperlink dirLink = new Hyperlink("directory");
        dirLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 13px; -fx-padding: 0;");

        Label pre = new Label("Add mapping for a ");
        pre.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px;");
        Label comma1 = new Label(", ");
        comma1.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px;");
        Label comma2 = new Label(", ");
        comma2.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px;");

        HBox linksRow = new HBox(pre, fileLink, comma1, patternLink, comma2, dirLink);
        linksRow.setAlignment(Pos.CENTER);

        VBox tablePlaceholder = new VBox(6, emptyTitle, linksRow);
        tablePlaceholder.setAlignment(Pos.CENTER);
        patternTable.setPlaceholder(tablePlaceholder);

        // Pattern Toolbar
        Button addPatternBtn = new Button("+");
        addPatternBtn.setTooltip(new Tooltip("Add file, path pattern, or directory mapping"));
        addPatternBtn.setStyle("-fx-min-width: 28px; -fx-min-height: 24px; -fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");

        Button removePatternBtn = new Button("—");
        removePatternBtn.setTooltip(new Tooltip("Remove"));
        removePatternBtn.setStyle("-fx-min-width: 28px; -fx-min-height: 24px; -fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        removePatternBtn.setDisable(true);

        Button editPatternBtn = new Button("✏");
        editPatternBtn.setTooltip(new Tooltip("Edit"));
        editPatternBtn.setStyle("-fx-min-width: 28px; -fx-min-height: 24px; -fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        editPatternBtn.setDisable(true);

        patternTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean has = newVal != null;
            removePatternBtn.setDisable(!has);
            editPatternBtn.setDisable(!has);
        });

        // Popup menu for Add (+ button) matching DataGrip screenshot 2!
        ContextMenu addMenu = new ContextMenu();
        MenuItem addFileItem = new MenuItem("📄 Add File");
        MenuItem addPatternItem = new MenuItem("🗎 Add File Path Pattern");
        MenuItem addDirItem = new MenuItem("📁 Add Directory");
        addMenu.getItems().addAll(addFileItem, addPatternItem, addDirItem);

        addPatternBtn.setOnAction(e -> addMenu.show(addPatternBtn, javafx.geometry.Side.BOTTOM, 0, 0));

        Runnable triggerAddFile = () -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select File to Map to Schema");
            Window win = patternTable.getScene() != null ? patternTable.getScene().getWindow() : null;
            File f = fc.showOpenDialog(win);
            if (f != null) {
                AppSettingsStore.JsonSchemaPatternMapping p = new AppSettingsStore.JsonSchemaPatternMapping(AppSettingsStore.JsonSchemaPatternMapping.PatternKind.FILE, f.getName());
                patterns.add(p);
                item.setPatterns(new ArrayList<>(patterns));
                patternTable.getSelectionModel().select(p);
            }
        };

        Runnable triggerAddPattern = () -> {
            TextInputDialog dialog = new TextInputDialog("*.config.json");
            dialog.setTitle("Add File Path Pattern");
            dialog.setHeaderText("Specify file name pattern (e.g. *.config.json, tsconfig*.json)");
            dialog.setContentText("Pattern:");
            Window win = patternTable.getScene() != null ? patternTable.getScene().getWindow() : null;
            if (win != null) dialog.initOwner(win);
            dialog.showAndWait().ifPresent(pat -> {
                if (!pat.isBlank()) {
                    AppSettingsStore.JsonSchemaPatternMapping p = new AppSettingsStore.JsonSchemaPatternMapping(AppSettingsStore.JsonSchemaPatternMapping.PatternKind.FILE_PATH_PATTERN, pat.trim());
                    patterns.add(p);
                    item.setPatterns(new ArrayList<>(patterns));
                    patternTable.getSelectionModel().select(p);
                }
            });
        };

        Runnable triggerAddDir = () -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Directory to Map to Schema");
            Window win = patternTable.getScene() != null ? patternTable.getScene().getWindow() : null;
            File d = dc.showDialog(win);
            if (d != null) {
                AppSettingsStore.JsonSchemaPatternMapping p = new AppSettingsStore.JsonSchemaPatternMapping(AppSettingsStore.JsonSchemaPatternMapping.PatternKind.DIRECTORY, d.getName());
                patterns.add(p);
                item.setPatterns(new ArrayList<>(patterns));
                patternTable.getSelectionModel().select(p);
            }
        };

        addFileItem.setOnAction(e -> triggerAddFile.run());
        addPatternItem.setOnAction(e -> triggerAddPattern.run());
        addDirItem.setOnAction(e -> triggerAddDir.run());

        fileLink.setOnAction(e -> triggerAddFile.run());
        patternLink.setOnAction(e -> triggerAddPattern.run());
        dirLink.setOnAction(e -> triggerAddDir.run());

        removePatternBtn.setOnAction(e -> {
            AppSettingsStore.JsonSchemaPatternMapping sel = patternTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                patterns.remove(sel);
                item.setPatterns(new ArrayList<>(patterns));
            }
        });

        editPatternBtn.setOnAction(e -> {
            AppSettingsStore.JsonSchemaPatternMapping sel = patternTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                TextInputDialog dialog = new TextInputDialog(sel.getPathOrPattern());
                dialog.setTitle("Edit Mapping");
                dialog.setHeaderText("Edit file path or pattern:");
                dialog.setContentText("Value:");
                Window win = patternTable.getScene() != null ? patternTable.getScene().getWindow() : null;
                if (win != null) dialog.initOwner(win);
                dialog.showAndWait().ifPresent(val -> {
                    if (!val.isBlank()) {
                        sel.setPathOrPattern(val.trim());
                        item.setPatterns(new ArrayList<>(patterns));
                        patternTable.refresh();
                    }
                });
            }
        });

        HBox patternToolbar = new HBox(4, addPatternBtn, removePatternBtn, editPatternBtn);
        patternToolbar.setAlignment(Pos.CENTER_LEFT);

        // Hint label at bottom (Image 2)
        Label hintLabel = new Label("Path to file or directory relative to project root, or file name pattern like *.config.json");
        hintLabel.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");

        detailBox.getChildren().addAll(
                nameRow,
                schemaUrlRow,
                versionRow,
                patternToolbar,
                patternTable,
                hintLabel
        );
        VBox.setVgrow(patternTable, Priority.ALWAYS);
        return detailBox;
    }

    private void showSchemaStoreCatalogPopup(Button anchor, TextField targetField) {
        ContextMenu menu = new ContextMenu();
        List<String[]> presets = List.of(
                new String[]{"TypeScript Configuration", "http://json.schemastore.org/tsconfig"},
                new String[]{"Package.json", "http://json.schemastore.org/package"},
                new String[]{"Babel Configuration", "http://json.schemastore.org/babelrc"},
                new String[]{"ESLint Configuration", "http://json.schemastore.org/eslintrc"},
                new String[]{"Prettier Configuration", "http://json.schemastore.org/prettierrc"},
                new String[]{"OpenAPI 3.0", "https://spec.openapis.org/oas/3.0/schema/2021-09-28"},
                new String[]{"GitHub Workflow", "http://json.schemastore.org/github-workflow"},
                new String[]{"Docker Compose", "https://raw.githubusercontent.com/compose-spec/compose-spec/master/schema/compose-spec.json"}
        );

        for (String[] preset : presets) {
            MenuItem mi = new MenuItem(preset[0] + " (" + preset[1] + ")");
            mi.setOnAction(e -> targetField.setText(preset[1]));
            menu.getItems().add(mi);
        }
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private String formatKind(AppSettingsStore.JsonSchemaPatternMapping.PatternKind kind) {
        if (kind == null) return "Pattern";
        return switch (kind) {
            case FILE -> "File";
            case FILE_PATH_PATTERN -> "Pattern";
            case DIRECTORY -> "Directory";
        };
    }
}
