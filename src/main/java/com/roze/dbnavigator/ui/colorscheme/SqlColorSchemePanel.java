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
 * DataGrip-identical SQL Color Scheme panel:
 * Displays all 21 SQL syntax elements with live interactive SQL preview matching DataGrip screenshot 1.
 */
public final class SqlColorSchemePanel {

    private SqlColorSchemePanel() {}

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

        // 2. TreeView of 21 SQL elements
        TreeItem<ColorSchemeElement> treeRoot = new TreeItem<>();
        List<ColorSchemeElement> sqlElements = ColorSchemeModel.getSqlElements();
        Map<String, TreeItem<ColorSchemeElement>> treeItemMap = new HashMap<>();

        for (ColorSchemeElement el : sqlElements) {
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

        // 4. Bottom Live SQL Preview
        VBox previewPane = new VBox(4);
        previewPane.setPadding(new Insets(8, 12, 8, 12));
        previewPane.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-family: 'JetBrains Mono', 'Fira Code', monospace; -fx-font-size: 12px;");
        previewPane.setMinHeight(220);

        ScrollPane previewScroll = new ScrollPane(previewPane);
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

        // 5. Render Preview with click-to-select token interaction
        class TokenBuilder {
            HBox makeToken(String text, String elementId) {
                Label lbl = new Label(text);
                lbl.setStyle("-fx-font-family: 'JetBrains Mono', 'Fira Code', monospace; -fx-font-size: 12px; -fx-cursor: hand;");
                HBox box = new HBox(lbl);
                box.setAlignment(Pos.CENTER_LEFT);

                Runnable styleUpdater = () -> {
                    String curScheme = schemeCombo.getValue() != null ? schemeCombo.getValue() : "Dark Theme default";
                    ColorSchemeAttribute attr = ColorSchemeModel.resolveAttribute(curScheme, elementId, workingOverrides);
                    StringBuilder sb = new StringBuilder("-fx-font-family: 'JetBrains Mono', 'Fira Code', monospace; -fx-font-size: 12px; -fx-cursor: hand;");
                    if (attr != null) {
                        if (attr.bold) sb.append("-fx-font-weight: bold;");
                        if (attr.italic) sb.append("-fx-font-style: italic;");
                        if (attr.foregroundEnabled && attr.foreground != null && !attr.foreground.isBlank()) {
                            sb.append("-fx-text-fill: #").append(attr.foreground).append(";");
                        } else {
                            sb.append("-fx-text-fill: #BCBEC4;");
                        }
                        if (attr.backgroundEnabled && attr.background != null && !attr.background.isBlank()) {
                            box.setStyle("-fx-background-color: #" + attr.background + "; -fx-background-radius: 2;");
                        } else {
                            box.setStyle("-fx-background-color: transparent;");
                        }
                    } else {
                        sb.append("-fx-text-fill: #BCBEC4;");
                        box.setStyle("-fx-background-color: transparent;");
                    }
                    lbl.setStyle(sb.toString());
                };
                styleUpdater.run();
                lbl.getProperties().put("tokenUpdater", styleUpdater);

                box.setOnMouseClicked(e -> {
                    TreeItem<ColorSchemeElement> target = treeItemMap.get(elementId);
                    if (target != null) {
                        treeView.getSelectionModel().select(target);
                        treeView.scrollTo(treeView.getRow(target));
                    }
                });
                return box;
            }

            Label makePlain(String text) {
                Label lbl = new Label(text);
                lbl.setStyle("-fx-font-family: 'JetBrains Mono', 'Fira Code', monospace; -fx-font-size: 12px; -fx-text-fill: #BCBEC4;");
                return lbl;
            }
        }

        TokenBuilder tb = new TokenBuilder();

        Runnable buildPreview = () -> {
            previewPane.getChildren().clear();

            // Line 1: -- DDL section
            previewPane.getChildren().add(new HBox(tb.makeToken("-- DDL section", "sql.comment")));

            // Line 2: create table crm.product (
            previewPane.getChildren().add(new HBox(
                    tb.makeToken("create", "sql.keyword"),
                    tb.makePlain(" "),
                    tb.makeToken("table", "sql.keyword"),
                    tb.makePlain(" "),
                    tb.makeToken("crm", "sql.database_object"),
                    tb.makeToken(".", "sql.dot"),
                    tb.makeToken("product", "sql.database_object"),
                    tb.makePlain(" "),
                    tb.makeToken("(", "sql.parentheses")
            ));

            // Line 3:   id numeric primary key,
            previewPane.getChildren().add(new HBox(
                    tb.makePlain("  "),
                    tb.makeToken("id", "sql.column"),
                    tb.makePlain(" "),
                    tb.makeToken("numeric", "sql.type"),
                    tb.makePlain(" "),
                    tb.makeToken("primary", "sql.keyword"),
                    tb.makePlain(" "),
                    tb.makeToken("key", "sql.keyword"),
                    tb.makeToken(",", "sql.comma")
            ));

            // Line 4:   title varchar(255) character set utf8
            previewPane.getChildren().add(new HBox(
                    tb.makePlain("  "),
                    tb.makeToken("title", "sql.column"),
                    tb.makePlain(" "),
                    tb.makeToken("varchar", "sql.type"),
                    tb.makeToken("(", "sql.parentheses"),
                    tb.makeToken("255", "sql.number_token"),
                    tb.makeToken(")", "sql.parentheses"),
                    tb.makePlain(" "),
                    tb.makeToken("character", "sql.keyword"),
                    tb.makePlain(" "),
                    tb.makeToken("set", "sql.keyword"),
                    tb.makePlain(" "),
                    tb.makeToken("utf8", "sql.database_object")
            ));

            // Line 5: );
            previewPane.getChildren().add(new HBox(
                    tb.makeToken(")", "sql.parentheses"),
                    tb.makeToken(";", "sql.semicolon")
            ));

            // Line 6: -- DML section
            previewPane.getChildren().add(new HBox(tb.makeToken("-- DML section", "sql.comment")));

            // Line 7: insert into product
            previewPane.getChildren().add(new HBox(
                    tb.makeToken("insert", "sql.keyword"),
                    tb.makePlain(" "),
                    tb.makeToken("into", "sql.keyword"),
                    tb.makePlain(" "),
                    tb.makeToken("product", "sql.database_object")
            ));

            // Line 8:   values (1, 'Product1');
            previewPane.getChildren().add(new HBox(
                    tb.makePlain("  "),
                    tb.makeToken("values", "sql.keyword"),
                    tb.makePlain(" "),
                    tb.makeToken("(", "sql.parentheses"),
                    tb.makeToken("1", "sql.number_token"),
                    tb.makeToken(",", "sql.comma"),
                    tb.makePlain(" "),
                    tb.makeToken("'Product1'", "sql.string"),
                    tb.makeToken(")", "sql.parentheses"),
                    tb.makeToken(";", "sql.semicolon")
            ));

            // Line 9: empty
            previewPane.getChildren().add(new Label(""));

            // Line 10: select count(*) from crm.product;
            previewPane.getChildren().add(new HBox(
                    tb.makeToken("select", "sql.keyword"),
                    tb.makePlain(" "),
                    tb.makeToken("count", "sql.procedure"),
                    tb.makeToken("(", "sql.parentheses"),
                    tb.makeToken("*", "sql.database_object"),
                    tb.makeToken(")", "sql.parentheses"),
                    tb.makePlain(" "),
                    tb.makeToken("from", "sql.keyword"),
                    tb.makePlain(" "),
                    tb.makeToken("crm", "sql.database_object"),
                    tb.makeToken(".", "sql.dot"),
                    tb.makeToken("product", "sql.database_object"),
                    tb.makeToken(";", "sql.semicolon")
            ));

            // Line 11: select id as ProductID, title as ProductName
            previewPane.getChildren().add(new HBox(
                    tb.makeToken("select", "sql.keyword"),
                    tb.makePlain(" "),
                    tb.makeToken("id", "sql.column"),
                    tb.makePlain(" "),
                    tb.makeToken("as", "sql.keyword"),
                    tb.makePlain(" "),
                    tb.makeToken("ProductID", "sql.label"),
                    tb.makeToken(",", "sql.comma"),
                    tb.makePlain(" "),
                    tb.makeToken("title", "sql.column"),
                    tb.makePlain(" "),
                    tb.makeToken("as", "sql.keyword"),
                    tb.makePlain(" "),
                    tb.makeToken("ProductName", "sql.label")
            ));

            // Line 12:   from crm.product where id = :id;
            previewPane.getChildren().add(new HBox(
                    tb.makePlain("  "),
                    tb.makeToken("from", "sql.keyword"),
                    tb.makePlain(" "),
                    tb.makeToken("crm", "sql.database_object"),
                    tb.makeToken(".", "sql.dot"),
                    tb.makeToken("product", "sql.database_object"),
                    tb.makePlain(" "),
                    tb.makeToken("where", "sql.keyword"),
                    tb.makePlain(" "),
                    tb.makeToken("id", "sql.column"),
                    tb.makePlain(" = "),
                    tb.makeToken(":id", "sql.parameter"),
                    tb.makeToken(";", "sql.semicolon")
            ));

            // Line 13: empty
            previewPane.getChildren().add(new Label(""));

            // Line 14: \set content `cat data.txt`
            previewPane.getChildren().add(new HBox(
                    tb.makeToken("\\set", "sql.external_command"),
                    tb.makePlain(" "),
                    tb.makeToken("content", "sql.variable"),
                    tb.makePlain(" "),
                    tb.makeToken("`cat data.txt`", "sql.quoted_identifier")
            ));
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

        // Select "Keyword" by default matching DataGrip Screenshot 1
        TreeItem<ColorSchemeElement> defaultSelection = treeItemMap.get("sql.keyword");
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
