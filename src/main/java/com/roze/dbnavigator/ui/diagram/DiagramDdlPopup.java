package com.roze.dbnavigator.ui.diagram;

import com.roze.dbnavigator.db.DdlGeneratorService;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.ui.Icons;
import com.roze.dbnavigator.ui.MainWindow;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/**
 * DataGrip-style Quick Definition / DDL overlay popup (Images 3 & 4),
 * displaying source metadata breadcrumb, formatted syntax DDL, and action buttons.
 */
public class DiagramDdlPopup extends VBox {

    private final ConnectionProfile profile;
    private final DbObject table;
    private final Runnable onClose;
    private final TextArea codeArea = new TextArea();
    private final Label statusLabel = new Label("Generating DDL…");
    private final ProgressIndicator spinner = new ProgressIndicator();
    private double dragStartX, dragStartY;

    public DiagramDdlPopup(ConnectionProfile profile, DbObject table, Runnable onClose) {
        this.profile = profile;
        this.table = table;
        this.onClose = onClose;

        getStyleClass().add("diagram-ddl-popup");
        setPrefWidth(520);
        setPrefHeight(380);
        setMaxWidth(680);
        setMaxHeight(500);

        // Header bar with metadata breadcrumb
        HBox header = buildHeader();
        header.getStyleClass().add("diagram-ddl-header");

        // Code area setup
        codeArea.setEditable(false);
        codeArea.setWrapText(false);
        codeArea.getStyleClass().add("diagram-ddl-code");
        VBox.setVgrow(codeArea, Priority.ALWAYS);

        // Footer toolbar
        HBox footer = buildFooter();
        footer.getStyleClass().add("diagram-ddl-footer");

        getChildren().addAll(header, codeArea, footer);

        // Enable dragging via header
        header.setOnMousePressed(e -> {
            dragStartX = e.getSceneX() - getLayoutX();
            dragStartY = e.getSceneY() - getLayoutY();
        });
        header.setOnMouseDragged(e -> {
            setLayoutX(Math.max(10, e.getSceneX() - dragStartX));
            setLayoutY(Math.max(10, e.getSceneY() - dragStartY));
        });

        // Load DDL asynchronously
        loadDdl();
    }

    private HBox buildHeader() {
        Node icon = Icons.of(FontAwesomeSolid.CODE, "#e5c07b", 12);

        String dsName = profile != null ? profile.getName() : "database";
        String dbName = table.getCatalog() != null ? table.getCatalog() : (profile != null ? profile.getDatabase() : "");
        String scName = table.getSchema() != null ? table.getSchema() : "public";
        String tbName = table.getName();

        Label dsLabel = makePill("Data Source", dsName);
        Label dbLabel = makePill("Database", dbName);
        Label scLabel = makePill("Schema", scName);
        Label tbLabel = makePill("Table", tbName);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button();
        closeBtn.setGraphic(Icons.of(FontAwesomeSolid.TIMES, "#a9b7c6", 11));
        closeBtn.setTooltip(new Tooltip("Close (Esc)"));
        closeBtn.getStyleClass().add("diagram-ddl-close-btn");
        closeBtn.setOnAction(e -> {
            if (onClose != null) onClose.run();
        });

        HBox header = new HBox(8, icon, dsLabel, dbLabel, scLabel, tbLabel, spacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 10, 8, 12));
        return header;
    }

    private Label makePill(String label, String value) {
        if (value == null || value.isBlank()) value = "-";
        Label pill = new Label(label + ": " + value);
        pill.getStyleClass().add("diagram-ddl-pill");
        return pill;
    }

    private HBox buildFooter() {
        spinner.setPrefSize(14, 14);
        statusLabel.getStyleClass().add("diagram-ddl-status");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button copyBtn = new Button("Copy DDL");
        copyBtn.setGraphic(Icons.of(FontAwesomeSolid.COPY, "#a9b7c6", 11));
        copyBtn.getStyleClass().add("diagram-ddl-action-btn");
        copyBtn.setOnAction(e -> {
            String text = codeArea.getText();
            if (text != null && !text.isBlank()) {
                ClipboardContent content = new ClipboardContent();
                content.putString(text);
                Clipboard.getSystemClipboard().setContent(content);
                statusLabel.setText("DDL copied to clipboard!");
            }
        });

        Button openConsoleBtn = new Button("Open in Console");
        openConsoleBtn.setGraphic(Icons.of(FontAwesomeSolid.TERMINAL, "#a9b7c6", 11));
        openConsoleBtn.getStyleClass().add("diagram-ddl-action-btn");
        openConsoleBtn.setOnAction(e -> {
            MainWindow mw = MainWindow.getInstance();
            if (mw != null) {
                String ddl = codeArea.getText();
                mw.openQueryTab(profile, table.getCatalog(), ddl);
            }
        });

        HBox footer = new HBox(10, spinner, statusLabel, spacer, copyBtn, openConsoleBtn);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(6, 12, 6, 12));
        return footer;
    }

    private void loadDdl() {
        spinner.setVisible(true);
        statusLabel.setText("Generating DDL…");
        codeArea.setText("-- Loading DDL definition for " + table.getName() + "…");

        AppExecutor.run(() -> {
            try {
                String ddl = DdlGeneratorService.generateDdl(profile, table);
                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    codeArea.setText(ddl);
                    codeArea.positionCaret(0);
                    statusLabel.setText("-- auto-generated definition");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    codeArea.setText("-- Error generating DDL: " + ex.getMessage());
                    statusLabel.setText("Failed to generate DDL");
                });
            }
        });
    }
}
