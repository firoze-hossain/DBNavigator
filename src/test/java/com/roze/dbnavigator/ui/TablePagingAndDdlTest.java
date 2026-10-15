package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import com.roze.dbnavigator.model.DbObject;
import javafx.application.Platform;
import javafx.scene.control.Button;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class TablePagingAndDdlTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    private ConnectionProfile createPostgresProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setId("pg-test");
        p.setName("postgres@localhost");
        p.setType(DatabaseType.POSTGRESQL);
        p.setHost("localhost");
        p.setPort(5432);
        p.setDatabase("thunder_db");
        return p;
    }

    private ConnectionProfile createMongoProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setId("mongo-test");
        p.setName("mongodb@local");
        p.setType(DatabaseType.MONGODB);
        p.setHost("localhost");
        p.setPort(27017);
        p.setDatabase("studentdb");
        return p;
    }

    @Test
    public void testTablePagingWidgetTextUpdates() {
        TablePagingWidget widget = new TablePagingWidget();
        Button rowsBtn = widget.getRowsButton();
        assertNotNull(rowsBtn);

        // 0 rows
        widget.update(0, 0, 0, true);
        assertEquals("0 rows", rowsBtn.getText());

        // 3 rows exact (matches screenshot 1 and 2)
        widget.update(1, 3, 3, true);
        assertEquals("3 rows", rowsBtn.getText());

        // 1 row exact
        widget.update(1, 1, 1, true);
        assertEquals("1 row", rowsBtn.getText());

        // Multi-page exact
        widget.update(1, 500, 1200, true);
        assertEquals("1\u2013500 of 1200 rows", rowsBtn.getText());

        // Multi-page approximate
        widget.update(1, 500, 1200, false);
        assertEquals("1\u2013500 of 1200+ rows", rowsBtn.getText());
    }

    @Test
    public void testTablePagingWidgetCallbacks() {
        TablePagingWidget widget = new TablePagingWidget();

        AtomicBoolean countCalled = new AtomicBoolean(false);
        widget.setOnCountRows(() -> countCalled.set(true));

        AtomicInteger pageSizeChanged = new AtomicInteger(0);
        widget.setOnPageSizeChanged(pageSizeChanged::set);

        AtomicReference<TablePagingWidget.Placement> placementChanged = new AtomicReference<>();
        widget.setOnPlacementChanged(placementChanged::set);

        widget.applyPageSize(100);
        assertEquals(100, pageSizeChanged.get());
        assertEquals(100, widget.getActivePageSize());

        widget.changePlacement(TablePagingWidget.Placement.TOOLBAR);
        assertEquals(TablePagingWidget.Placement.TOOLBAR, placementChanged.get());
        assertEquals(TablePagingWidget.Placement.TOOLBAR, widget.getPlacement());
    }

    @Test
    public void testDataTabDdlButtonAndPagingForPostgres() {
        ConnectionProfile profile = createPostgresProfile();
        DbObject table = new DbObject("environments", DbObject.Kind.TABLE, "thunder_db", "public");
        DataTab tab = new DataTab(profile, table);

        // DDL button must be present for PostgreSQL
        Button ddlBtn = tab.getDdlButton();
        assertNotNull(ddlBtn, "DDL button should be present for PostgreSQL");
        assertEquals("DDL", ddlBtn.getText());
        assertTrue(ddlBtn.getStyleClass().contains("ddl-button"));
        assertNotNull(ddlBtn.getTooltip());
        assertEquals("Go to DDL", ddlBtn.getTooltip().getText());

        // Paging widget must be present with default placement BOTTOM_CENTER (Middle)
        TablePagingWidget pager = tab.getPagingWidget();
        assertNotNull(pager, "TablePagingWidget must be present in DataTab");
        assertEquals(TablePagingWidget.Placement.BOTTOM_CENTER, pager.getPlacement());
    }

    @Test
    public void testDataTabDdlButtonAbsentForMongo() {
        ConnectionProfile profile = createMongoProfile();
        DbObject table = new DbObject("students", DbObject.Kind.TABLE, "studentdb", null);
        DataTab tab = new DataTab(profile, table);

        // DDL button must NOT be present for MongoDB
        assertNull(tab.getDdlButton(), "DDL button must NOT be present for MongoDB");

        // Paging widget is still present with default BOTTOM_CENTER
        assertNotNull(tab.getPagingWidget(), "TablePagingWidget must be present");
        assertEquals(TablePagingWidget.Placement.BOTTOM_CENTER, tab.getPagingWidget().getPlacement());
    }

    @Test
    public void testMongoConsoleTabHasNoDdlButtonAndHasPaging() {
        ConnectionProfile profile = createMongoProfile();
        MongoConsoleTab tab = new MongoConsoleTab(profile, "studentdb", "console 1");

        assertNotNull(tab.getPagingWidget(), "MongoConsoleTab must have TablePagingWidget");
        assertEquals(TablePagingWidget.Placement.BOTTOM_CENTER, tab.getPagingWidget().getPlacement());
    }

    @Test
    public void testQueryTabTableResolutionAndPlacement() {
        ConnectionProfile profile = createPostgresProfile();
        QueryTab tab = new QueryTab(profile, "thunder_db", "console_1.sql");
        tab.setLastExecutedSql("select * from environments;");

        DbObject resolved = tab.resolveCurrentTableObject();
        assertNotNull(resolved);
        assertEquals("environments", resolved.getName());
        assertEquals("thunder_db", resolved.getCatalog());

        assertNotNull(tab.getPagingWidget(), "QueryTab must have TablePagingWidget");
        assertEquals(TablePagingWidget.Placement.BOTTOM_CENTER, tab.getPagingWidget().getPlacement());
    }

    @Test
    public void testChangePageSizeDialog() throws Exception {
        java.util.concurrent.CompletableFuture<Void> future = new java.util.concurrent.CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                ChangePageSizeDialog dialog = new ChangePageSizeDialog(500);
                assertTrue(dialog.getLimitCheckBox().isSelected());
                assertEquals("500", dialog.getSizeField().getText());
                assertFalse(dialog.getSizeField().isDisabled());

                // Unchecking disables size field
                dialog.getLimitCheckBox().setSelected(false);
                assertTrue(dialog.getSizeField().isDisabled());

                // Result converter with unchecked returns 100000 (all/unlimited)
                Integer resUnlimited = dialog.getResultConverter().call(javafx.scene.control.ButtonType.OK);
                assertEquals(100000, resUnlimited);

                // Re-checking and setting custom size
                dialog.getLimitCheckBox().setSelected(true);
                assertFalse(dialog.getSizeField().isDisabled());
                dialog.getSizeField().setText("250");
                Integer resLimited = dialog.getResultConverter().call(javafx.scene.control.ButtonType.OK);
                assertEquals(250, resLimited);

                // Cancel returns null
                assertNull(dialog.getResultConverter().call(javafx.scene.control.ButtonType.CANCEL));
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        future.get(10, java.util.concurrent.TimeUnit.SECONDS);
    }
}
