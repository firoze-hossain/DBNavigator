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

        List<String> itemTexts = menu.getItems().stream().map(MenuItem::getText).filter(java.util.Objects::nonNull).toList();
        assertTrue(itemTexts.contains("Go to DDL"), "Sequence menu must contain 'Go to DDL'");
        assertTrue(itemTexts.contains("Jump to Query Console"), "Sequence menu must contain 'Jump to Query Console'");
        assertTrue(itemTexts.contains("Refresh"), "Sequence menu must contain 'Refresh'");

        assertNotNull(Icons.forObject(seq, profile, false), "Sequence must have non-null icon");
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
}

