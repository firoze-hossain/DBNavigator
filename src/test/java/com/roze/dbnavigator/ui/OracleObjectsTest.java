package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.DdlGeneratorService;
import com.roze.dbnavigator.db.JdbcClient;
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

public class OracleObjectsTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    private ConnectionProfile createOracleProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setId("oracle-1");
        p.setName("oracle@localhost");
        p.setType(DatabaseType.ORACLE);
        p.setHost("localhost");
        p.setPort(1521);
        p.setDatabase("ORCL");
        p.setUsername("system");
        p.setPassword("secret");
        p.setSavePassword(true);
        return p;
    }

    @Test
    public void testBreadcrumbsMatchingExplorerHierarchy() {
        TreeItem<DbObject> root = new TreeItem<>(new DbObject("root", Kind.MESSAGE));
        TreeItem<DbObject> connItem = new TreeItem<>(new DbObject("oracle@localhost", Kind.CONNECTION));
        root.getChildren().add(connItem);

        // Case 1: virtual views > sessions
        TreeItem<DbObject> vvFolder = new TreeItem<>(new DbObject("virtual views", Kind.VIRTUAL_VIEWS_FOLDER));
        connItem.getChildren().add(vvFolder);
        TreeItem<DbObject> sessionsItem = new TreeItem<>(new DbObject("sessions", Kind.VIRTUAL_VIEW));
        vvFolder.getChildren().add(sessionsItem);

        assertEquals("Database > oracle@localhost > virtual views > sessions",
                SchemaTreePane.buildBreadcrumb(sessionsItem));

        // Case 2: Server Objects > tablespaces > SYSAUX > /opt/oracle/oradata/ORCLCDB/ORCL/sysaux01.dbf
        TreeItem<DbObject> serverObjsItem = new TreeItem<>(new DbObject("Server Objects", Kind.SERVER_OBJECTS));
        connItem.getChildren().add(serverObjsItem);
        TreeItem<DbObject> tsFolder = new TreeItem<>(new DbObject("tablespaces", Kind.TABLESPACES_FOLDER));
        serverObjsItem.getChildren().add(tsFolder);
        TreeItem<DbObject> sysaux = new TreeItem<>(new DbObject("SYSAUX", Kind.TABLESPACE));
        tsFolder.getChildren().add(sysaux);
        TreeItem<DbObject> datafile = new TreeItem<>(new DbObject("/opt/oracle/oradata/ORCLCDB/ORCL/sysaux01.dbf", Kind.DATA_FILE));
        sysaux.getChildren().add(datafile);

        assertEquals("Database > oracle@localhost > Server Objects > tablespaces > SYSAUX > /opt/oracle/oradata/ORCLCDB/ORCL/sysaux01.dbf",
                SchemaTreePane.buildBreadcrumb(datafile));

        // Case 3: Server Objects > users > GGG
        TreeItem<DbObject> usersFolder = new TreeItem<>(new DbObject("users", Kind.USERS_FOLDER));
        serverObjsItem.getChildren().add(usersFolder);
        TreeItem<DbObject> user = new TreeItem<>(new DbObject("GGG", Kind.USER));
        usersFolder.getChildren().add(user);

        assertEquals("Database > oracle@localhost > Server Objects > users > GGG",
                SchemaTreePane.buildBreadcrumb(user));
    }

    @Test
    public void testOracleTablespaceDetailFormatting() {
        // SYSAUX: permanent, online, block size 8192, force logging
        assertEquals("permanent, online, block size 8192, force logging",
                MetadataService.formatOracleTablespaceDetail("PERMANENT", "ONLINE", 8192, "LOGGING", "YES"));

        // SYSTEM: permanent, online, block size 8192, force logging
        assertEquals("permanent, online, block size 8192, force logging",
                MetadataService.formatOracleTablespaceDetail("PERMANENT", "ONLINE", 8192, "LOGGING", "YES"));

        // TEMP: temporary, online, block size 8192, no logging
        assertEquals("temporary, online, block size 8192, no logging",
                MetadataService.formatOracleTablespaceDetail("TEMPORARY", "ONLINE", 8192, "NOLOGGING", "NO"));

        // UNDOTBS1: undo, online, block size 8192, logging
        assertEquals("undo, online, block size 8192, logging",
                MetadataService.formatOracleTablespaceDetail("UNDO", "ONLINE", 8192, "LOGGING", "NO"));

        // USERS: permanent, online, block size 8192, logging
        assertEquals("permanent, online, block size 8192, logging",
                MetadataService.formatOracleTablespaceDetail("PERMANENT", "ONLINE", 8192, "LOGGING", "NO"));
    }

    @Test
    public void testFileSizeFormatting() {
        // 356.52 M (exact value matching sysaux01.dbf in image 2)
        long sysauxBytes = 373833728L;
        assertEquals("356.52 M", MetadataService.formatFileSize(sysauxBytes));

        assertEquals("2.00 G", MetadataService.formatFileSize(2L * 1024 * 1024 * 1024));
        assertEquals("512.00 K", MetadataService.formatFileSize(512L * 1024));
        assertEquals("500 B", MetadataService.formatFileSize(500L));
        assertEquals("0 B", MetadataService.formatFileSize(0L));
    }

    @Test
    public void testOracleDdlGeneration() throws Exception {
        ConnectionProfile profile = createOracleProfile();

        // 1. User DDL
        DbObject user = new DbObject("GGG", Kind.USER);
        String userDdl = DdlGeneratorService.generateDdl(profile, user);
        assertTrue(userDdl.contains("create user GGG identified by ??? default tablespace USERS temporary tablespace TEMP"));
        assertTrue(userDdl.endsWith("/\n"));

        // 2. Datafile DDL
        DbObject datafile = new DbObject("/opt/oracle/oradata/ORCLCDB/ORCL/sysaux01.dbf", Kind.DATA_FILE);
        String dfDdl = DdlGeneratorService.generateDdl(profile, datafile);
        assertEquals("-- auto-generated definition\n-- No source text available\n", dfDdl);

        // 3. Virtual View sessions DDL
        DbObject sessions = new DbObject("sessions", Kind.VIRTUAL_VIEW);
        String vvDdl = DdlGeneratorService.generateDdl(profile, sessions);
        assertTrue(vvDdl.contains("select *\nfrom v$session;"));

        // 4. Tablespace DDL
        DbObject sysaux = new DbObject("SYSAUX", Kind.TABLESPACE);
        String tsDdl = DdlGeneratorService.generateDdl(profile, sysaux);
        assertTrue(tsDdl.contains("create tablespace SYSAUX;"));
    }

    @Test
    public void testHexFormattingForRawColumns() {
        // SADDR row 1 from image 1: 0x000000008C70C200
        byte[] saddrBytes = new byte[] { 0, 0, 0, 0, (byte) 0x8C, 0x70, (byte) 0xC2, 0 };
        assertEquals("0x000000008C70C200", JdbcClient.formatValue(saddrBytes));

        // PADDR row 1 from image 1: 0x000000008C529888
        byte[] paddrBytes = new byte[] { 0, 0, 0, 0, (byte) 0x8C, 0x52, (byte) 0x98, (byte) 0x88 };
        assertEquals("0x000000008C529888", JdbcClient.formatValue(paddrBytes));

        // Regular types pass through
        assertEquals("hello", JdbcClient.formatValue("hello"));
        assertEquals("123", JdbcClient.formatValue(123));
        assertNull(JdbcClient.formatValue(null));
    }

    @Test
    public void testOracleVirtualViewsConfigured() {
        ConnectionProfile profile = createOracleProfile();
        DbObject vvFolder = new DbObject("virtual views", Kind.VIRTUAL_VIEWS_FOLDER);
        List<DbObject> views = MetadataService.loadVirtualViews(profile, vvFolder);

        assertEquals(1, views.size());
        DbObject sessions = views.get(0);
        assertEquals("sessions", sessions.getName());
        assertEquals("v$session", sessions.qualifiedName());
    }

    @Test
    public void testIconsForOracleObjects() {
        assertNotNull(Icons.forObject(new DbObject("users", Kind.USERS_FOLDER), false));
        assertNotNull(Icons.forObject(new DbObject("GGG", Kind.USER), false));
        assertNotNull(Icons.forObject(new DbObject("/path/to/file.dbf", Kind.DATA_FILE), false));
        assertNotNull(Icons.forObject(new DbObject("sessions", Kind.VIRTUAL_VIEW), false));
    }

    @Test
    public void testComputeBadgeAcrossEngines() {
        // SQLite
        ConnectionProfile sqlite = new ConnectionProfile();
        sqlite.setType(DatabaseType.SQLITE);
        assertEquals("1", SchemaTreePane.computeBadge(sqlite));

        // Oracle default (empty filter)
        ConnectionProfile oracleDefault = new ConnectionProfile();
        oracleDefault.setType(DatabaseType.ORACLE);
        oracleDefault.setTotalDatabases(39);
        assertEquals("2 of 39", SchemaTreePane.computeBadge(oracleDefault));

        // Oracle customized filter
        ConnectionProfile oracleFiltered = new ConnectionProfile();
        oracleFiltered.setType(DatabaseType.ORACLE);
        oracleFiltered.setTotalDatabases(39);
        oracleFiltered.setVisibleDatabases(List.of("GGG", "HELLO", "SYS", "SYSTEM"));
        assertEquals("4 of 39", SchemaTreePane.computeBadge(oracleFiltered));

        // PostgreSQL
        ConnectionProfile pg = new ConnectionProfile();
        pg.setType(DatabaseType.POSTGRESQL);
        pg.setTotalDatabases(13);
        pg.setVisibleDatabases(List.of("nexacom_db", "thunder_db", "tik"));
        assertEquals("3 of 13", SchemaTreePane.computeBadge(pg));

        // PostgreSQL without filter (all visible)
        ConnectionProfile pgAll = new ConnectionProfile();
        pgAll.setType(DatabaseType.POSTGRESQL);
        pgAll.setTotalDatabases(13);
        assertEquals("13 of 13", SchemaTreePane.computeBadge(pgAll));

        // MariaDB
        ConnectionProfile maria = new ConnectionProfile();
        maria.setType(DatabaseType.MARIADB);
        maria.setTotalDatabases(8);
        maria.setVisibleDatabases(List.of("hellodb", "nation"));
        assertEquals("2 of 8", SchemaTreePane.computeBadge(maria));

        // MongoDB
        ConnectionProfile mongo = new ConnectionProfile();
        mongo.setType(DatabaseType.MONGODB);
        mongo.setTotalDatabases(6);
        mongo.setVisibleDatabases(List.of("oladb", "studentdb"));
        assertEquals("2 of 6", SchemaTreePane.computeBadge(mongo));

        // SQL Server
        ConnectionProfile mssql = new ConnectionProfile();
        mssql.setType(DatabaseType.SQLSERVER);
        mssql.setTotalDatabases(6);
        assertEquals("6 of 6", SchemaTreePane.computeBadge(mssql));

        // MySQL
        ConnectionProfile mysql = new ConnectionProfile();
        mysql.setType(DatabaseType.MYSQL);
        mysql.setTotalDatabases(16);
        mysql.setVisibleDatabases(List.of("bagisto", "classicmodels", "dddd", "sakila"));
        assertEquals("4 of 16", SchemaTreePane.computeBadge(mysql));
    }

    @Test
    public void testIsOracleSystemSchema() {
        assertTrue(MetadataService.isOracleSystemSchema("SYS"));
        assertTrue(MetadataService.isOracleSystemSchema("sys"));
        assertTrue(MetadataService.isOracleSystemSchema("SYSTEM"));
        assertTrue(MetadataService.isOracleSystemSchema("system"));
        assertTrue(MetadataService.isOracleSystemSchema("ANONYMOUS"));
        assertTrue(MetadataService.isOracleSystemSchema("XDB"));
        assertTrue(MetadataService.isOracleSystemSchema("APPQOSSYS"));

        assertFalse(MetadataService.isOracleSystemSchema("GGG"));
        assertFalse(MetadataService.isOracleSystemSchema("HELLO"));
        assertFalse(MetadataService.isOracleSystemSchema("MY_APP"));
        assertFalse(MetadataService.isOracleSystemSchema(null));
    }

    @Test
    public void testTotalDatabasesProfileCopy() {
        ConnectionProfile p = new ConnectionProfile();
        p.setTotalDatabases(39);
        ConnectionProfile copy = p.copy();
        assertEquals(39, copy.getTotalDatabases());
    }
}
