package com.roze.dbnavigator.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists which query consoles were open when the app last closed, to
 * ~/.dbnavigator/session.json, so they reopen automatically next launch —
 * the same "pick up where you left off" behavior most IDEs (and DataGrip
 * itself) provide.
 *
 * Deliberately scoped to consoles only, not every open tab: data grids,
 * diagrams, and structure views are cheap to reopen by clicking the object
 * in the tree again, and carry no unsaved state of their own worth
 * preserving. This mirrors the existing "Reopen Closed Tab" feature, which
 * made the same scoping choice for the same reason — this file format is
 * the durable, cross-restart counterpart to that in-memory stack.
 */
public final class SessionStore {

    private static final Path FILE =
            Path.of(System.getProperty("user.home"), ".dbnavigator", "session.json");
    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /** One open console: which connection, which database (nullable), its SQL, its tab title, and its kind. */
    public record OpenTab(String profileId, String catalog, String sql, String title, boolean mongo) {}

    private SessionStore() {}

    public static synchronized List<OpenTab> load() {
        if (!Files.exists(FILE)) return new ArrayList<>();
        try {
            List<OpenTab> loaded = MAPPER.readValue(FILE.toFile(), new TypeReference<List<OpenTab>>() {});
            return new ArrayList<>(loaded);
        } catch (IOException e) {
            System.err.println("Could not read session file: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static synchronized void save(List<OpenTab> tabs) {
        try {
            Files.createDirectories(FILE.getParent());
            MAPPER.writeValue(FILE.toFile(), tabs);
        } catch (IOException e) {
            // Best-effort — losing the reopen-on-restart list isn't worth
            // interrupting shutdown over, and there's no user left to show
            // an error dialog to by the time this runs.
            System.err.println("Could not save session file: " + e.getMessage());
        }
    }
}
