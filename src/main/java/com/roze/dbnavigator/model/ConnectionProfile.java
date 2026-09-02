package com.roze.dbnavigator.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.File;
import java.nio.file.Path;
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
        STRATOSDB("StratosDB", 6582, true),
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

    /**
     * Mirrors DataGrip's own three-way toggle for a MongoDB data source.
     * DEFAULT builds a standard mongodb:// URI from the individual fields;
     * SRV builds a mongodb+srv:// one instead (Atlas and most managed
     * MongoDB hosts hand out an SRV record instead of a fixed host:port —
     * the port is omitted from the URI in this mode because the SRV DNS
     * record supplies the real hosts and ports); URL_ONLY ignores every
     * other connection field and uses {@link #mongoUrlOverride} verbatim,
     * for a full connection string pasted in from elsewhere.
     */
    public enum MongoConnectionType { DEFAULT, SRV, URL_ONLY }

    /**
     * Oracle's classic JDBC "thin" driver supports several distinct URL
     * shapes for the same underlying protocol — this mirrors DataGrip's own
     * "Connection type" dropdown for Oracle. SID is the older, still very
     * common form (e.g. every default Oracle XE install uses SID "XE" —
     * this is the exact case the previous Service-Name-only implementation
     * couldn't express at all). SERVICE_NAME is the modern, PDB/CDB-aware
     * form most current Oracle databases actually expect. TNS accepts
     * either a tnsnames.ora alias or a full descriptor string verbatim.
     * URL_ONLY ignores every other field and uses {@link #oracleConnectString}
     * as the complete jdbc:oracle:... URL.
     */
    public enum OracleConnectionType { SID, SERVICE_NAME, TNS, URL_ONLY }

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
    // MongoDB-only; ignored by every other engine.
    private MongoConnectionType mongoConnectionType = MongoConnectionType.DEFAULT;
    private String replicaSet = "";
    private String readPreference = "";
    private String mongoUrlOverride = "";
    // Oracle-only; ignored by every other engine. Defaults to SERVICE_NAME
    // so existing saved profiles — which only ever knew Service Name —
    // behave exactly as before unless a user explicitly picks something else.
    private OracleConnectionType oracleConnectionType = OracleConnectionType.SERVICE_NAME;
    private String oracleSid = "";
    private String oracleConnectString = "";

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
     *
     * Oracle never honors this override: one Oracle connection always points
     * at the same database/service — there's no per-connection "catalog" to
     * switch the way PostgreSQL/MySQL/SQL Server have. When this is called
     * for an Oracle "console scoped to one schema" (schemas are Oracle's
     * real organizing unit — see MetadataService), the value passed in is a
     * schema name, not an alternate service name/SID, so applying it here
     * exactly like the other engines would silently try to connect to a
     * nonexistent service literally named after the schema. That case is
     * instead handled by JdbcClient calling Connection.setSchema() after
     * connecting — a session-level CURRENT_SCHEMA change, not a new URL.
     */
    @JsonIgnore
    public String getJdbcUrl(String databaseOverride) {
        boolean applyOverride = type != DatabaseType.ORACLE;
        String database = (applyOverride && databaseOverride != null && !databaseOverride.isBlank())
                ? databaseOverride : this.database;
        boolean blank = database == null || database.isBlank();

        return switch (type) {
            // PostgreSQL's protocol always needs a real database name — "postgres"
            // is the near-universal administrative database every installation
            // has, and is exactly what psql itself falls back to by convention.
            case POSTGRESQL -> "jdbc:postgresql://%s:%d/%s%s"
                                   .formatted(host, port, blank ? "postgres" : database, useSsl ? "?ssl=true" : "");
            // StratosDB has no real multiple-databases-per-server concept yet -
            // any non-blank value is accepted without being meaningfully
            // validated server-side (see StratosDriver's own javadoc), so
            // "stratos" here is just a reasonable, real default, not a
            // required administrative database the way Postgres's "postgres"
            // database actually is.
            case STRATOSDB  -> "jdbc:stratos://%s:%d/%s%s"
                                   .formatted(host, port, blank ? "stratos" : database, useSsl ? "?ssl=true" : "");

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
            // SQLite genuinely needs a specific identifier (a file path) —
            // there's no meaningful "default" to substitute, so it still
            // requires a value.
            // Oracle's thin driver understands several distinct URL shapes for
            // the same connection — which one applies depends entirely on how
            // the target database is configured, so this is driven by the
            // explicit connection-type toggle rather than one fixed format.
            case ORACLE -> switch (oracleConnectionType) {
                // A full, ready-to-use URL pasted in from elsewhere — every
                // other Oracle field is ignored, same as Mongo's URL_ONLY.
                case URL_ONLY -> oracleConnectString;
                // Either a tnsnames.ora alias (resolved via the
                // oracle.net.tns_admin system property, if one is
                // configured — this app doesn't manage that file) or a
                // full manually-written (DESCRIPTION=...) connect
                // descriptor, which always works standalone with no
                // external file needed.
                case TNS -> "jdbc:oracle:thin:@" + oracleConnectString;
                // The older, still very common form — this is what every
                // default Oracle XE install actually uses (SID "XE"),
                // which is exactly the case the previous Service-Name-only
                // implementation had no way to express at all.
                case SID -> "jdbc:oracle:thin:@%s:%d:%s"
                                .formatted(host, port, oracleSid.isBlank() ? "XE" : oracleSid);
                // The modern, PDB/CDB-aware form most current Oracle
                // databases expect — this was the only form supported
                // before, and remains the default for existing profiles.
                case SERVICE_NAME -> "jdbc:oracle:thin:@//%s:%d/%s".formatted(host, port, database);
            };
            case SQLITE     -> "jdbc:sqlite:%s".formatted(resolveSqlitePath(database));
            case MONGODB    -> throw new IllegalStateException("MongoDB does not use JDBC");
        };
    }

    /**
     * Stable, per-user folder a bare/relative SQLite filename resolves
     * against. DataGrip can get away with a relative "File:" path because it
     * resolves it against the current *project's* root — a folder that's
     * fixed for as long as you keep opening that same project. This app has
     * no equivalent notion of a project, and the alternative — resolving
     * against the JVM's working directory — depends entirely on how the app
     * happens to be launched (double-click, terminal, an IDE run config all
     * differ, and can even differ between two launches of the exact same
     * shortcut). That's what silently produced several different, empty
     * "identifier.sqlite" files during earlier testing. Anchoring to a fixed
     * folder under the user's home directory instead means one saved
     * connection with a relative name always opens the exact same file,
     * every time, regardless of how the app was started.
     */
    public static Path sqliteHomeDir() {
        return Path.of(System.getProperty("user.home"), ".dbnavigator", "sqlite");
    }

    /**
     * Resolves whatever was typed/browsed into the Database field into the
     * actual file SQLite will open. An absolute path — including anything
     * chosen via the Browse file picker, which always yields one — is used
     * exactly as given, untouched. Only a bare relative name gets anchored,
     * via {@link #sqliteHomeDir()}, instead of being left to resolve against
     * whatever the process's working directory happens to be.
     */
    public static String resolveSqlitePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) return rawPath;
        File asGiven = new File(rawPath);
        if (asGiven.isAbsolute()) return asGiven.getPath();

        Path anchored = sqliteHomeDir().resolve(rawPath).normalize();
        try {
            java.nio.file.Files.createDirectories(anchored.getParent());
        } catch (java.io.IOException ignored) {
            // Best-effort: if this fails, SQLite's own "unable to open
            // database file" error on connect will explain why, which is a
            // clearer signal than swallowing it silently here.
        }
        return anchored.toString();
    }

    /** The actual file this profile's SQLite connection will open — see {@link #resolveSqlitePath}. */
    @JsonIgnore
    public String resolvedSqlitePath() {
        return resolveSqlitePath(database);
    }

    @JsonIgnore
    /**
     * DEFAULT/SRV modes build the URI from the individual fields, same as
     * DataGrip's own "default" and "MongoDB Atlas (SRV protocol)" toggle
     * positions. URL_ONLY mode — DataGrip's "URL only" position — instead
     * uses {@link #mongoUrlOverride} verbatim and ignores every other field
     * here, for a full connection string (e.g. one copied from Atlas)
     * that already encodes auth, replica set, TLS, etc. on its own.
     */
    public String getMongoUri() {
        if (mongoConnectionType == MongoConnectionType.URL_ONLY) {
            return mongoUrlOverride == null ? "" : mongoUrlOverride.trim();
        }

        boolean srv = mongoConnectionType == MongoConnectionType.SRV;
        StringBuilder uri = new StringBuilder(srv ? "mongodb+srv://" : "mongodb://");
        if (username != null && !username.isBlank()) {
            uri.append(urlEncode(username)).append(':')
                    .append(urlEncode(password == null ? "" : password)).append('@');
        }
        uri.append(host);
        // An SRV record already encodes each host's port; a literal port
        // here would conflict with it rather than override it.
        if (!srv) uri.append(':').append(port);
        uri.append('/');
        if (database != null && !database.isBlank()) uri.append(urlEncodePathSegment(database));

        java.util.List<String> params = new java.util.ArrayList<>();
        if (username != null && !username.isBlank()) params.add("authSource=admin");
        if (replicaSet != null && !replicaSet.isBlank()) params.add("replicaSet=" + urlEncode(replicaSet));
        if (readPreference != null && !readPreference.isBlank()) params.add("readPreference=" + readPreference);
        if (!params.isEmpty()) uri.append('?').append(String.join("&", params));
        return uri.toString();
    }

    private static String urlEncodePathSegment(String s) {
        // Path segments use the same percent-encoding as query values, except
        // a literal "/" would otherwise be encoded and break the path itself.
        return urlEncode(s).replace("%2F", "/");
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }

    @JsonIgnore
    public String getSummary() {
        if (type == DatabaseType.SQLITE) return resolvedSqlitePath();
        if (type == DatabaseType.ORACLE) return switch (oracleConnectionType) {
            case URL_ONLY -> oracleConnectString;
            case TNS -> host + ":" + port + " @" + oracleConnectString;
            case SID -> host + ":" + port + ":" + (oracleSid.isBlank() ? "XE" : oracleSid);
            case SERVICE_NAME -> host + ":" + port
                    + (database == null || database.isBlank() ? "" : "/" + database);
        };
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
        c.mongoConnectionType = mongoConnectionType;
        c.replicaSet = replicaSet;
        c.readPreference = readPreference;
        c.mongoUrlOverride = mongoUrlOverride;
        c.oracleConnectionType = oracleConnectionType;
        c.oracleSid = oracleSid;
        c.oracleConnectString = oracleConnectString;
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
    public MongoConnectionType getMongoConnectionType() { return mongoConnectionType; }
    public void setMongoConnectionType(MongoConnectionType mongoConnectionType) {
        this.mongoConnectionType = mongoConnectionType == null
                ? MongoConnectionType.DEFAULT : mongoConnectionType;
    }
    public String getReplicaSet() { return replicaSet; }
    public void setReplicaSet(String replicaSet) { this.replicaSet = replicaSet == null ? "" : replicaSet; }
    public String getReadPreference() { return readPreference; }
    public void setReadPreference(String readPreference) {
        this.readPreference = readPreference == null ? "" : readPreference;
    }
    public String getMongoUrlOverride() { return mongoUrlOverride; }
    public void setMongoUrlOverride(String mongoUrlOverride) {
        this.mongoUrlOverride = mongoUrlOverride == null ? "" : mongoUrlOverride;
    }
    public OracleConnectionType getOracleConnectionType() { return oracleConnectionType; }
    public void setOracleConnectionType(OracleConnectionType oracleConnectionType) {
        this.oracleConnectionType = oracleConnectionType == null
                ? OracleConnectionType.SERVICE_NAME : oracleConnectionType;
    }
    public String getOracleSid() { return oracleSid; }
    public void setOracleSid(String oracleSid) { this.oracleSid = oracleSid == null ? "" : oracleSid; }
    public String getOracleConnectString() { return oracleConnectString; }
    public void setOracleConnectString(String oracleConnectString) {
        this.oracleConnectString = oracleConnectString == null ? "" : oracleConnectString;
    }

    @Override public String toString() { return name; }
}
