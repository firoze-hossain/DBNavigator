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
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.*;
import java.util.function.Consumer;

/**
 * DataGrip-aligned Diff & Merge Color Scheme panel:
 * Displays Changed lines (Changed, Conflict, Deleted, Inserted) and Folded unchanged fragments (Wave)
 * with dedicated Important/Ignored/Error stripe mark controls for Changed lines,
 * and an authentic 3-way interactive side-by-side diff preview buffer matching DataGrip screenshots 2 & 4.
 */
public final class DiffMergePanel {

    private DiffMergePanel() {}

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
        Tooltip.install(helpIcon, new Tooltip("Configure Diff & Merge highlight colors"));

        HBox topBar = new HBox(8, schemeLabel, schemeCombo, gearBtn, themeLink, helpIcon);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // 2. TreeView
        TreeItem<ColorSchemeElement> rootItem = new TreeItem<>();
        rootItem.setExpanded(true);

        TreeItem<ColorSchemeElement> changedLinesCat = new TreeItem<>(new ColorSchemeElement("cat.changed_lines", "Changed lines", List.of("Changed lines"), null, null, null, "Diff & Merge"));
        changedLinesCat.setExpanded(true);
        TreeItem<ColorSchemeElement> foldedCat = new TreeItem<>(new ColorSchemeElement("cat.folded", "Folded unchanged fragments", List.of("Folded unchanged fragments"), null, null, null, "Diff & Merge"));
        foldedCat.setExpanded(true);

        rootItem.getChildren().addAll(changedLinesCat, foldedCat);

