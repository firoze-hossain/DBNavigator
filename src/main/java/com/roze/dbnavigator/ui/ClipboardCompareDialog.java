package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.util.TextDiff;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;

/**
 * "Compare with Clipboard": a focused two-pane diff (clipboard vs. editor
 * text/selection) reusing the same word-level {@link TextDiff} engine as
 * Local History, without the snapshot list panel — just the comparison.
 */
public final class ClipboardCompareDialog {

    private ClipboardCompareDialog() {}

    public static void show(Window owner, String clipboardText, String editorText) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Compare with Clipboard");
        stage.setMinWidth(820);
        stage.setMinHeight(520);

        List<TextDiff.DiffLine> diff = TextDiff.diffLines(clipboardText, editorText);
        long differences = TextDiff.countDifferences(diff);

        Label leftHeader = new Label("Clipboard");
        leftHeader.getStyleClass().add("diff-header-label");
        Label rightHeader = new Label("Editor");
        rightHeader.getStyleClass().add("diff-header-label");
        Label countLabel = new Label(differences + (differences == 1 ? " difference" : " differences"));
        countLabel.getStyleClass().add("console-status");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox headerRow = new HBox(leftHeader, new Separator(javafx.geometry.Orientation.VERTICAL),
                rightHeader, headerSpacer, countLabel);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(6, 12, 6, 12));
        headerRow.getStyleClass().add("diff-header-row");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("diff-grid");
        ColumnConstraints lineNoCol = new ColumnConstraints(46);
        ColumnConstraints contentCol = new ColumnConstraints();
        contentCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(lineNoCol, contentCol, lineNoCol, contentCol);

        int leftLineNo = 1, rightLineNo = 1;
        for (int r = 0; r < diff.size(); r++) {
            TextDiff.DiffLine line = diff.get(r);
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

            grid.add(leftNo, 0, r);
            grid.add(leftContent, 1, r);
            grid.add(rightNo, 2, r);
            grid.add(rightContent, 3, r);

            if (hasLeft) leftLineNo++;
            if (hasRight) rightLineNo++;
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("diff-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox root = new VBox(headerRow, scroll);
        root.getStyleClass().add("app-root");
        Scene scene = new Scene(root, 860, 560);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
        stage.show();
    }

    private static FlowPane renderSegments(List<TextDiff.Segment> segments, String rowStyle) {
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
}
