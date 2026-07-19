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

import java.io.File;

/**
 * DataGrip-style "Export Data" dialog: pick an extractor format, see a live
 * preview built from the actual query result, then copy to clipboard or
 * export to a file. Mirrors the reference dialog's layout and field names.
 */
public final class ExportDataDialog {

    private final Stage stage = new Stage();
    private final ConnectionProfile profile;
    private final DbObject table;
    private final QueryResult result;
    private final ComboBox<DataExporters.Format> extractorCombo = new ComboBox<>();
    private final TextArea previewArea = new TextArea();
    private final Label rowCountLabel = new Label();

    private ExportDataDialog(Window owner, ConnectionProfile profile, DbObject table, QueryResult result) {
        this.profile = profile;
        this.table = table;
        this.result = result;
        buildUi(owner);
        refreshPreview();
    }

    public static void show(Window owner, ConnectionProfile profile, DbObject table, QueryResult result) {
        new ExportDataDialog(owner, profile, table, result).stage.showAndWait();
    }

    private void buildUi(Window owner) {
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Export Data");
        stage.setMinWidth(720);
        stage.setMinHeight(460);

        Label sourceLabel = new Label("Source:");
        sourceLabel.getStyleClass().add("connection-field-label");
        TextField sourceField = new TextField(qualifiedDisplayName());
        sourceField.setEditable(false);
        HBox.setHgrow(sourceField, Priority.ALWAYS);
        VBox sourceBox = new VBox(4, sourceLabel, sourceField);

        Label extractorLabel = new Label("Extractor:");
        extractorLabel.getStyleClass().add("connection-field-label");
        extractorCombo.getItems().addAll(DataExporters.Format.values());
        extractorCombo.getSelectionModel().selectFirst();
        extractorCombo.setPrefWidth(220);
        extractorCombo.valueProperty().addListener((obs, o, n) -> refreshPreview());
        VBox extractorBox = new VBox(4, extractorLabel, extractorCombo);

        VBox leftColumn = new VBox(16, sourceBox, extractorBox);
        leftColumn.setPrefWidth(260);
        leftColumn.setPadding(new Insets(16));

        Label previewLabel = new Label("Export preview:");
        previewLabel.getStyleClass().add("connection-field-label");
        previewArea.setEditable(false);
        previewArea.setWrapText(false);
        previewArea.getStyleClass().add("process-output");
        VBox.setVgrow(previewArea, Priority.ALWAYS);

        rowCountLabel.getStyleClass().add("console-status");
        VBox rightColumn = new VBox(6, previewLabel, previewArea, rowCountLabel);
        rightColumn.setPadding(new Insets(16));
        VBox.setVgrow(previewArea, Priority.ALWAYS);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        HBox bodyBox = new HBox(leftColumn, new Separator(javafx.geometry.Orientation.VERTICAL), rightColumn);
        VBox.setVgrow(bodyBox, Priority.ALWAYS);

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> stage.close());
        Button copyToClipboard = new Button("Copy to Clipboard");
        copyToClipboard.setOnAction(e -> copyToClipboard());
        Button exportToFile = new Button("Export to File");
        exportToFile.getStyleClass().add("run-button");
        exportToFile.setDefaultButton(true);
        exportToFile.setOnAction(e -> exportToFile());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10, spacer, cancel, copyToClipboard, exportToFile);
        buttons.setPadding(new Insets(0, 16, 16, 16));
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(bodyBox, buttons);
        root.getStyleClass().add("app-root");
        Scene scene = new Scene(root, 780, 480);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
    }

    private String qualifiedDisplayName() {
        if (table == null) return profile.getName();
        StringBuilder sb = new StringBuilder();
        if (table.getCatalog() != null) sb.append(table.getCatalog()).append('.');
        if (table.getSchema() != null) sb.append(table.getSchema()).append('.');
        sb.append(table.getName());
        return sb.toString();
    }

    private void refreshPreview() {
        DataExporters.Format format = extractorCombo.getValue();
        if (DataExporters.isBinary(format)) {
            previewArea.setText("(binary format — no text preview; use Export to File)");
            rowCountLabel.setText(result.getRows().size() + " row(s) will be exported");
            return;
        }
        String qualifiedTable = table != null ? table.qualifiedName() : "table";
        int previewLimit = Math.min(50, result.getRows().size());
        previewArea.setText(DataExporters.render(format, sliced(previewLimit), qualifiedTable, previewLimit));
        rowCountLabel.setText(result.getRows().size() + " row(s) total"
                + (result.getRows().size() > previewLimit ? " — preview shows first " + previewLimit : ""));
    }

    private QueryResult sliced(int limit) {
        if (limit >= result.getRows().size()) return result;
        QueryResult partial = new QueryResult();
        partial.getColumns().addAll(result.getColumns());
        partial.getColumnTypes().addAll(result.getColumnTypes());
        partial.getRows().addAll(result.getRows().subList(0, limit));
        return partial;
    }

    private void copyToClipboard() {
        DataExporters.Format format = extractorCombo.getValue();
        if (DataExporters.isBinary(format)) {
            DialogTheme.apply(new Alert(Alert.AlertType.WARNING, "Excel export can't be copied as text — use Export to File."))
                    .showAndWait();
            return;
        }
        String qualifiedTable = table != null ? table.qualifiedName() : "table";
        String text = DataExporters.render(format, result, qualifiedTable, result.getRows().size());
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
        stage.close();
    }

    private void exportToFile() {
        DataExporters.Format format = extractorCombo.getValue();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export to File");
        chooser.setInitialFileName((table != null ? table.getName() : "export") + defaultExtension(format));
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        stage.close();
        AppExecutor.run(() -> {
            try {
                if (format == DataExporters.Format.EXCEL) {
                    XlsxWriter.write(file, result.getColumns(), result.getRows());
                } else {
                    String qualifiedTable = table != null ? table.qualifiedName() : "table";
                    String text = DataExporters.render(format, result, qualifiedTable, result.getRows().size());
                    java.nio.file.Files.writeString(file.toPath(), text, java.nio.charset.StandardCharsets.UTF_8);
                }
                Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.INFORMATION,
                        "Exported " + result.getRows().size() + " row(s) to " + file.getName())).showAndWait());
            } catch (Exception ex) {
                Platform.runLater(() -> DialogTheme.apply(new Alert(Alert.AlertType.ERROR,
                        "Export failed: " + ex.getMessage())).showAndWait());
            }
        });
    }

    private static String defaultExtension(DataExporters.Format format) {
        return switch (format) {
            case EXCEL -> ".xlsx";
            case CSV -> ".csv";
            case TSV -> ".tsv";
            case PIPE, SEMICOLON -> ".txt";
            case SQL_INSERTS, SQL_UPDATES, WHERE_CLAUSE -> ".sql";
        };
    }
}
