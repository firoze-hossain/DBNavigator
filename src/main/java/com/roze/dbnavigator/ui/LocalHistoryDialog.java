package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.LocalHistoryStore;
import com.roze.dbnavigator.util.TextDiff;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

/**
 * DataGrip-style "Local History" viewer: a list of auto-captured snapshots
 * on the left, and a side-by-side diff (word-level highlighting, difference
 * count) of the selected snapshot against the current content on the right.
 *
 * Honest simplification vs. the reference IDE: the toolbar's "Side-by-side
 * viewer / Do not ignore / Highlight words" controls are shown for visual
 * parity but aren't independently switchable yet — this dialog always runs
 * in side-by-side, word-highlighted mode. "Current" is captured once, at the
 * moment the dialog opens (not live-updating while it stays open).
 */
public final class LocalHistoryDialog {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("d/M/yy, h:mm a");

    private final Stage stage = new Stage();
    private final ListView<LocalHistoryStore.Entry> entryList = new ListView<>();
    private final Label beforeHeader = new Label();
    private final Label differenceCountLabel = new Label();
    private final GridPane diffGrid = new GridPane();
    private final ScrollPane diffScroll = new ScrollPane(diffGrid);

    private final String fileId;
    private final String displayName;
    private final String currentContent;
    private List<LocalHistoryStore.Entry> entries;
    private List<TextDiff.DiffLine> currentDiff = List.of();

    private LocalHistoryDialog(Window owner, String fileId, String displayName, String currentContent) {
        this.fileId = fileId;
        this.displayName = displayName;
        this.currentContent = currentContent;
        buildUi(owner);
        loadEntries();
    }

    /** Full diff viewer for one file's history, pre-selecting its newest entry. */
    public static void showForFile(Window owner, String fileId, String displayName,
                                   Supplier<String> currentContentSupplier) {
        new LocalHistoryDialog(owner, fileId, displayName, currentContentSupplier.get()).stage.showAndWait();
    }

    /** Same viewer, but with a specific entry pre-selected (used from project-wide/recent lists). */
    public static void showForFileAtEntry(Window owner, String fileId, String displayName,
                                          String currentContent, LocalHistoryStore.Entry toSelect) {
        LocalHistoryDialog dialog = new LocalHistoryDialog(owner, fileId, displayName, currentContent);
        for (LocalHistoryStore.Entry e : dialog.entries) {
            if (e.timestamp() == toSelect.timestamp()) {
                dialog.entryList.getSelectionModel().select(e);
                break;
            }
        }
        dialog.stage.showAndWait();
    }

    // --------------------------------------------------------------- UI

    private void buildUi(Window owner) {
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Local History: " + displayName);
        stage.setMinWidth(900);
        stage.setMinHeight(560);

        entryList.setPrefWidth(230);
        entryList.getStyleClass().add("history-list");
        entryList.setCellFactory(list -> new HistoryCell());
        entryList.getSelectionModel().selectedItemProperty().addListener((obs, was, entry) -> {
            if (entry != null) refreshDiff(entry);
        });

        // ---- diff toolbar (visual parity with the reference; see class javadoc) ----
        Button upButton = new Button();
        upButton.setGraphic(Icons.of(FontAwesomeSolid.CHEVRON_UP, "#a9b7c6", 11));
        upButton.setTooltip(new Tooltip("Previous difference"));
        upButton.setOnAction(e -> jumpToDifference(-1));
        Button downButton = new Button();
        downButton.setGraphic(Icons.of(FontAwesomeSolid.CHEVRON_DOWN, "#a9b7c6", 11));
        downButton.setTooltip(new Tooltip("Next difference"));
        downButton.setOnAction(e -> jumpToDifference(1));

        Label sideBySideLabel = pillLabel("Side-by-side viewer");
        Label ignoreLabel = pillLabel("Do not ignore");
        Label highlightLabel = pillLabel("Highlight words");

        Region toolbarSpacer = new Region();
        HBox.setHgrow(toolbarSpacer, Priority.ALWAYS);
        differenceCountLabel.getStyleClass().add("console-status");

        HBox diffToolbar = new HBox(6, upButton, downButton, sideBySideLabel, ignoreLabel,
                highlightLabel, toolbarSpacer, differenceCountLabel);
        diffToolbar.setAlignment(Pos.CENTER_LEFT);
        diffToolbar.setPadding(new Insets(6, 12, 6, 12));
        diffToolbar.getStyleClass().add("console-toolbar");

        beforeHeader.getStyleClass().add("diff-header-label");
        Label currentHeader = new Label("Current");
        currentHeader.getStyleClass().add("diff-header-label");
        HBox leftHeaderBox = new HBox(beforeHeader);
        leftHeaderBox.setAlignment(Pos.CENTER_LEFT);
        leftHeaderBox.setPadding(new Insets(4, 0, 4, 12));
        HBox.setHgrow(leftHeaderBox, Priority.ALWAYS);
        HBox rightHeaderBox = new HBox(currentHeader);
        rightHeaderBox.setAlignment(Pos.CENTER_LEFT);
        rightHeaderBox.setPadding(new Insets(4, 0, 4, 12));
        HBox.setHgrow(rightHeaderBox, Priority.ALWAYS);
        HBox headerRow = new HBox(leftHeaderBox, new Separator(javafx.geometry.Orientation.VERTICAL), rightHeaderBox);
        headerRow.getStyleClass().add("diff-header-row");

        diffGrid.getStyleClass().add("diff-grid");
        diffScroll.setFitToWidth(true);
        diffScroll.getStyleClass().add("diff-scroll");

        VBox rightSide = new VBox(diffToolbar, headerRow, diffScroll);
        VBox.setVgrow(diffScroll, Priority.ALWAYS);

        SplitPane split = new SplitPane(entryList, rightSide);
        split.setDividerPositions(0.24);
        SplitPane.setResizableWithParent(entryList, false);

        VBox root = new VBox(split);
        VBox.setVgrow(split, Priority.ALWAYS);

        Scene scene = new Scene(root, 960, 600);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
    }

