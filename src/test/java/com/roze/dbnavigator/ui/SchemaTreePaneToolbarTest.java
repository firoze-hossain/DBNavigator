package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.DbObject.Kind;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SchemaTreePaneToolbarTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    private ConnectionProfile createPostgreSqlProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setId("pg-1");
        p.setName("postgres@localhost");
        p.setType(ConnectionProfile.DatabaseType.POSTGRESQL);
        p.setHost("localhost");
        p.setPort(5432);
        p.setDatabase("thunder_db");
        p.setUsername("postgres");
        p.setPassword("secret");
        p.setSavePassword(true);
        return p;
    }

    @Test
    public void testInitialStateNoSelection() {
        SchemaTreePane pane = new SchemaTreePane(null);
        pane.updateToolbarState(null);

        assertTrue(pane.getJumpToConsoleButton().isDisabled(), "Jump to Query Console should be inactive when no selection");
        assertTrue(pane.getGoToDdlButton().isDisabled(), "Go to DDL should be inactive when no selection");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data should be inactive when no selection");
    }

    @Test
    public void testConnectionSelected() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        pane.addConnectionNode(profile);
        TreeItem<DbObject> connItem = pane.getTree().getRoot().getChildren().get(0);

        pane.updateToolbarState(connItem);

        // Connection root: Console is active, DDL is inactive, Edit Data is inactive
        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Query Console should be ACTIVE for connection");
        assertTrue(pane.getGoToDdlButton().isDisabled(), "Go to DDL should be INACTIVE for root connection");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data should be INACTIVE for root connection");
    }

    @Test
    public void testDatabaseSelectedMatchingDataGrip() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        pane.addConnectionNode(profile);

        TreeItem<DbObject> connItem = pane.getTree().getRoot().getChildren().get(0);
        DbObject dbObj = new DbObject("thunder_db", Kind.DATABASE, "thunder_db", null);
        TreeItem<DbObject> dbItem = new TreeItem<>(dbObj);
        connItem.getChildren().add(dbItem);

        pane.updateToolbarState(dbItem);

        // Screenshot 1 & 2: thunder_db selected
        // Jump to Query Console: ACTIVE
        // Go to DDL: ACTIVE
        // Edit Data: INACTIVE
        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Query Console must be ACTIVE for database");
        assertFalse(pane.getGoToDdlButton().isDisabled(), "Go to DDL must be ACTIVE for database");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data must be INACTIVE for database");
    }

    @Test
    public void testSchemaSelectedMatchingDataGrip() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        pane.addConnectionNode(profile);

        TreeItem<DbObject> connItem = pane.getTree().getRoot().getChildren().get(0);
        DbObject dbObj = new DbObject("thunder_db", Kind.DATABASE, "thunder_db", null);
        TreeItem<DbObject> dbItem = new TreeItem<>(dbObj);
        connItem.getChildren().add(dbItem);

        DbObject schemaObj = new DbObject("public", Kind.SCHEMA, "thunder_db", "public");
        TreeItem<DbObject> schemaItem = new TreeItem<>(schemaObj);
        dbItem.getChildren().add(schemaItem);

        pane.updateToolbarState(schemaItem);

        // Screenshot 3: public schema selected
        // Jump to Query Console: ACTIVE
        // Go to DDL: ACTIVE
        // Edit Data: INACTIVE
        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Query Console must be ACTIVE for schema");
        assertFalse(pane.getGoToDdlButton().isDisabled(), "Go to DDL must be ACTIVE for schema");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data must be INACTIVE for schema");
    }

    @Test
    public void testTablesFolderSelectedMatchingDataGrip() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        pane.addConnectionNode(profile);

        TreeItem<DbObject> connItem = pane.getTree().getRoot().getChildren().get(0);
        DbObject dbObj = new DbObject("thunder_db", Kind.DATABASE, "thunder_db", null);
        TreeItem<DbObject> dbItem = new TreeItem<>(dbObj);
        connItem.getChildren().add(dbItem);

        DbObject schemaObj = new DbObject("public", Kind.SCHEMA, "thunder_db", "public");
        TreeItem<DbObject> schemaItem = new TreeItem<>(schemaObj);
        dbItem.getChildren().add(schemaItem);

        DbObject tablesFolderObj = new DbObject("tables", Kind.TABLES_FOLDER, "thunder_db", "public");
        TreeItem<DbObject> folderItem = new TreeItem<>(tablesFolderObj);
        schemaItem.getChildren().add(folderItem);

        pane.updateToolbarState(folderItem);

        // Screenshot 4: tables 24 folder selected
        // Jump to Query Console: ACTIVE
        // Go to DDL: INACTIVE
        // Edit Data: INACTIVE
        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Query Console must be ACTIVE for tables folder");
        assertTrue(pane.getGoToDdlButton().isDisabled(), "Go to DDL must be INACTIVE for tables folder");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data must be INACTIVE for tables folder");
    }

    @Test
    public void testTableSelectedMatchingDataGrip() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        pane.addConnectionNode(profile);

        TreeItem<DbObject> connItem = pane.getTree().getRoot().getChildren().get(0);
        DbObject dbObj = new DbObject("thunder_db", Kind.DATABASE, "thunder_db", null);
        TreeItem<DbObject> dbItem = new TreeItem<>(dbObj);
        connItem.getChildren().add(dbItem);

        DbObject schemaObj = new DbObject("public", Kind.SCHEMA, "thunder_db", "public");
        TreeItem<DbObject> schemaItem = new TreeItem<>(schemaObj);
        dbItem.getChildren().add(schemaItem);

        DbObject tableObj = new DbObject("collections", Kind.TABLE, "thunder_db", "public");
        TreeItem<DbObject> tableItem = new TreeItem<>(tableObj);
        schemaItem.getChildren().add(tableItem);

        pane.updateToolbarState(tableItem);

        // Screenshot 5: collections table selected
        // Jump to Query Console: ACTIVE
        // Go to DDL: ACTIVE
        // Edit Data: ACTIVE
        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Query Console must be ACTIVE for table");
        assertFalse(pane.getGoToDdlButton().isDisabled(), "Go to DDL must be ACTIVE for table");
        assertFalse(pane.getEditDataButton().isDisabled(), "Edit Data must be ACTIVE for table");
    }

    @Test
    public void testViewSelected() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        pane.addConnectionNode(profile);

        TreeItem<DbObject> connItem = pane.getTree().getRoot().getChildren().get(0);
        DbObject viewObj = new DbObject("user_view", Kind.VIEW, "thunder_db", "public");
        TreeItem<DbObject> viewItem = new TreeItem<>(viewObj);
        connItem.getChildren().add(viewItem);

        pane.updateToolbarState(viewItem);

        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Query Console must be ACTIVE for view");
        assertFalse(pane.getGoToDdlButton().isDisabled(), "Go to DDL must be ACTIVE for view");
        assertFalse(pane.getEditDataButton().isDisabled(), "Edit Data must be ACTIVE for view");
    }

    @Test
    public void testRoutineSelected() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        pane.addConnectionNode(profile);

        TreeItem<DbObject> connItem = pane.getTree().getRoot().getChildren().get(0);
        DbObject funcObj = new DbObject("calculate_tax", Kind.FUNCTION, "thunder_db", "public");
        TreeItem<DbObject> funcItem = new TreeItem<>(funcObj);
        connItem.getChildren().add(funcItem);

        pane.updateToolbarState(funcItem);

        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Query Console must be ACTIVE for routine");
        assertFalse(pane.getGoToDdlButton().isDisabled(), "Go to DDL must be ACTIVE for routine");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data must be INACTIVE for routine");
    }

    @Test
    public void testSequenceSelected() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        pane.addConnectionNode(profile);

        TreeItem<DbObject> connItem = pane.getTree().getRoot().getChildren().get(0);
        DbObject seqObj = new DbObject("order_id_seq", Kind.SEQUENCE, "thunder_db", "public");
        TreeItem<DbObject> seqItem = new TreeItem<>(seqObj);
        connItem.getChildren().add(seqItem);

        pane.updateToolbarState(seqItem);

        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Query Console must be ACTIVE for sequence");
        assertFalse(pane.getGoToDdlButton().isDisabled(), "Go to DDL must be ACTIVE for sequence");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data must be INACTIVE for sequence");
    }

    @Test
    public void testScrollFromEditorButtonExists() {
        SchemaTreePane pane = new SchemaTreePane(null);
        assertNotNull(pane.getScrollFromEditorButton(), "Scroll from Editor button must exist");
        assertNotNull(pane.getScrollFromEditorButton().getTooltip(), "Tooltip must be present");
        assertEquals("Scroll from Editor", pane.getScrollFromEditorButton().getTooltip().getText(),
                "Tooltip text must match DataGrip exactly");
    }

    @Test
    public void testSelectObjectInTreePostgreSQLHierarchy() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        pane.addConnectionNode(profile);

        TreeItem<DbObject> connItem = pane.findConnectionItem(profile);
        assertNotNull(connItem);
        connItem.getChildren().clear(); // Clear loadingNode

        DbObject dbObj = new DbObject("thunder_db", Kind.DATABASE, "thunder_db", null);
        TreeItem<DbObject> dbItem = new TreeItem<>(dbObj);
        connItem.getChildren().add(dbItem);

        DbObject schemaObj = new DbObject("public", Kind.SCHEMA, "thunder_db", "public");
        TreeItem<DbObject> schemaItem = new TreeItem<>(schemaObj);
        dbItem.getChildren().add(schemaItem);

        DbObject tablesFolderObj = new DbObject("tables", Kind.TABLES_FOLDER, "thunder_db", "public");
        TreeItem<DbObject> folderItem = new TreeItem<>(tablesFolderObj);
        schemaItem.getChildren().add(folderItem);

        DbObject tableObj = new DbObject("collections", Kind.TABLE, "thunder_db", "public");
        TreeItem<DbObject> tableItem = new TreeItem<>(tableObj);
        folderItem.getChildren().add(tableItem);

        // Before selection: initially collapsed
        connItem.setExpanded(false);
        dbItem.setExpanded(false);
        schemaItem.setExpanded(false);
        folderItem.setExpanded(false);

        // Invoke selectObjectInTree
        pane.selectObjectInTree(profile, tableObj);

        // Verify expansion down the hierarchy
        assertTrue(connItem.isExpanded(), "Connection node must be expanded");
        assertTrue(dbItem.isExpanded(), "Database node must be expanded");
        assertTrue(schemaItem.isExpanded(), "Schema node must be expanded");
        assertTrue(folderItem.isExpanded(), "Tables folder must be expanded");

        // Verify selection
        assertEquals(tableItem, pane.getSelectedItem(), "Target table collections must be selected");

        // Verify toolbar state is updated: Jump to Console, Go to DDL, Edit Data must all be ACTIVE!
        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Console must be ACTIVE for selected table");
        assertFalse(pane.getGoToDdlButton().isDisabled(), "Go to DDL must be ACTIVE for selected table");
        assertFalse(pane.getEditDataButton().isDisabled(), "Edit Data must be ACTIVE for selected table");
    }

    @Test
    public void testSelectObjectInTreeMySQLHierarchy() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = new ConnectionProfile();
        profile.setId("mysql-1");
        profile.setName("mysql@localhost");
        profile.setType(ConnectionProfile.DatabaseType.MYSQL);
        profile.setDatabase("mydb");
        profile.setUsername("root");
        profile.setPassword("secret");
        profile.setSavePassword(true);
        pane.addConnectionNode(profile);

        TreeItem<DbObject> connItem = pane.findConnectionItem(profile);
        assertNotNull(connItem);
        connItem.getChildren().clear();

        DbObject dbObj = new DbObject("mydb", Kind.DATABASE, "mydb", null);
        TreeItem<DbObject> dbItem = new TreeItem<>(dbObj);
        connItem.getChildren().add(dbItem);

        DbObject tablesFolderObj = new DbObject("Tables", Kind.TABLES_FOLDER, "mydb", null);
        TreeItem<DbObject> folderItem = new TreeItem<>(tablesFolderObj);
        dbItem.getChildren().add(folderItem);

        DbObject tableObj = new DbObject("users", Kind.TABLE, "mydb", null);
        TreeItem<DbObject> tableItem = new TreeItem<>(tableObj);
        folderItem.getChildren().add(tableItem);

        pane.selectObjectInTree(profile, tableObj);

        assertTrue(connItem.isExpanded());
        assertTrue(dbItem.isExpanded());
        assertTrue(folderItem.isExpanded());
        assertEquals(tableItem, pane.getSelectedItem());
        assertFalse(pane.getEditDataButton().isDisabled());
    }

    @Test
    public void testSelectObjectInTreeDatabaseOnly() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        pane.addConnectionNode(profile);

        TreeItem<DbObject> connItem = pane.findConnectionItem(profile);
        assertNotNull(connItem);
        connItem.getChildren().clear();

        DbObject dbObj = new DbObject("thunder_db", Kind.DATABASE, "thunder_db", null);
        TreeItem<DbObject> dbItem = new TreeItem<>(dbObj);
        connItem.getChildren().add(dbItem);

        pane.selectObjectInTree(profile, dbObj);

        assertTrue(connItem.isExpanded());
        assertEquals(dbItem, pane.getSelectedItem());
        assertFalse(pane.getJumpToConsoleButton().isDisabled());
        assertFalse(pane.getGoToDdlButton().isDisabled());
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data must be INACTIVE for database");
    }
}
