package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.DdlGeneratorService;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.DbObject.Kind;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MssqlObjectsTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    private ConnectionProfile createMssqlProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setId("mssql-1");
        p.setName("mssql@localhost [3]");
        p.setType(DatabaseType.SQLSERVER);
        p.setHost("localhost");
        p.setPort(1433);
        p.setDatabase("CompanyDB");
        p.setUsername("sa");
        p.setPassword("secret");
        p.setSavePassword(true);
        return p;
    }

    @Test
    public void testBreadcrumbsMatchingExplorerHierarchy() {
        TreeItem<DbObject> root = new TreeItem<>(new DbObject("root", Kind.MESSAGE));
        TreeItem<DbObject> connItem = new TreeItem<>(new DbObject("mssql@localhost [3]", Kind.CONNECTION));
        root.getChildren().add(connItem);

        // Case 1: CompanyDB > Database Objects
        TreeItem<DbObject> dbItem = new TreeItem<>(new DbObject("CompanyDB", Kind.DATABASE, "CompanyDB", null));
        connItem.getChildren().add(dbItem);
        TreeItem<DbObject> dbObjsItem = new TreeItem<>(new DbObject("Database Objects", Kind.DATABASE_OBJECTS, "CompanyDB", null));
        dbItem.getChildren().add(dbObjsItem);

        assertEquals("Database > mssql@localhost [3] > CompanyDB > Database Objects",
                SchemaTreePane.buildBreadcrumb(dbObjsItem));

        // Case 2: CompanyDB > Database Objects > database principals (maps to roles) > db_accessadmin
        TreeItem<DbObject> dbPrincipalsFolder = new TreeItem<>(new DbObject("database principals", Kind.DATABASE_PRINCIPALS_FOLDER, "CompanyDB", null));
        dbObjsItem.getChildren().add(dbPrincipalsFolder);
        TreeItem<DbObject> dbAccessAdmin = new TreeItem<>(new DbObject("db_accessadmin", Kind.DATABASE_PRINCIPAL, "CompanyDB", null));
        dbPrincipalsFolder.getChildren().add(dbAccessAdmin);

        assertEquals("Database > mssql@localhost [3] > CompanyDB > Database Objects > roles > db_accessadmin",
                SchemaTreePane.buildBreadcrumb(dbAccessAdmin));

        // Case 3: Server Objects > collations > Albanian_100_BIN
        TreeItem<DbObject> serverObjsItem = new TreeItem<>(new DbObject("Server Objects", Kind.SERVER_OBJECTS));
        connItem.getChildren().add(serverObjsItem);
        TreeItem<DbObject> collationsFolder = new TreeItem<>(new DbObject("collations", Kind.COLLATIONS_FOLDER));
        serverObjsItem.getChildren().add(collationsFolder);
        TreeItem<DbObject> collation = new TreeItem<>(new DbObject("Albanian_100_BIN", Kind.COLLATION));
        collationsFolder.getChildren().add(collation);

        assertEquals("Database > mssql@localhost [3] > Server Objects > collations > Albanian_100_BIN",
                SchemaTreePane.buildBreadcrumb(collation));

        // Case 4: Server Objects > linked servers > firoze
        TreeItem<DbObject> linkedServersFolder = new TreeItem<>(new DbObject("linked servers", Kind.LINKED_SERVERS_FOLDER));
        serverObjsItem.getChildren().add(linkedServersFolder);
        TreeItem<DbObject> linkedServer = new TreeItem<>(new DbObject("firoze", Kind.LINKED_SERVER));
        linkedServersFolder.getChildren().add(linkedServer);

        assertEquals("Database > mssql@localhost [3] > Server Objects > linked servers > firoze",
                SchemaTreePane.buildBreadcrumb(linkedServer));

        // Case 5: Server Objects > server principals (maps to logins) > ##MS_DatabaseConnector##
        TreeItem<DbObject> serverPrincipalsFolder = new TreeItem<>(new DbObject("server principals", Kind.SERVER_PRINCIPALS_FOLDER));
        serverObjsItem.getChildren().add(serverPrincipalsFolder);
        TreeItem<DbObject> serverPrincipal = new TreeItem<>(new DbObject("##MS_DatabaseConnector##", Kind.SERVER_PRINCIPAL));
        serverPrincipalsFolder.getChildren().add(serverPrincipal);

        assertEquals("Database > mssql@localhost [3] > Server Objects > logins > ##MS_DatabaseConnector##",
                SchemaTreePane.buildBreadcrumb(serverPrincipal));
    }

    @Test
    public void testMssqlDdlGeneration() throws Exception {
        ConnectionProfile profile = createMssqlProfile();

        // 1. Assembly
        DbObject assembly = new DbObject("Microsoft.SqlServer.Types", Kind.ASSEMBLY, "CompanyDB", null);
        String assemblyDdl = DdlGeneratorService.generateDdl(profile, assembly);
        assertEquals("-- auto-generated definition\n-- No source text available\n", assemblyDdl);

        // 2. Collation
        DbObject collation = new DbObject("Albanian_100_BIN", Kind.COLLATION);
        String collationDdl = DdlGeneratorService.generateDdl(profile, collation);
        assertEquals("-- auto-generated definition\n-- No source text available\n", collationDdl);

        // 3. Linked Server
        DbObject linkedServer = new DbObject("firoze", Kind.LINKED_SERVER);
        String linkedServerDdl = DdlGeneratorService.generateDdl(profile, linkedServer);
        assertEquals("-- auto-generated definition\n-- No source text available\n", linkedServerDdl);

        // 4. Filegroup
        DbObject filegroup = new DbObject("PRIMARY", Kind.FILEGROUP, "CompanyDB", null);
        String filegroupDdl = DdlGeneratorService.generateDdl(profile, filegroup);
        assertEquals("-- auto-generated definition\nalter database CompanyDB\n    add filegroup [PRIMARY]\ngo\n", filegroupDdl);

        // 5. Database Principal (Role heuristic: db_accessadmin)
        DbObject rolePrincipal = new DbObject("db_accessadmin", Kind.DATABASE_PRINCIPAL, "CompanyDB", null);
        String roleDdl = DdlGeneratorService.generateDdl(profile, rolePrincipal);
        assertEquals("-- auto-generated definition\ncreate role db_accessadmin authorization dbo\ngo\n", roleDdl);

        // 6. Database Principal (User heuristic: guest)
        DbObject userPrincipal = new DbObject("guest", Kind.DATABASE_PRINCIPAL, "CompanyDB", null);
        String userDdl = DdlGeneratorService.generateDdl(profile, userPrincipal);
        assertEquals("-- auto-generated definition\ncreate user [guest] without login\ngo\n", userDdl);

        // 7. Server Principal (Server Role heuristic: ##MS_DatabaseConnector##)
        DbObject serverRole = new DbObject("##MS_DatabaseConnector##", Kind.SERVER_PRINCIPAL);
        String serverRoleDdl = DdlGeneratorService.generateDdl(profile, serverRole);
        assertEquals("-- auto-generated definition\ncreate server role ##MS_DatabaseConnector## authorization dbo\ngo\n", serverRoleDdl);

        // 8. Server Principal (Login heuristic: sa)
        DbObject serverLogin = new DbObject("sa", Kind.SERVER_PRINCIPAL);
        String serverLoginDdl = DdlGeneratorService.generateDdl(profile, serverLogin);
        assertEquals("-- auto-generated definition\ncreate login [sa] with password = '***'\ngo\n", serverLoginDdl);
    }

    @Test
    public void testIconsForMssqlObjects() {
        ConnectionProfile profile = createMssqlProfile();

        assertNotNull(Icons.forObject(new DbObject("collations", Kind.COLLATIONS_FOLDER), profile, false));
        assertNotNull(Icons.forObject(new DbObject("linked servers", Kind.LINKED_SERVERS_FOLDER), profile, false));
        assertNotNull(Icons.forObject(new DbObject("server principals", Kind.SERVER_PRINCIPALS_FOLDER), profile, false));
        assertNotNull(Icons.forObject(new DbObject("assemblies", Kind.ASSEMBLIES_FOLDER), profile, false));
        assertNotNull(Icons.forObject(new DbObject("filegroups", Kind.FILEGROUPS_FOLDER), profile, false));
        assertNotNull(Icons.forObject(new DbObject("database principals", Kind.DATABASE_PRINCIPALS_FOLDER), profile, false));

        assertNotNull(Icons.forObject(new DbObject("Albanian_100_BIN", Kind.COLLATION), profile, false));
        assertNotNull(Icons.forObject(new DbObject("firoze", Kind.LINKED_SERVER), profile, false));
        assertNotNull(Icons.forObject(new DbObject("##MS_DatabaseConnector##", Kind.SERVER_PRINCIPAL), profile, false));
        assertNotNull(Icons.forObject(new DbObject("Microsoft.SqlServer.Types", Kind.ASSEMBLY), profile, false));
        assertNotNull(Icons.forObject(new DbObject("PRIMARY", Kind.FILEGROUP), profile, false));
        assertNotNull(Icons.forObject(new DbObject("db_accessadmin", Kind.DATABASE_PRINCIPAL), profile, false));
    }

    @Test
    public void testToolbarStateForMssqlObjects() {
        SchemaTreePane pane = new SchemaTreePane(null);
        ConnectionProfile profile = createMssqlProfile();
        pane.addConnectionNode(profile);
        TreeItem<DbObject> connItem = pane.getTree().getRoot().getChildren().get(0);

        TreeItem<DbObject> collationItem = new TreeItem<>(new DbObject("Albanian_100_BIN", Kind.COLLATION));
        connItem.getChildren().add(collationItem);
        pane.updateToolbarState(collationItem);
        assertFalse(pane.getGoToDdlButton().isDisabled(), "Go to DDL should be active for collation");
        assertTrue(pane.getEditDataButton().isDisabled(), "Edit Data should be inactive for collation");

        TreeItem<DbObject> collationsFolder = new TreeItem<>(new DbObject("collations", Kind.COLLATIONS_FOLDER));
        connItem.getChildren().add(collationsFolder);
        pane.updateToolbarState(collationsFolder);
        assertTrue(pane.getGoToDdlButton().isDisabled(), "Go to DDL should be inactive for collations folder");
    }

    @Test
    public void testVirtualViewSessionsForMssql() throws Exception {
        ConnectionProfile profile = createMssqlProfile();
        DbObject vvFolder = new DbObject("virtual views", Kind.VIRTUAL_VIEWS_FOLDER);
        var views = com.roze.dbnavigator.db.MetadataService.loadVirtualViews(profile, vvFolder);
        assertEquals(1, views.size());

        DbObject sessions = views.get(0);
        assertEquals("sessions", sessions.getName());
        assertEquals(Kind.VIRTUAL_VIEW, sessions.getKind());
        assertNotNull(sessions.getQualifiedNameOverride());
        assertTrue(sessions.getQualifiedNameOverride().contains("master.dbo.sysprocesses"));
        assertTrue(sessions.getQualifiedNameOverride().contains("SPID"));
        assertTrue(sessions.getQualifiedNameOverride().contains("Status"));
        assertTrue(sessions.getQualifiedNameOverride().contains("Login"));
        assertTrue(sessions.getQualifiedNameOverride().contains("HostName"));
        assertTrue(sessions.getQualifiedNameOverride().contains("BlkBy"));
        assertTrue(sessions.getQualifiedNameOverride().contains("DBName"));
        assertTrue(sessions.getQualifiedNameOverride().contains("Command"));
        assertEquals(sessions.getQualifiedNameOverride(), sessions.qualifiedName());

        DataTab dataTab = new DataTab(profile, sessions);
        assertEquals("sessions [mssql@localhost [3]]", dataTab.getText());

        String ddl = DdlGeneratorService.generateDdl(profile, sessions);
        assertTrue(ddl.contains("create virtual view sessions as"));
        assertTrue(ddl.contains("from master.dbo.sysprocesses;"));
        assertTrue(ddl.contains("SPID"));
    }
}
