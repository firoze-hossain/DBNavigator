package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.db.SqlValueBinder;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DataGrip-style "Import Data from File(s)…": pick a CSV/TSV file, map its
 * columns to the target table's columns (auto-matched by name, overridable),
 * preview the first rows, then batch-insert in one transaction with progress
 * streamed into the docked Run panel.
 *
 * Honest scope note: reads delimited text (CSV/TSV/pipe/semicolon), not
 * arbitrary Excel binaries — those need either Apache POI or a hand-rolled
 * reader, and delimited files cover the overwhelming majority of "import
 * data" requests. A confirm dialog lets you pick the delimiter if it's not
 * auto-detected correctly.
 */
public final class ImportDataDialog {

    private final MainWindow mainWindow;
    private final ConnectionProfile profile;
    private final DbObject table;
    private final Stage stage = new Stage();

    private final TextField fileField = new TextField();
    private final ComboBox<String> delimiterCombo = new ComboBox<>();
    private final CheckBox firstRowIsHeader = new CheckBox("First row is header");
    private final TableView<String[]> previewTable = new TableView<>();
    private final GridPane mappingGrid = new GridPane();
    private final List<ComboBox<String>> columnMappings = new ArrayList<>();

    private List<String> fileHeaders = List.of();
    private List<String[]> fileRows = List.of();
    private List<String> tableColumns = List.of();

    private ImportDataDialog(MainWindow mainWindow, ConnectionProfile profile, DbObject table) {
        this.mainWindow = mainWindow;
        this.profile = profile;
        this.table = table;
        buildUi();
    }

    public static void show(MainWindow mainWindow, ConnectionProfile profile, DbObject table) {
        new ImportDataDialog(mainWindow, profile, table).stage.showAndWait();
    }

