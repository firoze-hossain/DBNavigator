package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.DdlGeneratorService;
import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.DbObject.Kind;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MysqlMariaDbObjectsTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    private ConnectionProfile createMariaDbProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setId("mariadb-1");
        p.setName("mariadb@localhost");
        p.setType(DatabaseType.MARIADB);
        p.setHost("localhost");
        p.setPort(3307);
        p.setDatabase("nation");
        p.setUsername("root");
        return p;
    }

    private ConnectionProfile createMysqlProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setId("mysql-1");
        p.setName("mysql@localhost");
        p.setType(DatabaseType.MYSQL);
        p.setHost("localhost");
        p.setPort(3306);
        p.setDatabase("bagisto");
        p.setUsername("root");
        return p;
    }

    @Test
    public void testMariaDbBreadcrumbsMatchingExplorerHierarchy() {
        TreeItem<DbObject> root = new TreeItem<>(new DbObject("root", Kind.MESSAGE));
        TreeItem<DbObject> connItem = new TreeItem<>(new DbObject("mariadb@localhost", Kind.CONNECTION));
        root.getChildren().add(connItem);

        // Case 1: virtual views > sessions
        TreeItem<DbObject> vvFolder = new TreeItem<>(new DbObject("virtual views", Kind.VIRTUAL_VIEWS_FOLDER));
        connItem.getChildren().add(vvFolder);
        TreeItem<DbObject> sessionsItem = new TreeItem<>(new DbObject("sessions", Kind.VIRTUAL_VIEW));
        vvFolder.getChildren().add(sessionsItem);

        assertEquals("Database > mariadb@localhost > virtual views > sessions",
                SchemaTreePane.buildBreadcrumb(sessionsItem));

        // Case 2: Server Objects > collations > armscii8_general_nopad_ci
        TreeItem<DbObject> serverObjsItem = new TreeItem<>(new DbObject("Server Objects", Kind.SERVER_OBJECTS));
        connItem.getChildren().add(serverObjsItem);
        TreeItem<DbObject> collationsFolder = new TreeItem<>(new DbObject("collations", Kind.COLLATIONS_FOLDER));
        serverObjsItem.getChildren().add(collationsFolder);
        TreeItem<DbObject> collation = new TreeItem<>(new DbObject("armscii8_general_nopad_ci", Kind.COLLATION));
        collationsFolder.getChildren().add(collation);

        assertEquals("Database > mariadb@localhost > Server Objects > collations > armscii8_general_nopad_ci",
                SchemaTreePane.buildBreadcrumb(collation));

        // Case 3: Server Objects > users > mariadb.sys
        TreeItem<DbObject> usersFolder = new TreeItem<>(new DbObject("users", Kind.USERS_FOLDER));
        serverObjsItem.getChildren().add(usersFolder);
        DbObject userObj = new DbObject("mariadb.sys", Kind.USER);
        userObj.setDetail("@localhost");
        TreeItem<DbObject> user = new TreeItem<>(userObj);
        usersFolder.getChildren().add(user);

        assertEquals("Database > mariadb@localhost > Server Objects > users > mariadb.sys",
                SchemaTreePane.buildBreadcrumb(user));
    }

    @Test
    public void testMysqlBreadcrumbsMatchingExplorerHierarchy() {
        TreeItem<DbObject> root = new TreeItem<>(new DbObject("root", Kind.MESSAGE));
        TreeItem<DbObject> connItem = new TreeItem<>(new DbObject("mysql@localhost", Kind.CONNECTION));
        root.getChildren().add(connItem);

        // Case 1: virtual views > sessions
        TreeItem<DbObject> vvFolder = new TreeItem<>(new DbObject("virtual views", Kind.VIRTUAL_VIEWS_FOLDER));
        connItem.getChildren().add(vvFolder);
        TreeItem<DbObject> sessionsItem = new TreeItem<>(new DbObject("sessions", Kind.VIRTUAL_VIEW));
        vvFolder.getChildren().add(sessionsItem);

        assertEquals("Database > mysql@localhost > virtual views > sessions",
                SchemaTreePane.buildBreadcrumb(sessionsItem));

        // Case 2: Server Objects > users > mysql.infoschema
        TreeItem<DbObject> serverObjsItem = new TreeItem<>(new DbObject("Server Objects", Kind.SERVER_OBJECTS));
        connItem.getChildren().add(serverObjsItem);
        TreeItem<DbObject> usersFolder = new TreeItem<>(new DbObject("users", Kind.USERS_FOLDER));
        serverObjsItem.getChildren().add(usersFolder);
        DbObject userObj = new DbObject("mysql.infoschema", Kind.USER);
        userObj.setDetail("@localhost");
        TreeItem<DbObject> user = new TreeItem<>(userObj);
        usersFolder.getChildren().add(user);

        assertEquals("Database > mysql@localhost > Server Objects > users > mysql.infoschema",
                SchemaTreePane.buildBreadcrumb(user));
    }

    @Test
    public void testVirtualViewsLoading() {
        ConnectionProfile maria = createMariaDbProfile();
        List<DbObject> mariaVv = MetadataService.loadVirtualViews(maria, null);
        assertEquals(1, mariaVv.size());
        DbObject mariaSessions = mariaVv.get(0);
        assertEquals("sessions", mariaSessions.getName());
        assertTrue(mariaSessions.qualifiedName().contains("PROGRESS AS Progress"));
        assertTrue(mariaSessions.qualifiedName().contains("information_schema.PROCESSLIST"));

        ConnectionProfile mysql = createMysqlProfile();
        List<DbObject> mysqlVv = MetadataService.loadVirtualViews(mysql, null);
        assertEquals(1, mysqlVv.size());
        DbObject mysqlSessions = mysqlVv.get(0);
        assertEquals("sessions", mysqlSessions.getName());
        assertFalse(mysqlSessions.qualifiedName().contains("PROGRESS AS Progress"));
        assertTrue(mysqlSessions.qualifiedName().contains("information_schema.PROCESSLIST"));
    }

    @Test
    public void testVirtualViewsDdlGeneration() throws Exception {
        ConnectionProfile maria = createMariaDbProfile();
        DbObject sessions = new DbObject("sessions", Kind.VIRTUAL_VIEW);
        String mariaDdl = DdlGeneratorService.generateDdl(maria, sessions);
        assertTrue(mariaDdl.contains("from information_schema.PROCESSLIST;"));
        assertTrue(mariaDdl.contains("Progress"));

        ConnectionProfile mysql = createMysqlProfile();
        String mysqlDdl = DdlGeneratorService.generateDdl(mysql, sessions);
        assertTrue(mysqlDdl.contains("from information_schema.PROCESSLIST;"));
        assertFalse(mysqlDdl.contains("Progress"));
    }

    @Test
    public void testCollationDdlGeneration() throws Exception {
        ConnectionProfile maria = createMariaDbProfile();
        DbObject collation = new DbObject("armscii8_general_nopad_ci", Kind.COLLATION);
        String ddl = DdlGeneratorService.generateDdl(maria, collation);
        assertEquals("-- auto-generated definition\n-- No source text available\n", ddl);
    }

    @Test
    public void testUserDdlGeneration() throws Exception {
        ConnectionProfile mysql = createMysqlProfile();
        DbObject roleObj = new DbObject("mysql.infoschema", Kind.USER);
        roleObj.setDetail("@localhost");
        String roleDdl = DdlGeneratorService.generateDdl(mysql, roleObj);
        assertEquals("-- auto-generated definition\ncreate role 'mysql.infoschema'@localhost;\n", roleDdl);

        DbObject userObj = new DbObject("root", Kind.USER);
        userObj.setDetail("@localhost");
        String userDdl = DdlGeneratorService.generateDdl(mysql, userObj);
        assertEquals("-- auto-generated definition\ncreate user 'root'@localhost;\n", userDdl);

        ConnectionProfile maria = createMariaDbProfile();
        DbObject emptyUserObj = new DbObject("\"\"", Kind.USER);
        emptyUserObj.setDetail("@localhost");
        String emptyUserDdl = DdlGeneratorService.generateDdl(maria, emptyUserObj);
        assertEquals("-- auto-generated definition\ncreate user ''@localhost;\n", emptyUserDdl);
    }

    @Test
    public void testIconsForCollationAndUser() {
        assertNotNull(Icons.forObject(new DbObject("collations", Kind.COLLATIONS_FOLDER), false));
        assertNotNull(Icons.forObject(new DbObject("armscii8_general_ci", Kind.COLLATION), false));
        assertNotNull(Icons.forObject(new DbObject("users", Kind.USERS_FOLDER), false));
        assertNotNull(Icons.forObject(new DbObject("root", Kind.USER), false));
    }
}
