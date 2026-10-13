package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.DbObject.Kind;
import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
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
}

