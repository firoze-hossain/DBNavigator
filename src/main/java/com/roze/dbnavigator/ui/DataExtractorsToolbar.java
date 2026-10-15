package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.QueryResult;
import com.roze.dbnavigator.util.AppExecutor;
import com.roze.dbnavigator.util.DataExporters;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.awt.Desktop;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Data viewer toolbar component providing Data Extractor selection (CSV, JSON, etc.),
 * Download/Export Data dialog launcher, and Copy to Clipboard action.
 * Matches the reference layout: [Extractor ▾] [Download] [Copy/Export].
 */
public class DataExtractorsToolbar extends HBox {

    private final ConnectionProfile.DatabaseType dbType;
    private final Supplier<QueryResult> dataSupplier;
    private final Supplier<String> sourceNameSupplier;
    private final Supplier<DbObject> tableSupplier;
    private final Supplier<ConnectionProfile> profileSupplier;
    private final Supplier<Window> windowSupplier;

    private DataExporters.Format activeFormat;
    private final MenuButton extractorButton = new MenuButton();
    private final Button downloadButton = new Button();
    private final Button copyButton = new Button();

    private final CheckMenuItem skipComputedCheck = new CheckMenuItem("Skip Computed Columns");
    private final CheckMenuItem skipGeneratedCheck = new CheckMenuItem("Skip Generated Columns");

    private final Map<DataExporters.Format, MenuItem> formatMenuItems = new HashMap<>();
    private final List<Consumer<DataExporters.Format>> formatListeners = new ArrayList<>();

    public DataExtractorsToolbar(ConnectionProfile.DatabaseType dbType,
                                 Supplier<QueryResult> dataSupplier,
                                 Supplier<String> sourceNameSupplier,
                                 Supplier<DbObject> tableSupplier,
                                 Supplier<ConnectionProfile> profileSupplier,
                                 Supplier<Window> windowSupplier) {
        this.dbType = dbType;
        this.dataSupplier = dataSupplier;
        this.sourceNameSupplier = sourceNameSupplier;
        this.tableSupplier = tableSupplier;
        this.profileSupplier = profileSupplier;
        this.windowSupplier = windowSupplier;

        // Relational databases default to CSV; MongoDB defaults to JSON
        this.activeFormat = (dbType == ConnectionProfile.DatabaseType.MONGODB)
                ? DataExporters.Format.JSON
                : DataExporters.Format.CSV;

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(4);

        buildControls();
        updateButtonLabelAndChecks();
    }

    public DataExporters.Format getActiveFormat() {
        return activeFormat;
    }

    public void setActiveFormat(DataExporters.Format format) {
        if (format == null) return;
        this.activeFormat = format;
        updateButtonLabelAndChecks();
        for (Consumer<DataExporters.Format> listener : formatListeners) {
            listener.accept(format);
        }
    }

    public void addFormatListener(Consumer<DataExporters.Format> listener) {
        if (listener != null) formatListeners.add(listener);
    }

    public boolean isSkipComputedColumns() {
        return skipComputedCheck.isSelected();
    }

    public boolean isSkipGeneratedColumns() {
        return skipGeneratedCheck.isSelected();
    }

    public MenuButton getExtractorButton() {
        return extractorButton;
    }

    public Button getDownloadButton() {
        return downloadButton;
    }

    public Button getCopyButton() {
        return copyButton;
    }

    private void buildControls() {
        // 1. Extractor Menu Button
        extractorButton.setStyle("-fx-background-color: transparent; -fx-text-fill: -text; -fx-font-size: 11px; -fx-cursor: hand;");
        extractorButton.getStyleClass().add("extractor-menu-button");
        buildExtractorMenu();

        // 2. Download Icon Button
        downloadButton.setGraphic(Icons.of(FontAwesomeSolid.DOWNLOAD, "#a9b7c6", 11));
        downloadButton.setTooltip(new Tooltip("Export Data... (Ctrl+Shift+E)"));
        downloadButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        downloadButton.setOnAction(e -> openExportDialog());

        // 3. Copy / Export Icon Button
        copyButton.setGraphic(Icons.of(FontAwesomeSolid.SHARE_SQUARE, "#a9b7c6", 11));
        copyButton.setTooltip(new Tooltip("Copy Data to Clipboard using " + activeFormat));
        copyButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        copyButton.setOnAction(e -> copyDataToClipboard());

        getChildren().addAll(extractorButton, downloadButton, copyButton);
    }

