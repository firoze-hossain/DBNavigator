package com.roze.dbnavigator.db;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.DbObject.Kind;
import com.roze.dbnavigator.model.QueryResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

/** Loads schema tree children lazily for both JDBC and MongoDB connections. */
public final class MetadataService {

    private MetadataService() {}

    private static final Set<String> PG_SYSTEM_SCHEMAS =
            Set.of("pg_catalog", "information_schema", "pg_toast");

    // SQL Server creates these fixed database-role schemas in every database
    // by default; they're almost never where a user's own objects live, and
    // Reference IDE tucks the equivalent noise away under its own "Database
    // Objects" node rather than listing it alongside real schemas like dbo.
    private static final Set<String> SQLSERVER_SYSTEM_SCHEMAS = Set.of(
            "db_owner", "db_accessadmin", "db_securityadmin", "db_ddladmin",
            "db_backupoperator", "db_datareader", "db_datawriter",
            "db_denydatareader", "db_denydatawriter", "sys", "guest",
            "information_schema");

    // Built-in Oracle component schemas and administrative schemas
    public static final Set<String> ORACLE_SYSTEM_SCHEMAS = Set.of(
            "anonymous", "appqossys", "audsys", "ctxsys", "dbsfwuser", "dbsnmp",
            "dip", "dvf", "dvsys", "ggsys", "gsmadmin_internal", "gsmcatuser",
            "gsmuser", "lbacsys", "mddata", "mdsys", "ojvmsys", "olapsys",
            "oracle_ocm", "orddata", "ordplugins", "ordsys", "outln", "pdbadmin",
            "remote_scheduler_agent", "si_informtn_schema", "sys", "sys$umf",
            "sysbackup", "sysdg", "syskm", "sysrac", "system", "wmsys", "xdb", "xs$null");

    public static boolean isOracleSystemSchema(String schema) {
        if (schema == null) return false;
        return ORACLE_SYSTEM_SCHEMAS.contains(schema.toLowerCase(Locale.ROOT));
    }

    private static JdbcClient client(ConnectionProfile profile, String catalog) {
        return ClientRegistry.jdbc(profile, catalog);
    }

    /** PostgreSQL's JDBC metadata calls ignore/reject foreign catalogs — pass null. */
    private static String metaCatalog(ConnectionProfile profile, String catalog) {
        // Oracle's JDBC driver never populates TABLE_CAT at all (it has no
        // catalog concept) — passing a real value here as a catalog filter
        // would match nothing. What looks like "catalog" for an Oracle
        // console scoped to one schema is actually a schema name; see
        // metaSchema below for where that value actually belongs.
        return profile.getType() == DatabaseType.POSTGRESQL
                || profile.getType() == DatabaseType.ORACLE ? null : catalog;
    }

    /**
     * The schema-filter argument for Oracle's metadata calls, when a console
     * is scoped to one schema (catalog here is actually that schema's name —
     * see JdbcClient's use of Connection.setSchema() for the same value).
     * Every other engine keeps filtering by catalog/database instead, via
     * the connection itself already being scoped to the right one, so this
     * stays null for them — an explicit schema filter would only narrow
     * results they don't actually want narrowed.
     */
    private static String metaSchema(ConnectionProfile profile, String catalog) {
        return profile.getType() == DatabaseType.ORACLE ? catalog : null;
    }

    /** Names of every database on the server — used by the show/hide dialog. */
    public static List<String> listDatabaseNames(ConnectionProfile profile) throws Exception {
        return switch (profile.getType()) {
            case MONGODB -> ClientRegistry.mongo(profile).listDatabases();
            case POSTGRESQL -> {
                List<String> names = new ArrayList<>();
                try (Connection conn = client(profile, null).getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(
                             "SELECT datname FROM pg_database " +
                             "WHERE datistemplate = false AND datallowconn ORDER BY datname")) {
                    while (rs.next()) names.add(rs.getString(1));
                }
                yield names;
            }
            case MYSQL, MARIADB -> {
                List<String> names = new ArrayList<>();
                try (Connection conn = client(profile, null).getConnection();
                     ResultSet rs = conn.getMetaData().getCatalogs()) {
                    while (rs.next()) names.add(rs.getString("TABLE_CAT"));
                }
                yield names;
            }
            case SQLSERVER -> {
                List<String> names = new ArrayList<>();
                try (Connection conn = client(profile, null).getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(
                             "SELECT name FROM sys.databases ORDER BY name")) {
                    while (rs.next()) names.add(rs.getString(1));
                }
                yield names;
            }
            // Oracle has no per-connection "database" the way the engines
            // above do — one instance is one database. Its real organizing
            // unit, and the thing this filter is actually useful for, is the
            // schema (1:1 with a user) — see loadTopLevel below, which
            // already lists every (non-noise) schema as Oracle's top-level
            // nodes; this reuses that exact same list for consistency.
            case ORACLE -> {
                List<DbObject> schemas;
                try (Connection conn = client(profile, null).getConnection()) {
                    schemas = loadSchemas(conn, profile, null);
                }
                yield schemas.stream().map(DbObject::getName).toList();
            }
            // Real, but conditional: SHOW DATABASES genuinely lists every
            // database only when this server is a real, multi-database
            // StratosCluster - a plain, single-database instance (this
            // engine's own real, established default) honestly returns an
            // empty result for it instead (see StratosDB's own
            // ExecutorEngine.executeShowDatabases javadoc), which would
            // otherwise leave this filter with nothing to show at all even
            // though a real connection - to exactly one, real database - is
            // sitting right there. Falls back to that one, real database's
            // own name in that case, matching what the connection is
            // actually, already using.
            case STRATOSDB -> {
                List<String> names = queryStratosDatabases(profile);
                yield names.isEmpty() ? List.of(stratosCurrentDatabaseName(profile)) : names;
            }
            default -> List.of();
        };
    }

    /** True when this engine shows DATABASE nodes that can be filtered. */
    public static boolean supportsDatabaseFilter(ConnectionProfile profile) {
        return switch (profile.getType()) {
            case POSTGRESQL, MYSQL, MARIADB, MONGODB, SQLSERVER, ORACLE, STRATOSDB -> true;
            default -> false;
        };
    }

    // ------------------------------------------------------------ top level

    /** Children of the connection root node. */
    public static List<DbObject> loadTopLevel(ConnectionProfile profile) throws Exception {
        if (profile.getType() == DatabaseType.MONGODB) {
            List<DbObject> dbs = new ArrayList<>();
            for (String name : ClientRegistry.mongo(profile).listDatabases()) {
                dbs.add(new DbObject(name, Kind.DATABASE, name, null));
            }
            return dbs;
        }

        return switch (profile.getType()) {
            // PostgreSQL: list all databases on the server
            case POSTGRESQL -> loadPostgresDatabases(profile);
            case MYSQL, MARIADB -> loadCatalogs(profile);
            // SQL Server: one login can see every database on the instance,
            // and each has its own set of schemas, so it gets its own DATABASE level
            // in the tree too (master, tempdb, etc. each as their own node).
            case SQLSERVER -> loadSqlServerDatabases(profile);
            case ORACLE -> {
                try (Connection conn = client(profile, null).getConnection()) {
                    yield loadSchemas(conn, profile, null);
                }
            }
            case SQLITE -> List.of(new DbObject("main", Kind.SCHEMA, null, "main"));
            // Real, but conditional, just like the database filter above:
            // a real, multi-database StratosCluster gets real, individually
            // expandable DATABASE nodes (PostgreSQL's own pattern) - a plain,
            // single-database instance (this engine's own real, established
            // default) falls back to going straight to Tables/Views/etc.
            // under the connection node (SQLite's own pattern), since SHOW
            // DATABASES on a plain instance honestly returns nothing to
            // build real database nodes from at all.
            case STRATOSDB -> loadStratosDatabases(profile);
            default -> List.of();
        };
    }

    private static List<DbObject> loadSqlServerDatabases(ConnectionProfile profile) throws SQLException {
        List<DbObject> result = new ArrayList<>();
        try (Connection conn = client(profile, null).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sys.databases ORDER BY name")) {
            while (rs.next()) {
                String db = rs.getString(1);
                DbObject obj = new DbObject(db, Kind.DATABASE, db, null);
                if (db.equals(profile.getDatabase())) obj.setDetail("(default)");
                result.add(obj);
            }
        }
        return result;
    }

