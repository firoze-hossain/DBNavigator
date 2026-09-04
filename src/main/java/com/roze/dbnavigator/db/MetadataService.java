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
    // DataGrip tucks the equivalent noise away under its own "Database
    // Objects" node rather than listing it alongside real schemas like dbo.
    private static final Set<String> SQLSERVER_SYSTEM_SCHEMAS = Set.of(
            "db_owner", "db_accessadmin", "db_securityadmin", "db_ddladmin",
            "db_backupoperator", "db_datareader", "db_datawriter",
            "db_denydatareader", "db_denydatawriter", "sys", "guest",
            "information_schema");

    // Every Oracle install ships several dozen built-in component schemas
    // (Oracle Text, Spatial, Multimedia, XML DB, label security, and so on)
    // that are essentially never where a user's own tables live, alongside
    // SYS/SYSTEM which — deliberately not included here — genuinely are
    // used directly by many real users (this app's own SYSTEM user among
    // them). DataGrip's equivalent tucks this same noise away under its own
    // "Database Objects" node rather than listing it next to real schemas.
    private static final Set<String> ORACLE_SYSTEM_SCHEMAS = Set.of(
            "anonymous", "appqossys", "audsys", "ctxsys", "dbsfwuser", "dbsnmp",
            "dip", "dvf", "dvsys", "ggsys", "gsmadmin_internal", "gsmcatuser",
            "gsmuser", "lbacsys", "mddata", "mdsys", "ojvmsys", "olapsys",
            "oracle_ocm", "orddata", "ordplugins", "ordsys", "outln", "pdbadmin",
            "remote_scheduler_agent", "si_informtn_schema", "sys$umf",
            "sysbackup", "sysdg", "syskm", "sysrac", "wmsys", "xdb", "xs$null");

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
            // PostgreSQL: list EVERY database on the server (DataGrip style)
            case POSTGRESQL -> loadPostgresDatabases(profile);
            case MYSQL, MARIADB -> loadCatalogs(profile);
            // SQL Server: same idea as PostgreSQL — one login can see every
            // database on the instance, and each has its own set of schemas,
            // so it gets its own DATABASE level in the tree too (DataGrip
            // does the same: CompanyDB, master, etc. each as their own node).
            case SQLSERVER -> loadSqlServerDatabases(profile);
            case ORACLE -> {
                try (Connection conn = client(profile, null).getConnection()) {
                    yield loadSchemas(conn, profile, null);
                }
            }
            case SQLITE -> objectFolders(null, null);
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
                if (db.equals(profile.getDatabase())) obj.setDetail("(default)");
                result.add(obj);
            }
        }
        return result;
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
                if (profile.getType() == DatabaseType.ORACLE
                        && ORACLE_SYSTEM_SCHEMAS.contains(schema.toLowerCase(Locale.ROOT))) continue;
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
            case MONGODB -> List.of(
                    folder("Collections", Kind.COLLECTIONS_FOLDER, database.getCatalog(), null));
            case POSTGRESQL, SQLSERVER -> {
                // schemas of that particular database (separate physical connection,
                // via databaseName=/dbname in the JDBC URL — see ConnectionProfile.getJdbcUrl)
                try (Connection conn = client(profile, database.getCatalog()).getConnection()) {
                    yield loadSchemas(conn, profile, database.getCatalog());
                }
            }
            // MySQL/MariaDB: catalog acts as schema
            default -> objectFolders(database.getCatalog(), null);
        };
    }

    /** Children of a SCHEMA node. */
    public static List<DbObject> loadSchemaChildren(DbObject schema) {
        return objectFolders(schema.getCatalog(), schema.getSchema());
    }

    private static List<DbObject> objectFolders(String catalog, String schema) {
        List<DbObject> folders = new ArrayList<>();
        folders.add(folder("Tables", Kind.TABLES_FOLDER, catalog, schema));
        folders.add(folder("Views", Kind.VIEWS_FOLDER, catalog, schema));
        folders.add(folder("Procedures", Kind.PROCEDURES_FOLDER, catalog, schema));
        folders.add(folder("Functions", Kind.FUNCTIONS_FOLDER, catalog, schema));
        folders.add(folder("Sequences", Kind.SEQUENCES_FOLDER, catalog, schema));
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

        return switch (dbFolder.getKind()) {
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
    }

    private static List<DbObject> loadTablesOrViews(ConnectionProfile profile, String catalog,
                                                    String schema, String type, Kind kind)
            throws SQLException {
        // PostgreSQL: hide partition child tables from the Tables folder —
        // they appear under their parent table's "partitions" node instead
        if (profile.getType() == DatabaseType.POSTGRESQL && "TABLE".equals(type)) {
            return loadPostgresTablesWithoutPartitions(profile, catalog, schema);
        }

        List<DbObject> result = new ArrayList<>();
        try (Connection conn = client(profile, catalog).getConnection();
             ResultSet rs = conn.getMetaData().getTables(
                     metaCatalog(profile, catalog), schema, "%", new String[]{type})) {
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
     * "Edit Source" console pre-filled with the object's own definition —
     * the same thing double-clicking a stored routine opens in DataGrip.
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
        String sql = switch (profile.getType()) {
            case POSTGRESQL -> "SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = '"
                    + schema + "'";
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

    // ------------------------------------------- table children (DataGrip style)

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
