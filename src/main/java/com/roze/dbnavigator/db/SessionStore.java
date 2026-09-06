package com.roze.dbnavigator.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.roze.dbnavigator.model.DbObject.Kind;

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
 * Also persists which data/collection tabs (a table or collection's own
 * real data grid, opened by double-clicking it in the tree) were open, to
 * a separate file (~/.dbnavigator/session-data-tabs.json) - a real,
 * previously-deliberate scope decision this class used to document right
 * here (data grids were called "cheap to reopen... and carrying no
 * unsaved state of their own worth preserving"), reversed after a real,
 * live report that DataGrip itself does restore these, and that leaving
 * a whole day's worth of open tables behind on every restart doesn't
 * actually feel "cheap" in practice. Kept in its own, separate file
 * rather than folded into the console list above, since it's a genuinely
 * different kind of state (which object was open, not what was typed) -
 * DataTab/MongoCollectionTab already exposed getProfileForReopen()/
 * getTableForReopen()/getCollectionForReopen() before this feature
 * existed, suggesting it was prepared for but never actually finished.
 */
public final class SessionStore {

    private static final Path FILE =
            Path.of(System.getProperty("user.home"), ".dbnavigator", "session.json");
    private static final Path DATA_TABS_FILE =
            Path.of(System.getProperty("user.home"), ".dbnavigator", "session-data-tabs.json");
    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /** One open console: which connection, which database (nullable), its SQL, its tab title, and its kind. */
    public record OpenTab(String profileId, String catalog, String sql, String title, boolean mongo) {}

    /** One open data/collection tab: which connection, which real object (by name/kind/catalog/schema - see MetadataService's own real, established shape for these fields), and whether it's a Mongo collection. */
    public record OpenDataTab(String profileId, String name, Kind kind, String catalog, String schema, boolean mongo) {}

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

    public static synchronized List<OpenDataTab> loadDataTabs() {
        if (!Files.exists(DATA_TABS_FILE)) return new ArrayList<>();
        try {
            List<OpenDataTab> loaded = MAPPER.readValue(DATA_TABS_FILE.toFile(), new TypeReference<List<OpenDataTab>>() {});
            return new ArrayList<>(loaded);
        } catch (IOException e) {
            System.err.println("Could not read data tab session file: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static synchronized void saveDataTabs(List<OpenDataTab> tabs) {
        try {
            Files.createDirectories(DATA_TABS_FILE.getParent());
            MAPPER.writeValue(DATA_TABS_FILE.toFile(), tabs);
        } catch (IOException e) {
            System.err.println("Could not save data tab session file: " + e.getMessage());
        }
    }
}
