package com.roze.dbnavigator.ui.tools;

import com.roze.dbnavigator.db.AppSettingsStore;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Map;

/**
 * Settings panel for Backup and Sync.
 * Matches DataGrip screenshot media_1791728482546_d331bd74.png.
 */
public class BackupSyncPanel {

    private final AppSettingsStore.Settings settings;
    private final Map<String, Object> inputs;

    public BackupSyncPanel(AppSettingsStore.Settings settings, Map<String, Object> inputs) {
        this.settings = settings;
        this.inputs = inputs;
    }

    public Node createPanel() {
        AppSettingsStore.BackupSyncConfig cfg = settings.getBackupSync();

        VBox content = new VBox(14);
        content.setPadding(new Insets(10, 16, 20, 16));
        content.setStyle("-fx-background-color: transparent;");

        // Subtitle description matching DataGrip
        Label descLabel = new Label("Sync UI, Code and System settings, Keymaps, Plugins, and Tools.");
        descLabel.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 13px;");

        // Enable backup and sync row
        CheckBox enableSyncCheck = new CheckBox("Enable backup and sync:");
        enableSyncCheck.setSelected(cfg.isEnableBackupAndSync());
        enableSyncCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 13px;");
        inputs.put("backup_sync_enable", enableSyncCheck);

        ObservableList<String> accountsList = FXCollections.observableArrayList(cfg.getAvailableAccounts());
        if (!accountsList.contains(cfg.getSyncAccount())) {
            accountsList.add(0, cfg.getSyncAccount());
        }

        ComboBox<String> accountCombo = new ComboBox<>(accountsList);
        accountCombo.setValue(cfg.getSyncAccount());
        accountCombo.setPrefWidth(260);
        accountCombo.setStyle("-fx-background-color: -control-bg; -fx-border-color: -border-color; -fx-border-radius: 4px;");
        accountCombo.disableProperty().bind(enableSyncCheck.selectedProperty().not());
        inputs.put("backup_sync_account", accountCombo);

        // Account cell formatting with icon
        accountCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    Label badge = new Label("👤");
                    badge.setStyle("-fx-padding: 0 4 0 0;");
                    setGraphic(badge);
                    setStyle("-fx-text-fill: -text; -fx-font-size: 12px;");
                }
            }
        });
        accountCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    Label badge = new Label("👤");
                    badge.setStyle("-fx-padding: 0 4 0 0;");
                    setGraphic(badge);
                    setStyle("-fx-text-fill: -text; -fx-font-size: 12px;");
                }
            }
        });

        HBox syncRow = new HBox(10, enableSyncCheck, accountCombo);
        syncRow.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().addAll(descLabel, syncRow);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }
}
