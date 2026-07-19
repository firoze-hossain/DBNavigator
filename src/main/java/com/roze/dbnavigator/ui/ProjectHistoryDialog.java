package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.LocalHistoryStore;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Flat, list-only history browser spanning every console — used for
 * "Show Project History…" (everything) and "Recent Changes" (capped, most
 * recent). Double-clicking an entry opens the full {@link LocalHistoryDialog}
 * diff view for that entry's file, rather than duplicating diff rendering here.
 */
public final class ProjectHistoryDialog {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("d/M/yy, h:mm a");

    private ProjectHistoryDialog() {}

    public static void showProjectWide(Window owner, BiConsumer<String, LocalHistoryStore.Entry> onOpen) {
        show(owner, "Project History", LocalHistoryStore.allEntriesNewestFirst(), onOpen);
    }

    public static void showRecentChanges(Window owner, BiConsumer<String, LocalHistoryStore.Entry> onOpen) {
        List<Map.Entry<String, LocalHistoryStore.Entry>> all = LocalHistoryStore.allEntriesNewestFirst();
        show(owner, "Recent Changes", all.subList(0, Math.min(30, all.size())), onOpen);
    }

    private static void show(Window owner, String title,
                             List<Map.Entry<String, LocalHistoryStore.Entry>> items,
                             BiConsumer<String, LocalHistoryStore.Entry> onOpen) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(title);

        ListView<Map.Entry<String, LocalHistoryStore.Entry>> list = new ListView<>();
        list.getStyleClass().add("history-list");
        list.getItems().setAll(items);
        list.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Map.Entry<String, LocalHistoryStore.Entry> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                LocalHistoryStore.Entry entry = item.getValue();
                String changeTitle = entry.label() != null ? entry.label() : "Change";

                Label titleLabel = new Label(changeTitle);
                titleLabel.getStyleClass().add("history-entry-title");
                Label fileLabel = new Label(fileName(item.getKey()));
                fileLabel.getStyleClass().add("history-entry-file");
                Label timeLabel = new Label(Instant.ofEpochMilli(entry.timestamp())
                        .atZone(ZoneId.systemDefault()).format(TIMESTAMP_FORMAT));
                timeLabel.getStyleClass().add("history-entry-time");

                VBox box = new VBox(2, titleLabel, fileLabel, timeLabel);
                box.setPadding(new Insets(4, 6, 4, 6));
                setGraphic(box);
            }
        });

        if (items.isEmpty()) {
            list.setPlaceholder(new Label("No local history recorded yet"));
        }

        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                var selected = list.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    stage.close();
                    onOpen.accept(selected.getKey(), selected.getValue());
                }
            }
        });

        VBox root = new VBox(list);
        Scene scene = new Scene(root, 420, 480);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
        stage.show();
    }

    private static String fileName(String fileId) {
        return fileId.replace(' ', '_') + ".sql";
    }
}
