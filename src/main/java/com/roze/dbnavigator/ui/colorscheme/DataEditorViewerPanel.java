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
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.*;
import java.util.function.Consumer;

/**
 * DataGrip-identical Data Editor and Viewer Color Scheme panel:
 * Displays Data Grid category (4 items: Alternating row color, Error data, Image data, Null data)
 * with live data cell and error stripe preview matching DataGrip screenshot 2.
 */
public final class DataEditorViewerPanel {

    private DataEditorViewerPanel() {}

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
        Tooltip.install(helpIcon, new Tooltip("Configure Data Grid and viewer cell colors"));

        HBox topBar = new HBox(8, schemeLabel, schemeCombo, gearBtn, themeLink, helpIcon);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // 2. TreeView with Data Grid category
        TreeItem<ColorSchemeElement> rootItem = new TreeItem<>();
        rootItem.setExpanded(true);

        TreeItem<ColorSchemeElement> dataGridCategory = new TreeItem<>(
                new ColorSchemeElement("cat.data_grid", "Data Grid", List.of("Data Grid"), null, null, null, "Data Editor and Viewer")
        );
        dataGridCategory.setExpanded(true);
        rootItem.getChildren().add(dataGridCategory);

        List<ColorSchemeElement> elements = ColorSchemeModel.getDataEditorViewerElements();
        TreeItem<ColorSchemeElement> initialSelectionItem = null;
        for (ColorSchemeElement el : elements) {
            TreeItem<ColorSchemeElement> item = new TreeItem<>(el);
            dataGridCategory.getChildren().add(item);
            if ("data_editor.error_data".equals(el.getId())) {
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

        Label selectedElementNameLabel = new Label("Error data");
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
        effectTypeCombo.getSelectionModel().select("Underwaved");
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

        // 4. Live Data Preview (Bottom Pane matching Image 2)
        VBox previewPane = new VBox(4);
        previewPane.setPadding(new Insets(8, 12, 8, 12));
        previewPane.setStyle("-fx-background-color: #1e1f22;");

        String[] currentSelectedRef = new String[]{"data_editor.error_data"};
        Pane errorStripePane = new Pane();
        errorStripePane.setPrefWidth(16);
        errorStripePane.setStyle("-fx-background-color: #2b2d30;");

        refreshPreviewRef[0] = () -> {
            previewPane.getChildren().clear();
            errorStripePane.getChildren().clear();
            String active = schemeCombo.getValue();

            ColorSchemeAttribute altRowAttr = ColorSchemeModel.resolveAttribute(active, "data_editor.alternating_row_color", workingOverrides);
            ColorSchemeAttribute errorAttr = ColorSchemeModel.resolveAttribute(active, "data_editor.error_data", workingOverrides);
            ColorSchemeAttribute imageAttr = ColorSchemeModel.resolveAttribute(active, "data_editor.image_data", workingOverrides);
            ColorSchemeAttribute nullAttr = ColorSchemeModel.resolveAttribute(active, "data_editor.null_data", workingOverrides);

            // Row 1: null
            HBox row1 = new HBox(1);
            row1.setPadding(new Insets(2, 4, 2, 4));
            Label nullLabel = makeTokenLabel("null", nullAttr, "data_editor.null_data", treeView, rootItem, currentSelectedRef);
            row1.getChildren().add(nullLabel);

            // Row 2: 400x300 PNG image 7.14K (with image data bg or alternating row bg)
            HBox row2 = new HBox(1);
            row2.setPadding(new Insets(2, 4, 2, 4));
            if (altRowAttr.backgroundEnabled && altRowAttr.background != null && !altRowAttr.background.isBlank()) {
                row2.setStyle("-fx-background-color: #" + altRowAttr.background + ";");
            }
            Label imageLabel = makeTokenLabel("400x300 PNG image 7.14K", imageAttr, "data_editor.image_data", treeView, rootItem, currentSelectedRef);
            row2.getChildren().add(imageLabel);

            // Row 3: <failed to load>
            HBox row3 = new HBox(1);
            row3.setPadding(new Insets(2, 4, 2, 4));
            Label errorLabel = makeTokenLabel("<failed to load>", errorAttr, "data_editor.error_data", treeView, rootItem, currentSelectedRef);
            row3.getChildren().add(errorLabel);

            previewPane.getChildren().addAll(row1, row2, row3);

            // Right scrollbar error stripe mark
            if (errorAttr.errorStripeEnabled && errorAttr.errorStripe != null && !errorAttr.errorStripe.isBlank()) {
                Rectangle stripe = new Rectangle(12, 4);
                stripe.setFill(Color.web("#" + errorAttr.errorStripe));
                stripe.setLayoutX(2);
                stripe.setLayoutY(54); // aligned with Row 3
                errorStripePane.getChildren().add(stripe);
            }
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
                        navigateTo.accept("Editor / Color Scheme / General");
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

        // Initial selection: Error data (matching Image 2)
        if (initialSelectionItem != null) {
            treeView.getSelectionModel().select(initialSelectionItem);
        } else if (!dataGridCategory.getChildren().isEmpty()) {
            treeView.getSelectionModel().select(dataGridCategory.getChildren().get(0));
        }

        // SplitPane for tree & editor
        SplitPane middleSplit = new SplitPane();
        middleSplit.getItems().addAll(treeView, attrEditor);
        middleSplit.setDividerPositions(0.48);
        middleSplit.setPrefHeight(250);
        middleSplit.setStyle("-fx-background-color: transparent;");

        // Preview container with right margin line and scrollbar error stripe
        BorderPane previewWrapper = new BorderPane();
        previewWrapper.setStyle("-fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-background-color: #1e1f22;");

        StackPane centerStack = new StackPane();
        ScrollPane previewScroll = new ScrollPane(previewPane);
        previewScroll.setFitToWidth(true);
        previewScroll.setPrefHeight(230);
        previewScroll.setStyle("-fx-background-color: transparent; -fx-background: #1e1f22; -fx-border-color: transparent;");

        Line marginLine = new Line(520, 0, 520, 1000);
        marginLine.setStroke(Color.web("#2b2d30"));
        marginLine.setStrokeWidth(1);
        Pane linePane = new Pane(marginLine);
        linePane.setMouseTransparent(true);
        centerStack.getChildren().addAll(previewScroll, linePane);

        previewWrapper.setCenter(centerStack);
        previewWrapper.setRight(errorStripePane);

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

        StringBuilder style = new StringBuilder("-fx-padding: 2 4;");
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
                String effColor = (attr.effectColor != null && !attr.effectColor.isBlank()) ? "#" + attr.effectColor : "#fa6675";
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
                "D6405B", "FA6675", "25272B", "393B40", "7A7E85", "BCBEC4",
                "CF8E6D", "C77DBB", "56A8F5", "2BBAC5", "2AACB8", "6AAB73"
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
