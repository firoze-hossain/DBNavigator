package com.roze.dbnavigator.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Automatic, local version history for console content — DataGrip's "Local
 * History" feature. Every meaningfully-different edit is snapshotted (no
 * manual save/commit needed) to ~/.dbnavigator/local-history.json, capped
 * per file so it doesn't grow unbounded.
 */
public final class LocalHistoryStore {

    public record Entry(long timestamp, String label, String content) {}

    private static final int MAX_PER_FILE = 200;
    private static final Path FILE =
            Path.of(System.getProperty("user.home"), ".dbnavigator", "local-history.json");
    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /** fileId -> entries, newest first */
    private static final Map<String, List<Entry>> history = new LinkedHashMap<>();
    private static boolean loaded = false;

    private LocalHistoryStore() {}

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        if (Files.exists(FILE)) {
            try {
                Map<String, List<Entry>> loadedMap = MAPPER.readValue(
                        FILE.toFile(), new TypeReference<Map<String, List<Entry>>>() {});
                history.putAll(loadedMap);
            } catch (IOException e) {
                System.err.println("Could not read local history: " + e.getMessage());
            }
        }
    }

    /** Auto-capture: records a snapshot only if the content actually changed. */
    public static synchronized void record(String fileId, String content) {
        ensureLoaded();
        List<Entry> entries = history.computeIfAbsent(fileId, k -> new ArrayList<>());
        if (!entries.isEmpty() && entries.get(0).content().equals(content)) return;
        entries.add(0, new Entry(System.currentTimeMillis(), null, content));
        trimAndPersist(entries);
    }

    /** "Put Label…": captures a snapshot right now, tagged with a user-chosen name. */
    public static synchronized void putLabel(String fileId, String content, String label) {
        ensureLoaded();
        List<Entry> entries = history.computeIfAbsent(fileId, k -> new ArrayList<>());
        entries.add(0, new Entry(System.currentTimeMillis(), label, content));
        trimAndPersist(entries);
    }

    public static synchronized List<Entry> forFile(String fileId) {
        ensureLoaded();
        return new ArrayList<>(history.getOrDefault(fileId, List.of()));
    }

    /** Every entry across every file, newest first — for the project-wide view. */
    public static synchronized List<Map.Entry<String, Entry>> allEntriesNewestFirst() {
        ensureLoaded();
        List<Map.Entry<String, Entry>> all = new ArrayList<>();
        for (var fileEntries : history.entrySet()) {
            for (Entry e : fileEntries.getValue()) {
                all.add(new AbstractMap.SimpleEntry<>(fileEntries.getKey(), e));
            }
        }
        all.sort((a, b) -> Long.compare(b.getValue().timestamp(), a.getValue().timestamp()));
        return all;
    }

    /** Used by "Invalidate Caches" → "Clear file system cache and Local History". */
    public static synchronized void clearAll() {
        ensureLoaded();
        history.clear();
        try {
            Files.deleteIfExists(FILE);
        } catch (IOException ignored) {}
    }

    private static void trimAndPersist(List<Entry> entries) {
        while (entries.size() > MAX_PER_FILE) entries.remove(entries.size() - 1);
        persist();
    }

    private static void persist() {
        try {
            Files.createDirectories(FILE.getParent());
            MAPPER.writeValue(FILE.toFile(), history);
        } catch (IOException e) {
            System.err.println("Could not save local history: " + e.getMessage());
        }
    }
}
