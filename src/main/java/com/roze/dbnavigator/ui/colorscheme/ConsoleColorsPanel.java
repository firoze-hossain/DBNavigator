package com.roze.dbnavigator.ui.colorscheme;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.db.AppSettingsStore.ColorSchemeAttribute;
import com.roze.dbnavigator.ui.Icons;
import com.roze.dbnavigator.ui.colorscheme.ColorSchemeModel.ColorSchemeElement;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.*;

/**
 * Dedicated DataGrip-identical panel for Editor > Color Scheme > Console Colors.
 * Provides hierarchical tree management of ANSI colors, Console outputs, Log console,
 * and Terminal attributes, with inheritance resolution and authentic live CLI preview.
 */
public final class ConsoleColorsPanel {

    public static VBox build(AppSettingsStore.Settings settings,
                             Map<String, Object> inputs,
                             java.util.function.Consumer<String> navigateTo) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10, 16, 16, 16));

        // Working state
        String currentScheme = settings.getEditorColorScheme();
        List<String> workingCustomSchemes = (inputs.containsKey("customColorSchemes"))
                ? (List<String>) inputs.get("customColorSchemes")
                : new ArrayList<>(settings.getCustomColorSchemes());
        Map<String, Map<String, ColorSchemeAttribute>> workingOverrides =
                (inputs.containsKey("colorSchemeOverrides"))
                        ? (Map<String, Map<String, ColorSchemeAttribute>>) inputs.get("colorSchemeOverrides")
                        : new LinkedHashMap<>();

        if (settings.getColorSchemeOverrides() != null && !inputs.containsKey("colorSchemeOverrides")) {
            for (Map.Entry<String, Map<String, ColorSchemeAttribute>> entry : settings.getColorSchemeOverrides().entrySet()) {
                Map<String, ColorSchemeAttribute> inner = new LinkedHashMap<>();
                for (Map.Entry<String, ColorSchemeAttribute> attrEntry : entry.getValue().entrySet()) {
                    inner.put(attrEntry.getKey(), attrEntry.getValue().copy());
                }
                workingOverrides.put(entry.getKey(), inner);
            }
        }

        inputs.put("customColorSchemes", workingCustomSchemes);
        inputs.put("colorSchemeOverrides", workingOverrides);

        // 1. Top Section: Scheme Selector + Gear ⚙ Menu + Change IDE Theme...
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
                schemeCombo.getSelectionModel().select(current);
            } else if (schemeCombo.getItems().contains(settings.getEditorColorScheme())) {
                schemeCombo.getSelectionModel().select(settings.getEditorColorScheme());
            } else if (!schemeCombo.getItems().isEmpty()) {
                schemeCombo.getSelectionModel().select(0);
            }
        };
        refreshSchemesList.run();
        inputs.put("editorColorSchemeCombo", schemeCombo);

        Button gearBtn = new Button();
        gearBtn.setGraphic(Icons.of(FontAwesomeSolid.COG, "#a9b7c6", 13));
        gearBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 6;");

        ContextMenu gearMenu = new ContextMenu();
        MenuItem miDuplicate = new MenuItem("Duplicate…");
        MenuItem miRestore = new MenuItem("Restore Defaults");
        MenuItem miExport = new MenuItem("Export…");
        MenuItem miRename = new MenuItem("Rename…");
        MenuItem miDelete = new MenuItem("Delete");
        gearMenu.getItems().addAll(miDuplicate, miRestore, miExport, miRename, miDelete);

        gearBtn.setOnAction(e -> {
            String active = schemeCombo.getValue();
            boolean isCustom = workingCustomSchemes.contains(active);
            miRename.setDisable(!isCustom);
            miDelete.setDisable(!isCustom);
            gearMenu.show(gearBtn, Side.BOTTOM, 0, 0);
        });

        Hyperlink changeThemeLink = new Hyperlink("Change IDE Theme...");
        changeThemeLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 13px; -fx-padding: 2 0; -fx-border-color: transparent; -fx-underline: false;");
        changeThemeLink.setOnMouseEntered(ev -> changeThemeLink.setStyle("-fx-text-fill: #70aeff; -fx-font-size: 13px; -fx-padding: 2 0; -fx-border-color: transparent; -fx-underline: true;"));
        changeThemeLink.setOnMouseExited(ev -> changeThemeLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 13px; -fx-padding: 2 0; -fx-border-color: transparent; -fx-underline: false;"));
        changeThemeLink.setOnAction(e -> {
            if (navigateTo != null) {
                navigateTo.accept("Appearance & Behavior / Appearance");
            }
        });

        Label helpIcon = new Label(" (?)");
        helpIcon.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px; -fx-cursor: hand;");
        helpIcon.setTooltip(new Tooltip("Configure ANSI and console output colors"));

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox topBar = new HBox(10, schemeLabel, schemeCombo, gearBtn, changeThemeLink, topSpacer, helpIcon);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // 2. TreeView of Console Colors Elements (Left Pane)
        TreeItem<ColorSchemeElement> rootItem = new TreeItem<>(new ColorSchemeElement("root", "Root", List.of("Root"), null, null, null, "Console Colors"));
        rootItem.setExpanded(true);

        populateConsoleColorTree(rootItem);

        TreeView<ColorSchemeElement> treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(false);
        treeView.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        treeView.setPrefWidth(360);
        treeView.setPrefHeight(260);

        treeView.setCellFactory(tv -> new TreeCell<>() {
            {
                selectedProperty().addListener((obs, wasSel, isSel) -> updateCellStyle());
                hoverProperty().addListener((obs, wasHov, isHov) -> updateCellStyle());
            }

            private void updateCellStyle() {
                if (isEmpty() || getItem() == null) {
                    setStyle("-fx-background-color: transparent;");
                    return;
                }
                if (isSelected()) {
                    setStyle("-fx-background-color: #2e436e; -fx-background-radius: 3; -fx-text-fill: #ffffff;");
                } else if (isHover()) {
                    setStyle("-fx-background-color: #2a2b2d; -fx-background-radius: 3; -fx-text-fill: #dfe1e5;");
                } else {
                    setStyle("-fx-background-color: transparent; -fx-text-fill: #dfe1e5;");
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
        attrEditor.setPadding(new Insets(10, 14, 10, 14));
        attrEditor.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        attrEditor.setPrefWidth(360);
        attrEditor.setPrefHeight(260);

        Label selectedElementNameLabel = new Label("Element Name");
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

        // Attribute rows helper
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
        effectTypeCombo.getSelectionModel().select("Underscored");
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

        // Inheritance row
        CheckBox inheritCheck = new CheckBox("Inherit values from:");
        inheritCheck.setStyle("-fx-text-fill: -text;");

        Hyperlink inheritLink = new Hyperlink();
        inheritLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 12px; -fx-padding: 0; -fx-border-color: transparent;");

        VBox inheritBox = new VBox(4, inheritCheck, inheritLink);
        inheritBox.setPadding(new Insets(6, 0, 0, 0));

        inheritCheck.selectedProperty().addListener((obs, oldV, newV) -> {
            boolean inh = Boolean.TRUE.equals(newV);
            boldCheck.setDisable(inh);
            italicCheck.setDisable(inh);
            fgRow.check.setDisable(inh);
            fgRow.colorBtn.setDisable(inh || !fgRow.check.isSelected());
            bgRow.check.setDisable(inh);
            bgRow.colorBtn.setDisable(inh || !bgRow.check.isSelected());
            errorStripeRow.check.setDisable(inh);
            errorStripeRow.colorBtn.setDisable(inh || !errorStripeRow.check.isSelected());
            effectsRow.check.setDisable(inh);
            effectsRow.colorBtn.setDisable(inh || !effectsRow.check.isSelected());
            effectTypeCombo.setDisable(inh || !effectsRow.check.isSelected());
        });

        attrEditor.getChildren().addAll(selectedElementNameLabel, fontStyleRow, fgRow.row, bgRow.row, errorStripeRow.row, effectsContainer, inheritBox);

        final String[] currentSelectedElementId = new String[]{"console.standard_output"};

        // 4. Live CLI Output Preview (Bottom Pane)
        VBox previewPane = new VBox(4);
        previewPane.setPadding(new Insets(10, 12, 10, 12));
        previewPane.setStyle("-fx-background-color: #1e1f22;");

        ScrollPane previewScroll = new ScrollPane(previewPane);
        previewScroll.setFitToWidth(true);
        previewScroll.setStyle("-fx-background: #1e1f22; -fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        previewScroll.setPrefHeight(230);
        VBox.setVgrow(previewScroll, Priority.ALWAYS);

        // Preview refresher
        Runnable refreshPreview = () -> {
            previewPane.getChildren().clear();
            String activeScheme = schemeCombo.getValue();

            ColorSchemeAttribute bgAttr = ColorSchemeModel.resolveAttribute(activeScheme, "console.background", workingOverrides);
            String consoleBg = (bgAttr.backgroundEnabled && bgAttr.background != null) ? "#" + bgAttr.background : "#1e1f22";
            previewPane.setStyle("-fx-background-color: " + consoleBg + ";");

            ColorSchemeAttribute stdoutAttr = ColorSchemeModel.resolveAttribute(activeScheme, "console.standard_output", workingOverrides);
            ColorSchemeAttribute stderrAttr = ColorSchemeModel.resolveAttribute(activeScheme, "console.error_output", workingOverrides);
            ColorSchemeAttribute sysoutAttr = ColorSchemeModel.resolveAttribute(activeScheme, "console.system_output", workingOverrides);
            ColorSchemeAttribute stdinAttr = ColorSchemeModel.resolveAttribute(activeScheme, "console.user_input", workingOverrides);

            ColorSchemeAttribute logErrAttr = ColorSchemeModel.resolveAttribute(activeScheme, "console.log.error", workingOverrides);
            ColorSchemeAttribute logWarnAttr = ColorSchemeModel.resolveAttribute(activeScheme, "console.log.warning", workingOverrides);
            ColorSchemeAttribute logInfoAttr = ColorSchemeModel.resolveAttribute(activeScheme, "console.log.info", workingOverrides);
            ColorSchemeAttribute logDbgAttr = ColorSchemeModel.resolveAttribute(activeScheme, "console.log.debug", workingOverrides);
            ColorSchemeAttribute logVerbAttr = ColorSchemeModel.resolveAttribute(activeScheme, "console.log.verbose", workingOverrides);

            // Line 1: C:\command.com
            Label l1 = makeClickableLabel("C:\\command.com", sysoutAttr, "console.system_output", treeView, rootItem, currentSelectedElementId);
            previewPane.getChildren().add(l1);

            // Line 2: - C:>
            Label l2 = makeClickableLabel("- C:>", sysoutAttr, "console.system_output", treeView, rootItem, currentSelectedElementId);
            previewPane.getChildren().add(l2);

            // Line 3: - help
            Label l3 = makeClickableLabel("- help", stdinAttr, "console.user_input", treeView, rootItem, currentSelectedElementId);
            previewPane.getChildren().add(l3);

            // Line 4: Bad command or file name
            Label l4 = makeClickableLabel("Bad command or file name", stderrAttr, "console.error_output", treeView, rootItem, currentSelectedElementId);
            previewPane.getChildren().add(l4);

            // Line 5: Log error
            HBox l5 = new HBox(4,
                    makeClickableLabel("2026-03-26 12:00:00 ", stdoutAttr, "console.standard_output", treeView, rootItem, currentSelectedElementId),
                    makeClickableLabel("[ERROR]", logErrAttr, "console.log.error", treeView, rootItem, currentSelectedElementId),
                    makeClickableLabel(" Failed to establish database connection to localhost:5432", logErrAttr, "console.log.error", treeView, rootItem, currentSelectedElementId)
            );
            previewPane.getChildren().add(l5);

            // Line 6: Log warning
            HBox l6 = new HBox(4,
                    makeClickableLabel("2026-03-26 12:00:01 ", stdoutAttr, "console.standard_output", treeView, rootItem, currentSelectedElementId),
                    makeClickableLabel("[WARN]", logWarnAttr, "console.log.warning", treeView, rootItem, currentSelectedElementId),
                    makeClickableLabel(" Connection pool exhausted, retrying in 2000ms...", stdoutAttr, "console.standard_output", treeView, rootItem, currentSelectedElementId)
            );
            previewPane.getChildren().add(l6);

            // Line 7: Log info
            HBox l7 = new HBox(4,
                    makeClickableLabel("2026-03-26 12:00:02 ", stdoutAttr, "console.standard_output", treeView, rootItem, currentSelectedElementId),
                    makeClickableLabel("[INFO]", logInfoAttr, "console.log.info", treeView, rootItem, currentSelectedElementId),
                    makeClickableLabel(" Connection established successfully (pool size: 10)", stdoutAttr, "console.standard_output", treeView, rootItem, currentSelectedElementId)
            );
            previewPane.getChildren().add(l7);

            // Line 8: Log debug
            HBox l8 = new HBox(4,
                    makeClickableLabel("2026-03-26 12:00:03 ", stdoutAttr, "console.standard_output", treeView, rootItem, currentSelectedElementId),
                    makeClickableLabel("[DEBUG]", logDbgAttr, "console.log.debug", treeView, rootItem, currentSelectedElementId),
                    makeClickableLabel(" Executing SQL: SELECT * FROM customers WHERE active = true", stdoutAttr, "console.standard_output", treeView, rootItem, currentSelectedElementId)
            );
            previewPane.getChildren().add(l8);

            // Line 9: Log verbose
            HBox l9 = new HBox(4,
                    makeClickableLabel("2026-03-26 12:00:04 ", stdoutAttr, "console.standard_output", treeView, rootItem, currentSelectedElementId),
                    makeClickableLabel("[VERBOSE]", logVerbAttr, "console.log.verbose", treeView, rootItem, currentSelectedElementId),
                    makeClickableLabel(" Fetched 142 rows in 12ms", stdoutAttr, "console.standard_output", treeView, rootItem, currentSelectedElementId)
            );
            previewPane.getChildren().add(l9);

            // Line 10: ANSI 16 Colors Badges
            Label ansi16Label = new Label("ANSI colors: ");
            ansi16Label.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #868a91;");
            HBox ansi16Row = new HBox(6, ansi16Label);
            ansi16Row.setAlignment(Pos.CENTER_LEFT);

            String[] standardAnsi = {"black", "red", "green", "yellow", "blue", "magenta", "cyan", "white"};
            for (String c : standardAnsi) {
                String id = "console.ansi." + c;
                ColorSchemeAttribute a = ColorSchemeModel.resolveAttribute(activeScheme, id, workingOverrides);
                ansi16Row.getChildren().add(makeAnsiBadge(c, a, id, treeView, rootItem, currentSelectedElementId));
            }
            previewPane.getChildren().add(ansi16Row);

            // Line 11: Bright ANSI Colors Badges
            Label brightLabel = new Label("Bright ANSI: ");
            brightLabel.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-text-fill: #868a91;");
            HBox brightRow = new HBox(6, brightLabel);
            brightRow.setAlignment(Pos.CENTER_LEFT);

            String[] brightAnsi = {"bright_black", "bright_red", "bright_green", "bright_yellow", "bright_blue", "bright_magenta", "bright_cyan", "bright_white"};
            for (String c : brightAnsi) {
                String id = "console.ansi." + c;
                ColorSchemeAttribute a = ColorSchemeModel.resolveAttribute(activeScheme, id, workingOverrides);
                brightRow.getChildren().add(makeAnsiBadge(c.replace("bright_", "B-"), a, id, treeView, rootItem, currentSelectedElementId));
            }
            previewPane.getChildren().add(brightRow);

            // Line 12: Reworked terminal blocks preview
            ColorSchemeAttribute cmdAttr = ColorSchemeModel.resolveAttribute(activeScheme, "console.reworked_terminal.command", workingOverrides);
            ColorSchemeAttribute promptSepAttr = ColorSchemeModel.resolveAttribute(activeScheme, "console.reworked_terminal.prompt_separator_color", workingOverrides);
            ColorSchemeAttribute promptTextAttr = ColorSchemeModel.resolveAttribute(activeScheme, "console.reworked_terminal.generate_command_prompt_text", workingOverrides);
            ColorSchemeAttribute selBlockAttr = ColorSchemeModel.resolveAttribute(activeScheme, "console.reworked_terminal.selected_block_background", workingOverrides);

            Label sepLabel = makeClickableLabel("❯ ", promptSepAttr, "console.reworked_terminal.prompt_separator_color", treeView, rootItem, currentSelectedElementId);
            Label cmdLabel = makeClickableLabel("mvn clean compile", cmdAttr, "console.reworked_terminal.command", treeView, rootItem, currentSelectedElementId);
            Label genPromptLabel = makeClickableLabel("  # Ask AI: build project", promptTextAttr, "console.reworked_terminal.generate_command_prompt_text", treeView, rootItem, currentSelectedElementId);

            HBox reworkedBlockRow = new HBox(4, sepLabel, cmdLabel, genPromptLabel);
            reworkedBlockRow.setAlignment(Pos.CENTER_LEFT);
            if (selBlockAttr.backgroundEnabled && selBlockAttr.background != null) {
                reworkedBlockRow.setStyle("-fx-background-color: #" + selBlockAttr.background + "22; -fx-padding: 2 6; -fx-background-radius: 3;");
            }
            previewPane.getChildren().add(reworkedBlockRow);

            // Line 13: Terminal: Command to run using IDE (git log, matching Image 1)
            ColorSchemeAttribute cmdToRunAttr = ColorSchemeModel.resolveAttribute(activeScheme, "console.terminal.command_to_run_using_ide", workingOverrides);
            Label lGitLog = makeClickableLabel("git log", cmdToRunAttr, "console.terminal.command_to_run_using_ide", treeView, rootItem, currentSelectedElementId);
            previewPane.getChildren().add(lGitLog);

            // Line 14: Process exit
            Label l14 = makeClickableLabel("Process finished with exit code 1", stderrAttr, "console.error_output", treeView, rootItem, currentSelectedElementId);
            previewPane.getChildren().add(l14);
        };

        // Wire committing changes back to workingOverrides
        commitAttrChangesRef[0] = () -> {
            TreeItem<ColorSchemeElement> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected == null || selected.getValue() == null) return;
            ColorSchemeElement el = selected.getValue();
            if (el.getCategoryPath().size() < 2) return; // ignore root/category nodes

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
            refreshPreview.run();
        };

        boldCheck.setOnAction(e -> commitAttrChangesRef[0].run());
        italicCheck.setOnAction(e -> commitAttrChangesRef[0].run());
        fgRow.check.setOnAction(e -> commitAttrChangesRef[0].run());
        bgRow.check.setOnAction(e -> commitAttrChangesRef[0].run());
        errorStripeRow.check.setOnAction(e -> commitAttrChangesRef[0].run());
        effectsRow.check.setOnAction(e -> commitAttrChangesRef[0].run());
        effectTypeCombo.setOnAction(e -> commitAttrChangesRef[0].run());
        inheritCheck.setOnAction(e -> commitAttrChangesRef[0].run());

        // Tree selection listener -> loads values into Right Pane
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.getValue() == null) return;
            ColorSchemeElement el = newVal.getValue();
            if (el.getCategoryPath().size() < 2) {
                // Category node
                selectedElementNameLabel.setText(el.getName());
                attrEditor.setDisable(true);
                return;
            }
            attrEditor.setDisable(false);
            selectedElementNameLabel.setText(el.getName());
            currentSelectedElementId[0] = el.getId();

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
                inheritLink.setOnAction(ev -> selectTreeItemById(treeView, rootItem, el.getInheritFromKey()));
            }
        });

        inheritLink.setOnAction(e -> {
            TreeItem<ColorSchemeElement> sel = treeView.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getValue() != null && sel.getValue().hasInheritance()) {
                selectTreeItemById(treeView, rootItem, sel.getValue().getInheritFromKey());
            }
        });

        schemeCombo.setOnAction(e -> {
            refreshPreview.run();
            TreeItem<ColorSchemeElement> cur = treeView.getSelectionModel().getSelectedItem();
            if (cur != null) {
                treeView.getSelectionModel().clearSelection();
                treeView.getSelectionModel().select(cur);
            }
        });

        // Pre-select first leaf element (ANSI colors > Black)
        if (!rootItem.getChildren().isEmpty() && !rootItem.getChildren().get(0).getChildren().isEmpty()) {
            treeView.getSelectionModel().select(rootItem.getChildren().get(0).getChildren().get(0));
        }

        refreshPreview.run();

        HBox splitTop = new HBox(10, treeView, attrEditor);
        HBox.setHgrow(treeView, Priority.ALWAYS);
        HBox.setHgrow(attrEditor, Priority.ALWAYS);

        Label previewHeader = new Label("Preview:");
        previewHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #868a91; -fx-font-size: 11px;");

        root.getChildren().addAll(topBar, new Separator(), splitTop, previewHeader, previewScroll);
        return root;
    }

    private static void populateConsoleColorTree(TreeItem<ColorSchemeElement> rootItem) {
        List<ColorSchemeElement> elements = ColorSchemeModel.getConsoleColorElements();
        Map<String, TreeItem<ColorSchemeElement>> categoryItems = new LinkedHashMap<>();

        for (ColorSchemeElement el : elements) {
            String cat = el.getCategory();
            TreeItem<ColorSchemeElement> catItem = categoryItems.computeIfAbsent(cat, k -> {
                TreeItem<ColorSchemeElement> ti = new TreeItem<>(new ColorSchemeElement("cat." + k, k, List.of(k), null, null, null, "Console Colors"));
                ti.setExpanded(true);
                rootItem.getChildren().add(ti);
                return ti;
            });
            TreeItem<ColorSchemeElement> leaf = new TreeItem<>(el);
            catItem.getChildren().add(leaf);
        }
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

    private static Label makeClickableLabel(String text,
                                            ColorSchemeAttribute attr,
                                            String elementId,
                                            TreeView<ColorSchemeElement> treeView,
                                            TreeItem<ColorSchemeElement> rootItem,
                                            String[] currentSelectedRef) {
        Label label = new Label(text);
        StringBuilder style = new StringBuilder("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-cursor: hand;");
        if (attr != null) {
            if (attr.foregroundEnabled && attr.foreground != null && !attr.foreground.isBlank()) {
                style.append(" -fx-text-fill: #").append(attr.foreground).append(";");
            } else {
                style.append(" -fx-text-fill: #bcbec4;");
            }
            if (attr.backgroundEnabled && attr.background != null && !attr.background.isBlank()) {
                style.append(" -fx-background-color: #").append(attr.background).append("; -fx-padding: 0 4; -fx-background-radius: 2;");
            }
            if (attr.bold) {
                style.append(" -fx-font-weight: bold;");
            }
            if (attr.italic) {
                style.append(" -fx-font-style: italic;");
            }
            if (attr.effectEnabled) {
                String effColor = (attr.effectColor != null && !attr.effectColor.isBlank()) ? "#" + attr.effectColor : "#589df6";
                if ("Underscored".equals(attr.effectType) || "Bold Underscored".equals(attr.effectType)) {
                    int w = "Bold Underscored".equals(attr.effectType) ? 2 : 1;
                    style.append(" -fx-border-color: transparent transparent ").append(effColor).append(" transparent; -fx-border-width: 0 0 ").append(w).append(" 0;");
                } else if ("Bordered".equals(attr.effectType)) {
                    style.append(" -fx-border-color: ").append(effColor).append("; -fx-border-width: 1; -fx-border-radius: 2;");
                }
            }
        } else {
            style.append(" -fx-text-fill: #bcbec4;");
        }
        label.setStyle(style.toString());
        label.setOnMouseClicked(e -> {
            selectTreeItemById(treeView, rootItem, elementId);
            currentSelectedRef[0] = elementId;
        });
        return label;
    }

    private static Label makeAnsiBadge(String name,
                                       ColorSchemeAttribute attr,
                                       String elementId,
                                       TreeView<ColorSchemeElement> treeView,
                                       TreeItem<ColorSchemeElement> rootItem,
                                       String[] currentSelectedRef) {
        Label badge = new Label(" " + name + " ");
        String fg = (attr != null && attr.foregroundEnabled && attr.foreground != null) ? "#" + attr.foreground : "#bcbec4";
        String bg = (attr != null && attr.backgroundEnabled && attr.background != null) ? "#" + attr.background : "#2b2d30";
        badge.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px; -fx-text-fill: " + fg +
                "; -fx-background-color: " + bg + "; -fx-border-color: #393b40; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;");
        badge.setOnMouseClicked(e -> {
            selectTreeItemById(treeView, rootItem, elementId);
            currentSelectedRef[0] = elementId;
        });
        return badge;
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

    private static void showColorPickerDialog(String currentHex, Node anchor, java.util.function.Consumer<String> onSelected) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Choose Color");
        dialog.initModality(Modality.APPLICATION_MODAL);

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text;");
        pane.setPrefWidth(300);

        Label prompt = new Label("Enter Hex Color (e.g. 4B5059 or #4B5059):");
        prompt.setStyle("-fx-text-fill: -text; -fx-font-size: 12px;");

        TextField hexInput = new TextField(currentHex != null ? currentHex : "DFE1E5");
        hexInput.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: #dfe1e5; -fx-border-color: #3574f0; -fx-font-weight: bold;");

        Region colorPreview = new Region();
        colorPreview.setPrefWidth(36);
        colorPreview.setPrefHeight(26);
        colorPreview.setStyle("-fx-background-color: #" + hexInput.getText().replace("#", "") + "; -fx-border-color: #393b40; -fx-border-radius: 3; -fx-background-radius: 3;");

        hexInput.textProperty().addListener((obs, oldV, newV) -> {
            String clean = newV != null ? newV.replace("#", "").trim() : "";
            if (clean.matches("[0-9a-fA-F]{6}")) {
                colorPreview.setStyle("-fx-background-color: #" + clean + "; -fx-border-color: #393b40; -fx-border-radius: 3; -fx-background-radius: 3;");
            }
        });

        HBox inputRow = new HBox(8, hexInput, colorPreview);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        Label paletteLabel = new Label("Standard Palette:");
        paletteLabel.setStyle("-fx-text-fill: #868a91; -fx-font-size: 11px;");

        FlowPane palette = new FlowPane(6, 6);
        List<String> swatches = List.of(
                "000000", "BC3F3C", "57965C", "C19C00", "3574F0", "AE67A0", "2BBAC5", "A9B7C6",
                "6C707E", "F75464", "4FC414", "E5B842", "589DF6", "C77DBB", "46B8DF", "FFFFFF"
        );
        for (String sw : swatches) {
            Button swBtn = new Button();
            swBtn.setPrefSize(20, 20);
            swBtn.setStyle("-fx-background-color: #" + sw + "; -fx-border-color: #393b40; -fx-border-radius: 2; -fx-background-radius: 2; -fx-cursor: hand;");
            swBtn.setOnAction(ev -> {
                hexInput.setText(sw);
                colorPreview.setStyle("-fx-background-color: #" + sw + "; -fx-border-color: #393b40; -fx-border-radius: 3; -fx-background-radius: 3;");
            });
            palette.getChildren().add(swBtn);
        }

        VBox content = new VBox(10, prompt, inputRow, paletteLabel, palette);
        content.setPadding(new Insets(14));
        pane.setContent(content);

        ButtonType btnOk = new ButtonType("Choose", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(btnCancel, btnOk);

        dialog.setResultConverter(bt -> {
            if (bt == btnOk) {
                return hexInput.getText() != null ? hexInput.getText().replace("#", "").trim().toUpperCase() : "";
            }
            return null;
        });

        dialog.showAndWait().ifPresent(hex -> {
            if (hex != null && !hex.isBlank() && onSelected != null) {
                onSelected.accept(hex);
            }
        });
    }
}
