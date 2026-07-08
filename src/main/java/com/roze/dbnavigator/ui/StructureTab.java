package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.QueryResult;
import com.roze.dbnavigator.util.AppExecutor;
import javafx.application.Platform;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/** Shows a table's columns and indexes in two sub-tabs. */
public class StructureTab extends Tab {

    public StructureTab(ConnectionProfile profile, DbObject table) {
        setText(table.getName() + " [structure]");
        setGraphic(Icons.of(FontAwesomeSolid.SITEMAP, "#c77dbb", 11));

        ResultGrid columnsGrid = new ResultGrid();
        ResultGrid indexesGrid = new ResultGrid();

        Tab columnsTab = new Tab("Columns", columnsGrid);
        columnsTab.setClosable(false);
        Tab indexesTab = new Tab("Indexes", indexesGrid);
        indexesTab.setClosable(false);

        TabPane inner = new TabPane(columnsTab, indexesTab);
        inner.getStyleClass().add("structure-tabs");
        setContent(inner);

        AppExecutor.run(() -> {
            try {
                QueryResult columns = MetadataService.loadTableStructure(profile, table);
                QueryResult indexes = MetadataService.loadTableIndexes(profile, table);
                Platform.runLater(() -> {
                    columnsGrid.showResult(columns);
                    indexesGrid.showResult(indexes);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> columnsGrid.setPlaceholder(
                        new javafx.scene.control.Label("Error: " + ex.getMessage())));
            }
        });
    }
}
