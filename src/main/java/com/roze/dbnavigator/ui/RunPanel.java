package com.roze.dbnavigator.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Docked "Run" tool window (bottom panel): one tab per execution (dump,
 * restore, or any future background command), each with its own live output
 * and a left icon rail (rerun, stop, wrap, scroll-to-end, save, clear, pin,
 * close) — mirrors DataGrip's Run tool window. Meant to sit in a vertical
 * SplitPane so the divider gives free mouse-drag resize.
 */
public class RunPanel extends BorderPane {

    private static final int MAX_UNPINNED_TABS = 10;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Handed to callers so they can drive one run's tab without seeing JavaFX internals. */
    public interface RunHandle {
        void appendLine(String line);
        void setProcess(Process process);
        void setRerunAction(Runnable rerun);
        void markFinished(int exitCode);
        void markFailed(String message);
    }

    private final TabPane tabs = new TabPane();
    private Runnable onMinimize;

    public RunPanel() {
        getStyleClass().add("run-panel");

        Label title = new Label("Run");
        title.getStyleClass().add("run-panel-title");

        tabs.getStyleClass().add("run-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        HBox.setHgrow(tabs, Priority.ALWAYS);

        Button minimizeButton = new Button();
        minimizeButton.setGraphic(Icons.of(FontAwesomeSolid.MINUS, "#a9b7c6", 11));
        minimizeButton.setTooltip(new Tooltip("Hide Run panel"));
        minimizeButton.getStyleClass().add("run-panel-chrome-button");
        minimizeButton.setOnAction(e -> { if (onMinimize != null) onMinimize.run(); });

        Button overflowButton = new Button();
        overflowButton.setGraphic(Icons.of(FontAwesomeSolid.ELLIPSIS_V, "#a9b7c6", 11));
        overflowButton.getStyleClass().add("run-panel-chrome-button");
        ContextMenu overflowMenu = new ContextMenu(menuItem("Close All Tabs", () -> tabs.getTabs().clear()));
        overflowButton.setOnAction(e -> overflowMenu.show(overflowButton, javafx.geometry.Side.BOTTOM, 0, 4));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(6, title, tabs, overflowButton, minimizeButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 8, 0, 10));
        header.getStyleClass().add("run-panel-header");

        setTop(header);
    }

    /** Wires the minimize ("—") button to whatever the host does to collapse this panel. */
    public void setOnMinimize(Runnable action) {
        this.onMinimize = action;
    }

    private static MenuItem menuItem(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(e -> action.run());
        return item;
    }

    /** Starts a new run tab (e.g. "pg_dump (postgres@localhost)") and selects it. */
    public RunHandle startRun(String title) {
        evictOldestUnpinnedIfFull();

        RunTabContent content = new RunTabContent();
        Tab tab = new Tab(title, content);
        tab.setGraphic(Icons.of(FontAwesomeSolid.PLAY, "#57965c", 10));
        content.owningTab = tab;

        tabs.getTabs().add(tab);
        tabs.getSelectionModel().select(tab);

        return content;
    }

    private void evictOldestUnpinnedIfFull() {
        if (tabs.getTabs().size() < MAX_UNPINNED_TABS) return;
        for (Tab t : tabs.getTabs()) {
            if (t.getContent() instanceof RunTabContent c && !c.pinned) {
                tabs.getTabs().remove(t);
                return;
            }
        }
    }

    // ------------------------------------------------------------ one tab

    /** Content of a single Run tab: left icon rail + live output area. */
    private static class RunTabContent extends HBox implements RunHandle {

        private final TextArea output = new TextArea();
        private final Button stopButton = new Button();
        private final Button rerunButton = new Button();
        private final Button pinButton = new Button();
        private boolean autoScroll = true;
        private boolean pinned = false;
        private Process liveProcess;
        private Runnable rerunAction;
        private Tab owningTab;

        RunTabContent() {
            getStyleClass().add("run-tab-content");

            output.setEditable(false);
            output.setWrapText(false);
            output.getStyleClass().add("process-output");
            HBox.setHgrow(output, Priority.ALWAYS);

            VBox rail = buildIconRail();
            getChildren().addAll(rail, new Separator(Orientation.VERTICAL), output);
        }

