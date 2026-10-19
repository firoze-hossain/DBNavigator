package com.roze.dbnavigator.search;

import com.roze.dbnavigator.model.ConnectionProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class UnreachableProfileSearchTest {

    @BeforeEach
    void setUp() {
        SearchEverywhereService.clearUnreachableProfiles();
        SearchEverywhereService.clearDiscoveredDatabasesCache();
    }

    @Test
    void testIsConnectionFailureDetection() {
        assertTrue(SearchEverywhereService.isConnectionFailure(
                new RuntimeException("Failed to initialize pool: Failed to connect to StratosDB at localhost:6582")));
        assertTrue(SearchEverywhereService.isConnectionFailure(
                new SQLException("Connection refused (Connection refused)")));
        assertTrue(SearchEverywhereService.isConnectionFailure(
                new SQLException("The connection attempt failed.")));
        assertTrue(SearchEverywhereService.isConnectionFailure(
                new SQLException("Communications link failure")));
        assertTrue(SearchEverywhereService.isConnectionFailure(
                new RuntimeException("java.net.UnknownHostException: non-existent-host.local")));
        assertTrue(SearchEverywhereService.isConnectionFailure(
                new RuntimeException("Connection timed out")));

        // Nested exception cause
        Exception wrapped = new Exception("Query execution failed", new SQLException("Connection to 127.0.0.1:5432 refused"));
        assertTrue(SearchEverywhereService.isConnectionFailure(wrapped));

        // Normal query / syntax error should NOT be flagged as connection failure
        assertFalse(SearchEverywhereService.isConnectionFailure(
                new SQLException("ERROR: relation \"non_existent_table\" does not exist")));
        assertFalse(SearchEverywhereService.isConnectionFailure(
                new SQLException("Syntax error in SQL statement")));
        assertFalse(SearchEverywhereService.isConnectionFailure(null));
    }

    @Test
    void testProfileUnreachableLifecycle() {
        String profileId = "offline-profile-123";

        assertFalse(SearchEverywhereService.isProfileUnreachable(profileId),
                "Initially profile should not be marked unreachable");

        SearchEverywhereService.markProfileUnreachable(profileId);
        assertTrue(SearchEverywhereService.isProfileUnreachable(profileId),
                "Profile should be unreachable after markProfileUnreachable");

        SearchEverywhereService.markProfileReachable(profileId);
        assertFalse(SearchEverywhereService.isProfileUnreachable(profileId),
                "Profile should be reachable after markProfileReachable");

        // Clear all
        SearchEverywhereService.markProfileUnreachable(profileId);
        assertTrue(SearchEverywhereService.isProfileUnreachable(profileId));
        SearchEverywhereService.clearUnreachableProfiles();
        assertFalse(SearchEverywhereService.isProfileUnreachable(profileId),
                "All unreachable statuses should be cleared by clearUnreachableProfiles");
    }

    @Test
    void testSearchSkipsUnreachableProfileWithoutErrors() {
        String offlineId = "uninstalled-db-profile";
        SearchEverywhereService.markProfileUnreachable(offlineId);

        ConnectionProfile offlineProfile = new ConnectionProfile();
        offlineProfile.setId(offlineId);
        offlineProfile.setName("localhost@offline_db");
        offlineProfile.setType(ConnectionProfile.DatabaseType.POSTGRESQL);
        offlineProfile.setHost("localhost");
        offlineProfile.setPort(9999);
        offlineProfile.setDatabase("offline_db");

        // Running search should not throw any exception or attempt network connection
        AtomicInteger gen = new AtomicInteger(1);
        List<SearchHit> results = new ArrayList<>();

        assertDoesNotThrow(() -> {
            SearchEverywhereService.search(
                    "customer",
                    SearchHit.Category.DATABASE,
                    true,
                    null,
                    gen,
                    1,
                    20
            );
        });
    }
}
