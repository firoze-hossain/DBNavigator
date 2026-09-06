package com.roze.dbnavigator.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Persists which Database Explorer tree nodes were expanded, to
 * ~/.dbnavigator/tree-state.json, so they reopen automatically next
 * launch - the same "pick up where you left off" behavior
 * {@link SessionStore} already provides for open query consoles, and the
 * same behavior DataGrip itself provides for its own database tree: the
 * tree you left open is the tree you get back, not a freshly-collapsed
 * one you have to manually re-expand every single time.
 *
 * A tree node is identified by a stable path string (built by walking up
 * its own real parent chain - see SchemaTreePane's own pathOf), not by
 * object identity or array index, since the actual DbObject/TreeItem
 * instances themselves are rebuilt from scratch on every reload - only a
 * name-based path survives that rebuild intact.
 */
public final class TreeStateStore {

    private static final Path FILE =
            Path.of(System.getProperty("user.home"), ".dbnavigator", "tree-state.json");
    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private TreeStateStore() {}

    public static synchronized Set<String> load() {
        if (!Files.exists(FILE)) return new HashSet<>();
        try {
            Set<String> loaded = MAPPER.readValue(FILE.toFile(), new TypeReference<Set<String>>() {});
            return new HashSet<>(loaded);
        } catch (IOException e) {
            System.err.println("Could not read tree state file: " + e.getMessage());
            return new HashSet<>();
        }
    }

    public static synchronized void save(Set<String> expandedPaths) {
        try {
            Files.createDirectories(FILE.getParent());
            MAPPER.writeValue(FILE.toFile(), expandedPaths);
        } catch (IOException e) {
            // Best-effort, matching SessionStore's own real reasoning: losing the
            // remembered tree expansion isn't worth interrupting shutdown over,
            // and there's no user left to show an error dialog to by the time
            // this runs.
            System.err.println("Could not save tree state file: " + e.getMessage());
        }
    }
}
