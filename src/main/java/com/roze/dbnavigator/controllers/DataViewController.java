package com.roze.dbnavigator.controllers;

import com.roze.dbnavigator.models.ConnectionProfile;
import com.roze.dbnavigator.models.DatabaseObject;
import com.roze.dbnavigator.models.TableColumnInfo;
import com.roze.dbnavigator.services.ConnectionService;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataViewController {
    @FXML
    private TableView<Map<String, Object>> dataTable;
    @FXML
    private Label rowCountLabel;

    private ConnectionProfile currentProfile;
    private DatabaseObject currentTable;

    @FXML
    public void initialize() {
        dataTable.setEditable(true);
    }

    public void loadTableData(ConnectionProfile profile, DatabaseObject table) {
        this.currentProfile = profile;
        this.currentTable = table;
        refreshData();
    }

    @FXML
    private void refreshData() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try (Connection conn = ConnectionService.getConnection(currentProfile)) {
                    // Get column info
                    DatabaseMetaData meta = conn.getMetaData();
                    List<TableColumnInfo> columns = new ArrayList<>();
                    try (ResultSet rs = meta.getColumns(null, currentTable.getSchema(), currentTable.getName(), "%")) {
                        while (rs.next()) {
                            columns.add(new TableColumnInfo(
                                    rs.getString("COLUMN_NAME"),
                                    rs.getString("TYPE_NAME"),
                                    rs.getInt("COLUMN_SIZE"),
                                    rs.getString("IS_NULLABLE"),
                                    rs.getString("COLUMN_DEF")
                            ));
                        }
                    }

                    // Get data
                    String query = "SELECT * FROM " + currentTable.getFullName() + " LIMIT 1000";
                    List<Map<String, Object>> data = new ArrayList<>();

                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(query)) {

                        ResultSetMetaData rsMeta = rs.getMetaData();
                        int columnCount = rsMeta.getColumnCount();

                        while (rs.next()) {
                            Map<String, Object> row = new HashMap<>();
                            for (int i = 1; i <= columnCount; i++) {
                                row.put(rsMeta.getColumnName(i), rs.getObject(i));
                            }
                            data.add(row);
                        }
                    }

                    Platform.runLater(() -> updateTable(columns, data));
                }
                return null;
            }
        };

        new Thread(task).start();
    }

    private void updateTable(List<TableColumnInfo> columns, List<Map<String, Object>> data) {
        dataTable.getColumns().clear();

        for (TableColumnInfo col : columns) {
            TableColumn<Map<String, Object>, Object> column = new TableColumn<>(col.getName());
            column.setCellValueFactory(data1 ->
                    new SimpleObjectProperty<>(data1.getValue().get(col.getName())));
            dataTable.getColumns().add(column);
        }

        dataTable.getItems().setAll(data);
        rowCountLabel.setText("Rows: " + data.size());
    }
}