        private VBox buildIconRail() {
            rerunButton.setGraphic(Icons.of(FontAwesomeSolid.REDO, "#6897bb", 12));
            rerunButton.setTooltip(new Tooltip("Rerun"));
            rerunButton.setDisable(true);
            rerunButton.setOnAction(e -> { if (rerunAction != null) rerunAction.run(); });

            stopButton.setGraphic(Icons.of(FontAwesomeSolid.STOP_CIRCLE, "#e05555", 12));
            stopButton.setTooltip(new Tooltip("Stop"));
            stopButton.setDisable(true);
            stopButton.setOnAction(e -> {
                if (liveProcess != null && liveProcess.isAlive()) {
                    liveProcess.destroy();
                    appendLine("[cancelled by user]");
                }
            });

            Button wrapButton = new Button();
            wrapButton.setGraphic(Icons.of(FontAwesomeSolid.ALIGN_LEFT, "#a9b7c6", 12));
            wrapButton.setTooltip(new Tooltip("Soft-wrap output"));
            wrapButton.setOnAction(e -> output.setWrapText(!output.isWrapText()));

            Button scrollButton = new Button();
            scrollButton.setGraphic(Icons.of(FontAwesomeSolid.ARROW_DOWN, "#a9b7c6", 12));
            scrollButton.setTooltip(new Tooltip("Scroll to end / toggle auto-scroll"));
            scrollButton.setOnAction(e -> {
                autoScroll = !autoScroll;
                if (autoScroll) output.positionCaret(output.getText().length());
            });

            Button saveButton = new Button();
            saveButton.setGraphic(Icons.of(FontAwesomeSolid.PRINT, "#a9b7c6", 12));
            saveButton.setTooltip(new Tooltip("Print / save output to file"));
            saveButton.setOnAction(e -> saveOutputToFile());

            Button clearButton = new Button();
            clearButton.setGraphic(Icons.of(FontAwesomeSolid.TRASH, "#a9b7c6", 12));
            clearButton.setTooltip(new Tooltip("Clear output"));
            clearButton.setOnAction(e -> output.clear());

            pinButton.setGraphic(Icons.of(FontAwesomeSolid.THUMBTACK, "#a9b7c6", 12));
            pinButton.setTooltip(new Tooltip("Pin tab"));
            pinButton.setOnAction(e -> {
                pinned = !pinned;
                pinButton.setGraphic(Icons.of(FontAwesomeSolid.THUMBTACK,
                        pinned ? "#e0a44c" : "#a9b7c6", 12));
            });

            Button closeButton = new Button();
            closeButton.setGraphic(Icons.of(FontAwesomeSolid.TIMES, "#a9b7c6", 12));
            closeButton.setTooltip(new Tooltip("Close tab"));
            closeButton.setOnAction(e -> {
                if (owningTab != null && owningTab.getTabPane() != null) {
                    owningTab.getTabPane().getTabs().remove(owningTab);
                }
            });

            VBox rail = new VBox(4, rerunButton, stopButton, new Separator(),
                    wrapButton, scrollButton, saveButton, clearButton,
                    new Separator(), pinButton, closeButton);
            rail.setAlignment(Pos.TOP_CENTER);
            rail.setPadding(new Insets(6, 4, 6, 4));
            rail.getStyleClass().add("run-tab-rail");
            return rail;
        }

        private void saveOutputToFile() {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Run Output");
            chooser.setInitialFileName("output.txt");
            File file = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
            if (file == null) return;
            try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
                out.print(output.getText());
            } catch (Exception ignored) {
                // best-effort — a failed save isn't worth interrupting the user over
            }
        }

        @Override
        public void appendLine(String line) {
            Platform.runLater(() -> {
                output.appendText("[" + LocalDateTime.now().format(TIME_FORMAT) + "] " + line
                        + System.lineSeparator());
                if (autoScroll) output.positionCaret(output.getText().length());
            });
        }

        @Override
        public void setProcess(Process process) {
            Platform.runLater(() -> {
                this.liveProcess = process;
                stopButton.setDisable(process == null || !process.isAlive());
            });
        }

        @Override
        public void setRerunAction(Runnable rerun) {
            this.rerunAction = rerun;
            Platform.runLater(() -> rerunButton.setDisable(rerun == null));
        }

        @Override
        public void markFinished(int exitCode) {
            Platform.runLater(() -> {
                stopButton.setDisable(true);
                if (owningTab != null) {
                    owningTab.setGraphic(Icons.of(
                            exitCode == 0 ? FontAwesomeSolid.CHECK_CIRCLE : FontAwesomeSolid.EXCLAMATION_CIRCLE,
                            exitCode == 0 ? "#57965c" : "#e05555", 10));
                }
            });
        }

        @Override
        public void markFailed(String message) {
            appendLine("ERROR: " + message);
            markFinished(-1);
        }
    }
}
