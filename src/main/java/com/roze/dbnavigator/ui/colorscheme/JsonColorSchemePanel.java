package com.roze.dbnavigator.ui.colorscheme;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.db.AppSettingsStore.ColorSchemeAttribute;
import com.roze.dbnavigator.ui.colorscheme.ColorSchemeModel.ColorSchemeElement;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.*;
import java.util.function.Consumer;

/**
 * DataGrip-identical JSON Color Scheme panel:
 * Displays all 14 JSON syntax elements with live interactive JSON editor preview
 * matching DataGrip screenshot 4.
 */
public final class JsonColorSchemePanel {

    private JsonColorSchemePanel() {}

    public static VBox build(AppSettingsStore.Settings settings,
                             Map<String, Object> inputs,
                             Consumer<String> navigateTo) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(12, 16, 12, 16));
        root.setStyle("-fx-background-color: transparent;");

        // Working state tracking
        final List<String> workingCustomSchemes;
        if (inputs.containsKey("customColorSchemes")) {
            workingCustomSchemes = (List<String>) inputs.get("customColorSchemes");
        } else {
            workingCustomSchemes = new ArrayList<>(settings.getCustomColorSchemes());
            inputs.put("customColorSchemes", workingCustomSchemes);
        }

        final Map<String, Map<String, ColorSchemeAttribute>> workingOverrides;
        if (inputs.containsKey("colorSchemeOverrides")) {
            workingOverrides = (Map<String, Map<String, ColorSchemeAttribute>>) inputs.get("colorSchemeOverrides");
        } else {
            workingOverrides = new LinkedHashMap<>();
            if (settings.getColorSchemeOverrides() != null) {
                for (Map.Entry<String, Map<String, ColorSchemeAttribute>> entry : settings.getColorSchemeOverrides().entrySet()) {
                    Map<String, ColorSchemeAttribute> inner = new LinkedHashMap<>();
                    for (Map.Entry<String, ColorSchemeAttribute> attrEntry : entry.getValue().entrySet()) {
                        inner.put(attrEntry.getKey(), attrEntry.getValue().copy());
                    }
                    workingOverrides.put(entry.getKey(), inner);
                }
            }
            inputs.put("colorSchemeOverrides", workingOverrides);
        }

        // 1. Top Bar: Scheme Selector + Gear Menu + Change IDE Theme...
        Label schemeLabel = new Label("Scheme:");
        schemeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        ComboBox<String> schemeCombo = new ComboBox<>();
        schemeCombo.setPrefWidth(210);
        schemeCombo.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");

        Runnable refreshSchemesList = () -> {
            String current = schemeCombo.getValue();
            schemeCombo.getItems().clear();
            schemeCombo.getItems().addAll(AppSettingsStore.defaultEditorColorSchemes());
            for (String custom : workingCustomSchemes) {
                if (!schemeCombo.getItems().contains(custom)) {
                    schemeCombo.getItems().add(custom);
                }
            }
            if (current != null && schemeCombo.getItems().contains(current)) {
                schemeCombo.setValue(current);
            } else if (schemeCombo.getItems().contains(settings.getEditorColorScheme())) {
                schemeCombo.setValue(settings.getEditorColorScheme());
            } else {
                schemeCombo.setValue(AppSettingsStore.defaultEditorColorSchemes().get(0));
            }
        };
        refreshSchemesList.run();
        inputs.put("editorColorSchemeCombo", schemeCombo);

        Button gearBtn = new Button("⚙");
        gearBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868a91; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 6;");

        ContextMenu gearMenu = new ContextMenu();
        MenuItem duplicateItem = new MenuItem("Duplicate...");
        duplicateItem.setOnAction(e -> {
            TextInputDialog tid = new TextInputDialog(schemeCombo.getValue() + " Copy");
            tid.setTitle("Duplicate Color Scheme");
            tid.setHeaderText("Enter new color scheme name:");
            tid.showAndWait().ifPresent(name -> {
                String trimmed = name.trim();
                if (!trimmed.isEmpty() && !schemeCombo.getItems().contains(trimmed)) {
                    workingCustomSchemes.add(trimmed);
                    Map<String, ColorSchemeAttribute> baseMap = workingOverrides.get(schemeCombo.getValue());
                    if (baseMap != null) {
                        Map<String, ColorSchemeAttribute> copyMap = new LinkedHashMap<>();
                        baseMap.forEach((k, v) -> copyMap.put(k, v.copy()));
                        workingOverrides.put(trimmed, copyMap);
                    }
                    refreshSchemesList.run();
                    schemeCombo.setValue(trimmed);
                }
            });
        });
        MenuItem restoreItem = new MenuItem("Restore Defaults");
        restoreItem.setOnAction(e -> {
            String active = schemeCombo.getValue();
            workingOverrides.remove(active);
        });
        gearMenu.getItems().addAll(duplicateItem, restoreItem);
        gearBtn.setOnAction(e -> gearMenu.show(gearBtn, javafx.geometry.Side.BOTTOM, 0, 0));

        Hyperlink themeLink = new Hyperlink("Change IDE Theme...");
        themeLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 12px; -fx-padding: 0 0 0 10; -fx-border-color: transparent;");
        themeLink.setOnAction(e -> {
            if (navigateTo != null) navigateTo.accept("Appearance & Behavior / Appearance");
        });

        Label helpIcon = new Label(" (?)");
        helpIcon.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px; -fx-cursor: hand;");
        Tooltip.install(helpIcon, new Tooltip("Configure JSON keys, strings, numbers, and escape syntax colors"));

        HBox topBar = new HBox(8, schemeLabel, schemeCombo, gearBtn, themeLink, helpIcon);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // 2. TreeView with 14 JSON elements (Alphabetical)
        TreeItem<ColorSchemeElement> rootItem = new TreeItem<>();
        rootItem.setExpanded(true);

        List<ColorSchemeElement> elements = ColorSchemeModel.getJsonElements();
        TreeItem<ColorSchemeElement> initialSelectionItem = null;
        for (ColorSchemeElement el : elements) {
            TreeItem<ColorSchemeElement> item = new TreeItem<>(el);
            rootItem.getChildren().add(item);
            if ("json.keyword".equals(el.getId())) {
                initialSelectionItem = item;
            }
        }

        TreeView<ColorSchemeElement> treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(false);
        treeView.setPrefWidth(320);
        treeView.setPrefHeight(230);
        treeView.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");

        treeView.setCellFactory(tv -> new TreeCell<>() {
            private void updateCellStyle() {
                if (isSelected()) {
                    setStyle("-fx-background-color: #2e436e; -fx-background-radius: 3; -fx-text-fill: #ffffff;");
                } else if (isHover()) {
                    setStyle("-fx-background-color: #2a2b2d; -fx-background-radius: 3; -fx-text-fill: #dfe1e5;");
                } else {
                    setStyle("-fx-background-color: transparent; -fx-text-fill: #bcbec4;");
                }
            }

            @Override
            protected void updateItem(ColorSchemeElement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item.getName());
                    updateCellStyle();
                }
            }
        });

        // 3. Attribute Editor (Right Pane)
        VBox attrEditor = new VBox(10);
        attrEditor.setPadding(new Insets(12, 16, 12, 16));
        attrEditor.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        attrEditor.setPrefWidth(360);
        attrEditor.setPrefHeight(230);

        Label selectedElementNameLabel = new Label("Keyword");
        selectedElementNameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px;");

        CheckBox boldCheck = new CheckBox("Bold");
        boldCheck.setStyle("-fx-text-fill: -text;");
        CheckBox italicCheck = new CheckBox("Italic");
        italicCheck.setStyle("-fx-text-fill: -text;");
        Region fontSpacer = new Region();
        HBox.setHgrow(fontSpacer, Priority.ALWAYS);
        HBox fontStyleRow = new HBox(16, fontSpacer, boldCheck, italicCheck);
        fontStyleRow.setAlignment(Pos.CENTER_RIGHT);

        final Runnable[] commitAttrChangesRef = new Runnable[1];
        final Runnable[] refreshPreviewRef = new Runnable[1];

        class AttrRow {
            final CheckBox check;
            final Button colorBtn;
            final HBox row;
            String colorHex;

            AttrRow(String labelText) {
                check = new CheckBox(labelText);
                check.setStyle("-fx-text-fill: -text;");
                check.setPrefWidth(140);

                colorBtn = new Button();
                colorBtn.setPrefWidth(90);
                colorBtn.setPrefHeight(24);
                colorBtn.setStyle("-fx-background-color: #2b2d30; -fx-background-radius: 3; -fx-border-radius: 3; -fx-border-color: #393b40; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                row = new HBox(8, check, spacer, colorBtn);
                row.setAlignment(Pos.CENTER_LEFT);

                colorBtn.setOnAction(e -> {
                    if (check.isSelected()) {
                        showColorPickerDialog(colorHex, colorBtn, newHex -> {
                            setColor(newHex);
                            if (commitAttrChangesRef[0] != null) commitAttrChangesRef[0].run();
                        });
                    }
                });

                check.selectedProperty().addListener((obs, oldV, newV) -> updateButtonState());
            }

            void updateButtonState() {
                boolean sel = check.isSelected();
                colorBtn.setDisable(!sel);
                if (!sel) {
                    colorBtn.setText("");
                    colorBtn.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #393b40; -fx-background-radius: 3; -fx-border-radius: 3; -fx-cursor: default;");
                } else {
                    setColor(colorHex != null && !colorHex.isBlank() ? colorHex : "FFFFFF");
                }
            }

            void setColor(String hex) {
                this.colorHex = hex != null ? hex.replace("#", "").toUpperCase() : null;
                if (check.isSelected() && this.colorHex != null && !this.colorHex.isBlank()) {
                    colorBtn.setText(this.colorHex);
                    colorBtn.setStyle("-fx-background-color: #" + this.colorHex + "; -fx-text-fill: " +
                            (isColorDark(this.colorHex) ? "#ffffff" : "#000000") +
                            "; -fx-border-color: #393b40; -fx-background-radius: 3; -fx-border-radius: 3; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");
                } else {
                    colorBtn.setText("");
                    colorBtn.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #393b40; -fx-background-radius: 3; -fx-border-radius: 3; -fx-cursor: default;");
                }
            }
        }

        AttrRow fgRow = new AttrRow("Foreground");
        AttrRow bgRow = new AttrRow("Background");
        AttrRow errorStripeRow = new AttrRow("Error stripe mark");
        AttrRow effectsRow = new AttrRow("Effects");

        ComboBox<String> effectTypeCombo = new ComboBox<>();
        effectTypeCombo.getItems().addAll("Underscored", "Bold Underscored", "Underwaved", "Strikeout", "Bordered", "Dotted line");
        effectTypeCombo.getSelectionModel().select("Bordered");
        effectTypeCombo.setPrefWidth(125);
        effectTypeCombo.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");

        Region effectSpacer = new Region();
        HBox.setHgrow(effectSpacer, Priority.ALWAYS);
        HBox effectComboRow = new HBox(effectSpacer, effectTypeCombo);
        effectComboRow.setAlignment(Pos.CENTER_RIGHT);
        VBox effectsContainer = new VBox(6, effectsRow.row, effectComboRow);

        effectsRow.check.selectedProperty().addListener((obs, oldV, newV) -> {
            effectTypeCombo.setDisable(!newV);
        });

        CheckBox inheritCheck = new CheckBox("Inherit values from:");
        inheritCheck.setStyle("-fx-text-fill: -text;");
        Hyperlink inheritLink = new Hyperlink();
        inheritLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 12px; -fx-padding: 0; -fx-border-color: transparent;");
        VBox inheritBox = new VBox(4, inheritCheck, inheritLink);
        inheritBox.setPadding(new Insets(6, 0, 0, 0));

        attrEditor.getChildren().addAll(selectedElementNameLabel, fontStyleRow, fgRow.row, bgRow.row, errorStripeRow.row, effectsContainer, inheritBox);

        // 4. Live JSON Preview (Bottom Pane matching Image 4)
        VBox previewPane = new VBox(2);
        previewPane.setPadding(new Insets(8, 12, 8, 12));
        previewPane.setStyle("-fx-background-color: #1e1f22;");

        String[] currentSelectedRef = new String[]{"json.keyword"};

        refreshPreviewRef[0] = () -> {
            previewPane.getChildren().clear();
            String active = schemeCombo.getValue();

            ColorSchemeAttribute blockCommentAttr = ColorSchemeModel.resolveAttribute(active, "json.block_comment", workingOverrides);
            ColorSchemeAttribute bracesAttr = ColorSchemeModel.resolveAttribute(active, "json.braces", workingOverrides);
            ColorSchemeAttribute bracketsAttr = ColorSchemeModel.resolveAttribute(active, "json.brackets", workingOverrides);
            ColorSchemeAttribute colonAttr = ColorSchemeModel.resolveAttribute(active, "json.colon", workingOverrides);
            ColorSchemeAttribute commaAttr = ColorSchemeModel.resolveAttribute(active, "json.comma", workingOverrides);
            ColorSchemeAttribute invEscapeAttr = ColorSchemeModel.resolveAttribute(active, "json.invalid_escape", workingOverrides);
            ColorSchemeAttribute keywordAttr = ColorSchemeModel.resolveAttribute(active, "json.keyword", workingOverrides);
            ColorSchemeAttribute lineCommentAttr = ColorSchemeModel.resolveAttribute(active, "json.line_comment", workingOverrides);
            ColorSchemeAttribute numberAttr = ColorSchemeModel.resolveAttribute(active, "json.number", workingOverrides);
            ColorSchemeAttribute propKeyAttr = ColorSchemeModel.resolveAttribute(active, "json.property_key", workingOverrides);
            ColorSchemeAttribute strAttr = ColorSchemeModel.resolveAttribute(active, "json.string", workingOverrides);
            ColorSchemeAttribute validEscapeAttr = ColorSchemeModel.resolveAttribute(active, "json.valid_escape", workingOverrides);

            // Line 1: {
            Label l1 = makeTokenLabel("{", bracesAttr, "json.braces", treeView, rootItem, currentSelectedRef);

            // Line 2:   // Line comments are not included in standard but nonetheless allowed.
            Label l2 = makeTokenLabel("  // Line comments are not included in standard but nonetheless allowed.", lineCommentAttr, "json.line_comment", treeView, rootItem, currentSelectedRef);

            // Line 3:   /* As well as block comments. */
            Label l3 = makeTokenLabel("  /* As well as block comments. */", blockCommentAttr, "json.block_comment", treeView, rootItem, currentSelectedRef);

            // Line 4:   "the only keywords are": [true, false, null],
            HBox l4 = new HBox(1,
                    makeTokenLabel("  \"the only keywords are\"", propKeyAttr, "json.property_key", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel(": ", colonAttr, "json.colon", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel("[", bracketsAttr, "json.brackets", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel("true", keywordAttr, "json.keyword", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel(", ", commaAttr, "json.comma", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel("false", keywordAttr, "json.keyword", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel(", ", commaAttr, "json.comma", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel("null", keywordAttr, "json.keyword", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel("]", bracketsAttr, "json.brackets", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel(",", commaAttr, "json.comma", treeView, rootItem, currentSelectedRef)
            );

            // Line 5:   "strings with": {
            HBox l5 = new HBox(1,
                    makeTokenLabel("  \"strings with\"", propKeyAttr, "json.property_key", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel(": ", colonAttr, "json.colon", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel("{", bracesAttr, "json.braces", treeView, rootItem, currentSelectedRef)
            );

            // Line 6:     "no escapes": "pseudopolinomiality",
            HBox l6 = new HBox(1,
                    makeTokenLabel("    \"no escapes\"", propKeyAttr, "json.property_key", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel(": ", colonAttr, "json.colon", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel("\"pseudopolinomiality\"", strAttr, "json.string", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel(",", commaAttr, "json.comma", treeView, rootItem, currentSelectedRef)
            );

            // Line 7:     "valid escapes": "C-style\r\n and unicode\u0021",
            HBox l7 = new HBox(1,
                    makeTokenLabel("    \"valid escapes\"", propKeyAttr, "json.property_key", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel(": ", colonAttr, "json.colon", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel("\"C-style", strAttr, "json.string", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel("\\r\\n", validEscapeAttr, "json.valid_escape", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel(" and unicode", strAttr, "json.string", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel("\\u0021", validEscapeAttr, "json.valid_escape", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel("\"", strAttr, "json.string", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel(",", commaAttr, "json.comma", treeView, rootItem, currentSelectedRef)
            );

            // Line 8:     "illegal escapes": "\0377\x\"
            HBox l8 = new HBox(1,
                    makeTokenLabel("    \"illegal escapes\"", propKeyAttr, "json.property_key", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel(": ", colonAttr, "json.colon", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel("\"", strAttr, "json.string", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel("\\0377", invEscapeAttr, "json.invalid_escape", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel("\\x", invEscapeAttr, "json.invalid_escape", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel("\\\"", invEscapeAttr, "json.invalid_escape", treeView, rootItem, currentSelectedRef)
            );

            // Line 9:   },
            HBox l9 = new HBox(1,
                    makeTokenLabel("  }", bracesAttr, "json.braces", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel(",", commaAttr, "json.comma", treeView, rootItem, currentSelectedRef)
            );

            // Line 10:   "some numbers": [
            HBox l10 = new HBox(1,
                    makeTokenLabel("  \"some numbers\"", propKeyAttr, "json.property_key", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel(": ", colonAttr, "json.colon", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel("[", bracketsAttr, "json.brackets", treeView, rootItem, currentSelectedRef)
            );

            // Line 11:     42,
            HBox l11 = new HBox(1,
                    makeTokenLabel("    42", numberAttr, "json.number", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel(",", commaAttr, "json.comma", treeView, rootItem, currentSelectedRef)
            );

            // Line 12:     -0.0e-0,
            HBox l12 = new HBox(1,
                    makeTokenLabel("    -0.0e-0", numberAttr, "json.number", treeView, rootItem, currentSelectedRef),
                    makeTokenLabel(",", commaAttr, "json.comma", treeView, rootItem, currentSelectedRef)
            );

            // Line 13:     6.626e-34
            Label l13 = makeTokenLabel("    6.626e-34", numberAttr, "json.number", treeView, rootItem, currentSelectedRef);

            // Line 14:   ]
            Label l14 = makeTokenLabel("  ]", bracketsAttr, "json.brackets", treeView, rootItem, currentSelectedRef);

            // Line 15: }
            Label l15 = makeTokenLabel("}", bracesAttr, "json.braces", treeView, rootItem, currentSelectedRef);

            previewPane.getChildren().addAll(l1, l2, l3, l4, l5, l6, l7, l8, l9, l10, l11, l12, l13, l14, l15);
        };

        // Commit logic
        commitAttrChangesRef[0] = () -> {
            TreeItem<ColorSchemeElement> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected == null || selected.getValue() == null) return;
            ColorSchemeElement el = selected.getValue();

            String active = schemeCombo.getValue();
            Map<String, ColorSchemeAttribute> map = workingOverrides.computeIfAbsent(active, k -> new LinkedHashMap<>());

            ColorSchemeAttribute current = new ColorSchemeAttribute(
                    boldCheck.isSelected(),
                    italicCheck.isSelected(),
                    fgRow.colorHex,
                    fgRow.check.isSelected(),
                    bgRow.colorHex,
                    bgRow.check.isSelected(),
                    errorStripeRow.colorHex,
                    errorStripeRow.check.isSelected(),
                    effectsRow.colorHex,
                    effectsRow.check.isSelected(),
                    effectTypeCombo.getValue(),
                    inheritCheck.isSelected(),
                    el.getInheritFromKey()
            );
            map.put(el.getId(), current);
            if (refreshPreviewRef[0] != null) refreshPreviewRef[0].run();
        };

        boldCheck.setOnAction(e -> { if (commitAttrChangesRef[0] != null) commitAttrChangesRef[0].run(); });
        italicCheck.setOnAction(e -> { if (commitAttrChangesRef[0] != null) commitAttrChangesRef[0].run(); });
        fgRow.check.setOnAction(e -> { if (commitAttrChangesRef[0] != null) commitAttrChangesRef[0].run(); });
        bgRow.check.setOnAction(e -> { if (commitAttrChangesRef[0] != null) commitAttrChangesRef[0].run(); });
        errorStripeRow.check.setOnAction(e -> { if (commitAttrChangesRef[0] != null) commitAttrChangesRef[0].run(); });
        effectsRow.check.setOnAction(e -> { if (commitAttrChangesRef[0] != null) commitAttrChangesRef[0].run(); });
        effectTypeCombo.setOnAction(e -> { if (commitAttrChangesRef[0] != null) commitAttrChangesRef[0].run(); });
        inheritCheck.setOnAction(e -> { if (commitAttrChangesRef[0] != null) commitAttrChangesRef[0].run(); });

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem == null || newItem.getValue() == null) return;
            ColorSchemeElement el = newItem.getValue();
            selectedElementNameLabel.setText(el.getName());
            currentSelectedRef[0] = el.getId();

            String active = schemeCombo.getValue();
            ColorSchemeAttribute attr = ColorSchemeModel.resolveAttribute(active, el.getId(), workingOverrides);

            boldCheck.setSelected(attr.bold);
            italicCheck.setSelected(attr.italic);

            fgRow.check.setSelected(attr.foregroundEnabled);
            fgRow.setColor(attr.foreground);
            fgRow.updateButtonState();

            bgRow.check.setSelected(attr.backgroundEnabled);
            bgRow.setColor(attr.background);
            bgRow.updateButtonState();

            errorStripeRow.check.setSelected(attr.errorStripeEnabled);
            errorStripeRow.setColor(attr.errorStripe);
            errorStripeRow.updateButtonState();

            effectsRow.check.setSelected(attr.effectEnabled);
            effectsRow.setColor(attr.effectColor);
            effectsRow.updateButtonState();
            if (attr.effectType != null) {
                effectTypeCombo.setValue(attr.effectType);
            }
            effectTypeCombo.setDisable(!attr.effectEnabled);

            if (el.hasInheritance()) {
                inheritBox.setVisible(true);
                inheritCheck.setSelected(attr.inherit);
                inheritLink.setText(el.getInheritFromDisplay());
                inheritLink.setOnAction(e -> {
                    if (navigateTo != null) {
                        navigateTo.accept("Editor / Color Scheme / Language Defaults");
                    }
                });
            } else {
                inheritBox.setVisible(false);
            }
        });

        schemeCombo.setOnAction(e -> {
            TreeItem<ColorSchemeElement> sel = treeView.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getValue() != null) {
                treeView.getSelectionModel().clearSelection();
                treeView.getSelectionModel().select(sel);
            }
            if (refreshPreviewRef[0] != null) refreshPreviewRef[0].run();
        });

        // Initial selection: Keyword (matching Image 4)
        if (initialSelectionItem != null) {
            treeView.getSelectionModel().select(initialSelectionItem);
        } else if (!rootItem.getChildren().isEmpty()) {
            treeView.getSelectionModel().select(rootItem.getChildren().get(0));
        }

        // SplitPane for tree & editor
        SplitPane middleSplit = new SplitPane();
        middleSplit.getItems().addAll(treeView, attrEditor);
        middleSplit.setDividerPositions(0.48);
        middleSplit.setPrefHeight(250);
        middleSplit.setStyle("-fx-background-color: transparent;");

        // Preview container with right margin line
        StackPane previewWrapper = new StackPane();
        previewWrapper.setStyle("-fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-background-color: #1e1f22;");
        ScrollPane previewScroll = new ScrollPane(previewPane);
        previewScroll.setFitToWidth(true);
        previewScroll.setPrefHeight(230);
        previewScroll.setStyle("-fx-background-color: transparent; -fx-background: #1e1f22; -fx-border-color: transparent;");

        Line marginLine = new Line(520, 0, 520, 1000);
        marginLine.setStroke(Color.web("#2b2d30"));
        marginLine.setStrokeWidth(1);
        Pane linePane = new Pane(marginLine);
        linePane.setMouseTransparent(true);

        previewWrapper.getChildren().addAll(previewScroll, linePane);

        refreshPreviewRef[0].run();

        root.getChildren().addAll(topBar, middleSplit, previewWrapper);
        VBox.setVgrow(previewWrapper, Priority.ALWAYS);

        return root;
    }

    private static Label makeTokenLabel(String text,
                                        ColorSchemeAttribute attr,
                                        String elementId,
                                        TreeView<ColorSchemeElement> treeView,
                                        TreeItem<ColorSchemeElement> rootItem,
                                        String[] currentSelectedRef) {
        Label label = new Label(text);
        label.setFont(javafx.scene.text.Font.font("Menlo", 12));
        label.setCursor(javafx.scene.Cursor.HAND);

        StringBuilder style = new StringBuilder("-fx-padding: 0;");
        if (attr != null) {
            if (attr.foregroundEnabled && attr.foreground != null && !attr.foreground.isBlank()) {
                style.append(" -fx-text-fill: #").append(attr.foreground).append(";");
            } else {
                style.append(" -fx-text-fill: #bcbec4;");
            }
            if (attr.backgroundEnabled && attr.background != null && !attr.background.isBlank()) {
                style.append(" -fx-background-color: #").append(attr.background).append(";");
            }
            if (attr.bold) {
                style.append(" -fx-font-weight: bold;");
            }
            if (attr.italic) {
                style.append(" -fx-font-style: italic;");
            }
            if (attr.effectEnabled) {
                String effColor = (attr.effectColor != null && !attr.effectColor.isBlank()) ? "#" + attr.effectColor : "#cf8e6d";
                if ("Underwaved".equals(attr.effectType)) {
                    style.append(" -fx-border-color: transparent transparent ").append(effColor).append(" transparent; -fx-border-width: 0 0 2 0;");
                } else if ("Bordered".equals(attr.effectType)) {
                    style.append(" -fx-border-color: ").append(effColor).append("; -fx-border-width: 1; -fx-border-radius: 2;");
                } else {
                    style.append(" -fx-border-color: transparent transparent ").append(effColor).append(" transparent; -fx-border-width: 0 0 1 0;");
                }
            }
        } else {
            style.append(" -fx-text-fill: #bcbec4;");
        }
        label.setStyle(style.toString());
        if (elementId != null) {
            label.setOnMouseClicked(e -> {
                selectTreeItemById(treeView, rootItem, elementId);
                currentSelectedRef[0] = elementId;
            });
        }
        return label;
    }

    public static void selectTreeItemById(TreeView<ColorSchemeElement> treeView,
                                          TreeItem<ColorSchemeElement> root,
                                          String elementId) {
        if (elementId == null) return;
        TreeItem<ColorSchemeElement> target = findTreeItem(root, elementId);
        if (target != null) {
            treeView.getSelectionModel().select(target);
            treeView.scrollTo(treeView.getRow(target));
        }
    }

    private static TreeItem<ColorSchemeElement> findTreeItem(TreeItem<ColorSchemeElement> current, String elementId) {
        if (current.getValue() != null && elementId.equals(current.getValue().getId())) {
            return current;
        }
        for (TreeItem<ColorSchemeElement> child : current.getChildren()) {
            TreeItem<ColorSchemeElement> res = findTreeItem(child, elementId);
            if (res != null) return res;
        }
        return null;
    }

    private static boolean isColorDark(String hex) {
        if (hex == null || hex.length() < 6) return true;
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            double lum = (0.299 * r + 0.587 * g + 0.114 * b);
            return lum < 128;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private static void showColorPickerDialog(String currentHex, Button anchor, Consumer<String> onColorSelected) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Choose Color");

        VBox content = new VBox(10);
        content.setPadding(new Insets(14));
        content.setStyle("-fx-background-color: #2b2d30;");

        Color initial = Color.web("#" + (currentHex != null && !currentHex.isBlank() ? currentHex : "FFFFFF"));
        ColorPicker cp = new ColorPicker(initial);
        cp.setStyle("-fx-background-color: #1e1f22; -fx-color-label-visible: true;");

        TextField hexField = new TextField(currentHex != null ? currentHex : "FFFFFF");
        hexField.setPrefWidth(80);
        hexField.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 3; -fx-background-radius: 3;");

        cp.valueProperty().addListener((obs, oldC, newC) -> {
            if (newC != null) {
                String h = String.format("%02X%02X%02X",
                        (int) (newC.getRed() * 255),
                        (int) (newC.getGreen() * 255),
                        (int) (newC.getBlue() * 255));
                hexField.setText(h);
            }
        });

        HBox pickRow = new HBox(8, new Label("Color:"), cp, new Label("#"), hexField);
        pickRow.setAlignment(Pos.CENTER_LEFT);

        FlowPane presets = new FlowPane(4, 4);
        presets.setPrefWrapLength(240);
        String[] palette = {
                "CF8E6D", "C792EA", "6A8759", "2AACB8", "FA6675", "7A7E85", "BCBEC4", "DFE1E5",
                "56A8F5", "2BBAC5", "6AAB73", "E5B842", "589DF6", "4FC414", "3574F0", "2B2D30"
        };
        for (String c : palette) {
            Button swatch = new Button();
            swatch.setPrefSize(18, 18);
            swatch.setStyle("-fx-background-color: #" + c + "; -fx-border-color: #393b40; -fx-background-radius: 2; -fx-border-radius: 2; -fx-cursor: hand;");
            swatch.setOnAction(e -> {
                hexField.setText(c);
                try {
                    cp.setValue(Color.web("#" + c));
                } catch (Exception ignored) {}
            });
            presets.getChildren().add(swatch);
        }

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setStyle("-fx-background-color: #3574f0; -fx-text-fill: white; -fx-background-radius: 4; -fx-padding: 4 14; -fx-cursor: hand;");
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #393b40; -fx-text-fill: -text; -fx-background-radius: 4; -fx-padding: 4 14; -fx-cursor: hand;");

        okBtn.setOnAction(e -> {
            String val = hexField.getText().trim().replace("#", "").toUpperCase();
            if (val.length() == 6) {
                onColorSelected.accept(val);
                dialog.close();
            }
        });
        cancelBtn.setOnAction(e -> dialog.close());

        HBox btnRow = new HBox(8, okBtn, cancelBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        content.getChildren().addAll(pickRow, new Separator(), new Label("Recent & Standard:"), presets, new Separator(), btnRow);

        Scene scene = new Scene(content, 300, 220);
        dialog.setScene(scene);
        dialog.show();
    }
}
