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
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.*;
import java.util.function.Consumer;

/**
 * DataGrip-aligned VCS Color Scheme panel:
 * Displays Editor Gutter (9 items) and VCS Annotations (7 items)
 * with an authentic 3-column live editor preview matching DataGrip screenshot 1.
 */
public final class VcsPanel {

    private VcsPanel() {}

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
        Tooltip.install(helpIcon, new Tooltip("Configure Version Control highlighting and gutter markers"));

        HBox topBar = new HBox(8, schemeLabel, schemeCombo, gearBtn, themeLink, helpIcon);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // 2. TreeView
        TreeItem<ColorSchemeElement> rootItem = new TreeItem<>();
        rootItem.setExpanded(true);

        TreeItem<ColorSchemeElement> gutterCat = new TreeItem<>(new ColorSchemeElement("cat.gutter", "Editor Gutter", List.of("Editor Gutter"), null, null, null, "VCS"));
        gutterCat.setExpanded(true);
        TreeItem<ColorSchemeElement> annotationsCat = new TreeItem<>(new ColorSchemeElement("cat.annotations", "VCS Annotations", List.of("VCS Annotations"), null, null, null, "VCS"));
        annotationsCat.setExpanded(true);

        rootItem.getChildren().addAll(gutterCat, annotationsCat);

        List<ColorSchemeElement> elements = ColorSchemeModel.getVcsElements();
        for (ColorSchemeElement el : elements) {
            if ("Editor Gutter".equals(el.getCategory())) {
                gutterCat.getChildren().add(new TreeItem<>(el));
            } else if ("VCS Annotations".equals(el.getCategory())) {
                annotationsCat.getChildren().add(new TreeItem<>(el));
            }
        }

        TreeView<ColorSchemeElement> treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(false);
        treeView.setPrefWidth(300);
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
        attrEditor.setPrefWidth(380);
        attrEditor.setPrefHeight(230);

        Label selectedElementNameLabel = new Label("Deleted lines");
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

        // 4. Live VCS Preview (Bottom Pane)
        VBox previewPane = new VBox(1);
        previewPane.setPadding(new Insets(6, 8, 6, 8));
        previewPane.setStyle("-fx-background-color: #1e1f22;");

