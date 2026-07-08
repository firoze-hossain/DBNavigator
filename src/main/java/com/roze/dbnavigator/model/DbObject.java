package com.roze.dbnavigator.model;

/** A node in the schema tree: a connection, database, schema, folder, table, etc. */
public class DbObject {

    public enum Kind {
        CONNECTION, DATABASE, SCHEMA,
        TABLES_FOLDER, VIEWS_FOLDER, PROCEDURES_FOLDER, FUNCTIONS_FOLDER,
        SEQUENCES_FOLDER, COLLECTIONS_FOLDER,
        TABLE, VIEW, PROCEDURE, FUNCTION, SEQUENCE, COLLECTION, COLUMN,
        MESSAGE
    }

    private final String name;
    private final Kind kind;
    private final String catalog;   // database name (nullable)
    private final String schema;    // schema name (nullable)
    private String detail;          // e.g. column type, row count
    private boolean loaded;

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
    public String getDetail()  { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public boolean isLoaded()  { return loaded; }
    public void setLoaded(boolean loaded) { this.loaded = loaded; }

    /** Fully qualified name for use in SQL. */
    public String qualifiedName() {
        StringBuilder sb = new StringBuilder();
        if (schema != null && !schema.isBlank()) {
            sb.append(quote(schema)).append('.');
        } else if (catalog != null && !catalog.isBlank()) {
            // MySQL/MariaDB qualify by catalog (database) instead of schema
            sb.append(quote(catalog)).append('.');
        }
        sb.append(quote(name));
        return sb.toString();
    }

    private static String quote(String ident) {
        // Quote only when necessary to keep SQL readable
        if (ident.matches("[A-Za-z_][A-Za-z0-9_]*")) return ident;
        return '"' + ident.replace("\"", "\"\"") + '"';
    }

    @Override public String toString() { return name; }
}
