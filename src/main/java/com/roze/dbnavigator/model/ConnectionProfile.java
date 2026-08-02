package com.roze.dbnavigator.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

/** A saved database connection. Serialized to ~/.dbnavigator/connections.json */
public class ConnectionProfile {

    public enum DatabaseType {
        MYSQL("MySQL", 3306, true),
        MARIADB("MariaDB", 3306, true),
        POSTGRESQL("PostgreSQL", 5432, true),
        SQLSERVER("SQL Server", 1433, true),
        ORACLE("Oracle", 1521, true),
        SQLITE("SQLite", 0, true),
        MONGODB("MongoDB", 27017, false);

        private final String displayName;
        private final int defaultPort;
        private final boolean relational;

        DatabaseType(String displayName, int defaultPort, boolean relational) {
            this.displayName = displayName;
            this.defaultPort = defaultPort;
            this.relational = relational;
        }

        public String getDisplayName() { return displayName; }
        public int getDefaultPort()    { return defaultPort; }
        public boolean isRelational()  { return relational; }

        @Override public String toString() { return displayName; }
    }

    private String id = UUID.randomUUID().toString();
    private String name = "";
    private DatabaseType type = DatabaseType.POSTGRESQL;
    private String host = "localhost";
    private int port;
    private String database = "";     // for SQLite this is the file path
    private String username = "";
    private String password = "";
    private boolean savePassword;
    private boolean useSsl;
    /** Databases shown under this connection; empty = show all. */
    private java.util.List<String> visibleDatabases = new java.util.ArrayList<>();

    public ConnectionProfile() {}

    @JsonIgnore
    public String getJdbcUrl() {
        return getJdbcUrl(null);
    }

    /**
     * JDBC URL, optionally pointing at a different database on the same server.
     * Used to browse every database under one PostgreSQL/SQL Server connection.
     *
     * The Database field is optional when creating a connection — most engines
     * don't actually require picking one upfront, since the schema tree lets
     * you browse every database on the server anyway. Each engine's own
     * convention for "no database specified" is used rather than passing an
     * empty string straight into the URL, which several drivers/servers
     * reject outright as an invalid database name.
     */
    @JsonIgnore
    public String getJdbcUrl(String databaseOverride) {
        String database = (databaseOverride != null && !databaseOverride.isBlank())
                ? databaseOverride : this.database;
        boolean blank = database == null || database.isBlank();

        return switch (type) {
            // PostgreSQL's protocol always needs a real database name — "postgres"
            // is the near-universal administrative database every installation
            // has, and is exactly what psql itself falls back to by convention.
            case POSTGRESQL -> "jdbc:postgresql://%s:%d/%s%s"
                                   .formatted(host, port, blank ? "postgres" : database, useSsl ? "?ssl=true" : "");
            // MySQL/MariaDB can omit the database segment entirely and still
            // connect — you just won't have a schema selected until you USE one.
            case MYSQL      -> blank
                    ? "jdbc:mysql://%s:%d?useSSL=%s&allowPublicKeyRetrieval=true&serverTimezone=UTC"
                          .formatted(host, port, useSsl)
                    : "jdbc:mysql://%s:%d/%s?useSSL=%s&allowPublicKeyRetrieval=true&serverTimezone=UTC"
                          .formatted(host, port, database, useSsl);
            case MARIADB    -> blank
                    ? "jdbc:mariadb://%s:%d".formatted(host, port)
                    : "jdbc:mariadb://%s:%d/%s".formatted(host, port, database);
            // SQL Server: simply omit databaseName= and it connects to the
            // login's own default database.
            case SQLSERVER  -> blank
                    ? "jdbc:sqlserver://%s:%d;encrypt=%s;trustServerCertificate=true".formatted(host, port, useSsl)
                    : "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=%s;trustServerCertificate=true"
                          .formatted(host, port, database, useSsl);
            // Oracle and SQLite genuinely need a specific identifier (a
            // service name/SID, or a file path) — there's no meaningful
            // "default" to substitute, so these still require a value.
            case ORACLE     -> "jdbc:oracle:thin:@//%s:%d/%s".formatted(host, port, database);
            case SQLITE     -> "jdbc:sqlite:%s".formatted(database);
            case MONGODB    -> throw new IllegalStateException("MongoDB does not use JDBC");
        };
    }

    @JsonIgnore
    public String getMongoUri() {
        if (username != null && !username.isBlank()) {
            return "mongodb://%s:%s@%s:%d/?authSource=admin".formatted(
                    urlEncode(username), urlEncode(password == null ? "" : password), host, port);
        }
        return "mongodb://%s:%d".formatted(host, port);
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }

    @JsonIgnore
    public String getSummary() {
        if (type == DatabaseType.SQLITE) return database;
        return host + ":" + port + (database == null || database.isBlank() ? "" : "/" + database);
    }

    public ConnectionProfile copy() {
        ConnectionProfile c = new ConnectionProfile();
        c.id = id;
        c.name = name;
        c.type = type;
        c.host = host;
        c.port = port;
        c.database = database;
        c.username = username;
        c.password = password;
        c.savePassword = savePassword;
        c.useSsl = useSsl;
        c.visibleDatabases = new java.util.ArrayList<>(visibleDatabases);
        return c;
    }

    // Getters / setters (Jackson needs these)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public DatabaseType getType() { return type; }
    public void setType(DatabaseType type) { this.type = type; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isSavePassword() { return savePassword; }
    public void setSavePassword(boolean savePassword) { this.savePassword = savePassword; }
    public boolean isUseSsl() { return useSsl; }
    public void setUseSsl(boolean useSsl) { this.useSsl = useSsl; }
    public java.util.List<String> getVisibleDatabases() { return visibleDatabases; }
    public void setVisibleDatabases(java.util.List<String> visibleDatabases) {
        this.visibleDatabases = visibleDatabases == null
                ? new java.util.ArrayList<>() : visibleDatabases;
    }

    @Override public String toString() { return name; }
}
