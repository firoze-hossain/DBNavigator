package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.QueryResult;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Read-only spreadsheet-like grid used everywhere results are shown. */
public class ResultGrid extends TableView<List<String>> {

    public ResultGrid() {
        getStyleClass().add("result-grid");
        setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        setPlaceholder(new Label("No data"));

        MenuItem copyCell = new MenuItem("Copy Cell");
        copyCell.setOnAction(e -> copySelectedCell());
        MenuItem copyRow = new MenuItem("Copy Row (TSV)");
        copyRow.setOnAction(e -> copySelectedRow());
        setContextMenu(new ContextMenu(copyCell, copyRow));

        getSelectionModel().setCellSelectionEnabled(true);
    }

    public void showResult(QueryResult result) {
        getColumns().clear();
        getItems().clear();
        if (result == null || !result.isResultSet()) return;

        List<String> columnNames = result.getColumns();
        for (int i = 0; i < columnNames.size(); i++) {
            final int index = i;
            TableColumn<List<String>, String> col = new TableColumn<>(columnNames.get(i));
            col.setCellValueFactory(data -> {
                List<String> row = data.getValue();
                String value = index < row.size() ? row.get(index) : null;
                return new ReadOnlyStringWrapper(value == null ? "NULL" : value);
            });
            col.setPrefWidth(Math.max(90, Math.min(280, columnNames.get(i).length() * 12 + 40)));
            getColumns().add(col);
        }

        ObservableList<List<String>> items = FXCollections.observableArrayList(result.getRows());
        setItems(items);
    }

    private void copySelectedCell() {
        TablePosition<?, ?> pos = getSelectionModel().getSelectedCells().stream()
                .findFirst().orElse(null);
        if (pos == null) return;
        List<String> row = getItems().get(pos.getRow());
        String value = pos.getColumn() < row.size() ? row.get(pos.getColumn()) : "";
        put(value == null ? "" : value);
    }

    private void copySelectedRow() {
        List<String> row = getSelectionModel().getSelectedItem();
        if (row == null) return;
        put(String.join("\t", row.stream().map(v -> v == null ? "" : v).toList()));
    }

    private static void put(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    /** Exports the currently displayed data to CSV. */
    public void exportCsv() {
        if (getItems().isEmpty()) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export CSV");
        chooser.setInitialFileName("export.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) return;

        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
            out.println(getColumns().stream()
                    .map(c -> csvEscape(c.getText()))
                    .reduce((a, b) -> a + "," + b).orElse(""));
            for (List<String> row : getItems()) {
                out.println(row.stream()
                        .map(v -> csvEscape(v == null ? "" : v))
                        .reduce((a, b) -> a + "," + b).orElse(""));
            }
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage()).showAndWait();
        }
    }

    private static String csvEscape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
