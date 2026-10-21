package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.Project;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class NavigationBarBreadcrumbTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    private void runOnFx(Runnable action) throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        boolean ok = latch.await(15, TimeUnit.SECONDS);
        if (!ok) {
            fail("JavaFX operation timed out");
        }
        if (err.get() != null) {
            throw err.get();
        }
    }

    @Test
    public void testDynamicConsoleBreadcrumbsMatchingDataGrip() throws Throwable {
        runOnFx(() -> {
            Stage stage = new Stage();
            Project testProj = new Project("test-breadcrumb-proj", "/tmp/test-breadcrumb-proj");
            MainWindow mw = new MainWindow(stage, testProj);

            // 1. Initial / Welcome tab state
            assertEquals("Database Consoles", mw.getNavBarBreadcrumbText());

            // 2. Open console 2 for postgres@223 on database thunder_db (Reference Image 3)
            ConnectionProfile profile223 = new ConnectionProfile();
            profile223.setId("pg-223");
            profile223.setName("postgres@223");
            profile223.setType(DatabaseType.POSTGRESQL);

            QueryTab console2 = new QueryTab(mw, profile223, "thunder_db", "console 2");
            mw.updateNavigationBarBreadcrumb(console2);

            assertEquals("Database Consoles › postgres@223 › console 2 [thunder_db]", mw.getNavBarBreadcrumbText());

            // Check that the last segment has the console terminal icon
            Node lastChild = mw.getNavBarBox().getChildren().get(mw.getNavBarBox().getChildren().size() - 1);
            assertTrue(lastChild instanceof Label);
            Label consoleLabel = (Label) lastChild;
            assertNotNull(consoleLabel.getGraphic(), "Console breadcrumb item should have an icon");
            assertTrue(consoleLabel.getGraphic() instanceof FontIcon);
            FontIcon icon = (FontIcon) consoleLabel.getGraphic();
            assertEquals(FontAwesomeSolid.TERMINAL.getDescription(), icon.getIconCode().getDescription());

            // 3. Switch to console 3 for postgresql@local on database erpdb (Reference Image 4)
            ConnectionProfile profileLocal = new ConnectionProfile();
            profileLocal.setId("pg-local");
            profileLocal.setName("postgresql@local");
            profileLocal.setType(DatabaseType.POSTGRESQL);

            QueryTab console3 = new QueryTab(mw, profileLocal, "erpdb", "console 3");
            mw.updateNavigationBarBreadcrumb(console3);

            assertEquals("Database Consoles › postgresql@local › console 3 [erpdb]", mw.getNavBarBreadcrumbText());

            // 4. Switch back to console 2 -> dynamically restores console 2 breadcrumb (Reference Image 2)
            mw.updateNavigationBarBreadcrumb(console2);
            assertEquals("Database Consoles › postgres@223 › console 2 [thunder_db]", mw.getNavBarBreadcrumbText());

            // 5. Schema switch in active console -> dynamically updates breadcrumb
            console2.switchDatabase("nexadb");
            assertEquals("Database Consoles › postgres@223 › console 2 [nexadb]", mw.getNavBarBreadcrumbText());

            // 6. Test DataTab breadcrumbs
            DbObject tableObj = new DbObject("orders", DbObject.Kind.TABLE, "postgres", "public");
            DataTab dataTab = new DataTab(profile223, tableObj);
            mw.updateNavigationBarBreadcrumb(dataTab);
            assertEquals("postgres@223 › public › orders", mw.getNavBarBreadcrumbText());

            // 7. Test MongoConsoleTab breadcrumbs
            ConnectionProfile mongoProfile = new ConnectionProfile();
            mongoProfile.setId("mongo-1");
            mongoProfile.setName("mongo@cluster");
            mongoProfile.setType(DatabaseType.MONGODB);

            MongoConsoleTab mongoConsole = new MongoConsoleTab(mongoProfile, "local", "console 1");
            mw.updateNavigationBarBreadcrumb(mongoConsole);
            assertEquals("Database Consoles › mongo@cluster › console 1", mw.getNavBarBreadcrumbText());

            // 8. Null tab / reset
            mw.updateNavigationBarBreadcrumb(null);
            assertEquals("Database Consoles", mw.getNavBarBreadcrumbText());
        });
    }
}