    private static List<DbObject> loadPostgresDatabases(ConnectionProfile profile) throws SQLException {
        List<DbObject> result = new ArrayList<>();
        try (Connection conn = client(profile, null).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT datname FROM pg_database " +
                     "WHERE datistemplate = false AND datallowconn ORDER BY datname")) {
            while (rs.next()) {
                String db = rs.getString(1);
                DbObject obj = new DbObject(db, Kind.DATABASE, db, null);
                obj.setDetail("1 of 3");
                result.add(obj);
            }
        }
        return result;
    }

    /**
     * Loads cluster-level Server Objects container: roles, tablespaces, collations, linked servers, server principals.
     */
    public static List<DbObject> loadServerObjects(ConnectionProfile profile) throws Exception {
        if (profile.getType() == DatabaseType.SQLSERVER) {
            return loadSqlServerServerObjects(profile);
        }
        if (profile.getType() == DatabaseType.ORACLE) {
            return loadOracleServerObjects(profile);
        }
        if (profile.getType() == DatabaseType.MYSQL || profile.getType() == DatabaseType.MARIADB) {
            return loadMysqlServerObjects(profile);
        }
        int rolesCount = 0;
        int tablespacesCount = 0;
        try (Connection conn = client(profile, null).getConnection();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM pg_roles")) {
                if (rs.next()) rolesCount = rs.getInt(1);
            }
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM pg_tablespace")) {
                if (rs.next()) tablespacesCount = rs.getInt(1);
            }
        }
        DbObject rolesFolder = new DbObject("roles", Kind.ROLES_FOLDER);
        rolesFolder.setDetail(String.valueOf(rolesCount));

        DbObject tablespacesFolder = new DbObject("tablespaces", Kind.TABLESPACES_FOLDER);
        tablespacesFolder.setDetail(String.valueOf(tablespacesCount));

        return List.of(rolesFolder, tablespacesFolder);
    }

    private static List<DbObject> loadMysqlServerObjects(ConnectionProfile profile) throws Exception {
        int collationsCount = 0;
        int usersCount = 0;

        try (Connection conn = client(profile, null).getConnection();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM information_schema.COLLATIONS")) {
                if (rs.next()) collationsCount = rs.getInt(1);
            } catch (Exception ignored) {}

            String userCountSql = (profile.getType() == DatabaseType.MARIADB)
                    ? "SELECT count(*) FROM mysql.user WHERE is_role != 'Y'"
                    : "SELECT count(*) FROM mysql.user";
            try (ResultSet rs = stmt.executeQuery(userCountSql)) {
                if (rs.next()) usersCount = rs.getInt(1);
            } catch (Exception ex) {
                if (profile.getType() == DatabaseType.MARIADB) {
                    try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM mysql.user")) {
                        if (rs.next()) usersCount = rs.getInt(1);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}

        List<DbObject> result = new ArrayList<>();
        if (collationsCount > 0) {
            DbObject collations = new DbObject("collations", Kind.COLLATIONS_FOLDER);
            collations.setDetail(String.valueOf(collationsCount));
            result.add(collations);
        }
        if (usersCount > 0) {
            DbObject usersFolder = new DbObject("users", Kind.USERS_FOLDER);
            usersFolder.setDetail(String.valueOf(usersCount));
            result.add(usersFolder);
        }
        return result;
    }

    private static List<DbObject> loadOracleServerObjects(ConnectionProfile profile) throws Exception {
        int tablespacesCount = 0;
        int usersCount = 0;

        try (Connection conn = client(profile, null).getConnection();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM dba_tablespaces")) {
                if (rs.next()) tablespacesCount = rs.getInt(1);
            } catch (Exception e1) {
                try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM all_tablespaces")) {
                    if (rs.next()) tablespacesCount = rs.getInt(1);
                } catch (Exception e2) {
                    try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM v$tablespace")) {
                        if (rs.next()) tablespacesCount = rs.getInt(1);
                    } catch (Exception ignored) {}
                }
            }

            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM all_users")) {
                if (rs.next()) usersCount = rs.getInt(1);
            } catch (Exception e1) {
                try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM dba_users")) {
                    if (rs.next()) usersCount = rs.getInt(1);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        List<DbObject> result = new ArrayList<>();
        if (tablespacesCount > 0) {
            DbObject tsFolder = new DbObject("tablespaces", Kind.TABLESPACES_FOLDER);
            tsFolder.setDetail(String.valueOf(tablespacesCount));
            result.add(tsFolder);
        }
        if (usersCount > 0) {
            DbObject usersFolder = new DbObject("users", Kind.USERS_FOLDER);
            usersFolder.setDetail(String.valueOf(usersCount));
            result.add(usersFolder);
        }
        return result;
    }

    private static List<DbObject> loadSqlServerServerObjects(ConnectionProfile profile) throws Exception {
        int collationsCount = 0;
        int linkedServersCount = 0;
        int principalsCount = 0;

        try (Connection conn = client(profile, null).getConnection();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM sys.fn_helpcollations()")) {
                if (rs.next()) collationsCount = rs.getInt(1);
            } catch (Exception ignored) {}
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM sys.servers")) {
                if (rs.next()) linkedServersCount = rs.getInt(1);
            } catch (Exception ignored) {}
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM sys.server_principals")) {
                if (rs.next()) principalsCount = rs.getInt(1);
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}

        List<DbObject> result = new ArrayList<>();
        if (collationsCount > 0) {
            DbObject collations = new DbObject("collations", Kind.COLLATIONS_FOLDER);
            collations.setDetail(String.valueOf(collationsCount));
            result.add(collations);
        }
        if (linkedServersCount > 0) {
            DbObject linkedServers = new DbObject("linked servers", Kind.LINKED_SERVERS_FOLDER);
            linkedServers.setDetail(String.valueOf(linkedServersCount));
            result.add(linkedServers);
        }
        if (principalsCount > 0) {
            DbObject serverPrincipals = new DbObject("server principals", Kind.SERVER_PRINCIPALS_FOLDER);
            serverPrincipals.setDetail(String.valueOf(principalsCount));
            result.add(serverPrincipals);
        }
        return result;
    }

    public static List<DbObject> loadCollations(ConnectionProfile profile) throws Exception {
        List<DbObject> list = new ArrayList<>();
        if (profile.getType() == DatabaseType.MYSQL || profile.getType() == DatabaseType.MARIADB) {
            try (Connection conn = client(profile, null).getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT COLLATION_NAME FROM information_schema.COLLATIONS ORDER BY COLLATION_NAME")) {
                while (rs.next()) {
                    list.add(new DbObject(rs.getString("COLLATION_NAME"), Kind.COLLATION));
                }
            }
            return list;
        }
        try (Connection conn = client(profile, null).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sys.fn_helpcollations() ORDER BY name")) {
            while (rs.next()) {
                list.add(new DbObject(rs.getString("name"), Kind.COLLATION));
            }
        }
        return list;
    }

    public static List<DbObject> loadLinkedServers(ConnectionProfile profile) throws Exception {
        List<DbObject> list = new ArrayList<>();
        try (Connection conn = client(profile, null).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT name FROM sys.servers ORDER BY name")) {
            while (rs.next()) {
                list.add(new DbObject(rs.getString("name"), Kind.LINKED_SERVER));
            }
        }
        return list;
    }

    public static List<DbObject> loadServerPrincipals(ConnectionProfile profile) throws Exception {
        List<DbObject> list = new ArrayList<>();
        try (Connection conn = client(profile, null).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sys.server_principals ORDER BY name")) {
            while (rs.next()) {
                list.add(new DbObject(rs.getString("name"), Kind.SERVER_PRINCIPAL));
            }
        }
        return list;
    }

    /**
     * Queries PostgreSQL pg_roles to list cluster-level roles/users.
     */
    public static List<DbObject> loadRoles(ConnectionProfile profile) throws Exception {
        List<DbObject> list = new ArrayList<>();
        try (Connection conn = client(profile, null).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT rolname FROM pg_roles ORDER BY rolname")) {
            while (rs.next()) {
                String name = rs.getString(1);
                list.add(new DbObject(name, Kind.ROLE));
            }
        }
        return list;
    }

    /**
     * Queries PostgreSQL pg_tablespace to list cluster-level tablespaces.
     */
    public static List<DbObject> loadTablespaces(ConnectionProfile profile) throws Exception {
        if (profile.getType() == DatabaseType.ORACLE) {
            return loadOracleTablespaces(profile);
        }
        List<DbObject> list = new ArrayList<>();
        try (Connection conn = client(profile, null).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT spcname FROM pg_tablespace ORDER BY spcname")) {
            while (rs.next()) {
                String name = rs.getString(1);
                list.add(new DbObject(name, Kind.TABLESPACE));
            }
        }
        return list;
    }

    private static List<DbObject> loadOracleTablespaces(ConnectionProfile profile) throws Exception {
        List<DbObject> list = new ArrayList<>();
        try (Connection conn = client(profile, null).getConnection();
             Statement stmt = conn.createStatement()) {
            boolean loaded = false;
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT tablespace_name, contents, status, block_size, logging, force_logging " +
                    "FROM dba_tablespaces ORDER BY tablespace_name")) {
                while (rs.next()) {
                    String name = rs.getString(1);
                    String contents = rs.getString(2);
                    String status = rs.getString(3);
                    long blockSize = rs.getLong(4);
                    String logging = rs.getString(5);
                    String forceLogging = rs.getString(6);

                    DbObject ts = new DbObject(name, Kind.TABLESPACE);
                    ts.setDetail(formatOracleTablespaceDetail(contents, status, blockSize, logging, forceLogging));
                    list.add(ts);
                }
                loaded = true;
            } catch (Exception ignored) {}

            if (!loaded) {
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT tablespace_name, contents, status, block_size, logging, force_logging " +
                        "FROM all_tablespaces ORDER BY tablespace_name")) {
                    while (rs.next()) {
                        String name = rs.getString(1);
                        String contents = rs.getString(2);
                        String status = rs.getString(3);
                        long blockSize = rs.getLong(4);
                        String logging = rs.getString(5);
                        String forceLogging = rs.getString(6);

                        DbObject ts = new DbObject(name, Kind.TABLESPACE);
                        ts.setDetail(formatOracleTablespaceDetail(contents, status, blockSize, logging, forceLogging));
                        list.add(ts);
                    }
                    loaded = true;
                } catch (Exception ignored) {}
            }

            if (!loaded) {
                try (ResultSet rs = stmt.executeQuery("SELECT name FROM v$tablespace ORDER BY name")) {
                    while (rs.next()) {
                        String name = rs.getString(1);
                        list.add(new DbObject(name, Kind.TABLESPACE));
                    }
                } catch (Exception ignored) {}
            }
        }
        return list;
    }

    public static String formatOracleTablespaceDetail(String contents, String status,
                                                      long blockSize, String logging,
                                                      String forceLogging) {
        String contentsStr = (contents != null && !contents.isBlank()) ? contents.toLowerCase(Locale.ROOT) : "permanent";
        String statusStr = (status != null && !status.isBlank()) ? status.toLowerCase(Locale.ROOT) : "online";
        String blockSizeStr = blockSize > 0 ? "block size " + blockSize : "";
        String loggingStr;
        if ("YES".equalsIgnoreCase(forceLogging)) {
            loggingStr = "force logging";
        } else if ("NOLOGGING".equalsIgnoreCase(logging) || "NO".equalsIgnoreCase(logging)) {
            loggingStr = "no logging";
        } else {
            loggingStr = "logging";
        }
        List<String> parts = new ArrayList<>();
        if (!contentsStr.isEmpty()) parts.add(contentsStr);
        if (!statusStr.isEmpty()) parts.add(statusStr);
        if (!blockSizeStr.isEmpty()) parts.add(blockSizeStr);
        if (!loggingStr.isEmpty()) parts.add(loggingStr);
        return String.join(", ", parts);
    }

    public static List<DbObject> loadTablespaceDataFiles(ConnectionProfile profile, DbObject tablespaceObj) throws Exception {
        if (profile.getType() != DatabaseType.ORACLE || tablespaceObj == null) {
            return List.of();
        }
        List<DbObject> list = new ArrayList<>();
        String tsName = tablespaceObj.getName();
        try (Connection conn = client(profile, null).getConnection()) {
            boolean loaded = false;
            String sql = "SELECT file_name, bytes FROM dba_data_files WHERE tablespace_name = ? " +
                         "UNION ALL " +
                         "SELECT file_name, bytes FROM dba_temp_files WHERE tablespace_name = ? " +
                         "ORDER BY 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tsName);
                stmt.setString(2, tsName);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String fileName = rs.getString(1);
                        long bytes = rs.getLong(2);
                        DbObject df = new DbObject(fileName, Kind.DATA_FILE);
                        df.setDetail(formatFileSize(bytes));
                        list.add(df);
                    }
                    loaded = true;
                }
            } catch (Exception ignored) {}

            if (!loaded) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT file_name, bytes FROM dba_data_files WHERE tablespace_name = ? ORDER BY file_name")) {
                    stmt.setString(1, tsName);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            String fileName = rs.getString(1);
                            long bytes = rs.getLong(2);
                            DbObject df = new DbObject(fileName, Kind.DATA_FILE);
                            df.setDetail(formatFileSize(bytes));
                            list.add(df);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return list;
    }

    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        double kb = bytes / 1024.0;
        double mb = kb / 1024.0;
        double gb = mb / 1024.0;
        if (gb >= 1.0) {
            return String.format(Locale.US, "%.2f G", gb);
        }
        if (mb >= 1.0) {
            return String.format(Locale.US, "%.2f M", mb);
        }
        if (kb >= 1.0) {
            return String.format(Locale.US, "%.2f K", kb);
        }
        return bytes + " B";
    }

    public static List<DbObject> loadUsers(ConnectionProfile profile) throws Exception {
        if (profile.getType() == DatabaseType.MYSQL || profile.getType() == DatabaseType.MARIADB) {
            return loadMysqlUsers(profile);
        }
        if (profile.getType() != DatabaseType.ORACLE) {
            return List.of();
        }
        List<DbObject> list = new ArrayList<>();
        try (Connection conn = client(profile, null).getConnection();
             Statement stmt = conn.createStatement()) {
            boolean loaded = false;
            try (ResultSet rs = stmt.executeQuery("SELECT username FROM all_users ORDER BY username")) {
                while (rs.next()) {
                    String username = rs.getString(1);
                    list.add(new DbObject(username, Kind.USER));
                }
                loaded = true;
            } catch (Exception ignored) {}

            if (!loaded) {
                try (ResultSet rs = stmt.executeQuery("SELECT username FROM dba_users ORDER BY username")) {
                    while (rs.next()) {
                        String username = rs.getString(1);
                        list.add(new DbObject(username, Kind.USER));
                    }
                } catch (Exception ignored) {}
            }
        }
        return list;
    }

    private static List<DbObject> loadMysqlUsers(ConnectionProfile profile) throws Exception {
        List<DbObject> list = new ArrayList<>();
        String sql = (profile.getType() == DatabaseType.MARIADB)
                ? "SELECT User, Host FROM mysql.user WHERE is_role != 'Y' ORDER BY User, Host"
                : "SELECT User, Host FROM mysql.user ORDER BY User, Host";

        try (Connection conn = client(profile, null).getConnection();
             Statement stmt = conn.createStatement()) {
            boolean queried = false;
            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    String u = rs.getString(1);
                    String h = rs.getString(2);
                    if (u == null) u = "";
                    String displayName = u.isEmpty() ? "\"\"" : u;
                    DbObject obj = new DbObject(displayName, Kind.USER);
                    if (h != null && !h.isBlank()) {
                        obj.setDetail("@" + h);
                    }
                    list.add(obj);
                }
                queried = true;
            } catch (Exception ex) {
                if (profile.getType() == DatabaseType.MARIADB) {
                    try (ResultSet rs = stmt.executeQuery("SELECT User, Host FROM mysql.user ORDER BY User, Host")) {
                        while (rs.next()) {
                            String u = rs.getString(1);
                            String h = rs.getString(2);
                            if (u == null) u = "";
                            String displayName = u.isEmpty() ? "\"\"" : u;
                            DbObject obj = new DbObject(displayName, Kind.USER);
                            if (h != null && !h.isBlank()) {
                                obj.setDetail("@" + h);
                            }
                            list.add(obj);
                        }
                        queried = true;
                    } catch (Exception ignored) {}
                }
            }
        }
        return list;
    }

    /**
     * Loads database objects folder: access methods, casts, extensions, languages (PostgreSQL),
     * or assemblies, filegroups, database principals (SQL Server).
     */
    public static List<DbObject> loadDatabaseObjects(ConnectionProfile profile, DbObject dbObj) throws Exception {
        if (profile.getType() == DatabaseType.SQLSERVER) {
            return loadSqlServerDatabaseObjects(profile, dbObj);
        }
        String catalog = dbObj.getCatalog();
        int amCount = 0;
        int castCount = 0;
        int extCount = 0;
        int langCount = 0;

        try (Connection conn = client(profile, catalog).getConnection();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM pg_am")) {
                if (rs.next()) amCount = rs.getInt(1);
            } catch (Exception ignored) {}
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM pg_cast")) {
                if (rs.next()) castCount = rs.getInt(1);
            } catch (Exception ignored) {}
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM pg_extension")) {
                if (rs.next()) extCount = rs.getInt(1);
            } catch (Exception ignored) {}
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM pg_language")) {
                if (rs.next()) langCount = rs.getInt(1);
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}

        DbObject amFolder = new DbObject("access methods", Kind.ACCESS_METHODS_FOLDER, catalog, null);
        amFolder.setDetail(String.valueOf(amCount));

        DbObject castsFolder = new DbObject("casts", Kind.CASTS_FOLDER, catalog, null);
        castsFolder.setDetail(String.valueOf(castCount));

        DbObject extFolder = new DbObject("extensions", Kind.EXTENSIONS_FOLDER, catalog, null);
        extFolder.setDetail(String.valueOf(extCount));

        DbObject langFolder = new DbObject("languages", Kind.LANGUAGES_FOLDER, catalog, null);
        langFolder.setDetail(String.valueOf(langCount));

        return List.of(amFolder, castsFolder, extFolder, langFolder);
    }

    private static List<DbObject> loadSqlServerDatabaseObjects(ConnectionProfile profile, DbObject dbObj) throws Exception {
        String catalog = dbObj.getCatalog();
        int assembliesCount = 0;
        int filegroupsCount = 0;
        int principalsCount = 0;

        try (Connection conn = client(profile, catalog).getConnection();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM sys.assemblies")) {
                if (rs.next()) assembliesCount = rs.getInt(1);
            } catch (Exception ignored) {}
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM sys.filegroups")) {
                if (rs.next()) filegroupsCount = rs.getInt(1);
            } catch (Exception ignored) {}
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM sys.database_principals")) {
                if (rs.next()) principalsCount = rs.getInt(1);
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}

        List<DbObject> result = new ArrayList<>();
        if (assembliesCount > 0) {
            DbObject assemblies = new DbObject("assemblies", Kind.ASSEMBLIES_FOLDER, catalog, null);
            assemblies.setDetail(String.valueOf(assembliesCount));
            result.add(assemblies);
        }
        if (filegroupsCount > 0) {
            DbObject filegroups = new DbObject("filegroups", Kind.FILEGROUPS_FOLDER, catalog, null);
            filegroups.setDetail(String.valueOf(filegroupsCount));
            result.add(filegroups);
        }
        if (principalsCount > 0) {
            DbObject principals = new DbObject("database principals", Kind.DATABASE_PRINCIPALS_FOLDER, catalog, null);
            principals.setDetail(String.valueOf(principalsCount));
            result.add(principals);
        }
        return result;
    }

    public static List<DbObject> loadAssemblies(ConnectionProfile profile, DbObject folder) throws Exception {
        String catalog = folder.getCatalog();
        List<DbObject> list = new ArrayList<>();
        try (Connection conn = client(profile, catalog).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sys.assemblies ORDER BY name")) {
            while (rs.next()) {
                list.add(new DbObject(rs.getString("name"), Kind.ASSEMBLY, catalog, null));
            }
        }
        return list;
    }

    public static List<DbObject> loadFilegroups(ConnectionProfile profile, DbObject folder) throws Exception {
        String catalog = folder.getCatalog();
        List<DbObject> list = new ArrayList<>();
        try (Connection conn = client(profile, catalog).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sys.filegroups ORDER BY name")) {
            while (rs.next()) {
                list.add(new DbObject(rs.getString("name"), Kind.FILEGROUP, catalog, null));
            }
        }
        return list;
    }

    public static List<DbObject> loadDatabasePrincipals(ConnectionProfile profile, DbObject folder) throws Exception {
        String catalog = folder.getCatalog();
        List<DbObject> list = new ArrayList<>();
        try (Connection conn = client(profile, catalog).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sys.database_principals ORDER BY name")) {
            while (rs.next()) {
                list.add(new DbObject(rs.getString("name"), Kind.DATABASE_PRINCIPAL, catalog, null));
            }
        }
        return list;
    }

    /**
     * Loads PostgreSQL access methods (e.g. btree, hash, gist, gin, spgist, brin).
     */
    public static List<DbObject> loadAccessMethods(ConnectionProfile profile, DbObject folder) throws Exception {
        String catalog = folder.getCatalog();
        List<DbObject> list = new ArrayList<>();
        try (Connection conn = client(profile, catalog).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT amname FROM pg_am ORDER BY amname")) {
            while (rs.next()) {
                list.add(new DbObject(rs.getString("amname"), Kind.ACCESS_METHOD, catalog, null));
            }
        }
        return list;
    }

    /**
     * Loads PostgreSQL type casts (e.g. bit as int8).
     */
    public static List<DbObject> loadCasts(ConnectionProfile profile, DbObject folder) throws Exception {
        String catalog = folder.getCatalog();
        List<DbObject> list = new ArrayList<>();
        String sql = """
            SELECT t1.typname AS src, t2.typname AS tgt
            FROM pg_cast c
            JOIN pg_type t1 ON c.castsource = t1.oid
            JOIN pg_type t2 ON c.casttarget = t2.oid
            ORDER BY t1.typname, t2.typname
        """;
        try (Connection conn = client(profile, catalog).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String castName = rs.getString("src") + " as " + rs.getString("tgt");
                list.add(new DbObject(castName, Kind.CAST, catalog, null));
            }
        }
        return list;
    }

    /**
     * Loads PostgreSQL extensions installed in this database (e.g. plpgsql).
     */
    public static List<DbObject> loadExtensions(ConnectionProfile profile, DbObject folder) throws Exception {
        String catalog = folder.getCatalog();
        List<DbObject> list = new ArrayList<>();
        try (Connection conn = client(profile, catalog).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT extname FROM pg_extension ORDER BY extname")) {
            while (rs.next()) {
                list.add(new DbObject(rs.getString("extname"), Kind.EXTENSION, catalog, null));
            }
        }
        return list;
    }

    /**
     * Loads PostgreSQL languages installed in this database (e.g. c, internal, plpgsql, sql).
     */
    public static List<DbObject> loadLanguages(ConnectionProfile profile, DbObject folder) throws Exception {
        String catalog = folder.getCatalog();
        List<DbObject> list = new ArrayList<>();
        try (Connection conn = client(profile, catalog).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT lanname FROM pg_language ORDER BY lanname")) {
            while (rs.next()) {
                list.add(new DbObject(rs.getString("lanname"), Kind.LANGUAGE, catalog, null));
            }
        }
        return list;
    }

    /**
     * Loads virtual views for the database/server (e.g. sessions backed by pg_stat_activity in PostgreSQL
     * or master.dbo.sysprocesses in SQL Server).
     */
    public static List<DbObject> loadVirtualViews(ConnectionProfile profile, DbObject folder) {
        String catalog = folder != null ? folder.getCatalog() : null;
        DbObject sessions = new DbObject("sessions", Kind.VIRTUAL_VIEW, catalog, null);
        if (profile != null && profile.getType() == DatabaseType.SQLSERVER) {
            sessions.setQualifiedNameOverride(
                "(SELECT spid AS SPID, rtrim(status) AS Status, rtrim(loginame) AS Login, " +
                "CASE WHEN rtrim(hostname) = '' THEN '.' ELSE rtrim(hostname) END AS HostName, " +
                "CASE WHEN blocked = 0 THEN '.' ELSE cast(blocked as varchar(10)) END AS BlkBy, " +
                "db_name(dbid) AS DBName, rtrim(cmd) AS Command " +
                "FROM master.dbo.sysprocesses) AS sessions"
            );
        } else if (profile != null && profile.getType() == DatabaseType.ORACLE) {
            sessions.setQualifiedNameOverride("v$session");
        } else if (profile != null && profile.getType() == DatabaseType.MARIADB) {
            sessions.setQualifiedNameOverride(
                "(SELECT ID AS Id, USER AS User, HOST AS Host, DB AS db, COMMAND AS Command, " +
                "TIME AS Time, STATE AS State, INFO AS Info, PROGRESS AS Progress " +
                "FROM information_schema.PROCESSLIST) AS sessions"
            );
        } else if (profile != null && profile.getType() == DatabaseType.MYSQL) {
            sessions.setQualifiedNameOverride(
                "(SELECT ID AS Id, USER AS User, HOST AS Host, DB AS db, COMMAND AS Command, " +
                "TIME AS Time, STATE AS State, INFO AS Info " +
                "FROM information_schema.PROCESSLIST) AS sessions"
            );
        }
        return List.of(sessions);
    }



    /**
     * Real DATABASE nodes for a real, multi-database StratosCluster - the
     * same real shape PostgreSQL's own connection tree already has, since
     * StratosDB's own real cluster support genuinely is that same shape
     * (see StratosCluster's own javadoc). Falls back to the plain,
     * single-database tree shape ({@code objectFolders}, SQLite's own real
     * pattern) when {@code SHOW DATABASES} honestly returns nothing - a
     * plain, non-clustered StratosDB instance, this engine's own real,
     * established default.
     */
    private static List<DbObject> loadStratosDatabases(ConnectionProfile profile) throws SQLException {
        List<String> names = queryStratosDatabases(profile);
        if (names.isEmpty()) {
            return objectFolders(null, null);
        }
        List<DbObject> result = new ArrayList<>();
        for (String name : names) {
            DbObject obj = new DbObject(name, Kind.DATABASE, name, null);
            if (name.equals(stratosCurrentDatabaseName(profile))) obj.setDetail("(default)");
            result.add(obj);
        }
        return result;
    }

    private static List<String> queryStratosDatabases(ConnectionProfile profile) throws SQLException {
        List<String> names = new ArrayList<>();
        try (Connection conn = client(profile, null).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW DATABASES")) {
            while (rs.next()) names.add(rs.getString(1));
        }
        return names;
    }

    /** StratosDB's own real default database name when none is explicitly configured - matching StratosDriver's own real, established convention (see its javadoc). */
    private static String stratosCurrentDatabaseName(ConnectionProfile profile) {
        String configured = profile.getDatabase();
        return configured == null || configured.isBlank() ? "stratos" : configured;
    }

    private static List<DbObject> loadCatalogs(ConnectionProfile profile) throws SQLException {
        List<DbObject> result = new ArrayList<>();
        try (Connection conn = client(profile, null).getConnection();
             ResultSet rs = conn.getMetaData().getCatalogs()) {
            while (rs.next()) {
                String catalog = rs.getString("TABLE_CAT");
                result.add(new DbObject(catalog, Kind.DATABASE, catalog, null));
            }
        }
        return result;
    }

    private static List<DbObject> loadSchemas(Connection conn, ConnectionProfile profile,
                                              String catalog) throws SQLException {
        List<DbObject> result = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getSchemas()) {
            while (rs.next()) {
                String schema = rs.getString("TABLE_SCHEM");
                if (profile.getType() == DatabaseType.POSTGRESQL
                        && PG_SYSTEM_SCHEMAS.contains(schema)) continue;
                if (profile.getType() == DatabaseType.SQLSERVER
                        && SQLSERVER_SYSTEM_SCHEMAS.contains(schema.toLowerCase(Locale.ROOT))) continue;
                result.add(new DbObject(schema, Kind.SCHEMA, catalog, schema));
            }
        }
        return result;
    }

    // -------------------------------------------------------- intermediate

    /** Children of a DATABASE node. */
    public static List<DbObject> loadDatabaseChildren(ConnectionProfile profile, DbObject database)
            throws Exception {
        return switch (profile.getType()) {
            case MONGODB -> {
                int collCount = 0;
                try {
                    collCount = ClientRegistry.mongo(profile).listCollections(database.getCatalog()).size();
                } catch (Exception ignored) {}
                DbObject collFolder = folder("collections", Kind.COLLECTIONS_FOLDER, database.getCatalog(), null);
                collFolder.setDetail(String.valueOf(collCount));
                yield List.of(collFolder);
            }
            case POSTGRESQL -> {
                List<DbObject> items = new ArrayList<>();
                try (Connection conn = client(profile, database.getCatalog()).getConnection()) {
                    items.addAll(loadSchemas(conn, profile, database.getCatalog()));
                }
                items.add(new DbObject("Database Objects", Kind.DATABASE_OBJECTS, database.getCatalog(), null));
                DbObject vv = new DbObject("virtual views", Kind.VIRTUAL_VIEWS_FOLDER, database.getCatalog(), null);
                vv.setDetail("1");
                items.add(vv);
                yield items;
            }
            case SQLSERVER -> {
                // schemas of that particular database (separate physical connection,
                // via databaseName=/dbname in the JDBC URL — see ConnectionProfile.getJdbcUrl)
                List<DbObject> items = new ArrayList<>();
                try (Connection conn = client(profile, database.getCatalog()).getConnection()) {
                    items.addAll(loadSchemas(conn, profile, database.getCatalog()));
                }
                items.add(new DbObject("Database Objects", Kind.DATABASE_OBJECTS, database.getCatalog(), null));
                yield items;
            }
            case MYSQL, MARIADB -> loadMysqlSchemaChildren(profile, database.getCatalog());
            default -> loadJdbcDynamicFolders(profile, database.getCatalog(), null);
        };
    }

    /** Children of a SCHEMA node. */
    public static List<DbObject> loadSchemaChildren(DbObject schema) {
        return loadSchemaChildren(null, schema);
    }

    /** Children of a SCHEMA node with engine-specific filtering (dynamic folders, hides empty folders). */
    public static List<DbObject> loadSchemaChildren(ConnectionProfile profile, DbObject schema) {
        if (profile != null && profile.getType() == DatabaseType.POSTGRESQL) {
            return loadPostgresSchemaChildren(profile, schema);
        }
        if (profile != null && profile.getType() == DatabaseType.SQLITE) {
            return loadSqliteSchemaChildren(profile, schema);
        }
        if (profile != null && profile.getType() == DatabaseType.ORACLE) {
            return loadOracleSchemaChildren(profile, schema);
        }
        if (profile != null && profile.getType() == DatabaseType.SQLSERVER) {
            return loadSqlServerSchemaChildren(profile, schema);
        }
        if (profile != null && (profile.getType() == DatabaseType.MYSQL || profile.getType() == DatabaseType.MARIADB)) {
            return loadMysqlSchemaChildren(profile, schema.getCatalog() != null ? schema.getCatalog() : schema.getSchema());
        }
        return loadJdbcDynamicFolders(profile, schema.getCatalog(), schema.getSchema());
    }

    private static List<DbObject> loadMysqlSchemaChildren(ConnectionProfile profile, String catalog) {
        int tablesCount = 0;
        int viewsCount = 0;
        int procsCount = 0;
        int funcsCount = 0;
        int seqsCount = 0;

        try (Connection conn = client(profile, catalog).getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT TABLE_TYPE, COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? GROUP BY TABLE_TYPE")) {
                ps.setString(1, catalog);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String type = rs.getString(1);
                        int cnt = rs.getInt(2);
                        if ("VIEW".equalsIgnoreCase(type)) {
                            viewsCount += cnt;
                        } else if ("SEQUENCE".equalsIgnoreCase(type)) {
                            seqsCount += cnt;
                        } else {
                            tablesCount += cnt;
                        }
                    }
                }
            } catch (Exception ignored) {}

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT ROUTINE_TYPE, COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA = ? GROUP BY ROUTINE_TYPE")) {
                ps.setString(1, catalog);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String type = rs.getString(1);
                        int cnt = rs.getInt(2);
                        if ("FUNCTION".equalsIgnoreCase(type)) {
                            funcsCount += cnt;
                        } else {
                            procsCount += cnt;
                        }
                    }
                }
            } catch (Exception ignored) {}
        } catch (Exception ignored) {
            return loadJdbcDynamicFolders(profile, catalog, null);
        }

        List<DbObject> folders = new ArrayList<>();
        DbObject tablesFolder = folder("tables", Kind.TABLES_FOLDER, catalog, null);
        if (tablesCount > 0) {
            tablesFolder.setDetail(String.valueOf(tablesCount));
        }
        folders.add(tablesFolder);

        if (viewsCount > 0) {
            DbObject viewsFolder = folder("views", Kind.VIEWS_FOLDER, catalog, null);
            viewsFolder.setDetail(String.valueOf(viewsCount));
            folders.add(viewsFolder);
        }

        if (procsCount > 0) {
            DbObject procsFolder = folder("procedures", Kind.PROCEDURES_FOLDER, catalog, null);
            procsFolder.setDetail(String.valueOf(procsCount));
            folders.add(procsFolder);
        }

        if (funcsCount > 0) {
            DbObject funcsFolder = folder("functions", Kind.FUNCTIONS_FOLDER, catalog, null);
            funcsFolder.setDetail(String.valueOf(funcsCount));
            folders.add(funcsFolder);
        }

        if (seqsCount > 0) {
            DbObject seqsFolder = folder("sequences", Kind.SEQUENCES_FOLDER, catalog, null);
            seqsFolder.setDetail(String.valueOf(seqsCount));
            folders.add(seqsFolder);
        }

        return folders;
    }

    private static List<DbObject> loadSqlServerSchemaChildren(ConnectionProfile profile, DbObject schemaObj) {
        String catalog = schemaObj.getCatalog();
        String schemaName = schemaObj.getSchema() != null && !schemaObj.getSchema().isBlank()
                ? schemaObj.getSchema() : schemaObj.getName();

        int tablesCount = 0;
        int viewsCount = 0;
        int procsCount = 0;
        int funcsCount = 0;
        int seqsCount = 0;

        try (Connection conn = client(profile, catalog).getConnection()) {
            String sql = "SELECT o.type, COUNT(*) FROM sys.objects o "
                    + "JOIN sys.schemas s ON o.schema_id = s.schema_id "
                    + "WHERE s.name = ? GROUP BY o.type";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, schemaName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String type = rs.getString(1) != null ? rs.getString(1).trim() : "";
                        int cnt = rs.getInt(2);
                        if ("U".equalsIgnoreCase(type)) {
                            tablesCount += cnt;
                        } else if ("V".equalsIgnoreCase(type)) {
                            viewsCount += cnt;
                        } else if ("P".equalsIgnoreCase(type) || "PC".equalsIgnoreCase(type)) {
                            procsCount += cnt;
                        } else if ("FN".equalsIgnoreCase(type) || "IF".equalsIgnoreCase(type) || "TF".equalsIgnoreCase(type)) {
                            funcsCount += cnt;
                        } else if ("SO".equalsIgnoreCase(type)) {
                            seqsCount += cnt;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            return loadJdbcDynamicFolders(profile, catalog, schemaName);
        }

        List<DbObject> folders = new ArrayList<>();
        DbObject tablesFolder = folder("tables", Kind.TABLES_FOLDER, catalog, schemaName);
        if (tablesCount > 0) {
            tablesFolder.setDetail(String.valueOf(tablesCount));
        }
        folders.add(tablesFolder);

        if (viewsCount > 0) {
            DbObject viewsFolder = folder("views", Kind.VIEWS_FOLDER, catalog, schemaName);
            viewsFolder.setDetail(String.valueOf(viewsCount));
            folders.add(viewsFolder);
        }

        if (procsCount > 0) {
            DbObject procsFolder = folder("procedures", Kind.PROCEDURES_FOLDER, catalog, schemaName);
            procsFolder.setDetail(String.valueOf(procsCount));
            folders.add(procsFolder);
        }

        if (funcsCount > 0) {
            DbObject funcsFolder = folder("functions", Kind.FUNCTIONS_FOLDER, catalog, schemaName);
            funcsFolder.setDetail(String.valueOf(funcsCount));
            folders.add(funcsFolder);
        }

        if (seqsCount > 0) {
            DbObject seqsFolder = folder("sequences", Kind.SEQUENCES_FOLDER, catalog, schemaName);
            seqsFolder.setDetail(String.valueOf(seqsCount));
            folders.add(seqsFolder);
        }

        return folders;
    }

    private static List<DbObject> loadJdbcDynamicFolders(ConnectionProfile profile, String catalog, String schema) {
        int tablesCount = 0;
        int viewsCount = 0;
        int procsCount = 0;
        try (Connection conn = client(profile, catalog).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String cat = metaCatalog(profile, catalog);
            String sch = metaSchema(profile, schema);
            try (ResultSet rs = meta.getTables(cat, sch, "%", new String[]{"TABLE"})) {
                while (rs.next()) tablesCount++;
            } catch (Exception ignored) {}
            try (ResultSet rs = meta.getTables(cat, sch, "%", new String[]{"VIEW"})) {
                while (rs.next()) viewsCount++;
            } catch (Exception ignored) {}
            try (ResultSet rs = meta.getProcedures(cat, sch, "%")) {
                while (rs.next()) procsCount++;
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}

        List<DbObject> folders = new ArrayList<>();
        DbObject tablesFolder = folder("tables", Kind.TABLES_FOLDER, catalog, schema);
        if (tablesCount > 0) {
            tablesFolder.setDetail(String.valueOf(tablesCount));
        }
        folders.add(tablesFolder);

        if (viewsCount > 0) {
            DbObject viewsFolder = folder("views", Kind.VIEWS_FOLDER, catalog, schema);
            viewsFolder.setDetail(String.valueOf(viewsCount));
            folders.add(viewsFolder);
        }
        if (procsCount > 0) {
            DbObject procsFolder = folder("procedures", Kind.PROCEDURES_FOLDER, catalog, schema);
            procsFolder.setDetail(String.valueOf(procsCount));
            folders.add(procsFolder);
        }
        return folders;
    }

    private static List<DbObject> loadOracleSchemaChildren(ConnectionProfile profile, DbObject schema) {
        String owner = (schema.getSchema() != null && !schema.getSchema().isBlank())
                ? schema.getSchema().toUpperCase(Locale.ROOT)
                : schema.getName().toUpperCase(Locale.ROOT);

        int tablesCount = 0;
        int viewsCount = 0;
        int routinesCount = 0;
        int seqsCount = 0;

        try (Connection conn = client(profile, null).getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT object_type, count(*) FROM all_objects WHERE owner = ? "
                     + "AND object_type IN ('TABLE', 'VIEW', 'PROCEDURE', 'FUNCTION', 'PACKAGE', 'SEQUENCE') "
                     + "GROUP BY object_type")) {
            ps.setString(1, owner);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString(1);
                    int count = rs.getInt(2);
                    if ("TABLE".equalsIgnoreCase(type)) tablesCount += count;
                    else if ("VIEW".equalsIgnoreCase(type)) viewsCount += count;
                    else if ("SEQUENCE".equalsIgnoreCase(type)) seqsCount += count;
                    else routinesCount += count;
                }
            }
        } catch (Exception ignored) {
            return objectFolders(schema.getCatalog(), schema.getSchema());
        }

        List<DbObject> folders = new ArrayList<>();
        DbObject tablesFolder = folder("tables", Kind.TABLES_FOLDER, schema.getCatalog(), schema.getSchema());
        if (tablesCount > 0) {
            tablesFolder.setDetail(String.valueOf(tablesCount));
        }
        folders.add(tablesFolder);

        if (viewsCount > 0) {
            DbObject viewsFolder = folder("views", Kind.VIEWS_FOLDER, schema.getCatalog(), schema.getSchema());
            viewsFolder.setDetail(String.valueOf(viewsCount));
            folders.add(viewsFolder);
        }
        if (routinesCount > 0) {
            DbObject routinesFolder = folder("routines", Kind.PROCEDURES_FOLDER, schema.getCatalog(), schema.getSchema());
            routinesFolder.setDetail(String.valueOf(routinesCount));
            folders.add(routinesFolder);
        }
        if (seqsCount > 0) {
            DbObject seqsFolder = folder("sequences", Kind.SEQUENCES_FOLDER, schema.getCatalog(), schema.getSchema());
            seqsFolder.setDetail(String.valueOf(seqsCount));
            folders.add(seqsFolder);
        }

        return folders;
    }

    private static List<DbObject> loadSqliteSchemaChildren(ConnectionProfile profile, DbObject schema) {
        int tablesCount = 0;
        int viewsCount = 0;
        try (Connection conn = client(profile, null).getConnection();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'")) {
                if (rs.next()) tablesCount = rs.getInt(1);
            }
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT count(*) FROM sqlite_master WHERE type = 'view'")) {
                if (rs.next()) viewsCount = rs.getInt(1);
            }
        } catch (Exception ignored) {
            return objectFolders(null, schema.getSchema());
        }

        List<DbObject> folders = new ArrayList<>();
        DbObject tablesFolder = folder("tables", Kind.TABLES_FOLDER, null, schema.getSchema());
        if (tablesCount > 0) {
            tablesFolder.setDetail(String.valueOf(tablesCount));
        }
        folders.add(tablesFolder);

        if (viewsCount > 0) {
            DbObject viewsFolder = folder("views", Kind.VIEWS_FOLDER, null, schema.getSchema());
            viewsFolder.setDetail(String.valueOf(viewsCount));
            folders.add(viewsFolder);
        }
        return folders;
    }

    private static List<DbObject> loadPostgresSchemaChildren(ConnectionProfile profile, DbObject schema) {
        String catalog = (schema.getCatalog() != null && !schema.getCatalog().isBlank())
                ? schema.getCatalog() : profile.getDatabase();
        String schemaName = schema.getSchema();
        if (schemaName == null || schemaName.isBlank()) {
            schemaName = schema.getName();
        }
        if (schemaName == null || schemaName.isBlank()) {
            schemaName = "public";
        }

        int tablesCount = 0;
        int viewsCount = 0;
        int seqsCount = 0;
        int procsCount = 0;
        int funcsCount = 0;

        try (Connection conn = client(profile, catalog).getConnection()) {
            // Count tables, views, sequences from pg_class
            String classSql = "SELECT " +
                    "COUNT(CASE WHEN c.relkind IN ('r', 'p', 'f') AND NOT EXISTS (SELECT 1 FROM pg_inherits i WHERE i.inhrelid = c.oid) THEN 1 END) AS tables_cnt, " +
                    "COUNT(CASE WHEN c.relkind IN ('v', 'm') THEN 1 END) AS views_cnt, " +
                    "COUNT(CASE WHEN c.relkind = 'S' THEN 1 END) AS seqs_cnt " +
                    "FROM pg_class c " +
                    "JOIN pg_namespace n ON n.oid = c.relnamespace " +
                    "WHERE n.nspname = ?";
            try (PreparedStatement stmt = conn.prepareStatement(classSql)) {
                stmt.setString(1, schemaName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        tablesCount = rs.getInt(1);
                        viewsCount = rs.getInt(2);
                        seqsCount = rs.getInt(3);
                    }
                }
            }

            // Count procedures and functions from pg_proc
            try {
                String procSql = "SELECT " +
                        "COUNT(CASE WHEN p.prokind = 'p' THEN 1 END) AS procs_cnt, " +
                        "COUNT(CASE WHEN p.prokind IS NULL OR p.prokind <> 'p' THEN 1 END) AS funcs_cnt " +
                        "FROM pg_proc p " +
                        "JOIN pg_namespace n ON n.oid = p.pronamespace " +
                        "WHERE n.nspname = ?";
                try (PreparedStatement stmt = conn.prepareStatement(procSql)) {
                    stmt.setString(1, schemaName);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            procsCount = rs.getInt(1);
                            funcsCount = rs.getInt(2);
                        }
                    }
                }
            } catch (SQLException ignored) {
                // PostgreSQL < 11 compatibility fallback: all routines are functions
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace WHERE n.nspname = ?")) {
                    stmt.setString(1, schemaName);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) funcsCount = rs.getInt(1);
                    }
                } catch (SQLException ignored2) {}
            }
        } catch (Exception e) {
            // Safe fallback to default folders on connection error
            return objectFolders(catalog, schemaName);
        }

        List<DbObject> folders = new ArrayList<>();

        // tables folder: always present, with count detail if > 0
        DbObject tablesFolder = folder("tables", Kind.TABLES_FOLDER, catalog, schemaName);
        if (tablesCount > 0) {
            tablesFolder.setDetail(String.valueOf(tablesCount));
        }
        folders.add(tablesFolder);

        // views folder: only show if viewsCount > 0
        if (viewsCount > 0) {
            DbObject viewsFolder = folder("views", Kind.VIEWS_FOLDER, catalog, schemaName);
            viewsFolder.setDetail(String.valueOf(viewsCount));
            folders.add(viewsFolder);
        }

        // sequences folder: only show if seqsCount > 0
        if (seqsCount > 0) {
            DbObject seqsFolder = folder("sequences", Kind.SEQUENCES_FOLDER, catalog, schemaName);
            seqsFolder.setDetail(String.valueOf(seqsCount));
            folders.add(seqsFolder);
        }

        // procedures folder: only show if procsCount > 0
        if (procsCount > 0) {
            DbObject procsFolder = folder("procedures", Kind.PROCEDURES_FOLDER, catalog, schemaName);
            procsFolder.setDetail(String.valueOf(procsCount));
            folders.add(procsFolder);
        }

        // functions folder: only show if funcsCount > 0
        if (funcsCount > 0) {
            DbObject funcsFolder = folder("functions", Kind.FUNCTIONS_FOLDER, catalog, schemaName);
            funcsFolder.setDetail(String.valueOf(funcsCount));
            folders.add(funcsFolder);
        }

        return folders;
    }

    private static List<DbObject> objectFolders(String catalog, String schema) {
        List<DbObject> folders = new ArrayList<>();
        folders.add(folder("tables", Kind.TABLES_FOLDER, catalog, schema));
        return folders;
    }

    private static DbObject folder(String name, Kind kind, String catalog, String schema) {
        return new DbObject(name, kind, catalog, schema);
    }

    // ------------------------------------------------------------- folders

    /** Children of a folder node (Tables, Views, Collections, Columns, ...). */
    public static List<DbObject> loadFolderChildren(ConnectionProfile profile, DbObject dbFolder)
            throws Exception {
        String catalog = dbFolder.getCatalog();
        String schema = dbFolder.getSchema();

        List<DbObject> result = switch (dbFolder.getKind()) {
            case COLLECTIONS_FOLDER -> {
                List<DbObject> colls = new ArrayList<>();
                for (String name : ClientRegistry.mongo(profile).listCollections(catalog)) {
                    colls.add(new DbObject(name, Kind.COLLECTION, catalog, null));
                }
                yield colls;
            }
            case TABLES_FOLDER -> loadTablesOrViews(profile, catalog, schema, "TABLE", Kind.TABLE);
            case VIEWS_FOLDER  -> loadTablesOrViews(profile, catalog, schema, "VIEW", Kind.VIEW);
            case PROCEDURES_FOLDER -> loadRoutines(profile, catalog, schema, true);
            case FUNCTIONS_FOLDER  -> loadRoutines(profile, catalog, schema, false);
            case SEQUENCES_FOLDER  -> loadSequences(profile, catalog, schema);
            case COLUMNS_FOLDER    -> loadColumns(profile, dbFolder);
            case INDEXES_FOLDER    -> profile.getType() == DatabaseType.MONGODB
                    ? loadMongoIndexes(profile, dbFolder) : loadIndexes(profile, dbFolder);
            case FIELDS_FOLDER       -> loadMongoFields(profile, dbFolder);
            case KEYS_FOLDER         -> loadKeys(profile, dbFolder);
            case FOREIGN_KEYS_FOLDER -> loadForeignKeysFolder(profile, dbFolder);
            case PARTITIONS_FOLDER -> loadPartitions(profile, dbFolder);
            default -> List.of();
        };

        // StratosDB has no schema/catalog-qualified table syntax at all in its
        // own real SQL grammar - a real, multi-database StratosCluster gets
        // per-database isolation entirely through a separate connection per
        // database (already correctly handled via ClientRegistry.jdbc using
        // this same catalog value), not through a "catalog.table" reference
        // within a single query. Without this, qualifiedName() would build a
        // real, syntactically invalid reference for any table/view/procedure/
        // function/sequence under a real database node in cluster mode - a
        // real bug found via a real, live DBNavigator session against a real
        // multi-database StratosDB cluster.
        if (profile.getType() == DatabaseType.STRATOSDB) {
            for (DbObject obj : result) {
                obj.setSkipCatalogQualifier(true);
            }
        }
        return result;
    }

    private static List<DbObject> loadTablesOrViews(ConnectionProfile profile, String catalog,
                                                    String schema, String type, Kind kind)
            throws SQLException {
        // PostgreSQL: hide partition child tables from the Tables folder —
        // they appear under their parent table's "partitions" node instead
        if (profile.getType() == DatabaseType.POSTGRESQL && "TABLE".equals(type)) {
            return loadPostgresTablesWithoutPartitions(profile, catalog, schema);
        }

        String schemaPattern = (profile.getType() == DatabaseType.SQLITE) ? null : schema;
        List<DbObject> result = new ArrayList<>();
        try (Connection conn = client(profile, catalog).getConnection();
             ResultSet rs = conn.getMetaData().getTables(
                     metaCatalog(profile, catalog), schemaPattern, "%", new String[]{type})) {
            while (rs.next()) {
                result.add(new DbObject(rs.getString("TABLE_NAME"), kind, catalog, schema));
            }
        }
        return result;
    }

    /**
     * Regular + partitioned parent tables of a schema, excluding partition
     * children (anything that is a child in pg_inherits — this covers both
     * declarative partitions and old-style inheritance children).
     */
    private static List<DbObject> loadPostgresTablesWithoutPartitions(
            ConnectionProfile profile, String catalog, String schema) throws SQLException {
        String sql = "SELECT c.relname FROM pg_class c " +
                "JOIN pg_namespace n ON n.oid = c.relnamespace " +
                "WHERE n.nspname = ? AND c.relkind IN ('r', 'p', 'f') " +
                "AND NOT EXISTS (SELECT 1 FROM pg_inherits i WHERE i.inhrelid = c.oid) " +
                "ORDER BY c.relname";
        List<DbObject> result = new ArrayList<>();
        try (Connection conn = client(profile, catalog).getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schema);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new DbObject(rs.getString(1), Kind.TABLE, catalog, schema));
                }
            }
        }
        return result;
    }

    private static List<DbObject> loadRoutines(ConnectionProfile profile, String catalog,
                                               String schema, boolean procedures) {
        List<DbObject> result = new ArrayList<>();
        try (Connection conn = client(profile, catalog).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = procedures
                    ? meta.getProcedures(metaCatalog(profile, catalog), schema, "%")
                    : meta.getFunctions(metaCatalog(profile, catalog), schema, "%");
            try (rs) {
                String nameCol = procedures ? "PROCEDURE_NAME" : "FUNCTION_NAME";
                while (rs.next()) {
                    result.add(new DbObject(rs.getString(nameCol),
                            procedures ? Kind.PROCEDURE : Kind.FUNCTION, catalog, schema));
                }
            }
        } catch (SQLException e) {
            return List.of();
        }
        return result;
    }

    /**
     * The CREATE-ready source of a stored procedure or function, for an
     * "Edit Source" console pre-filled with the object's own definition.
     *
     * Oracle-specific for now: ALL_SOURCE holds exactly the text needed,
     * for any schema the connected user has privilege to see (not just
     * their own, the way USER_SOURCE would be), reconstructed with a
     * CREATE OR REPLACE prefix since ALL_SOURCE's own TEXT column starts
     * mid-statement, right after that keyword. Every other engine has a
     * genuinely different mechanism for this (PostgreSQL: pg_get_
     * functiondef(); MySQL: SHOW CREATE PROCEDURE; SQL Server: sys.sql_
     * modules / OBJECT_DEFINITION()) and isn't wired up yet — this
     * returns null for them, which the caller falls back on gracefully.
     */
    public static String getRoutineSource(ConnectionProfile profile, DbObject obj) throws SQLException {
        if (profile.getType() != DatabaseType.ORACLE) return null;
        String type = obj.getKind() == Kind.FUNCTION ? "FUNCTION" : "PROCEDURE";
        StringBuilder body = new StringBuilder();
        try (Connection conn = client(profile, obj.getCatalog()).getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT text FROM all_source WHERE owner = ? AND name = ? AND type = ? ORDER BY line")) {
            ps.setString(1, obj.getSchema());
            ps.setString(2, obj.getName());
            ps.setString(3, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) body.append(rs.getString(1));
            }
        }
        if (body.isEmpty()) return null;
        return "CREATE OR REPLACE " + body;
    }

    /**
     * IN/INOUT parameter names of a procedure or function, in declared
     * order — used to build a ready-to-fill "BEGIN name(:param1, :param2);
     * END;" invocation block. Standard JDBC (DatabaseMetaData.
     * getProcedureColumns), so this works the same way across every
     * relational engine without engine-specific code.
     */
    public static List<String> loadProcedureParameters(ConnectionProfile profile, DbObject obj)
            throws SQLException {
        List<String> params = new ArrayList<>();
        try (Connection conn = client(profile, obj.getCatalog()).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = obj.getKind() == Kind.FUNCTION
                    ? meta.getFunctionColumns(metaCatalog(profile, obj.getCatalog()), obj.getSchema(),
                            obj.getName(), "%")
                    : meta.getProcedureColumns(metaCatalog(profile, obj.getCatalog()), obj.getSchema(),
                            obj.getName(), "%");
            try (rs) {
                while (rs.next()) {
                    short columnType = rs.getShort("COLUMN_TYPE");
                    boolean isIn = columnType == DatabaseMetaData.procedureColumnIn
                            || columnType == DatabaseMetaData.procedureColumnInOut
                            || columnType == DatabaseMetaData.functionColumnIn
                            || columnType == DatabaseMetaData.functionColumnInOut;
                    if (!isIn) continue;
                    String name = rs.getString("COLUMN_NAME");
                    if (name != null && !name.isBlank()) params.add(name);
                }
            }
        }
        return params;
    }

    private static List<DbObject> loadSequences(ConnectionProfile profile, String catalog, String schema) {
        if (profile.getType() == DatabaseType.POSTGRESQL) {
            return loadPostgresSequences(profile, catalog, schema);
        }
        String sql = switch (profile.getType()) {
            case ORACLE -> "SELECT sequence_name FROM all_sequences WHERE sequence_owner = '" + schema + "'";
            case SQLSERVER -> "SELECT name AS sequence_name FROM sys.sequences";
            default -> null;
        };
        if (sql == null) return List.of();

        List<DbObject> result = new ArrayList<>();
        try (Connection conn = client(profile, catalog).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new DbObject(rs.getString(1), Kind.SEQUENCE, catalog, schema));
            }
        } catch (SQLException ignored) {
            return List.of();
        }
        return result;
    }

    private static List<DbObject> loadPostgresSequences(ConnectionProfile profile, String catalog, String schema) {
        String targetSchema = (schema != null && !schema.isBlank()) ? schema : "public";
        String sql = "SELECT c.relname AS seqname, " +
                "COALESCE(format_type(s.seqtypid, null), 'bigint') AS seqtype " +
                "FROM pg_class c " +
                "JOIN pg_namespace n ON n.oid = c.relnamespace " +
                "LEFT JOIN pg_sequence s ON s.seqrelid = c.oid " +
                "WHERE c.relkind = 'S' AND n.nspname = ? " +
                "ORDER BY c.relname";
        List<DbObject> result = new ArrayList<>();
        try (Connection conn = client(profile, catalog).getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, targetSchema);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String seqName = rs.getString(1);
                    String seqType = rs.getString(2);
                    DbObject obj = new DbObject(seqName, Kind.SEQUENCE, catalog, targetSchema);
                    if (seqType != null && !seqType.isBlank()) {
                        obj.setDetail(seqType);
                    }
                    result.add(obj);
                }
            }
        } catch (SQLException ignored) {
            return List.of();
        }
        return result;
    }

    // ------------------------------------------- table children folder structure

    /**
     * Children of a TABLE node: "columns N", "indexes N" (and "partitions N"
     * for PostgreSQL) folders with counts in the detail text.
     */
    /** A MongoDB collection's children: fields (inferred schema) and indexes — always both, unlike SQL's conditional Keys/Foreign Keys. */
    public static List<DbObject> loadCollectionChildren(ConnectionProfile profile, DbObject collection)
            throws Exception {
        List<DbObject> result = new ArrayList<>();

        DbObject fields = childFolder("fields", Kind.FIELDS_FOLDER, collection);
        int fieldCount = ClientRegistry.mongo(profile)
                .inferFields(collection.getCatalog(), collection.getName(), 100).size();
        fields.setDetail(String.valueOf(fieldCount));
        result.add(fields);

        DbObject indexes = childFolder("indexes", Kind.INDEXES_FOLDER, collection);
        int indexCount = ClientRegistry.mongo(profile)
                .listIndexes(collection.getCatalog(), collection.getName()).size();
        indexes.setDetail(String.valueOf(indexCount));
        result.add(indexes);

        return result;
    }

    private static List<DbObject> loadMongoFields(ConnectionProfile profile, DbObject folder) {
        List<DbObject> result = new ArrayList<>();
        for (MongoDbClient.FieldInfo field
                : ClientRegistry.mongo(profile).inferFields(folder.getCatalog(), folder.getTableName(), 100)) {
            DbObject obj = new DbObject(field.name(), Kind.FIELD, folder.getCatalog(), null);
            obj.setTableName(folder.getTableName());
            obj.setDetail(field.type());
            result.add(obj);
        }
        return result;
    }

    private static List<DbObject> loadMongoIndexes(ConnectionProfile profile, DbObject folder) {
        List<DbObject> result = new ArrayList<>();
        for (MongoDbClient.IndexInfo index
                : ClientRegistry.mongo(profile).listIndexes(folder.getCatalog(), folder.getTableName())) {
            DbObject obj = new DbObject(index.name(), Kind.INDEX, folder.getCatalog(), null);
            obj.setTableName(folder.getTableName());
            obj.setDetail(index.detail());
            result.add(obj);
        }
        return result;
    }

    public static List<DbObject> loadTableChildren(ConnectionProfile profile, DbObject table)
            throws SQLException {
        List<DbObject> result = new ArrayList<>();

        int columnCount = 0;
        Set<String> indexNames = new LinkedHashSet<>();
        int primaryKeyColumnCount = 0;
        Set<String> foreignKeyNames = new LinkedHashSet<>();
        try (Connection conn = client(profile, table.getCatalog()).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String cat = metaCatalog(profile, table.getCatalog());
            try (ResultSet rs = meta.getColumns(cat, table.getSchema(), table.getName(), "%")) {
                while (rs.next()) columnCount++;
            }
            try (ResultSet rs = meta.getIndexInfo(cat, table.getSchema(), table.getName(), false, true)) {
                while (rs.next()) {
                    String name = rs.getString("INDEX_NAME");
                    if (name != null) indexNames.add(name);
                }
            } catch (SQLException ignored) { /* some drivers can't */ }
            try (ResultSet rs = meta.getPrimaryKeys(cat, table.getSchema(), table.getName())) {
                while (rs.next()) primaryKeyColumnCount++;
            } catch (SQLException ignored) { /* some drivers can't */ }
            try (ResultSet rs = meta.getImportedKeys(cat, table.getSchema(), table.getName())) {
                while (rs.next()) {
                    String name = rs.getString("FK_NAME");
                    foreignKeyNames.add(name != null ? name : rs.getString("FKCOLUMN_NAME"));
                }
            } catch (SQLException ignored) { /* some drivers can't */ }
        }

        DbObject columns = childFolder("columns", Kind.COLUMNS_FOLDER, table);
        columns.setDetail(String.valueOf(columnCount));
        result.add(columns);

        // Matches the reference: these two folders only appear when the
        // table actually has a primary key / foreign keys, not as empty folders.
        if (primaryKeyColumnCount > 0) {
            DbObject keys = childFolder("keys", Kind.KEYS_FOLDER, table);
            keys.setDetail("1");
            result.add(keys);
        }
        if (!foreignKeyNames.isEmpty()) {
            DbObject foreignKeys = childFolder("foreign keys", Kind.FOREIGN_KEYS_FOLDER, table);
            foreignKeys.setDetail(String.valueOf(foreignKeyNames.size()));
            result.add(foreignKeys);
        }

        DbObject indexes = childFolder("indexes", Kind.INDEXES_FOLDER, table);
        indexes.setDetail(String.valueOf(indexNames.size()));
        result.add(indexes);

        if (profile.getType() == DatabaseType.POSTGRESQL) {
            long partitionCount = countPartitions(profile, table);
            if (partitionCount > 0) {
                DbObject partitions = childFolder("partitions", Kind.PARTITIONS_FOLDER, table);
                partitions.setDetail(String.valueOf(partitionCount));
                result.add(partitions);
            }
        }
        return result;
    }

    private static DbObject childFolder(String name, Kind kind, DbObject table) {
        DbObject folder = new DbObject(name, kind, table.getCatalog(), table.getSchema());
        folder.setTableName(table.getName());
        return folder;
    }

    private static long countPartitions(ConnectionProfile profile, DbObject table) {
        String sql = "SELECT count(*) FROM pg_inherits i " +
                "JOIN pg_class p ON p.oid = i.inhparent " +
                "JOIN pg_namespace n ON n.oid = p.relnamespace " +
                "WHERE n.nspname = ? AND p.relname = ?";
        try (Connection conn = client(profile, table.getCatalog()).getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, table.getSchema());
            stmt.setString(2, table.getName());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    /** The table's primary key, as a single "keys" folder entry — a table has at most one. */
    private static List<DbObject> loadKeys(ConnectionProfile profile, DbObject folder)
            throws SQLException {
        Map<Integer, String> columnsBySeq = new java.util.TreeMap<>();
        String pkName = null;
        try (Connection conn = client(profile, folder.getCatalog()).getConnection();
             ResultSet rs = conn.getMetaData().getPrimaryKeys(
                     metaCatalog(profile, folder.getCatalog()), folder.getSchema(), folder.getTableName())) {
            while (rs.next()) {
                columnsBySeq.put(rs.getInt("KEY_SEQ"), rs.getString("COLUMN_NAME"));
                pkName = rs.getString("PK_NAME");
            }
        }
        if (columnsBySeq.isEmpty()) return List.of();

        DbObject key = new DbObject(pkName != null && !pkName.isBlank() ? pkName : "PRIMARY", Kind.KEY,
                folder.getCatalog(), folder.getSchema());
        key.setTableName(folder.getTableName());
        key.setDetail("(" + String.join(", ", columnsBySeq.values()) + ")");
        return List.of(key);
    }

    /** Every foreign key this table declares, one entry per constraint (which may span multiple columns). */
    private static List<DbObject> loadForeignKeysFolder(ConnectionProfile profile, DbObject folder)
            throws SQLException {
        Map<String, List<String>> localColumns = new LinkedHashMap<>();
        Map<String, List<String>> foreignColumns = new LinkedHashMap<>();
        Map<String, String> foreignTable = new LinkedHashMap<>();

        try (Connection conn = client(profile, folder.getCatalog()).getConnection();
             ResultSet rs = conn.getMetaData().getImportedKeys(
                     metaCatalog(profile, folder.getCatalog()), folder.getSchema(), folder.getTableName())) {
            while (rs.next()) {
                String name = rs.getString("FK_NAME");
                if (name == null) name = rs.getString("FKCOLUMN_NAME") + "_fk";
                localColumns.computeIfAbsent(name, k -> new ArrayList<>()).add(rs.getString("FKCOLUMN_NAME"));
                foreignColumns.computeIfAbsent(name, k -> new ArrayList<>()).add(rs.getString("PKCOLUMN_NAME"));
                foreignTable.putIfAbsent(name, rs.getString("PKTABLE_NAME"));
            }
        }

        List<DbObject> result = new ArrayList<>();
        for (String name : localColumns.keySet()) {
            DbObject fk = new DbObject(name, Kind.FOREIGN_KEY, folder.getCatalog(), folder.getSchema());
            fk.setTableName(folder.getTableName());
            fk.setDetail("(" + String.join(", ", localColumns.get(name)) + ")  \u2192  "
                    + foreignTable.get(name) + " (" + String.join(", ", foreignColumns.get(name)) + ")");
            result.add(fk);
        }
        return result;
    }

    private static List<DbObject> loadColumns(ConnectionProfile profile, DbObject folder)
            throws SQLException {
        List<DbObject> result = new ArrayList<>();
        try (Connection conn = client(profile, folder.getCatalog()).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String cat = metaCatalog(profile, folder.getCatalog());

            Set<String> primaryKeys = new LinkedHashSet<>();
            try (ResultSet pk = meta.getPrimaryKeys(cat, folder.getSchema(), folder.getTableName())) {
                while (pk.next()) primaryKeys.add(pk.getString("COLUMN_NAME"));
            } catch (SQLException ignored) {}

            try (ResultSet rs = meta.getColumns(cat, folder.getSchema(), folder.getTableName(), "%")) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    DbObject col = new DbObject(name, Kind.COLUMN,
                            folder.getCatalog(), folder.getSchema());
                    col.setTableName(folder.getTableName());
                    String type = formatColumnType(rs.getString("TYPE_NAME"),
                            rs.getInt("COLUMN_SIZE"), rs.getInt("DECIMAL_DIGITS"));
                    boolean nullable = "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
                    col.setDetail(type
                            + (primaryKeys.contains(name) ? "  PK" : nullable ? "" : "  not null"));
                    result.add(col);
                }
            }
        }
        return result;
    }

    private static final Set<String> CHAR_LENGTH_TYPES = Set.of(
            "varchar", "char", "character varying", "character", "nvarchar", "nchar", "bpchar", "binary", "varbinary");
    private static final Set<String> PRECISION_SCALE_TYPES = Set.of("numeric", "decimal");
    private static final Set<String> FRACTIONAL_SECONDS_TYPES = Set.of(
            "timestamp", "timestamptz", "timestamp with time zone", "timestamp without time zone",
            "time", "timetz", "time with time zone", "time without time zone", "datetime");

    /**
     * Builds the exact type string the reference IDE shows — e.g.
     * {@code numeric(19,4)}, {@code varchar(255)}, {@code timestamp(6)} —
     * rather than just the bare type name JDBC's TYPE_NAME reports on its
     * own, which drops the precision/scale/length that's actually part of
     * the column's real declared type.
     */
    private static String formatColumnType(String typeName, int columnSize, int decimalDigits) {
        String lower = typeName.toLowerCase(Locale.ROOT);
        if (CHAR_LENGTH_TYPES.contains(lower) && columnSize > 0) {
            return lower + "(" + columnSize + ")";
        }
        if (PRECISION_SCALE_TYPES.contains(lower) && columnSize > 0) {
            return decimalDigits > 0 ? lower + "(" + columnSize + "," + decimalDigits + ")"
                                      : lower + "(" + columnSize + ")";
        }
        if (FRACTIONAL_SECONDS_TYPES.contains(lower) && decimalDigits >= 0) {
            return lower + "(" + decimalDigits + ")";
        }
        return lower;
    }

    private static List<DbObject> loadIndexes(ConnectionProfile profile, DbObject folder)
            throws SQLException {
        Map<String, List<String>> indexColumns = new LinkedHashMap<>();
        Map<String, Boolean> unique = new LinkedHashMap<>();
        try (Connection conn = client(profile, folder.getCatalog()).getConnection();
             ResultSet rs = conn.getMetaData().getIndexInfo(
                     metaCatalog(profile, folder.getCatalog()),
                     folder.getSchema(), folder.getTableName(), false, true)) {
            while (rs.next()) {
                String name = rs.getString("INDEX_NAME");
                if (name == null) continue;
                indexColumns.computeIfAbsent(name, k -> new ArrayList<>())
                        .add(rs.getString("COLUMN_NAME"));
                unique.put(name, !rs.getBoolean("NON_UNIQUE"));
            }
        }
        List<DbObject> result = new ArrayList<>();
        for (var entry : indexColumns.entrySet()) {
            DbObject idx = new DbObject(entry.getKey(), Kind.INDEX,
                    folder.getCatalog(), folder.getSchema());
            idx.setTableName(folder.getTableName());
            idx.setDetail("(" + String.join(", ", entry.getValue()) + ")"
                    + (Boolean.TRUE.equals(unique.get(entry.getKey())) ? "  unique" : ""));
            result.add(idx);
        }
        return result;
    }

    private static List<DbObject> loadPartitions(ConnectionProfile profile, DbObject folder) {
        // Partitions ARE tables — fetch them with their own schema so they can be
        // opened, expanded to columns/indexes, and queried like any other table.
        String sql = "SELECT c.relname, cn.nspname FROM pg_inherits i " +
                "JOIN pg_class c ON c.oid = i.inhrelid " +
                "JOIN pg_namespace cn ON cn.oid = c.relnamespace " +
                "JOIN pg_class p ON p.oid = i.inhparent " +
                "JOIN pg_namespace n ON n.oid = p.relnamespace " +
                "WHERE n.nspname = ? AND p.relname = ? ORDER BY c.relname";
        List<DbObject> result = new ArrayList<>();
        try (Connection conn = client(profile, folder.getCatalog()).getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, folder.getSchema());
            stmt.setString(2, folder.getTableName());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DbObject part = new DbObject(rs.getString(1), Kind.PARTITION,
                            folder.getCatalog(), rs.getString(2));
                    result.add(part);
                }
            }
        } catch (SQLException e) {
            return List.of();
        }
        return result;
    }

    // -------------------------------------------------------- structure view

    /** Column definitions of a table — used by the structure viewer. */
    public static QueryResult loadTableStructure(ConnectionProfile profile, DbObject table)
            throws SQLException {
        QueryResult result = new QueryResult();
        result.getColumns().addAll(List.of("Column", "Type", "Size", "Nullable", "Default", "Key"));

        try (Connection conn = client(profile, table.getCatalog()).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String cat = metaCatalog(profile, table.getCatalog());

            List<String> primaryKeys = new ArrayList<>();
            try (ResultSet pk = meta.getPrimaryKeys(cat, table.getSchema(), table.getName())) {
                while (pk.next()) primaryKeys.add(pk.getString("COLUMN_NAME"));
            }

            try (ResultSet rs = meta.getColumns(cat, table.getSchema(), table.getName(), "%")) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    String typeName = rs.getString("TYPE_NAME");
                    int columnSize = rs.getInt("COLUMN_SIZE");
                    int decimalDigits = rs.getInt("DECIMAL_DIGITS");
                    // Oracle's driver backs COLUMN_DEF with a LONG column
                    // (mirroring DBA_TAB_COLUMNS.DATA_DEFAULT in the data
                    // dictionary), which — like every LONG/LONG RAW column —
                    // can only be read once, and only before any
                    // later-positioned column in the result set is read.
                    // IS_NULLABLE sits after COLUMN_DEF in the standard JDBC
                    // column ordering, so it must be read second, not first,
                    // or Oracle throws ORA-17027 ("Stream has already been
                    // closed") the moment this tries to read COLUMN_DEF at
                    // all — every other engine tolerates either order fine,
                    // which is exactly why this only ever broke on Oracle.
                    String defaultValue = rs.getString("COLUMN_DEF");
                    String nullable = rs.getString("IS_NULLABLE");
                    result.getRows().add(new ArrayList<>(List.of(
                            name,
                            String.valueOf(typeName),
                            String.valueOf(columnSize),
                            "YES".equalsIgnoreCase(nullable) ? "YES" : "NO",
                            String.valueOf(defaultValue),
                            primaryKeys.contains(name) ? "PK" : ""
                    )));
                }
            }
        }
        return result;
    }

    /** Indexes of a table. */
    public static QueryResult loadTableIndexes(ConnectionProfile profile, DbObject table)
            throws SQLException {
        QueryResult result = new QueryResult();
        result.getColumns().addAll(List.of("Index", "Column", "Unique", "Type"));

        try (Connection conn = client(profile, table.getCatalog()).getConnection();
             ResultSet rs = conn.getMetaData().getIndexInfo(
                     metaCatalog(profile, table.getCatalog()),
                     table.getSchema(), table.getName(), false, true)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if (indexName == null) continue;
                result.getRows().add(new ArrayList<>(List.of(
                        indexName,
                        String.valueOf(rs.getString("COLUMN_NAME")),
                        rs.getBoolean("NON_UNIQUE") ? "NO" : "YES",
                        indexType(rs.getShort("TYPE"))
                )));
            }
        }
        return result;
    }

    /**
     * JDBC type code (java.sql.Types) per column name — used so grid edits are
     * bound as typed PreparedStatement parameters instead of raw SQL literals.
     */
    public static Map<String, Integer> loadColumnTypes(ConnectionProfile profile, DbObject table)
            throws SQLException {
        Map<String, Integer> types = new LinkedHashMap<>();
        try (Connection conn = client(profile, table.getCatalog()).getConnection();
             ResultSet rs = conn.getMetaData().getColumns(
                     metaCatalog(profile, table.getCatalog()), table.getSchema(), table.getName(), "%")) {
            while (rs.next()) {
                types.put(rs.getString("COLUMN_NAME"), rs.getInt("DATA_TYPE"));
            }
        }
        return types;
    }

    /** One column's full detail — used by Modify Table and the ER diagram. */
    public record ColumnInfo(String name, String typeName, int size, boolean nullable,
                             String defaultValue, boolean primaryKey) {}

    public static List<ColumnInfo> loadColumnInfo(ConnectionProfile profile, DbObject table)
            throws SQLException {
        List<ColumnInfo> result = new ArrayList<>();
        try (Connection conn = client(profile, table.getCatalog()).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String cat = metaCatalog(profile, table.getCatalog());

            Set<String> pk = new LinkedHashSet<>();
            try (ResultSet rs = meta.getPrimaryKeys(cat, table.getSchema(), table.getName())) {
                while (rs.next()) pk.add(rs.getString("COLUMN_NAME"));
            }
            try (ResultSet rs = meta.getColumns(cat, table.getSchema(), table.getName(), "%")) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    int columnSize = rs.getInt("COLUMN_SIZE");
                    String formattedType = formatColumnType(rs.getString("TYPE_NAME"),
                            columnSize, rs.getInt("DECIMAL_DIGITS"));
                    // See loadTableStructure just above for why COLUMN_DEF
                    // must be read before IS_NULLABLE, not after — Oracle's
                    // driver backs it with a LONG column, which can only be
                    // read once and only before any later-positioned column
                    // in the result set has already been read.
                    String defaultValue = rs.getString("COLUMN_DEF");
                    boolean nullable = "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
                    result.add(new ColumnInfo(
                            name,
                            formattedType,
                            columnSize,
                            nullable,
                            defaultValue,
                            pk.contains(name)));
                }
            }
        }
        return result;
    }

    /** One foreign-key relationship, direction-agnostic once resolved. */
    public record ForeignKey(String fromSchema, String fromTable, String fromColumn,
                             String toSchema, String toTable, String toColumn,
                             String constraintName) {}

    /**
     * FK relationships one hop from a table: both the FKs it declares (this
     * table \u2192 parent tables) and the FKs other tables declare against it
     * (child tables \u2192 this table). Powers the ER diagram.
     */
    public static List<ForeignKey> loadRelatedForeignKeys(ConnectionProfile profile, DbObject table)
            throws SQLException {
        List<ForeignKey> result = new ArrayList<>();
        try (Connection conn = client(profile, table.getCatalog()).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String cat = metaCatalog(profile, table.getCatalog());

            // This table's own FK columns -> the tables they reference
            try (ResultSet rs = meta.getImportedKeys(cat, table.getSchema(), table.getName())) {
                while (rs.next()) {
                    result.add(new ForeignKey(
                            rs.getString("FKTABLE_SCHEM"), rs.getString("FKTABLE_NAME"), rs.getString("FKCOLUMN_NAME"),
                            rs.getString("PKTABLE_SCHEM"), rs.getString("PKTABLE_NAME"), rs.getString("PKCOLUMN_NAME"),
                            rs.getString("FK_NAME")));
                }
            }
            // Other tables' FK columns that reference this table
            try (ResultSet rs = meta.getExportedKeys(cat, table.getSchema(), table.getName())) {
                while (rs.next()) {
                    String fkTable = rs.getString("FKTABLE_NAME");
                    if (fkTable.equalsIgnoreCase(table.getName())) continue;   // avoid duplicate self-ref
                    result.add(new ForeignKey(
                            rs.getString("FKTABLE_SCHEM"), fkTable, rs.getString("FKCOLUMN_NAME"),
                            rs.getString("PKTABLE_SCHEM"), rs.getString("PKTABLE_NAME"), rs.getString("PKCOLUMN_NAME"),
                            rs.getString("FK_NAME")));
                }
            }
        }
        return result;
    }

    /** Primary key column names of a table (used by the editable data grid). */
    public static List<String> loadPrimaryKeys(ConnectionProfile profile, DbObject table)
            throws SQLException {
        List<String> keys = new ArrayList<>();
        try (Connection conn = client(profile, table.getCatalog()).getConnection();
             ResultSet rs = conn.getMetaData().getPrimaryKeys(
                     metaCatalog(profile, table.getCatalog()), table.getSchema(), table.getName())) {
            while (rs.next()) keys.add(rs.getString("COLUMN_NAME"));
        }
        return keys;
    }

    /**
     * All table names visible in a database — used for autocomplete.
     * PostgreSQL: partition child tables are excluded, only parent tables
     * (and regular tables/views) are suggested.
     */
    public static List<String> listAllTables(ConnectionProfile profile, String catalog) {
        if (profile.getType() == DatabaseType.POSTGRESQL) {
            List<String> tables = new ArrayList<>();
            String sql = "SELECT c.relname FROM pg_class c " +
                    "JOIN pg_namespace n ON n.oid = c.relnamespace " +
                    "WHERE c.relkind IN ('r', 'p', 'v', 'm') " +
                    "AND n.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast') " +
                    "AND NOT EXISTS (SELECT 1 FROM pg_inherits i WHERE i.inhrelid = c.oid) " +
                    "ORDER BY 1";
            try (Connection conn = client(profile, catalog).getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) tables.add(rs.getString(1));
            } catch (SQLException ignored) {}
            return tables;
        }

        List<String> tables = new ArrayList<>();
        try (Connection conn = client(profile, catalog).getConnection();
             ResultSet rs = conn.getMetaData().getTables(
                     metaCatalog(profile, catalog), metaSchema(profile, catalog), "%",
                     new String[]{"TABLE", "VIEW"})) {
            while (rs.next()) {
                String schema = rs.getString("TABLE_SCHEM");
                if (schema != null && PG_SYSTEM_SCHEMAS.contains(schema)) continue;
                tables.add(rs.getString("TABLE_NAME"));
            }
        } catch (SQLException ignored) {}
        return tables;
    }

    /** All user-defined sequence names visible in a database — used for autocomplete. */
    public static List<String> listAllSequences(ConnectionProfile profile, String catalog) {
        List<String> sequences = new ArrayList<>();
        if (profile.getType() == DatabaseType.POSTGRESQL) {
            String sql = "SELECT c.relname FROM pg_class c " +
                    "JOIN pg_namespace n ON n.oid = c.relnamespace " +
                    "WHERE c.relkind = 'S' " +
                    "AND n.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast') " +
                    "ORDER BY c.relname";
            try (Connection conn = client(profile, catalog).getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) sequences.add(rs.getString(1));
            } catch (SQLException ignored) {}
            return sequences;
        }

        try (Connection conn = client(profile, catalog).getConnection();
             ResultSet rs = conn.getMetaData().getTables(
                     metaCatalog(profile, catalog), metaSchema(profile, catalog), "%",
                     new String[]{"SEQUENCE"})) {
            while (rs.next()) sequences.add(rs.getString("TABLE_NAME"));
        } catch (SQLException ignored) {}
        return sequences;
    }

    /** All user-defined schema names visible in a database — used for autocomplete. */
    public static List<String> listAllSchemas(ConnectionProfile profile, String catalog) {
        List<String> schemas = new ArrayList<>();
        if (profile == null) return schemas;
        try (Connection conn = client(profile, catalog).getConnection()) {
            if (profile.getType() == DatabaseType.POSTGRESQL) {
                String sql = "SELECT nspname FROM pg_namespace " +
                        "WHERE nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast') " +
                        "AND nspname NOT LIKE 'pg_temp_%' AND nspname NOT LIKE 'pg_toast_temp_%' " +
                        "ORDER BY nspname";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) schemas.add(rs.getString(1));
                }
            } else {
                try (ResultSet rs = conn.getMetaData().getSchemas(metaCatalog(profile, catalog), "%")) {
                    while (rs.next()) {
                        String s = rs.getString("TABLE_SCHEM");
                        if (s != null && !PG_SYSTEM_SCHEMAS.contains(s) && !isOracleSystemSchema(s)) {
                            schemas.add(s);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return schemas;
    }

    /** Distinct column names of every user table — global autocomplete pool. */
    public static List<String> listAllColumns(ConnectionProfile profile, String catalog) {
        Set<String> columns = new LinkedHashSet<>();
        try (Connection conn = client(profile, catalog).getConnection();
             ResultSet rs = conn.getMetaData().getColumns(
                     metaCatalog(profile, catalog), metaSchema(profile, catalog), "%", "%")) {
            while (rs.next() && columns.size() < 4000) {
                String schema = rs.getString("TABLE_SCHEM");
                if (schema != null && PG_SYSTEM_SCHEMAS.contains(schema)) continue;
                columns.add(rs.getString("COLUMN_NAME"));
            }
        } catch (SQLException ignored) {}
        return new ArrayList<>(columns);
    }

    /** Column names of one table — used for autocomplete after "table.". */
    public static List<String> listColumns(ConnectionProfile profile, String catalog, String table) {
        List<String> columns = new ArrayList<>();
        try (Connection conn = client(profile, catalog).getConnection();
             ResultSet rs = conn.getMetaData().getColumns(
                     metaCatalog(profile, catalog), metaSchema(profile, catalog), table, "%")) {
            while (rs.next()) columns.add(rs.getString("COLUMN_NAME"));
        } catch (SQLException ignored) {}
        return columns;
    }

    /**
     * Fast approximate row count from PostgreSQL's planner statistics
     * (pg_class.reltuples) — avoids a full COUNT(*) scan on huge tables.
     * Returns -1 when unavailable (non-Postgres, or stats never ANALYZEd).
     */
    public static long estimateRowCount(ConnectionProfile profile, DbObject table) {
        if (profile.getType() != DatabaseType.POSTGRESQL) return -1;
        String sql = "SELECT c.reltuples FROM pg_class c " +
                "JOIN pg_namespace n ON n.oid = c.relnamespace " +
                "WHERE n.nspname = ? AND c.relname = ?";
        try (Connection conn = client(profile, table.getCatalog()).getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, table.getSchema());
            stmt.setString(2, table.getName());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    float estimate = rs.getFloat(1);
                    return estimate < 0 ? -1 : Math.round(estimate);
                }
            }
        } catch (SQLException ignored) {}
        return -1;
    }

    /**
     * Every table across every non-system schema in this database — used by
     * the database-wide ER diagram. Unlike the Tables-folder listing, this
     * intentionally does not filter out PostgreSQL partition children: a
     * whole-database overview is meant to show every physical table, and
     * partitions rarely have FKs of their own anyway.
     */
    public static List<DbObject> loadDatabaseTables(ConnectionProfile profile, String catalog)
            throws SQLException {
        List<DbObject> result = new ArrayList<>();
        try (Connection conn = client(profile, catalog).getConnection();
             ResultSet rs = conn.getMetaData().getTables(
                     metaCatalog(profile, catalog), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String schema = rs.getString("TABLE_SCHEM");
                if (schema != null && PG_SYSTEM_SCHEMAS.contains(schema)) continue;
                result.add(new DbObject(rs.getString("TABLE_NAME"), Kind.TABLE, catalog, schema));
            }
        }
        return result;
    }

    /** Every FK relationship among the given tables — used by the database-wide ER diagram. */
    public static List<ForeignKey> loadForeignKeysForTables(ConnectionProfile profile, String catalog,
                                                            List<DbObject> tables) {
        List<ForeignKey> result = new ArrayList<>();
        try (Connection conn = client(profile, catalog).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String cat = metaCatalog(profile, catalog);
            for (DbObject table : tables) {
                try (ResultSet rs = meta.getImportedKeys(cat, table.getSchema(), table.getName())) {
                    while (rs.next()) {
                        result.add(new ForeignKey(
                                rs.getString("FKTABLE_SCHEM"), rs.getString("FKTABLE_NAME"), rs.getString("FKCOLUMN_NAME"),
                                rs.getString("PKTABLE_SCHEM"), rs.getString("PKTABLE_NAME"), rs.getString("PKCOLUMN_NAME"),
                                rs.getString("FK_NAME")));
                    }
                } catch (SQLException ignored) {
                    // some drivers/tables can't report FKs — skip rather than fail the whole diagram
                }
            }
        } catch (SQLException ignored) {
            return result;
        }
        return result;
    }

    /** Tables/views whose name contains the query — for Search Everywhere. */
    public static List<DbObject> searchTables(ConnectionProfile profile, String catalog,
                                              String query, int limit) {
        // PostgreSQL: search parent tables only — partition children are noise
        if (profile.getType() == DatabaseType.POSTGRESQL) {
            List<DbObject> out = new ArrayList<>();
            String sql = "SELECT c.relname, n.nspname FROM pg_class c " +
                    "JOIN pg_namespace n ON n.oid = c.relnamespace " +
                    "WHERE c.relkind IN ('r', 'p', 'v', 'm') " +
                    "AND n.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast') " +
                    "AND c.relname ILIKE ? " +
                    "AND NOT EXISTS (SELECT 1 FROM pg_inherits i WHERE i.inhrelid = c.oid) " +
                    "ORDER BY c.relname LIMIT " + limit;
            try (Connection conn = client(profile, catalog).getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "%" + query + "%");
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        out.add(new DbObject(rs.getString(1), Kind.TABLE,
                                catalog, rs.getString(2)));
                    }
                }
            } catch (SQLException ignored) {}
            return out;
        }

        List<DbObject> out = new ArrayList<>();
        try (Connection conn = client(profile, catalog).getConnection();
             ResultSet rs = conn.getMetaData().getTables(
                     metaCatalog(profile, catalog), metaSchema(profile, catalog), "%" + query + "%",
                     new String[]{"TABLE", "VIEW"})) {
            while (rs.next() && out.size() < limit) {
                String schema = rs.getString("TABLE_SCHEM");
                if (schema != null && PG_SYSTEM_SCHEMAS.contains(schema)) continue;
                String rowCatalog = rs.getString("TABLE_CAT");
                String useCatalog = catalog != null ? catalog : rowCatalog;
                out.add(new DbObject(rs.getString("TABLE_NAME"), Kind.TABLE, useCatalog, schema));
            }
        } catch (SQLException ignored) {}
        return out;
    }

    private static String indexType(short type) {
        return switch (type) {
            case DatabaseMetaData.tableIndexClustered -> "CLUSTERED";
            case DatabaseMetaData.tableIndexHashed    -> "HASHED";
            case DatabaseMetaData.tableIndexStatistic -> "STATISTIC";
            default -> "OTHER";
        };
    }
}
