package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.QueryResult;
import com.roze.dbnavigator.util.AppExecutor;
import com.roze.dbnavigator.util.DataExporters;
import com.roze.dbnavigator.util.XlsxWriter;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern "Export Data" dialog: pick an extractor format, configure transposition
 * and headers, see a live line-numbered preview built from the actual query result,
 * then copy to clipboard or export directly to a file.
 */
public final class ExportDataDialog {

    private final Stage stage = new Stage();
    private final ConnectionProfile profile;
    private final DbObject table;
    private final QueryResult result;
    private final String customSourceName;
    private final ConnectionProfile.DatabaseType dbType;

    private final ComboBox<DataExporters.Format> extractorCombo = new ComboBox<>();
    private final CheckBox transposeCheck = new CheckBox("Transpose");
    private final CheckBox addColumnHeaderCheck = new CheckBox("Add column header");
    private final CheckBox addRowHeaderCheck = new CheckBox("Add row header");
    private final TextField sourceField = new TextField();
    private final TextField outputFileField = new TextField();

    private final TextArea lineNumbersGutter = new TextArea();
    private final TextArea previewArea = new TextArea();
    private final Label rowCountLabel = new Label();

    private ExportDataDialog(Window owner, ConnectionProfile profile, DbObject table, QueryResult result,
                             String customSourceName, ConnectionProfile.DatabaseType dbType,
                             DataExporters.Format initialFormat) {
        this.profile = profile;
        this.table = table;
        this.result = (result != null) ? result : new QueryResult();
        this.customSourceName = customSourceName;
        this.dbType = dbType != null ? dbType : (profile != null ? profile.getType() : ConnectionProfile.DatabaseType.POSTGRESQL);

        DataExporters.Format defaultFmt = initialFormat != null
                ? initialFormat
                : (this.dbType == ConnectionProfile.DatabaseType.MONGODB ? DataExporters.Format.JSON : DataExporters.Format.CSV);

        buildUi(owner, defaultFmt);
        refreshPreview();
    }

    public static void show(Window owner, ConnectionProfile profile, DbObject table, QueryResult result) {
        show(owner, profile, table, result, null);
    }

    public static void show(Window owner, ConnectionProfile profile, DbObject table, QueryResult result,
                            DataExporters.Format initialFormat) {
        new ExportDataDialog(owner, profile, table, result, null,
                profile != null ? profile.getType() : null, initialFormat).stage.showAndWait();
    }

    public static void show(Window owner, String qualifiedSourceName, QueryResult result,
                            ConnectionProfile.DatabaseType dbType, DataExporters.Format initialFormat) {
        new ExportDataDialog(owner, null, null, result, qualifiedSourceName, dbType, initialFormat).stage.showAndWait();
    }