    private void buildUi() {
        stage.initOwner(mainWindow.getOwnerWindow());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Import Data into " + table.getName());
        stage.setMinWidth(760);
        stage.setMinHeight(560);

        fileField.setPromptText("Choose a CSV/TSV file to import");
        HBox.setHgrow(fileField, Priority.ALWAYS);
        Button browse = new Button();
        browse.setGraphic(Icons.of(FontAwesomeSolid.FOLDER_OPEN, "#e0a44c", 12));
        browse.setOnAction(e -> chooseFile());

        delimiterCombo.getItems().addAll("Auto-detect", "Comma (,)", "Tab", "Semicolon (;)", "Pipe (|)");
        delimiterCombo.getSelectionModel().selectFirst();
        delimiterCombo.setOnAction(e -> reloadPreview());
        firstRowIsHeader.setSelected(true);
        firstRowIsHeader.setOnAction(e -> reloadPreview());

        HBox fileRow = new HBox(8, fileField, browse);
        HBox optionsRow = new HBox(16, new Label("Delimiter:"), delimiterCombo, firstRowIsHeader);
        optionsRow.setAlignment(Pos.CENTER_LEFT);

        Label previewLabel = new Label("File preview:");
        previewLabel.getStyleClass().add("connection-field-label");
        previewTable.setPrefHeight(180);
        previewTable.setPlaceholder(new Label("Choose a file to see a preview"));

        Label mappingLabel = new Label("Column mapping (file column \u2192 table column):");
        mappingLabel.getStyleClass().add("connection-field-label");
        ScrollPane mappingScroll = new ScrollPane(mappingGrid);
        mappingScroll.setFitToWidth(true);
        mappingScroll.setPrefHeight(180);

        VBox content = new VBox(10, fileRow, optionsRow, new Separator(),
                previewLabel, previewTable, new Separator(), mappingLabel, mappingScroll);
        content.setPadding(new Insets(16));
        VBox.setVgrow(mappingScroll, Priority.ALWAYS);

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> stage.close());
        Button importButton = new Button("Import");
        importButton.getStyleClass().add("run-button");
        importButton.setDefaultButton(true);
        importButton.setOnAction(e -> runImport());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10, spacer, cancel, importButton);
        buttons.setPadding(new Insets(0, 16, 16, 16));

        VBox root = new VBox(content, buttons);
        root.getStyleClass().add("app-root");
        VBox.setVgrow(content, Priority.ALWAYS);

        Scene scene = new Scene(root, 800, 620);
        Window owner = mainWindow.getOwnerWindow();
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);

        AppExecutor.run(() -> {
            try {
                tableColumns = new ArrayList<>(
                        com.roze.dbnavigator.db.MetadataService.loadColumnTypes(profile, table).keySet());
            } catch (Exception ignored) {
                tableColumns = List.of();
            }
        });
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose file to import");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Delimited text", "*.csv", "*.tsv", "*.txt"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        fileField.setText(file.getAbsolutePath());
        reloadPreview();
    }

    private char resolveDelimiter(String firstLine) {
        return switch (delimiterCombo.getValue()) {
            case "Comma (,)" -> ',';
            case "Tab" -> '\t';
            case "Semicolon (;)" -> ';';
            case "Pipe (|)" -> '|';
            default -> autoDetectDelimiter(firstLine);
        };
    }

    private char autoDetectDelimiter(String firstLine) {
        if (firstLine == null) return ',';
        char best = ',';
        long bestCount = -1;
        for (char candidate : new char[]{',', '\t', ';', '|'}) {
            long count = firstLine.chars().filter(c -> c == candidate).count();
            if (count > bestCount) { bestCount = count; best = candidate; }
        }
        return best;
    }

    private void reloadPreview() {
        String path = fileField.getText();
        if (path.isBlank() || !new File(path).isFile()) return;

        try (BufferedReader reader = Files.newBufferedReader(new File(path).toPath(), StandardCharsets.UTF_8)) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null && lines.size() < 200) lines.add(line);
            if (lines.isEmpty()) return;

            char delim = resolveDelimiter(lines.get(0));
            List<String[]> parsed = new ArrayList<>();
            for (String l : lines) parsed.add(splitLine(l, delim));

            if (firstRowIsHeader.isSelected()) {
                fileHeaders = List.of(parsed.get(0));
                fileRows = parsed.subList(1, parsed.size());
            } else {
                String[] first = parsed.get(0);
                List<String> generated = new ArrayList<>();
                for (int i = 0; i < first.length; i++) generated.add("column" + (i + 1));
                fileHeaders = generated;
                fileRows = parsed;
            }
            renderPreviewTable();
            renderMappingGrid();
        } catch (IOException ex) {
            DialogTheme.apply(new Alert(Alert.AlertType.ERROR, "Could not read file: " + ex.getMessage())).showAndWait();
        }
    }

    /** Simple CSV-style split that respects double-quoted fields. */
    private static String[] splitLine(String line, char delim) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                else if (c == '"') inQuotes = false;
                else cur.append(c);
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == delim) {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        fields.add(cur.toString());
        return fields.toArray(new String[0]);
    }

    @SuppressWarnings("unchecked")
    private void renderPreviewTable() {
        previewTable.getColumns().clear();
        for (int i = 0; i < fileHeaders.size(); i++) {
            final int idx = i;
            TableColumn<String[], String> col = new TableColumn<>(fileHeaders.get(i));
            col.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(
                    idx < data.getValue().length ? data.getValue()[idx] : ""));
            previewTable.getColumns().add(col);
        }
        previewTable.getItems().setAll(fileRows.subList(0, Math.min(20, fileRows.size())));
    }

    private void renderMappingGrid() {
        mappingGrid.getChildren().clear();
        mappingGrid.setHgap(10);
        mappingGrid.setVgap(8);
        columnMappings.clear();

        for (int i = 0; i < fileHeaders.size(); i++) {
            Label fileCol = new Label(fileHeaders.get(i));
            fileCol.setMinWidth(160);
            Label arrow = new Label("\u2192");
            ComboBox<String> targetCombo = new ComboBox<>();
            targetCombo.getItems().add("(skip this column)");
            targetCombo.getItems().addAll(tableColumns);
            String header = fileHeaders.get(i);
            String bestMatch = tableColumns.stream()
                    .filter(c -> c.equalsIgnoreCase(header))
                    .findFirst()
                    .orElse(tableColumns.isEmpty() ? "(skip this column)" : "(skip this column)");
            targetCombo.getSelectionModel().select(bestMatch);
            targetCombo.setPrefWidth(220);

            mappingGrid.add(fileCol, 0, i);
            mappingGrid.add(arrow, 1, i);
            mappingGrid.add(targetCombo, 2, i);
            columnMappings.add(targetCombo);
        }
    }

    private void runImport() {
        if (fileRows.isEmpty()) {
            DialogTheme.apply(new Alert(Alert.AlertType.WARNING, "Choose a file with at least one data row first.")).showAndWait();
            return;
        }

        List<Integer> sourceIndexes = new ArrayList<>();
        List<String> targetColumns = new ArrayList<>();
        for (int i = 0; i < columnMappings.size(); i++) {
            String target = columnMappings.get(i).getValue();
            if (target != null && !target.equals("(skip this column)")) {
                sourceIndexes.add(i);
                targetColumns.add(target);
            }
        }
        if (targetColumns.isEmpty()) {
            DialogTheme.apply(new Alert(Alert.AlertType.WARNING, "Map at least one column before importing.")).showAndWait();
            return;
        }

        stage.close();
        RunPanel.RunHandle handle = mainWindow.getRunPanel().startRun("Import into " + table.getName());
        mainWindow.showRunPanel();
        handle.appendLine("Importing " + fileRows.size() + " row(s) from " + fileField.getText()
                + " into " + table.qualifiedName());

        String insertSql = "INSERT INTO " + table.qualifiedName() + " ("
                + String.join(", ", targetColumns.stream().map(DbObject::quote).toList())
                + ") VALUES (" + String.join(", ", targetColumns.stream().map(c -> "?").toList()) + ")";

        AppExecutor.run(() -> {
            int success = 0, failed = 0;
            try {
                Map<String, Integer> types = com.roze.dbnavigator.db.MetadataService.loadColumnTypes(profile, table);
                try (Connection conn = ClientRegistry.jdbc(profile, table.getCatalog()).getConnection()) {
                    conn.setAutoCommit(false);
                    try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                        for (String[] row : fileRows) {
                            for (int i = 0; i < sourceIndexes.size(); i++) {
                                int srcIdx = sourceIndexes.get(i);
                                String value = srcIdx < row.length ? row[srcIdx] : null;
                                Integer type = types.get(targetColumns.get(i));
                                SqlValueBinder.bind(stmt, i + 1, type, value);
                            }
                            try {
                                stmt.executeUpdate();
                                success++;
                            } catch (Exception rowFailure) {
                                failed++;
                                handle.appendLine("Row failed: " + rowFailure.getMessage());
                            }
                        }
                        conn.commit();
                    } catch (Exception txFailure) {
                        conn.rollback();
                        throw txFailure;
                    }
                }
                handle.appendLine("\u2713 Imported " + success + " row(s)"
                        + (failed > 0 ? ", " + failed + " failed" : ""));
                handle.markFinished(failed > 0 ? 1 : 0);
            } catch (Exception ex) {
                handle.markFailed(ex.getMessage() == null ? ex.toString() : ex.getMessage());
            }
        });
    }
}
