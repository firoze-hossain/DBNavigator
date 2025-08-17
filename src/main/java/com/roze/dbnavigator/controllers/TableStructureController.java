package com.roze.dbnavigator.controllers;

import com.roze.dbnavigator.models.DatabaseObject;
import com.roze.dbnavigator.models.TableColumnInfo;
import com.roze.dbnavigator.models.TableIndex;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class TableStructureController {
    @FXML
    private TableView<TableColumnInfo> columnsTable;
    @FXML
    private TableView<TableIndex> indexesTable;
    @FXML
    private TextArea ddlText;

    @FXML
    public void initialize() {
        configureTableColumns();
    }

    private void configureTableColumns() {
        // Columns table
        TableColumn<TableColumnInfo, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<TableColumnInfo, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<TableColumnInfo, Integer> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("size"));

        TableColumn<TableColumnInfo, String> nullableCol = new TableColumn<>("Nullable");
        nullableCol.setCellValueFactory(new PropertyValueFactory<>("nullable"));

        TableColumn<TableColumnInfo, String> defaultCol = new TableColumn<>("Default");
        defaultCol.setCellValueFactory(new PropertyValueFactory<>("defaultValue"));

        columnsTable.getColumns().setAll(nameCol, typeCol, sizeCol, nullableCol, defaultCol);

        // Indexes table
        TableColumn<TableIndex, String> idxNameCol = new TableColumn<>("Name");
        idxNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<TableIndex, String> idxColCol = new TableColumn<>("Column");
        idxColCol.setCellValueFactory(new PropertyValueFactory<>("columnName"));

        TableColumn<TableIndex, Boolean> uniqueCol = new TableColumn<>("Unique");
        uniqueCol.setCellValueFactory(new PropertyValueFactory<>("nonUnique"));

        indexesTable.getColumns().setAll(idxNameCol, idxColCol, uniqueCol);
    }

    public void initializeTable(DatabaseObject table, List<TableColumnInfo> columns,
                                List<String> primaryKeys, List<TableIndex> indexes) {
        Platform.runLater(() -> {
            columnsTable.getItems().setAll(columns);
            indexesTable.getItems().setAll(indexes);
            generateDDL(table, columns, primaryKeys);
        });
    }

    private void generateDDL(DatabaseObject table, List<TableColumnInfo> columns,
                             List<String> primaryKeys) {
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TABLE ").append(table.getFullName()).append(" (\n");

        for (TableColumnInfo col : columns) {
            ddl.append("  ").append(col.getName()).append(" ").append(col.getType());
            if (col.getSize() > 0) {
                ddl.append("(").append(col.getSize()).append(")");
            }
            if ("NO".equals(col.getNullable())) {
                ddl.append(" NOT NULL");
            }
            if (col.getDefaultValue() != null) {
                ddl.append(" DEFAULT ").append(col.getDefaultValue());
            }
            ddl.append(",\n");
        }

        if (!primaryKeys.isEmpty()) {
            ddl.append("  PRIMARY KEY (");
            ddl.append(String.join(", ", primaryKeys));
            ddl.append("),\n");
        }

        // Remove trailing comma and newline
        if (ddl.length() > 2) {
            ddl.setLength(ddl.length() - 2);
        }

        ddl.append("\n);");
        ddlText.setText(ddl.toString());
    }
}