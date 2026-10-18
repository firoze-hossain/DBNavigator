package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import javafx.application.Platform;
import javafx.scene.text.Font;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class QueryTabStatementHighlightTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    private ConnectionProfile createTestProfile() {
        ConnectionProfile p = new ConnectionProfile();
        p.setId("test-pg-hl");
        p.setName("postgres@localhost");
        p.setType(ConnectionProfile.DatabaseType.POSTGRESQL);
        p.setHost("localhost");
        p.setPort(5432);
        p.setDatabase("testdb");
        p.setUsername("postgres");
        p.setPassword("secret");
        return p;
    }

    private void runOnFxThread(Runnable action) throws Exception {
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for FX thread execution");
        if (error.get() != null) {
            if (error.get() instanceof AssertionError) throw (AssertionError) error.get();
            if (error.get() instanceof Exception) throw (Exception) error.get();
            throw new RuntimeException(error.get());
        }
    }

    @Test
    public void testBundledFontLoading() throws Exception {
        runOnFxThread(() -> {
            FontLoader.loadBundledFonts();
            Font font = Font.font("DBNavigator Mono", 13.5);
            assertNotNull(font, "Font DBNavigator Mono must be resolvable");
            assertEquals("DBNavigator Mono", font.getFamily(),
                    "Resolved font family must be DBNavigator Mono, but got: " + font.getFamily());
        });
    }

    @Test
    public void testStatementHighlightAndNoDoubledBorders() throws Exception {
        runOnFxThread(() -> {
            ConnectionProfile profile = createTestProfile();
            QueryTab tab = new QueryTab(null, profile, "testdb", "console [postgres@localhost]");

            String sql = "SELECT * FROM users WHERE active = true;\n\nSELECT id, name FROM departments;\n";
            tab.getEditor().replaceText(sql);
            tab.getEditor().moveTo(5); // Caret inside the first statement

            // Verify paragraph styles do not contain any full-width statement-box borders
            int count = tab.getEditor().getParagraphs().size();
            for (int i = 0; i < count; i++) {
                var styles = tab.getEditor().getParagraph(i).getParagraphStyle();
                assertFalse(styles.contains("statement-box-top"), "Paragraph should not have statement-box-top border");
                assertFalse(styles.contains("statement-box-bottom"), "Paragraph should not have statement-box-bottom border");
                assertFalse(styles.contains("statement-box-single"), "Paragraph should not have statement-box-single border");
                assertFalse(styles.contains("statement-box-middle"), "Paragraph should not have statement-box-middle border");
            }

            // Verify moving caret inside second statement
            assertDoesNotThrow(() -> {
                tab.getEditor().moveTo(sql.indexOf("departments") + 2);
            });

            // Verify setSql with single-line replacing multi-line works cleanly
            assertDoesNotThrow(() -> {
                tab.setSql("SELECT 1;");
            });
            assertEquals("SELECT 1;", tab.getEditor().getText());
        });
    }

    @Test
    public void testExecutedGutterCheckmark() throws Exception {
        runOnFxThread(() -> {
            ConnectionProfile profile = createTestProfile();
            QueryTab tab = new QueryTab(null, profile, "testdb", "console [postgres@localhost]");

            assertNotNull(tab.getEditor());
            assertNotNull(tab.getEditor().getParagraphGraphicFactory(), "Gutter factory should be configured");
        });
    }
}
