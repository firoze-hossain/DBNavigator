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
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * DataGrip-parity "Export Data" dialog:
 * - Dynamic extractor options (CSV headers/transpose, SQL Inserts computed/generated/DDL, SQL Updates, Where Clause)
 * - Clean smooth dark styling matching DataGrip dialogs
 * - Filtered system columns (tableoid, ctid are excluded)
 * - Non-selected Source field and default ~/Downloads destination
 * - Integrated folder browse button
 * - Live line-numbered syntax-highlighted preview using CodeArea
 * - Cancel, Copy to Clipboard, Export to File action buttons
 */
public final class ExportDataDialog {

    private final Stage stage = new Stage();
    private final ConnectionProfile profile;
    private final DbObject table;
    private final QueryResult result;
    private final String customSourceName;
    private final ConnectionProfile.DatabaseType dbType;

    final ComboBox<DataExporters.Format> extractorCombo = new ComboBox<>();
    final CheckBox transposeCheck = new CheckBox("Transpose");
    final CheckBox addColumnHeaderCheck = new CheckBox("Add column header");
    final CheckBox addRowHeaderCheck = new CheckBox("Add row header");
    final CheckBox addComputedCheck = new CheckBox("Computed");
    final CheckBox addGeneratedCheck = new CheckBox("Generated");
    final CheckBox addTableDefCheck = new CheckBox("Add table definition (DDL)");
    final CheckBox outputQueryToSeparateSheetCheck = new CheckBox("Output query to separate sheet");

    final VBox dynamicOptionsBox = new VBox(8);

    final TextField sourceField = new TextField();
    final TextField outputFileField = new TextField();

    final CodeArea previewEditor = new CodeArea();
    final VirtualizedScrollPane<CodeArea> previewScroll = new VirtualizedScrollPane<>(previewEditor);
    final Label binaryPlaceholder = new Label("Extractor is binary. Preview is not available.");
    final StackPane previewContainer = new StackPane(previewScroll, binaryPlaceholder);

    final Button copyToClipboardBtn = new Button("Copy to Clipboard");

