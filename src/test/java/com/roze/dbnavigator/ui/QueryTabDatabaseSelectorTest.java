package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import com.roze.dbnavigator.model.DbObject;
import javafx.application.Platform;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class QueryTabDatabaseSelectorTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    private ConnectionProfile createPostgresProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setId("pg-test-id");
        p.setName("postgres@localhost");
        p.setType(DatabaseType.POSTGRESQL);
        p.setHost("localhost");
        p.setPort(5432);
        p.setDatabase("postgres");
        p.setUsername("postgres");
        p.setVisibleDatabases(new ArrayList<>(List.of("nexacom_db", "thunder_db", "tik")));
        return p;
    }

    private ConnectionProfile createSqliteProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setId("sqlite-test-id");
        p.setName("identifier.sqlite");
        p.setType(DatabaseType.SQLITE);
        p.setDatabase("/tmp/test.db");
        return p;
    }

    private ConnectionProfile createSqlServerProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setId("mssql-test-id");
        p.setName("mssq@localhost");
        p.setType(DatabaseType.SQLSERVER);
        p.setDatabase("master");
        return p;
    }

    private ConnectionProfile createOracleProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setId("oracle-test-id");
        p.setName("oracle@localhost");
        p.setType(DatabaseType.ORACLE);
        p.setUsername("SYSTEM");
        return p;
    }

    private ConnectionProfile createMysqlProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setId("mysql-test-id");
        p.setName("mysql@localhost");
        p.setType(DatabaseType.MYSQL);
        p.setDatabase("bagisto");
        return p;
    }

    @Test
    public void testPostgresSelectorFormatAndIntrospectionBanner() {
        runOnFxThread(() -> {
            ConnectionProfile profile = createPostgresProfile();
            // Opening console with un-introspected database "postgres"
            QueryTab tab = new QueryTab(null, profile, "postgres", "console_14");

            MenuButton selector = tab.getDbSelectorButton();
            assertNotNull(selector, "PostgreSQL console must have a database selector MenuButton");
            assertEquals("postgres.<schema>", selector.getText(),
                    "PostgreSQL selector text should format as <database>.<schema>");

            HBox banner = tab.getIntrospectionBanner();
            assertNotNull(banner, "Introspection banner must be initialized");
            assertTrue(banner.isVisible(),
                    "Banner should be visible when current database ('postgres') is not in visibleDatabases");

            // Switch to introspected database "nexacom_db"
            tab.switchDatabase("nexacom_db");
            assertEquals("nexacom_db", tab.getCatalog());
            assertEquals("nexacom_db.<schema>", selector.getText());
            assertTrue(tab.getText().contains("[nexacom_db]"), "Tab text should update with new catalog");
            assertFalse(banner.isVisible(),
                    "Banner should be hidden when current database ('nexacom_db') is introspected");
        });
    }

    private void runOnFxThread(Runnable r) {
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> err = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                r.run();
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        try {
            assertTrue(latch.await(5, java.util.concurrent.TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            fail(e);
        }
        if (err.get() != null) {
            if (err.get() instanceof RuntimeException re) throw re;
            if (err.get() instanceof AssertionError ae) throw ae;
            throw new RuntimeException(err.get());
        }
    }

    @Test
    public void testSqliteHasNoDropdownAndBannerHidden() {
        runOnFxThread(() -> {
            ConnectionProfile profile = createSqliteProfile();
            QueryTab tab = new QueryTab(null, profile, null, "console_3");

            assertNull(tab.getDbSelectorButton(),
                    "SQLite only has 'main', so no dropdown MenuButton is created");
            assertEquals("main", tab.formatSelectorText(null));

            HBox banner = tab.getIntrospectionBanner();
            assertNotNull(banner);
            assertFalse(banner.isVisible(), "Introspection banner must always be hidden for SQLite");
        });
    }

    @Test
    public void testSqlServerAndOracleSelectorFormatting() {
        runOnFxThread(() -> {
            ConnectionProfile mssql = createSqlServerProfile();
            QueryTab mssqlTab = new QueryTab(null, mssql, "CompanyDB", "console_2");
            assertEquals("CompanyDB.<schema>", mssqlTab.formatSelectorText("CompanyDB"));
            assertEquals("CompanyDB.<schema>", mssqlTab.getDbSelectorButton().getText());

            ConnectionProfile oracle = createOracleProfile();
            QueryTab oracleTab = new QueryTab(null, oracle, "HELLO", "console_2");
            assertEquals("HELLO", oracleTab.formatSelectorText("hello"));
            assertEquals("HELLO", oracleTab.getDbSelectorButton().getText());
        });
    }

    @Test
    public void testMysqlSelectorFormatting() {
        runOnFxThread(() -> {
            ConnectionProfile mysql = createMysqlProfile();
            QueryTab tab = new QueryTab(null, mysql, "bagisto", "console_1");
            assertEquals("bagisto", tab.formatSelectorText("bagisto"));
            assertEquals("bagisto", tab.getDbSelectorButton().getText());

            tab.switchDatabase("sakila");
            assertEquals("sakila", tab.getCatalog());
            assertEquals("sakila", tab.getDbSelectorButton().getText());
        });
    }

    @Test
    public void testEmptyFolderHidingLogic() {
        ConnectionProfile profile = createPostgresProfile();
        DbObject schema = new DbObject("public", DbObject.Kind.SCHEMA, "postgres", "public");

        // When connection is unavailable, fallback must only return tables folder, no empty views/routines
        List<DbObject> folders = MetadataService.loadSchemaChildren(profile, schema);
        assertNotNull(folders);
        assertFalse(folders.isEmpty(), "Folders should at least contain tables");
        assertEquals(1, folders.size(), "Fallback/empty schema should only contain tables folder");
        assertEquals("tables", folders.get(0).getName().toLowerCase());
        assertEquals(DbObject.Kind.TABLES_FOLDER, folders.get(0).getKind());
    }
}
