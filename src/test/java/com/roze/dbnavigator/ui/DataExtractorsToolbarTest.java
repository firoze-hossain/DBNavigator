package com.roze.dbnavigator.ui;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.QueryResult;
import com.roze.dbnavigator.util.DataExporters;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class DataExtractorsToolbarTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignore) {
            // Platform already initialized
        }
    }

    @Test
    public void testDefaultFormatSelection() {
        // Relational databases must default to CSV
        DataExtractorsToolbar pgToolbar = new DataExtractorsToolbar(
                ConnectionProfile.DatabaseType.POSTGRESQL,
                QueryResult::new, () -> "tbl", () -> null, () -> null, () -> null
        );
        assertEquals(DataExporters.Format.CSV, pgToolbar.getActiveFormat());
        assertEquals("CSV", pgToolbar.getExtractorButton().getText());

        DataExtractorsToolbar mysqlToolbar = new DataExtractorsToolbar(
                ConnectionProfile.DatabaseType.MYSQL,
                QueryResult::new, () -> "tbl", () -> null, () -> null, () -> null
        );
        assertEquals(DataExporters.Format.CSV, mysqlToolbar.getActiveFormat());

        DataExtractorsToolbar mssqlToolbar = new DataExtractorsToolbar(
                ConnectionProfile.DatabaseType.SQLSERVER,
                QueryResult::new, () -> "tbl", () -> null, () -> null, () -> null
        );
        assertEquals(DataExporters.Format.CSV, mssqlToolbar.getActiveFormat());

        DataExtractorsToolbar oracleToolbar = new DataExtractorsToolbar(
                ConnectionProfile.DatabaseType.ORACLE,
                QueryResult::new, () -> "tbl", () -> null, () -> null, () -> null
        );
        assertEquals(DataExporters.Format.CSV, oracleToolbar.getActiveFormat());

        DataExtractorsToolbar sqliteToolbar = new DataExtractorsToolbar(
                ConnectionProfile.DatabaseType.SQLITE,
                QueryResult::new, () -> "tbl", () -> null, () -> null, () -> null
        );
        assertEquals(DataExporters.Format.CSV, sqliteToolbar.getActiveFormat());

        DataExtractorsToolbar mariaToolbar = new DataExtractorsToolbar(
                ConnectionProfile.DatabaseType.MARIADB,
                QueryResult::new, () -> "tbl", () -> null, () -> null, () -> null
        );
        assertEquals(DataExporters.Format.CSV, mariaToolbar.getActiveFormat());

        // MongoDB must default to JSON
        DataExtractorsToolbar mongoToolbar = new DataExtractorsToolbar(
                ConnectionProfile.DatabaseType.MONGODB,
                QueryResult::new, () -> "coll", () -> null, () -> null, () -> null
        );
        assertEquals(DataExporters.Format.JSON, mongoToolbar.getActiveFormat());
        assertEquals("JSON", mongoToolbar.getExtractorButton().getText());
    }

    @Test
    public void testFormatChangeAndListener() {
        DataExtractorsToolbar toolbar = new DataExtractorsToolbar(
                ConnectionProfile.DatabaseType.POSTGRESQL,
                QueryResult::new, () -> "tbl", () -> null, () -> null, () -> null
        );

        AtomicReference<DataExporters.Format> changed = new AtomicReference<>();
        toolbar.addFormatListener(changed::set);

        toolbar.setActiveFormat(DataExporters.Format.MARKDOWN);
        assertEquals(DataExporters.Format.MARKDOWN, toolbar.getActiveFormat());
        assertEquals("Markdown", toolbar.getExtractorButton().getText());
        assertEquals(DataExporters.Format.MARKDOWN, changed.get());
    }

    @Test
    public void testResultGridIntegration() {
        ResultGrid grid = new ResultGrid();
        assertEquals(DataExporters.Format.CSV, grid.getActiveExtractorFormat());

        grid.setActiveExtractorFormat(DataExporters.Format.JSON);
        assertEquals(DataExporters.Format.JSON, grid.getActiveExtractorFormat());

        QueryResult sample = new QueryResult();
        sample.getColumns().add("col1");
        sample.getRows().add(java.util.List.of("val1"));
        grid.showResult(sample);

        QueryResult roundTrip = grid.toQueryResult();
        assertEquals(1, roundTrip.getColumns().size());
        assertEquals("col1", roundTrip.getColumns().get(0));
        assertEquals(1, roundTrip.getRows().size());
        assertEquals("val1", roundTrip.getRows().get(0).get(0));
    }
}
