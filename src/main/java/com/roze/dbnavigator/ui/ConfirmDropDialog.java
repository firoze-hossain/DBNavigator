package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * DataGrip-grade Confirm Drop dialog.
 * Supports dropping single or multiple selected objects (tables, sequences, views, etc.)
 * with schema qualification options, IF EXISTS, DROP CASCADE, and dynamic SQL preview.
 */
public class ConfirmDropDialog extends Dialog<ButtonType> {

    public static void show(MainWindow mainWindow, ConnectionProfile profile, List<DbObject> objects, Runnable onSuccess) {
        if (objects == null || objects.isEmpty()) return;
        ConfirmDropDialog dialog = new ConfirmDropDialog(mainWindow, profile, objects, onSuccess);
        dialog.showAndWait();
    }

    public ConfirmDropDialog(MainWindow mainWindow, ConnectionProfile profile, List<DbObject> objects, Runnable onSuccess) {
        DialogTheme.apply(this);
        if (ThemeManager.getCurrent() == com.roze.dbnavigator.db.AppSettingsStore.Theme.LIGHT) {
            getDialogPane().getStylesheets().clear();
            getDialogPane().getStylesheets().add(ThemeManager.stylesheetUrl(com.roze.dbnavigator.db.AppSettingsStore.Theme.LIGHT));
        }

        if (mainWindow != null && mainWindow.getOwnerWindow() != null) {
            initOwner(mainWindow.getOwnerWindow());
        }

        setResizable(false);
        setTitle("Confirm Drop");
        setHeaderText(null);
        setGraphic(null);

        ComboBox<String> qualifyCombo = new ComboBox<>();
        qualifyCombo.getItems().addAll("Auto", "Always", "Never");
        qualifyCombo.setValue("Auto");
        qualifyCombo.getStyleClass().add("drop-qualify-combo");

        HBox qualifyBox = new HBox(10, new Label("Qualify objects with schema names:"), qualifyCombo);
        qualifyBox.setAlignment(Pos.CENTER_LEFT);

        CheckBox ifExistsCb = new CheckBox("Use IF EXISTS syntax");
        CheckBox cascadeCb = new CheckBox("Use DROP CASCADE syntax");
        CheckBox reformatCb = new CheckBox("Reformat generated code");
        reformatCb.setSelected(true);

        Label previewLabel = new Label("⌄ Preview");
        previewLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: -text-dim;");

        Button copySqlBtn = new Button();
        copySqlBtn.setGraphic(Icons.of(FontAwesomeSolid.COPY, "#868a91", 11));
        copySqlBtn.setTooltip(new Tooltip("Copy SQL to clipboard"));
        copySqlBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2 4;");

        HBox previewHeaderBox = new HBox(previewLabel);
        previewHeaderBox.setAlignment(Pos.CENTER_LEFT);

        TextArea previewArea = new TextArea();
        previewArea.setEditable(false);
        previewArea.setWrapText(false);
        previewArea.setPrefRowCount(Math.min(5, Math.max(2, objects.size())));
        previewArea.getStyleClass().add("confirm-drop-preview");
        previewArea.setMinHeight(75);
        previewArea.setPrefHeight(90);
        previewArea.setMaxHeight(130);
        VBox.setVgrow(previewArea, Priority.NEVER);

        copySqlBtn.setOnAction(e -> {
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(previewArea.getText());
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        });

        Runnable updateSqlPreview = () -> {
            StringBuilder sb = new StringBuilder();
            String qualifyMode = qualifyCombo.getValue();
            boolean ifExists = ifExistsCb.isSelected();
            boolean cascade = cascadeCb.isSelected();

            for (DbObject obj : objects) {
                String kindKeyword = resolveDropKeyword(obj);
                String schema = obj.getSchema();
                String name = obj.getName();
                String qualifiedName;

                if ("Always".equals(qualifyMode)) {
                    qualifiedName = (schema != null && !schema.isBlank() ? schema : "public") + "." + name;
                } else if ("Never".equals(qualifyMode)) {
                    qualifiedName = name;
                } else { // "Auto"
                    if (schema != null && !schema.isBlank()) {
                        qualifiedName = schema + "." + name;
                    } else {
                        qualifiedName = name;
                    }
                }

                sb.append("drop ").append(kindKeyword);
                if (ifExists) {
                    sb.append(" if exists");
                }
                sb.append(" ").append(qualifiedName);
                if (cascade) {
                    sb.append(" cascade");
                }
                sb.append(";\n");
            }
            previewArea.setText(sb.toString().trim());
        };

        qualifyCombo.valueProperty().addListener((obs, o, n) -> updateSqlPreview.run());
        ifExistsCb.selectedProperty().addListener((obs, o, n) -> updateSqlPreview.run());
        cascadeCb.selectedProperty().addListener((obs, o, n) -> updateSqlPreview.run());
        updateSqlPreview.run();

        VBox content = new VBox(10,
                qualifyBox,
                ifExistsCb,
                cascadeCb,
                reformatCb,
                previewHeaderBox,
                previewArea
        );
        content.setPadding(new Insets(14, 18, 12, 18));
        content.setPrefWidth(490);
        content.setMaxWidth(490);
        content.setFillWidth(true);

        getDialogPane().getStyleClass().add("confirm-drop-dialog");
        getDialogPane().setPrefWidth(500);
        getDialogPane().setMaxWidth(500);
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
        if (okBtn != null) {
            okBtn.setDefaultButton(true);
            okBtn.getStyleClass().add("primary-button");
        }

        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String sqlToExecute = previewArea.getText();
                executeDropStatements(mainWindow, profile, objects, sqlToExecute, onSuccess);
            }
            return btn;
        });
    }

    private static String resolveDropKeyword(DbObject obj) {
        if (obj == null || obj.getKind() == null) return "table";
        return switch (obj.getKind()) {
            case TABLE, PARTITION -> "table";
            case VIEW -> "view";
            case SEQUENCE -> "sequence";
            case INDEX -> "index";
            case FUNCTION -> "function";
            case PROCEDURE -> "procedure";
            case SCHEMA -> "schema";
            case COLLECTION -> "collection";
            default -> "table";
        };
    }

    private static void executeDropStatements(
            MainWindow mainWindow,
            ConnectionProfile profile,
            List<DbObject> objects,
            String script,
            Runnable onSuccess) {

        AppExecutor.run(() -> {
            try {
                String catalog = objects.get(0).getCatalog();
                try (Connection conn = ClientRegistry.jdbc(profile, catalog).getConnection();
                     Statement stmt = conn.createStatement()) {

                    String[] statements = script.split(";");
                    for (String sql : statements) {
                        String trimmed = sql.trim();
                        if (!trimmed.isBlank()) {
                            stmt.execute(trimmed);
                        }
                    }
                }

                Platform.runLater(() -> {
                    if (mainWindow != null) {
                        if (objects.size() == 1) {
                            mainWindow.setStatus("Dropped " + resolveDropKeyword(objects.get(0)) + " " + objects.get(0).getName());
                        } else {
                            mainWindow.setStatus("Dropped " + objects.size() + " database object(s)");
                        }
                    }
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Could not drop objects: " + msg);
                    DialogTheme.apply(alert);
                    alert.showAndWait();
                });
            }
        });
    }
}