    ExportDataDialog(Window owner, ConnectionProfile profile, DbObject table, QueryResult result,
                     String customSourceName, ConnectionProfile.DatabaseType dbType,
                     DataExporters.Format initialFormat) {
        this.profile = profile;
        this.table = table;
        this.result = filterSystemColumns(result != null ? result : new QueryResult());
        this.customSourceName = customSourceName;
        this.dbType = dbType != null ? dbType : (profile != null ? profile.getType() : ConnectionProfile.DatabaseType.POSTGRESQL);

        DataExporters.Format defaultFmt = initialFormat != null
                ? initialFormat
                : (this.dbType == ConnectionProfile.DatabaseType.MONGODB ? DataExporters.Format.JSON : DataExporters.Format.CSV);

        buildUi(owner, defaultFmt);
        updateDynamicOptions(defaultFmt);
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

    static QueryResult filterSystemColumns(QueryResult original) {
        if (original == null) return new QueryResult();
        List<String> cols = original.getColumns();
        if (cols == null || cols.isEmpty()) return original;

        List<Integer> keepIndices = new ArrayList<>();
        List<String> newCols = new ArrayList<>();
        List<String> newTypes = new ArrayList<>();

        for (int i = 0; i < cols.size(); i++) {
            String colName = cols.get(i);
            if ("tableoid".equalsIgnoreCase(colName) || "ctid".equalsIgnoreCase(colName)) {
                continue;
            }
            keepIndices.add(i);
            newCols.add(colName);
            if (original.getColumnTypes() != null && i < original.getColumnTypes().size()) {
                newTypes.add(original.getColumnTypes().get(i));
            }
        }

        if (keepIndices.size() == cols.size()) {
            return original;
        }

        QueryResult clean = new QueryResult();
        clean.getColumns().addAll(newCols);
        clean.getColumnTypes().addAll(newTypes);
        for (List<String> row : original.getRows()) {
            List<String> cleanRow = new ArrayList<>(keepIndices.size());
            for (int idx : keepIndices) {
                cleanRow.add(idx < row.size() ? row.get(idx) : "");
            }
            clean.getRows().add(cleanRow);
        }
        return clean;
    }

    private void buildUi(Window owner, DataExporters.Format defaultFormat) {
        if (owner != null) stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Export Data");
        stage.setMinWidth(780);
        stage.setMinHeight(480);
        stage.setWidth(860);
        stage.setHeight(540);

        // ---------------- Left Column (Options & Output) ----------------
        Label sourceLabel = new Label("Source:");
        sourceLabel.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px;");

        sourceField.setText(qualifiedDisplayName());
        sourceField.setEditable(false);
        sourceField.setFocusTraversable(false); // Do not steal focus; avoid blue text selection
        sourceField.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: #dfe1e5; -fx-border-color: #393b40; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 5 8 5 8; -fx-font-size: 12px;");
        sourceField.setPrefHeight(30);

        Label extractorLabel = new Label("Extractor:");
        extractorLabel.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px;");

        extractorCombo.getItems().addAll(DataExporters.Format.values());
        extractorCombo.setValue(defaultFormat != null ? defaultFormat : DataExporters.Format.CSV);
        extractorCombo.setMaxWidth(Double.MAX_VALUE);
        extractorCombo.setPrefHeight(30);
        extractorCombo.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: #dfe1e5; -fx-border-color: #3574F0; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-font-size: 12px;");

        // Checkboxes styling
        transposeCheck.setStyle("-fx-text-fill: #dfe1e5; -fx-font-size: 12px; -fx-cursor: hand;");
        transposeCheck.setSelected(false);

        addColumnHeaderCheck.setStyle("-fx-text-fill: #dfe1e5; -fx-font-size: 12px; -fx-cursor: hand;");
        addColumnHeaderCheck.setSelected(false); // Unchecked by default matching DataGrip Image 1

        addRowHeaderCheck.setStyle("-fx-text-fill: #dfe1e5; -fx-font-size: 12px; -fx-cursor: hand;");
        addRowHeaderCheck.setSelected(false);

        addComputedCheck.setStyle("-fx-text-fill: #dfe1e5; -fx-font-size: 12px; -fx-cursor: hand;");
        addComputedCheck.setSelected(false);

        addGeneratedCheck.setStyle("-fx-text-fill: #dfe1e5; -fx-font-size: 12px; -fx-cursor: hand;");
        addGeneratedCheck.setSelected(true); // Checked by default matching DataGrip Image 3

        addTableDefCheck.setStyle("-fx-text-fill: #dfe1e5; -fx-font-size: 12px; -fx-cursor: hand;");
        addTableDefCheck.setSelected(false);

        outputQueryToSeparateSheetCheck.setStyle("-fx-text-fill: #dfe1e5; -fx-font-size: 12px; -fx-cursor: hand;");
        outputQueryToSeparateSheetCheck.setSelected(true); // Checked by default matching DataGrip Image 1

        dynamicOptionsBox.setPadding(new Insets(4, 0, 4, 0));

        // Output file with integrated browse folder icon
        Label outputLabel = new Label("Output file:");
        outputLabel.setStyle("-fx-text-fill: #868a91; -fx-font-size: 12px;");

        outputFileField.setText(buildDefaultOutputPath(extractorCombo.getValue()));
        outputFileField.setStyle("-fx-background-color: transparent; -fx-text-fill: #dfe1e5; -fx-border-color: transparent; -fx-font-size: 12px; -fx-padding: 5 32 5 8;");
        outputFileField.setPrefHeight(30);

        Button browseButton = new Button();
        browseButton.setGraphic(Icons.of(FontAwesomeSolid.FOLDER, "#868a91", 13));
        browseButton.setTooltip(new Tooltip("Browse output file location"));
        browseButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 6 4 6;");
        browseButton.setOnAction(e -> handleBrowseFile());

        StackPane outputBox = new StackPane(outputFileField, browseButton);
        StackPane.setAlignment(browseButton, Pos.CENTER_RIGHT);
        StackPane.setMargin(browseButton, new Insets(0, 4, 0, 0));
        outputBox.setStyle("-fx-background-color: #2b2d30; -fx-border-color: #393b40; -fx-border-radius: 4px; -fx-background-radius: 4px;");

        outputFileField.focusedProperty().addListener((obs, oldV, isFocused) -> {
            outputBox.setStyle("-fx-background-color: #2b2d30; -fx-border-color: " + (isFocused ? "#3574F0" : "#393b40") + "; -fx-border-radius: 4px; -fx-background-radius: 4px;");
        });

        // Listeners for dynamic UI changes and live preview updates
        extractorCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateOutputFileExtension(newVal);
            updateDynamicOptions(newVal);
            refreshPreview();
        });
        transposeCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview());
        addColumnHeaderCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview());
        addRowHeaderCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview());
        addComputedCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview());
        addGeneratedCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview());
        addTableDefCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview());
        outputQueryToSeparateSheetCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview());

        VBox leftColumn = new VBox(10,
                sourceLabel, sourceField,
                extractorLabel, extractorCombo,
                dynamicOptionsBox,
                outputLabel, outputBox);
        leftColumn.setPrefWidth(290);
        leftColumn.setMinWidth(270);
        leftColumn.setMaxWidth(310);
        leftColumn.setPadding(new Insets(16, 0, 8, 16));

        // ---------------- Right Column (Preview with Line Numbers & Syntax Highlighting) ----------------
        Label previewLabel = new Label("Export preview:");
        previewLabel.setStyle("-fx-text-fill: #dfe1e5; -fx-font-size: 12px;");

        previewEditor.setEditable(false);
        previewEditor.setWrapText(false);
        previewEditor.setParagraphGraphicFactory(LineNumberFactory.get(previewEditor));
        previewEditor.getStyleClass().add("sql-editor");
        previewEditor.setStyle("-fx-font-family: 'DBNavigator Mono', monospace; -fx-font-size: 12px; -fx-background-color: #1e1f22;");

        previewScroll.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4px; -fx-background-radius: 4px;");
        VBox.setVgrow(previewScroll, Priority.ALWAYS);
        HBox.setHgrow(previewScroll, Priority.ALWAYS);

        binaryPlaceholder.setStyle("-fx-text-fill: #868a91; -fx-font-size: 13px;");
        binaryPlaceholder.setAlignment(Pos.CENTER);
        StackPane.setAlignment(binaryPlaceholder, Pos.CENTER);
        binaryPlaceholder.setVisible(false);
        binaryPlaceholder.setManaged(false);

        previewContainer.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4px; -fx-background-radius: 4px;");
        VBox.setVgrow(previewContainer, Priority.ALWAYS);
        HBox.setHgrow(previewContainer, Priority.ALWAYS);

        VBox rightColumn = new VBox(8, previewLabel, previewContainer);
        rightColumn.setPadding(new Insets(16, 16, 8, 16));
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        HBox bodyBox = new HBox(12, leftColumn, rightColumn);
        VBox.setVgrow(bodyBox, Priority.ALWAYS);

        // ---------------- Bottom Bar (Help ? on left, Cancel, Copy, Export on right) ----------------
        Button helpButton = new Button("?");
        helpButton.setStyle("-fx-background-radius: 12px; -fx-border-radius: 12px; -fx-min-width: 24px; -fx-max-width: 24px; "
                + "-fx-min-height: 24px; -fx-max-height: 24px; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; "
                + "-fx-background-color: transparent; -fx-text-fill: #868a91; -fx-border-color: #4e5157;");
        helpButton.setTooltip(new Tooltip("Data Export:\n"
                + "• Select format from the Extractor dropdown.\n"
                + "• Transpose switches rows and columns.\n"
                + "• Live preview updates instantly.\n"
                + "• Binary formats (Excel XLSX) can be exported directly to file."));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: #dfe1e5; -fx-border-color: #4e5157; "
                + "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 6 16 6 16; -fx-cursor: hand; -fx-font-size: 12px;");
        cancelBtn.setOnAction(e -> stage.close());

        copyToClipboardBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: #dfe1e5; -fx-border-color: #4e5157; "
                + "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 6 16 6 16; -fx-cursor: hand; -fx-font-size: 12px;");
        copyToClipboardBtn.setOnAction(e -> handleCopyToClipboard());

        Button exportToFileBtn = new Button("Export to File");
        exportToFileBtn.setDefaultButton(true);
        exportToFileBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #ffffff; -fx-border-color: #3574F0; "
                + "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 6 16 6 16; -fx-cursor: hand; -fx-font-size: 12px;");
        exportToFileBtn.setOnAction(e -> handleExportToFile());

        HBox buttons = new HBox(10, helpButton, spacer, cancelBtn, copyToClipboardBtn, exportToFileBtn);
        buttons.setPadding(new Insets(10, 16, 16, 16));
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(bodyBox, buttons);
        root.setStyle("-fx-background-color: #2b2d30;");
        root.getStyleClass().add("app-root");

        Scene scene = new Scene(root, 860, 540);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);

        // Put initial focus on the Extractor ComboBox matching DataGrip
        Platform.runLater(extractorCombo::requestFocus);
    }

    void updateDynamicOptions(DataExporters.Format format) {
        dynamicOptionsBox.getChildren().clear();
        if (format == null) return;

        switch (format) {
            case EXCEL -> {
                // Image 1: Transpose, Output query to separate sheet (checked by default)
                dynamicOptionsBox.getChildren().addAll(transposeCheck, outputQueryToSeparateSheetCheck);
            }
            case CSV, TSV, PIPE, SEMICOLON -> {
                // Delimited / CSV Formats: Transpose, Add column header, Add row header
                dynamicOptionsBox.getChildren().addAll(transposeCheck, addColumnHeaderCheck, addRowHeaderCheck);
            }
            case SQL_INSERTS -> {
                // SQL Inserts (built-in): Row: Add columns: [ ] Computed [X] Generated, then Transpose, then Add table definition (DDL)
                Label addColsLabel = new Label("Add columns:");
                addColsLabel.setStyle("-fx-text-fill: #dfe1e5; -fx-font-size: 12px;");
                HBox addColsRow = new HBox(8, addColsLabel, addComputedCheck, addGeneratedCheck);
                addColsRow.setAlignment(Pos.CENTER_LEFT);
                dynamicOptionsBox.getChildren().addAll(addColsRow, transposeCheck, addTableDefCheck);
            }
            default -> {
                // All scripted formats (HTML, JSON, Markdown, One-row, Pretty, Python, CSV-Groovy, XML, SQL_INSERT_MULTIROW, SQL_INSERT_STATEMENTS)
                // and updates/where formats only show Transpose in DataGrip
                dynamicOptionsBox.getChildren().addAll(transposeCheck);
            }
        }
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

    private String sqlTargetTableName() {
        if (table != null) {
            if (table.getSchema() != null && !table.getSchema().isBlank()) {
                return table.getSchema() + "." + table.getName();
            }
            return table.getName();
        }
        if (customSourceName != null && !customSourceName.isBlank()) {
            return DataExporters.sqlTableName(customSourceName);
        }
        return "table";
    }

    String buildDefaultOutputPath(DataExporters.Format format) {
        File downloads = new File(System.getProperty("user.home", "."), "Downloads");
        File dir = (downloads.exists() && downloads.isDirectory()) ? downloads : new File(System.getProperty("user.home", "."));
        String ext = DataExporters.defaultExtension(format);
        return new File(dir, baseName() + ext).getAbsolutePath();
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
        opts.setAddComputedColumns(addComputedCheck.isSelected());
        opts.setAddGeneratedColumns(addGeneratedCheck.isSelected());
        opts.setAddTableDefinition(addTableDefCheck.isSelected());
        opts.setOutputQueryToSeparateSheet(outputQueryToSeparateSheetCheck.isSelected());
        return opts;
    }

    private void refreshPreview() {
        DataExporters.Format format = extractorCombo.getValue();
        if (DataExporters.isBinary(format)) {
            previewScroll.setVisible(false);
            previewScroll.setManaged(false);
            binaryPlaceholder.setVisible(true);
            binaryPlaceholder.setManaged(true);
            previewContainer.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
            copyToClipboardBtn.setDisable(true);
            copyToClipboardBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: #5a5d63; -fx-border-color: #393b40; "
                    + "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 6 16 6 16; -fx-opacity: 0.5; -fx-cursor: default; -fx-font-size: 12px;");
            return;
        }

        previewScroll.setVisible(true);
        previewScroll.setManaged(true);
        binaryPlaceholder.setVisible(false);
        binaryPlaceholder.setManaged(false);
        previewContainer.setStyle("-fx-background-color: #1e1f22; -fx-border-color: #393b40; -fx-border-radius: 4px; -fx-background-radius: 4px;");
        copyToClipboardBtn.setDisable(false);
        copyToClipboardBtn.setStyle("-fx-background-color: #2b2d30; -fx-text-fill: #dfe1e5; -fx-border-color: #4e5157; "
                + "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 6 16 6 16; -fx-opacity: 1.0; -fx-cursor: hand; -fx-font-size: 12px;");

        DataExporters.ExportOptions options = buildOptions();
        String targetTable = sqlTargetTableName();

        int totalCount = result.getRows().size();
        int previewLimit = Math.min(50, totalCount);

        QueryResult slice = sliced(previewLimit);
        String text = DataExporters.render(format, slice, targetTable, previewLimit, options);

        previewEditor.replaceText(text);

        if (text.isEmpty()) {
            previewEditor.setStyleSpans(0, emptySpans(0));
        } else if (format == DataExporters.Format.PRETTY || format == DataExporters.Format.CSV
                || format == DataExporters.Format.TSV || format == DataExporters.Format.PIPE
                || format == DataExporters.Format.SEMICOLON || format == DataExporters.Format.CSV_GROOVY
                || format == DataExporters.Format.MARKDOWN) {
            previewEditor.setStyleSpans(0, emptySpans(text.length()));
        } else if (format == DataExporters.Format.XML || format == DataExporters.Format.HTML_GROOVY || format == DataExporters.Format.HTML_JS) {
            previewEditor.setStyleSpans(0, SqlHighlighter.computeXmlHighlighting(text));
        } else {
            previewEditor.setStyleSpans(0, SqlHighlighter.computeHighlighting(text));
        }

        Platform.runLater(() -> previewEditor.moveTo(0));
    }

    private StyleSpans<Collection<String>> emptySpans(int length) {
        StyleSpansBuilder<Collection<String>> b = new StyleSpansBuilder<>();
        b.add(Collections.emptyList(), Math.max(0, length));
        return b.create();
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
        String targetTable = sqlTargetTableName();
        String text = DataExporters.render(format, result, targetTable, result.getRows().size(), options);

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
        String targetTable = sqlTargetTableName();

        stage.close();
        AppExecutor.run(() -> {
            try {
                if (file.getParentFile() != null && !file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                if (format == DataExporters.Format.EXCEL) {
                    String query = "SELECT * FROM " + qualifiedDisplayName() + ";";
                    XlsxWriter.write(file, result.getColumns(), result.getRows(), query, options.isOutputQueryToSeparateSheet());
                } else {
                    String text = DataExporters.render(format, result, targetTable, result.getRows().size(), options);
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