        ScrollPane previewScroll = new ScrollPane(previewPane);
        previewScroll.setFitToWidth(true);
        previewScroll.setPrefHeight(230);
        previewScroll.setStyle("-fx-background: #1e1f22; -fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(previewScroll, Priority.ALWAYS);

        refreshPreviewRef[0] = () -> {
            previewPane.getChildren().clear();
            String activeScheme = schemeCombo.getValue();

            // VCS Annotations colors
            ColorSchemeAttribute a1 = ColorSchemeModel.resolveAttribute(activeScheme, "vcs.annotations.bg_color_1", workingOverrides);
            ColorSchemeAttribute a2 = ColorSchemeModel.resolveAttribute(activeScheme, "vcs.annotations.bg_color_2", workingOverrides);
            ColorSchemeAttribute a3 = ColorSchemeModel.resolveAttribute(activeScheme, "vcs.annotations.bg_color_3", workingOverrides);
            ColorSchemeAttribute aFg = ColorSchemeModel.resolveAttribute(activeScheme, "vcs.annotations.foreground", workingOverrides);
            ColorSchemeAttribute aFgLast = ColorSchemeModel.resolveAttribute(activeScheme, "vcs.annotations.foreground_last_commit", workingOverrides);

            String a1Bg = (a1.backgroundEnabled && a1.background != null) ? "#" + a1.background : "#25324D";
            String a2Bg = (a2.backgroundEnabled && a2.background != null) ? "#" + a2.background : "#2E3A4D";
            String a3Bg = (a3.backgroundEnabled && a3.background != null) ? "#" + a3.background : "#384659";
            String fgNorm = (aFg.foregroundEnabled && aFg.foreground != null) ? "#" + aFg.foreground : "#868A91";
            String fgLast = (aFgLast.foregroundEnabled && aFgLast.foreground != null) ? "#" + aFgLast.foreground : "#DFE1E5";

            // Gutter colors
            ColorSchemeAttribute addedAttr = ColorSchemeModel.resolveAttribute(activeScheme, "vcs.gutter.added_lines", workingOverrides);
            ColorSchemeAttribute modifiedAttr = ColorSchemeModel.resolveAttribute(activeScheme, "vcs.gutter.modified_lines", workingOverrides);
            ColorSchemeAttribute deletedAttr = ColorSchemeModel.resolveAttribute(activeScheme, "vcs.gutter.deleted_lines", workingOverrides);
            ColorSchemeAttribute wsAttr = ColorSchemeModel.resolveAttribute(activeScheme, "vcs.gutter.whitespace_modified_lines", workingOverrides);
            ColorSchemeAttribute delIgnoredBorder = ColorSchemeModel.resolveAttribute(activeScheme, "vcs.gutter.deleted_ignored_lines_border", workingOverrides);
            ColorSchemeAttribute modIgnoredBorder = ColorSchemeModel.resolveAttribute(activeScheme, "vcs.gutter.modified_ignored_lines_border", workingOverrides);

            String addedColor = (addedAttr.backgroundEnabled && addedAttr.background != null) ? "#" + addedAttr.background : "#436946";
            String modColor = (modifiedAttr.backgroundEnabled && modifiedAttr.background != null) ? "#" + modifiedAttr.background : "#385570";
            String delColor = (deletedAttr.backgroundEnabled && deletedAttr.background != null) ? "#" + deletedAttr.background : "#868A91";
            String wsColor = (wsAttr.backgroundEnabled && wsAttr.background != null) ? "#" + wsAttr.background : "#4B5059";
            String delIgnBorder = (delIgnoredBorder.effectEnabled && delIgnoredBorder.effectColor != null) ? "#" + delIgnoredBorder.effectColor : "#868A91";
            String modIgnBorder = (modIgnoredBorder.effectEnabled && modIgnoredBorder.effectColor != null) ? "#" + modIgnoredBorder.effectColor : "#385570";

            // 14 lines matching Image 1
            Object[][] lines = {
                    { "Annotation background #1", a1Bg, fgNorm, "vcs.annotations.bg_color_1", 1, "Deleted line below", delColor, "vcs.gutter.deleted_lines", "notch" },
                    { "Annotation background", a1Bg, fgNorm, "vcs.annotations.bg_color_1", 2, "", null, null, null },
                    { "Annotation background", a1Bg, fgNorm, "vcs.annotations.bg_color_1", 3, "Modified line", modColor, "vcs.gutter.modified_lines", "bar" },
                    { "Annotation background", a1Bg, fgNorm, "vcs.annotations.bg_color_1", 4, "", null, null, null },
                    { "Annotation background", a1Bg, fgNorm, "vcs.annotations.bg_color_1", 5, "Added line", addedColor, "vcs.gutter.added_lines", "bar" },
                    { "Annotation background #2", a2Bg, fgNorm, "vcs.annotations.bg_color_2", 6, "", null, null, null },
                    { "Annotation background", a2Bg, fgNorm, "vcs.annotations.bg_color_2", 7, "Line with modified whitespaces", wsColor, "vcs.gutter.whitespace_modified_lines", "bar" },
                    { "Annotation background", a2Bg, fgNorm, "vcs.annotations.bg_color_2", 8, "", null, null, null },
                    { "Annotation background", a2Bg, fgNorm, "vcs.annotations.bg_color_2", 9, "Added line", addedColor, "vcs.gutter.added_lines", "bar" },
                    { "Annotation background", a2Bg, fgNorm, "vcs.annotations.bg_color_2", 10, "Line with modified whitespaces and deletion after", wsColor, "vcs.gutter.whitespace_modified_lines", "ws_del" },
                    { "Annotation background #3", a3Bg, fgNorm, "vcs.annotations.bg_color_3", 11, "", null, null, null },
                    { "Annotation background", a3Bg, fgNorm, "vcs.annotations.bg_color_3", 12, "Deleted ignored line below", delIgnBorder, "vcs.gutter.deleted_ignored_lines_border", "border_notch" },
                    { "Annotation background", a3Bg, fgNorm, "vcs.annotations.bg_color_3", 13, "", null, null, null },
                    { "Annotation background", a3Bg, fgLast, "vcs.annotations.foreground_last_commit", 14, "Modified ignored line", modIgnBorder, "vcs.gutter.modified_ignored_lines_border", "border_bar" }
            };

            for (Object[] row : lines) {
                String annotText = (String) row[0];
                String annotBg = (String) row[1];
                String annotFg = (String) row[2];
                String annotId = (String) row[3];
                int lineNum = (int) row[4];
                String codeText = (String) row[5];
                String markerColor = (String) row[6];
                String markerId = (String) row[7];
                String markerType = (String) row[8];

                // 1. Annotation column
                Label annotLabel = new Label(annotText);
                annotLabel.setPrefWidth(180);
                annotLabel.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px; -fx-padding: 1 6; -fx-background-color: " +
                        annotBg + "; -fx-text-fill: " + annotFg + "; -fx-cursor: hand;");
                annotLabel.setOnMouseClicked(e -> selectTreeItemById(treeView, rootItem, annotId));

                // 2. Line number
                Label numLabel = new Label(String.format("%2d", lineNum));
                numLabel.setPrefWidth(24);
                numLabel.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px; -fx-text-fill: #5a5d60;");

                // 3. Gutter marker
                Pane markerPane = new Pane();
                markerPane.setPrefSize(8, 16);
                if ("bar".equals(markerType)) {
                    Rectangle rect = new Rectangle(0, 0, 4, 16);
                    rect.setFill(Color.web(markerColor));
                    markerPane.getChildren().add(rect);
                } else if ("notch".equals(markerType)) {
                    Polygon triangle = new Polygon(0.0, 12.0, 6.0, 16.0, 0.0, 16.0);
                    triangle.setFill(Color.web(markerColor));
                    markerPane.getChildren().add(triangle);
                } else if ("ws_del".equals(markerType)) {
                    Rectangle rect = new Rectangle(0, 0, 3, 16);
                    rect.setFill(Color.web(markerColor));
                    Polygon triangle = new Polygon(0.0, 12.0, 6.0, 16.0, 0.0, 16.0);
                    triangle.setFill(Color.web(delColor));
                    markerPane.getChildren().addAll(rect, triangle);
                } else if ("border_bar".equals(markerType)) {
                    Rectangle rect = new Rectangle(0, 0, 4, 16);
                    rect.setFill(Color.TRANSPARENT);
                    rect.setStroke(Color.web(markerColor));
                    rect.setStrokeWidth(1);
                    markerPane.getChildren().add(rect);
                } else if ("border_notch".equals(markerType)) {
                    Polygon triangle = new Polygon(0.0, 12.0, 6.0, 16.0, 0.0, 16.0);
                    triangle.setFill(Color.TRANSPARENT);
                    triangle.setStroke(Color.web(markerColor));
                    triangle.setStrokeWidth(1);
                    markerPane.getChildren().add(triangle);
                }
                markerPane.setStyle("-fx-cursor: hand;");
                if (markerId != null) {
                    markerPane.setOnMouseClicked(e -> selectTreeItemById(treeView, rootItem, markerId));
                }

                // 4. Code text
                Label codeLabel = new Label("  " + codeText);
                codeLabel.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px; -fx-text-fill: #bcbec4; -fx-cursor: hand;");
                if (markerId != null) {
                    codeLabel.setOnMouseClicked(e -> selectTreeItemById(treeView, rootItem, markerId));
                }

                HBox lineRow = new HBox(4, annotLabel, numLabel, markerPane, codeLabel);
                lineRow.setAlignment(Pos.CENTER_LEFT);
                previewPane.getChildren().add(lineRow);
            }
        };

