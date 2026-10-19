package com.roze.dbnavigator.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SearchHistoryStoreTest {

    @BeforeEach
    void setUp() {
        SearchHistoryStore.clearForTesting();
    }

    @Test
    void testLastQueryPersistenceAcrossSessions() {
        assertEquals("", SearchHistoryStore.getLastQuery());

        // Session 1: User searches for "environments"
        SearchHistoryStore.recordSearch("environments");
        assertEquals("environments", SearchHistoryStore.getLastQuery());

        // Session 1: User then searches for "appointment_"
        SearchHistoryStore.recordSearch("appointment_");
        assertEquals("appointment_", SearchHistoryStore.getLastQuery());

        List<String> recents = SearchHistoryStore.getRecentQueries();
        assertTrue(recents.size() >= 2);
        assertEquals("appointment_", recents.get(0));
        assertEquals("environments", recents.get(1));
    }

    @Test
    void testRecentItemsRecordingAndDeduplication() {
        SearchHistoryStore.RecentItem item1 = new SearchHistoryStore.RecentItem(
                "TABLE", "environments", "prof-1", "erp-161", "thunder_db", "public",
                null, null, "DATABASE/prof-1/table/environments", System.currentTimeMillis()
        );

        SearchHistoryStore.RecentItem item2 = new SearchHistoryStore.RecentItem(
                "COLUMN", "environment_id", "prof-1", "erp-161", "thunder_db", "public",
                "environment_variables", "environment_id",
                "DATABASE/prof-1/table/environment_variables/col/environment_id", System.currentTimeMillis()
        );

        SearchHistoryStore.recordRecentItem(item1);
        SearchHistoryStore.recordRecentItem(item2);

        List<SearchHistoryStore.RecentItem> list = SearchHistoryStore.getRecentItems();
        assertEquals(2, list.size());
        assertEquals("environment_id", list.get(0).name());
        assertEquals("COLUMN", list.get(0).type());
        assertEquals("environments", list.get(1).name());

        // Record item1 again: should move to head without duplicating
        SearchHistoryStore.recordRecentItem(item1);
        List<SearchHistoryStore.RecentItem> updated = SearchHistoryStore.getRecentItems();
        assertEquals(2, updated.size());
        assertEquals("environments", updated.get(0).name());
        assertEquals("environment_id", updated.get(1).name());
    }
}
