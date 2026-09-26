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
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.*;
import java.util.function.Consumer;

/**
 * DataGrip-identical Table Diff Color Scheme panel:
 * Displays Table Diff elements with error stripe markers and live diff comparison preview
 * matching DataGrip screenshot 4.
 */
public final class TableDiffColorSchemePanel {

    private TableDiffColorSchemePanel() {}

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
                    Map<String, ColorSchemeAttribute> sourceMap = workingOverrides.get(schemeCombo.getValue());
                    if (sourceMap != null) {
                        Map<String, ColorSchemeAttribute> copyMap = new LinkedHashMap<>();
                        sourceMap.forEach((k, v) -> copyMap.put(k, v.copy()));
                        workingOverrides.put(trimmed, copyMap);
                    }
                    refreshSchemesList.run();
                    schemeCombo.setValue(trimmed);
                }
            });
        });

        MenuItem restoreItem = new MenuItem("Restore Defaults");
        restoreItem.setOnAction(e -> {
            String cur = schemeCombo.getValue();
            if (cur != null) {
                workingOverrides.remove(cur);
                refreshSchemesList.run();
            }
        });

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            String cur = schemeCombo.getValue();
            if (cur != null && workingCustomSchemes.contains(cur)) {
                workingCustomSchemes.remove(cur);
                workingOverrides.remove(cur);
                refreshSchemesList.run();
                schemeCombo.setValue(AppSettingsStore.defaultEditorColorSchemes().get(0));
            }
        });

        gearMenu.getItems().addAll(duplicateItem, restoreItem, deleteItem);
        gearBtn.setOnAction(e -> {
            String cur = schemeCombo.getValue();
            boolean isCustom = cur != null && workingCustomSchemes.contains(cur);
            deleteItem.setDisable(!isCustom);
            gearMenu.show(gearBtn, javafx.geometry.Side.BOTTOM, 0, 0);
        });

        Hyperlink changeThemeLink = new Hyperlink("Change IDE Theme...");
        changeThemeLink.setStyle("-fx-text-fill: #569cd6; -fx-font-size: 12px; -fx-padding: 0;");
        changeThemeLink.setOnAction(e -> {
            if (navigateTo != null) {
                navigateTo.accept("Appearance & Behavior / Appearance");
            }
        });

        Label helpIcon = new Label("?");
        helpIcon.setStyle("-fx-text-fill: #868a91; -fx-font-size: 13px; -fx-cursor: hand; -fx-border-color: #868a91; -fx-border-radius: 10; -fx-padding: 0 4;");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox topBar = new HBox(8, schemeLabel, schemeCombo, gearBtn, changeThemeLink, helpIcon, topSpacer);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // 2. TreeView of 3 Table Diff elements
        TreeItem<ColorSchemeElement> treeRoot = new TreeItem<>();
        List<ColorSchemeElement> diffElements = ColorSchemeModel.getTableDiffElements();
        Map<String, TreeItem<ColorSchemeElement>> treeItemMap = new HashMap<>();

        for (ColorSchemeElement el : diffElements) {
            TreeItem<ColorSchemeElement> item = new TreeItem<>(el);
            treeRoot.getChildren().add(item);
            treeItemMap.put(el.getId(), item);
        }

        TreeView<ColorSchemeElement> treeView = new TreeView<>(treeRoot);
        treeView.setShowRoot(false);
        treeView.setPrefWidth(350);
        VBox.setVgrow(treeView, Priority.ALWAYS);
        treeView.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");

        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(ColorSchemeElement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item.getName());
                    String curScheme = schemeCombo.getValue() != null ? schemeCombo.getValue() : "Dark Theme default";
                    ColorSchemeAttribute attr = ColorSchemeModel.resolveAttribute(curScheme, item.getId(), workingOverrides);
                    String colorStyle = "-fx-text-fill: #BCBEC4;";
                    if (attr != null && attr.foregroundEnabled && attr.foreground != null && !attr.foreground.isBlank()) {
                        colorStyle = "-fx-text-fill: #" + attr.foreground + ";";
                    }
                    if (isSelected()) {
                        setStyle("-fx-background-color: #2e436e; " + colorStyle);
                    } else {
                        setStyle("-fx-background-color: transparent; " + colorStyle);
                    }
                }
            }
        });

        // 3. Right Attribute Editor Pane
        VBox attrEditorPane = new VBox(12);
        attrEditorPane.setPadding(new Insets(10, 16, 10, 16));
        attrEditorPane.setPrefWidth(320);
        attrEditorPane.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");

        CheckBox boldCheck = new CheckBox("Bold");
        boldCheck.setStyle("-fx-text-fill: -text;");
        CheckBox italicCheck = new CheckBox("Italic");
        italicCheck.setStyle("-fx-text-fill: -text;");
        HBox fontStyleRow = new HBox(16, boldCheck, italicCheck);
        fontStyleRow.setAlignment(Pos.CENTER_LEFT);

        final Runnable[] commitAttrChangesRef = new Runnable[1];

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
        inheritLink.setStyle("-fx-text-fill: #569cd6; -fx-font-size: 12px; -fx-padding: 0;");
        inheritLink.setWrapText(true);

        VBox inheritBox = new VBox(4, inheritCheck, inheritLink);
        inheritBox.setPadding(new Insets(10, 0, 0, 0));

        attrEditorPane.getChildren().addAll(
                fontStyleRow,
                fgRow.row,
                bgRow.row,
                errorStripeRow.row,
                effectsContainer,
                inheritBox
        );

        // 4. Bottom Live Table Diff Preview
        VBox previewLines = new VBox(4);
        previewLines.setPadding(new Insets(8, 12, 8, 12));
        previewLines.setStyle("-fx-background-color: #1e1f22; -fx-font-family: 'JetBrains Mono', 'Fira Code', monospace; -fx-font-size: 12px;");

        // Error stripe track on the right
        VBox errorStripeTrack = new VBox(4);
        errorStripeTrack.setPrefWidth(14);
        errorStripeTrack.setMinWidth(14);
        errorStripeTrack.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #393b40; -fx-border-width: 0 0 0 1;");
        errorStripeTrack.setAlignment(Pos.TOP_CENTER);
        errorStripeTrack.setPadding(new Insets(8, 0, 8, 0));

        HBox previewWithStripe = new HBox(previewLines, errorStripeTrack);
        HBox.setHgrow(previewLines, Priority.ALWAYS);
        previewWithStripe.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");
        previewWithStripe.setMinHeight(220);

        ScrollPane previewScroll = new ScrollPane(previewWithStripe);
        previewScroll.setFitToWidth(true);
        previewScroll.setStyle("-fx-background: #1e1f22; -fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4;");
        previewScroll.setPrefHeight(240);

        final Runnable[] refreshPreviewTokensRef = new Runnable[1];

        // Selection syncing
        final boolean[] updatingUI = {false};

        Runnable loadSelectedElementToUI = () -> {
            TreeItem<ColorSchemeElement> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected == null || selected.getValue() == null) {
                attrEditorPane.setDisable(true);
                return;
            }
            attrEditorPane.setDisable(false);
            ColorSchemeElement el = selected.getValue();
            String curScheme = schemeCombo.getValue() != null ? schemeCombo.getValue() : "Dark Theme default";

            ColorSchemeAttribute curAttr = workingOverrides.getOrDefault(curScheme, Collections.emptyMap()).get(el.getId());
            boolean isOverridden = curAttr != null;
            ColorSchemeAttribute effAttr = ColorSchemeModel.resolveAttribute(curScheme, el.getId(), workingOverrides);

            updatingUI[0] = true;
            try {
                if (el.getInheritFromKey() != null) {
                    inheritBox.setVisible(true);
                    inheritBox.setManaged(true);
                    inheritCheck.setSelected(!isOverridden || curAttr.inherit);
                    inheritLink.setText(el.getInheritFromDisplay());
                    inheritLink.setOnAction(ev -> {
                        if (navigateTo != null && el.getInheritFromKey() != null) {
                            if (el.getInheritFromKey().startsWith("lang.")) {
                                navigateTo.accept("Editor / Color Scheme / Language Defaults");
                            } else if (el.getInheritFromKey().startsWith("gen.")) {
                                navigateTo.accept("Editor / Color Scheme / General");
                            }
                        }
                    });
                } else {
                    inheritBox.setVisible(false);
                    inheritBox.setManaged(false);
                }

                boldCheck.setSelected(effAttr != null && effAttr.bold);
                italicCheck.setSelected(effAttr != null && effAttr.italic);

                fgRow.check.setSelected(effAttr != null && effAttr.foregroundEnabled);
                fgRow.setColor(effAttr != null ? effAttr.foreground : null);

                bgRow.check.setSelected(effAttr != null && effAttr.backgroundEnabled);
                bgRow.setColor(effAttr != null ? effAttr.background : null);

                errorStripeRow.check.setSelected(effAttr != null && effAttr.errorStripeEnabled);
                errorStripeRow.setColor(effAttr != null ? effAttr.errorStripe : null);

                effectsRow.check.setSelected(effAttr != null && effAttr.effectEnabled);
                effectsRow.setColor(effAttr != null ? effAttr.effectColor : null);
                if (effAttr != null && effAttr.effectType != null) {
                    effectTypeCombo.getSelectionModel().select(effAttr.effectType);
                }
                effectTypeCombo.setDisable(effAttr == null || !effAttr.effectEnabled);

                boolean enableCustomization = !inheritCheck.isSelected() || el.getInheritFromKey() == null;
                boldCheck.setDisable(!enableCustomization);
                italicCheck.setDisable(!enableCustomization);
                fgRow.check.setDisable(!enableCustomization);
                bgRow.check.setDisable(!enableCustomization);
                errorStripeRow.check.setDisable(!enableCustomization);
                effectsRow.check.setDisable(!enableCustomization);
                fgRow.colorBtn.setDisable(!enableCustomization || !fgRow.check.isSelected());
                bgRow.colorBtn.setDisable(!enableCustomization || !bgRow.check.isSelected());
                errorStripeRow.colorBtn.setDisable(!enableCustomization || !errorStripeRow.check.isSelected());
                effectsRow.colorBtn.setDisable(!enableCustomization || !effectsRow.check.isSelected());
                effectTypeCombo.setDisable(!enableCustomization || !effectsRow.check.isSelected());
            } finally {
                updatingUI[0] = false;
            }
        };

        Runnable commitAttrChanges = () -> {
            if (updatingUI[0]) return;
            TreeItem<ColorSchemeElement> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected == null || selected.getValue() == null) return;
            ColorSchemeElement el = selected.getValue();
            String curScheme = schemeCombo.getValue() != null ? schemeCombo.getValue() : "Dark Theme default";

            Map<String, ColorSchemeAttribute> schemeMap = workingOverrides.computeIfAbsent(curScheme, k -> new LinkedHashMap<>());

            if (inheritCheck.isVisible() && inheritCheck.isSelected()) {
                schemeMap.remove(el.getId());
            } else {
                ColorSchemeAttribute newAttr = new ColorSchemeAttribute(
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
                        false,
                        el.getInheritFromKey()
                );
                schemeMap.put(el.getId(), newAttr);
            }

            treeView.refresh();
            if (refreshPreviewTokensRef[0] != null) refreshPreviewTokensRef[0].run();
            loadSelectedElementToUI.run();
        };
        commitAttrChangesRef[0] = commitAttrChanges;

        boldCheck.selectedProperty().addListener((obs, oldV, newV) -> commitAttrChanges.run());
        italicCheck.selectedProperty().addListener((obs, oldV, newV) -> commitAttrChanges.run());
        fgRow.check.selectedProperty().addListener((obs, oldV, newV) -> commitAttrChanges.run());
        bgRow.check.selectedProperty().addListener((obs, oldV, newV) -> commitAttrChanges.run());
        errorStripeRow.check.selectedProperty().addListener((obs, oldV, newV) -> commitAttrChanges.run());
        effectsRow.check.selectedProperty().addListener((obs, oldV, newV) -> commitAttrChanges.run());
        effectTypeCombo.valueProperty().addListener((obs, oldV, newV) -> commitAttrChanges.run());
        inheritCheck.selectedProperty().addListener((obs, oldV, newV) -> commitAttrChanges.run());

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            loadSelectedElementToUI.run();
        });

        schemeCombo.valueProperty().addListener((obs, oldV, newV) -> {
            treeView.refresh();
            loadSelectedElementToUI.run();
            if (refreshPreviewTokensRef[0] != null) refreshPreviewTokensRef[0].run();
        });

        // 5. Render Preview
        Runnable buildPreview = () -> {
            previewLines.getChildren().clear();
            errorStripeTrack.getChildren().clear();

            String curScheme = schemeCombo.getValue() != null ? schemeCombo.getValue() : "Dark Theme default";
            ColorSchemeAttribute excludedAttr = ColorSchemeModel.resolveAttribute(curScheme, "table_diff.excluded", workingOverrides);
            ColorSchemeAttribute matchedAttr = ColorSchemeModel.resolveAttribute(curScheme, "table_diff.fuzzy_matched", workingOverrides);
            ColorSchemeAttribute mismatchedAttr = ColorSchemeModel.resolveAttribute(curScheme, "table_diff.fuzzy_mismatched", workingOverrides);

            // Row 1: Matched text
            Label matchedLbl = new Label("Matched text");
            matchedLbl.setStyle("-fx-font-family: 'JetBrains Mono', 'Fira Code', monospace; -fx-font-size: 12px; -fx-text-fill: #BCBEC4;");
            HBox row1 = new HBox(matchedLbl);
            row1.setPadding(new Insets(2, 4, 2, 4));
            row1.setOnMouseClicked(e -> {
                TreeItem<ColorSchemeElement> target = treeItemMap.get("table_diff.fuzzy_matched");
                if (target != null) treeView.getSelectionModel().select(target);
            });
            previewLines.getChildren().add(row1);
            errorStripeTrack.getChildren().add(new Rectangle(10, 16, Color.TRANSPARENT));

            // Row 2: Inserted text (green diff)
            Label insertedLbl = new Label("Inserted text");
            insertedLbl.setStyle("-fx-font-family: 'JetBrains Mono', 'Fira Code', monospace; -fx-font-size: 12px; -fx-text-fill: #BCBEC4; -fx-background-color: #264D3B; -fx-padding: 1 4;");
            HBox row2 = new HBox(insertedLbl);
            previewLines.getChildren().add(row2);
            Rectangle greenStripe = new Rectangle(10, 4, Color.web("#388E3C"));
            errorStripeTrack.getChildren().add(new StackPane(greenStripe));

            // Row 3: Removed text (diff)
            Label removedLbl = new Label("Removed text");
            removedLbl.setStyle("-fx-font-family: 'JetBrains Mono', 'Fira Code', monospace; -fx-font-size: 12px; -fx-text-fill: #BCBEC4; -fx-background-color: #3A2323; -fx-padding: 1 4;");
            HBox row3 = new HBox(removedLbl);
            previewLines.getChildren().add(row3);
            Rectangle redStripe = new Rectangle(10, 4, Color.web("#D32F2F"));
            errorStripeTrack.getChildren().add(new StackPane(redStripe));

            // Row 4: Modified text (diff)
            Label modLbl = new Label("Modified text");
            modLbl.setStyle("-fx-font-family: 'JetBrains Mono', 'Fira Code', monospace; -fx-font-size: 12px; -fx-text-fill: #BCBEC4; -fx-background-color: #1B3B54; -fx-padding: 1 4;");
            HBox row4 = new HBox(modLbl);
            previewLines.getChildren().add(row4);
            Rectangle blueStripe = new Rectangle(10, 4, Color.web("#1976D2"));
            errorStripeTrack.getChildren().add(new StackPane(blueStripe));

            // Row 5: Fuzzy matched /mismatch/ text
            Label fuzzyPrefix = new Label("Fuzzy matched ");
            fuzzyPrefix.setStyle("-fx-font-family: 'JetBrains Mono', 'Fira Code', monospace; -fx-font-size: 12px; -fx-text-fill: #7A7E85;");

            Label mismatchToken = new Label("/mismatch/");
            StringBuilder msb = new StringBuilder("-fx-font-family: 'JetBrains Mono', 'Fira Code', monospace; -fx-font-size: 12px; -fx-padding: 1 4; -fx-cursor: hand;");
            if (mismatchedAttr != null) {
                if (mismatchedAttr.backgroundEnabled && mismatchedAttr.background != null && !mismatchedAttr.background.isBlank()) {
                    msb.append("-fx-background-color: #").append(mismatchedAttr.background).append(";");
                } else {
                    msb.append("-fx-background-color: #114957;");
                }
                if (mismatchedAttr.foregroundEnabled && mismatchedAttr.foreground != null && !mismatchedAttr.foreground.isBlank()) {
                    msb.append("-fx-text-fill: #").append(mismatchedAttr.foreground).append(";");
                } else {
                    msb.append("-fx-text-fill: #72D6D6;");
                }
                if (mismatchedAttr.effectEnabled && "Bordered".equalsIgnoreCase(mismatchedAttr.effectType)) {
                    String borderHex = mismatchedAttr.effectColor != null ? mismatchedAttr.effectColor : "165E70";
                    msb.append("-fx-border-color: #").append(borderHex).append("; -fx-border-width: 1;");
                }
            } else {
                msb.append("-fx-background-color: #114957; -fx-text-fill: #72D6D6; -fx-border-color: #165E70;");
            }
            mismatchToken.setStyle(msb.toString());

            Label fuzzySuffix = new Label(" text");
            fuzzySuffix.setStyle("-fx-font-family: 'JetBrains Mono', 'Fira Code', monospace; -fx-font-size: 12px; -fx-text-fill: #7A7E85;");

            HBox row5 = new HBox(fuzzyPrefix, mismatchToken, fuzzySuffix);
            row5.setAlignment(Pos.CENTER_LEFT);
            row5.setOnMouseClicked(e -> {
                TreeItem<ColorSchemeElement> target = treeItemMap.get("table_diff.fuzzy_mismatched");
                if (target != null) treeView.getSelectionModel().select(target);
            });
            previewLines.getChildren().add(row5);

            String stripeColor = (mismatchedAttr != null && mismatchedAttr.errorStripeEnabled && mismatchedAttr.errorStripe != null)
                    ? mismatchedAttr.errorStripe : "72D6D6";
            Rectangle cyanStripe = new Rectangle(10, 4, Color.web("#" + stripeColor));
            errorStripeTrack.getChildren().add(new StackPane(cyanStripe));

            // Row 6: Excluded from diff
            Label excludedLbl = new Label("Excluded from diff");
            StringBuilder exsb = new StringBuilder("-fx-font-family: 'JetBrains Mono', 'Fira Code', monospace; -fx-font-size: 12px; -fx-padding: 1 4; -fx-cursor: hand;");
            if (excludedAttr != null && excludedAttr.backgroundEnabled && excludedAttr.background != null && !excludedAttr.background.isBlank()) {
                exsb.append("-fx-background-color: #").append(excludedAttr.background).append(";");
            } else {
                exsb.append("-fx-background-color: #2E3A3B;");
            }
            exsb.append("-fx-text-fill: #7A7E85;");
            excludedLbl.setStyle(exsb.toString());

            HBox row6 = new HBox(excludedLbl);
            row6.setOnMouseClicked(e -> {
                TreeItem<ColorSchemeElement> target = treeItemMap.get("table_diff.excluded");
                if (target != null) treeView.getSelectionModel().select(target);
            });
            previewLines.getChildren().add(row6);
        };

        refreshPreviewTokensRef[0] = buildPreview;
        buildPreview.run();

        // Assemble Top Split (Tree + Attribute Editor)
        SplitPane topSplit = new SplitPane(treeView, attrEditorPane);
        topSplit.setDividerPositions(0.55);
        VBox.setVgrow(topSplit, Priority.ALWAYS);
        topSplit.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        // Assemble Main Vertical Split (Top Split + Preview Pane)
        SplitPane mainSplit = new SplitPane(topSplit, previewScroll);
        mainSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        mainSplit.setDividerPositions(0.60);
        VBox.setVgrow(mainSplit, Priority.ALWAYS);
        mainSplit.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        root.getChildren().addAll(topBar, mainSplit);

        // Select "Fuzzy match - mismatched" by default matching DataGrip Screenshot 4
        TreeItem<ColorSchemeElement> defaultSelection = treeItemMap.get("table_diff.fuzzy_mismatched");
        if (defaultSelection != null) {
            treeView.getSelectionModel().select(defaultSelection);
            treeView.scrollTo(treeView.getRow(defaultSelection));
        }

        return root;
    }

    private static boolean isColorDark(String hex) {
        if (hex == null || hex.length() < 6) return true;
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            double luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
            return luminance < 0.5;
        } catch (Exception e) {
            return true;
        }
    }

    private static void showColorPickerDialog(String currentHex, Button triggerBtn, Consumer<String> onColorChosen) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Choose Color");

        Color initial = Color.WHITE;
        if (currentHex != null && currentHex.length() == 6) {
            try {
                initial = Color.web("#" + currentHex);
            } catch (Exception ignored) {}
        }

        ColorPicker picker = new ColorPicker(initial);
        picker.setStyle("-fx-color-label-visible: true;");

        Button okBtn = new Button("Choose");
        okBtn.setStyle("-fx-background-color: #3574f0; -fx-text-fill: white; -fx-font-weight: bold;");
        okBtn.setOnAction(e -> {
            Color c = picker.getValue();
            if (c != null) {
                int r = (int) Math.round(c.getRed() * 255);
                int g = (int) Math.round(c.getGreen() * 255);
                int b = (int) Math.round(c.getBlue() * 255);
                String hex = String.format("%02X%02X%02X", r, g, b);
                onColorChosen.accept(hex);
            }
            dialog.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox btnBox = new HBox(8, okBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(12, new Label("Select color:"), picker, btnBox);
        layout.setPadding(new Insets(16));
        layout.setStyle("-fx-background-color: #2b2d30;");

        dialog.setScene(new Scene(layout));
        dialog.showAndWait();
    }
}