        List<ColorSchemeElement> elements = ColorSchemeModel.getDiffMergeElements();
        for (ColorSchemeElement el : elements) {
            if ("Changed lines".equals(el.getCategory())) {
                changedLinesCat.getChildren().add(new TreeItem<>(el));
            } else if ("Folded unchanged fragments".equals(el.getCategory())) {
                foldedCat.getChildren().add(new TreeItem<>(el));
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
        StackPane editorContainer = new StackPane();
        editorContainer.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        editorContainer.setPrefWidth(380);
        editorContainer.setPrefHeight(230);

        final Runnable[] refreshPreviewRef = new Runnable[1];
        final Runnable[] commitChangesRef = new Runnable[1];

        // Panel A: Changed lines editor (Important, Ignored, Error stripe mark, Inherit ignored color)
        VBox changedLinesEditor = new VBox(12);
        changedLinesEditor.setPadding(new Insets(14, 16, 14, 16));

        Label changedLineTitle = new Label("Deleted");
        changedLineTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px;");

        Button importantBtn = new Button("484A4A");
        importantBtn.setPrefWidth(90);
        importantBtn.setPrefHeight(24);
        importantBtn.setStyle("-fx-background-color: #484A4A; -fx-text-fill: #ffffff; -fx-background-radius: 3; -fx-border-radius: 3; -fx-border-color: #393b40; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");

        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        HBox importantRow = new HBox(8, new Label("Important"), sp1, importantBtn);
        importantRow.setAlignment(Pos.CENTER_LEFT);

        Button ignoredBtn = new Button();
        ignoredBtn.setPrefWidth(90);
        ignoredBtn.setPrefHeight(24);
        ignoredBtn.setStyle("-fx-background-color: #2b2d30; -fx-background-radius: 3; -fx-border-radius: 3; -fx-border-color: #393b40; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");

        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);
        HBox ignoredRow = new HBox(8, new Label("Ignored"), sp2, ignoredBtn);
        ignoredRow.setAlignment(Pos.CENTER_LEFT);

        Button errorStripeBtn = new Button("656E76");
        errorStripeBtn.setPrefWidth(90);
        errorStripeBtn.setPrefHeight(24);
        errorStripeBtn.setStyle("-fx-background-color: #656E76; -fx-text-fill: #ffffff; -fx-background-radius: 3; -fx-border-radius: 3; -fx-border-color: #393b40; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");

        Region sp3 = new Region();
        HBox.setHgrow(sp3, Priority.ALWAYS);
        HBox errorStripeRow = new HBox(8, new Label("Error stripe mark"), sp3, errorStripeBtn);
        errorStripeRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox inheritIgnoredCheck = new CheckBox("Inherit ignored color");
        inheritIgnoredCheck.setStyle("-fx-text-fill: -text;");
        inheritIgnoredCheck.setSelected(true);

        changedLinesEditor.getChildren().addAll(changedLineTitle, importantRow, ignoredRow, errorStripeRow, inheritIgnoredCheck);

        // Panel B: Wave editor (Bold, Italic, Foreground, Background, Error stripe, Effects)
        VBox waveEditor = new VBox(10);
        waveEditor.setPadding(new Insets(12, 16, 12, 16));

        Label waveTitle = new Label("Wave");
        waveTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: -text; -fx-font-size: 13px;");

        CheckBox waveBold = new CheckBox("Bold");
        waveBold.setStyle("-fx-text-fill: -text;");
        CheckBox waveItalic = new CheckBox("Italic");
        waveItalic.setStyle("-fx-text-fill: -text;");
        Region waveFontSpacer = new Region();
        HBox.setHgrow(waveFontSpacer, Priority.ALWAYS);
        HBox waveFontRow = new HBox(16, waveFontSpacer, waveBold, waveItalic);
        waveFontRow.setAlignment(Pos.CENTER_RIGHT);

        CheckBox waveFgCheck = new CheckBox("Foreground");
        waveFgCheck.setStyle("-fx-text-fill: -text;");
        waveFgCheck.setPrefWidth(140);
        Button waveFgBtn = new Button("555555");
        waveFgBtn.setPrefWidth(90);
        waveFgBtn.setPrefHeight(24);
        waveFgBtn.setStyle("-fx-background-color: #555555; -fx-text-fill: #ffffff; -fx-background-radius: 3; -fx-border-radius: 3; -fx-border-color: #393b40; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");
        Region waveSpFg = new Region();
        HBox.setHgrow(waveSpFg, Priority.ALWAYS);
        HBox waveFgRow = new HBox(8, waveFgCheck, waveSpFg, waveFgBtn);
        waveFgRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox waveBgCheck = new CheckBox("Background");
        waveBgCheck.setStyle("-fx-text-fill: -text;");
        waveBgCheck.setPrefWidth(140);
        Button waveBgBtn = new Button();
        waveBgBtn.setPrefWidth(90);
        waveBgBtn.setPrefHeight(24);
        waveBgBtn.setStyle("-fx-background-color: #2b2d30; -fx-background-radius: 3; -fx-border-radius: 3; -fx-border-color: #393b40; -fx-cursor: default;");
        Region waveSpBg = new Region();
        HBox.setHgrow(waveSpBg, Priority.ALWAYS);
        HBox waveBgRow = new HBox(8, waveBgCheck, waveSpBg, waveBgBtn);
        waveBgRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox waveErrCheck = new CheckBox("Error stripe mark");
        waveErrCheck.setStyle("-fx-text-fill: -text;");
        waveErrCheck.setPrefWidth(140);
        Button waveErrBtn = new Button();
        waveErrBtn.setPrefWidth(90);
        waveErrBtn.setPrefHeight(24);
        waveErrBtn.setStyle("-fx-background-color: #2b2d30; -fx-background-radius: 3; -fx-border-radius: 3; -fx-border-color: #393b40; -fx-cursor: default;");
        Region waveSpErr = new Region();
        HBox.setHgrow(waveSpErr, Priority.ALWAYS);
        HBox waveErrRow = new HBox(8, waveErrCheck, waveSpErr, waveErrBtn);
        waveErrRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox waveEffectsCheck = new CheckBox("Effects");
        waveEffectsCheck.setStyle("-fx-text-fill: -text;");
        waveEffectsCheck.setPrefWidth(140);
        Button waveEffectsBtn = new Button();
        waveEffectsBtn.setPrefWidth(90);
        waveEffectsBtn.setPrefHeight(24);
        waveEffectsBtn.setStyle("-fx-background-color: #2b2d30; -fx-background-radius: 3; -fx-border-radius: 3; -fx-border-color: #393b40; -fx-cursor: default;");
        Region waveSpEff = new Region();
        HBox.setHgrow(waveSpEff, Priority.ALWAYS);
        HBox waveEffectsRow = new HBox(8, waveEffectsCheck, waveSpEff, waveEffectsBtn);
        waveEffectsRow.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> waveEffectTypeCombo = new ComboBox<>();
        waveEffectTypeCombo.getItems().addAll("Underscored", "Bold Underscored", "Underwaved", "Strikeout", "Bordered", "Dotted line");
        waveEffectTypeCombo.getSelectionModel().select("Underscored");
        waveEffectTypeCombo.setPrefWidth(125);
        waveEffectTypeCombo.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
        Region waveEffectSpacer = new Region();
        HBox.setHgrow(waveEffectSpacer, Priority.ALWAYS);
        HBox waveEffectComboRow = new HBox(waveEffectSpacer, waveEffectTypeCombo);
        waveEffectComboRow.setAlignment(Pos.CENTER_RIGHT);
        VBox waveEffectsContainer = new VBox(6, waveEffectsRow, waveEffectComboRow);

        waveEditor.getChildren().addAll(waveTitle, waveFontRow, waveFgRow, waveBgRow, waveErrRow, waveEffectsContainer);

        editorContainer.getChildren().addAll(changedLinesEditor, waveEditor);

        // State holder for active element
        final String[] activeElementId = new String[]{"diff.deleted"};
        final String[] curImportantHex = new String[]{"484A4A"};
        final String[] curIgnoredHex = new String[]{null};
        final String[] curErrorStripeHex = new String[]{"656E76"};
        final String[] curWaveFgHex = new String[]{"555555"};

        // Color button click handlers
        importantBtn.setOnAction(e -> showColorPickerDialog(curImportantHex[0], importantBtn, hex -> {
            curImportantHex[0] = hex;
            setButtonHex(importantBtn, hex);
            if (commitChangesRef[0] != null) commitChangesRef[0].run();
        }));
        ignoredBtn.setOnAction(e -> showColorPickerDialog(curIgnoredHex[0], ignoredBtn, hex -> {
            curIgnoredHex[0] = hex;
            setButtonHex(ignoredBtn, hex);
            if (commitChangesRef[0] != null) commitChangesRef[0].run();
        }));
        errorStripeBtn.setOnAction(e -> showColorPickerDialog(curErrorStripeHex[0], errorStripeBtn, hex -> {
            curErrorStripeHex[0] = hex;
            setButtonHex(errorStripeBtn, hex);
            if (commitChangesRef[0] != null) commitChangesRef[0].run();
        }));
        waveFgBtn.setOnAction(e -> {
            if (waveFgCheck.isSelected()) {
                showColorPickerDialog(curWaveFgHex[0], waveFgBtn, hex -> {
                    curWaveFgHex[0] = hex;
                    setButtonHex(waveFgBtn, hex);
                    if (commitChangesRef[0] != null) commitChangesRef[0].run();
                });
            }
        });

        inheritIgnoredCheck.setOnAction(e -> {
            boolean inh = inheritIgnoredCheck.isSelected();
            ignoredBtn.setDisable(inh);
            if (commitChangesRef[0] != null) commitChangesRef[0].run();
        });

        // 4. Live 3-Way Diff Preview (Bottom Pane)
        VBox previewPane = new VBox(4);
        previewPane.setPadding(new Insets(8, 10, 8, 10));
        previewPane.setStyle("-fx-background-color: #1e1f22;");

        ScrollPane previewScroll = new ScrollPane(previewPane);
        previewScroll.setFitToWidth(true);
        previewScroll.setPrefHeight(230);
        previewScroll.setStyle("-fx-background: #1e1f22; -fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(previewScroll, Priority.ALWAYS);

        refreshPreviewRef[0] = () -> {
            previewPane.getChildren().clear();
            String activeScheme = schemeCombo.getValue();

            ColorSchemeAttribute changedAttr = ColorSchemeModel.resolveAttribute(activeScheme, "diff.changed", workingOverrides);
            ColorSchemeAttribute conflictAttr = ColorSchemeModel.resolveAttribute(activeScheme, "diff.conflict", workingOverrides);
            ColorSchemeAttribute deletedAttr = ColorSchemeModel.resolveAttribute(activeScheme, "diff.deleted", workingOverrides);
            ColorSchemeAttribute insertedAttr = ColorSchemeModel.resolveAttribute(activeScheme, "diff.inserted", workingOverrides);
            ColorSchemeAttribute waveAttr = ColorSchemeModel.resolveAttribute(activeScheme, "diff.folded_wave", workingOverrides);

            String changedBg = (changedAttr.backgroundEnabled && changedAttr.background != null) ? "#" + changedAttr.background : "#2E436E";
            String conflictBg = (conflictAttr.backgroundEnabled && conflictAttr.background != null) ? "#" + conflictAttr.background : "#5E3838";
            String deletedBg = (deletedAttr.backgroundEnabled && deletedAttr.background != null) ? "#" + deletedAttr.background : "#484A4A";
            String insertedBg = (insertedAttr.backgroundEnabled && insertedAttr.background != null) ? "#" + insertedAttr.background : "#294436";
            String waveColor = (waveAttr.foregroundEnabled && waveAttr.foreground != null) ? "#" + waveAttr.foreground : "#555555";

            // Headers with lock icons
            Label lock1 = new Label("  🔒  Left");
            lock1.setStyle("-fx-text-fill: #868a91; -fx-font-size: 11px;");
            Label lock2 = new Label("  🔒  Center (Base / Merged)");
            lock2.setStyle("-fx-text-fill: #868a91; -fx-font-size: 11px;");
            Label lock3 = new Label("  🔒  Right");
            lock3.setStyle("-fx-text-fill: #868a91; -fx-font-size: 11px;");

            HBox headerRow = new HBox(20, lock1, lock2, lock3);
            headerRow.setStyle("-fx-padding: 0 0 6 0; -fx-border-color: transparent transparent #393b40 transparent; -fx-border-width: 0 0 1 0;");
            previewPane.getChildren().add(headerRow);

            // Lines 1 to 14
            String[][] leftCode = {
                    {"1", "class MyClass {", null, null},
                    {"2", "    int value;", null, null},
                    {"3", "", null, null},
                    {"4", "    void leftOnly() {}", insertedBg, "diff.inserted"},
                    {"5", "", insertedBg, "diff.inserted"},
                    {"6", "    void foo() {", null, null},
                    {"7", "        // Left changes", conflictBg, "diff.conflict"},
                    {"8", "    }", null, null},
                    {"9", "", null, null},
                    {"10", "    void bar() {", null, null},
                    {"11", "", null, null},
                    {"12", "    }", null, null},
                    {"13", "}", null, null}
            };

            String[][] centerCode = {
                    {"1", "class MyClass {", null, null},
                    {"2", "    int value;", changedBg, "diff.changed"},
                    {"3", "", null, null},
                    {"4", "    void foo() {", null, null},
                    {"5", "    }", null, null},
                    {"6", "    void removedFromLeft() {}", deletedBg, "diff.deleted"},
                    {"7", "", deletedBg, "diff.deleted"},
                    {"8", "", deletedBg, "diff.deleted"},
                    {"9", "    void bar() {", null, null},
                    {"10", "    }", null, null},
                    {"11", "", null, null},
                    {"12", "}", null, null},
                    {"13", "", null, null}
            };

            String[][] rightCode = {
                    {"1", "class MyClass {", null, null},
                    {"2", "    long value;", changedBg, "diff.changed"},
                    {"3", "", null, null},
                    {"4", "    void foo() {", null, null},
                    {"5", "        // Right changes", conflictBg, "diff.conflict"},
                    {"6", "    }", null, null},
                    {"7", "", null, null},
                    {"8", "    void removedFromLeft() {}", null, null},
                    {"9", "", null, null},
                    {"10", "    void bar() {", null, null},
                    {"11", "    }", null, null},
                    {"12", "}", insertedBg, "diff.inserted"},
                    {"13", "", null, null}
            };

            for (int i = 0; i < 13; i++) {
                HBox leftCol = makeDiffLine(leftCode[i][0], leftCode[i][1], leftCode[i][2], leftCode[i][3], treeView, rootItem);
                HBox centerCol = makeDiffLine(centerCode[i][0], centerCode[i][1], centerCode[i][2], centerCode[i][3], treeView, rootItem);
                HBox rightCol = makeDiffLine(rightCode[i][0], rightCode[i][1], rightCode[i][2], rightCode[i][3], treeView, rootItem);

                HBox.setHgrow(leftCol, Priority.ALWAYS);
                HBox.setHgrow(centerCol, Priority.ALWAYS);
                HBox.setHgrow(rightCol, Priority.ALWAYS);

                HBox row = new HBox(8, leftCol, centerCol, rightCol);
                row.setAlignment(Pos.CENTER_LEFT);
                previewPane.getChildren().add(row);
            }

            // Line 14: Folded unchanged fragments wave
            Label wave1 = new Label("14  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            Label wave2 = new Label("14  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            Label wave3 = new Label("14  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            String waveStyle = "-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px; -fx-text-fill: " + waveColor + "; -fx-cursor: hand;";
            wave1.setStyle(waveStyle);
            wave2.setStyle(waveStyle);
            wave3.setStyle(waveStyle);
            HBox.setHgrow(wave1, Priority.ALWAYS);
            HBox.setHgrow(wave2, Priority.ALWAYS);
            HBox.setHgrow(wave3, Priority.ALWAYS);

            HBox waveRow = new HBox(8, wave1, wave2, wave3);
            waveRow.setAlignment(Pos.CENTER_LEFT);
            waveRow.setOnMouseClicked(e -> selectTreeItemById(treeView, rootItem, "diff.folded_wave"));
            previewPane.getChildren().add(waveRow);
        };

        // Commit logic
        commitChangesRef[0] = () -> {
            TreeItem<ColorSchemeElement> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected == null || selected.getValue() == null) return;
            ColorSchemeElement el = selected.getValue();
            if (el.getCategoryPath().size() < 2) return;

            String active = schemeCombo.getValue();
            Map<String, ColorSchemeAttribute> map = workingOverrides.computeIfAbsent(active, k -> new LinkedHashMap<>());

            if ("Folded unchanged fragments".equals(el.getCategory())) {
                ColorSchemeAttribute current = new ColorSchemeAttribute(
                        waveBold.isSelected(),
                        waveItalic.isSelected(),
                        curWaveFgHex[0],
                        waveFgCheck.isSelected(),
                        null,
                        waveBgCheck.isSelected(),
                        null,
                        waveErrCheck.isSelected(),
                        null,
                        waveEffectsCheck.isSelected(),
                        waveEffectTypeCombo.getValue(),
                        false,
                        null
                );
                map.put(el.getId(), current);
            } else {
                ColorSchemeAttribute current = new ColorSchemeAttribute(
                        false,
                        false,
                        null,
                        false,
                        curImportantHex[0],
                        true,
                        curErrorStripeHex[0],
                        true,
                        null,
                        false,
                        "Underscored",
                        false,
                        null,
                        curIgnoredHex[0],
                        !inheritIgnoredCheck.isSelected(),
                        inheritIgnoredCheck.isSelected()
                );
                map.put(el.getId(), current);
            }
            if (refreshPreviewRef[0] != null) refreshPreviewRef[0].run();
        };

        // Wave controls listeners
        waveBold.setOnAction(e -> { if (commitChangesRef[0] != null) commitChangesRef[0].run(); });
        waveItalic.setOnAction(e -> { if (commitChangesRef[0] != null) commitChangesRef[0].run(); });
        waveFgCheck.setOnAction(e -> { if (commitChangesRef[0] != null) commitChangesRef[0].run(); });
        waveBgCheck.setOnAction(e -> { if (commitChangesRef[0] != null) commitChangesRef[0].run(); });
        waveErrCheck.setOnAction(e -> { if (commitChangesRef[0] != null) commitChangesRef[0].run(); });
        waveEffectsCheck.setOnAction(e -> { if (commitChangesRef[0] != null) commitChangesRef[0].run(); });
        waveEffectTypeCombo.setOnAction(e -> { if (commitChangesRef[0] != null) commitChangesRef[0].run(); });

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem == null || newItem.getValue() == null) return;
            ColorSchemeElement el = newItem.getValue();
            if (el.getCategoryPath().size() < 2) return; // skip category headers

            activeElementId[0] = el.getId();
            String active = schemeCombo.getValue();
            ColorSchemeAttribute attr = ColorSchemeModel.resolveAttribute(active, el.getId(), workingOverrides);

            if ("Folded unchanged fragments".equals(el.getCategory())) {
                changedLinesEditor.setVisible(false);
                changedLinesEditor.setManaged(false);
                waveEditor.setVisible(true);
                waveEditor.setManaged(true);

                waveTitle.setText(el.getName());
                waveBold.setSelected(attr.bold);
                waveItalic.setSelected(attr.italic);
                waveFgCheck.setSelected(attr.foregroundEnabled);
                curWaveFgHex[0] = attr.foreground;
                setButtonHex(waveFgBtn, attr.foreground);

                waveBgCheck.setSelected(attr.backgroundEnabled);
                waveErrCheck.setSelected(attr.errorStripeEnabled);
                waveEffectsCheck.setSelected(attr.effectEnabled);
                if (attr.effectType != null) waveEffectTypeCombo.setValue(attr.effectType);
            } else {
                waveEditor.setVisible(false);
                waveEditor.setManaged(false);
                changedLinesEditor.setVisible(true);
                changedLinesEditor.setManaged(true);

                changedLineTitle.setText(el.getName());
                curImportantHex[0] = attr.background;
                setButtonHex(importantBtn, attr.background);

                curIgnoredHex[0] = attr.ignoredColor;
                setButtonHex(ignoredBtn, attr.ignoredColor);

                curErrorStripeHex[0] = attr.errorStripe;
                setButtonHex(errorStripeBtn, attr.errorStripe);

                inheritIgnoredCheck.setSelected(attr.inheritIgnored);
                ignoredBtn.setDisable(attr.inheritIgnored);
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

        // Pre-select "Deleted" in Changed lines (matching Image 2)
        selectTreeItemById(treeView, rootItem, "diff.deleted");

        refreshPreviewRef[0].run();

        HBox splitTop = new HBox(10, treeView, editorContainer);
        HBox.setHgrow(treeView, Priority.ALWAYS);
        HBox.setHgrow(editorContainer, Priority.ALWAYS);

        Label previewHeader = new Label("Preview:");
        previewHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #868a91; -fx-font-size: 11px;");

        root.getChildren().addAll(topBar, new Separator(), splitTop, previewHeader, previewScroll);
        return root;
    }

    private static HBox makeDiffLine(String lineNum, String code, String bgColor, String elementId,
                                     TreeView<ColorSchemeElement> treeView, TreeItem<ColorSchemeElement> rootItem) {
        Label numLabel = new Label(String.format("%2s ", lineNum));
        numLabel.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px; -fx-text-fill: #5a5d60;");

        Label codeLabel = new Label(code.isEmpty() ? " " : code);
        codeLabel.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px; -fx-text-fill: #bcbec4;");

        HBox lineBox = new HBox(4, numLabel, codeLabel);
        lineBox.setAlignment(Pos.CENTER_LEFT);
        lineBox.setPadding(new Insets(1, 4, 1, 4));

        if (bgColor != null) {
            lineBox.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 2; -fx-cursor: hand;");
            if (elementId != null) {
                lineBox.setOnMouseClicked(e -> selectTreeItemById(treeView, rootItem, elementId));
            }
        }
        return lineBox;
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

    private static void setButtonHex(Button btn, String hex) {
        if (hex != null && !hex.isBlank()) {
            String clean = hex.replace("#", "").toUpperCase();
            btn.setText(clean);
            boolean dark = isColorDark(clean);
            btn.setStyle("-fx-background-color: #" + clean + "; -fx-text-fill: " + (dark ? "#ffffff" : "#000000") +
                    "; -fx-background-radius: 3; -fx-border-radius: 3; -fx-border-color: #393b40; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            btn.setText("");
            btn.setStyle("-fx-background-color: #2b2d30; -fx-background-radius: 3; -fx-border-radius: 3; -fx-border-color: #393b40; -fx-font-size: 11px; -fx-cursor: default;");
        }
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
                "484A4A", "656E76", "2E436E", "385570", "294436", "436946", "5E3838", "D54040",
                "555555", "383838", "2E3540", "203328", "402828", "DFE1E5", "2B2D30", "1E1F22"
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
