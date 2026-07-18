package com.roze.dbnavigator.db;

import com.roze.dbnavigator.model.ConnectionProfile;
import com.roze.dbnavigator.model.ConnectionProfile.DatabaseType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps live clients per connection profile (and per database for PostgreSQL). */
public final class ClientRegistry {

    private static final Map<String, JdbcClient> jdbcClients = new ConcurrentHashMap<>();
    private static final Map<String, MongoDbClient> mongoClients = new ConcurrentHashMap<>();

    private ClientRegistry() {}

    public static JdbcClient jdbc(ConnectionProfile profile) {
        return jdbc(profile, null);
    }

    /**
     * Client for a specific database on the profile's server. Only PostgreSQL
     * needs a distinct physical connection per database; every other engine
     * reaches all its catalogs through the default connection.
     */
    public static JdbcClient jdbc(ConnectionProfile profile, String catalog) {
        boolean perCatalog = catalog != null && !catalog.isBlank()
                && profile.getType() == DatabaseType.POSTGRESQL
                && !catalog.equals(profile.getDatabase());
        String key = perCatalog ? profile.getId() + "::" + catalog : profile.getId();
        String override = perCatalog ? catalog : null;
        return jdbcClients.computeIfAbsent(key, k -> new JdbcClient(profile, override));
    }

    public static MongoDbClient mongo(ConnectionProfile profile) {
        return mongoClients.computeIfAbsent(profile.getId(), id -> new MongoDbClient(profile));
    }

    public static boolean isConnected(ConnectionProfile profile) {
        return jdbcClients.containsKey(profile.getId()) || mongoClients.containsKey(profile.getId());
    }

    public static void disconnect(ConnectionProfile profile) {
        // Close the default client and every per-database client of this profile
        jdbcClients.entrySet().removeIf(e -> {
            if (e.getKey().equals(profile.getId()) || e.getKey().startsWith(profile.getId() + "::")) {
                e.getValue().close();
                return true;
            }
            return false;
        });
        MongoDbClient mongo = mongoClients.remove(profile.getId());
        if (mongo != null) mongo.close();
    }

    /**
     * Closes only the pool for one database on this connection (e.g. before
     * DROP DATABASE), leaving the rest of the connection's other pools open.
     * Uses the same key logic as {@link #jdbc(ConnectionProfile, String)} so a
     * database matching the profile's own default database closes the default
     * pool rather than a nonexistent per-catalog one.
     */
    public static void disconnectCatalog(ConnectionProfile profile, String catalog) {
        boolean isDefault = catalog == null || catalog.isBlank() || catalog.equals(profile.getDatabase());
        String key = isDefault ? profile.getId() : profile.getId() + "::" + catalog;
        JdbcClient client = jdbcClients.remove(key);
        if (client != null) client.close();
    }

    public static void closeAll() {
        jdbcClients.values().forEach(JdbcClient::close);
        jdbcClients.clear();
        mongoClients.values().forEach(MongoDbClient::close);
        mongoClients.clear();
    }

    /** Opens a client and verifies the server is reachable. Throws on failure. */
    public static void connectAndVerify(ConnectionProfile profile) throws Exception {
        if (profile.getType() == DatabaseType.MONGODB) {
            mongo(profile).ping();
        } else {
            try (var conn = jdbc(profile).getConnection()) {
                conn.isValid(5);
            }
        }
    }
}
