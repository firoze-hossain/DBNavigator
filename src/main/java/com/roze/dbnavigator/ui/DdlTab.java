package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.DdlGeneratorService;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/**
 * Tab displaying auto-generated DDL for a database object, matching DataGrip's DDL editor view.
 */
public class DdlTab extends Tab {

    private final MainWindow mainWindow;
    private final ConnectionProfile profile;
    private final DbObject object;
    private final CodeArea editor;
    private final Button copyButton;
    private final Button openConsoleButton;
    private final Label statusLabel;

    public DdlTab(MainWindow mainWindow, ConnectionProfile profile, DbObject object) {
        this.mainWindow = mainWindow;
        this.profile = profile;
        this.object = object;

        setText(object.getName() + " (DDL) [" + profile.getName() + "]");
        setGraphic(Icons.of(FontAwesomeSolid.FILE_CODE, "#c77dbb", 11));
        setTooltip(new Tooltip(profile.getName()
                + (object.getCatalog() != null ? " ▸ " + object.getCatalog() : "")
                + (object.getSchema() != null ? " ▸ " + object.getSchema() : "")
                + " ▸ " + object.getName() + " (DDL)"));

        this.editor = SqlHighlighter.createEditor();
        this.editor.setEditable(true);

        this.copyButton = new Button("Copy DDL");
        this.copyButton.setGraphic(Icons.of(FontAwesomeSolid.COPY, "#6897bb", 11));
        this.copyButton.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(editor.getText());
            Clipboard.getSystemClipboard().setContent(content);
            copyButton.setText("Copied ✓");
        });
        this.editor.plainTextChanges().subscribe(c -> copyButton.setText("Copy DDL"));

        this.openConsoleButton = new Button("Open in Console");
        this.openConsoleButton.setGraphic(Icons.of(FontAwesomeSolid.TERMINAL, "#57965c", 11));
        this.openConsoleButton.setOnAction(e -> {
            String catalog = object.getCatalog() != null ? object.getCatalog() : profile.getDatabase();
            mainWindow.openQueryTab(profile, catalog, editor.getText());
        });

        Button refreshButton = new Button();
        refreshButton.setGraphic(Icons.of(FontAwesomeSolid.SYNC_ALT, "#6897bb", 11));
        refreshButton.setTooltip(new Tooltip("Refresh DDL"));
        refreshButton.setOnAction(e -> loadDdlAsync());

        this.statusLabel = new Label("-- auto-generated definition");
        this.statusLabel.getStyleClass().add("console-status");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(8, copyButton, openConsoleButton, refreshButton, spacer, statusLabel);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.getStyleClass().add("console-toolbar");

        VirtualizedScrollPane<CodeArea> scroll = new VirtualizedScrollPane<>(editor);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(toolbar, scroll);
        VBox.setVgrow(content, Priority.ALWAYS);
        setContent(content);

        loadDdlAsync();
    }

    public ConnectionProfile getProfile() {
        return profile;
    }

    public DbObject getObject() {
        return object;
    }

    public CodeArea getEditor() {
        return editor;
    }

    public void loadDdlAsync() {
        statusLabel.setText("Generating DDL…");
        AppExecutor.run(() -> {
            try {
                String ddl = DdlGeneratorService.generateDdl(profile, object);
                Platform.runLater(() -> {
                    editor.replaceText(ddl);
                    statusLabel.setText("-- auto-generated definition");
                    mainWindow.setStatus("Generated DDL for " + object.getName());
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
                Platform.runLater(() -> {
                    editor.replaceText("-- Error generating DDL: " + msg);
                    statusLabel.setText("Error generating DDL");
                });
            }
        });
    }
}
