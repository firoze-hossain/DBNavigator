package com.roze.dbnavigator.db;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Persists Global Search ("Search Everywhere") history across sessions.
 * Preserves the last search query, recent search terms, and recently accessed
 * database objects/consoles/actions so that reopening or restarting the IDE
 * immediately restores the last search session and recent items (DataGrip behavior).
 */
public final class SearchHistoryStore {

    private static final Path FILE =
            Path.of(System.getProperty("user.home"), ".dbnavigator", "search-history.json");
    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static final int MAX_QUERIES = 30;
    private static final int MAX_ITEMS = 40;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HistoryData {
        public String lastQuery = "";
        public List<String> recentQueries = new ArrayList<>();
        public List<RecentItem> recentItems = new ArrayList<>();

        public HistoryData() {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RecentItem(
            String type,            // "TABLE", "VIEW", "COLUMN", "SEQUENCE", "INDEX", "CONSOLE", "FILE", "ACTION"
            String name,            // e.g. "environments", "environment_id"
            String profileId,       // Connection profile ID
            String profileName,     // e.g. "erp-161", "postgres@localhost"
            String catalog,         // Database catalog (e.g. "thunder_db")
            String schema,          // Schema (e.g. "public")
            String parentTable,     // For column/index/key (e.g. "environment_variables")
            String columnName,      // Column name if applicable
            String uri,             // Full qualified URI (e.g. "DATABASE/.../TABLE/...")
            long timestamp
    ) {
        public String uniqueKey() {
            return (type != null ? type : "") + "::"
                    + (profileId != null ? profileId : "") + "::"
                    + (catalog != null ? catalog : "") + "::"
                    + (schema != null ? schema : "") + "::"
                    + (parentTable != null ? parentTable : "") + "::"
                    + (name != null ? name : "");
        }
    }

    private static HistoryData cachedData;
    private static boolean initialized = false;

    private SearchHistoryStore() {}

    private static synchronized void ensureLoaded() {
        if (initialized && cachedData != null) return;
        initialized = true;
        if (Files.exists(FILE)) {
            try {
                cachedData = MAPPER.readValue(FILE.toFile(), HistoryData.class);
            } catch (Exception e) {
                System.err.println("Could not read search history file: " + e.getMessage());
            }
        }
        if (cachedData == null) {
            cachedData = new HistoryData();
        }
    }

    private static synchronized void persist() {
        if (cachedData == null) return;
        try {
            File parent = FILE.getParent().toFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            MAPPER.writeValue(FILE.toFile(), cachedData);
        } catch (Exception e) {
            // Best effort - allow gracefully continuing even in read-only sandbox
            System.err.println("Could not save search history: " + e.getMessage());
        }
    }

    public static synchronized String getLastQuery() {
        ensureLoaded();
        return cachedData.lastQuery != null ? cachedData.lastQuery : "";
    }

    public static synchronized void recordSearch(String query) {
        if (query == null) return;
        String trimmed = query.trim();
        ensureLoaded();
        cachedData.lastQuery = trimmed;

        if (!trimmed.isEmpty()) {
            cachedData.recentQueries.remove(trimmed);
            cachedData.recentQueries.add(0, trimmed);
            if (cachedData.recentQueries.size() > MAX_QUERIES) {
                cachedData.recentQueries = new ArrayList<>(cachedData.recentQueries.subList(0, MAX_QUERIES));
            }
        }
        persist();
    }

    public static synchronized List<String> getRecentQueries() {
        ensureLoaded();
        return new ArrayList<>(cachedData.recentQueries);
    }

    public static synchronized void recordRecentItem(RecentItem item) {
        if (item == null) return;
        ensureLoaded();
        String itemKey = item.uniqueKey();
        Iterator<RecentItem> it = cachedData.recentItems.iterator();
        while (it.hasNext()) {
            RecentItem existing = it.next();
            if (existing != null && itemKey.equals(existing.uniqueKey())) {
                it.remove();
            }
        }
        cachedData.recentItems.add(0, item);
        if (cachedData.recentItems.size() > MAX_ITEMS) {
            cachedData.recentItems = new ArrayList<>(cachedData.recentItems.subList(0, MAX_ITEMS));
        }
        persist();
    }

    public static synchronized List<RecentItem> getRecentItems() {
        ensureLoaded();
        return new ArrayList<>(cachedData.recentItems);
    }

    public static synchronized void clearForTesting() {
        cachedData = new HistoryData();
        initialized = true;
    }
}
