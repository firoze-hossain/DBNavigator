package com.roze.dbnavigator.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * DataGrip-style "Show Bookmarks…": every bookmarked line across every open
 * console, in one list. Double-click or Enter jumps to it, switching to
 * that console's tab first if needed.
 */
public final class BookmarksDialog {

    /** One bookmarked line, with everything needed to jump to it. */
    public record Entry(QueryTab console, String consoleTitle, int line, String preview) {}

    private BookmarksDialog() {}

    public static void show(MainWindow mainWindow, List<QueryTab> consoles) {
        List<Entry> entries = new ArrayList<>();
        for (QueryTab console : consoles) {
            for (int line : console.getBookmarkedLines()) {
                String preview = console.getLineText(line).strip();
                entries.add(new Entry(console, console.getText(), line, preview));
            }
        }

        Stage stage = new Stage();
        Window owner = mainWindow.getOwnerWindow();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Bookmarks");
        stage.setMinWidth(480);
        stage.setMinHeight(360);

        ListView<Entry> list = new ListView<>();
        list.getItems().setAll(entries);
        list.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Entry entry, boolean empty) {
                super.updateItem(entry, empty);
                if (empty || entry == null) {
                    setText(null);
                } else {
                    String text = entry.preview().isEmpty() ? "(blank line)" : entry.preview();
                    setText(entry.consoleTitle() + "  :  line " + (entry.line() + 1) + "  \u2014  " + text);
                }
            }
        });
        if (entries.isEmpty()) {
            list.setPlaceholder(new Label("No bookmarks yet \u2014 right-click a console tab, then "
                    + "Bookmarks \u2192 Toggle Bookmark on the line you want to mark."));
        }
        VBox.setVgrow(list, Priority.ALWAYS);

        Runnable jump = () -> {
            Entry selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            mainWindow.selectTab(selected.console());
            selected.console().jumpToLine(selected.line());
            stage.close();
        };
        list.setOnMouseClicked(e -> { if (e.getClickCount() == 2) jump.run(); });
        list.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) jump.run();
        });

        Button close = new Button("Close");
        close.setOnAction(e -> stage.close());
        Button jumpButton = new Button("Jump to Bookmark");
        jumpButton.getStyleClass().add("run-button");
        jumpButton.setDefaultButton(true);
        jumpButton.setDisable(entries.isEmpty());
        jumpButton.setOnAction(e -> jump.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10, spacer, jumpButton, close);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(10, 16, 12, 16));

        VBox root = new VBox(8, list, buttons);
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(12, 12, 0, 12));
        Scene scene = new Scene(root, 520, 400);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
        stage.show();
    }
}