    private static Label pillLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("diff-toolbar-pill");
        return l;
    }

    private void loadEntries() {
        entries = LocalHistoryStore.forFile(fileId);
        entryList.getItems().setAll(entries);
        if (!entries.isEmpty()) entryList.getSelectionModel().selectFirst();
    }

    // ------------------------------------------------------------ list cell

    private class HistoryCell extends ListCell<LocalHistoryStore.Entry> {
        @Override
        protected void updateItem(LocalHistoryStore.Entry entry, boolean empty) {
            super.updateItem(entry, empty);
            if (empty || entry == null) { setGraphic(null); return; }

            boolean isOldest = entries.indexOf(entry) == entries.size() - 1;
            String title = entry.label() != null ? entry.label()
                    : isOldest ? "Create " + displayName
                    : "Change";

            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("history-entry-title");
            Label timeLabel = new Label(format(entry.timestamp()));
            timeLabel.getStyleClass().add("history-entry-time");

            VBox box = new VBox(2, titleLabel, timeLabel);
            box.setPadding(new Insets(4, 6, 4, 6));
            setGraphic(box);
        }
    }

    private static String format(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(TIMESTAMP_FORMAT);
    }

    // ------------------------------------------------------------ diff render

    private void refreshDiff(LocalHistoryStore.Entry entry) {
        beforeHeader.setText("Before " + format(entry.timestamp()));
        currentDiff = TextDiff.diffLines(entry.content(), currentContent);
        long diffs = TextDiff.countDifferences(currentDiff);
        differenceCountLabel.setText(diffs + (diffs == 1 ? " difference" : " differences"));

        diffGrid.getChildren().clear();
        diffGrid.getColumnConstraints().clear();
        ColumnConstraints lineNoCol = new ColumnConstraints(46);
        ColumnConstraints contentCol = new ColumnConstraints();
        contentCol.setHgrow(Priority.ALWAYS);
        diffGrid.getColumnConstraints().addAll(lineNoCol, contentCol, lineNoCol, contentCol);

        int leftLineNo = 1, rightLineNo = 1;
        for (int r = 0; r < currentDiff.size(); r++) {
            TextDiff.DiffLine line = currentDiff.get(r);
            String rowStyle = switch (line.kind()) {
                case CHANGED -> "diff-row-changed";
                case ADDED -> "diff-row-added";
                case REMOVED -> "diff-row-removed";
                case EQUAL -> "diff-row-equal";
            };

            boolean hasLeft = line.kind() != TextDiff.LineKind.ADDED;
            boolean hasRight = line.kind() != TextDiff.LineKind.REMOVED;

            Label leftNo = new Label(hasLeft ? String.valueOf(leftLineNo) : "");
            leftNo.getStyleClass().addAll("diff-lineno", rowStyle);
            leftNo.setMaxWidth(Double.MAX_VALUE);
            FlowPane leftContent = renderSegments(hasLeft ? line.left() : null, rowStyle);
            Label rightNo = new Label(hasRight ? String.valueOf(rightLineNo) : "");
            rightNo.getStyleClass().addAll("diff-lineno", rowStyle);
            rightNo.setMaxWidth(Double.MAX_VALUE);
            FlowPane rightContent = renderSegments(hasRight ? line.right() : null, rowStyle);

            diffGrid.add(leftNo, 0, r);
            diffGrid.add(leftContent, 1, r);
            diffGrid.add(rightNo, 2, r);
            diffGrid.add(rightContent, 3, r);

            if (hasLeft) leftLineNo++;
            if (hasRight) rightLineNo++;
        }
    }

    private FlowPane renderSegments(List<TextDiff.Segment> segments, String rowStyle) {
        FlowPane pane = new FlowPane();
        pane.getStyleClass().addAll("diff-content", rowStyle);
        if (segments == null) return pane;
        for (TextDiff.Segment seg : segments) {
            Label word = new Label(seg.text().equals(" ") ? "\u00A0" : seg.text());
            word.getStyleClass().add("diff-segment");
            if (seg.changed()) word.getStyleClass().add("diff-segment-changed");
            pane.getChildren().add(word);
        }
        return pane;
    }

    private void jumpToDifference(int direction) {
        boolean anyDiff = currentDiff.stream().anyMatch(l -> l.kind() != TextDiff.LineKind.EQUAL);
        if (!anyDiff) return;
        // Approximate scroll position — a pixel-exact "scroll this row into view" would
        // need per-row layout bookkeeping; proportional scroll is a reasonable stand-in.
        double target = direction > 0
                ? Math.min(1.0, diffScroll.getVvalue() + 0.15)
                : Math.max(0.0, diffScroll.getVvalue() - 0.15);
        diffScroll.setVvalue(target);
    }
}
