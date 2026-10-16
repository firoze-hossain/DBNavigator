package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.DbObject.Kind;
import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    private Menu findMenu(ContextMenu menu, String text) {
        for (MenuItem item : menu.getItems()) {
            if (item instanceof Menu m && m.getText() != null && m.getText().startsWith(text)) {
                return m;
            }
        }
        return null;
    }

    private MenuItem findItem(ContextMenu menu, String text) {
        if (menu == null) return null;
        for (MenuItem item : menu.getItems()) {
            if (item.getText() != null && item.getText().startsWith(text)) {
                return item;
            }
        }
        return null;
    }

    private boolean menuHasItem(Menu parent, String text) {
        if (parent == null) return false;
        for (MenuItem item : parent.getItems()) {
            if (item.getText() != null && item.getText().startsWith(text)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testPostgreSQLContextMenuDynamicOptions() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject connObj = new DbObject(profile.getName(), Kind.CONNECTION);
        ContextMenu menu = pane.buildConnectionMenu(null, connObj, profile);
        assertNotNull(menu);

        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu, "New menu must exist");
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertTrue(menuHasItem(newMenu, "Database"), "PostgreSQL must have Database in New menu");
        assertTrue(menuHasItem(newMenu, "Role"), "PostgreSQL must have Role in New menu");
        assertTrue(menuHasItem(newMenu, "User"), "PostgreSQL must have User in New menu");
        assertFalse(menuHasItem(newMenu, "Schema"), "PostgreSQL must NOT have Schema in root New menu");

        Menu objActions = findMenu(menu, "Object Actions");
        assertNotNull(objActions);
        assertTrue(menuHasItem(objActions, "Show / Hide Databases"));
        assertFalse(menuHasItem(objActions, "Show / Hide Schemas"));

        Menu tools = findMenu(menu, "Tools");
        assertNotNull(tools);
        assertTrue(menuHasItem(tools, "Manage Databases"));
        assertTrue(menuHasItem(tools, "Verify Connection"));

        Menu impExp = findMenu(menu, "Import/Export");
        assertNotNull(impExp);
        assertTrue(menuHasItem(impExp, "Dump with pg_dump"));

        assertEquals("database", SchemaTreePane.schemaOrDatabaseNoun(profile.getType()));
    }

    @Test
    public void testMySQLContextMenuDynamicOptions() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = new ConnectionProfile();
        profile.setId("mysql-1");
        profile.setName("mysql@localhost");
        profile.setType(ConnectionProfile.DatabaseType.MYSQL);
        DbObject connObj = new DbObject(profile.getName(), Kind.CONNECTION);
        ContextMenu menu = pane.buildConnectionMenu(null, connObj, profile);
        assertNotNull(menu);

        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Schema"), "MySQL must have Schema in New menu");
        assertTrue(menuHasItem(newMenu, "User"), "MySQL must have User in New menu");
        assertTrue(menuHasItem(newMenu, "Role"), "MySQL must have Role in New menu");
        assertFalse(menuHasItem(newMenu, "Database"), "MySQL must NOT have Database in New menu");

        Menu objActions = findMenu(menu, "Object Actions");
        assertNotNull(objActions);
        assertTrue(menuHasItem(objActions, "Show / Hide Schemas"));
        assertFalse(menuHasItem(objActions, "Show / Hide Databases"));

        Menu tools = findMenu(menu, "Tools");
        assertNotNull(tools);
        assertTrue(menuHasItem(tools, "Manage Schemas"));
        assertTrue(menuHasItem(tools, "Verify Connection"));

        Menu impExp = findMenu(menu, "Import/Export");
        assertNotNull(impExp);
        assertTrue(menuHasItem(impExp, "Dump with mysqldump"));

        assertEquals("schema", SchemaTreePane.schemaOrDatabaseNoun(profile.getType()));
    }

    @Test
    public void testMariaDBContextMenuDynamicOptions() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = new ConnectionProfile();
        profile.setId("maria-1");
        profile.setName("mariadb@localhost");
        profile.setType(ConnectionProfile.DatabaseType.MARIADB);
        DbObject connObj = new DbObject(profile.getName(), Kind.CONNECTION);
        ContextMenu menu = pane.buildConnectionMenu(null, connObj, profile);
        assertNotNull(menu);

        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Schema"), "MariaDB must have Schema in New menu");
        assertTrue(menuHasItem(newMenu, "User"), "MariaDB must have User in New menu");
        assertTrue(menuHasItem(newMenu, "Role"), "MariaDB must have Role in New menu");
        assertFalse(menuHasItem(newMenu, "Database"), "MariaDB must NOT have Database in New menu");

        Menu objActions = findMenu(menu, "Object Actions");
        assertNotNull(objActions);
        assertTrue(menuHasItem(objActions, "Show / Hide Schemas"));
        assertFalse(menuHasItem(objActions, "Show / Hide Databases"));

        Menu tools = findMenu(menu, "Tools");
        assertNotNull(tools);
        assertTrue(menuHasItem(tools, "Manage Schemas"));

        Menu impExp = findMenu(menu, "Import/Export");
        assertNotNull(impExp);
        assertTrue(menuHasItem(impExp, "Dump with mariadb-dump"));

        assertEquals("schema", SchemaTreePane.schemaOrDatabaseNoun(profile.getType()));
    }

    @Test
    public void testMongoDBContextMenuDynamicOptions() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = new ConnectionProfile();
        profile.setId("mongo-1");
        profile.setName("mongo@localhost");
        profile.setType(ConnectionProfile.DatabaseType.MONGODB);
        DbObject connObj = new DbObject(profile.getName(), Kind.CONNECTION);
        ContextMenu menu = pane.buildConnectionMenu(null, connObj, profile);
        assertNotNull(menu);

        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertFalse(menuHasItem(newMenu, "Schema"), "MongoDB must NOT have Schema in New menu");
        assertFalse(menuHasItem(newMenu, "Database"), "MongoDB must NOT have Database in New menu");
        assertFalse(menuHasItem(newMenu, "Role"), "MongoDB must NOT have Role in New menu");
        assertFalse(menuHasItem(newMenu, "User"), "MongoDB must NOT have User in New menu");
        assertTrue(menuHasItem(newMenu, "Virtual View"));
        assertTrue(menuHasItem(newMenu, "Data Source"));

        Menu objActions = findMenu(menu, "Object Actions");
        assertNotNull(objActions);
        assertTrue(menuHasItem(objActions, "Show / Hide Databases"));

        Menu tools = findMenu(menu, "Tools");
        assertNotNull(tools);
        assertTrue(menuHasItem(tools, "Manage Databases"));

        Menu impExp = findMenu(menu, "Import/Export");
        assertNotNull(impExp);
        assertTrue(menuHasItem(impExp, "Dump with mongodump"));

        assertEquals("database", SchemaTreePane.schemaOrDatabaseNoun(profile.getType()));
    }

    @Test
    public void testSQLiteContextMenuDynamicOptions() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = new ConnectionProfile();
        profile.setId("sqlite-1");
        profile.setName("sqlite_local");
        profile.setType(ConnectionProfile.DatabaseType.SQLITE);
        DbObject connObj = new DbObject(profile.getName(), Kind.CONNECTION);
        ContextMenu menu = pane.buildConnectionMenu(null, connObj, profile);
        assertNotNull(menu);

        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertFalse(menuHasItem(newMenu, "Schema"), "SQLite must NOT have Schema in New menu");
        assertFalse(menuHasItem(newMenu, "Database"), "SQLite must NOT have Database in New menu");
        assertFalse(menuHasItem(newMenu, "Role"), "SQLite must NOT have Role in New menu");
        assertFalse(menuHasItem(newMenu, "User"), "SQLite must NOT have User in New menu");

        Menu objActions = findMenu(menu, "Object Actions");
        assertNotNull(objActions);
        assertFalse(menuHasItem(objActions, "Show / Hide Databases"), "SQLite has no show/hide databases");
        assertTrue(menuHasItem(objActions, "Forget Saved Passwords"));

        Menu tools = findMenu(menu, "Tools");
        assertNotNull(tools);
        assertFalse(menuHasItem(tools, "Manage Databases"), "SQLite has no manage databases");
        assertTrue(menuHasItem(tools, "Verify Connection"));

        Menu impExp = findMenu(menu, "Import/Export");
        assertNotNull(impExp);
        assertTrue(menuHasItem(impExp, "Export SQLite Database"));
    }

    @Test
    public void testOracleAndSqlServerNouns() {
        assertEquals("schema", SchemaTreePane.schemaOrDatabaseNoun(ConnectionProfile.DatabaseType.ORACLE));
        assertEquals("database", SchemaTreePane.schemaOrDatabaseNoun(ConnectionProfile.DatabaseType.SQLSERVER));
    }

    @Test
    public void testSqlServerContextMenuDynamicOptions() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = new ConnectionProfile();
        profile.setId("mssql-1");
        profile.setName("sqlserver@localhost");
        profile.setType(ConnectionProfile.DatabaseType.SQLSERVER);
        DbObject connObj = new DbObject(profile.getName(), Kind.CONNECTION);
        ContextMenu menu = pane.buildConnectionMenu(null, connObj, profile);
        assertNotNull(menu);

        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertTrue(menuHasItem(newMenu, "Database"), "SQL Server must have Database in New menu");
        assertTrue(menuHasItem(newMenu, "Server Principal"), "SQL Server must have Server Principal in New menu");
        assertFalse(menuHasItem(newMenu, "Schema"), "SQL Server root must NOT have Schema in New menu");
        assertFalse(menuHasItem(newMenu, "Role"), "SQL Server root must NOT have Role in New menu");
        assertFalse(menuHasItem(newMenu, "User"), "SQL Server root must NOT have generic User in New menu");

        Menu objActions = findMenu(menu, "Object Actions");
        assertNotNull(objActions, "SQL Server must have Object Actions menu");
        assertTrue(menuHasItem(objActions, "Show / Hide Databases"));
        assertTrue(menuHasItem(objActions, "Forget Saved Passwords"));

        Menu tools = findMenu(menu, "Tools");
        assertNotNull(tools);
        assertTrue(menuHasItem(tools, "Manage Databases"));
        assertTrue(menuHasItem(tools, "Verify Connection"));

        Menu impExp = findMenu(menu, "Import/Export");
        assertNotNull(impExp);
        assertTrue(menuHasItem(impExp, "Export Database with bcp"));
        assertTrue(menuHasItem(impExp, "Restore"));

        assertEquals("database", SchemaTreePane.schemaOrDatabaseNoun(profile.getType()));
    }

    @Test
    public void testOracleContextMenuDynamicOptions() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = new ConnectionProfile();
        profile.setId("oracle-1");
        profile.setName("oracle@localhost");
        profile.setType(ConnectionProfile.DatabaseType.ORACLE);
        DbObject connObj = new DbObject(profile.getName(), Kind.CONNECTION);
        ContextMenu menu = pane.buildConnectionMenu(null, connObj, profile);
        assertNotNull(menu);

        // In DataGrip Image 2: Oracle has "Download Driver Files" at the very top of menu
        assertTrue(menu.getItems().stream().anyMatch(it -> it.getText() != null && it.getText().startsWith("Download Driver Files")),
                "Oracle menu must start with Download Driver Files");

        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        // Oracle has neither Database, Schema, User, nor Role at root New menu
        assertFalse(menuHasItem(newMenu, "Database"), "Oracle root New menu must NOT have Database");
        assertFalse(menuHasItem(newMenu, "Schema"), "Oracle root New menu must NOT have Schema");
        assertFalse(menuHasItem(newMenu, "User"), "Oracle root New menu must NOT have User");
        assertFalse(menuHasItem(newMenu, "Role"), "Oracle root New menu must NOT have Role");
        assertTrue(menuHasItem(newMenu, "Virtual View"));
        assertTrue(menuHasItem(newMenu, "Data Source"));

        // In DataGrip Image 2: Object Actions is omitted on Oracle root
        Menu objActions = findMenu(menu, "Object Actions");
        assertNull(objActions, "Oracle connection menu must NOT have Object Actions menu");

        Menu tools = findMenu(menu, "Tools");
        assertNotNull(tools);
        assertTrue(menuHasItem(tools, "Manage Schemas"));
        assertTrue(menuHasItem(tools, "Verify Connection"));

        Menu impExp = findMenu(menu, "Import/Export");
        assertNotNull(impExp);
        assertTrue(menuHasItem(impExp, "Export with expdp"));
        assertTrue(menuHasItem(impExp, "Restore"));

        assertEquals("schema", SchemaTreePane.schemaOrDatabaseNoun(profile.getType()));
    }

    @Test
    public void testDatabaseIconsGeneratedForAllEngines() {
        for (ConnectionProfile.DatabaseType type : ConnectionProfile.DatabaseType.values()) {
            javafx.scene.Node icon16Connected = DatabaseIcon.create(type, 16, true);
            assertNotNull(icon16Connected, "Vector icon must be created for " + type + " (connected)");

            javafx.scene.Node icon16Disconnected = DatabaseIcon.create(type, 16, false);
            assertNotNull(icon16Disconnected, "Vector icon must be created for " + type + " (disconnected)");

            javafx.scene.Node iconFromIconsHelper = Icons.forDatabase(type, 16, true);
            assertNotNull(iconFromIconsHelper, "Icons.forDatabase helper must return icon for " + type);
        }
    }

    @Test
    public void testPostgresServerObjectsHierarchyAndToolbar() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        pane.addConnectionNode(profile);

        TreeItem<DbObject> connItem = pane.findConnectionItem(profile);
        assertNotNull(connItem);
        connItem.getChildren().clear();

        // 1. Database nodes and Server Objects node matching DataGrip Image 1
        DbObject dbObj = new DbObject("thunder_db", Kind.DATABASE, "thunder_db", null);
        TreeItem<DbObject> dbItem = new TreeItem<>(dbObj);
        DbObject serverObjectsObj = new DbObject("Server Objects", Kind.SERVER_OBJECTS);
        TreeItem<DbObject> serverObjectsItem = new TreeItem<>(serverObjectsObj);
        connItem.getChildren().addAll(dbItem, serverObjectsItem);

        // Verify Server Objects toolbar state
        pane.updateToolbarState(serverObjectsItem);
        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Console must be ACTIVE for Server Objects");
        assertTrue(pane.getGoToDdlButton().isDisabled(), "Go to DDL must be INACTIVE for Server Objects container");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data must be INACTIVE for Server Objects container");

        // 2. roles and tablespaces folders matching DataGrip Image 2
        DbObject rolesFolderObj = new DbObject("roles", Kind.ROLES_FOLDER);
        rolesFolderObj.setDetail("16");
        TreeItem<DbObject> rolesFolderItem = new TreeItem<>(rolesFolderObj);

        DbObject tablespacesFolderObj = new DbObject("tablespaces", Kind.TABLESPACES_FOLDER);
        tablespacesFolderObj.setDetail("2");
        TreeItem<DbObject> tablespacesFolderItem = new TreeItem<>(tablespacesFolderObj);

        serverObjectsItem.getChildren().addAll(rolesFolderItem, tablespacesFolderItem);

        // Verify roles folder toolbar state
        pane.updateToolbarState(rolesFolderItem);
        assertFalse(pane.getJumpToConsoleButton().isDisabled());
        assertTrue(pane.getGoToDdlButton().isDisabled());
        assertTrue(pane.getEditDataButton().isDisabled());

        // 3. Specific role and tablespace items matching DataGrip Image 3
        DbObject roleObj = new DbObject("pg_checkpoint", Kind.ROLE);
        TreeItem<DbObject> roleItem = new TreeItem<>(roleObj);
        rolesFolderItem.getChildren().add(roleItem);

        DbObject tsObj = new DbObject("pg_default", Kind.TABLESPACE);
        TreeItem<DbObject> tsItem = new TreeItem<>(tsObj);
        tablespacesFolderItem.getChildren().add(tsItem);

        // Verify role toolbar state: Jump to Console ACTIVE, Go to DDL ACTIVE, Edit Data INACTIVE!
        pane.updateToolbarState(roleItem);
        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Console must be ACTIVE for role");
        assertFalse(pane.getGoToDdlButton().isDisabled(), "Go to DDL must be ACTIVE for role (Image 4)");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data must be INACTIVE for role");

        // Verify tablespace toolbar state: Jump to Console ACTIVE, Go to DDL ACTIVE, Edit Data INACTIVE!
        pane.updateToolbarState(tsItem);
        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Console must be ACTIVE for tablespace");
        assertFalse(pane.getGoToDdlButton().isDisabled(), "Go to DDL must be ACTIVE for tablespace (Image 5)");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data must be INACTIVE for tablespace");
    }

    @Test
    public void testServerObjectsNavigationAndSelection() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        pane.addConnectionNode(profile);

        TreeItem<DbObject> connItem = pane.findConnectionItem(profile);
        assertNotNull(connItem);
        connItem.getChildren().clear();

        DbObject serverObjectsObj = new DbObject("Server Objects", Kind.SERVER_OBJECTS);
        TreeItem<DbObject> serverObjectsItem = new TreeItem<>(serverObjectsObj);
        connItem.getChildren().add(serverObjectsItem);

        DbObject rolesFolderObj = new DbObject("roles", Kind.ROLES_FOLDER);
        TreeItem<DbObject> rolesFolderItem = new TreeItem<>(rolesFolderObj);
        DbObject tsFolderObj = new DbObject("tablespaces", Kind.TABLESPACES_FOLDER);
        TreeItem<DbObject> tsFolderItem = new TreeItem<>(tsFolderObj);
        serverObjectsItem.getChildren().addAll(rolesFolderItem, tsFolderItem);

        DbObject roleObj = new DbObject("pg_checkpoint", Kind.ROLE);
        TreeItem<DbObject> roleItem = new TreeItem<>(roleObj);
        rolesFolderItem.getChildren().add(roleItem);

        DbObject tsObj = new DbObject("pg_default", Kind.TABLESPACE);
        TreeItem<DbObject> tsItem = new TreeItem<>(tsObj);
        tsFolderItem.getChildren().add(tsItem);

        // Select role pg_checkpoint in tree
        pane.selectObjectInTree(profile, roleObj);
        assertTrue(connItem.isExpanded());
        assertTrue(serverObjectsItem.isExpanded());
        assertTrue(rolesFolderItem.isExpanded());
        assertEquals(roleItem, pane.getSelectedItem());

        // Select tablespace pg_default in tree
        pane.selectObjectInTree(profile, tsObj);
        assertTrue(tsFolderItem.isExpanded());
        assertEquals(tsItem, pane.getSelectedItem());
    }

    @Test
    public void testServerObjectsDdlGeneration() throws Exception {
        ConnectionProfile profile = createPostgreSqlProfile();

        // 1. Role DDL matching Image 4
        DbObject role = new DbObject("pg_checkpoint", Kind.ROLE);
        String roleDdl = com.roze.dbnavigator.db.DdlGeneratorService.generateDdl(profile, role);
        assertNotNull(roleDdl);
        assertTrue(roleDdl.contains("-- auto-generated definition"), "Must contain auto-generated definition header");
        assertTrue(roleDdl.contains("create role pg_checkpoint"), "Must contain create role pg_checkpoint matching Image 4");

        // 2. Tablespace DDL matching Image 5
        DbObject tablespace = new DbObject("pg_default", Kind.TABLESPACE);
        String tsDdl = com.roze.dbnavigator.db.DdlGeneratorService.generateDdl(profile, tablespace);
        assertNotNull(tsDdl);
        assertTrue(tsDdl.contains("-- auto-generated definition"));
        assertTrue(tsDdl.contains("create tablespace pg_default owner postgres location ???;"),
                "Must contain exact DataGrip definition matching Image 5");
    }

    @Test
    public void testServerObjectsIcons() {
        ConnectionProfile profile = createPostgreSqlProfile();

        assertNotNull(Icons.forObject(new DbObject("Server Objects", Kind.SERVER_OBJECTS), profile, true));
        assertNotNull(Icons.forObject(new DbObject("roles", Kind.ROLES_FOLDER), profile, true));
        assertNotNull(Icons.forObject(new DbObject("tablespaces", Kind.TABLESPACES_FOLDER), profile, true));
        assertNotNull(Icons.forObject(new DbObject("pg_checkpoint", Kind.ROLE), profile, true));
        assertNotNull(Icons.forObject(new DbObject("pg_default", Kind.TABLESPACE), profile, true));
    }

    @Test
    public void testDatabaseObjectsAndVirtualViewsHierarchyAndToolbarStates() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        pane.addConnectionNode(profile);

        TreeItem<DbObject> connItem = pane.getTree().getRoot().getChildren().get(0);
        DbObject dbObj = new DbObject("thunder_db", Kind.DATABASE, "thunder_db", null);
        TreeItem<DbObject> dbItem = new TreeItem<>(dbObj);
        connItem.getChildren().add(dbItem);

        // 1. Database Objects folder
        DbObject dbObjectsObj = new DbObject("Database Objects", Kind.DATABASE_OBJECTS, "thunder_db", null);
        TreeItem<DbObject> dbObjectsItem = new TreeItem<>(dbObjectsObj);
        dbItem.getChildren().add(dbObjectsItem);

        pane.updateToolbarState(dbObjectsItem);
        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Console ACTIVE for Database Objects");
        assertTrue(pane.getGoToDdlButton().isDisabled(), "Go to DDL INACTIVE for Database Objects folder");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data INACTIVE for Database Objects folder");

        // 2. access methods folder and item
        DbObject amFolderObj = new DbObject("access methods", Kind.ACCESS_METHODS_FOLDER, "thunder_db", null);
        amFolderObj.setDetail("7");
        TreeItem<DbObject> amFolderItem = new TreeItem<>(amFolderObj);
        dbObjectsItem.getChildren().add(amFolderItem);

        pane.updateToolbarState(amFolderItem);
        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Console ACTIVE for access methods folder");
        assertTrue(pane.getGoToDdlButton().isDisabled(), "Go to DDL INACTIVE for access methods folder");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data INACTIVE for access methods folder");

        DbObject brinObj = new DbObject("brin", Kind.ACCESS_METHOD, "thunder_db", null);
        TreeItem<DbObject> brinItem = new TreeItem<>(brinObj);
        amFolderItem.getChildren().add(brinItem);

        pane.updateToolbarState(brinItem);
        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Console ACTIVE for brin");
        assertFalse(pane.getGoToDdlButton().isDisabled(), "Go to DDL ACTIVE for brin matching Image 2");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data INACTIVE for brin");

        // 3. casts folder and item
        DbObject castsFolderObj = new DbObject("casts", Kind.CASTS_FOLDER, "thunder_db", null);
        castsFolderObj.setDetail("229");
        TreeItem<DbObject> castsFolderItem = new TreeItem<>(castsFolderObj);
        dbObjectsItem.getChildren().add(castsFolderItem);

        pane.updateToolbarState(castsFolderItem);
        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Console ACTIVE for casts folder");
        assertTrue(pane.getGoToDdlButton().isDisabled(), "Go to DDL INACTIVE for casts folder");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data INACTIVE for casts folder");

        DbObject castObj = new DbObject("bit as int8", Kind.CAST, "thunder_db", null);
        TreeItem<DbObject> castItem = new TreeItem<>(castObj);
        castsFolderItem.getChildren().add(castItem);

        pane.updateToolbarState(castItem);
        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Console ACTIVE for bit as int8");
        assertFalse(pane.getGoToDdlButton().isDisabled(), "Go to DDL ACTIVE for bit as int8 matching Image 3");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data INACTIVE for bit as int8");

        // 4. extensions folder and item
        DbObject extFolderObj = new DbObject("extensions", Kind.EXTENSIONS_FOLDER, "thunder_db", null);
        extFolderObj.setDetail("1");
        TreeItem<DbObject> extFolderItem = new TreeItem<>(extFolderObj);
        dbObjectsItem.getChildren().add(extFolderItem);

        pane.updateToolbarState(extFolderItem);
        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Console ACTIVE for extensions folder");
        assertTrue(pane.getGoToDdlButton().isDisabled(), "Go to DDL INACTIVE for extensions folder");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data INACTIVE for extensions folder");

        DbObject extObj = new DbObject("plpgsql", Kind.EXTENSION, "thunder_db", null);
        TreeItem<DbObject> extItem = new TreeItem<>(extObj);
        extFolderItem.getChildren().add(extItem);

        pane.updateToolbarState(extItem);
        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Console ACTIVE for plpgsql");
        assertFalse(pane.getGoToDdlButton().isDisabled(), "Go to DDL ACTIVE for plpgsql matching Image 4");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data INACTIVE for plpgsql");

        // 5. languages folder and item
        DbObject langFolderObj = new DbObject("languages", Kind.LANGUAGES_FOLDER, "thunder_db", null);
        langFolderObj.setDetail("4");
        TreeItem<DbObject> langFolderItem = new TreeItem<>(langFolderObj);
        dbObjectsItem.getChildren().add(langFolderItem);

        pane.updateToolbarState(langFolderItem);
        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Console ACTIVE for languages folder");
        assertTrue(pane.getGoToDdlButton().isDisabled(), "Go to DDL INACTIVE for languages folder");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data INACTIVE for languages folder");

        DbObject langObj = new DbObject("c", Kind.LANGUAGE, "thunder_db", null);
        TreeItem<DbObject> langItem = new TreeItem<>(langObj);
        langFolderItem.getChildren().add(langItem);

        pane.updateToolbarState(langItem);
        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Console ACTIVE for c");
        assertFalse(pane.getGoToDdlButton().isDisabled(), "Go to DDL ACTIVE for c matching Image 5");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data INACTIVE for c");

        // 6. virtual views folder and sessions item
        DbObject vvFolderObj = new DbObject("virtual views", Kind.VIRTUAL_VIEWS_FOLDER, "thunder_db", null);
        vvFolderObj.setDetail("1");
        TreeItem<DbObject> vvFolderItem = new TreeItem<>(vvFolderObj);
        dbItem.getChildren().add(vvFolderItem);

        pane.updateToolbarState(vvFolderItem);
        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Console ACTIVE for virtual views folder");
        assertTrue(pane.getGoToDdlButton().isDisabled(), "Go to DDL INACTIVE for virtual views folder");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data INACTIVE for virtual views folder");

        DbObject sessionsObj = new DbObject("sessions", Kind.VIRTUAL_VIEW, "thunder_db", null);
        TreeItem<DbObject> sessionsItem = new TreeItem<>(sessionsObj);
        vvFolderItem.getChildren().add(sessionsItem);

        pane.updateToolbarState(sessionsItem);
        // sessions virtual view: ALL THREE ACTIVE! Jump to Console, Go to DDL, and Edit Data!
        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Console ACTIVE for sessions virtual view");
        assertFalse(pane.getGoToDdlButton().isDisabled(), "Go to DDL ACTIVE for sessions virtual view");
        assertFalse(pane.getEditDataButton().isDisabled(), "Edit Data ACTIVE for sessions virtual view");
    }

    @Test
    public void testDatabaseObjectsAndVirtualViewsDdlGeneration() throws Exception {
        ConnectionProfile profile = createPostgreSqlProfile();

        // 1. Access Method DDL matching Image 2
        DbObject am = new DbObject("brin", Kind.ACCESS_METHOD, "thunder_db", null);
        String amDdl = com.roze.dbnavigator.db.DdlGeneratorService.generateDdl(profile, am);
        assertNotNull(amDdl);
        assertTrue(amDdl.contains("-- access method brin"), "Must contain access method header");
        assertTrue(amDdl.contains("create access method brin type index handler brinhandler;"),
                "Must match create access method statement in Image 2");

        // 2. Cast DDL matching Image 3
        DbObject cast = new DbObject("bit as int8", Kind.CAST, "thunder_db", null);
        String castDdl = com.roze.dbnavigator.db.DdlGeneratorService.generateDdl(profile, cast);
        assertNotNull(castDdl);
        assertTrue(castDdl.contains("-- cast bit as int8"), "Must contain cast header");
        assertTrue(castDdl.contains("create cast (bit as int8) with function int8(bit);"),
                "Must match create cast statement in Image 3");

        // 3. Extension DDL matching Image 4
        DbObject ext = new DbObject("plpgsql", Kind.EXTENSION, "thunder_db", null);
        String extDdl = com.roze.dbnavigator.db.DdlGeneratorService.generateDdl(profile, ext);
        assertNotNull(extDdl);
        assertTrue(extDdl.contains("-- extension plpgsql"), "Must contain extension header");
        assertTrue(extDdl.contains("create extension plpgsql schema pg_catalog version '1.0';"),
                "Must match create extension statement in Image 4");

        // 4. Language DDL matching Image 5
        DbObject lang = new DbObject("c", Kind.LANGUAGE, "thunder_db", null);
        String langDdl = com.roze.dbnavigator.db.DdlGeneratorService.generateDdl(profile, lang);
        assertNotNull(langDdl);
        assertTrue(langDdl.contains("-- language c"), "Must contain language header");
        assertTrue(langDdl.contains("create language c validator pg_catalog.fmgr_c_validator;"),
                "Must match create language statement in Image 5");

        // 5. Virtual View DDL matching Image 1
        DbObject sessions = new DbObject("sessions", Kind.VIRTUAL_VIEW, "thunder_db", null);
        String sessionsDdl = com.roze.dbnavigator.db.DdlGeneratorService.generateDdl(profile, sessions);
        assertNotNull(sessionsDdl);
        assertTrue(sessionsDdl.contains("-- virtual view thunder_db.sessions"), "Must contain virtual view header");
        assertTrue(sessionsDdl.contains("create virtual view sessions as"), "Must contain create virtual view");
        assertTrue(sessionsDdl.contains("from pg_stat_activity;"), "Must select from pg_stat_activity matching Image 1");
    }

    @Test
    public void testDatabaseObjectsAndVirtualViewsIcons() {
        ConnectionProfile profile = createPostgreSqlProfile();

        assertNotNull(Icons.forObject(new DbObject("Database Objects", Kind.DATABASE_OBJECTS), profile, true));
        assertNotNull(Icons.forObject(new DbObject("access methods", Kind.ACCESS_METHODS_FOLDER), profile, true));
        assertNotNull(Icons.forObject(new DbObject("brin", Kind.ACCESS_METHOD), profile, true));
        assertNotNull(Icons.forObject(new DbObject("casts", Kind.CASTS_FOLDER), profile, true));
        assertNotNull(Icons.forObject(new DbObject("bit as int8", Kind.CAST), profile, true));
        assertNotNull(Icons.forObject(new DbObject("extensions", Kind.EXTENSIONS_FOLDER), profile, true));
        assertNotNull(Icons.forObject(new DbObject("plpgsql", Kind.EXTENSION), profile, true));
        assertNotNull(Icons.forObject(new DbObject("languages", Kind.LANGUAGES_FOLDER), profile, true));
        assertNotNull(Icons.forObject(new DbObject("c", Kind.LANGUAGE), profile, true));
        assertNotNull(Icons.forObject(new DbObject("virtual views", Kind.VIRTUAL_VIEWS_FOLDER), profile, true));
        assertNotNull(Icons.forObject(new DbObject("sessions", Kind.VIRTUAL_VIEW), profile, true));
    }

    @Test
    public void testDatabaseObjectsAndVirtualViewsScrollFromEditor() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        pane.addConnectionNode(profile);

        TreeItem<DbObject> connItem = pane.findConnectionItem(profile);
        assertNotNull(connItem);
        connItem.getChildren().clear();
        connItem.getValue().setLoaded(true);

        DbObject dbObj = new DbObject("thunder_db", Kind.DATABASE, "thunder_db", null);
        dbObj.setLoaded(true);
        TreeItem<DbObject> dbItem = new TreeItem<>(dbObj);
        connItem.getChildren().add(dbItem);

        DbObject dbObjectsObj = new DbObject("Database Objects", Kind.DATABASE_OBJECTS, "thunder_db", null);
        dbObjectsObj.setLoaded(true);
        TreeItem<DbObject> dbObjectsItem = new TreeItem<>(dbObjectsObj);
        dbItem.getChildren().add(dbObjectsItem);

        DbObject amFolderObj = new DbObject("access methods", Kind.ACCESS_METHODS_FOLDER, "thunder_db", null);
        amFolderObj.setLoaded(true);
        TreeItem<DbObject> amFolderItem = new TreeItem<>(amFolderObj);
        dbObjectsItem.getChildren().add(amFolderItem);

        DbObject brinObj = new DbObject("brin", Kind.ACCESS_METHOD, "thunder_db", null);
        TreeItem<DbObject> brinItem = new TreeItem<>(brinObj);
        amFolderItem.getChildren().add(brinItem);

        DbObject vvFolderObj = new DbObject("virtual views", Kind.VIRTUAL_VIEWS_FOLDER, "thunder_db", null);
        vvFolderObj.setLoaded(true);
        TreeItem<DbObject> vvFolderItem = new TreeItem<>(vvFolderObj);
        dbItem.getChildren().add(vvFolderItem);

        DbObject sessionsObj = new DbObject("sessions", Kind.VIRTUAL_VIEW, "thunder_db", null);
        TreeItem<DbObject> sessionsItem = new TreeItem<>(sessionsObj);
        vvFolderItem.getChildren().add(sessionsItem);

        // Scroll to brin
        pane.selectObjectInTree(profile, brinObj);
        assertTrue(connItem.isExpanded());
        assertTrue(dbItem.isExpanded());
        assertTrue(dbObjectsItem.isExpanded());
        assertTrue(amFolderItem.isExpanded());
        assertEquals(brinItem, pane.getSelectedItem());

        // Scroll to sessions
        pane.selectObjectInTree(profile, sessionsObj);
        assertTrue(vvFolderItem.isExpanded());
        assertEquals(sessionsItem, pane.getSelectedItem());
    }

    @Test
    public void testVirtualViewSessionsQualifiedName() {
        DbObject sessions = new DbObject("sessions", Kind.VIRTUAL_VIEW, "thunder_db", null);
        assertEquals("pg_stat_activity", sessions.qualifiedName(),
                "Virtual view sessions qualifiedName must resolve directly to pg_stat_activity for querying");
    }

    @Test
    public void testVirtualViewSessionsDataTabNoTableoid() {
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject sessions = new DbObject("sessions", Kind.VIRTUAL_VIEW, "thunder_db", null);
        DataTab dataTab = new DataTab(profile, sessions);

        assertEquals("sessions", dataTab.getText());
        assertNotNull(dataTab.getTooltip());
        assertTrue(dataTab.getTooltip().getText().contains("virtual views"),
                "Breadcrumb must contain virtual views folder");
        assertTrue(dataTab.getTooltip().getText().contains("sessions"));
        assertEquals("postgres@localhost/pg_stat_activity", dataTab.getQualifiedTableReference(),
                "Qualified reference must use pg_stat_activity");
    }

    @Test
    public void testSequenceSelectedToolbarState() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        pane.addConnectionNode(profile);

        TreeItem<DbObject> connItem = pane.getTree().getRoot().getChildren().get(0);
        DbObject seq = new DbObject("addresses_id_seq", Kind.SEQUENCE, "thunder_db", "public");
        seq.setDetail("bigint");
        TreeItem<DbObject> seqItem = new TreeItem<>(seq);
        connItem.getChildren().add(seqItem);

        pane.updateToolbarState(seqItem);

        assertFalse(pane.getJumpToConsoleButton().isDisabled(), "Jump to Console must be active for SEQUENCE");
        assertFalse(pane.getGoToDdlButton().isDisabled(), "Go to DDL must be active for SEQUENCE");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data must be inactive for SEQUENCE");
    }

    @Test
    public void testSequenceContextMenuAndIcon() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        pane.addConnectionNode(profile);

        DbObject seq = new DbObject("addresses_id_seq", Kind.SEQUENCE, "thunder_db", "public");
        seq.setDetail("bigint");
        TreeItem<DbObject> seqItem = new TreeItem<>(seq);

        ContextMenu menu = pane.buildMenuForTest(seq, profile);
        assertNotNull(menu, "Sequence context menu must not be null");

        Menu nav = findMenu(menu, "Navigation");
        assertNotNull(nav, "Sequence menu must contain 'Navigation'");
        assertTrue(menuHasItem(nav, "Go to DDL"), "Sequence Navigation must contain 'Go to DDL'");
        assertTrue(menuHasItem(nav, "Jump to Query Console"), "Sequence Navigation must contain 'Jump to Query Console'");
        assertTrue(menuHasItem(nav, "Select in Database Explorer"), "Sequence Navigation must contain 'Select in Database Explorer'");

        MenuItem refresh = findItem(menu, "Refresh");
        assertNotNull(refresh, "Sequence menu must contain 'Refresh'");

        assertNotNull(findItem(menu, "Modify Sequence"), "Sequence menu must contain 'Modify Sequence…'");
        assertNotNull(findMenu(menu, "New"), "Sequence menu must contain 'New'");

        assertNotNull(Icons.forObject(seq, profile, false), "Sequence must have non-null icon");
    }

    @Test
    public void testPostgreSqlSchemaContextMenuMatchingDataGripImage1() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject schemaObj = new DbObject("public", Kind.SCHEMA, "thunder_db", "public");
        ContextMenu menu = pane.buildMenuForTest(schemaObj, profile);
        assertNotNull(menu);

        // 1. New >
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertTrue(menuHasItem(newMenu, "Table"));
        assertTrue(menuHasItem(newMenu, "Materialized View"));
        assertTrue(menuHasItem(newMenu, "View"));
        assertTrue(menuHasItem(newMenu, "Operator"));
        assertTrue(menuHasItem(newMenu, "Sequence"));
        assertTrue(menuHasItem(newMenu, "Collation"));
        assertTrue(menuHasItem(newMenu, "Schema"));
        assertTrue(menuHasItem(newMenu, "Language"));
        assertTrue(menuHasItem(newMenu, "Database"));
        assertTrue(menuHasItem(newMenu, "Role"));
        assertTrue(menuHasItem(newMenu, "User"));
        assertTrue(menuHasItem(newMenu, "Virtual View"));
        assertTrue(menuHasItem(newMenu, "Data Source"));
        assertTrue(menuHasItem(newMenu, "Driver"));

        // 2. Modify Schema…
        MenuItem modify = findItem(menu, "Modify Schema");
        assertNotNull(modify);
        assertEquals(KeyCode.F6, ((KeyCodeCombination) modify.getAccelerator()).getCode());

        // 3. Copy/Paste >
        Menu copyPaste = findMenu(menu, "Copy/Paste");
        assertNotNull(copyPaste);
        assertTrue(menuHasItem(copyPaste, "Copy"));
        assertTrue(menuHasItem(copyPaste, "Copy Path/Reference"));

        // 4. Refresh (Ctrl+F5)
        MenuItem refresh = findItem(menu, "Refresh");
        assertNotNull(refresh);
        assertEquals(KeyCode.F5, ((KeyCodeCombination) refresh.getAccelerator()).getCode());

        // 5. Rename… (Shift+F6, enabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertFalse(rename.isDisable());

        // 6. Quick Documentation (Ctrl+Q)
        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertFalse(quickDoc.isDisable());

        // 7. Compare Schema Structure With… (Ctrl+D)
        MenuItem compare = findItem(menu, "Compare Schema Structure With");
        assertNotNull(compare);

        // 8. Navigation >
        Menu nav = findMenu(menu, "Navigation");
        assertNotNull(nav);
        assertTrue(menuHasItem(nav, "Go to DDL"));
        assertTrue(menuHasItem(nav, "Jump to Query Console"));
        assertTrue(menuHasItem(nav, "Select in Database Explorer"));

        // 9. Find Usages (Alt+Shift+7)
        assertNotNull(findItem(menu, "Find Usages"));

        // 10. Introspection Level >
        Menu intro = findMenu(menu, "Introspection Level");
        assertNotNull(intro);
        assertTrue(menuHasItem(intro, "Auto"));
        assertTrue(menuHasItem(intro, "High"));
        assertTrue(menuHasItem(intro, "Medium"));
        assertTrue(menuHasItem(intro, "Low"));

        // 11. SQL Scripts >
        Menu sql = findMenu(menu, "SQL Scripts");
        assertNotNull(sql);
        assertTrue(menuHasItem(sql, "Open Query Console"));
        assertTrue(menuHasItem(sql, "Run SQL Script"));
        assertTrue(menuHasItem(sql, "Generate DDL to Query Console"));

        // 12. Object Actions >
        Menu objActions = findMenu(menu, "Object Actions");
        assertNotNull(objActions);
        assertTrue(menuHasItem(objActions, "Show / Hide Schemas"));
        assertTrue(menuHasItem(objActions, "Forget Saved Passwords"));

        // 13. Tools >
        Menu tools = findMenu(menu, "Tools");
        assertNotNull(tools);
        assertTrue(menuHasItem(tools, "Manage Schemas"));
        assertTrue(menuHasItem(tools, "Verify Connection"));

        // 14. Import/Export >
        Menu impExp = findMenu(menu, "Import/Export");
        assertNotNull(impExp);
        assertTrue(menuHasItem(impExp, "Dump with pg_dump"));
        assertTrue(menuHasItem(impExp, "Restore"));

        // 15. Diagrams >
        Menu diagrams = findMenu(menu, "Diagrams");
        assertNotNull(diagrams);
        assertTrue(menuHasItem(diagrams, "Show Diagram"));

        // 16. Diagnostics >
        Menu diagnostics = findMenu(menu, "Diagnostics");
        assertNotNull(diagnostics);
        assertTrue(menuHasItem(diagnostics, "Diagnostic Refresh"));

        // 17. Drop… (Delete, enabled)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertFalse(drop.isDisable());
    }

    @Test
    public void testPostgreSqlTablesFolderContextMenuMatchingDataGripImage2() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject folder = new DbObject("tables", Kind.TABLES_FOLDER, "thunder_db", "public");
        ContextMenu menu = pane.buildMenuForTest(folder, profile);
        assertNotNull(menu);

        // 1. New > (starts with Table)
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertEquals("Table", newMenu.getItems().get(3).getText());

        // Category folder must NOT have Modify
        assertNull(findItem(menu, "Modify"));

        // Copy/Paste
        assertNotNull(findMenu(menu, "Copy/Paste"));

        // Open the List (F4)
        MenuItem openList = findItem(menu, "Open the List");
        assertNotNull(openList);
        assertEquals(KeyCode.F4, ((KeyCodeCombination) openList.getAccelerator()).getCode());

        // Refresh
        assertNotNull(findItem(menu, "Refresh"));

        // Rename… (disabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable());

        // Quick Documentation (disabled)
        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertTrue(quickDoc.isDisable());

        // Compare Schema Structure (disabled)
        MenuItem compare = findItem(menu, "Compare Schema Structure");
        assertNotNull(compare);
        assertTrue(compare.isDisable());

        // NO Navigation and NO Find Usages on category folders
        assertNull(findMenu(menu, "Navigation"));
        assertNull(findItem(menu, "Find Usages"));

        // Introspection Level
        assertNotNull(findMenu(menu, "Introspection Level"));

        // SQL Scripts
        assertNotNull(findMenu(menu, "SQL Scripts"));

        // Object Actions
        assertNotNull(findMenu(menu, "Object Actions"));

        // Tools
        assertNotNull(findMenu(menu, "Tools"));

        // Import/Export
        assertNotNull(findMenu(menu, "Import/Export"));

        // Diagrams
        assertNotNull(findMenu(menu, "Diagrams"));

        // Diagnostics
        assertNotNull(findMenu(menu, "Diagnostics"));

        // Drop… (disabled)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertTrue(drop.isDisable());
    }

    @Test
    public void testPostgreSqlTableContextMenuMatchingDataGripImage3() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject table = new DbObject("collection_variables", Kind.TABLE, "thunder_db", "public");
        ContextMenu menu = pane.buildMenuForTest(table, profile);
        assertNotNull(menu);

        // 1. New > (has table-specific items: Column, Unique Key, Foreign Key, Index, Check)
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Column"));
        assertTrue(menuHasItem(newMenu, "Unique Key"));
        assertTrue(menuHasItem(newMenu, "Foreign Key"));
        assertTrue(menuHasItem(newMenu, "Index"));
        assertTrue(menuHasItem(newMenu, "Check"));
        assertTrue(menuHasItem(newMenu, "Table"));
        assertTrue(menuHasItem(newMenu, "Virtual Column"));
        assertTrue(menuHasItem(newMenu, "Virtual Foreign Key"));
        assertTrue(menuHasItem(newMenu, "Virtual View"));

        // 2. Modify Table…
        MenuItem modify = findItem(menu, "Modify Table");
        assertNotNull(modify);
        assertEquals(KeyCode.F6, ((KeyCodeCombination) modify.getAccelerator()).getCode());

        // 3. Copy/Paste >
        assertNotNull(findMenu(menu, "Copy/Paste"));

        // 4. Edit Data (F4)
        MenuItem editData = findItem(menu, "Edit Data");
        assertNotNull(editData);
        assertEquals(KeyCode.F4, ((KeyCodeCombination) editData.getAccelerator()).getCode());

        // 5. Refresh
        assertNotNull(findItem(menu, "Refresh"));

        // 6. Rename… (enabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertFalse(rename.isDisable());

        // 7. Quick Documentation
        assertNotNull(findItem(menu, "Quick Documentation"));

        // 8. Compare Schema Structure With…
        assertNotNull(findItem(menu, "Compare Schema Structure With"));

        // 9. Navigation >
        Menu nav = findMenu(menu, "Navigation");
        assertNotNull(nav);
        assertTrue(menuHasItem(nav, "Go to DDL"));
        assertTrue(menuHasItem(nav, "Jump to Query Console"));
        assertTrue(menuHasItem(nav, "Select in Database Explorer"));

        // 10. Find Usages
        assertNotNull(findItem(menu, "Find Usages"));

        // 11. Introspection Level
        assertNotNull(findMenu(menu, "Introspection Level"));

        // 12. SQL Scripts > (includes SELECT * and COUNT(*))
        Menu sql = findMenu(menu, "SQL Scripts");
        assertNotNull(sql);
        assertTrue(menuHasItem(sql, "New Query: SELECT *"));
        assertTrue(menuHasItem(sql, "New Query: COUNT(*)"));

        // 13. Object Actions
        assertNotNull(findMenu(menu, "Object Actions"));

        // 14. Tools
        assertNotNull(findMenu(menu, "Tools"));

        // 15. Import/Export > (Table specific: Export Data to File, Import Data from File, Export with 'pg_dump', Restore with 'pg_restore')
        Menu impExp = findMenu(menu, "Import/Export");
        assertNotNull(impExp);
        assertTrue(menuHasItem(impExp, "Export Data to File"));
        assertTrue(menuHasItem(impExp, "Import Data from File(s)"));
        assertTrue(menuHasItem(impExp, "Export with 'pg_dump'"));
        assertTrue(menuHasItem(impExp, "Restore with 'pg_restore'"));

        // 16. Diagrams
        assertNotNull(findMenu(menu, "Diagrams"));

        // 17. Diagnostics
        assertNotNull(findMenu(menu, "Diagnostics"));

        // 18. Drop… (enabled)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertFalse(drop.isDisable());
    }

    @Test
    public void testPostgreSqlSequencesFolderContextMenuMatchingDataGripImage4() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject folder = new DbObject("sequences", Kind.SEQUENCES_FOLDER, "thunder_db", "public");
        ContextMenu menu = pane.buildMenuForTest(folder, profile);
        assertNotNull(menu);

        // 1. New > (starts with Sequence)
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertEquals("Sequence", newMenu.getItems().get(3).getText());

        // Category folder must NOT have Modify
        assertNull(findItem(menu, "Modify"));

        // Copy/Paste
        assertNotNull(findMenu(menu, "Copy/Paste"));

        // Open the List (F4)
        MenuItem openList = findItem(menu, "Open the List");
        assertNotNull(openList);
        assertEquals(KeyCode.F4, ((KeyCodeCombination) openList.getAccelerator()).getCode());

        // Refresh
        assertNotNull(findItem(menu, "Refresh"));

        // Rename… (disabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable());

        // Quick Documentation (disabled)
        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertTrue(quickDoc.isDisable());

        // Compare Schema Structure (disabled)
        MenuItem compare = findItem(menu, "Compare Schema Structure");
        assertNotNull(compare);
        assertTrue(compare.isDisable());

        // NO Navigation and NO Find Usages on category folders
        assertNull(findMenu(menu, "Navigation"));
        assertNull(findItem(menu, "Find Usages"));

        // Introspection Level
        assertNotNull(findMenu(menu, "Introspection Level"));

        // SQL Scripts
        assertNotNull(findMenu(menu, "SQL Scripts"));

        // In DataGrip Image 4: sequences category folder does NOT have Object Actions
        assertNull(findMenu(menu, "Object Actions"));

        // Tools
        assertNotNull(findMenu(menu, "Tools"));

        // Import/Export
        assertNotNull(findMenu(menu, "Import/Export"));

        // Diagrams
        assertNotNull(findMenu(menu, "Diagrams"));

        // Diagnostics
        assertNotNull(findMenu(menu, "Diagnostics"));

        // Drop… (disabled)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertTrue(drop.isDisable());
    }

    @Test
    public void testPostgreSqlSequenceContextMenuMatchingDataGripImage5() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject seq = new DbObject("mail_settings_id_seq", Kind.SEQUENCE, "thunder_db", "public");
        ContextMenu menu = pane.buildMenuForTest(seq, profile);
        assertNotNull(menu);

        // 1. New > (starts with Sequence)
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertEquals("Sequence", newMenu.getItems().get(3).getText());

        // 2. Modify Sequence…
        MenuItem modify = findItem(menu, "Modify Sequence");
        assertNotNull(modify);
        assertEquals(KeyCode.F6, ((KeyCodeCombination) modify.getAccelerator()).getCode());

        // 3. Copy/Paste >
        assertNotNull(findMenu(menu, "Copy/Paste"));

        // Sequence does NOT have Edit Data or Open the List
        assertNull(findItem(menu, "Edit Data"));
        assertNull(findItem(menu, "Open the List"));

        // 4. Refresh
        assertNotNull(findItem(menu, "Refresh"));

        // 5. Rename… (enabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertFalse(rename.isDisable());

        // 6. Quick Documentation
        assertNotNull(findItem(menu, "Quick Documentation"));

        // 7. Compare Schema Structure With…
        assertNotNull(findItem(menu, "Compare Schema Structure With"));

        // 8. Navigation >
        Menu nav = findMenu(menu, "Navigation");
        assertNotNull(nav);
        assertTrue(menuHasItem(nav, "Go to DDL"));
        assertTrue(menuHasItem(nav, "Jump to Query Console"));
        assertTrue(menuHasItem(nav, "Select in Database Explorer"));

        // 9. Find Usages
        assertNotNull(findItem(menu, "Find Usages"));

        // 10. Introspection Level
        assertNotNull(findMenu(menu, "Introspection Level"));

        // 11. SQL Scripts
        assertNotNull(findMenu(menu, "SQL Scripts"));

        // 12. Object Actions
        assertNotNull(findMenu(menu, "Object Actions"));

        // 13. Tools
        assertNotNull(findMenu(menu, "Tools"));

        // 14. Import/Export
        assertNotNull(findMenu(menu, "Import/Export"));

        // 15. Diagrams
        assertNotNull(findMenu(menu, "Diagrams"));

        // 16. Diagnostics
        assertNotNull(findMenu(menu, "Diagnostics"));

        // 17. Drop… (enabled)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertFalse(drop.isDisable());
    }

    @Test
    public void testLoadSchemaChildrenFallback() {
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject schema = new DbObject("public", Kind.SCHEMA, "thunder_db", "public");

        // When offline, falls back safely to default object folders
        List<DbObject> children = com.roze.dbnavigator.db.MetadataService.loadSchemaChildren(profile, schema);
        assertNotNull(children);
        assertFalse(children.isEmpty());
        assertTrue(children.stream().anyMatch(c -> c.getKind() == Kind.TABLES_FOLDER));
    }

    @Test
    public void testPostgreSqlDatabaseContextMenuMatchingDataGripImage1() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject dbObj = new DbObject("nexacom_db", Kind.DATABASE, "nexacom_db", null);
        ContextMenu menu = pane.buildMenuForTest(dbObj, profile);
        assertNotNull(menu, "PostgreSQL Database context menu must not be null");

        MenuItem modify = findItem(menu, "Modify Database");
        assertNotNull(modify, "PostgreSQL Database must have 'Modify Database…'");

        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertTrue(menuHasItem(newMenu, "Schema"));
        assertTrue(menuHasItem(newMenu, "Language"));
        assertTrue(menuHasItem(newMenu, "Database"));
        assertTrue(menuHasItem(newMenu, "Role"));
        assertTrue(menuHasItem(newMenu, "User"));
        assertTrue(menuHasItem(newMenu, "Virtual View"));
        assertTrue(menuHasItem(newMenu, "Data Source"));
        assertTrue(menuHasItem(newMenu, "Driver"));

        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertFalse(rename.isDisable(), "Rename must be ENABLED for PostgreSQL database");

        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertFalse(drop.isDisable(), "Drop must be ENABLED for PostgreSQL database");

        Menu impExp = findMenu(menu, "Import/Export");
        assertNotNull(impExp);
        assertTrue(menuHasItem(impExp, "Dump with pg_dump"));
    }

    @Test
    public void testPostgreSqlSchemaContextMenu() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject schemaObj = new DbObject("public", Kind.SCHEMA, "nexacom_db", "public");
        ContextMenu menu = pane.buildMenuForTest(schemaObj, profile);
        assertNotNull(menu, "PostgreSQL Schema context menu must not be null");

        MenuItem modify = findItem(menu, "Modify Schema");
        assertNotNull(modify, "PostgreSQL Schema must have 'Modify Schema…'");

        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertTrue(menuHasItem(newMenu, "Table"));
        assertTrue(menuHasItem(newMenu, "View"));
        assertTrue(menuHasItem(newMenu, "Materialized View"));
        assertTrue(menuHasItem(newMenu, "Sequence"));
        assertTrue(menuHasItem(newMenu, "Routine"));
        assertTrue(menuHasItem(newMenu, "Virtual View"));

        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertFalse(rename.isDisable(), "Rename must be ENABLED for PostgreSQL schema");

        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertFalse(drop.isDisable(), "Drop must be ENABLED for PostgreSQL schema");
    }

    @Test
    public void testMySqlDatabaseContextMenuMatchingDataGripImage2() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = new ConnectionProfile();
        profile.setId("mysql-1");
        profile.setName("mysql@localhost");
        profile.setType(ConnectionProfile.DatabaseType.MYSQL);
        DbObject dbObj = new DbObject("bagisto", Kind.DATABASE, "bagisto", null);
        ContextMenu menu = pane.buildMenuForTest(dbObj, profile);
        assertNotNull(menu, "MySQL context menu must not be null");

        MenuItem modify = findItem(menu, "Modify Schema");
        assertNotNull(modify, "MySQL must have 'Modify Schema…' (DataGrip Image 2)");
        assertNull(findItem(menu, "Modify Database"), "MySQL must NOT have 'Modify Database…'");

        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertTrue(menuHasItem(newMenu, "Table"));
        assertTrue(menuHasItem(newMenu, "View"));
        assertTrue(menuHasItem(newMenu, "Schema"));
        assertTrue(menuHasItem(newMenu, "User"));
        assertTrue(menuHasItem(newMenu, "Role"));
        assertTrue(menuHasItem(newMenu, "Virtual View"));
        assertTrue(menuHasItem(newMenu, "Data Source"));
        assertTrue(menuHasItem(newMenu, "Driver"));

        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable(), "Rename must be DISABLED for MySQL");

        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertFalse(drop.isDisable(), "Drop must be ENABLED for MySQL");

        Menu impExp = findMenu(menu, "Import/Export");
        assertNotNull(impExp);
        assertTrue(menuHasItem(impExp, "Dump with mysqldump"));
    }

    @Test
    public void testMariaDbDatabaseContextMenuMatchingDataGripImage4() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = new ConnectionProfile();
        profile.setId("maria-1");
        profile.setName("mariadb@localhost");
        profile.setType(ConnectionProfile.DatabaseType.MARIADB);
        DbObject dbObj = new DbObject("nation", Kind.DATABASE, "nation", null);
        ContextMenu menu = pane.buildMenuForTest(dbObj, profile);
        assertNotNull(menu, "MariaDB context menu must not be null");

        MenuItem modify = findItem(menu, "Modify Schema");
        assertNotNull(modify, "MariaDB must have 'Modify Schema…' (DataGrip Image 4)");
        assertNull(findItem(menu, "Modify Database"), "MariaDB must NOT have 'Modify Database…'");

        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertTrue(menuHasItem(newMenu, "Table"));
        assertTrue(menuHasItem(newMenu, "View"));
        assertTrue(menuHasItem(newMenu, "Schema"));
        assertTrue(menuHasItem(newMenu, "User"));
        assertTrue(menuHasItem(newMenu, "Role"));
        assertTrue(menuHasItem(newMenu, "Virtual View"));

        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable(), "Rename must be DISABLED for MariaDB");

        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertFalse(drop.isDisable(), "Drop must be ENABLED for MariaDB");

        Menu impExp = findMenu(menu, "Import/Export");
        assertNotNull(impExp);
        assertTrue(menuHasItem(impExp, "Dump with mariadb-dump"));
    }

    @Test
    public void testMongoDbDatabaseContextMenuMatchingDataGripImage3() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = new ConnectionProfile();
        profile.setId("mongo-1");
        profile.setName("studentdb@localhost");
        profile.setType(ConnectionProfile.DatabaseType.MONGODB);
        DbObject dbObj = new DbObject("studentdb", Kind.DATABASE, "studentdb", null);
        ContextMenu menu = pane.buildMenuForTest(dbObj, profile);
        assertNotNull(menu, "MongoDB context menu must not be null");

        assertNull(findItem(menu, "Modify Database"), "MongoDB must NOT have 'Modify Database…'");
        assertNull(findItem(menu, "Modify Schema"), "MongoDB must NOT have 'Modify Schema…'");

        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertTrue(menuHasItem(newMenu, "Collection"));
        assertTrue(menuHasItem(newMenu, "Virtual View"));

        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable(), "Rename must be DISABLED for MongoDB");

        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertFalse(drop.isDisable(), "Drop must be ENABLED for MongoDB");

        Menu impExp = findMenu(menu, "Import/Export");
        assertNotNull(impExp);
        assertTrue(menuHasItem(impExp, "Dump with mongodump"));
    }

    @Test
    public void testSqliteSchemaContextMenuMatchingDataGripImage5() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = new ConnectionProfile();
        profile.setId("sqlite-1");
        profile.setName("identifier.sqlite");
        profile.setType(ConnectionProfile.DatabaseType.SQLITE);
        DbObject schemaObj = new DbObject("main", Kind.SCHEMA, null, "main");
        ContextMenu menu = pane.buildMenuForTest(schemaObj, profile);
        assertNotNull(menu, "SQLite context menu must not be null");

        assertNull(findItem(menu, "Modify Schema"), "SQLite 'main' must NOT have 'Modify Schema…'");
        assertNull(findItem(menu, "Modify Database"), "SQLite 'main' must NOT have 'Modify Database…'");

        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertTrue(menuHasItem(newMenu, "Table"));
        assertTrue(menuHasItem(newMenu, "View"));
        assertTrue(menuHasItem(newMenu, "Virtual View"));

        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable(), "Rename must be DISABLED for SQLite");

        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertTrue(drop.isDisable(), "Drop must be DISABLED for SQLite 'main' schema");

        Menu impExp = findMenu(menu, "Import/Export");
        assertNotNull(impExp);
        assertTrue(menuHasItem(impExp, "Export SQLite Database"));
    }

    @Test
    public void testSqlServerDatabaseContextMenuMatchingDataGripImage1() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = new ConnectionProfile();
        profile.setId("mssql-1");
        profile.setName("mssql@localhost");
        profile.setType(ConnectionProfile.DatabaseType.SQLSERVER);
        DbObject dbObj = new DbObject("CompanyDB", Kind.DATABASE, "CompanyDB", null);
        ContextMenu menu = pane.buildMenuForTest(dbObj, profile);
        assertNotNull(menu, "SQL Server context menu must not be null");

        MenuItem modify = findItem(menu, "Modify Database");
        assertNotNull(modify, "SQL Server database must have 'Modify Database…' (DataGrip Image 1)");
        assertNull(findItem(menu, "Modify Schema"), "SQL Server database must NOT have 'Modify Schema…'");

        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertTrue(menuHasItem(newMenu, "Schema"));
        assertTrue(menuHasItem(newMenu, "Database Principal"));
        assertTrue(menuHasItem(newMenu, "Database"));
        assertTrue(menuHasItem(newMenu, "Server Principal"));
        assertTrue(menuHasItem(newMenu, "Virtual View"));

        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable(), "Rename must be DISABLED for SQL Server database (DataGrip Image 1)");

        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertFalse(drop.isDisable(), "Drop must be ENABLED for SQL Server database (DataGrip Image 1)");

        Menu objActions = findMenu(menu, "Object Actions");
        assertNotNull(objActions, "SQL Server database must have 'Object Actions >' (DataGrip Image 1)");

        Menu impExp = findMenu(menu, "Import/Export");
        assertNotNull(impExp);
        assertTrue(menuHasItem(impExp, "Export Database with bcp"));
    }

    @Test
    public void testOracleSchemaContextMenuMatchingDataGripImage2() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = new ConnectionProfile();
        profile.setId("oracle-1");
        profile.setName("oracle@localhost");
        profile.setType(ConnectionProfile.DatabaseType.ORACLE);
        DbObject schemaObj = new DbObject("HELLO", Kind.SCHEMA, null, "HELLO");
        ContextMenu menu = pane.buildMenuForTest(schemaObj, profile);
        assertNotNull(menu, "Oracle context menu must not be null");

        assertNull(findItem(menu, "Modify Schema"), "Oracle schema must NOT have 'Modify Schema…' (DataGrip Image 2)");
        assertNull(findItem(menu, "Modify Database"), "Oracle schema must NOT have 'Modify Database…'");

        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertFalse(menuHasItem(newMenu, "Table"), "Oracle schema + New must NOT have 'Table' directly (DataGrip Image 2)");
        assertFalse(menuHasItem(newMenu, "View"), "Oracle schema + New must NOT have 'View' directly (DataGrip Image 2)");
        assertTrue(menuHasItem(newMenu, "Virtual View"));

        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable(), "Rename must be DISABLED for Oracle schema (DataGrip Image 2)");

        assertNull(findMenu(menu, "Object Actions"), "Oracle schema must NOT have 'Object Actions >' (DataGrip Image 2)");

        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertTrue(drop.isDisable(), "Drop must be DISABLED for Oracle schema in tree (DataGrip Image 2)");

        Menu impExp = findMenu(menu, "Import/Export");
        assertNotNull(impExp);
        assertTrue(menuHasItem(impExp, "Export with expdp"));
    }

    @Test
    public void testCollapseAllButtonPropertiesMatchingDataGrip() {
        SchemaTreePane pane = new SchemaTreePane(null);
        javafx.scene.control.Button collapseBtn = pane.getCollapseAllButton();
        assertNotNull(collapseBtn, "Collapse All button must be present in SchemaTreePane header");
        assertNotNull(collapseBtn.getTooltip(), "Collapse All button must have tooltip");
        assertTrue(collapseBtn.getTooltip().getText().startsWith("Collapse All"), "Tooltip must start with 'Collapse All'");
        assertTrue(collapseBtn.getTooltip().getText().contains("NumPad -"), "Tooltip must mention NumPad - shortcut");
        assertNotNull(collapseBtn.getGraphic(), "Collapse All button must have an icon");
    }

    @Test
    public void testCollapseAllRecursivelyCollapsesAllNodes() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile pgProfile = createPostgreSqlProfile();
        pane.addConnectionNode(pgProfile);

        ConnectionProfile mysqlProfile = new ConnectionProfile();
        mysqlProfile.setId("mysql-1");
        mysqlProfile.setName("mysql@localhost");
        mysqlProfile.setType(ConnectionProfile.DatabaseType.MYSQL);
        pane.addConnectionNode(mysqlProfile);

        TreeItem<DbObject> conn1 = pane.getTree().getRoot().getChildren().get(0);
        TreeItem<DbObject> conn2 = pane.getTree().getRoot().getChildren().get(1);

        TreeItem<DbObject> dbItem1 = new TreeItem<>(new DbObject("thunder_db", Kind.DATABASE, "thunder_db", null));
        TreeItem<DbObject> schemaItem1 = new TreeItem<>(new DbObject("public", Kind.SCHEMA, "thunder_db", "public"));
        TreeItem<DbObject> tablesFolder = new TreeItem<>(new DbObject("tables", Kind.TABLES_FOLDER, "thunder_db", "public"));
        TreeItem<DbObject> tableItem = new TreeItem<>(new DbObject("users", Kind.TABLE, "thunder_db", "public"));

        conn1.getChildren().add(dbItem1);
        dbItem1.getChildren().add(schemaItem1);
        schemaItem1.getChildren().add(tablesFolder);
        tablesFolder.getChildren().add(tableItem);

        TreeItem<DbObject> dbItem2 = new TreeItem<>(new DbObject("test_db", Kind.DATABASE, "test_db", null));
        conn2.getChildren().add(dbItem2);

        // Expand all of them
        conn1.setExpanded(true);
        dbItem1.setExpanded(true);
        schemaItem1.setExpanded(true);
        tablesFolder.setExpanded(true);
        tableItem.setExpanded(true);
        conn2.setExpanded(true);
        dbItem2.setExpanded(true);

        assertTrue(conn1.isExpanded());
        assertTrue(dbItem1.isExpanded());
        assertTrue(schemaItem1.isExpanded());
        assertTrue(tablesFolder.isExpanded());
        assertTrue(conn2.isExpanded());
        assertTrue(dbItem2.isExpanded());

        // Select nested table item
        pane.getTree().getSelectionModel().select(tableItem);
        assertEquals(tableItem, pane.getTree().getSelectionModel().getSelectedItem());

        // Trigger collapse all
        pane.collapseAll();

        // Verify all nodes recursively collapsed
        assertFalse(conn1.isExpanded(), "conn1 must be collapsed");
        assertFalse(dbItem1.isExpanded(), "dbItem1 must be collapsed");
        assertFalse(schemaItem1.isExpanded(), "schemaItem1 must be collapsed");
        assertFalse(tablesFolder.isExpanded(), "tablesFolder must be collapsed");
        assertFalse(tableItem.isExpanded(), "tableItem must be collapsed");
        assertFalse(conn2.isExpanded(), "conn2 must be collapsed");
        assertFalse(dbItem2.isExpanded(), "dbItem2 must be collapsed");

        // Verify selection was gracefully moved to top-level connection ancestor
        assertEquals(conn1, pane.getTree().getSelectionModel().getSelectedItem(),
                "Selection should gracefully fall back to the top-level connection ancestor");
    }

    @Test
    public void testPostgreSqlDatabaseObjectsFolderContextMenuMatchingDataGripImage1() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject dbObjs = new DbObject("Database Objects", Kind.DATABASE_OBJECTS, "thunder_db", null);
        ContextMenu menu = pane.buildMenuForTest(dbObjs, profile);
        assertNotNull(menu);

        // 1. + New >
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertFalse(menuHasItem(newMenu, "Table"));
        assertFalse(menuHasItem(newMenu, "View"));
        assertFalse(menuHasItem(newMenu, "Schema"));
        assertTrue(menuHasItem(newMenu, "Data Source"));
        assertTrue(menuHasItem(newMenu, "DDL Data Source"));

        // No Modify
        assertNull(findItem(menu, "Modify"));

        // Copy/Paste (disabled)
        Menu cp = findMenu(menu, "Copy/Paste");
        assertNotNull(cp);
        assertTrue(cp.isDisable());

        // Refresh (Ctrl+F5)
        assertNotNull(findItem(menu, "Refresh"));

        // Rename… (disabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable());

        // Open the List (F4)
        MenuItem openList = findItem(menu, "Open the List");
        assertNotNull(openList);
        assertEquals(KeyCode.F4, ((KeyCodeCombination) openList.getAccelerator()).getCode());

        // Quick Documentation (disabled)
        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertTrue(quickDoc.isDisable());

        // Compare Schema Structure (disabled, no "With…")
        MenuItem compare = findItem(menu, "Compare Schema Structure");
        assertNotNull(compare);
        assertTrue(compare.isDisable());
        assertNull(findItem(menu, "Compare Schema Structure With"));

        // No Navigation, No Find Usages
        assertNull(findMenu(menu, "Navigation"));
        assertNull(findItem(menu, "Find Usages"));

        // Introspection Level
        assertNotNull(findMenu(menu, "Introspection Level"));

        // SQL Scripts
        assertNotNull(findMenu(menu, "SQL Scripts"));

        // Tools
        assertNotNull(findMenu(menu, "Tools"));

        // No Object Actions and No Import/Export
        assertNull(findMenu(menu, "Object Actions"));
        assertNull(findMenu(menu, "Import/Export"));

        // Diagrams
        assertNotNull(findMenu(menu, "Diagrams"));

        // Diagnostics
        assertNotNull(findMenu(menu, "Diagnostics"));

        // Drop… (disabled)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertTrue(drop.isDisable());
    }

    @Test
    public void testPostgreSqlVirtualViewsFolderContextMenuMatchingDataGripImage2() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject vvFolder = new DbObject("virtual views", Kind.VIRTUAL_VIEWS_FOLDER, "thunder_db", null);
        ContextMenu menu = pane.buildMenuForTest(vvFolder, profile);
        assertNotNull(menu);

        // 1. + New >
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertTrue(menuHasItem(newMenu, "Virtual View"));
        assertTrue(menuHasItem(newMenu, "Schema"));
        assertTrue(menuHasItem(newMenu, "Language"));
        assertTrue(menuHasItem(newMenu, "Database"));
        assertTrue(menuHasItem(newMenu, "Role"));
        assertTrue(menuHasItem(newMenu, "User"));
        assertFalse(menuHasItem(newMenu, "Table"));

        // Modify Virtual View… (Ctrl+F6, enabled)
        MenuItem modify = findItem(menu, "Modify Virtual View");
        assertNotNull(modify);
        assertFalse(modify.isDisable());
        assertEquals(KeyCode.F6, ((KeyCodeCombination) modify.getAccelerator()).getCode());

        // Copy/Paste (disabled)
        Menu cp = findMenu(menu, "Copy/Paste");
        assertNotNull(cp);
        assertTrue(cp.isDisable());

        // Refresh (Ctrl+F5)
        assertNotNull(findItem(menu, "Refresh"));

        // Rename… (disabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable());

        // Open the List (F4)
        MenuItem openList = findItem(menu, "Open the List");
        assertNotNull(openList);
        assertEquals(KeyCode.F4, ((KeyCodeCombination) openList.getAccelerator()).getCode());

        // Quick Documentation (disabled)
        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertTrue(quickDoc.isDisable());

        // Compare Schema Structure (disabled)
        MenuItem compare = findItem(menu, "Compare Schema Structure");
        assertNotNull(compare);
        assertTrue(compare.isDisable());

        // No Navigation, No Find Usages
        assertNull(findMenu(menu, "Navigation"));
        assertNull(findItem(menu, "Find Usages"));

        // Introspection Level
        assertNotNull(findMenu(menu, "Introspection Level"));

        // SQL Scripts
        assertNotNull(findMenu(menu, "SQL Scripts"));

        // No Object Actions
        assertNull(findMenu(menu, "Object Actions"));

        // Tools
        assertNotNull(findMenu(menu, "Tools"));

        // Import/Export (has pg_dump, restore)
        Menu impExp = findMenu(menu, "Import/Export");
        assertNotNull(impExp);
        assertTrue(menuHasItem(impExp, "Dump with pg_dump"));

        // Diagrams
        assertNotNull(findMenu(menu, "Diagrams"));

        // Diagnostics
        assertNotNull(findMenu(menu, "Diagnostics"));

        // Drop… (disabled)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertTrue(drop.isDisable());
    }

    @Test
    public void testPostgreSqlAccessMethodsFolderContextMenuMatchingDataGripImage3() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject amFolder = new DbObject("access methods", Kind.ACCESS_METHODS_FOLDER, "thunder_db", null);
        ContextMenu menu = pane.buildMenuForTest(amFolder, profile);
        assertNotNull(menu);

        // 1. + New >
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertFalse(menuHasItem(newMenu, "Table"));
        assertFalse(menuHasItem(newMenu, "Virtual View"));

        // No Modify
        assertNull(findItem(menu, "Modify"));

        // Copy/Paste (disabled)
        Menu cp = findMenu(menu, "Copy/Paste");
        assertNotNull(cp);
        assertTrue(cp.isDisable());

        // Open the List (F4)
        MenuItem openList = findItem(menu, "Open the List");
        assertNotNull(openList);
        assertEquals(KeyCode.F4, ((KeyCodeCombination) openList.getAccelerator()).getCode());

        // Rename… (disabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable());

        // Quick Documentation (disabled)
        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertTrue(quickDoc.isDisable());

        // Compare Schema Structure (disabled)
        MenuItem compare = findItem(menu, "Compare Schema Structure");
        assertNotNull(compare);
        assertTrue(compare.isDisable());

        // No Navigation, No Find Usages
        assertNull(findMenu(menu, "Navigation"));
        assertNull(findItem(menu, "Find Usages"));

        // Introspection Level
        assertNotNull(findMenu(menu, "Introspection Level"));

        // SQL Scripts
        assertNotNull(findMenu(menu, "SQL Scripts"));

        // Tools
        assertNotNull(findMenu(menu, "Tools"));

        // No Object Actions and No Import/Export
        assertNull(findMenu(menu, "Object Actions"));
        assertNull(findMenu(menu, "Import/Export"));

        // Diagrams
        assertNotNull(findMenu(menu, "Diagrams"));

        // Diagnostics
        assertNotNull(findMenu(menu, "Diagnostics"));

        // Drop… (disabled)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertTrue(drop.isDisable());
    }

    @Test
    public void testPostgreSqlAccessMethodItemContextMenuMatchingDataGripImage4() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject brin = new DbObject("brin", Kind.ACCESS_METHOD, "thunder_db", null);
        ContextMenu menu = pane.buildMenuForTest(brin, profile);
        assertNotNull(menu);

        // 1. + New >
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertTrue(menuHasItem(newMenu, "Schema"));
        assertTrue(menuHasItem(newMenu, "Language"));
        assertTrue(menuHasItem(newMenu, "Database"));
        assertTrue(menuHasItem(newMenu, "Role"));
        assertTrue(menuHasItem(newMenu, "User"));
        assertTrue(menuHasItem(newMenu, "Virtual View"));

        // No Modify
        assertNull(findItem(menu, "Modify"));

        // Copy/Paste (enabled)
        Menu cp = findMenu(menu, "Copy/Paste");
        assertNotNull(cp);
        assertFalse(cp.isDisable());

        // No Edit Data and No Open the List
        assertNull(findItem(menu, "Edit Data"));
        assertNull(findItem(menu, "Open the List"));

        // Refresh (Ctrl+F5)
        assertNotNull(findItem(menu, "Refresh"));

        // Rename… (disabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable());

        // Quick Documentation (enabled)
        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertFalse(quickDoc.isDisable());

        // Compare Schema Structure With… (disabled)
        MenuItem compare = findItem(menu, "Compare Schema Structure With");
        assertNotNull(compare);
        assertTrue(compare.isDisable());

        // Navigation
        Menu nav = findMenu(menu, "Navigation");
        assertNotNull(nav);
        assertTrue(menuHasItem(nav, "Go to DDL"));
        assertTrue(menuHasItem(nav, "Jump to Query Console"));
        assertTrue(menuHasItem(nav, "Select in Database Explorer"));

        // Find Usages (disabled)
        MenuItem findUsages = findItem(menu, "Find Usages");
        assertNotNull(findUsages);
        assertTrue(findUsages.isDisable());

        // Introspection Level
        assertNotNull(findMenu(menu, "Introspection Level"));

        // SQL Scripts
        assertNotNull(findMenu(menu, "SQL Scripts"));

        // Object Actions (enabled)
        Menu objActions = findMenu(menu, "Object Actions");
        assertNotNull(objActions);
        assertTrue(menuHasItem(objActions, "Show / Hide Schemas"));
        assertTrue(menuHasItem(objActions, "Forget Saved Passwords"));

        // Tools
        assertNotNull(findMenu(menu, "Tools"));

        // Import/Export
        assertNotNull(findMenu(menu, "Import/Export"));

        // Diagrams
        assertNotNull(findMenu(menu, "Diagrams"));

        // Diagnostics
        assertNotNull(findMenu(menu, "Diagnostics"));

        // Drop… (enabled!)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertFalse(drop.isDisable());
    }

    @Test
    public void testPostgreSqlVirtualViewItemContextMenuMatchingDataGripImage5() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject sessionView = new DbObject("sessions", Kind.VIRTUAL_VIEW, "thunder_db", null);
        ContextMenu menu = pane.buildMenuForTest(sessionView, profile);
        assertNotNull(menu);

        // 1. + New >
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertTrue(menuHasItem(newMenu, "Virtual Column"));
        assertTrue(menuHasItem(newMenu, "Virtual Foreign Key"));
        assertTrue(menuHasItem(newMenu, "Virtual View"));
        assertTrue(menuHasItem(newMenu, "Schema"));
        assertTrue(menuHasItem(newMenu, "Language"));
        assertTrue(menuHasItem(newMenu, "Database"));
        assertTrue(menuHasItem(newMenu, "Role"));
        assertTrue(menuHasItem(newMenu, "User"));

        // Modify Virtual View… (Ctrl+F6, enabled)
        MenuItem modify = findItem(menu, "Modify Virtual View");
        assertNotNull(modify);
        assertFalse(modify.isDisable());
        assertEquals(KeyCode.F6, ((KeyCodeCombination) modify.getAccelerator()).getCode());

        // Copy/Paste (enabled)
        Menu cp = findMenu(menu, "Copy/Paste");
        assertNotNull(cp);
        assertFalse(cp.isDisable());

        // Edit Data (F4, enabled)
        MenuItem editData = findItem(menu, "Edit Data");
        assertNotNull(editData);
        assertFalse(editData.isDisable());
        assertEquals(KeyCode.F4, ((KeyCodeCombination) editData.getAccelerator()).getCode());

        // Refresh (Ctrl+F5)
        assertNotNull(findItem(menu, "Refresh"));

        // Rename… (disabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable());

        // Quick Documentation (enabled)
        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertFalse(quickDoc.isDisable());

        // Compare Schema Structure (disabled, no "With…")
        MenuItem compare = findItem(menu, "Compare Schema Structure");
        assertNotNull(compare);
        assertTrue(compare.isDisable());
        assertNull(findItem(menu, "Compare Schema Structure With"));

        // Navigation
        Menu nav = findMenu(menu, "Navigation");
        assertNotNull(nav);
        assertTrue(menuHasItem(nav, "Go to DDL"));
        assertTrue(menuHasItem(nav, "Jump to Query Console"));
        assertTrue(menuHasItem(nav, "Select in Database Explorer"));

        // Find Usages (disabled)
        MenuItem findUsages = findItem(menu, "Find Usages");
        assertNotNull(findUsages);
        assertTrue(findUsages.isDisable());

        // Introspection Level
        assertNotNull(findMenu(menu, "Introspection Level"));

        // SQL Scripts
        assertNotNull(findMenu(menu, "SQL Scripts"));

        // No Object Actions for Virtual View
        assertNull(findMenu(menu, "Object Actions"));

        // Tools
        assertNotNull(findMenu(menu, "Tools"));

        // Import/Export
        assertNotNull(findMenu(menu, "Import/Export"));

        // Diagrams
        assertNotNull(findMenu(menu, "Diagrams"));

        // Diagnostics
        assertNotNull(findMenu(menu, "Diagnostics"));

        // Drop… (enabled!)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertFalse(drop.isDisable());
    }

    @Test
    public void testPostgreSqlCastsFolderContextMenuMatchingDataGripImage1() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject castsFolder = new DbObject("casts", Kind.CASTS_FOLDER, "thunder_db", null);
        ContextMenu menu = pane.buildMenuForTest(castsFolder, profile);
        assertNotNull(menu);

        // 1. + New > (only consoles and data sources)
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertFalse(menuHasItem(newMenu, "Table"));
        assertFalse(menuHasItem(newMenu, "View"));
        assertFalse(menuHasItem(newMenu, "Virtual View"));
        assertTrue(menuHasItem(newMenu, "Data Source"));
        assertTrue(menuHasItem(newMenu, "DDL Data Source"));

        // No Modify
        assertNull(findItem(menu, "Modify"));

        // Copy/Paste (disabled)
        Menu cp = findMenu(menu, "Copy/Paste");
        assertNotNull(cp);
        assertTrue(cp.isDisable());

        // Refresh (Ctrl+F5)
        assertNotNull(findItem(menu, "Refresh"));

        // Rename… (disabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable());

        // Open the List (F4)
        MenuItem openList = findItem(menu, "Open the List");
        assertNotNull(openList);
        assertEquals(KeyCode.F4, ((KeyCodeCombination) openList.getAccelerator()).getCode());

        // Quick Documentation (disabled)
        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertTrue(quickDoc.isDisable());

        // Compare Schema Structure (disabled, no "With…")
        MenuItem compare = findItem(menu, "Compare Schema Structure");
        assertNotNull(compare);
        assertTrue(compare.isDisable());
        assertNull(findItem(menu, "Compare Schema Structure With"));

        // No Navigation, No Find Usages
        assertNull(findMenu(menu, "Navigation"));
        assertNull(findItem(menu, "Find Usages"));

        // Introspection Level
        assertNotNull(findMenu(menu, "Introspection Level"));

        // SQL Scripts
        assertNotNull(findMenu(menu, "SQL Scripts"));

        // Tools
        assertNotNull(findMenu(menu, "Tools"));

        // No Object Actions and No Import/Export
        assertNull(findMenu(menu, "Object Actions"));
        assertNull(findMenu(menu, "Import/Export"));

        // Diagrams
        assertNotNull(findMenu(menu, "Diagrams"));

        // Diagnostics
        assertNotNull(findMenu(menu, "Diagnostics"));

        // Drop… (disabled)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertTrue(drop.isDisable());
    }

    @Test
    public void testPostgreSqlExtensionsFolderContextMenuMatchingDataGripImage2() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject extFolder = new DbObject("extensions", Kind.EXTENSIONS_FOLDER, "thunder_db", null);
        ContextMenu menu = pane.buildMenuForTest(extFolder, profile);
        assertNotNull(menu);

        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertFalse(menuHasItem(newMenu, "Table"));
        assertFalse(menuHasItem(newMenu, "Virtual View"));

        assertNull(findItem(menu, "Modify"));

        Menu cp = findMenu(menu, "Copy/Paste");
        assertNotNull(cp);
        assertTrue(cp.isDisable());

        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable());

        MenuItem openList = findItem(menu, "Open the List");
        assertNotNull(openList);
        assertEquals(KeyCode.F4, ((KeyCodeCombination) openList.getAccelerator()).getCode());

        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertTrue(quickDoc.isDisable());

        MenuItem compare = findItem(menu, "Compare Schema Structure");
        assertNotNull(compare);
        assertTrue(compare.isDisable());

        assertNull(findMenu(menu, "Navigation"));
        assertNull(findItem(menu, "Find Usages"));
        assertNull(findMenu(menu, "Object Actions"));
        assertNull(findMenu(menu, "Import/Export"));

        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertTrue(drop.isDisable());
    }

    @Test
    public void testPostgreSqlLanguagesFolderContextMenuMatchingDataGripImage3() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject langFolder = new DbObject("languages", Kind.LANGUAGES_FOLDER, "thunder_db", null);
        ContextMenu menu = pane.buildMenuForTest(langFolder, profile);
        assertNotNull(menu);

        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertFalse(menuHasItem(newMenu, "Table"));
        assertFalse(menuHasItem(newMenu, "Virtual View"));

        assertNull(findItem(menu, "Modify"));

        Menu cp = findMenu(menu, "Copy/Paste");
        assertNotNull(cp);
        assertTrue(cp.isDisable());

        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable());

        MenuItem openList = findItem(menu, "Open the List");
        assertNotNull(openList);
        assertEquals(KeyCode.F4, ((KeyCodeCombination) openList.getAccelerator()).getCode());

        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertTrue(quickDoc.isDisable());

        MenuItem compare = findItem(menu, "Compare Schema Structure");
        assertNotNull(compare);
        assertTrue(compare.isDisable());

        assertNull(findMenu(menu, "Navigation"));
        assertNull(findItem(menu, "Find Usages"));
        assertNull(findMenu(menu, "Object Actions"));
        assertNull(findMenu(menu, "Import/Export"));

        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertTrue(drop.isDisable());
    }

    @Test
    public void testPostgreSqlCastItemContextMenuMatchingDataGripImage4() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject castObj = new DbObject("bit as bit", Kind.CAST, "thunder_db", null);
        ContextMenu menu = pane.buildMenuForTest(castObj, profile);
        assertNotNull(menu);

        // 1. + New >
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertTrue(menuHasItem(newMenu, "Schema"));
        assertTrue(menuHasItem(newMenu, "Language"));
        assertTrue(menuHasItem(newMenu, "Database"));
        assertTrue(menuHasItem(newMenu, "Role"));
        assertTrue(menuHasItem(newMenu, "User"));
        assertTrue(menuHasItem(newMenu, "Virtual View"));

        // No Modify
        assertNull(findItem(menu, "Modify"));

        // Copy/Paste (enabled)
        Menu cp = findMenu(menu, "Copy/Paste");
        assertNotNull(cp);
        assertFalse(cp.isDisable());

        // No Edit Data and No Open the List
        assertNull(findItem(menu, "Edit Data"));
        assertNull(findItem(menu, "Open the List"));

        // Refresh (Ctrl+F5)
        assertNotNull(findItem(menu, "Refresh"));

        // Rename… (disabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable());

        // Quick Documentation (enabled)
        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertFalse(quickDoc.isDisable());

        // Compare Schema Structure With… (disabled)
        MenuItem compare = findItem(menu, "Compare Schema Structure With");
        assertNotNull(compare);
        assertTrue(compare.isDisable());

        // Navigation
        Menu nav = findMenu(menu, "Navigation");
        assertNotNull(nav);
        assertTrue(menuHasItem(nav, "Go to DDL"));
        assertTrue(menuHasItem(nav, "Jump to Query Console"));
        assertTrue(menuHasItem(nav, "Select in Database Explorer"));

        // Find Usages (disabled)
        MenuItem findUsages = findItem(menu, "Find Usages");
        assertNotNull(findUsages);
        assertTrue(findUsages.isDisable());

        // Introspection Level
        assertNotNull(findMenu(menu, "Introspection Level"));

        // SQL Scripts
        assertNotNull(findMenu(menu, "SQL Scripts"));

        // Object Actions (enabled)
        Menu objActions = findMenu(menu, "Object Actions");
        assertNotNull(objActions);
        assertTrue(menuHasItem(objActions, "Show / Hide Schemas"));
        assertTrue(menuHasItem(objActions, "Forget Saved Passwords"));

        // Tools
        assertNotNull(findMenu(menu, "Tools"));

        // Import/Export (enabled)
        Menu impExp = findMenu(menu, "Import/Export");
        assertNotNull(impExp);
        assertTrue(menuHasItem(impExp, "Dump with pg_dump"));

        // Diagrams
        assertNotNull(findMenu(menu, "Diagrams"));

        // Diagnostics
        assertNotNull(findMenu(menu, "Diagnostics"));

        // Drop… (enabled!)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertFalse(drop.isDisable());
    }

    @Test
    public void testPostgreSqlExtensionItemContextMenuMatchingDataGripImage5() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject extObj = new DbObject("plpgsql", Kind.EXTENSION, "thunder_db", null);
        ContextMenu menu = pane.buildMenuForTest(extObj, profile);
        assertNotNull(menu);

        // 1. + New >
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertTrue(menuHasItem(newMenu, "Schema"));
        assertTrue(menuHasItem(newMenu, "Language"));
        assertTrue(menuHasItem(newMenu, "Database"));
        assertTrue(menuHasItem(newMenu, "Role"));
        assertTrue(menuHasItem(newMenu, "User"));
        assertTrue(menuHasItem(newMenu, "Virtual View"));

        // No Modify
        assertNull(findItem(menu, "Modify"));

        // Copy/Paste (enabled)
        Menu cp = findMenu(menu, "Copy/Paste");
        assertNotNull(cp);
        assertFalse(cp.isDisable());

        // No Edit Data and No Open the List
        assertNull(findItem(menu, "Edit Data"));
        assertNull(findItem(menu, "Open the List"));

        // Refresh (Ctrl+F5)
        assertNotNull(findItem(menu, "Refresh"));

        // Rename… (disabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable());

        // Quick Documentation (enabled)
        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertFalse(quickDoc.isDisable());

        // Compare Schema Structure With… (disabled)
        MenuItem compare = findItem(menu, "Compare Schema Structure With");
        assertNotNull(compare);
        assertTrue(compare.isDisable());

        // Navigation
        Menu nav = findMenu(menu, "Navigation");
        assertNotNull(nav);
        assertTrue(menuHasItem(nav, "Go to DDL"));
        assertTrue(menuHasItem(nav, "Jump to Query Console"));
        assertTrue(menuHasItem(nav, "Select in Database Explorer"));

        // Find Usages (disabled)
        MenuItem findUsages = findItem(menu, "Find Usages");
        assertNotNull(findUsages);
        assertTrue(findUsages.isDisable());

        // Introspection Level
        assertNotNull(findMenu(menu, "Introspection Level"));

        // SQL Scripts
        assertNotNull(findMenu(menu, "SQL Scripts"));

        // Object Actions (enabled)
        Menu objActions = findMenu(menu, "Object Actions");
        assertNotNull(objActions);
        assertTrue(menuHasItem(objActions, "Show / Hide Schemas"));
        assertTrue(menuHasItem(objActions, "Forget Saved Passwords"));

        // Tools
        assertNotNull(findMenu(menu, "Tools"));

        // Import/Export (enabled)
        Menu impExp = findMenu(menu, "Import/Export");
        assertNotNull(impExp);
        assertTrue(menuHasItem(impExp, "Dump with pg_dump"));

        // Diagrams
        assertNotNull(findMenu(menu, "Diagrams"));

        // Diagnostics
        assertNotNull(findMenu(menu, "Diagnostics"));

        // Drop… (enabled!)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertFalse(drop.isDisable());
    }

    @Test
    public void testPostgreSqlLanguageItemContextMenuImage1() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject langObj = new DbObject("c", Kind.LANGUAGE, "thunder_db", null);
        ContextMenu menu = pane.buildMenuForTest(langObj, profile);
        assertNotNull(menu);

        // 1. + New >
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertTrue(menuHasItem(newMenu, "Query Console"));
        assertTrue(menuHasItem(newMenu, "Query File"));
        assertEquals("Language", newMenu.getItems().get(3).getText(), "Language must be first after separator");
        assertEquals("Schema", newMenu.getItems().get(4).getText());
        assertEquals("Database", newMenu.getItems().get(5).getText());
        assertEquals("Role", newMenu.getItems().get(6).getText());
        assertEquals("User", newMenu.getItems().get(7).getText());
        assertTrue(menuHasItem(newMenu, "Virtual View"));

        // 2. Modify Language… (enabled, Ctrl+F6)
        MenuItem modify = findItem(menu, "Modify Language");
        assertNotNull(modify);
        assertFalse(modify.isDisable());
        assertEquals(KeyCode.F6, ((KeyCodeCombination) modify.getAccelerator()).getCode());

        // 3. Copy/Paste (enabled)
        Menu cp = findMenu(menu, "Copy/Paste");
        assertNotNull(cp);
        assertFalse(cp.isDisable());

        // No Edit Data and No Open the List
        assertNull(findItem(menu, "Edit Data"));
        assertNull(findItem(menu, "Open the List"));

        // 4. Refresh (Ctrl+F5)
        assertNotNull(findItem(menu, "Refresh"));

        // 5. Rename… (disabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable());

        // 6. Quick Documentation (enabled)
        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertFalse(quickDoc.isDisable());

        // 7. Compare Schema Structure With… (disabled)
        MenuItem compare = findItem(menu, "Compare Schema Structure With");
        assertNotNull(compare);
        assertTrue(compare.isDisable());

        // 8. Navigation
        Menu nav = findMenu(menu, "Navigation");
        assertNotNull(nav);
        assertTrue(menuHasItem(nav, "Go to DDL"));
        assertTrue(menuHasItem(nav, "Jump to Query Console"));
        assertTrue(menuHasItem(nav, "Select in Database Explorer"));

        // 9. Find Usages (disabled)
        MenuItem findUsages = findItem(menu, "Find Usages");
        assertNotNull(findUsages);
        assertTrue(findUsages.isDisable());

        // 10. Introspection Level
        assertNotNull(findMenu(menu, "Introspection Level"));

        // 11. SQL Scripts
        assertNotNull(findMenu(menu, "SQL Scripts"));

        // 12. Object Actions (enabled)
        Menu objActions = findMenu(menu, "Object Actions");
        assertNotNull(objActions);
        assertTrue(menuHasItem(objActions, "Show / Hide Schemas"));
        assertTrue(menuHasItem(objActions, "Forget Saved Passwords"));

        // 13. Tools
        assertNotNull(findMenu(menu, "Tools"));

        // 14. Import/Export (enabled)
        Menu impExp = findMenu(menu, "Import/Export");
        assertNotNull(impExp);
        assertTrue(menuHasItem(impExp, "Dump with pg_dump"));

        // 15. Diagrams
        assertNotNull(findMenu(menu, "Diagrams"));

        // 16. Diagnostics
        assertNotNull(findMenu(menu, "Diagnostics"));

        // 17. Drop… (enabled!)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertFalse(drop.isDisable());
    }

    @Test
    public void testPostgreSqlColumnsFolderContextMenuImage2() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject colsObj = new DbObject("columns", Kind.COLUMNS_FOLDER, "thunder_db", "public");
        colsObj.setTableName("collection_variables");
        ContextMenu menu = pane.buildMenuForTest(colsObj, profile);
        assertNotNull(menu);

        // 1. + New >
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertEquals("Column", newMenu.getItems().get(3).getText(), "Columns folder starts with Column");
        assertEquals("Unique Key", newMenu.getItems().get(4).getText());
        assertEquals("Foreign Key", newMenu.getItems().get(5).getText());
        assertEquals("Index", newMenu.getItems().get(6).getText());
        assertEquals("Check", newMenu.getItems().get(7).getText());
        assertEquals("Table", newMenu.getItems().get(8).getText(), "Immediately followed by Table with no separator");
        assertTrue(menuHasItem(newMenu, "Virtual Column"));
        assertTrue(menuHasItem(newMenu, "Virtual Foreign Key"));
        assertTrue(menuHasItem(newMenu, "Virtual View"));

        // No Modify
        assertNull(findItem(menu, "Modify"));

        // Copy/Paste (disabled)
        Menu cp = findMenu(menu, "Copy/Paste");
        assertNotNull(cp);
        assertTrue(cp.isDisable());

        // Open the List (F4)
        MenuItem openList = findItem(menu, "Open the List");
        assertNotNull(openList);
        assertEquals(KeyCode.F4, ((KeyCodeCombination) openList.getAccelerator()).getCode());

        // Refresh
        assertNotNull(findItem(menu, "Refresh"));

        // Rename… (disabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable());

        // Quick Documentation (disabled)
        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertTrue(quickDoc.isDisable());

        // Compare Schema Structure (disabled, no With)
        MenuItem compare = findItem(menu, "Compare Schema Structure");
        assertNotNull(compare);
        assertTrue(compare.isDisable());

        // NO Navigation and NO Find Usages on category folders
        assertNull(findMenu(menu, "Navigation"));
        assertNull(findItem(menu, "Find Usages"));

        // Introspection Level
        assertNotNull(findMenu(menu, "Introspection Level"));

        // SQL Scripts
        assertNotNull(findMenu(menu, "SQL Scripts"));

        // Columns folder does NOT have Object Actions
        assertNull(findMenu(menu, "Object Actions"));

        // Tools
        assertNotNull(findMenu(menu, "Tools"));

        // Import/Export
        assertNotNull(findMenu(menu, "Import/Export"));

        // Diagrams
        assertNotNull(findMenu(menu, "Diagrams"));

        // Diagnostics
        assertNotNull(findMenu(menu, "Diagnostics"));

        // Drop… (disabled)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertTrue(drop.isDisable());
    }

    @Test
    public void testPostgreSqlKeysFolderContextMenuImage3() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject keysObj = new DbObject("keys", Kind.KEYS_FOLDER, "thunder_db", "public");
        keysObj.setTableName("collection_variables");
        ContextMenu menu = pane.buildMenuForTest(keysObj, profile);
        assertNotNull(menu);

        // 1. + New >
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertEquals("Unique Key", newMenu.getItems().get(3).getText(), "Keys folder starts with Unique Key");
        assertEquals("Column", newMenu.getItems().get(4).getText());
        assertEquals("Foreign Key", newMenu.getItems().get(5).getText());
        assertEquals("Index", newMenu.getItems().get(6).getText());
        assertEquals("Check", newMenu.getItems().get(7).getText());
        assertEquals("Table", newMenu.getItems().get(8).getText());

        // 2. Modify Key… (Ctrl+F6, enabled)
        MenuItem modify = findItem(menu, "Modify Key");
        assertNotNull(modify);
        assertFalse(modify.isDisable());
        assertEquals(KeyCode.F6, ((KeyCodeCombination) modify.getAccelerator()).getCode());

        // Copy/Paste (disabled)
        Menu cp = findMenu(menu, "Copy/Paste");
        assertNotNull(cp);
        assertTrue(cp.isDisable());

        // Open the List (F4)
        MenuItem openList = findItem(menu, "Open the List");
        assertNotNull(openList);

        // Refresh
        assertNotNull(findItem(menu, "Refresh"));

        // Rename… (disabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable());

        // Quick Documentation (disabled)
        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertTrue(quickDoc.isDisable());

        // Compare Schema Structure (disabled)
        MenuItem compare = findItem(menu, "Compare Schema Structure");
        assertNotNull(compare);
        assertTrue(compare.isDisable());

        // NO Navigation and NO Find Usages
        assertNull(findMenu(menu, "Navigation"));
        assertNull(findItem(menu, "Find Usages"));

        // Introspection Level
        assertNotNull(findMenu(menu, "Introspection Level"));

        // SQL Scripts
        assertNotNull(findMenu(menu, "SQL Scripts"));

        // Keys folder HAS Object Actions
        Menu objActions = findMenu(menu, "Object Actions");
        assertNotNull(objActions);
        assertTrue(menuHasItem(objActions, "Show / Hide Schemas"));
        assertTrue(menuHasItem(objActions, "Forget Saved Passwords"));

        // Tools
        assertNotNull(findMenu(menu, "Tools"));

        // Import/Export
        assertNotNull(findMenu(menu, "Import/Export"));

        // Diagrams
        assertNotNull(findMenu(menu, "Diagrams"));

        // Diagnostics
        assertNotNull(findMenu(menu, "Diagnostics"));

        // Drop… (disabled)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertTrue(drop.isDisable());
    }

    @Test
    public void testPostgreSqlForeignKeysFolderContextMenuImage4() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject fkObj = new DbObject("foreign keys", Kind.FOREIGN_KEYS_FOLDER, "thunder_db", "public");
        fkObj.setTableName("collection_variables");
        ContextMenu menu = pane.buildMenuForTest(fkObj, profile);
        assertNotNull(menu);

        // 1. + New >
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertEquals("Foreign Key", newMenu.getItems().get(3).getText(), "Foreign keys folder starts with Foreign Key");
        assertEquals("Column", newMenu.getItems().get(4).getText());
        assertEquals("Unique Key", newMenu.getItems().get(5).getText());
        assertEquals("Index", newMenu.getItems().get(6).getText());
        assertEquals("Check", newMenu.getItems().get(7).getText());
        assertEquals("Table", newMenu.getItems().get(8).getText());

        // 2. Modify Foreign Key… (Ctrl+F6, enabled)
        MenuItem modify = findItem(menu, "Modify Foreign Key");
        assertNotNull(modify);
        assertFalse(modify.isDisable());
        assertEquals(KeyCode.F6, ((KeyCodeCombination) modify.getAccelerator()).getCode());

        // Copy/Paste (disabled)
        Menu cp = findMenu(menu, "Copy/Paste");
        assertNotNull(cp);
        assertTrue(cp.isDisable());

        // Open the List (F4)
        MenuItem openList = findItem(menu, "Open the List");
        assertNotNull(openList);

        // Refresh
        assertNotNull(findItem(menu, "Refresh"));

        // Rename… (disabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable());

        // Quick Documentation (disabled)
        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertTrue(quickDoc.isDisable());

        // Compare Schema Structure (disabled)
        MenuItem compare = findItem(menu, "Compare Schema Structure");
        assertNotNull(compare);
        assertTrue(compare.isDisable());

        // NO Navigation and NO Find Usages
        assertNull(findMenu(menu, "Navigation"));
        assertNull(findItem(menu, "Find Usages"));

        // Introspection Level
        assertNotNull(findMenu(menu, "Introspection Level"));

        // SQL Scripts
        assertNotNull(findMenu(menu, "SQL Scripts"));

        // Foreign keys folder HAS Object Actions
        Menu objActions = findMenu(menu, "Object Actions");
        assertNotNull(objActions);
        assertTrue(menuHasItem(objActions, "Show / Hide Schemas"));
        assertTrue(menuHasItem(objActions, "Forget Saved Passwords"));

        // Tools
        assertNotNull(findMenu(menu, "Tools"));

        // Import/Export
        assertNotNull(findMenu(menu, "Import/Export"));

        // Diagrams
        assertNotNull(findMenu(menu, "Diagrams"));

        // Diagnostics
        assertNotNull(findMenu(menu, "Diagnostics"));

        // Drop… (disabled)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertTrue(drop.isDisable());
    }

    @Test
    public void testPostgreSqlIndexesFolderContextMenuImage5() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject idxObj = new DbObject("indexes", Kind.INDEXES_FOLDER, "thunder_db", "public");
        idxObj.setTableName("collection_variables");
        ContextMenu menu = pane.buildMenuForTest(idxObj, profile);
        assertNotNull(menu);

        // 1. + New >
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertEquals("Index", newMenu.getItems().get(3).getText(), "Indexes folder starts with Index");
        assertEquals("Column", newMenu.getItems().get(4).getText());
        assertEquals("Unique Key", newMenu.getItems().get(5).getText());
        assertEquals("Foreign Key", newMenu.getItems().get(6).getText());
        assertEquals("Check", newMenu.getItems().get(7).getText());
        assertEquals("Table", newMenu.getItems().get(8).getText());

        // 2. Modify Index… (Ctrl+F6, enabled)
        MenuItem modify = findItem(menu, "Modify Index");
        assertNotNull(modify);
        assertFalse(modify.isDisable());
        assertEquals(KeyCode.F6, ((KeyCodeCombination) modify.getAccelerator()).getCode());

        // Copy/Paste (disabled)
        Menu cp = findMenu(menu, "Copy/Paste");
        assertNotNull(cp);
        assertTrue(cp.isDisable());

        // Open the List (F4)
        MenuItem openList = findItem(menu, "Open the List");
        assertNotNull(openList);

        // Refresh
        assertNotNull(findItem(menu, "Refresh"));

        // Rename… (disabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable());

        // Quick Documentation (disabled)
        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertTrue(quickDoc.isDisable());

        // Compare Schema Structure (disabled)
        MenuItem compare = findItem(menu, "Compare Schema Structure");
        assertNotNull(compare);
        assertTrue(compare.isDisable());

        // NO Navigation and NO Find Usages
        assertNull(findMenu(menu, "Navigation"));
        assertNull(findItem(menu, "Find Usages"));

        // Introspection Level
        assertNotNull(findMenu(menu, "Introspection Level"));

        // SQL Scripts
        assertNotNull(findMenu(menu, "SQL Scripts"));

        // Indexes folder does NOT have Object Actions
        assertNull(findMenu(menu, "Object Actions"));

        // Tools
        assertNotNull(findMenu(menu, "Tools"));

        // Import/Export
        assertNotNull(findMenu(menu, "Import/Export"));

        // Diagrams
        assertNotNull(findMenu(menu, "Diagrams"));

        // Diagnostics
        assertNotNull(findMenu(menu, "Diagnostics"));

        // Drop… (disabled)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertTrue(drop.isDisable());
    }

    @Test
    public void testPostgreSqlColumnItemContextMenuImage1() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject colObj = new DbObject("collection_id", Kind.COLUMN, "thunder_db", "public");
        colObj.setTableName("collection_variables");
        ContextMenu menu = pane.buildMenuForTest(colObj, profile);
        assertNotNull(menu);

        // 1. + New >
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertEquals("Column", newMenu.getItems().get(3).getText(), "Column item starts with Column");
        assertEquals("Unique Key", newMenu.getItems().get(4).getText());
        assertEquals("Foreign Key", newMenu.getItems().get(5).getText());
        assertEquals("Index", newMenu.getItems().get(6).getText());
        assertEquals("Check", newMenu.getItems().get(7).getText());
        assertEquals("Table", newMenu.getItems().get(8).getText(), "Followed by Table with no separator");
        assertTrue(menuHasItem(newMenu, "Virtual Column"));
        assertTrue(menuHasItem(newMenu, "Virtual Foreign Key"));
        assertTrue(menuHasItem(newMenu, "Virtual View"));

        // 2. Modify Column… (Ctrl+F6, enabled)
        MenuItem modify = findItem(menu, "Modify Column");
        assertNotNull(modify);
        assertFalse(modify.isDisable());
        assertEquals(KeyCode.F6, ((KeyCodeCombination) modify.getAccelerator()).getCode());

        // 3. Copy/Paste (enabled)
        Menu cp = findMenu(menu, "Copy/Paste");
        assertNotNull(cp);
        assertFalse(cp.isDisable());

        // 4. Edit Data (F4, enabled)
        MenuItem editData = findItem(menu, "Edit Data");
        assertNotNull(editData);
        assertFalse(editData.isDisable());
        assertEquals(KeyCode.F4, ((KeyCodeCombination) editData.getAccelerator()).getCode());
        assertNull(findItem(menu, "Open the List"));

        // 5. Refresh (Ctrl+F5)
        assertNotNull(findItem(menu, "Refresh"));

        // 6. Rename… (disabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable());

        // 7. Quick Documentation (enabled)
        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertFalse(quickDoc.isDisable());

        // 8. Compare Schema Structure With… (disabled)
        MenuItem compare = findItem(menu, "Compare Schema Structure With");
        assertNotNull(compare);
        assertTrue(compare.isDisable());

        // 9. Navigation
        Menu nav = findMenu(menu, "Navigation");
        assertNotNull(nav);
        assertTrue(menuHasItem(nav, "Go to DDL"));
        assertTrue(menuHasItem(nav, "Jump to Query Console"));
        assertTrue(menuHasItem(nav, "Select in Database Explorer"));

        // 10. Find Usages (disabled)
        MenuItem findUsages = findItem(menu, "Find Usages");
        assertNotNull(findUsages);
        assertTrue(findUsages.isDisable());

        // 11. Introspection Level
        assertNotNull(findMenu(menu, "Introspection Level"));

        // 12. SQL Scripts
        assertNotNull(findMenu(menu, "SQL Scripts"));

        // 13. Object Actions (enabled)
        Menu objActions = findMenu(menu, "Object Actions");
        assertNotNull(objActions);
        assertTrue(menuHasItem(objActions, "Show / Hide Schemas"));
        assertTrue(menuHasItem(objActions, "Forget Saved Passwords"));

        // 14. Tools
        assertNotNull(findMenu(menu, "Tools"));

        // 15. Import/Export
        assertNotNull(findMenu(menu, "Import/Export"));

        // 16. Diagrams
        assertNotNull(findMenu(menu, "Diagrams"));

        // 17. Diagnostics
        assertNotNull(findMenu(menu, "Diagnostics"));

        // 18. Drop… (enabled!)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertFalse(drop.isDisable());
    }

    @Test
    public void testPostgreSqlKeyItemContextMenuImage2() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject keyObj = new DbObject("collection_variables_pkey", Kind.KEY, "thunder_db", "public");
        keyObj.setTableName("collection_variables");
        ContextMenu menu = pane.buildMenuForTest(keyObj, profile);
        assertNotNull(menu);

        // 1. + New >
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertEquals("Unique Key", newMenu.getItems().get(3).getText(), "Key item starts with Unique Key");
        assertEquals("Column", newMenu.getItems().get(4).getText());
        assertEquals("Foreign Key", newMenu.getItems().get(5).getText());
        assertEquals("Index", newMenu.getItems().get(6).getText());
        assertEquals("Check", newMenu.getItems().get(7).getText());
        assertEquals("Table", newMenu.getItems().get(8).getText());

        // 2. Modify Key… (Ctrl+F6, enabled)
        MenuItem modify = findItem(menu, "Modify Key");
        assertNotNull(modify);
        assertFalse(modify.isDisable());
        assertEquals(KeyCode.F6, ((KeyCodeCombination) modify.getAccelerator()).getCode());

        // 3. Copy/Paste (enabled)
        Menu cp = findMenu(menu, "Copy/Paste");
        assertNotNull(cp);
        assertFalse(cp.isDisable());

        // 4. Edit Data (F4, enabled)
        MenuItem editData = findItem(menu, "Edit Data");
        assertNotNull(editData);
        assertFalse(editData.isDisable());
        assertNull(findItem(menu, "Open the List"));

        // 5. Refresh
        assertNotNull(findItem(menu, "Refresh"));

        // 6. Rename… (disabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable());

        // 7. Quick Documentation (enabled)
        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertFalse(quickDoc.isDisable());

        // 8. Compare Schema Structure With… (disabled)
        MenuItem compare = findItem(menu, "Compare Schema Structure With");
        assertNotNull(compare);
        assertTrue(compare.isDisable());

        // 9. Navigation
        assertNotNull(findMenu(menu, "Navigation"));

        // 10. Find Usages (disabled)
        assertNotNull(findItem(menu, "Find Usages"));

        // 11. Introspection Level
        assertNotNull(findMenu(menu, "Introspection Level"));

        // 12. SQL Scripts
        assertNotNull(findMenu(menu, "SQL Scripts"));

        // 13. Object Actions (enabled)
        Menu objActions = findMenu(menu, "Object Actions");
        assertNotNull(objActions);
        assertTrue(menuHasItem(objActions, "Show / Hide Schemas"));
        assertTrue(menuHasItem(objActions, "Forget Saved Passwords"));

        // 14. Tools
        assertNotNull(findMenu(menu, "Tools"));

        // 15. Import/Export
        assertNotNull(findMenu(menu, "Import/Export"));

        // 16. Diagrams
        assertNotNull(findMenu(menu, "Diagrams"));

        // 17. Diagnostics
        assertNotNull(findMenu(menu, "Diagnostics"));

        // 18. Drop… (enabled!)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertFalse(drop.isDisable());
    }

    @Test
    public void testPostgreSqlForeignKeyItemContextMenuImage3() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject fkObj = new DbObject("fkhdhikjmkt7o8ag61vwqxmxuta", Kind.FOREIGN_KEY, "thunder_db", "public");
        fkObj.setTableName("collection_variables");
        ContextMenu menu = pane.buildMenuForTest(fkObj, profile);
        assertNotNull(menu);

        // 1. + New >
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertEquals("Foreign Key", newMenu.getItems().get(3).getText(), "Foreign Key item starts with Foreign Key");
        assertEquals("Column", newMenu.getItems().get(4).getText());
        assertEquals("Unique Key", newMenu.getItems().get(5).getText());
        assertEquals("Index", newMenu.getItems().get(6).getText());
        assertEquals("Check", newMenu.getItems().get(7).getText());
        assertEquals("Table", newMenu.getItems().get(8).getText());

        // 2. Modify Foreign Key… (Ctrl+F6, enabled)
        MenuItem modify = findItem(menu, "Modify Foreign Key");
        assertNotNull(modify);
        assertFalse(modify.isDisable());
        assertEquals(KeyCode.F6, ((KeyCodeCombination) modify.getAccelerator()).getCode());

        // 3. Copy/Paste (enabled)
        Menu cp = findMenu(menu, "Copy/Paste");
        assertNotNull(cp);
        assertFalse(cp.isDisable());

        // 4. Edit Data (F4, enabled)
        MenuItem editData = findItem(menu, "Edit Data");
        assertNotNull(editData);
        assertFalse(editData.isDisable());
        assertNull(findItem(menu, "Open the List"));

        // 5. Refresh
        assertNotNull(findItem(menu, "Refresh"));

        // 6. Rename… (disabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable());

        // 7. Quick Documentation (enabled)
        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertFalse(quickDoc.isDisable());

        // 8. Compare Schema Structure With… (disabled)
        MenuItem compare = findItem(menu, "Compare Schema Structure With");
        assertNotNull(compare);
        assertTrue(compare.isDisable());

        // 9. Navigation
        assertNotNull(findMenu(menu, "Navigation"));

        // 10. Find Usages (disabled)
        assertNotNull(findItem(menu, "Find Usages"));

        // 11. Introspection Level
        assertNotNull(findMenu(menu, "Introspection Level"));

        // 12. SQL Scripts
        assertNotNull(findMenu(menu, "SQL Scripts"));

        // 13. Object Actions (enabled)
        Menu objActions = findMenu(menu, "Object Actions");
        assertNotNull(objActions);
        assertTrue(menuHasItem(objActions, "Show / Hide Schemas"));
        assertTrue(menuHasItem(objActions, "Forget Saved Passwords"));

        // 14. Tools
        assertNotNull(findMenu(menu, "Tools"));

        // 15. Import/Export
        assertNotNull(findMenu(menu, "Import/Export"));

        // 16. Diagrams
        assertNotNull(findMenu(menu, "Diagrams"));

        // 17. Diagnostics
        assertNotNull(findMenu(menu, "Diagnostics"));

        // 18. Drop… (enabled!)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertFalse(drop.isDisable());
    }

    @Test
    public void testPostgreSqlIndexItemContextMenuImage4() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createPostgreSqlProfile();
        DbObject idxObj = new DbObject("collection_variables_pkey", Kind.INDEX, "thunder_db", "public");
        idxObj.setTableName("collection_variables");
        ContextMenu menu = pane.buildMenuForTest(idxObj, profile);
        assertNotNull(menu);

        // 1. + New >
        Menu newMenu = findMenu(menu, "New");
        assertNotNull(newMenu);
        assertEquals("Index", newMenu.getItems().get(3).getText(), "Index item starts with Index");
        assertEquals("Column", newMenu.getItems().get(4).getText());
        assertEquals("Unique Key", newMenu.getItems().get(5).getText());
        assertEquals("Foreign Key", newMenu.getItems().get(6).getText());
        assertEquals("Check", newMenu.getItems().get(7).getText());
        assertEquals("Table", newMenu.getItems().get(8).getText());

        // 2. Modify Index… (Ctrl+F6, enabled)
        MenuItem modify = findItem(menu, "Modify Index");
        assertNotNull(modify);
        assertFalse(modify.isDisable());
        assertEquals(KeyCode.F6, ((KeyCodeCombination) modify.getAccelerator()).getCode());

        // 3. Copy/Paste (enabled)
        Menu cp = findMenu(menu, "Copy/Paste");
        assertNotNull(cp);
        assertFalse(cp.isDisable());

        // 4. Edit Data (F4, enabled)
        MenuItem editData = findItem(menu, "Edit Data");
        assertNotNull(editData);
        assertFalse(editData.isDisable());
        assertNull(findItem(menu, "Open the List"));

        // 5. Refresh
        assertNotNull(findItem(menu, "Refresh"));

        // 6. Rename… (disabled)
        MenuItem rename = findItem(menu, "Rename");
        assertNotNull(rename);
        assertTrue(rename.isDisable());

        // 7. Quick Documentation (enabled)
        MenuItem quickDoc = findItem(menu, "Quick Documentation");
        assertNotNull(quickDoc);
        assertFalse(quickDoc.isDisable());

        // 8. Compare Schema Structure With… (disabled)
        MenuItem compare = findItem(menu, "Compare Schema Structure With");
        assertNotNull(compare);
        assertTrue(compare.isDisable());

        // 9. Navigation
        assertNotNull(findMenu(menu, "Navigation"));

        // 10. Find Usages (disabled)
        assertNotNull(findItem(menu, "Find Usages"));

        // 11. Introspection Level
        assertNotNull(findMenu(menu, "Introspection Level"));

        // 12. SQL Scripts
        assertNotNull(findMenu(menu, "SQL Scripts"));

        // 13. Object Actions (enabled)
        Menu objActions = findMenu(menu, "Object Actions");
        assertNotNull(objActions);
        assertTrue(menuHasItem(objActions, "Show / Hide Schemas"));
        assertTrue(menuHasItem(objActions, "Forget Saved Passwords"));

        // 14. Tools
        assertNotNull(findMenu(menu, "Tools"));

        // 15. Import/Export
        assertNotNull(findMenu(menu, "Import/Export"));

        // 16. Diagrams
        assertNotNull(findMenu(menu, "Diagrams"));

        // 17. Diagnostics
        assertNotNull(findMenu(menu, "Diagnostics"));

        // 18. Drop… (enabled!)
        MenuItem drop = findItem(menu, "Drop");
        assertNotNull(drop);
        assertFalse(drop.isDisable());
    }
}


