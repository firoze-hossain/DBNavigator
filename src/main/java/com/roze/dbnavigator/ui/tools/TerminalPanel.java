package com.roze.dbnavigator.ui.tools;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Settings panel for Tools > Terminal.
 * Matches DataGrip screenshots media_1791727643185_9394cc32.png and media_1791727658253_c9c90f46.png.
 */
public class TerminalPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;
    private final Consumer<String> navigateTo;

    public TerminalPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs, Consumer<String> navigateTo) {
        this.settings = settings;
        this.inputs = inputs;
        this.navigateTo = navigateTo != null ? navigateTo : path -> {};
    }

    public Node createPanel() {
        AppSettingsStore.TerminalConfig cfg = settings.getTerminal();

        VBox content = new VBox(16);
        content.setPadding(new Insets(10, 16, 20, 16));
        content.setStyle("-fx-background-color: transparent;");

        // Top bar: Terminal engine & Revert changes
        Label engineLbl = new Label("Terminal engine:");
        engineLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ComboBox<String> engineCombo = new ComboBox<>(FXCollections.observableArrayList("Reworked 2025", "Classic"));
        engineCombo.setValue(cfg.getTerminalEngine());
        engineCombo.setStyle("-fx-background-color: -control-bg; -fx-border-color: -border-color; -fx-border-radius: 4px;");
        inputs.put("tools_terminal_engine", engineCombo);

        Hyperlink revertLink = new Hyperlink("Revert changes");
        revertLink.setStyle("-fx-text-fill: -accent; -fx-font-size: 12px; -fx-underline: false;");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        HBox topBar = new HBox(10, engineLbl, engineCombo, topSpacer, revertLink);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // =========================================================================
        // 1. Command Completion
        // =========================================================================
        Label completionSectionTitle = new Label("Command Completion");
        completionSectionTitle.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-font-weight: bold;");

        CheckBox showCompletionCheck = new CheckBox("Show a completion popup as you type:");
        showCompletionCheck.setSelected(cfg.isShowCompletionPopup());
        showCompletionCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_terminal_showCompletionPopup", showCompletionCheck);

        ToggleGroup completionTriggerGroup = new ToggleGroup();
        inputs.put("tools_terminal_completionTriggerGroup", completionTriggerGroup);

        RadioButton alwaysRadio = new RadioButton("Always");
        alwaysRadio.setToggleGroup(completionTriggerGroup);
        alwaysRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        RadioButton paramsOnlyRadio = new RadioButton("Only for parameters");
        paramsOnlyRadio.setToggleGroup(completionTriggerGroup);
        paramsOnlyRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        if ("Always".equalsIgnoreCase(cfg.getCompletionTrigger())) {
            alwaysRadio.setSelected(true);
        } else {
            paramsOnlyRadio.setSelected(true);
        }

        VBox completionRadioBox = new VBox(6, alwaysRadio, paramsOnlyRadio);
        completionRadioBox.setPadding(new Insets(0, 0, 0, 24));
        completionRadioBox.disableProperty().bind(showCompletionCheck.selectedProperty().not());

        CheckBox showPopupWithCheck = new CheckBox("Show completion popup with:");
        showPopupWithCheck.setSelected(cfg.isShowCompletionShortcutEnabled());
        showPopupWithCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_terminal_showCompletionShortcutCheck", showPopupWithCheck);

        ComboBox<String> shortcutCombo = new ComboBox<>(FXCollections.observableArrayList("Ctrl+Space", "Alt+Space", "Tab"));
        shortcutCombo.setValue(cfg.getCompletionShortcut());
        shortcutCombo.disableProperty().bind(showPopupWithCheck.selectedProperty().not());
        inputs.put("tools_terminal_completionShortcut", shortcutCombo);

        HBox shortcutRow = new HBox(8, showPopupWithCheck, shortcutCombo);
        shortcutRow.setAlignment(Pos.CENTER_LEFT);

        Label insertWithLbl = new Label("Insert suggestion with:");
        insertWithLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ComboBox<String> insertWithCombo = new ComboBox<>(FXCollections.observableArrayList("Enter", "Tab", "Space"));
        insertWithCombo.setValue(cfg.getInsertSuggestionWith());
        inputs.put("tools_terminal_insertSuggestionWith", insertWithCombo);

        HBox insertWithRow = new HBox(8, insertWithLbl, insertWithCombo);
        insertWithRow.setAlignment(Pos.CENTER_LEFT);

        VBox completionSection = new VBox(8, completionSectionTitle, showCompletionCheck, completionRadioBox, shortcutRow, insertWithRow);

        // =========================================================================
        // 2. Project Settings
        // =========================================================================
        Label projectSectionTitle = new Label("Project Settings");
        projectSectionTitle.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-font-weight: bold;");

        GridPane projectGrid = new GridPane();
        projectGrid.setHgap(10);
        projectGrid.setVgap(10);

        Label startDirLbl = new Label("Start directory:");
        startDirLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        TextField startDirField = new TextField(cfg.getStartDirectory());
        startDirField.setPrefWidth(350);
        inputs.put("tools_terminal_startDirectory", startDirField);

        Button startDirBrowseBtn = new Button("📁");
        startDirBrowseBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Start Directory");
            File dir = chooser.showDialog(startDirBrowseBtn.getScene().getWindow());
            if (dir != null) startDirField.setText(dir.getAbsolutePath());
        });

        HBox startDirBox = new HBox(6, startDirField, startDirBrowseBtn);
        HBox.setHgrow(startDirField, Priority.ALWAYS);

        Label envVarsLbl = new Label("Environment variables:");
        envVarsLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        TextField envVarsField = new TextField(cfg.getEnvironmentVariables());
        envVarsField.setPrefWidth(350);
        inputs.put("tools_terminal_environmentVariables", envVarsField);

        Button envVarsExpandBtn = new Button("⤢");
        envVarsExpandBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(envVarsField.getText());
            dialog.setTitle("Environment Variables");
            dialog.setHeaderText("Specify environment variables (format: KEY=VALUE;KEY2=VALUE2):");
            dialog.setContentText("Variables:");
            dialog.showAndWait().ifPresent(envVarsField::setText);
        });

        HBox envVarsBox = new HBox(6, envVarsField, envVarsExpandBtn);
        HBox.setHgrow(envVarsField, Priority.ALWAYS);

        projectGrid.add(startDirLbl, 0, 0);
        projectGrid.add(startDirBox, 1, 0);
        projectGrid.add(envVarsLbl, 0, 1);
        projectGrid.add(envVarsBox, 1, 1);

        VBox projectSection = new VBox(8, projectSectionTitle, projectGrid);

        // =========================================================================
        // 3. Font Settings
        // =========================================================================
        Label fontSectionTitle = new Label("Font Settings");
        fontSectionTitle.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-font-weight: bold;");

        List<String> fontFamilies = new ArrayList<>(Font.getFamilies());
        if (!fontFamilies.contains("DBNavigator Mono")) {
            fontFamilies.add(0, "DBNavigator Mono");
        }

        Label fontLbl = new Label("Font:");
        fontLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        ComboBox<String> fontCombo = new ComboBox<>(FXCollections.observableArrayList(fontFamilies));
        fontCombo.setValue(cfg.getFont());
        fontCombo.setPrefWidth(180);
        inputs.put("tools_terminal_font", fontCombo);

        Label fallbackFontLbl = new Label("Fallback:");
        fallbackFontLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        ComboBox<String> fallbackCombo = new ComboBox<>(FXCollections.observableArrayList(fontFamilies));
        fallbackCombo.setValue(cfg.getFallbackFont());
        fallbackCombo.setPrefWidth(180);
        inputs.put("tools_terminal_fallbackFont", fallbackCombo);

        HBox fontRow = new HBox(12, fontLbl, fontCombo, fallbackFontLbl, fallbackCombo);
        fontRow.setAlignment(Pos.CENTER_LEFT);

        Label sizeLbl = new Label("Size:");
        sizeLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        Spinner<Double> sizeSpinner = new Spinner<>(8.0, 48.0, cfg.getFontSize(), 0.5);
        sizeSpinner.setEditable(true);
        sizeSpinner.setPrefWidth(85);
        inputs.put("tools_terminal_fontSize", sizeSpinner);

        Label lineHeightLbl = new Label("Line height:");
        lineHeightLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        Spinner<Double> lineHeightSpinner = new Spinner<>(0.5, 3.0, cfg.getLineHeight(), 0.1);
        lineHeightSpinner.setEditable(true);
        lineHeightSpinner.setPrefWidth(85);
        inputs.put("tools_terminal_lineHeight", lineHeightSpinner);

        Label colWidthLbl = new Label("Column width:");
        colWidthLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        Spinner<Double> colWidthSpinner = new Spinner<>(0.5, 3.0, cfg.getColumnWidth(), 0.1);
        colWidthSpinner.setEditable(true);
        colWidthSpinner.setPrefWidth(85);
        inputs.put("tools_terminal_columnWidth", colWidthSpinner);

        HBox metricRow = new HBox(12, sizeLbl, sizeSpinner, lineHeightLbl, lineHeightSpinner, colWidthLbl, colWidthSpinner);
        metricRow.setAlignment(Pos.CENTER_LEFT);

        Hyperlink configureColorsLink = new Hyperlink("Configure colors...");
        configureColorsLink.setStyle("-fx-text-fill: -accent; -fx-font-size: 12px; -fx-underline: false;");
        configureColorsLink.setOnAction(e -> navigateTo.accept("Editor / Color Scheme / Console Colors"));

        VBox fontSection = new VBox(8, fontSectionTitle, fontRow, metricRow, configureColorsLink);

        // =========================================================================
        // 4. Application Settings
        // =========================================================================
        Label appSectionTitle = new Label("Application Settings");
        appSectionTitle.setStyle("-fx-text-fill: -text; -fx-font-size: 13px; -fx-font-weight: bold;");

        GridPane appGrid = new GridPane();
        appGrid.setHgap(10);
        appGrid.setVgap(10);

        Label shellPathLbl = new Label("Shell path:");
        shellPathLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        TextField shellPathField = new TextField(cfg.getShellPath());
        shellPathField.setPrefWidth(350);
        inputs.put("tools_terminal_shellPath", shellPathField);

        Button shellPathBrowseBtn = new Button("...");
        shellPathBrowseBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Shell Executable");
            File f = chooser.showOpenDialog(shellPathBrowseBtn.getScene().getWindow());
            if (f != null) shellPathField.setText(f.getAbsolutePath());
        });

        HBox shellPathBox = new HBox(6, shellPathField, shellPathBrowseBtn);
        HBox.setHgrow(shellPathField, Priority.ALWAYS);

        Label tabNameLbl = new Label("Default tab name:");
        tabNameLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        TextField tabNameField = new TextField(cfg.getDefaultTabName());
        tabNameField.setPrefWidth(350);
        inputs.put("tools_terminal_defaultTabName", tabNameField);

        appGrid.add(shellPathLbl, 0, 0);
        appGrid.add(shellPathBox, 1, 0);
        appGrid.add(tabNameLbl, 0, 1);
        appGrid.add(tabNameField, 1, 1);

        CheckBox useAppTitleCheck = new CheckBox("Use application title as tab name:");
        useAppTitleCheck.setSelected(cfg.isUseApplicationTitleAsTabName());
        useAppTitleCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_terminal_useApplicationTitleAsTabName", useAppTitleCheck);

        ToggleGroup appTitleGroup = new ToggleGroup();
        inputs.put("tools_terminal_appTitleModeGroup", appTitleGroup);

        RadioButton runningRadio = new RadioButton("When command is running");
        runningRadio.setToggleGroup(appTitleGroup);
        runningRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        RadioButton appAlwaysRadio = new RadioButton("Always");
        appAlwaysRadio.setToggleGroup(appTitleGroup);
        appAlwaysRadio.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        if ("Always".equalsIgnoreCase(cfg.getApplicationTitleMode())) {
            appAlwaysRadio.setSelected(true);
        } else {
            runningRadio.setSelected(true);
        }

        VBox appTitleRadioBox = new VBox(6, runningRadio, appAlwaysRadio);
        appTitleRadioBox.setPadding(new Insets(0, 0, 0, 24));
        appTitleRadioBox.disableProperty().bind(useAppTitleCheck.selectedProperty().not());

        CheckBox contrastCheck = new CheckBox("Enforce minimum contrast ratio:");
        contrastCheck.setSelected(cfg.isEnforceMinimumContrastRatio());
        contrastCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_terminal_enforceMinimumContrastRatio", contrastCheck);

        TextField contrastField = new TextField(String.valueOf(cfg.getMinimumContrastRatio()));
        contrastField.setPrefWidth(60);
        contrastField.disableProperty().bind(contrastCheck.selectedProperty().not());
        inputs.put("tools_terminal_minimumContrastRatio", contrastField);

        Label helpContrast = new Label("❓");
        helpContrast.setTooltip(new Tooltip("Ensures text foreground and background colors meet the specified WCAG contrast threshold."));

        HBox contrastBox = new HBox(8, contrastCheck, contrastField, helpContrast);
        contrastBox.setAlignment(Pos.CENTER_LEFT);

        CheckBox showSeparatorsCheck = new CheckBox("Show separators between executed commands");
        showSeparatorsCheck.setSelected(cfg.isShowSeparatorsBetweenExecutedCommands());
        showSeparatorsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_terminal_showSeparators", showSeparatorsCheck);

        CheckBox audibleBellCheck = new CheckBox("Audible bell");
        audibleBellCheck.setSelected(cfg.isAudibleBell());
        audibleBellCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_terminal_audibleBell", audibleBellCheck);

        CheckBox closeSessionCheck = new CheckBox("Close session when it ends");
        closeSessionCheck.setSelected(cfg.isCloseSessionWhenItEnds());
        closeSessionCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_terminal_closeSessionWhenItEnds", closeSessionCheck);

        CheckBox mouseReportingCheck = new CheckBox("Mouse reporting");
        mouseReportingCheck.setSelected(cfg.isMouseReporting());
        mouseReportingCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_terminal_mouseReporting", mouseReportingCheck);

        CheckBox moveFocusCheck = new CheckBox("Move focus to the Editor with:");
        moveFocusCheck.setSelected(cfg.isMoveFocusToEditor());
        moveFocusCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_terminal_moveFocusToEditor", moveFocusCheck);

        ComboBox<String> moveFocusCombo = new ComboBox<>(FXCollections.observableArrayList("Escape", "Ctrl+E", "F12"));
        moveFocusCombo.setValue(cfg.getMoveFocusKey());
        moveFocusCombo.disableProperty().bind(moveFocusCheck.selectedProperty().not());
        inputs.put("tools_terminal_moveFocusKey", moveFocusCombo);

        HBox moveFocusBox = new HBox(8, moveFocusCheck, moveFocusCombo);
        moveFocusBox.setAlignment(Pos.CENTER_LEFT);

        CheckBox pasteOnMiddleCheck = new CheckBox("Paste on middle mouse button click");
        pasteOnMiddleCheck.setSelected(cfg.isPasteOnMiddleMouseButtonClick());
        pasteOnMiddleCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_terminal_pasteOnMiddleClick", pasteOnMiddleCheck);

        CheckBox overrideShortcutsCheck = new CheckBox("Override IDE shortcuts");
        overrideShortcutsCheck.setSelected(cfg.isOverrideIdeShortcuts());
        overrideShortcutsCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_terminal_overrideIdeShortcuts", overrideShortcutsCheck);

        Hyperlink configureShortcutsLink = new Hyperlink("Configure terminal keybindings");
        configureShortcutsLink.setStyle("-fx-text-fill: -accent; -fx-font-size: 12px; -fx-underline: false;");
        configureShortcutsLink.setOnAction(e -> navigateTo.accept("Keymap"));

        HBox overrideRow = new HBox(12, overrideShortcutsCheck, configureShortcutsLink);
        overrideRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox shellIntegrationCheck = new CheckBox("Shell integration");
        shellIntegrationCheck.setSelected(cfg.isShellIntegration());
        shellIntegrationCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_terminal_shellIntegration", shellIntegrationCheck);

        CheckBox highlightLinksCheck = new CheckBox("Highlight hyperlinks");
        highlightLinksCheck.setSelected(cfg.isHighlightHyperlinks());
        highlightLinksCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("tools_terminal_highlightHyperlinks", highlightLinksCheck);

        Label cursorShapeLbl = new Label("Cursor shape:");
        cursorShapeLbl.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");

        ComboBox<String> cursorCombo = new ComboBox<>(FXCollections.observableArrayList("Block", "Underline", "Vertical bar"));
        cursorCombo.setValue(cfg.getCursorShape());
        inputs.put("tools_terminal_cursorShape", cursorCombo);

        HBox cursorRow = new HBox(10, cursorShapeLbl, cursorCombo);
        cursorRow.setAlignment(Pos.CENTER_LEFT);

        VBox appSection = new VBox(10,
                appSectionTitle,
                appGrid,
                useAppTitleCheck,
                appTitleRadioBox,
                contrastBox,
                showSeparatorsCheck,
                audibleBellCheck,
                closeSessionCheck,
                mouseReportingCheck,
                moveFocusBox,
                pasteOnMiddleCheck,
                overrideRow,
                shellIntegrationCheck,
                highlightLinksCheck,
                cursorRow
        );

        // Revert changes handler
        revertLink.setOnAction(e -> {
            AppSettingsStore.TerminalConfig def = settings.getTerminal();
            engineCombo.setValue(def.getTerminalEngine());
            showCompletionCheck.setSelected(def.isShowCompletionPopup());
            if ("Always".equalsIgnoreCase(def.getCompletionTrigger())) alwaysRadio.setSelected(true);
            else paramsOnlyRadio.setSelected(true);
            showPopupWithCheck.setSelected(def.isShowCompletionShortcutEnabled());
            shortcutCombo.setValue(def.getCompletionShortcut());
            insertWithCombo.setValue(def.getInsertSuggestionWith());
            startDirField.setText(def.getStartDirectory());
            envVarsField.setText(def.getEnvironmentVariables());
            fontCombo.setValue(def.getFont());
            fallbackCombo.setValue(def.getFallbackFont());
            sizeSpinner.getValueFactory().setValue(def.getFontSize());
            lineHeightSpinner.getValueFactory().setValue(def.getLineHeight());
            colWidthSpinner.getValueFactory().setValue(def.getColumnWidth());
            shellPathField.setText(def.getShellPath());
            tabNameField.setText(def.getDefaultTabName());
            useAppTitleCheck.setSelected(def.isUseApplicationTitleAsTabName());
            if ("Always".equalsIgnoreCase(def.getApplicationTitleMode())) appAlwaysRadio.setSelected(true);
            else runningRadio.setSelected(true);
            contrastCheck.setSelected(def.isEnforceMinimumContrastRatio());
            contrastField.setText(String.valueOf(def.getMinimumContrastRatio()));
            showSeparatorsCheck.setSelected(def.isShowSeparatorsBetweenExecutedCommands());
            audibleBellCheck.setSelected(def.isAudibleBell());
            closeSessionCheck.setSelected(def.isCloseSessionWhenItEnds());
            mouseReportingCheck.setSelected(def.isMouseReporting());
            moveFocusCheck.setSelected(def.isMoveFocusToEditor());
            moveFocusCombo.setValue(def.getMoveFocusKey());
            pasteOnMiddleCheck.setSelected(def.isPasteOnMiddleMouseButtonClick());
            overrideShortcutsCheck.setSelected(def.isOverrideIdeShortcuts());
            shellIntegrationCheck.setSelected(def.isShellIntegration());
            highlightLinksCheck.setSelected(def.isHighlightHyperlinks());
            cursorCombo.setValue(def.getCursorShape());
        });

        content.getChildren().addAll(topBar, completionSection, projectSection, fontSection, appSection);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }
}
