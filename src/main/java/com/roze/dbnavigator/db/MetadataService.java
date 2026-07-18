package com.roze.dbnavigator.db;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.DbObject.Kind;
import com.roze.dbnavigator.model.QueryResult;

import java.sql.*;
import java.util.*;

/** Loads schema tree children lazily for both JDBC and MongoDB connections. */
public final class MetadataService {

    private MetadataService() {}

    private static final Set<String> PG_SYSTEM_SCHEMAS =
            Set.of("pg_catalog", "information_schema", "pg_toast");

    private static JdbcClient client(ConnectionProfile profile, String catalog) {
        return ClientRegistry.jdbc(profile, catalog);
    }

    /** PostgreSQL's JDBC metadata calls ignore/reject foreign catalogs — pass null. */
    private static String metaCatalog(ConnectionProfile profile, String catalog) {
        return profile.getType() == DatabaseType.POSTGRESQL ? null : catalog;
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
            default -> List.of();
        };
    }

    /** True when this engine shows DATABASE nodes that can be filtered. */
    public static boolean supportsDatabaseFilter(ConnectionProfile profile) {
        return switch (profile.getType()) {
            case POSTGRESQL, MYSQL, MARIADB, MONGODB -> true;
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
            case SQLSERVER, ORACLE -> {
                try (Connection conn = client(profile, null).getConnection()) {
                    yield loadSchemas(conn, profile, null);
                }
            }
            case SQLITE -> objectFolders(null, null);
            default -> List.of();
        };
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
            case POSTGRESQL -> {
                // schemas of that particular database (separate physical connection)
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
            case INDEXES_FOLDER    -> loadIndexes(profile, dbFolder);
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
    public static List<DbObject> loadTableChildren(ConnectionProfile profile, DbObject table)
            throws SQLException {
        List<DbObject> result = new ArrayList<>();

        int columnCount = 0;
        Set<String> indexNames = new LinkedHashSet<>();
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
        }

        DbObject columns = childFolder("columns", Kind.COLUMNS_FOLDER, table);
        columns.setDetail(String.valueOf(columnCount));
        result.add(columns);

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
                    String type = rs.getString("TYPE_NAME");
                    boolean nullable = "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
                    col.setDetail(type.toLowerCase()
                            + (primaryKeys.contains(name) ? "  PK" : nullable ? "" : "  not null"));
                    result.add(col);
                }
            }
        }
        return result;
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
                    result.getRows().add(new ArrayList<>(List.of(
                            name,
                            String.valueOf(rs.getString("TYPE_NAME")),
                            String.valueOf(rs.getInt("COLUMN_SIZE")),
                            "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")) ? "YES" : "NO",
                            String.valueOf(rs.getString("COLUMN_DEF")),
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
                     metaCatalog(profile, catalog), null, "%", new String[]{"TABLE", "VIEW"})) {
            while (rs.next()) {
                String schema = rs.getString("TABLE_SCHEM");
                if (schema != null && PG_SYSTEM_SCHEMAS.contains(schema)) continue;
                tables.add(rs.getString("TABLE_NAME"));
            }
        } catch (SQLException ignored) {}
        return tables;
    }

    /** Distinct column names of every user table — global autocomplete pool. */
    public static List<String> listAllColumns(ConnectionProfile profile, String catalog) {
        Set<String> columns = new LinkedHashSet<>();
        try (Connection conn = client(profile, catalog).getConnection();
             ResultSet rs = conn.getMetaData().getColumns(
                     metaCatalog(profile, catalog), null, "%", "%")) {
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
                     metaCatalog(profile, catalog), null, table, "%")) {
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
                     metaCatalog(profile, catalog), null, "%" + query + "%",
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
