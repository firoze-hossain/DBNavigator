package com.roze.dbnavigator.util;

import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.model.ConnectionProfile;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseToolDetectorTest {

    @Test
    public void testDetectPostgreSqlToolsOnCurrentSystem() {
        String pgDump = DatabaseToolDetector.detectToolPath("pg_dump");
        assertNotNull(pgDump);
        assertFalse(pgDump.isBlank());

        String psql = DatabaseToolDetector.detectToolPath("psql");
        assertNotNull(psql);
        assertFalse(psql.isBlank());

        String pgRestore = DatabaseToolDetector.detectToolPath("pg_restore");
        assertNotNull(pgRestore);
        assertFalse(pgRestore.isBlank());

        // If running on macOS with /Library/PostgreSQL/16/bin/pg_dump installed, verify exact detection
        File edbPgDump = new File("/Library/PostgreSQL/16/bin/pg_dump");
        if (edbPgDump.exists() && edbPgDump.canExecute()) {
            assertEquals(edbPgDump.getAbsolutePath(), pgDump);
            File edbPsql = new File("/Library/PostgreSQL/16/bin/psql");
            if (edbPsql.exists() && edbPsql.canExecute()) {
                assertEquals(edbPsql.getAbsolutePath(), psql);
            }
            File edbRestore = new File("/Library/PostgreSQL/16/bin/pg_restore");
            if (edbRestore.exists() && edbRestore.canExecute()) {
                assertEquals(edbRestore.getAbsolutePath(), pgRestore);
            }
        }
    }

    @Test
    public void testDetectSqliteOnCurrentSystem() {
        String sqlite3 = DatabaseToolDetector.detectToolPath("sqlite3");
        assertNotNull(sqlite3);
        assertFalse(sqlite3.isBlank());

        if (DatabaseToolDetector.OS.current() == DatabaseToolDetector.OS.MAC
                || DatabaseToolDetector.OS.current() == DatabaseToolDetector.OS.LINUX) {
            File usrBinSqlite = new File("/usr/bin/sqlite3");
            if (usrBinSqlite.exists() && usrBinSqlite.canExecute()) {
                assertTrue(new File(sqlite3).exists(), "Detected sqlite3 must exist");
            }
        }
    }

    @Test
    public void testFallbackForUnknownTool() {
        String unknown = DatabaseToolDetector.detectToolPath("unknown_db_tool_xyz123");
        assertNotNull(unknown);
        if (DatabaseToolDetector.OS.current() == DatabaseToolDetector.OS.WINDOWS) {
            assertEquals("unknown_db_tool_xyz123.exe", unknown);
        } else {
            assertEquals("unknown_db_tool_xyz123", unknown);
        }
    }

    @Test
    public void testProfileVersionHintPrioritizesMatchingVersion() {
        ConnectionProfile profile = new ConnectionProfile();
        profile.setName("erp-161");
        profile.setType(ConnectionProfile.DatabaseType.POSTGRESQL);

        String path = DatabaseToolDetector.detectToolPath("pg_dump", profile);
        assertNotNull(path);
        assertFalse(path.isBlank());

        File edb16 = new File("/Library/PostgreSQL/16/bin/pg_dump");
        if (edb16.exists() && edb16.canExecute()) {
            assertEquals(edb16.getAbsolutePath(), path);
        }
    }

    @Test
    public void testSaveAndRetrieveCustomToolPath() {
        File dummy = new File(System.getProperty("user.home"), ".dbnavigator_test_tool");
        try {
            boolean created = dummy.createNewFile();
            if (!created && !dummy.exists()) return;
            dummy.setExecutable(true);

            DatabaseToolDetector.saveToolPath("custom_test_tool", dummy.getAbsolutePath());

            String detected = DatabaseToolDetector.detectToolPath("custom_test_tool");
            assertEquals(dummy.getAbsolutePath(), detected);
        } catch (Exception ignored) {
        } finally {
            try {
                var s = AppSettingsStore.load();
                if (s != null && s.getDatabaseClientToolPaths() != null) {
                    s.getDatabaseClientToolPaths().remove("custom_test_tool");
                    AppSettingsStore.save(s);
                }
                dummy.delete();
            } catch (Exception ignored) {}
        }
    }

    @Test
    public void testHasTool() {
        // Unknown tool should return false
        assertFalse(DatabaseToolDetector.hasTool("nonexistent_fake_tool_9999"));
    }
}