    private void buildExtractorMenu() {
        extractorButton.getItems().clear();
        formatMenuItems.clear();

        // Title
        MenuItem titleItem = new MenuItem("Data Extractors");
        titleItem.setDisable(true);
        titleItem.setStyle("-fx-font-weight: bold; -fx-opacity: 0.9; -fx-text-fill: -text-dim;");
        extractorButton.getItems().add(titleItem);

        // Section 1: Built-in
        MenuItem builtInHeader = new MenuItem("Built-in");
        builtInHeader.setDisable(true);
        builtInHeader.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-dim; -fx-padding: 4 6 2 6;");
        extractorButton.getItems().add(builtInHeader);

        addFormatItem(DataExporters.Format.SQL_INSERTS);
        addFormatItem(DataExporters.Format.SQL_UPDATES);
        addFormatItem(DataExporters.Format.WHERE_CLAUSE);

        extractorButton.getItems().add(new SeparatorMenuItem());

        skipComputedCheck.setSelected(true);
        skipGeneratedCheck.setSelected(false);
        extractorButton.getItems().addAll(skipComputedCheck, skipGeneratedCheck);

        extractorButton.getItems().add(new SeparatorMenuItem());

        // Section 2: CSV
        MenuItem csvHeader = new MenuItem("CSV");
        csvHeader.setDisable(true);
        csvHeader.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-dim; -fx-padding: 4 6 2 6;");
        extractorButton.getItems().add(csvHeader);

        addFormatItem(DataExporters.Format.CSV);
        addFormatItem(DataExporters.Format.TSV);
        addFormatItem(DataExporters.Format.PIPE);
        addFormatItem(DataExporters.Format.SEMICOLON);

        MenuItem configCsvItem = new MenuItem("Configure CSV Formats...");
        configCsvItem.setGraphic(Icons.of(FontAwesomeSolid.SLIDERS_H, "#a9b7c6", 11));
        configCsvItem.setOnAction(e -> handleConfigureCsvFormats());
        extractorButton.getItems().add(configCsvItem);

        extractorButton.getItems().add(new SeparatorMenuItem());

        // Section 3: Scripted
        MenuItem scriptedHeader = new MenuItem("Scripted");
        scriptedHeader.setDisable(true);
        scriptedHeader.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-dim; -fx-padding: 4 6 2 6;");
        extractorButton.getItems().add(scriptedHeader);

        addFormatItem(DataExporters.Format.HTML_GROOVY);
        addFormatItem(DataExporters.Format.HTML_JS);
        addFormatItem(DataExporters.Format.JSON);
        addFormatItem(DataExporters.Format.MARKDOWN);
        addFormatItem(DataExporters.Format.ONE_ROW);
        addFormatItem(DataExporters.Format.PRETTY);
        addFormatItem(DataExporters.Format.PYTHON_DATAFRAME);
        addFormatItem(DataExporters.Format.SQL_INSERT_MULTIROW);
        addFormatItem(DataExporters.Format.SQL_INSERT_STATEMENTS);
        addFormatItem(DataExporters.Format.XML);

        extractorButton.getItems().add(new SeparatorMenuItem());

        MenuItem scriptsDirItem = new MenuItem("Go to Scripts Directory");
        scriptsDirItem.setGraphic(Icons.of(FontAwesomeSolid.FOLDER, "#e0a44c", 11));
        scriptsDirItem.setOnAction(e -> handleGoToScriptsDirectory());
        extractorButton.getItems().add(scriptsDirItem);

        extractorButton.getItems().add(new SeparatorMenuItem());

        // Footer note about binary extractors
        MenuItem footerNote = new MenuItem("Binary extractors (like Excel XLSX)\nare available in \"Export Data\" action");
        footerNote.setDisable(true);
        footerNote.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-dim; -fx-font-style: italic;");
        extractorButton.getItems().add(footerNote);
    }

