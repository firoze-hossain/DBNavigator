package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
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

    private Bounds statementOverlayBounds(QueryTab tab, javafx.scene.Node node) {
        if (node == null || tab == null) return null;
        javafx.geometry.Bounds b = node.localToScreen(node.getBoundsInLocal());
        if (b == null) return null;
        var r = tab.getStatementBoundingBox();
        javafx.scene.Node overlay = r.getParent();
        return overlay.screenToLocal(b);
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

    @Test
    public void testImage4QueryStatements() throws Exception {
        runOnFxThread(() -> {
            ConnectionProfile profile = createTestProfile();
            QueryTab tab = new QueryTab(null, profile, "erpdb", "console 1 [erpdb]");
            javafx.scene.Scene scene = new javafx.scene.Scene(new javafx.scene.layout.StackPane(tab.getContent()), 1280, 800);
            ThemeManager.init(scene, com.roze.dbnavigator.db.AppSettingsStore.Theme.DARK);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setScene(scene);
            stage.show();

            String sql = "select *\n" +
                    "from fna_cash_bank_payer_advance_details order by created_at desc ;\n\n" +
                    "select *\n" +
                    "from fna_cash_bank_payer_advance where id=297467 order by created_at desc ;\n\n" +
                    "select *\n" +
                    "from fna_cash_bank_payer_advance where uhid=1000707328 and status<>2 and pending_amount>0;\n\n" +
                    "select *\n" +
                    "from fna_cash_bank_collection_tx where uh_id=1000707328;\n\n" +
                    "select *\n" +
                    "from fna_cash_bank_payer_advance_details where id=102952;\n\n" +
                    "select *\n" +
                    "from fna_cash_bank_payer_receipt_details where id=2896402;\n\n" +
                    "select * from fna_cash_bank_payer_advance_details where uhid=1000707328;\n\n" +
                    "SELECT\n" +
                    "        b.id AS invoiceId,\n" +
                    "        b.uhid AS uhid,\n" +
                    "        p.full_name AS patientName,\n" +
                    "        b.payerid AS payerid,\n" +
                    "        c.title AS payer,\n" +
                    "        b.status AS status,\n" +
                    "        b.bill_date_time::date AS billDateTime,\n" +
                    "        b.total_amount AS totalAmount,\n" +
                    "        b.final_bill_amount AS finalBillAmount,\n" +
                    "        b.management_discount AS managementDiscount\n" +
                    "FROM bbill b\n" +
                    "LEFT JOIN ehr_patient p ON p.id = b.uhid\n" +
                    "LEFT JOIN ehr_corporate c ON c.id = b.payerid\n" +
                    "WHERE b.bill_date_time::date BETWEEN :startDate AND :endDate\n" +
                    "AND b.status NOT IN (1,2,21)\n" +
                    "AND b.management_discount > 0\n";

            var stmts = com.roze.dbnavigator.util.SqlStatementSplitter.split(sql);
            assertEquals(8, stmts.size(), "Should identify all statements in editor");

            tab.getEditor().replaceText(sql);
            stage.setWidth(1280);
            stage.setHeight(900);
            stage.show();
            stage.toFront();
            tab.getEditor().requestFollowCaret();
            tab.getEditor().layout();

            int pos = sql.indexOf("b.id");
            tab.getEditor().moveTo(pos);
            tab.getEditor().requestFollowCaret();

            tab.refreshEditorStylingPublic();
            var bbox = tab.getStatementBoundingBox();

            assertTrue(bbox.isVisible(), "Statement bounding box must be visible");
            assertTrue(bbox.getX() >= 48.0 && bbox.getX() <= 65.0, "Box left edge must start right next to gutter, actual: " + bbox.getX());
            assertTrue(bbox.getWidth() >= 400, "Box width should enclose longest line");
            assertTrue(bbox.getHeight() >= 300, "Box height should enclose all lines of statement");
            stage.close();
        });
    }
}
