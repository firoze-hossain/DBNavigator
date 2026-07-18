package com.roze.dbnavigator.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists executed SQL per connection to ~/.dbnavigator/history.json, most
 * recent first, capped per connection — DataGrip's console history panel.
 */
public final class QueryHistoryStore {

    public record Entry(String sql, long executedAtEpochMillis) {}

    private static final int MAX_PER_CONNECTION = 200;
    private static final Path FILE =
            Path.of(System.getProperty("user.home"), ".dbnavigator", "history.json");
    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /** connectionId -> entries, newest first */
    private static final Map<String, List<Entry>> history = new LinkedHashMap<>();
    private static boolean loaded = false;

    private QueryHistoryStore() {}

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        if (Files.exists(FILE)) {
            try {
                Map<String, List<Entry>> loadedMap = MAPPER.readValue(
                        FILE.toFile(), new TypeReference<Map<String, List<Entry>>>() {});
                history.putAll(loadedMap);
            } catch (IOException e) {
                System.err.println("Could not read query history: " + e.getMessage());
            }
        }
    }

    /** Records a run SQL statement for a connection (newest first, deduplicated adjacent). */
    public static synchronized void record(String connectionId, String sql) {
        ensureLoaded();
        String trimmed = sql.strip();
        if (trimmed.isEmpty()) return;
        List<Entry> entries = history.computeIfAbsent(connectionId, k -> new ArrayList<>());
        if (!entries.isEmpty() && entries.get(0).sql().equals(trimmed)) return;   // skip immediate repeat
        entries.add(0, new Entry(trimmed, Instant.now().toEpochMilli()));
        while (entries.size() > MAX_PER_CONNECTION) entries.remove(entries.size() - 1);
        persist();
    }

    /** Entries for one connection, newest first. */
    public static synchronized List<Entry> forConnection(String connectionId) {
        ensureLoaded();
        return new ArrayList<>(history.getOrDefault(connectionId, List.of()));
    }

    public static synchronized void clear(String connectionId) {
        ensureLoaded();
        history.remove(connectionId);
        persist();
    }

    private static void persist() {
        try {
            Files.createDirectories(FILE.getParent());
            MAPPER.writeValue(FILE.toFile(), history);
        } catch (IOException e) {
            System.err.println("Could not save query history: " + e.getMessage());
        }
    }
}