    private void addFormatItem(DataExporters.Format format) {
        MenuItem item = new MenuItem(format.toString());
        item.setOnAction(e -> setActiveFormat(format));
        formatMenuItems.put(format, item);
        extractorButton.getItems().add(item);
    }

    private void updateButtonLabelAndChecks() {
        extractorButton.setText(activeFormat != null ? activeFormat.toString() : "CSV");
        copyButton.setTooltip(new Tooltip("Copy Data to Clipboard using " + activeFormat));

        for (Map.Entry<DataExporters.Format, MenuItem> entry : formatMenuItems.entrySet()) {
            MenuItem item = entry.getValue();
            boolean isCurrent = (entry.getKey() == activeFormat);
            if (isCurrent) {
                item.setGraphic(Icons.of(FontAwesomeSolid.CHECK, "#3574F0", 11));
            } else {
                item.setGraphic(null);
            }
        }
    }

    public void openExportDialog() {
        QueryResult result = (dataSupplier != null) ? dataSupplier.get() : null;
        if (result == null) result = new QueryResult();
        Window owner = (windowSupplier != null) ? windowSupplier.get() : null;
        if (owner == null && getScene() != null) owner = getScene().getWindow();

        DbObject table = (tableSupplier != null) ? tableSupplier.get() : null;
        ConnectionProfile profile = (profileSupplier != null) ? profileSupplier.get() : null;
        String sourceName = (sourceNameSupplier != null) ? sourceNameSupplier.get() : null;

        if (table != null && profile != null) {
            ExportDataDialog.show(owner, profile, table, result, activeFormat);
        } else {
            ExportDataDialog.show(owner, sourceName != null ? sourceName : "export", result, dbType, activeFormat);
        }
    }

    public void copyDataToClipboard() {
        QueryResult result = (dataSupplier != null) ? dataSupplier.get() : null;
        if (result == null || result.getRows().isEmpty()) return;

        if (DataExporters.isBinary(activeFormat)) {
            DialogTheme.apply(new Alert(Alert.AlertType.WARNING,
                    "Excel export can't be copied as text — use Export to File.")).showAndWait();
            return;
        }

        String sourceName = (sourceNameSupplier != null) ? sourceNameSupplier.get() : "table";
        DataExporters.ExportOptions options = new DataExporters.ExportOptions();
        options.setSkipComputedColumns(skipComputedCheck.isSelected());
        options.setSkipGeneratedColumns(skipGeneratedCheck.isSelected());

        String text = DataExporters.render(activeFormat, result, sourceName, result.getRows().size(), options);
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);

        // Feedback in tooltip
        Tooltip tooltip = new Tooltip("Copied " + result.getRows().size() + " row(s) as " + activeFormat);
        Tooltip.install(copyButton, tooltip);
        Window win = getScene() != null ? getScene().getWindow() : null;
        if (win != null) {
            var bounds = copyButton.localToScreen(copyButton.getBoundsInLocal());
            if (bounds != null) {
                tooltip.show(win, bounds.getMinX(), bounds.getMaxY() + 4);
                AppExecutor.run(() -> {
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(tooltip::hide);
                });
            }
        }
    }

    private void handleConfigureCsvFormats() {
        MainWindow mw = MainWindow.getInstance();
        if (mw != null) {
            SettingsDialog.show(mw, "CSV Formats");
        }
    }

    private void handleGoToScriptsDirectory() {
        try {
            File scriptsDir = new File(System.getProperty("user.home"), ".dbnavigator/scripts");
            if (!scriptsDir.exists()) scriptsDir.mkdirs();
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(scriptsDir);
            }
        } catch (Exception ignore) {}
    }
}
