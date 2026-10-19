package com.roze.dbnavigator.search;

import com.roze.dbnavigator.db.ClientRegistry;
import com.roze.dbnavigator.db.ConnectionStore;
import com.roze.dbnavigator.db.MetadataService;
import com.roze.dbnavigator.db.ProjectStore;
import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.DbObject.Kind;
import com.roze.dbnavigator.ui.MainWindow;
import com.roze.dbnavigator.ui.QueryTab;
import com.roze.dbnavigator.ui.action.ActionManager;
import com.roze.dbnavigator.ui.action.AnAction;
import javafx.application.Platform;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.stream.Stream;

/**
 * High-performance, multi-source search engine for DataGrip-grade "Search Everywhere".
 * Searches across:
 * - Database objects: Tables, Views, Columns, Sequences, Keys, Constraints, Indexes
 * - Files: Query Consoles, SQL scripts, Scratch files
 * - Actions & Settings: IDE actions and configuration dialogs
 * - Code & Text: Full-text occurrences in open query tabs and SQL files
 */
public final class SearchEverywhereService {

    private SearchEverywhereService() {}

    public static List<SearchHit> search(
            String query,
            SearchHit.Category category,
            boolean includeNonProject,
            MainWindow mainWindow,
            AtomicInteger activeGeneration,
            int currentGen,
            int limit
    ) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String q = query.trim();
        List<SearchHit> results = new ArrayList<>();

        boolean searchDb = (category == SearchHit.Category.ALL || category == SearchHit.Category.DATABASE);
        boolean searchFiles = (category == SearchHit.Category.ALL || category == SearchHit.Category.FILES);
        boolean searchActions = (category == SearchHit.Category.ALL || category == SearchHit.Category.ACTIONS);
        boolean searchText = (category == SearchHit.Category.ALL || category == SearchHit.Category.TEXT || category == SearchHit.Category.CODE);

        // 1. Database Search across all configured data sources
        if (searchDb) {
            searchDatabase(q, includeNonProject, mainWindow, activeGeneration, currentGen, results, limit);
        }
        if (isCancelled(activeGeneration, currentGen)) return List.of();

        // 2. Consoles and Files Search
        if (searchFiles) {
            searchConsolesAndFiles(q, mainWindow, activeGeneration, currentGen, results, limit);
        }
        if (isCancelled(activeGeneration, currentGen)) return List.of();

        // 3. Actions and Settings Search (deduplicated, word-matched, capped in ALL tab)
        if (searchActions) {
            searchActions(q, category, activeGeneration, currentGen, results, limit);
        }
        if (isCancelled(activeGeneration, currentGen)) return List.of();

        // 4. Text and Code Search
        if (searchText) {
            searchOpenTabsText(q, mainWindow, activeGeneration, currentGen, results, limit);
        }
        if (isCancelled(activeGeneration, currentGen)) return List.of();

        // Deduplicate results by (Type + Name + Detail)
        Set<String> seenKeys = new HashSet<>();
        List<SearchHit> deduped = new ArrayList<>();
        for (SearchHit hit : results) {
            String key = hit.getType() + ":" + hit.getName() + ":" + (hit.getDetail() != null ? hit.getDetail() : "");
            if (seenKeys.add(key)) {
                deduped.add(hit);
            }
        }
        results = deduped;

        // Sort by DataGrip ranking score (tables and columns strictly prioritize over actions)
        results.sort(Comparator.comparingInt((SearchHit h) -> h.calculateScore(q)).reversed());