    private void buildUi(Window owner, DataExporters.Format defaultFormat) {
        if (owner != null) stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Export Data");
        stage.setMinWidth(760);
        stage.setMinHeight(480);
        stage.setWidth(820);
        stage.setHeight(520);

        // ---------------- Left Column (Options & Output) ----------------
        Label sourceLabel = new Label("Source:");
        sourceLabel.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px;");
        sourceField.setText(qualifiedDisplayName());
        sourceField.setEditable(false);
        sourceField.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4px; -fx-background-radius: 4px;");
        sourceField.setPrefHeight(30);

        Label extractorLabel = new Label("Extractor:");
        extractorLabel.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px;");
        extractorCombo.getItems().addAll(DataExporters.Format.values());
        extractorCombo.setValue(defaultFormat != null ? defaultFormat : DataExporters.Format.CSV);
        extractorCombo.setMaxWidth(Double.MAX_VALUE);
        extractorCombo.setPrefHeight(30);
        extractorCombo.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #3574F0; -fx-border-radius: 4px; -fx-background-radius: 4px;");

        // Checkboxes
        transposeCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 12px;");
        addColumnHeaderCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 12px;");
        addColumnHeaderCheck.setSelected(true);
        addRowHeaderCheck.setStyle("-fx-text-fill: -text; -fx-font-size: 12px;");
        addRowHeaderCheck.setSelected(false);

        VBox checksBox = new VBox(8, transposeCheck, addColumnHeaderCheck, addRowHeaderCheck);
        checksBox.setPadding(new Insets(6, 0, 6, 0));

        // Output file
        Label outputLabel = new Label("Output file:");
        outputLabel.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px;");
        outputFileField.setText(buildDefaultOutputPath(extractorCombo.getValue()));
        outputFileField.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; -fx-border-radius: 4px; -fx-background-radius: 4px;");
        outputFileField.setPrefHeight(30);
        HBox.setHgrow(outputFileField, Priority.ALWAYS);

        Button browseButton = new Button();
        browseButton.setGraphic(Icons.of(FontAwesomeSolid.FOLDER_OPEN, "#a9b7c6", 12));
        browseButton.setTooltip(new Tooltip("Browse output file location"));
        browseButton.setPrefHeight(30);
        browseButton.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #393b40; -fx-border-radius: 4px; -fx-cursor: hand;");
        browseButton.setOnAction(e -> handleBrowseFile());

        HBox outputBox = new HBox(6, outputFileField, browseButton);
        outputBox.setAlignment(Pos.CENTER_LEFT);

        extractorCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateOutputFileExtension(newVal);
            refreshPreview();
        });
        transposeCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview());
        addColumnHeaderCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview());
        addRowHeaderCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview());

        VBox leftColumn = new VBox(12,
                sourceLabel, sourceField,
                extractorLabel, extractorCombo,
                checksBox,
                outputLabel, outputBox);
        leftColumn.setPrefWidth(280);
        leftColumn.setMinWidth(260);
        leftColumn.setPadding(new Insets(16));

        // ---------------- Right Column (Preview with Line Numbers) ----------------
        Label previewLabel = new Label("Export preview:");
        previewLabel.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 12px;");

        // Line numbers gutter
        lineNumbersGutter.setEditable(false);
        lineNumbersGutter.setFocusTraversable(false);
        lineNumbersGutter.setMouseTransparent(true);
        lineNumbersGutter.setWrapText(false);
        lineNumbersGutter.setPrefWidth(38);
        lineNumbersGutter.setMinWidth(38);
        lineNumbersGutter.setMaxWidth(48);
        lineNumbersGutter.setStyle("-fx-background-color: #1e1f22; -fx-control-inner-background: #1e1f22; -fx-text-fill: #6e7681; "
                + "-fx-font-family: monospace; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 2 4 2 4;");

        previewArea.setEditable(false);
        previewArea.setWrapText(false);
        previewArea.setStyle("-fx-background-color: #1e1f22; -fx-control-inner-background: #1e1f22; -fx-text-fill: #bcbec4; "
                + "-fx-font-family: monospace; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 2 4 2 4;");
        HBox.setHgrow(previewArea, Priority.ALWAYS);
        VBox.setVgrow(previewArea, Priority.ALWAYS);

        // Bind scrolling between gutter and preview area
        lineNumbersGutter.scrollTopProperty().bindBidirectional(previewArea.scrollTopProperty());

        HBox editorBox = new HBox(lineNumbersGutter, previewArea);
        editorBox.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4px; -fx-background-radius: 4px;");
        VBox.setVgrow(editorBox, Priority.ALWAYS);

        rowCountLabel.setStyle("-fx-text-fill: -text-dim; -fx-font-size: 11px;");
        VBox rightColumn = new VBox(6, previewLabel, editorBox, rowCountLabel);
        rightColumn.setPadding(new Insets(16));
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        HBox bodyBox = new HBox(leftColumn, new Separator(javafx.geometry.Orientation.VERTICAL), rightColumn);
        VBox.setVgrow(bodyBox, Priority.ALWAYS);

        // ---------------- Bottom Bar ----------------
        Button helpButton = new Button("?");
        helpButton.setStyle("-fx-background-radius: 12px; -fx-min-width: 24px; -fx-min-height: 24px; "
                + "-fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; "
                + "-fx-background-color: #2b2d30; -fx-text-fill: -text-dim; -fx-border-color: #393b40; -fx-border-radius: 12px;");
        helpButton.setTooltip(new Tooltip("Data Export:\n"
                + "• Select format from the Extractor dropdown.\n"
                + "• Transpose switches rows and columns.\n"
                + "• Live preview updates instantly.\n"
                + "• Binary formats (Excel XLSX) can be exported directly to file."));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button exportToFileBtn = new Button("Export to File");
        exportToFileBtn.setDefaultButton(true);
        exportToFileBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-padding: 6 16 6 16; -fx-background-radius: 4px; -fx-cursor: hand;");
        exportToFileBtn.setOnAction(e -> handleExportToFile());

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; "
                + "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> stage.close());

        Button copyToClipboardBtn = new Button("Copy to Clipboard");
        copyToClipboardBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: -text; -fx-border-color: #393b40; "
                + "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
        copyToClipboardBtn.setOnAction(e -> handleCopyToClipboard());

        HBox buttons = new HBox(10, helpButton, spacer, exportToFileBtn, cancelBtn, copyToClipboardBtn);
        buttons.setPadding(new Insets(10, 16, 16, 16));
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(bodyBox, buttons);
        root.setStyle("-fx-background-color: #1e1f22;");
        root.getStyleClass().add("app-root");

        Scene scene = new Scene(root, 820, 520);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
    }

    private String baseName() {
        if (table != null && table.getName() != null && !table.getName().isBlank()) {
            return table.getName();
        }
        if (customSourceName != null && !customSourceName.isBlank()) {
            int lastDot = customSourceName.lastIndexOf('.');
            return lastDot >= 0 ? customSourceName.substring(lastDot + 1) : customSourceName;
        }
        return "export";
    }

    private String qualifiedDisplayName() {
        if (customSourceName != null && !customSourceName.isBlank()) return customSourceName;
        if (table == null) return profile != null ? profile.getName() : "export";
        StringBuilder sb = new StringBuilder();
        if (table.getCatalog() != null && !table.getCatalog().isBlank()) sb.append(table.getCatalog()).append('.');
        if (table.getSchema() != null && !table.getSchema().isBlank()) sb.append(table.getSchema()).append('.');
        sb.append(table.getName());
        return sb.toString();
    }

    private String buildDefaultOutputPath(DataExporters.Format format) {
        String home = System.getProperty("user.home", ".");
        String ext = DataExporters.defaultExtension(format);
        return new File(home, baseName() + ext).getAbsolutePath();
    }

    private void updateOutputFileExtension(DataExporters.Format newFormat) {
        String currentPath = outputFileField.getText().trim();
        String newExt = DataExporters.defaultExtension(newFormat);
        if (currentPath.isEmpty()) {
            outputFileField.setText(buildDefaultOutputPath(newFormat));
            return;
        }
        int lastDot = currentPath.lastIndexOf('.');
        int lastSep = Math.max(currentPath.lastIndexOf('/'), currentPath.lastIndexOf('\\'));
        if (lastDot > lastSep) {
            outputFileField.setText(currentPath.substring(0, lastDot) + newExt);
        } else {
            outputFileField.setText(currentPath + newExt);
        }
    }

    private void handleBrowseFile() {
        DataExporters.Format format = extractorCombo.getValue();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Output File");

        String currentPath = outputFileField.getText().trim();
        if (!currentPath.isEmpty()) {
            File currentFile = new File(currentPath);
            File parent = currentFile.getParentFile();
            if (parent != null && parent.isDirectory()) {
                chooser.setInitialDirectory(parent);
            }
            chooser.setInitialFileName(currentFile.getName());
        } else {
            chooser.setInitialFileName(baseName() + DataExporters.defaultExtension(format));
        }

        String ext = DataExporters.defaultExtension(format).replace(".", "");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(format.toString() + " (*." + ext + ")", "*." + ext));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files (*.*)", "*.*"));

        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            outputFileField.setText(file.getAbsolutePath());
        }
    }

    private DataExporters.ExportOptions buildOptions() {
        DataExporters.ExportOptions opts = new DataExporters.ExportOptions();
        opts.setTranspose(transposeCheck.isSelected());
        opts.setAddColumnHeader(addColumnHeaderCheck.isSelected());
        opts.setAddRowHeader(addRowHeaderCheck.isSelected());
        return opts;
    }

    private void refreshPreview() {
        DataExporters.Format format = extractorCombo.getValue();
        DataExporters.ExportOptions options = buildOptions();
        String qualifiedTable = qualifiedDisplayName();

        int totalCount = result.getRows().size();
        int previewLimit = Math.min(50, totalCount);

        String previewText;
        if (DataExporters.isBinary(format)) {
            previewText = DataExporters.render(format, result, qualifiedTable, previewLimit, options);
            previewArea.setText(previewText);
            rowCountLabel.setText(totalCount + " row(s) total — Excel XLSX binary export");
        } else {
            QueryResult slice = sliced(previewLimit);
            previewText = DataExporters.render(format, slice, qualifiedTable, previewLimit, options);
            previewArea.setText(previewText);
            rowCountLabel.setText(totalCount + " row(s) total"
                    + (totalCount > previewLimit ? " — preview shows first " + previewLimit : ""));
        }

        updateLineNumbersGutter(previewText);
    }

    private void updateLineNumbersGutter(String text) {
        if (text == null || text.isEmpty()) {
            lineNumbersGutter.setText("1\n");
            return;
        }
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') lines++;
        }
        // Match the reference image where an extra line number is shown at the end
        lines = Math.max(lines, 4);

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) {
            sb.append(i).append('\n');
        }
        lineNumbersGutter.setText(sb.toString());
    }

    private QueryResult sliced(int limit) {
        if (limit >= result.getRows().size()) return result;
        QueryResult partial = new QueryResult();
        partial.getColumns().addAll(result.getColumns());
        partial.getColumnTypes().addAll(result.getColumnTypes());
        partial.getRows().addAll(result.getRows().subList(0, limit));
        return partial;
    }

    private void handleCopyToClipboard() {
        DataExporters.Format format = extractorCombo.getValue();
        if (DataExporters.isBinary(format)) {
            DialogTheme.apply(new Alert(Alert.AlertType.WARNING,
                    "Excel export can't be copied as text — use Export to File.")).showAndWait();
            return;
        }
        DataExporters.ExportOptions options = buildOptions();
        String qualifiedTable = qualifiedDisplayName();
        String text = DataExporters.render(format, result, qualifiedTable, result.getRows().size(), options);

        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
        stage.close();
    }

    private void handleExportToFile() {
        String path = outputFileField.getText().trim();
        if (path.isEmpty()) {
            DialogTheme.apply(new Alert(Alert.AlertType.WARNING, "Please specify an output file path.")).showAndWait();
            return;
        }

        File file = new File(path);
        DataExporters.Format format = extractorCombo.getValue();
        DataExporters.ExportOptions options = buildOptions();
        String qualifiedTable = qualifiedDisplayName();

        stage.close();
        AppExecutor.run(() -> {
            try {
                if (file.getParentFile() != null && !file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                if (format == DataExporters.Format.EXCEL) {
                    XlsxWriter.write(file, result.getColumns(), result.getRows());
                } else {
                    String text = DataExporters.render(format, result, qualifiedTable, result.getRows().size(), options);
                    Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
                }
                Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION,
                        "Exported " + result.getRows().size() + " row(s) to " + file.getName())).showAndWait());
            } catch (Exception ex) {
                Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                        "Export failed: " + ex.getMessage())).showAndWait());
            }
        });
    }
}
