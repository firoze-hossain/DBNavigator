package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.QueryResult;
import com.roze.dbnavigator.util.DataExporters;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class ExportDataDialogTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    private void runOnFxThread(Runnable action) throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
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
            if (error.get() instanceof Exception e) throw e;
            if (error.get() instanceof Error err) throw err;
            throw new RuntimeException(error.get());
        }
    }

    @Test
    public void testFilterSystemColumns() {
        QueryResult original = new QueryResult();
        original.getColumns().addAll(List.of("id", "tableoid", "name", "ctid", "created_at"));
        original.getColumnTypes().addAll(List.of("INT", "OID", "VARCHAR", "TID", "TIMESTAMP"));
        original.getRows().add(List.of("101", "16384", "Doc 1", "(0,1)", "2026-01-01"));
        original.getRows().add(List.of("102", "16384", "Doc 2", "(0,2)", "2026-01-02"));

        QueryResult clean = ExportDataDialog.filterSystemColumns(original);

        assertEquals(List.of("id", "name", "created_at"), clean.getColumns());
        assertEquals(List.of("INT", "VARCHAR", "TIMESTAMP"), clean.getColumnTypes());
        assertEquals(2, clean.getRows().size());
        assertEquals(List.of("101", "Doc 1", "2026-01-01"), clean.getRows().get(0));
        assertEquals(List.of("102", "Doc 2", "2026-01-02"), clean.getRows().get(1));
    }

    @Test
    public void testFilterSystemColumnsNoSystemCols() {
        QueryResult original = new QueryResult();
        original.getColumns().addAll(List.of("id", "val"));
        original.getRows().add(List.of("1", "alpha"));

        QueryResult result = ExportDataDialog.filterSystemColumns(original);
        assertEquals(List.of("id", "val"), result.getColumns());
        assertEquals(1, result.getRows().size());
    }

    @Test
    public void testDialogDefaultsAndUi() throws Exception {
        runOnFxThread(() -> {
            DbObject table = new DbObject("appointment_tx", DbObject.Kind.TABLE, "erpdb", "public");

            QueryResult qr = new QueryResult();
            qr.getColumns().addAll(List.of("id", "name"));
            qr.getRows().add(List.of("1", "Checkup"));

            ExportDataDialog dialog = new ExportDataDialog(null, null, table, qr, null,
                    ConnectionProfile.DatabaseType.POSTGRESQL, DataExporters.Format.CSV);

            // Add column header must be unchecked by default (DataGrip Image 1)
            assertFalse(dialog.addColumnHeaderCheck.isSelected(), "Add column header should be unchecked by default");
            assertFalse(dialog.addRowHeaderCheck.isSelected(), "Add row header should be unchecked by default");
            assertFalse(dialog.transposeCheck.isSelected(), "Transpose should be unchecked by default");

            // Generated check must be checked by default (DataGrip Image 3)
            assertTrue(dialog.addGeneratedCheck.isSelected(), "Generated columns should be checked by default");
            assertFalse(dialog.addComputedCheck.isSelected(), "Computed columns should be unchecked by default");
            assertFalse(dialog.addTableDefCheck.isSelected(), "Add table definition should be unchecked by default");

            // Source field must not steal focus and not be editable
            assertFalse(dialog.sourceField.isEditable(), "Source field must not be editable");
            assertFalse(dialog.sourceField.isFocusTraversable(), "Source field must not be focus-traversable to avoid blue text selection");
            assertEquals("erpdb.public.appointment_tx", dialog.sourceField.getText());

            // Output path should default to ~/Downloads/<table_name>.csv
            String outputPath = dialog.outputFileField.getText();
            assertNotNull(outputPath);
            assertTrue(outputPath.endsWith("appointment_tx.csv"), "Output path must end with appointment_tx.csv: " + outputPath);
            File downloadsDir = new File(System.getProperty("user.home", "."), "Downloads");
            if (downloadsDir.exists() && downloadsDir.isDirectory()) {
                assertTrue(outputPath.contains("Downloads"), "Output path should point to Downloads directory: " + outputPath);
            }

            // Preview editor must have LineNumberFactory attached
            assertNotNull(dialog.previewEditor.getParagraphGraphicFactory(), "Preview editor must have line numbers enabled");
        });
    }

    @Test
    public void testDynamicOptionsSwitching() throws Exception {
        runOnFxThread(() -> {
            DbObject table = new DbObject("appointment_tx", DbObject.Kind.TABLE, "erpdb", "public");

            QueryResult qr = new QueryResult();
            qr.getColumns().addAll(List.of("id", "name"));
            qr.getRows().add(List.of("1", "Checkup"));

            ExportDataDialog dialog = new ExportDataDialog(null, null, table, qr, null,
                    ConnectionProfile.DatabaseType.POSTGRESQL, DataExporters.Format.CSV);

            // 1. CSV format (DataGrip Image 1: Transpose, Add column header, Add row header)
            dialog.updateDynamicOptions(DataExporters.Format.CSV);
            assertEquals(3, dialog.dynamicOptionsBox.getChildren().size());
            assertSame(dialog.transposeCheck, dialog.dynamicOptionsBox.getChildren().get(0));
            assertSame(dialog.addColumnHeaderCheck, dialog.dynamicOptionsBox.getChildren().get(1));
            assertSame(dialog.addRowHeaderCheck, dialog.dynamicOptionsBox.getChildren().get(2));

            // 2. SQL Inserts format (DataGrip Image 3: Add columns row, Transpose, Add table definition DDL)
            dialog.updateDynamicOptions(DataExporters.Format.SQL_INSERTS);
            assertEquals(3, dialog.dynamicOptionsBox.getChildren().size());
            Node firstChild = dialog.dynamicOptionsBox.getChildren().get(0);
            assertInstanceOf(HBox.class, firstChild);
            HBox addColsRow = (HBox) firstChild;
            assertTrue(addColsRow.getChildren().contains(dialog.addComputedCheck));
            assertTrue(addColsRow.getChildren().contains(dialog.addGeneratedCheck));
            assertSame(dialog.transposeCheck, dialog.dynamicOptionsBox.getChildren().get(1));
            assertSame(dialog.addTableDefCheck, dialog.dynamicOptionsBox.getChildren().get(2));

            // 3. SQL Updates format (DataGrip Image 4: Transpose only)
            dialog.updateDynamicOptions(DataExporters.Format.SQL_UPDATES);
            assertEquals(1, dialog.dynamicOptionsBox.getChildren().size());
            assertSame(dialog.transposeCheck, dialog.dynamicOptionsBox.getChildren().get(0));

            // 4. Where Clause format (DataGrip Image 5: Transpose only)
            dialog.updateDynamicOptions(DataExporters.Format.WHERE_CLAUSE);
            assertEquals(1, dialog.dynamicOptionsBox.getChildren().size());
            assertSame(dialog.transposeCheck, dialog.dynamicOptionsBox.getChildren().get(0));

            // 5. Excel format (Image 1: Transpose, Output query to separate sheet - checked by default)
            dialog.updateDynamicOptions(DataExporters.Format.EXCEL);
            assertEquals(2, dialog.dynamicOptionsBox.getChildren().size());
            assertSame(dialog.transposeCheck, dialog.dynamicOptionsBox.getChildren().get(0));
            assertSame(dialog.outputQueryToSeparateSheetCheck, dialog.dynamicOptionsBox.getChildren().get(1));
            assertTrue(dialog.outputQueryToSeparateSheetCheck.isSelected(), "Output query to separate sheet should be checked by default");

            // 6. TSV format (Image 2: Transpose, Add column header, Add row header)
            dialog.updateDynamicOptions(DataExporters.Format.TSV);
            assertEquals(3, dialog.dynamicOptionsBox.getChildren().size());
            assertSame(dialog.transposeCheck, dialog.dynamicOptionsBox.getChildren().get(0));
            assertSame(dialog.addColumnHeaderCheck, dialog.dynamicOptionsBox.getChildren().get(1));
            assertSame(dialog.addRowHeaderCheck, dialog.dynamicOptionsBox.getChildren().get(2));

            // 7. Pipe-separated format (Image 3: Transpose, Add column header, Add row header, .txt)
            dialog.updateDynamicOptions(DataExporters.Format.PIPE);
            assertEquals(3, dialog.dynamicOptionsBox.getChildren().size());
            assertSame(dialog.transposeCheck, dialog.dynamicOptionsBox.getChildren().get(0));
            assertTrue(dialog.buildDefaultOutputPath(DataExporters.Format.PIPE).endsWith("appointment_tx.txt"));

            // 8. Semicolon-separated format (Image 4: Transpose, Add column header, Add row header, .txt)
            dialog.updateDynamicOptions(DataExporters.Format.SEMICOLON);
            assertEquals(3, dialog.dynamicOptionsBox.getChildren().size());
            assertSame(dialog.transposeCheck, dialog.dynamicOptionsBox.getChildren().get(0));
            assertTrue(dialog.buildDefaultOutputPath(DataExporters.Format.SEMICOLON).endsWith("appointment_tx.txt"));

            // 9. CSV_GROOVY format (Image 5: Transpose only, .csv)
            dialog.updateDynamicOptions(DataExporters.Format.CSV_GROOVY);
            assertEquals(1, dialog.dynamicOptionsBox.getChildren().size());
            assertSame(dialog.transposeCheck, dialog.dynamicOptionsBox.getChildren().get(0));
            assertTrue(dialog.buildDefaultOutputPath(DataExporters.Format.CSV_GROOVY).endsWith("appointment_tx.csv"));

            // 10. Binary preview and Copy button behavior when switching to Excel
            dialog.extractorCombo.setValue(DataExporters.Format.EXCEL);
            assertTrue(dialog.copyToClipboardBtn.isDisable(), "Copy to Clipboard must be disabled for Excel binary export");
            assertTrue(dialog.binaryPlaceholder.isVisible(), "Binary placeholder must be visible for Excel");
            assertFalse(dialog.previewScroll.isVisible(), "CodeArea scroll pane must be hidden for Excel");

            // Switching back to CSV enables Copy button and text preview
            dialog.extractorCombo.setValue(DataExporters.Format.CSV);
            assertFalse(dialog.copyToClipboardBtn.isDisable(), "Copy to Clipboard must be enabled for CSV");
            assertFalse(dialog.binaryPlaceholder.isVisible(), "Binary placeholder must be hidden for CSV");
            assertTrue(dialog.previewScroll.isVisible(), "CodeArea scroll pane must be visible for CSV");

            // 11. HTML (groovy) format (Image 1: Transpose only, .html)
            dialog.updateDynamicOptions(DataExporters.Format.HTML_GROOVY);
            assertEquals(1, dialog.dynamicOptionsBox.getChildren().size());
            assertSame(dialog.transposeCheck, dialog.dynamicOptionsBox.getChildren().get(0));
            assertTrue(dialog.buildDefaultOutputPath(DataExporters.Format.HTML_GROOVY).endsWith("appointment_tx.html"));

            // 12. HTML (js) format (Image 2: Transpose only, .html)
            dialog.updateDynamicOptions(DataExporters.Format.HTML_JS);
            assertEquals(1, dialog.dynamicOptionsBox.getChildren().size());
            assertSame(dialog.transposeCheck, dialog.dynamicOptionsBox.getChildren().get(0));
            assertTrue(dialog.buildDefaultOutputPath(DataExporters.Format.HTML_JS).endsWith("appointment_tx.html"));

            // 13. JSON format (Image 3: Transpose only, .json)
            dialog.updateDynamicOptions(DataExporters.Format.JSON);
            assertEquals(1, dialog.dynamicOptionsBox.getChildren().size());
            assertSame(dialog.transposeCheck, dialog.dynamicOptionsBox.getChildren().get(0));
            assertTrue(dialog.buildDefaultOutputPath(DataExporters.Format.JSON).endsWith("appointment_tx.json"));

            // 14. Markdown format (Image 4: Transpose only, .md)
            dialog.updateDynamicOptions(DataExporters.Format.MARKDOWN);
            assertEquals(1, dialog.dynamicOptionsBox.getChildren().size());
            assertSame(dialog.transposeCheck, dialog.dynamicOptionsBox.getChildren().get(0));
            assertTrue(dialog.buildDefaultOutputPath(DataExporters.Format.MARKDOWN).endsWith("appointment_tx.md"));

            // 15. One-row format (Image 5: Transpose only, .sql)
            dialog.updateDynamicOptions(DataExporters.Format.ONE_ROW);
            assertEquals(1, dialog.dynamicOptionsBox.getChildren().size());
            assertSame(dialog.transposeCheck, dialog.dynamicOptionsBox.getChildren().get(0));
            assertTrue(dialog.buildDefaultOutputPath(DataExporters.Format.ONE_ROW).endsWith("appointment_tx.sql"),
                    "One-row default extension must be .sql matching DataGrip Image 5");

            // 16. Pretty format (Image 1: Transpose only, .txt)
            dialog.updateDynamicOptions(DataExporters.Format.PRETTY);
            assertEquals(1, dialog.dynamicOptionsBox.getChildren().size());
            assertSame(dialog.transposeCheck, dialog.dynamicOptionsBox.getChildren().get(0));
            assertTrue(dialog.buildDefaultOutputPath(DataExporters.Format.PRETTY).endsWith("appointment_tx.txt"),
                    "Pretty default extension must be .txt matching DataGrip Image 1");

            // 17. SQL-Insert-Multirow format (Image 2: Transpose only, .sql)
            dialog.updateDynamicOptions(DataExporters.Format.SQL_INSERT_MULTIROW);
            assertEquals(1, dialog.dynamicOptionsBox.getChildren().size());
            assertSame(dialog.transposeCheck, dialog.dynamicOptionsBox.getChildren().get(0));
            assertTrue(dialog.buildDefaultOutputPath(DataExporters.Format.SQL_INSERT_MULTIROW).endsWith("appointment_tx.sql"),
                    "SQL-Insert-Multirow default extension must be .sql matching DataGrip Image 2");

            // 18. SQL-Insert-Statements format (Image 3: Transpose only, .sql)
            dialog.updateDynamicOptions(DataExporters.Format.SQL_INSERT_STATEMENTS);
            assertEquals(1, dialog.dynamicOptionsBox.getChildren().size());
            assertSame(dialog.transposeCheck, dialog.dynamicOptionsBox.getChildren().get(0));
            assertTrue(dialog.buildDefaultOutputPath(DataExporters.Format.SQL_INSERT_STATEMENTS).endsWith("appointment_tx.sql"),
                    "SQL-Insert-Statements default extension must be .sql matching DataGrip Image 3");

            // 19. XML format (Image 4: Transpose only, .xml)
            dialog.updateDynamicOptions(DataExporters.Format.XML);
            assertEquals(1, dialog.dynamicOptionsBox.getChildren().size());
            assertSame(dialog.transposeCheck, dialog.dynamicOptionsBox.getChildren().get(0));
            assertTrue(dialog.buildDefaultOutputPath(DataExporters.Format.XML).endsWith("appointment_tx.xml"),
                    "XML default extension must be .xml matching DataGrip Image 4");
        });
    }
}