        if (results.size() > limit) {
            return new ArrayList<>(results.subList(0, limit));
        }
        return results;
    }

    private static boolean isCancelled(AtomicInteger gen, int currentGen) {
        return gen != null && gen.get() != currentGen;
    }

    private static String buildSqlPattern(String query) {
        if (query == null || query.isBlank()) return "%";
        String trimmed = query.trim();
        String[] parts = trimmed.split("[ \\t]+");
        if (parts.length > 1) {
            StringBuilder sb = new StringBuilder("%");
            for (String p : parts) {
                if (!p.isEmpty()) {
                    sb.append(p).append("%");
                }
            }
            return sb.toString();
        }
        return "%" + trimmed + "%";
    }

    private static final Map<String, List<String>> discoveredDatabasesCache = new ConcurrentHashMap<>();
    private static final Map<String, Long> unreachableProfiles = new ConcurrentHashMap<>();
    private static final long UNREACHABLE_COOLDOWN_MS = 180_000; // 3 minutes cooldown

    public static boolean isProfileUnreachable(String profileId) {
        if (profileId == null) return false;
        Long failedAt = unreachableProfiles.get(profileId);
        if (failedAt == null) return false;
        if (System.currentTimeMillis() - failedAt < UNREACHABLE_COOLDOWN_MS) {
            return true;
        }
        unreachableProfiles.remove(profileId);
        return false;
    }

    public static void markProfileUnreachable(String profileId) {
        if (profileId != null) {
            unreachableProfiles.put(profileId, System.currentTimeMillis());
        }
    }

    public static void markProfileReachable(String profileId) {
        if (profileId != null) {
            unreachableProfiles.remove(profileId);
        }
    }

    public static void clearUnreachableProfiles() {
        unreachableProfiles.clear();
    }

    public static boolean isConnectionFailure(Throwable t) {
        if (t == null) return false;
        if (t instanceof java.net.SocketException
                || t instanceof java.net.UnknownHostException
                || t instanceof java.util.concurrent.TimeoutException) {
            return true;
        }
        String className = t.getClass().getName().toLowerCase(Locale.ROOT);
        if (className.contains("connectexception")
                || className.contains("unknownhostexception")
                || className.contains("sockettimeoutexception")
                || className.contains("noroutetohostexception")) {
            return true;
        }
        String msg = t.getMessage() != null ? t.getMessage().toLowerCase(Locale.ROOT) : "";
        if (msg.contains("failed to connect")
                || msg.contains("connection refused")
                || msg.contains("failed to initialize pool")
                || msg.contains("the connection attempt failed")
                || msg.contains("communications link failure")
                || msg.contains("unable to connect")
                || msg.contains("unknown host")
                || msg.contains("unknownhost")
                || msg.contains("network is unreachable")
                || msg.contains("timed out")
                || msg.contains("timeout")
                || msg.contains("could not connect")
                || msg.contains("no route to host")
                || msg.contains("connection reset")
                || msg.contains("connection closed")
                || msg.contains("server not found")
                || msg.contains("host not found")
                || msg.contains("cannot open connection")
                || msg.contains("io error")
                || (msg.contains("connection to") && msg.contains("refused"))) {
            return true;
        }
        if (t.getCause() != null && t.getCause() != t) {
            return isConnectionFailure(t.getCause());
        }
        return false;
    }

    public static void clearDiscoveredDatabasesCache() {
        discoveredDatabasesCache.clear();
    }

    public static void invalidateDatabaseCache(String profileId) {
        if (profileId != null) {
            discoveredDatabasesCache.remove(profileId);
        }
    }

    static Map<String, List<String>> getDiscoveredDatabasesCache() {
        return discoveredDatabasesCache;
    }

    static List<String> resolveDatabasesForProfile(ConnectionProfile profile) {
        // A. If the user explicitly configured visibleDatabases (e.g. from Show/Hide Schemas dialog), respect it!
        if (profile.getVisibleDatabases() != null && !profile.getVisibleDatabases().isEmpty()) {
            return new ArrayList<>(profile.getVisibleDatabases());
        }

        // B. SQLite has 1 schema/database ("main")
        if (profile.getType() == DatabaseType.SQLITE) {
            return List.of("main");
        }

        // C. Oracle organizes by schemas rather than catalogs
        if (profile.getType() == DatabaseType.ORACLE) {
            return List.of("");
        }

        // D. Check discovery cache
        List<String> cached = discoveredDatabasesCache.get(profile.getId());
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        // D2. If this profile is currently known to be unreachable (offline/uninstalled), skip network discovery
        if (isProfileUnreachable(profile.getId())) {
            if (profile.getDatabase() != null && !profile.getDatabase().isBlank()) {
                return List.of(profile.getDatabase());
            }
            return List.of("");
        }

        // E. Dynamic discovery without requiring manual tree exploration
        try {
            List<String> discovered = MetadataService.listDatabaseNames(profile);
            if (discovered != null && !discovered.isEmpty()) {
                discoveredDatabasesCache.put(profile.getId(), discovered);
                markProfileReachable(profile.getId());
                return discovered;
            }
        } catch (Exception e) {
            if (isConnectionFailure(e)) {
                markProfileUnreachable(profile.getId());
            }
        }

        // F. Fallback to default configured database if discovery fails
        if (profile.getDatabase() != null && !profile.getDatabase().isBlank()) {
            return List.of(profile.getDatabase());
        }
        return List.of("");
    }

    private static void searchDatabase(
            String query,
            boolean includeNonProject,
            MainWindow mainWindow,
            AtomicInteger activeGen,
            int currentGen,
            List<SearchHit> results,
            int limit
    ) {
        // 1. In-memory loaded tree items search (0ms instant response)
        if (mainWindow != null && mainWindow.getSchemaPane() != null) {
            try {
                if (Platform.isFxApplicationThread()) {
                    mainWindow.getSchemaPane().searchLoadedTreeItems(query, results, limit);
                } else {
                    java.util.concurrent.CompletableFuture<List<SearchHit>> future = new java.util.concurrent.CompletableFuture<>();
                    Platform.runLater(() -> {
                        try {
                            List<SearchHit> loadedHits = new ArrayList<>();
                            mainWindow.getSchemaPane().searchLoadedTreeItems(query, loadedHits, limit);
                            future.complete(loadedHits);
                        } catch (Exception e) {
                            future.complete(List.of());
                        }
                    });
                    List<SearchHit> loadedHits = future.get(150, java.util.concurrent.TimeUnit.MILLISECONDS);
                    if (loadedHits != null) {
                        results.addAll(loadedHits);
                    }
                }
            } catch (Exception ignored) {}
        }

        // 2. Query configured database connections
        List<ConnectionProfile> profiles = includeNonProject
                ? ConnectionStore.load()
                : ConnectionStore.loadForProject(mainWindow != null && mainWindow.getProject() != null ? mainWindow.getProject().getName() : "default");
        if (profiles.isEmpty()) {
            profiles = ConnectionStore.load();
        }

        for (ConnectionProfile profile : profiles) {
            if (isCancelled(activeGen, currentGen)) return;

            // If database server is offline or not installed, softly skip remote queries
            if (isProfileUnreachable(profile.getId())) {
                continue;
            }

            try {
                if (profile.getType() == DatabaseType.MONGODB) {
                    searchMongo(profile, query, results, limit);
                    continue;
                }
                if (profile.getType() == DatabaseType.ORACLE) {
                    searchOracle(profile, query, results, limit);
                    continue;
                }
                if (profile.getType() == DatabaseType.MYSQL || profile.getType() == DatabaseType.MARIADB) {
                    searchMySql(profile, query, results, limit);
                    continue;
                }
                if (profile.getType() == DatabaseType.SQLITE) {
                    searchSqlite(profile, "main", query, results, limit);
                    continue;
                }

                // Multi-catalog engines: PostgreSQL, SQL Server, etc.
                List<String> catalogs = resolveDatabasesForProfile(profile);

                // If resolveDatabasesForProfile discovered server is offline, skip querying
                if (isProfileUnreachable(profile.getId())) {
                    continue;
                }

                for (String catalog : catalogs) {
                    if (isCancelled(activeGen, currentGen)) return;
                    try {
                        if (profile.getType() == DatabaseType.POSTGRESQL) {
                            searchPostgreSql(profile, catalog, query, results, limit);
                        } else if (profile.getType() == DatabaseType.SQLSERVER) {
                            searchSqlServer(profile, catalog, query, results, limit);
                        } else {
                            searchGenericJdbc(profile, catalog, query, results, limit);
                        }
                    } catch (Exception e) {
                        if (isConnectionFailure(e)) {
                            markProfileUnreachable(profile.getId());
                            break; // Stop attempting remaining catalogs for this unreachable server
                        }
                    }
                    if (results.size() >= limit * 2) break;
                }
            } catch (Exception e) {
                if (isConnectionFailure(e)) {
                    markProfileUnreachable(profile.getId());
                }
            }
            if (results.size() >= limit * 2) break;
        }
    }

    private static void searchPostgreSql(
            ConnectionProfile profile,
            String catalog,
            String query,
            List<SearchHit> results,
            int limit
    ) {
        String dbName = catalog != null ? catalog : (profile.getDatabase() != null ? profile.getDatabase() : "postgres");

        // A. Tables & Views
        String tableSql = """
                SELECT c.relname, n.nspname, c.relkind
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE c.relkind IN ('r', 'p', 'v', 'm')
                  AND n.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
                  AND c.relname ILIKE ?
                  AND NOT EXISTS (SELECT 1 FROM pg_inherits i WHERE i.inhrelid = c.oid)
                ORDER BY c.relname
                LIMIT ?
                """;

        // B. Columns
        String colSql = """
                SELECT a.attname AS col_name, c.relname AS tbl_name, n.nspname AS sch_name,
                       CASE WHEN EXISTS (SELECT 1 FROM pg_constraint k WHERE k.conrelid = a.attrelid AND a.attnum = ANY(k.conkey) AND k.contype = 'p') THEN 1 ELSE 0 END AS is_pk,
                       CASE WHEN EXISTS (SELECT 1 FROM pg_constraint k WHERE k.conrelid = a.attrelid AND a.attnum = ANY(k.conkey) AND k.contype = 'f') THEN 1 ELSE 0 END AS is_fk
                FROM pg_attribute a
                JOIN pg_class c ON c.oid = a.attrelid
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE a.attnum > 0 AND NOT a.attisdropped
                  AND c.relkind IN ('r', 'p', 'v', 'm')
                  AND n.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
                  AND a.attname ILIKE ?
                ORDER BY a.attname, c.relname
                LIMIT ?
                """;

        // C. Sequences
        String seqSql = """
                SELECT c.relname, n.nspname
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE c.relkind = 'S'
                  AND n.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
                  AND c.relname ILIKE ?
                ORDER BY c.relname
                LIMIT ?
                """;

        // D. Indexes & Constraints
        String idxSql = """
                SELECT c.relname AS idx_name, t.relname AS tbl_name, n.nspname AS sch_name,
                       i.indisunique, i.indisprimary
                FROM pg_index i
                JOIN pg_class c ON c.oid = i.indexrelid
                JOIN pg_class t ON t.oid = i.indrelid
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
                  AND c.relname ILIKE ?
                ORDER BY c.relname
                LIMIT ?
                """;

        try (Connection conn = ClientRegistry.jdbc(profile, catalog).getConnection()) {
            // Run tables
            try (PreparedStatement stmt = conn.prepareStatement(tableSql)) {
                stmt.setString(1, buildSqlPattern(query));
                stmt.setInt(2, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString(1);
                        String schema = rs.getString(2);
                        String relkind = rs.getString(3);
                        boolean isView = "v".equalsIgnoreCase(relkind) || "m".equalsIgnoreCase(relkind);
                        DbObject obj = new DbObject(name, isView ? Kind.VIEW : Kind.TABLE, catalog, schema);
                        String loc = dbName + "." + schema + " [" + profile.getName() + "]";
                        String uri = "DATABASE/" + profile.getId() + "/database/" + dbName + "/schema/" + schema + "/table/" + name;
                        results.add(new SearchHit.Builder()
                                .type(isView ? SearchHit.Type.VIEW : SearchHit.Type.TABLE)
                                .name(name)
                                .detail(loc)
                                .fullUri(uri)
                                .profile(profile)
                                .dbObject(obj)
                                .parentTable(name)
                                .build());
                    }
                }
            } catch (SQLException ignored) {}

            // Run columns
            try (PreparedStatement stmt = conn.prepareStatement(colSql)) {
                stmt.setString(1, buildSqlPattern(query));
                stmt.setInt(2, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String colName = rs.getString(1);
                        String tblName = rs.getString(2);
                        String schema = rs.getString(3);
                        boolean isPk = rs.getInt(4) == 1;
                        boolean isFk = rs.getInt(5) == 1;

                        DbObject tblObj = new DbObject(tblName, Kind.TABLE, catalog, schema);
                        String loc = dbName + "." + schema + "." + tblName + " [" + profile.getName() + "]";
                        String uri = "DATABASE/" + profile.getId() + "/database/" + dbName + "/schema/" + schema + "/table/" + tblName + "/column/" + colName;

                        results.add(new SearchHit.Builder()
                                .type(SearchHit.Type.COLUMN)
                                .name(colName)
                                .detail(loc)
                                .fullUri(uri)
                                .profile(profile)
                                .dbObject(tblObj)
                                .parentTable(tblName)
                                .columnName(colName)
                                .primaryKey(isPk)
                                .foreignKey(isFk)
                                .build());
                    }
                }
            } catch (SQLException ignored) {}

            // Run sequences
            try (PreparedStatement stmt = conn.prepareStatement(seqSql)) {
                stmt.setString(1, buildSqlPattern(query));
                stmt.setInt(2, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String seqName = rs.getString(1);
                        String schema = rs.getString(2);
                        DbObject seqObj = new DbObject(seqName, Kind.SEQUENCE, catalog, schema);
                        String loc = dbName + "." + schema + " [" + profile.getName() + "]";
                        String uri = "DATABASE/" + profile.getId() + "/database/" + dbName + "/schema/" + schema + "/sequence/" + seqName;

                        results.add(new SearchHit.Builder()
                                .type(SearchHit.Type.SEQUENCE)
                                .name(seqName)
                                .detail(loc)
                                .fullUri(uri)
                                .profile(profile)
                                .dbObject(seqObj)
                                .build());
                    }
                }
            } catch (SQLException ignored) {}

            // Run indexes & constraints
            try (PreparedStatement stmt = conn.prepareStatement(idxSql)) {
                stmt.setString(1, buildSqlPattern(query));
                stmt.setInt(2, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String idxName = rs.getString(1);
                        String tblName = rs.getString(2);
                        String schema = rs.getString(3);
                        boolean isUnique = rs.getBoolean(4);
                        boolean isPk = rs.getBoolean(5);

                        DbObject idxObj = new DbObject(idxName, isPk ? Kind.KEY : Kind.INDEX, catalog, schema);
                        idxObj.setTableName(tblName);

                        String loc = dbName + "." + schema + "." + tblName + " [" + profile.getName() + "]";
                        String uri = "DATABASE/" + profile.getId() + "/database/" + dbName + "/schema/" + schema + "/table/" + tblName + "/index/" + idxName;

                        results.add(new SearchHit.Builder()
                                .type(isPk ? SearchHit.Type.PRIMARY_KEY : SearchHit.Type.INDEX)
                                .name(idxName)
                                .detail(loc)
                                .fullUri(uri)
                                .profile(profile)
                                .dbObject(idxObj)
                                .parentTable(tblName)
                                .primaryKey(isPk)
                                .uniqueIndex(isUnique)
                                .build());
                    }
                }
            } catch (SQLException ignored) {}
        } catch (SQLException e) {
            if (isConnectionFailure(e)) {
                markProfileUnreachable(profile.getId());
            }
        }
    }

    private static void searchMySql(
            ConnectionProfile profile,
            String query,
            List<SearchHit> results,
            int limit
    ) {
        List<String> visible = profile.getVisibleDatabases();
        boolean hasFilter = visible != null && !visible.isEmpty();

        String schemaFilter;
        if (hasFilter) {
            schemaFilter = "TABLE_SCHEMA IN (" + String.join(",", visible.stream().map(s -> "?").toList()) + ")";
        } else {
            schemaFilter = "TABLE_SCHEMA NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys')";
        }

        String tableSql = "SELECT TABLE_NAME, TABLE_SCHEMA, TABLE_TYPE " +
                "FROM information_schema.TABLES " +
                "WHERE " + schemaFilter + " AND TABLE_NAME LIKE ? " +
                "ORDER BY TABLE_NAME LIMIT ?";

        String colSql = "SELECT COLUMN_NAME, TABLE_NAME, TABLE_SCHEMA, COLUMN_KEY " +
                "FROM information_schema.COLUMNS " +
                "WHERE " + schemaFilter + " AND COLUMN_NAME LIKE ? " +
                "ORDER BY COLUMN_NAME, TABLE_NAME LIMIT ?";

        String idxSql = "SELECT INDEX_NAME, TABLE_NAME, TABLE_SCHEMA, NON_UNIQUE " +
                "FROM information_schema.STATISTICS " +
                "WHERE " + schemaFilter + " AND INDEX_NAME LIKE ? " +
                "ORDER BY INDEX_NAME LIMIT ?";

        try (Connection conn = ClientRegistry.jdbc(profile, null).getConnection()) {
            // Tables
            try (PreparedStatement stmt = conn.prepareStatement(tableSql)) {
                int paramIdx = 1;
                if (hasFilter) {
                    for (String db : visible) {
                        stmt.setString(paramIdx++, db);
                    }
                }
                stmt.setString(paramIdx++, buildSqlPattern(query));
                stmt.setInt(paramIdx, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString(1);
                        String schema = rs.getString(2);
                        String type = rs.getString(3);
                        boolean isView = "VIEW".equalsIgnoreCase(type);
                        DbObject obj = new DbObject(name, isView ? Kind.VIEW : Kind.TABLE, schema, null);
                        String loc = schema + " [" + profile.getName() + "]";
                        String uri = "DATABASE/" + profile.getId() + "/database/" + schema + "/table/" + name;

                        results.add(new SearchHit.Builder()
                                .type(isView ? SearchHit.Type.VIEW : SearchHit.Type.TABLE)
                                .name(name)
                                .detail(loc)
                                .fullUri(uri)
                                .profile(profile)
                                .dbObject(obj)
                                .parentTable(name)
                                .build());
                    }
                }
            } catch (SQLException ignored) {}

            // Columns
            try (PreparedStatement stmt = conn.prepareStatement(colSql)) {
                int paramIdx = 1;
                if (hasFilter) {
                    for (String db : visible) {
                        stmt.setString(paramIdx++, db);
                    }
                }
                stmt.setString(paramIdx++, buildSqlPattern(query));
                stmt.setInt(paramIdx, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String colName = rs.getString(1);
                        String tblName = rs.getString(2);
                        String schema = rs.getString(3);
                        String key = rs.getString(4);
                        boolean isPk = "PRI".equalsIgnoreCase(key);
                        boolean isMul = "MUL".equalsIgnoreCase(key);

                        DbObject tblObj = new DbObject(tblName, Kind.TABLE, schema, null);
                        String loc = schema + "." + tblName + " [" + profile.getName() + "]";
                        String uri = "DATABASE/" + profile.getId() + "/database/" + schema + "/table/" + tblName + "/column/" + colName;

                        results.add(new SearchHit.Builder()
                                .type(SearchHit.Type.COLUMN)
                                .name(colName)
                                .detail(loc)
                                .fullUri(uri)
                                .profile(profile)
                                .dbObject(tblObj)
                                .parentTable(tblName)
                                .columnName(colName)
                                .primaryKey(isPk)
                                .foreignKey(isMul)
                                .build());
                    }
                }
            } catch (SQLException ignored) {}

            // Indexes & PKs
            try (PreparedStatement stmt = conn.prepareStatement(idxSql)) {
                int paramIdx = 1;
                if (hasFilter) {
                    for (String db : visible) {
                        stmt.setString(paramIdx++, db);
                    }
                }
                stmt.setString(paramIdx++, buildSqlPattern(query));
                stmt.setInt(paramIdx, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String idxName = rs.getString(1);
                        String tblName = rs.getString(2);
                        String schema = rs.getString(3);
                        int nonUnique = rs.getInt(4);
                        boolean isPk = "PRIMARY".equalsIgnoreCase(idxName);
                        boolean isUnique = nonUnique == 0;

                        DbObject idxObj = new DbObject(idxName, isPk ? Kind.KEY : Kind.INDEX, schema, null);
                        idxObj.setTableName(tblName);
                        String loc = schema + "." + tblName + " [" + profile.getName() + "]";
                        String uri = "DATABASE/" + profile.getId() + "/database/" + schema + "/table/" + tblName + "/index/" + idxName;

                        results.add(new SearchHit.Builder()
                                .type(isPk ? SearchHit.Type.PRIMARY_KEY : SearchHit.Type.INDEX)
                                .name(idxName)
                                .detail(loc)
                                .fullUri(uri)
                                .profile(profile)
                                .dbObject(idxObj)
                                .parentTable(tblName)
                                .primaryKey(isPk)
                                .uniqueIndex(isUnique)
                                .build());
                    }
                }
            } catch (SQLException ignored) {}
        } catch (SQLException e) {
            if (isConnectionFailure(e)) {
                markProfileUnreachable(profile.getId());
            }
        }
    }

    private static void searchOracle(
            ConnectionProfile profile,
            String query,
            List<SearchHit> results,
            int limit
    ) {
        List<String> visible = profile.getVisibleDatabases();
        boolean hasFilter = visible != null && !visible.isEmpty();

        String ownerFilter;
        if (hasFilter) {
            ownerFilter = "OWNER IN (" + String.join(",", visible.stream().map(s -> "?").toList()) + ")";
        } else {
            ownerFilter = "OWNER NOT IN (" +
                    String.join(",", MetadataService.ORACLE_SYSTEM_SCHEMAS.stream().map(s -> "'" + s.toUpperCase(Locale.ROOT) + "'").toList()) +
                    ")";
        }

        String tableSql = "SELECT * FROM (SELECT TABLE_NAME, OWNER FROM ALL_TABLES " +
                "WHERE " + ownerFilter + " AND UPPER(TABLE_NAME) LIKE ? ORDER BY TABLE_NAME) WHERE ROWNUM <= ?";

        String viewSql = "SELECT * FROM (SELECT VIEW_NAME, OWNER FROM ALL_VIEWS " +
                "WHERE " + ownerFilter + " AND UPPER(VIEW_NAME) LIKE ? ORDER BY VIEW_NAME) WHERE ROWNUM <= ?";

        String colSql = "SELECT * FROM (SELECT COLUMN_NAME, TABLE_NAME, OWNER FROM ALL_TAB_COLUMNS " +
                "WHERE " + ownerFilter + " AND UPPER(COLUMN_NAME) LIKE ? ORDER BY COLUMN_NAME, TABLE_NAME) WHERE ROWNUM <= ?";

        String seqOwnerFilter = hasFilter ? "SEQUENCE_" + ownerFilter : "SEQUENCE_OWNER NOT IN (" +
                String.join(",", MetadataService.ORACLE_SYSTEM_SCHEMAS.stream().map(s -> "'" + s.toUpperCase(Locale.ROOT) + "'").toList()) + ")";
        String seqSql = "SELECT * FROM (SELECT SEQUENCE_NAME, SEQUENCE_OWNER FROM ALL_SEQUENCES " +
                "WHERE " + seqOwnerFilter + " AND UPPER(SEQUENCE_NAME) LIKE ? ORDER BY SEQUENCE_NAME) WHERE ROWNUM <= ?";

        String idxSql = "SELECT * FROM (SELECT INDEX_NAME, TABLE_NAME, OWNER, UNIQUENESS FROM ALL_INDEXES " +
                "WHERE " + ownerFilter + " AND UPPER(INDEX_NAME) LIKE ? ORDER BY INDEX_NAME) WHERE ROWNUM <= ?";

        try (Connection conn = ClientRegistry.jdbc(profile, null).getConnection()) {
            // Tables
            try (PreparedStatement stmt = conn.prepareStatement(tableSql)) {
                int p = 1;
                if (hasFilter) {
                    for (String s : visible) stmt.setString(p++, s.toUpperCase(Locale.ROOT));
                }
                stmt.setString(p++, buildSqlPattern(query).toUpperCase(Locale.ROOT));
                stmt.setInt(p, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString(1);
                        String owner = rs.getString(2);
                        DbObject obj = new DbObject(name, Kind.TABLE, owner, owner);
                        String loc = owner + " [" + profile.getName() + "]";
                        String uri = "DATABASE/" + profile.getId() + "/schema/" + owner + "/table/" + name;
                        results.add(new SearchHit.Builder()
                                .type(SearchHit.Type.TABLE)
                                .name(name)
                                .detail(loc)
                                .fullUri(uri)
                                .profile(profile)
                                .dbObject(obj)
                                .parentTable(name)
                                .build());
                    }
                }
            } catch (SQLException ignored) {}

            // Views
            try (PreparedStatement stmt = conn.prepareStatement(viewSql)) {
                int p = 1;
                if (hasFilter) {
                    for (String s : visible) stmt.setString(p++, s.toUpperCase(Locale.ROOT));
                }
                stmt.setString(p++, buildSqlPattern(query).toUpperCase(Locale.ROOT));
                stmt.setInt(p, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString(1);
                        String owner = rs.getString(2);
                        DbObject obj = new DbObject(name, Kind.VIEW, owner, owner);
                        String loc = owner + " [" + profile.getName() + "]";
                        String uri = "DATABASE/" + profile.getId() + "/schema/" + owner + "/table/" + name;
                        results.add(new SearchHit.Builder()
                                .type(SearchHit.Type.VIEW)
                                .name(name)
                                .detail(loc)
                                .fullUri(uri)
                                .profile(profile)
                                .dbObject(obj)
                                .parentTable(name)
                                .build());
                    }
                }
            } catch (SQLException ignored) {}

            // Columns
            try (PreparedStatement stmt = conn.prepareStatement(colSql)) {
                int p = 1;
                if (hasFilter) {
                    for (String s : visible) stmt.setString(p++, s.toUpperCase(Locale.ROOT));
                }
                stmt.setString(p++, buildSqlPattern(query).toUpperCase(Locale.ROOT));
                stmt.setInt(p, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String colName = rs.getString(1);
                        String tblName = rs.getString(2);
                        String owner = rs.getString(3);
                        DbObject tblObj = new DbObject(tblName, Kind.TABLE, owner, owner);
                        String loc = owner + "." + tblName + " [" + profile.getName() + "]";
                        String uri = "DATABASE/" + profile.getId() + "/schema/" + owner + "/table/" + tblName + "/column/" + colName;
                        results.add(new SearchHit.Builder()
                                .type(SearchHit.Type.COLUMN)
                                .name(colName)
                                .detail(loc)
                                .fullUri(uri)
                                .profile(profile)
                                .dbObject(tblObj)
                                .parentTable(tblName)
                                .columnName(colName)
                                .build());
                    }
                }
            } catch (SQLException ignored) {}

            // Sequences
            try (PreparedStatement stmt = conn.prepareStatement(seqSql)) {
                int p = 1;
                if (hasFilter) {
                    for (String s : visible) stmt.setString(p++, s.toUpperCase(Locale.ROOT));
                }
                stmt.setString(p++, buildSqlPattern(query).toUpperCase(Locale.ROOT));
                stmt.setInt(p, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String seqName = rs.getString(1);
                        String owner = rs.getString(2);
                        DbObject seqObj = new DbObject(seqName, Kind.SEQUENCE, owner, owner);
                        String loc = owner + " [" + profile.getName() + "]";
                        String uri = "DATABASE/" + profile.getId() + "/schema/" + owner + "/sequence/" + seqName;
                        results.add(new SearchHit.Builder()
                                .type(SearchHit.Type.SEQUENCE)
                                .name(seqName)
                                .detail(loc)
                                .fullUri(uri)
                                .profile(profile)
                                .dbObject(seqObj)
                                .build());
                    }
                }
            } catch (SQLException ignored) {}

            // Indexes
            try (PreparedStatement stmt = conn.prepareStatement(idxSql)) {
                int p = 1;
                if (hasFilter) {
                    for (String s : visible) stmt.setString(p++, s.toUpperCase(Locale.ROOT));
                }
                stmt.setString(p++, buildSqlPattern(query).toUpperCase(Locale.ROOT));
                stmt.setInt(p, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String idxName = rs.getString(1);
                        String tblName = rs.getString(2);
                        String owner = rs.getString(3);
                        String uniqueness = rs.getString(4);
                        boolean isUnique = "UNIQUE".equalsIgnoreCase(uniqueness);

                        DbObject idxObj = new DbObject(idxName, Kind.INDEX, owner, owner);
                        idxObj.setTableName(tblName);
                        String loc = owner + "." + tblName + " [" + profile.getName() + "]";
                        String uri = "DATABASE/" + profile.getId() + "/schema/" + owner + "/table/" + tblName + "/index/" + idxName;

                        results.add(new SearchHit.Builder()
                                .type(SearchHit.Type.INDEX)
                                .name(idxName)
                                .detail(loc)
                                .fullUri(uri)
                                .profile(profile)
                                .dbObject(idxObj)
                                .parentTable(tblName)
                                .uniqueIndex(isUnique)
                                .build());
                    }
                }
            } catch (SQLException ignored) {}
        } catch (SQLException e) {
            if (isConnectionFailure(e)) {
                markProfileUnreachable(profile.getId());
            }
        }
    }

    private static void searchSqlServer(
            ConnectionProfile profile,
            String catalog,
            String query,
            List<SearchHit> results,
            int limit
    ) {
        String dbName = catalog != null ? catalog : (profile.getDatabase() != null ? profile.getDatabase() : "master");

        String tableSql = """
                SELECT TOP (?) t.name, s.name, 'TABLE'
                FROM sys.tables t
                JOIN sys.schemas s ON s.schema_id = t.schema_id
                WHERE s.name NOT IN ('sys', 'information_schema')
                  AND t.name LIKE ?
                ORDER BY t.name
                """;

        String viewSql = """
                SELECT TOP (?) v.name, s.name, 'VIEW'
                FROM sys.views v
                JOIN sys.schemas s ON s.schema_id = v.schema_id
                WHERE s.name NOT IN ('sys', 'information_schema')
                  AND v.name LIKE ?
                ORDER BY v.name
                """;

        String colSql = """
                SELECT TOP (?) c.name AS col_name, o.name AS tbl_name, s.name AS sch_name,
                       CASE WHEN EXISTS (
                           SELECT 1 FROM sys.index_columns ic
                           JOIN sys.indexes i ON i.object_id = ic.object_id AND i.index_id = ic.index_id
                           WHERE ic.object_id = c.object_id AND ic.column_id = c.column_id AND i.is_primary_key = 1
                       ) THEN 1 ELSE 0 END AS is_pk
                FROM sys.columns c
                JOIN sys.objects o ON o.object_id = c.object_id
                JOIN sys.schemas s ON s.schema_id = o.schema_id
                WHERE o.type IN ('U', 'V')
                  AND s.name NOT IN ('sys', 'information_schema')
                  AND c.name LIKE ?
                ORDER BY c.name, o.name
                """;

        String idxSql = """
                SELECT TOP (?) i.name AS idx_name, t.name AS tbl_name, s.name AS sch_name,
                       i.is_unique, i.is_primary_key
                FROM sys.indexes i
                JOIN sys.tables t ON t.object_id = i.object_id
                JOIN sys.schemas s ON s.schema_id = t.schema_id
                WHERE s.name NOT IN ('sys', 'information_schema')
                  AND i.name IS NOT NULL
                  AND i.name LIKE ?
                ORDER BY i.name
                """;

        try (Connection conn = ClientRegistry.jdbc(profile, catalog).getConnection()) {
            // Tables
            try (PreparedStatement stmt = conn.prepareStatement(tableSql)) {
                stmt.setInt(1, limit);
                stmt.setString(2, buildSqlPattern(query));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString(1);
                        String schema = rs.getString(2);
                        DbObject obj = new DbObject(name, Kind.TABLE, catalog, schema);
                        String loc = dbName + "." + schema + " [" + profile.getName() + "]";
                        String uri = "DATABASE/" + profile.getId() + "/database/" + dbName + "/schema/" + schema + "/table/" + name;
                        results.add(new SearchHit.Builder()
                                .type(SearchHit.Type.TABLE)
                                .name(name)
                                .detail(loc)
                                .fullUri(uri)
                                .profile(profile)
                                .dbObject(obj)
                                .parentTable(name)
                                .build());
                    }
                }
            } catch (SQLException ignored) {}

            // Views
            try (PreparedStatement stmt = conn.prepareStatement(viewSql)) {
                stmt.setInt(1, limit);
                stmt.setString(2, buildSqlPattern(query));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString(1);
                        String schema = rs.getString(2);
                        DbObject obj = new DbObject(name, Kind.VIEW, catalog, schema);
                        String loc = dbName + "." + schema + " [" + profile.getName() + "]";
                        String uri = "DATABASE/" + profile.getId() + "/database/" + dbName + "/schema/" + schema + "/table/" + name;
                        results.add(new SearchHit.Builder()
                                .type(SearchHit.Type.VIEW)
                                .name(name)
                                .detail(loc)
                                .fullUri(uri)
                                .profile(profile)
                                .dbObject(obj)
                                .parentTable(name)
                                .build());
                    }
                }
            } catch (SQLException ignored) {}

            // Columns
            try (PreparedStatement stmt = conn.prepareStatement(colSql)) {
                stmt.setInt(1, limit);
                stmt.setString(2, buildSqlPattern(query));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String colName = rs.getString(1);
                        String tblName = rs.getString(2);
                        String schema = rs.getString(3);
                        boolean isPk = rs.getInt(4) == 1;

                        DbObject tblObj = new DbObject(tblName, Kind.TABLE, catalog, schema);
                        String loc = dbName + "." + schema + "." + tblName + " [" + profile.getName() + "]";
                        String uri = "DATABASE/" + profile.getId() + "/database/" + dbName + "/schema/" + schema + "/table/" + tblName + "/column/" + colName;

                        results.add(new SearchHit.Builder()
                                .type(SearchHit.Type.COLUMN)
                                .name(colName)
                                .detail(loc)
                                .fullUri(uri)
                                .profile(profile)
                                .dbObject(tblObj)
                                .parentTable(tblName)
                                .columnName(colName)
                                .primaryKey(isPk)
                                .build());
                    }
                }
            } catch (SQLException ignored) {}

            // Indexes & PKs
            try (PreparedStatement stmt = conn.prepareStatement(idxSql)) {
                stmt.setInt(1, limit);
                stmt.setString(2, buildSqlPattern(query));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String idxName = rs.getString(1);
                        String tblName = rs.getString(2);
                        String schema = rs.getString(3);
                        boolean isUnique = rs.getBoolean(4);
                        boolean isPk = rs.getBoolean(5);

                        DbObject idxObj = new DbObject(idxName, isPk ? Kind.KEY : Kind.INDEX, catalog, schema);
                        idxObj.setTableName(tblName);

                        String loc = dbName + "." + schema + "." + tblName + " [" + profile.getName() + "]";
                        String uri = "DATABASE/" + profile.getId() + "/database/" + dbName + "/schema/" + schema + "/table/" + tblName + "/index/" + idxName;

                        results.add(new SearchHit.Builder()
                                .type(isPk ? SearchHit.Type.PRIMARY_KEY : SearchHit.Type.INDEX)
                                .name(idxName)
                                .detail(loc)
                                .fullUri(uri)
                                .profile(profile)
                                .dbObject(idxObj)
                                .parentTable(tblName)
                                .primaryKey(isPk)
                                .uniqueIndex(isUnique)
                                .build());
                    }
                }
            } catch (SQLException ignored) {}
        } catch (SQLException e) {
            if (isConnectionFailure(e)) {
                markProfileUnreachable(profile.getId());
            }
        }
    }

    private static void searchSqlite(
            ConnectionProfile profile,
            String catalog,
            String query,
            List<SearchHit> results,
            int limit
    ) {
        String sql = "SELECT name, type, tbl_name FROM sqlite_master WHERE (name LIKE ? OR tbl_name LIKE ?) AND name NOT LIKE 'sqlite_%' LIMIT ?";
        try (Connection conn = ClientRegistry.jdbc(profile, catalog).getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, buildSqlPattern(query));
            stmt.setString(2, buildSqlPattern(query));
            stmt.setInt(3, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString(1);
                    String type = rs.getString(2);
                    String tblName = rs.getString(3);

                    if ("table".equalsIgnoreCase(type) || "view".equalsIgnoreCase(type)) {
                        boolean isView = "view".equalsIgnoreCase(type);
                        DbObject obj = new DbObject(name, isView ? Kind.VIEW : Kind.TABLE, null, null);
                        String loc = profile.getName() + " [" + profile.getDatabase() + "]";
                        String uri = "DATABASE/" + profile.getId() + "/table/" + name;

                        results.add(new SearchHit.Builder()
                                .type(isView ? SearchHit.Type.VIEW : SearchHit.Type.TABLE)
                                .name(name)
                                .detail(loc)
                                .fullUri(uri)
                                .profile(profile)
                                .dbObject(obj)
                                .parentTable(name)
                                .build());
                    } else if ("index".equalsIgnoreCase(type)) {
                        DbObject idxObj = new DbObject(name, Kind.INDEX, null, null);
                        idxObj.setTableName(tblName);
                        String loc = tblName + " [" + profile.getName() + "]";
                        String uri = "DATABASE/" + profile.getId() + "/table/" + tblName + "/index/" + name;

                        results.add(new SearchHit.Builder()
                                .type(SearchHit.Type.INDEX)
                                .name(name)
                                .detail(loc)
                                .fullUri(uri)
                                .profile(profile)
                                .dbObject(idxObj)
                                .parentTable(tblName)
                                .build());
                    }
                }
            }
        } catch (SQLException ignored) {}
    }

    private static void searchGenericJdbc(
            ConnectionProfile profile,
            String catalog,
            String query,
            List<SearchHit> results,
            int limit
    ) {
        try (Connection conn = ClientRegistry.jdbc(profile, catalog).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(catalog, null, "%" + query + "%", new String[]{"TABLE", "VIEW"})) {
                while (rs.next() && results.size() < limit) {
                    String tblName = rs.getString("TABLE_NAME");
                    String schName = rs.getString("TABLE_SCHEM");
                    String catName = rs.getString("TABLE_CAT");
                    String type = rs.getString("TABLE_TYPE");
                    boolean isView = "VIEW".equalsIgnoreCase(type);

                    DbObject obj = new DbObject(tblName, isView ? Kind.VIEW : Kind.TABLE, catName != null ? catName : catalog, schName);
                    String loc = (catName != null ? catName + "." : "") + (schName != null ? schName + "." : "") + tblName + " [" + profile.getName() + "]";
                    String uri = "DATABASE/" + profile.getId() + "/table/" + tblName;

                    results.add(new SearchHit.Builder()
                            .type(isView ? SearchHit.Type.VIEW : SearchHit.Type.TABLE)
                            .name(tblName)
                            .detail(loc)
                            .fullUri(uri)
                            .profile(profile)
                            .dbObject(obj)
                            .parentTable(tblName)
                            .build());
                }
            }
        } catch (Exception e) {
            if (isConnectionFailure(e)) {
                markProfileUnreachable(profile.getId());
            }
        }
    }

    private static void searchMongo(
            ConnectionProfile profile,
            String query,
            List<SearchHit> results,
            int limit
    ) {
        try {
            var client = ClientRegistry.mongo(profile);
            List<String> visible = profile.getVisibleDatabases();
            for (String db : client.listDatabases()) {
                if (visible != null && !visible.isEmpty() && !visible.contains(db)) {
                    continue;
                }
                if (db.toLowerCase().contains(query.toLowerCase())) {
                    String loc = profile.getName();
                    String uri = "DATABASE/" + profile.getId() + "/database/" + db;
                    results.add(new SearchHit.Builder()
                            .type(SearchHit.Type.TABLE)
                            .name(db)
                            .detail(loc)
                            .fullUri(uri)
                            .profile(profile)
                            .build());
                }
                for (String coll : client.listCollections(db)) {
                    if (coll.toLowerCase().contains(query.toLowerCase())) {
                        DbObject obj = new DbObject(coll, Kind.COLLECTION, db, null);
                        String loc = db + " [" + profile.getName() + "]";
                        String uri = "DATABASE/" + profile.getId() + "/database/" + db + "/collection/" + coll;
                        results.add(new SearchHit.Builder()
                                .type(SearchHit.Type.TABLE)
                                .name(coll)
                                .detail(loc)
                                .fullUri(uri)
                                .profile(profile)
                                .dbObject(obj)
                                .parentTable(coll)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            if (isConnectionFailure(e)) {
                markProfileUnreachable(profile.getId());
            }
        }
    }

    private static void searchConsolesAndFiles(
            String query,
            MainWindow mainWindow,
            AtomicInteger activeGen,
            int currentGen,
            List<SearchHit> results,
            int limit
    ) {
        String q = query.toLowerCase();

        // Open tabs
        if (mainWindow != null) {
            for (TabPane pane : mainWindow.editorTabPanes()) {
                for (Tab tab : pane.getTabs()) {
                    if (isCancelled(activeGen, currentGen)) return;
                    String title = tab.getText();
                    if (title != null && title.toLowerCase().contains(q)) {
                        String detail = "Open Tabs";
                        ConnectionProfile prof = null;
                        if (tab instanceof QueryTab qt) {
                            prof = qt.getProfile();
                            detail = "Database Consoles/" + (prof != null ? prof.getName() : "console");
                        }
                        String uri = "TAB/" + title;
                        results.add(new SearchHit.Builder()
                                .type(SearchHit.Type.CONSOLE)
                                .name(title)
                                .detail(detail)
                                .fullUri(uri)
                                .profile(prof)
                                .build());
                    }
                }
            }
        }

        // Project SQL Files
        try {
            var proj = ProjectStore.getCurrentProject();
            if (proj != null && proj.getPath() != null) {
                Path root = Path.of(proj.getPath());
                if (Files.exists(root)) {
                    try (Stream<Path> stream = Files.walk(root, 4)) {
                        stream.filter(Files::isRegularFile)
                                .filter(p -> p.getFileName().toString().toLowerCase().contains(q))
                                .limit(20)
                                .forEach(p -> {
                                    String fname = p.getFileName().toString();
                                    String rel = root.relativize(p).toString();
                                    results.add(new SearchHit.Builder()
                                            .type(SearchHit.Type.FILE)
                                            .name(fname)
                                            .detail(rel)
                                            .fullUri("FILE/" + p.toAbsolutePath())
                                            .file(p.toFile())
                                            .build());
                                });
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static void searchActions(
            String query,
            SearchHit.Category category,
            AtomicInteger activeGen,
            int currentGen,
            List<SearchHit> results,
            int limit
    ) {
        String q = query.toLowerCase().trim();
        Map<String, AnAction> actionMap = ActionManager.getInstance().getActionMap();

        Set<String> seenNames = new HashSet<>();
        int actionCount = 0;
        int maxActions = (category == SearchHit.Category.ALL) ? 4 : limit;

        for (Map.Entry<String, AnAction> entry : actionMap.entrySet()) {
            if (isCancelled(activeGen, currentGen)) return;
            AnAction action = entry.getValue();
            if (action == null) continue;

            String text = action.getText();
            String id = action.getId();

            // Ignore technical, empty, or internal handler IDs
            if (text == null || text.isBlank() || text.length() < 2) continue;
            if (id != null && (id.contains(".LangGroup") || id.startsWith("ChangesView."))) continue;
            if (text.startsWith("ChangesView.") || text.contains(".LangGroup")) continue;

            // Deduplicate actions by clean display text
            String normalizedText = text.trim();
            if (seenNames.contains(normalizedText)) continue;

            // Only match if a word starts with query, or entire text starts with query
            if (!matchesActionQuery(normalizedText, q)) continue;

            seenNames.add(normalizedText);

            String location = "Actions";
            if (id != null) {
                if (id.startsWith("file.")) location = "File";
                else if (id.startsWith("edit.")) location = "Edit";
                else if (id.startsWith("view.")) location = "View";
                else if (id.startsWith("database.")) location = "Database";
                else if (id.startsWith("tools.")) location = "Tools";
                else if (id.startsWith("window.")) location = "Window";
                else if (id.startsWith("help.")) location = "Help";
                else if (id.contains("setting") || id.contains("pref")) location = "Settings";
            }
            if (normalizedText.contains("Setting") || normalizedText.contains("Plugin") || normalizedText.contains("Path Variable")) {
                location = "Settings > " + (normalizedText.contains("Path Variable") ? "Path Variables" : "Preferences");
            }

            results.add(new SearchHit.Builder()
                    .type(SearchHit.Type.ACTION)
                    .name(normalizedText)
                    .detail(location)
                    .fullUri("ACTION/" + (id != null ? id : normalizedText))
                    .action(action)
                    .build());

            actionCount++;
            if (actionCount >= maxActions) break;
        }
    }

    private static boolean matchesActionQuery(String text, String query) {
        String t = text.toLowerCase();
        if (t.startsWith(query)) return true;

        // Word boundary match: any word starts with query (avoids matching middle of words like 'wrapper' or 'mapping')
        String[] words = t.split("[\\s._\\->/]+");
        for (String w : words) {
            if (w.startsWith(query)) return true;
        }

        return false;
    }

    private static void searchOpenTabsText(
            String query,
            MainWindow mainWindow,
            AtomicInteger activeGen,
            int currentGen,
            List<SearchHit> results,
            int limit
    ) {
        if (mainWindow == null) return;
        String q = query.toLowerCase();

        for (TabPane pane : mainWindow.editorTabPanes()) {
            for (Tab tab : pane.getTabs()) {
                if (isCancelled(activeGen, currentGen)) return;
                if (tab instanceof QueryTab qt) {
                    String sql = qt.getSqlText();
                    if (sql != null && !sql.isBlank() && sql.toLowerCase().contains(q)) {
                        String[] lines = sql.split("\n");
                        for (int i = 0; i < lines.length; i++) {
                            if (lines[i].toLowerCase().contains(q)) {
                                String snippet = lines[i].trim();
                                if (snippet.length() > 60) {
                                    snippet = snippet.substring(0, 60) + "…";
                                }
                                String detail = "Database Consoles/" + (qt.getProfile() != null ? qt.getProfile().getName() : "") + "/" + qt.getText() + " " + (i + 1);
                                String uri = "TEXT/" + qt.getText() + ":" + (i + 1);

                                results.add(new SearchHit.Builder()
                                        .type(SearchHit.Type.TEXT)
                                        .name(snippet)
                                        .detail(detail)
                                        .fullUri(uri)
                                        .profile(qt.getProfile())
                                        .lineSnippet(snippet)
                                        .lineNumber(i + 1)
                                        .build());
                                break; // top 1 hit per console
                            }
                        }
                    }
                }
            }
        }
    }
}
