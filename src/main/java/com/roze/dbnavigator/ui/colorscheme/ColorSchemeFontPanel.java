package com.roze.dbnavigator.ui.colorscheme;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.ui.Icons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.*;

/**
 * Dedicated DataGrip-identical panel for Editor > Color Scheme > Color Scheme Font.
 * Allows customizing primary and fallback fonts, size, line height, and ligatures
 * specifically for the active color scheme, with live preview and interactive text entry.
 */
public final class ColorSchemeFontPanel {

    private static final List<String> COMMON_MONOSPACED = List.of(
            "JetBrains Mono", "Fira Code", "Source Code Pro", "Consolas",
            "Menlo", "Monaco", "Courier New", "DejaVu Sans Mono",
            "Inconsolata", "Cascadia Code", "Ubuntu Mono", "Liberation Mono"
    );

    public static VBox build(AppSettingsStore.Settings settings,
                             Map<String, Object> inputs,
                             java.util.function.Consumer<String> navigateTo) {
        VBox root = new VBox(14);
        root.setPadding(new Insets(10, 16, 16, 16));

        // 1. Top Bar: Scheme Selector + Gear ⚙ Menu + Change IDE Theme...
        Label schemeLabel = new Label("Scheme:");
        schemeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");

        ComboBox<String> schemeCombo = new ComboBox<>();
        schemeCombo.setPrefWidth(210);
        schemeCombo.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");

        List<String> customSchemes = (inputs.containsKey("customColorSchemes"))
                ? (List<String>) inputs.get("customColorSchemes")
                : new ArrayList<>(settings.getCustomColorSchemes());
        inputs.put("customColorSchemes", customSchemes);

        Runnable refreshSchemesList = () -> {
            String current = schemeCombo.getValue();
            schemeCombo.getItems().clear();
            schemeCombo.getItems().addAll(AppSettingsStore.defaultEditorColorSchemes());
            for (String custom : customSchemes) {
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
            boolean isCustom = customSchemes.contains(active);
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
        helpIcon.setTooltip(new Tooltip("Configure color scheme font settings"));

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox topBar = new HBox(10, schemeLabel, schemeCombo, gearBtn, changeThemeLink, topSpacer, helpIcon);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // 2. Master Toggle: Use color scheme font instead of the default (JetBrains Mono,13)
        CheckBox masterToggle = new CheckBox("Use color scheme font instead of the default ("
                + settings.getEditorFontFamily() + "," + (int) settings.getEditorFontSize() + ")");
        masterToggle.setSelected(settings.isUseColorSchemeFontInsteadOfDefault());
        masterToggle.setStyle("-fx-font-weight: bold; -fx-text-fill: -text;");
        inputs.put("csFont_useInsteadOfDefault", masterToggle);

        // 3. Typography Controls Grid
        VBox controlsBox = new VBox(10);
        controlsBox.setPadding(new Insets(4, 0, 6, 0));

        // Row 1: Font & Fallback font
        Label fontLabel = new Label("Font:");
        fontLabel.setPrefWidth(90);
        fontLabel.setStyle("-fx-text-fill: -text;");

        ComboBox<String> fontCombo = new ComboBox<>();
        fontCombo.setPrefWidth(210);
        fontCombo.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        inputs.put("csFont_fontFamily", fontCombo);

        Label fallbackLabel = new Label("Fallback font:");
        fallbackLabel.setPrefWidth(90);
        fallbackLabel.setStyle("-fx-text-fill: -text;");

        ComboBox<String> fallbackCombo = new ComboBox<>();
        fallbackCombo.setPrefWidth(160);
        fallbackCombo.getItems().addAll("<None>", "JetBrains Mono", "Courier New", "Menlo", "Consolas");
        String currentFallback = settings.getColorSchemeFontFallbackFamily();
        fallbackCombo.setValue(currentFallback != null ? currentFallback : "<None>");
        fallbackCombo.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        inputs.put("csFont_fallbackFont", fallbackCombo);

        HBox fontRow = new HBox(12, fontLabel, fontCombo, fallbackLabel, fallbackCombo);
        fontRow.setAlignment(Pos.CENTER_LEFT);

        // Row 2: Show only monospaced fonts CheckBox
        CheckBox monospacedCheck = new CheckBox("Show only monospaced fonts");
        monospacedCheck.setSelected(settings.isColorSchemeFontShowOnlyMonospaced());
        monospacedCheck.setStyle("-fx-text-fill: -text;");
        inputs.put("csFont_showOnlyMonospaced", monospacedCheck);

        Runnable populateFonts = () -> {
            String selected = fontCombo.getValue();
            fontCombo.getItems().clear();
            List<String> systemFamilies = Font.getFamilies();
            boolean onlyMono = monospacedCheck.isSelected();

            List<String> items = new ArrayList<>();
            for (String f : COMMON_MONOSPACED) {
                if (systemFamilies.contains(f) && !items.contains(f)) {
                    items.add(f);
                }
            }
            if (!onlyMono) {
                for (String f : systemFamilies) {
                    if (!items.contains(f)) {
                        items.add(f);
                    }
                }
            } else {
                for (String f : systemFamilies) {
                    String lower = f.toLowerCase(Locale.ROOT);
                    if ((lower.contains("mono") || lower.contains("code") || lower.contains("console") || lower.contains("courier")) && !items.contains(f)) {
                        items.add(f);
                    }
                }
            }
            if (items.isEmpty()) {
                items.addAll(List.of("JetBrains Mono", "Menlo", "Consolas", "Courier New"));
            }
            fontCombo.getItems().addAll(items);
            if (selected != null && fontCombo.getItems().contains(selected)) {
                fontCombo.setValue(selected);
            } else if (fontCombo.getItems().contains(settings.getColorSchemeFontFamily())) {
                fontCombo.setValue(settings.getColorSchemeFontFamily());
            } else {
                fontCombo.setValue(fontCombo.getItems().get(0));
            }
        };
        populateFonts.run();
        monospacedCheck.setOnAction(e -> populateFonts.run());

        // Row 3: Size & Line height
        Label sizeLabel = new Label("Size:");
        sizeLabel.setPrefWidth(90);
        sizeLabel.setStyle("-fx-text-fill: -text;");

        Spinner<Double> sizeSpinner = new Spinner<>(8.0, 36.0, settings.getColorSchemeFontSize(), 1.0);
        sizeSpinner.setEditable(true);
        sizeSpinner.setPrefWidth(90);
        sizeSpinner.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        inputs.put("csFont_fontSize", sizeSpinner);

        Label lineHeightLabel = new Label("Line height:");
        lineHeightLabel.setStyle("-fx-text-fill: -text;");

        Spinner<Double> lineHeightSpinner = new Spinner<>(0.6, 3.0, settings.getColorSchemeFontLineHeight(), 0.1);
        lineHeightSpinner.setEditable(true);
        lineHeightSpinner.setPrefWidth(90);
        lineHeightSpinner.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        inputs.put("csFont_lineHeight", lineHeightSpinner);

        HBox metricRow = new HBox(12, sizeLabel, sizeSpinner, lineHeightLabel, lineHeightSpinner);
        metricRow.setAlignment(Pos.CENTER_LEFT);

        // Row 4: Ligatures & Reader mode link
        CheckBox ligaturesCheck = new CheckBox("Enable ligatures");
        ligaturesCheck.setSelected(settings.isColorSchemeFontEnableLigatures());
        ligaturesCheck.setStyle("-fx-text-fill: -text;");
        inputs.put("csFont_enableLigatures", ligaturesCheck);

        Label ligaturesHelp = new Label(" (?)");
        ligaturesHelp.setStyle("-fx-text-fill: #868a91; -fx-font-size: 11px; -fx-cursor: hand;");
        ligaturesHelp.setTooltip(new Tooltip("Enables typographic ligatures like !=, <=, >=, -> in supported fonts"));

        Hyperlink readerModeLink = new Hyperlink("See line height and ligatures also in Reader mode");
        readerModeLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 12px; -fx-padding: 0; -fx-border-color: transparent;");
        readerModeLink.setOnAction(e -> {
            if (navigateTo != null) {
                navigateTo.accept("Editor / Reader Mode");
            }
        });

        HBox ligaturesRow = new HBox(8, ligaturesCheck, ligaturesHelp, new Region(), readerModeLink);
        ligaturesRow.setAlignment(Pos.CENTER_LEFT);

        controlsBox.getChildren().addAll(fontRow, monospacedCheck, metricRow, ligaturesRow);

        // Enable / disable controls with master toggle
        Runnable updateControlsState = () -> {
            boolean active = masterToggle.isSelected();
            controlsBox.setDisable(!active);
            controlsBox.setOpacity(active ? 1.0 : 0.55);
        };
        updateControlsState.run();
        masterToggle.setOnAction(e -> updateControlsState.run());

        // 4. Live Preview Area
        Label previewTitle = new Label("Preview:");
        previewTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #868a91; -fx-font-size: 11px;");

        VBox previewCard = new VBox(6);
        previewCard.setPadding(new Insets(12));
        previewCard.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(previewCard, Priority.ALWAYS);

        TextFlow codeFlow = new TextFlow();
        TextField customPreviewField = new TextField();
        customPreviewField.setPromptText("Enter any text to preview");
        customPreviewField.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: #dfe1e5; -fx-border-color: #393b40; -fx-border-radius: 3; -fx-background-radius: 3; -fx-prompt-text-fill: #868a91;");

        Runnable updatePreview = () -> {
            codeFlow.getChildren().clear();
            String family = fontCombo.getValue() != null ? fontCombo.getValue() : "JetBrains Mono";
            double size = sizeSpinner.getValue() != null ? sizeSpinner.getValue() : 13.0;
            double lh = lineHeightSpinner.getValue() != null ? lineHeightSpinner.getValue() : 1.2;
            codeFlow.setLineSpacing(size * (lh - 1.0));

            Font regularFont = Font.font(family, FontWeight.NORMAL, FontPosture.REGULAR, size);
            Font boldFont = Font.font(family, FontWeight.BOLD, FontPosture.REGULAR, size);
            Font italicFont = Font.font(family, FontWeight.NORMAL, FontPosture.ITALIC, size);

            // Sample code
            addToken(codeFlow, "1  ", Color.web("#4b5059"), regularFont);
            addToken(codeFlow, "public class ", Color.web("#cf8e6d"), boldFont);
            addToken(codeFlow, "HelloWorld {\n", Color.web("#bcbec4"), regularFont);

            addToken(codeFlow, "2      ", Color.web("#4b5059"), regularFont);
            addToken(codeFlow, "public static void ", Color.web("#cf8e6d"), boldFont);
            addToken(codeFlow, "main", Color.web("#56a8f5"), regularFont);
            addToken(codeFlow, "(String[] args) {\n", Color.web("#bcbec4"), regularFont);

            addToken(codeFlow, "3          ", Color.web("#4b5059"), regularFont);
            addToken(codeFlow, "// The quick brown fox jumps over the lazy dog\n", Color.web("#7a7e85"), italicFont);

            addToken(codeFlow, "4          ", Color.web("#4b5059"), regularFont);
            addToken(codeFlow, "System.out.println(", Color.web("#bcbec4"), regularFont);
            addToken(codeFlow, "\"Hello, World! 0123456789\"", Color.web("#6aab73"), regularFont);
            addToken(codeFlow, ");\n", Color.web("#bcbec4"), regularFont);

            addToken(codeFlow, "5          ", Color.web("#4b5059"), regularFont);
            addToken(codeFlow, "int[] array = new int[]{ 1, 2, 3, 4, 5 };\n", Color.web("#bcbec4"), regularFont);

            addToken(codeFlow, "6          ", Color.web("#4b5059"), regularFont);
            addToken(codeFlow, "boolean ", Color.web("#cf8e6d"), regularFont);
            addToken(codeFlow, "ligatures = (x != null && y >= 10 || z <= 20);\n", Color.web("#bcbec4"), regularFont);

            addToken(codeFlow, "7      }\n", Color.web("#bcbec4"), regularFont);
            addToken(codeFlow, "8  }\n\n", Color.web("#bcbec4"), regularFont);

            // Character set display
            addToken(codeFlow, "ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz 0123456789 ()[]{}<>+-*/=;:.,!?&|\n", Color.web("#bcbec4"), regularFont);
            addToken(codeFlow, "ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz 0123456789 ()[]{}<>+-*/=;:.,!?&|\n", Color.web("#dfe1e5"), boldFont);

            // Dynamic user-entered preview text
            String userText = customPreviewField.getText();
            if (userText != null && !userText.isBlank()) {
                addToken(codeFlow, "\nCustom preview:\n", Color.web("#589df6"), boldFont);
                addToken(codeFlow, userText + "\n", Color.web("#ffffff"), regularFont);
            }
        };

        // Wire preview listeners
        fontCombo.valueProperty().addListener((obs, oldV, newV) -> updatePreview.run());
        sizeSpinner.valueProperty().addListener((obs, oldV, newV) -> updatePreview.run());
        lineHeightSpinner.valueProperty().addListener((obs, oldV, newV) -> updatePreview.run());
        customPreviewField.textProperty().addListener((obs, oldV, newV) -> updatePreview.run());
        updatePreview.run();

        // 5. Interactive Footer for Custom Text Preview
        Label footerLabel = new Label("Enter any text to preview:");
        footerLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 12px;");
        HBox.setHgrow(customPreviewField, Priority.ALWAYS);
        HBox footerBox = new HBox(10, footerLabel, customPreviewField);
        footerBox.setAlignment(Pos.CENTER_LEFT);
        footerBox.setPadding(new Insets(6, 0, 0, 0));

        ScrollPane previewScroll = new ScrollPane(codeFlow);
        previewScroll.setFitToWidth(true);
        previewScroll.setPrefHeight(230);
        previewScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        previewCard.getChildren().addAll(previewScroll, new Separator(), footerBox);

        root.getChildren().addAll(topBar, new Separator(), masterToggle, controlsBox, previewTitle, previewCard);
        return root;
    }

    private static void addToken(TextFlow flow, String text, Color color, Font font) {
        Text t = new Text(text);
        t.setFill(color);
        t.setFont(font);
        flow.getChildren().add(t);
    }
}
