package com.roze.dbnavigator.model;

/** A node in the schema tree: a connection, database, schema, folder, table, etc. */
public class DbObject {

    public enum Kind {
        CONNECTION, DATABASE, SCHEMA,
        TABLES_FOLDER, VIEWS_FOLDER, PROCEDURES_FOLDER, FUNCTIONS_FOLDER,
        SEQUENCES_FOLDER, COLLECTIONS_FOLDER,
        COLUMNS_FOLDER, INDEXES_FOLDER, PARTITIONS_FOLDER, KEYS_FOLDER, FOREIGN_KEYS_FOLDER, FIELDS_FOLDER,
        TABLE, VIEW, PROCEDURE, FUNCTION, SEQUENCE, COLLECTION,
        COLUMN, INDEX, PARTITION, KEY, FOREIGN_KEY, FIELD,
        MESSAGE
    }

    private final String name;
    private final Kind kind;
    private final String catalog;   // database name (nullable)
    private final String schema;    // schema name (nullable)
    private String tableName;       // owning table for COLUMN/INDEX/folder nodes (nullable)
    private String detail;          // e.g. column type, child count
    private boolean loaded;
    /**
     * True only for engines where {@code catalog} is used purely to pick
     * the right per-database connection (see ClientRegistry.jdbc), not as
     * a real, valid part of a qualified SQL table reference. StratosDB is
     * the real, current example: unlike MySQL/MariaDB, its own SQL
     * grammar has no {@code catalog.table} syntax at all - a real,
     * multi-database StratosCluster achieves per-database isolation
     * entirely through a separate connection per database, not through
     * schema/catalog-qualified references within a single query. Without
     * this flag, {@link #qualifiedName()} would build a real, syntactically
     * invalid reference like {@code mydb.employees} for a StratosDB table
     * that already has the right, single database selected via its own
     * dedicated connection.
     */
    private boolean skipCatalogQualifier;

    public DbObject(String name, Kind kind) {
        this(name, kind, null, null);
    }

    public DbObject(String name, Kind kind, String catalog, String schema) {
        this.name = name;
        this.kind = kind;
        this.catalog = catalog;
        this.schema = schema;
    }

    public String getName()    { return name; }
    public Kind getKind()      { return kind; }
    public String getCatalog() { return catalog; }
    public String getSchema()  { return schema; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getDetail()  { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public boolean isLoaded()  { return loaded; }
    public void setLoaded(boolean loaded) { this.loaded = loaded; }
    public void setSkipCatalogQualifier(boolean skipCatalogQualifier) { this.skipCatalogQualifier = skipCatalogQualifier; }

    /** Fully qualified name for use in SQL. */
    public String qualifiedName() {
        StringBuilder sb = new StringBuilder();
        if (schema != null && !schema.isBlank()) {
            sb.append(quote(schema)).append('.');
        } else if (catalog != null && !catalog.isBlank() && !skipCatalogQualifier) {
            // MySQL/MariaDB qualify by catalog (database) instead of schema
            sb.append(quote(catalog)).append('.');
        }
        sb.append(quote(name));
        return sb.toString();
    }

    public static String quote(String ident) {
        // Quote only when necessary to keep SQL readable
        if (ident.matches("[A-Za-z_][A-Za-z0-9_]*")) return ident;
        return '"' + ident.replace("\"", "\"\"") + '"';
    }

    @Override public String toString() { return name; }
}
