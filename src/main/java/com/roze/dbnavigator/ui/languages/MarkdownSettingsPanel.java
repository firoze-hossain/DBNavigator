package com.roze.dbnavigator.ui.languages;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Settings panel for Languages > Markdown.
 * Matches DataGrip screenshot media_1791719403622_978c7901.png.
 */
public class MarkdownSettingsPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;
    private final Consumer<String> navigateTo;

    public MarkdownSettingsPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs, Consumer<String> navigateTo) {
        this.settings = settings;
        this.inputs = inputs;
        this.navigateTo = navigateTo;
    }

    public Node createPanel() {
        AppSettingsStore.MarkdownSettingsConfig cfg = settings.getMarkdown();

        VBox content = new VBox(14);
        content.setPadding(new Insets(10, 16, 20, 14));
        content.setStyle("-fx-background-color: transparent;");

        // Top form layout: 4 dropdown/spinner rows
        GridPane topGrid = new GridPane();
        topGrid.setHgap(16);
        topGrid.setVgap(10);
        topGrid.setAlignment(Pos.CENTER_LEFT);

        // 1. Preview rendering engine
        Label engineLabel = new Label("Preview rendering engine:");
        engineLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        ComboBox<String> engineCombo = new ComboBox<>();
        engineCombo.getItems().addAll("Chromium browser", "JavaFX WebView");
        engineCombo.setValue(cfg.getPreviewRenderingEngine());
        engineCombo.setPrefWidth(220);
        engineCombo.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        inputs.put("languages_markdown_previewRenderingEngine", engineCombo);

        topGrid.add(engineLabel, 0, 0);
        topGrid.add(engineCombo, 1, 0);

        // 2. Default layout
        Label defaultLayoutLabel = new Label("Default layout:");
        defaultLayoutLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        ComboBox<String> defaultLayoutCombo = new ComboBox<>();
        defaultLayoutCombo.getItems().addAll("Editor and Preview", "Preview only", "Editor only");
        defaultLayoutCombo.setValue(cfg.getDefaultLayout());
        defaultLayoutCombo.setPrefWidth(220);
        defaultLayoutCombo.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        inputs.put("languages_markdown_defaultLayout", defaultLayoutCombo);

        topGrid.add(defaultLayoutLabel, 0, 1);
        topGrid.add(defaultLayoutCombo, 1, 1);

        // 3. Preview layout
        Label previewLayoutLabel = new Label("Preview layout:");
        previewLayoutLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        ComboBox<String> previewLayoutCombo = new ComboBox<>();
        previewLayoutCombo.getItems().addAll("Split vertically", "Split horizontally");
        previewLayoutCombo.setValue(cfg.getPreviewLayout());
        previewLayoutCombo.setPrefWidth(220);
        previewLayoutCombo.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        inputs.put("languages_markdown_previewLayout", previewLayoutCombo);

        topGrid.add(previewLayoutLabel, 0, 2);
        topGrid.add(previewLayoutCombo, 1, 2);

        // 4. Preview font size
        Label fontSizeLabel = new Label("Preview font size:");
        fontSizeLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        Spinner<Integer> fontSizeSpinner = new Spinner<>(8, 72, cfg.getPreviewFontSize());
        fontSizeSpinner.setEditable(true);
        fontSizeSpinner.setPrefWidth(80);
        fontSizeSpinner.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        inputs.put("languages_markdown_previewFontSize", fontSizeSpinner);

        topGrid.add(fontSizeLabel, 0, 3);
        topGrid.add(fontSizeSpinner, 1, 3);

        // Checkboxes group
        CheckBox syncScrollCheck = new CheckBox("Sync scroll in the editor and preview");
        syncScrollCheck.setSelected(cfg.isSyncScroll());
        syncScrollCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("languages_markdown_syncScroll", syncScrollCheck);

        CheckBox injectLanguagesCheck = new CheckBox("Inject languages in code fences");
        injectLanguagesCheck.setSelected(cfg.isInjectLanguagesInCodeFences());
        injectLanguagesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("languages_markdown_injectLanguagesInCodeFences", injectLanguagesCheck);

        CheckBox showProblemsCheck = new CheckBox("Show problems in code fences");
        showProblemsCheck.setSelected(cfg.isShowProblemsInCodeFences());
        showProblemsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("languages_markdown_showProblemsInCodeFences", showProblemsCheck);

        CheckBox groupDocsCheck = new CheckBox("Group documents with the same name, but different extensions (.md, .html, .docx, .pdf)");
        groupDocsCheck.setSelected(cfg.isGroupDocumentsWithSameName());
        groupDocsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("languages_markdown_groupDocumentsWithSameName", groupDocsCheck);

        CheckBox detectCommandsCheck = new CheckBox("Detect commands that can be run right from Markdown files");
        detectCommandsCheck.setSelected(cfg.isDetectCommandsCanRun());
        detectCommandsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("languages_markdown_detectCommandsCanRun", detectCommandsCheck);

        VBox checksBox = new VBox(8, syncScrollCheck, injectLanguagesCheck, showProblemsCheck, groupDocsCheck, detectCommandsCheck);

        // Section: Markdown Extensions
        VBox extensionsSection = buildMarkdownExtensionsSection(cfg);

        // Section: Custom CSS
        VBox customCssSection = buildCustomCssSection(cfg);

        // Section: Pandoc Settings
        VBox pandocSection = buildPandocSection(cfg);

        // Footer: Link to Smart Keys
        Hyperlink smartKeysLink = new Hyperlink("Configure in-editor assistance in Smart Keys Settings");
        smartKeysLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 12px; -fx-padding: 6 0 0 0;");
        smartKeysLink.setOnAction(e -> {
            if (navigateTo != null) {
                navigateTo.accept("Editor / General / Smart Keys / Markdown");
            }
        });

        content.getChildren().addAll(
                topGrid,
                checksBox,
                extensionsSection,
                customCssSection,
                pandocSection,
                smartKeysLink
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        return scroll;
    }

    private VBox buildMarkdownExtensionsSection(AppSettingsStore.MarkdownSettingsConfig cfg) {
        Label title = new Label("Markdown Extensions");
        title.setStyle("-fx-text-fill: -text; -fx-font-weight: bold; -fx-font-size: 13px;");

        Separator sep = new Separator();
        HBox header = new HBox(10, title, sep);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(sep, Priority.ALWAYS);

        CheckBox plantUmlCheck = new CheckBox("PlantUML");
        plantUmlCheck.setSelected(cfg.isPlantUmlEnabled());
        plantUmlCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("languages_markdown_plantUmlEnabled", plantUmlCheck);

        Button helpBtn = new Button("?");
        helpBtn.setTooltip(new Tooltip("PlantUML integration enables diagram rendering directly in Markdown previews."));
        helpBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text-dim; -fx-border-color: #393b40; -fx-border-radius: 10; -fx-background-radius: 10; -fx-font-size: 10px; -fx-min-width: 18px; -fx-min-height: 18px; -fx-padding: 0;");

        Hyperlink installLink = new Hyperlink("Install");
        installLink.setStyle("-fx-text-fill: #589df6; -fx-font-size: 12px;");
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #62b543; -fx-font-size: 11px;");

        installLink.setOnAction(e -> {
            plantUmlCheck.setSelected(true);
            statusLabel.setText("PlantUML extension is ready");
        });

        HBox row = new HBox(8, plantUmlCheck, helpBtn, installLink, statusLabel);
        row.setAlignment(Pos.CENTER_LEFT);

        return new VBox(8, header, row);
    }

    private VBox buildCustomCssSection(AppSettingsStore.MarkdownSettingsConfig cfg) {
        Label title = new Label("Custom CSS");
        title.setStyle("-fx-text-fill: -text; -fx-font-weight: bold; -fx-font-size: 13px;");

        Separator sep = new Separator();
        HBox header = new HBox(10, title, sep);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(sep, Priority.ALWAYS);

        // Row 1: Load from
        CheckBox loadFromCheck = new CheckBox("Load from:");
        loadFromCheck.setSelected(cfg.isCustomCssLoadFromEnabled());
        loadFromCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-min-width: 100px;");
        inputs.put("languages_markdown_customCssLoadFromEnabled", loadFromCheck);

        TextField loadFromField = new TextField(cfg.getCustomCssPath());
        loadFromField.setPromptText("Path to stylesheet.css");
        loadFromField.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        HBox.setHgrow(loadFromField, Priority.ALWAYS);
        loadFromField.disableProperty().bind(loadFromCheck.selectedProperty().not());
        inputs.put("languages_markdown_customCssPath", loadFromField);

        Button browseCssBtn = new Button("📁");
        browseCssBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        browseCssBtn.disableProperty().bind(loadFromCheck.selectedProperty().not());
        browseCssBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Custom CSS Stylesheet");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("CSS Files (*.css)", "*.css"),
                    new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
            );
            File f = chooser.showOpenDialog(contentNodeWindow(browseCssBtn));
            if (f != null) {
                loadFromField.setText(f.getAbsolutePath());
            }
        });

        HBox loadFromRow = new HBox(8, loadFromCheck, loadFromField, browseCssBtn);
        loadFromRow.setAlignment(Pos.CENTER_LEFT);

        // Row 2: CSS rules
        CheckBox rulesCheck = new CheckBox("CSS rules:");
        rulesCheck.setSelected(cfg.isCustomCssRulesEnabled());
        rulesCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("languages_markdown_customCssRulesEnabled", rulesCheck);

        // Line-numbered code area
        TextArea rulesArea = new TextArea(cfg.getCustomCssRules());
        rulesArea.setPromptText("/* Add custom CSS rules here */");
        rulesArea.setPrefHeight(100);
        rulesArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px; -fx-control-inner-background: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4;");
        rulesArea.disableProperty().bind(rulesCheck.selectedProperty().not());
        inputs.put("languages_markdown_customCssRules", rulesArea);

        VBox rulesBox = new VBox(6, rulesCheck, rulesArea);

        return new VBox(8, header, loadFromRow, rulesBox);
    }

    private VBox buildPandocSection(AppSettingsStore.MarkdownSettingsConfig cfg) {
        Label title = new Label("Pandoc Settings");
        title.setStyle("-fx-text-fill: -text; -fx-font-weight: bold; -fx-font-size: 13px;");

        Separator sep = new Separator();
        HBox header = new HBox(10, title, sep);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(sep, Priority.ALWAYS);

        // Row 1: Path to Pandoc executable
        Label pathLabel = new Label("Path to Pandoc executable:");
        pathLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-min-width: 220px;");

        String currentPandoc = cfg.getPandocExecutablePath();
        TextField pathField = new TextField(currentPandoc);
        pathField.setPromptText("Cannot find the Pandoc executable");
        pathField.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        HBox.setHgrow(pathField, Priority.ALWAYS);
        inputs.put("languages_markdown_pandocExecutablePath", pathField);

        Button browsePandocBtn = new Button("📁");
        browsePandocBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        browsePandocBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Pandoc Executable");
            File f = chooser.showOpenDialog(contentNodeWindow(browsePandocBtn));
            if (f != null) {
                pathField.setText(f.getAbsolutePath());
            }
        });

        Button testBtn = new Button("Test");
        testBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-min-width: 55px;");

        Label testResultLabel = new Label();
        testResultLabel.setStyle("-fx-font-size: 12px;");

        testBtn.setOnAction(e -> {
            String path = pathField.getText().trim();
            if (path.isEmpty()) {
                path = "pandoc";
            }
            try {
                Process p = new ProcessBuilder(path, "--version").start();
                try (InputStream in = p.getInputStream()) {
                    String out = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    if (out.toLowerCase().contains("pandoc")) {
                        String firstLine = out.split("\\R")[0];
                        testResultLabel.setText("✓ " + firstLine);
                        testResultLabel.setStyle("-fx-text-fill: #62b543; -fx-font-size: 12px;");
                    } else {
                        testResultLabel.setText("✗ Cannot find the Pandoc executable");
                        testResultLabel.setStyle("-fx-text-fill: #e06c75; -fx-font-size: 12px;");
                    }
                }
            } catch (Exception ex) {
                testResultLabel.setText("✗ Cannot find the Pandoc executable");
                testResultLabel.setStyle("-fx-text-fill: #e06c75; -fx-font-size: 12px;");
            }
        });

        HBox pathRow = new HBox(8, pathLabel, pathField, browsePandocBtn, testBtn);
        pathRow.setAlignment(Pos.CENTER_LEFT);

        // Row 2: Save images from Microsoft Word to:
        Label wordImagesLabel = new Label("Save images from Microsoft Word to:");
        wordImagesLabel.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-min-width: 220px;");

        TextField wordImagesField = new TextField(cfg.getPandocWordImagesDir());
        wordImagesField.setPromptText("Directory to store extracted Word images");
        wordImagesField.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        HBox.setHgrow(wordImagesField, Priority.ALWAYS);
        inputs.put("languages_markdown_pandocWordImagesDir", wordImagesField);

        Button browseWordImagesBtn = new Button("📁");
        browseWordImagesBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4; -fx-background-radius: 4;");
        browseWordImagesBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Choose Folder to Save Images From Word");
            File d = chooser.showDialog(contentNodeWindow(browseWordImagesBtn));
            if (d != null) {
                wordImagesField.setText(d.getAbsolutePath());
            }
        });

        HBox wordImagesRow = new HBox(8, wordImagesLabel, wordImagesField, browseWordImagesBtn);
        wordImagesRow.setAlignment(Pos.CENTER_LEFT);

        VBox pandocBox = new VBox(8, header, pathRow);
        if (!testResultLabel.getText().isEmpty()) {
            pandocBox.getChildren().add(testResultLabel);
        }
        pandocBox.getChildren().add(wordImagesRow);
        return pandocBox;
    }

    private javafx.stage.Window contentNodeWindow(Node node) {
        if (node != null && node.getScene() != null) {
            return node.getScene().getWindow();
        }
        return null;
    }
}
