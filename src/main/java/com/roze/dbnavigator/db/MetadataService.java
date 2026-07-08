package com.roze.dbnavigator.db;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;
import com.roze.dbnavigator.model.DbObject;
import com.roze.dbnavigator.model.DbObject.Kind;
import com.roze.dbnavigator.model.QueryResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Loads schema tree children lazily for both JDBC and MongoDB connections. */
public final class MetadataService {

    private MetadataService() {}

    private static final Set<String> PG_SYSTEM_SCHEMAS =
            Set.of("pg_catalog", "information_schema", "pg_toast");

    /** Children of the connection root node. */
    public static List<DbObject> loadTopLevel(ConnectionProfile profile) throws Exception {
        if (profile.getType() == DatabaseType.MONGODB) {
            List<DbObject> dbs = new ArrayList<>();
            for (String name : ClientRegistry.mongo(profile).listDatabases()) {
                dbs.add(new DbObject(name, Kind.DATABASE, name, null));
            }
            return dbs;
        }

        JdbcClient client = ClientRegistry.jdbc(profile);
        try (Connection conn = client.getConnection()) {
            return switch (profile.getType()) {
                case POSTGRESQL, ORACLE -> loadSchemas(conn, profile);
                case MYSQL, MARIADB     -> loadCatalogs(conn);
                case SQLSERVER          -> loadSchemas(conn, profile);
                case SQLITE             -> objectFolders(null, null);
                default -> List.of();
            };
        }
    }

    private static List<DbObject> loadCatalogs(Connection conn) throws SQLException {
        List<DbObject> result = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getCatalogs()) {
            while (rs.next()) {
                String catalog = rs.getString("TABLE_CAT");
                result.add(new DbObject(catalog, Kind.DATABASE, catalog, null));
            }
        }
        return result;
    }

    private static List<DbObject> loadSchemas(Connection conn, ConnectionProfile profile) throws SQLException {
        List<DbObject> result = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getSchemas()) {
            while (rs.next()) {
                String schema = rs.getString("TABLE_SCHEM");
                if (profile.getType() == DatabaseType.POSTGRESQL
                        && PG_SYSTEM_SCHEMAS.contains(schema)) continue;
                result.add(new DbObject(schema, Kind.SCHEMA, null, schema));
            }
        }
        return result;
    }

    /** Children of a DATABASE node (MySQL catalog or Mongo database). */
    public static List<DbObject> loadDatabaseChildren(ConnectionProfile profile, DbObject database)
            throws Exception {
        if (profile.getType() == DatabaseType.MONGODB) {
            return List.of(folder("Collections", Kind.COLLECTIONS_FOLDER, database.getCatalog(), null));
        }
        // MySQL/MariaDB: catalog acts as schema
        return objectFolders(database.getCatalog(), null);
    }

    /** Children of a SCHEMA node. */
    public static List<DbObject> loadSchemaChildren(DbObject schema) {
        return objectFolders(null, schema.getSchema());
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

    /** Children of a folder node (Tables, Views, Collections, ...). */
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
            default -> List.of();
        };
    }

    private static List<DbObject> loadTablesOrViews(ConnectionProfile profile, String catalog,
                                                    String schema, String type, Kind kind)
            throws SQLException {
        List<DbObject> result = new ArrayList<>();
        try (Connection conn = ClientRegistry.jdbc(profile).getConnection();
             ResultSet rs = conn.getMetaData().getTables(catalog, schema, "%", new String[]{type})) {
            while (rs.next()) {
                result.add(new DbObject(rs.getString("TABLE_NAME"), kind, catalog, schema));
            }
        }
        return result;
    }

    private static List<DbObject> loadRoutines(ConnectionProfile profile, String catalog,
                                               String schema, boolean procedures)
            throws SQLException {
        List<DbObject> result = new ArrayList<>();
        try (Connection conn = ClientRegistry.jdbc(profile).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = procedures
                    ? meta.getProcedures(catalog, schema, "%")
                    : meta.getFunctions(catalog, schema, "%");
            try (rs) {
                String nameCol = procedures ? "PROCEDURE_NAME" : "FUNCTION_NAME";
                while (rs.next()) {
                    result.add(new DbObject(rs.getString(nameCol),
                            procedures ? Kind.PROCEDURE : Kind.FUNCTION, catalog, schema));
                }
            }
        } catch (SQLException e) {
            // Some drivers (e.g. SQLite) don't support routines — return empty rather than fail
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
        try (Connection conn = ClientRegistry.jdbc(profile).getConnection();
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

    /** Column definitions of a table — used by the structure viewer. */
    public static QueryResult loadTableStructure(ConnectionProfile profile, DbObject table)
            throws SQLException {
        QueryResult result = new QueryResult();
        result.getColumns().addAll(List.of("Column", "Type", "Size", "Nullable", "Default", "Key"));

        try (Connection conn = ClientRegistry.jdbc(profile).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();

            List<String> primaryKeys = new ArrayList<>();
            try (ResultSet pk = meta.getPrimaryKeys(table.getCatalog(), table.getSchema(), table.getName())) {
                while (pk.next()) primaryKeys.add(pk.getString("COLUMN_NAME"));
            }

            try (ResultSet rs = meta.getColumns(table.getCatalog(), table.getSchema(), table.getName(), "%")) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    result.getRows().add(List.of(
                            name,
                            String.valueOf(rs.getString("TYPE_NAME")),
                            String.valueOf(rs.getInt("COLUMN_SIZE")),
                            "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")) ? "YES" : "NO",
                            String.valueOf(rs.getString("COLUMN_DEF")),
                            primaryKeys.contains(name) ? "PK" : ""
                    ));
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

        try (Connection conn = ClientRegistry.jdbc(profile).getConnection();
             ResultSet rs = conn.getMetaData().getIndexInfo(
                     table.getCatalog(), table.getSchema(), table.getName(), false, true)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if (indexName == null) continue;
                result.getRows().add(List.of(
                        indexName,
                        String.valueOf(rs.getString("COLUMN_NAME")),
                        rs.getBoolean("NON_UNIQUE") ? "NO" : "YES",
                        indexType(rs.getShort("TYPE"))
                ));
            }
        }
        return result;
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
