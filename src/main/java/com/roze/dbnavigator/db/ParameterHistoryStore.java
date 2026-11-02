package com.roze.dbnavigator.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Persists user parameter values across queries, consoles, and application restarts.
 * Matches DataGrip behavior where previously entered parameter values are remembered
 * and automatically pre-filled when running parameterized dynamic queries.
 */
public final class ParameterHistoryStore {

    private static final Path FILE =
            Path.of(System.getProperty("user.home"), ".dbnavigator", "parameter-history.json");
    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static final Map<String, String> values = new LinkedHashMap<>();
    private static boolean loaded = false;

    private ParameterHistoryStore() {}

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        if (Files.exists(FILE)) {
            try {
                Map<String, String> loadedMap = MAPPER.readValue(
                        FILE.toFile(), new TypeReference<Map<String, String>>() {});
                if (loadedMap != null) {
                    values.putAll(loadedMap);
                }
            } catch (Exception e) {
                System.err.println("Could not read parameter history: " + e.getMessage());
            }
        }
    }

    public static synchronized void persist() {
        try {
            File parent = FILE.getParent().toFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            MAPPER.writeValue(FILE.toFile(), values);
        } catch (Exception e) {
            // Graceful in read-only sandbox or permissions issue
            System.err.println("Could not save parameter history: " + e.getMessage());
        }
    }

    public static synchronized String get(String paramName) {
        if (paramName == null || paramName.isBlank()) return null;
        ensureLoaded();
        String val = values.get(paramName);
        if (val != null) return val;
        String lower = paramName.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(lower)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static synchronized void put(String paramName, String value) {
        if (paramName == null || paramName.isBlank()) return;
        ensureLoaded();
        if (value == null) {
            values.remove(paramName);
        } else {
            values.put(paramName, value);
        }
        persist();
    }

    public static synchronized void putAll(Map<String, String> map) {
        if (map == null || map.isEmpty()) return;
        ensureLoaded();
        values.putAll(map);
        persist();
    }

    public static synchronized Map<String, String> getAll() {
        ensureLoaded();
        return new LinkedHashMap<>(values);
    }

    public static synchronized void clear() {
        ensureLoaded();
        values.clear();
        persist();
    }
}
