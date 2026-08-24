package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * DataGrip-style Settings dialog: a category tree on the left (Appearance &
 * Behavior > Appearance, Editor > General, Editor > Font, Plugins) and the
 * matching options panel on the right, with OK/Cancel/Apply.
 *
 * Honest scope note: this covers the specific categories requested, not
 * DataGrip's full settings tree (Keymap, Version Control, dozens of Editor
 * sub-pages, etc.) — those aren't implemented.
 */
public final class SettingsDialog {

    private SettingsDialog() {}

    public static void show(MainWindow mainWindow) {
        AppSettingsStore.Settings settings = AppSettingsStore.load();
        Stage stage = new Stage();
        Window owner = mainWindow.getOwnerWindow();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Settings");
        stage.setMinWidth(760);
        stage.setMinHeight(520);

        // ---- category tree ----
        TreeItem<String> root = new TreeItem<>("root");
        TreeItem<String> appearanceBehavior = new TreeItem<>("Appearance & Behavior");
        TreeItem<String> appearance = new TreeItem<>("Appearance");
        appearanceBehavior.getChildren().add(appearance);
        appearanceBehavior.setExpanded(true);

        TreeItem<String> editor = new TreeItem<>("Editor");
        TreeItem<String> general = new TreeItem<>("General");
        TreeItem<String> font = new TreeItem<>("Font");
        editor.getChildren().addAll(general, font);
        editor.setExpanded(true);

        TreeItem<String> plugins = new TreeItem<>("Plugins");
        TreeItem<String> updates = new TreeItem<>("Updates");

        root.getChildren().addAll(appearanceBehavior, editor, plugins, updates);
        TreeView<String> tree = new TreeView<>(root);
        tree.setShowRoot(false);
        tree.setPrefWidth(220);

        // ---- panels ----
        VBox appearancePanel = buildAppearancePanel(settings);
        VBox generalPanel = buildGeneralPanel(settings);
        VBox fontPanel = buildFontPanel(settings);
        VBox pluginsPanel = buildPluginsPanel();
        VBox updatesPanel = buildUpdatesPanel(settings);

        StackPane content = new StackPane(appearancePanel, generalPanel, fontPanel, pluginsPanel, updatesPanel);
        content.setPadding(new Insets(20));
        showOnly(content, appearancePanel);

        tree.getSelectionModel().selectedItemProperty().addListener((obs, was, item) -> {
            if (item == null) return;
            switch (item.getValue()) {
                case "Appearance" -> showOnly(content, appearancePanel);
                case "General" -> showOnly(content, generalPanel);
                case "Font" -> showOnly(content, fontPanel);
                case "Plugins" -> showOnly(content, pluginsPanel);
                case "Updates" -> showOnly(content, updatesPanel);
                default -> { /* category header clicked — keep current panel */ }
            }
        });
        tree.getSelectionModel().select(appearance);

        SplitPane split = new SplitPane(tree, content);
        split.setDividerPositions(0.26);
        SplitPane.setResizableWithParent(tree, false);
        VBox.setVgrow(split, Priority.ALWAYS);

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> stage.close());
        Button apply = new Button("Apply");
        Button ok = new Button("OK");
        ok.getStyleClass().add("run-button");
        ok.setDefaultButton(true);