        // Commit logic
        commitAttrChangesRef[0] = () -> {
            TreeItem<ColorSchemeElement> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected == null || selected.getValue() == null) return;
            ColorSchemeElement el = selected.getValue();
            if (el.getCategoryPath().size() < 2) return;

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
            if (el.getCategoryPath().size() < 2) return; // skip category headers

            selectedElementNameLabel.setText(el.getName());
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
            if (attr.effectType != null) effectTypeCombo.setValue(attr.effectType);

            boolean hasInh = el.hasInheritance();
            inheritBox.setVisible(hasInh);
            inheritBox.setManaged(hasInh);
            if (hasInh) {
                inheritCheck.setSelected(attr.inherit);
                inheritLink.setText(el.getInheritFromDisplay());
            }
        });

        schemeCombo.setOnAction(e -> {
            if (refreshPreviewRef[0] != null) refreshPreviewRef[0].run();
            TreeItem<ColorSchemeElement> cur = treeView.getSelectionModel().getSelectedItem();
            if (cur != null) {
                treeView.getSelectionModel().clearSelection();
                treeView.getSelectionModel().select(cur);
            }
        });

        // Pre-select "Deleted lines" in Editor Gutter (matching Image 1)
        selectTreeItemById(treeView, rootItem, "vcs.gutter.deleted_lines");

        refreshPreviewRef[0].run();

        HBox splitTop = new HBox(10, treeView, attrEditor);
        HBox.setHgrow(treeView, Priority.ALWAYS);
        HBox.setHgrow(attrEditor, Priority.ALWAYS);

        Label previewHeader = new Label("Preview:");
        previewHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #868a91; -fx-font-size: 11px;");

        root.getChildren().addAll(topBar, new Separator(), splitTop, previewHeader, previewScroll);
        return root;
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
                "868A91", "436946", "385570", "4B5059", "393B40", "2B2D30", "25324D", "2E3A4D",
                "384659", "425266", "4C5E73", "DFE1E5", "5594FA", "F75464", "E5B842", "1E1F22"
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