        Runnable applyAction = () -> applySettings(mainWindow, settings, appearancePanel, generalPanel, fontPanel, updatesPanel);
        apply.setOnAction(e -> applyAction.run());
        ok.setOnAction(e -> { applyAction.run(); stage.close(); });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10, spacer, ok, cancel, apply);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(10, 16, 12, 16));

        VBox rootBox = new VBox(split, buttons);
        rootBox.getStyleClass().add("app-root");
        Scene scene = new Scene(rootBox, 800, 560);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
        stage.show();
    }

    private static void showOnly(StackPane content, VBox panel) {
        for (var node : content.getChildren()) {
            node.setVisible(node == panel);
            node.setManaged(node == panel);
        }
    }

    // -------------------------------------------------------------- panels

    private static VBox buildAppearancePanel(AppSettingsStore.Settings settings) {
        Label title = new Label("Appearance & Behavior \u203a Appearance");
        title.getStyleClass().add("panel-header");

        Label themeLabel = new Label("Theme:");
        themeLabel.getStyleClass().add("connection-field-label");
        ComboBox<AppSettingsStore.Theme> themeCombo = new ComboBox<>();
        themeCombo.getItems().addAll(AppSettingsStore.Theme.DARK, AppSettingsStore.Theme.LIGHT);
        themeCombo.getSelectionModel().select(settings.getTheme());
        themeCombo.setPrefWidth(220);

        Label hint = new Label("Changes apply immediately to this window; already-open tabs/dialogs "
                + "pick up the new theme the next time they're opened.");
        hint.getStyleClass().add("console-status");
        hint.setWrapText(true);
        hint.setMaxWidth(420);

        HBox row = new HBox(10, themeLabel, themeCombo);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox panel = new VBox(14, title, row, hint);
        panel.setUserData(themeCombo);
        return panel;
    }

    private static VBox buildGeneralPanel(AppSettingsStore.Settings settings) {
        Label title = new Label("Editor \u203a General");
        title.getStyleClass().add("panel-header");

        Label mouseControl = new Label("Mouse Control");
        mouseControl.getStyleClass().add("connection-section-label");

        CheckBox ctrlScrollCheck = new CheckBox("Change font size with Ctrl+Mouse Wheel in the editor");
        ctrlScrollCheck.setSelected(settings.isCtrlScrollZoomEnabled());

        Label hint = new Label("Scroll with Ctrl held over any SQL console to zoom that editor's text in or out. "
                + "Ctrl+Plus / Ctrl+Minus always work too, regardless of this setting or your OS's own "
                + "scroll-gesture settings.");
        hint.getStyleClass().add("console-status");
        hint.setWrapText(true);
        hint.setMaxWidth(420);

        VBox panel = new VBox(14, title, mouseControl, ctrlScrollCheck, hint);
        panel.setUserData(ctrlScrollCheck);
        return panel;
    }

    private static VBox buildFontPanel(AppSettingsStore.Settings settings) {
        Label title = new Label("Editor \u203a Font");
        title.getStyleClass().add("panel-header");

        Label fontLabel = new Label("Font:");
        fontLabel.getStyleClass().add("connection-field-label");
        ComboBox<String> fontCombo = new ComboBox<>();
        fontCombo.getItems().addAll(javafx.scene.text.Font.getFamilies());
        fontCombo.getSelectionModel().select(settings.getEditorFontFamily());
        fontCombo.setPrefWidth(240);
        fontCombo.setEditable(true);

        Label sizeLabel = new Label("Size:");
        sizeLabel.getStyleClass().add("connection-field-label");
        Spinner<Double> sizeSpinner = new Spinner<>(8, 48, settings.getEditorFontSize(), 1);
        sizeSpinner.setEditable(true);
        sizeSpinner.setPrefWidth(90);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        grid.add(fontLabel, 0, 0);
        grid.add(fontCombo, 1, 0);
        grid.add(sizeLabel, 0, 1);
        grid.add(sizeSpinner, 1, 1);

        TextArea preview = new TextArea("SELECT * FROM users WHERE id = 1;\n-- Live preview of the editor font");
        preview.setEditable(false);
        preview.setPrefRowCount(4);
        preview.getStyleClass().add("process-output");
        Runnable refreshPreview = () -> preview.setStyle(
                "-fx-font-family: '" + fontCombo.getValue() + "'; -fx-font-size: " + sizeSpinner.getValue() + "px;");
        fontCombo.valueProperty().addListener((o, a, b) -> refreshPreview.run());
        sizeSpinner.valueProperty().addListener((o, a, b) -> refreshPreview.run());
        refreshPreview.run();

        VBox panel = new VBox(14, title, grid, preview);
        VBox.setVgrow(preview, Priority.ALWAYS);
        panel.getProperties().put("fontCombo", fontCombo);
        panel.getProperties().put("sizeSpinner", sizeSpinner);
        return panel;
    }

    private static VBox buildPluginsPanel() {
        Label title = new Label("Plugins");
        title.getStyleClass().add("panel-header");
        Label body = new Label(
                "DBNavigator Pro doesn't have a plugin system \u2014 it's a fixed-feature desktop "
                + "app rather than an extensible IDE, so there's nothing to install or manage here.\n\n"
                + "Everything in this app ships built in: connection management, SQL consoles with "
                + "autocomplete, data editing, dump/restore, ER diagrams, and Local History.");
        body.getStyleClass().add("console-status");
        body.setWrapText(true);
        body.setMaxWidth(480);
        VBox panel = new VBox(14, title, body);
        return panel;
    }


    private static VBox buildUpdatesPanel(AppSettingsStore.Settings settings) {
        Label title = new Label("Updates");
        title.getStyleClass().add("panel-header");

        CheckBox autoCheck = new CheckBox("Automatically check for application updates");
        autoCheck.setSelected(settings.isAutoUpdateEnabled());
        CheckBox autoDownload = new CheckBox("Automatically download updates when available");
        autoDownload.setSelected(settings.isAutoDownloadUpdates());

        Label channelLabel = new Label("Update channel:");
        channelLabel.getStyleClass().add("connection-field-label");
        ComboBox<String> channel = new ComboBox<>();
        channel.getItems().addAll("Stable", "Beta", "Nightly");
        channel.getSelectionModel().select(settings.getUpdateChannel());
        channel.setPrefWidth(180);

        Label endpointLabel = new Label("RozeHub endpoint:");
        endpointLabel.getStyleClass().add("connection-field-label");
        TextField endpoint = new TextField(settings.getUpdateEndpoint());
        endpoint.setPromptText("Leave blank to use the configured RozeHub endpoint");
        endpoint.setPrefWidth(420);

        Label hint = new Label("Updates are downloaded outside the DBNavigator installation. Every package is verified with SHA-256 before the operating-system installer is started.");
        hint.getStyleClass().add("console-status");
        hint.setWrapText(true);
        hint.setMaxWidth(500);

        Button check = new Button("Check for Updates…");
        check.setOnAction(e -> {
            Window owner = check.getScene() == null ? null : check.getScene().getWindow();
            AppUpdateDialog.check(owner, false);
        });

        VBox panel = new VBox(14, title, autoCheck, autoDownload, channelLabel, channel, endpointLabel, endpoint, hint, check);
        panel.getProperties().put("autoCheck", autoCheck);
        panel.getProperties().put("autoDownload", autoDownload);
        panel.getProperties().put("channel", channel);
        panel.getProperties().put("endpoint", endpoint);
        return panel;
    }

    // ------------------------------------------------------------- apply

    @SuppressWarnings("unchecked")
    private static void applySettings(MainWindow mainWindow, AppSettingsStore.Settings settings,
                                      VBox appearancePanel, VBox generalPanel, VBox fontPanel, VBox updatesPanel) {
        ComboBox<AppSettingsStore.Theme> themeCombo = (ComboBox<AppSettingsStore.Theme>) appearancePanel.getUserData();
        CheckBox ctrlScrollCheck = (CheckBox) generalPanel.getUserData();
        ComboBox<String> fontCombo = (ComboBox<String>) fontPanel.getProperties().get("fontCombo");
        Spinner<Double> sizeSpinner = (Spinner<Double>) fontPanel.getProperties().get("sizeSpinner");

        settings.setTheme(themeCombo.getValue());
        settings.setCtrlScrollZoomEnabled(ctrlScrollCheck.isSelected());
        settings.setEditorFontFamily(fontCombo.getValue());
        settings.setEditorFontSize(sizeSpinner.getValue());

        CheckBox autoCheck = (CheckBox) updatesPanel.getProperties().get("autoCheck");
        CheckBox autoDownload = (CheckBox) updatesPanel.getProperties().get("autoDownload");
        ComboBox<String> channel = (ComboBox<String>) updatesPanel.getProperties().get("channel");
        TextField endpoint = (TextField) updatesPanel.getProperties().get("endpoint");
        settings.setAutoUpdateEnabled(autoCheck.isSelected());
        settings.setAutoDownloadUpdates(autoDownload.isSelected());
        settings.setUpdateChannel(channel.getValue() == null ? "Stable" : channel.getValue());
        settings.setUpdateEndpoint(endpoint.getText() == null ? "" : endpoint.getText().trim());
        AppSettingsStore.save(settings);

        ThemeManager.setTheme(settings.getTheme());
        mainWindow.applyEditorFontToOpenConsoles();
        mainWindow.setStatus("Settings applied");
    }
}
