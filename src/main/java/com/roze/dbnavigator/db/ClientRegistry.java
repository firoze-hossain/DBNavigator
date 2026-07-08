package com.roze.dbnavigator.db;

import com.roze.dbnavigator.model.ConnectionProfile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps one live client per connection profile and closes them on demand. */
public final class ClientRegistry {

    private static final Map<String, JdbcClient> jdbcClients = new ConcurrentHashMap<>();
    private static final Map<String, MongoDbClient> mongoClients = new ConcurrentHashMap<>();

    private ClientRegistry() {}

    public static JdbcClient jdbc(ConnectionProfile profile) {
        return jdbcClients.computeIfAbsent(profile.getId(), id -> new JdbcClient(profile));
    }

    public static MongoDbClient mongo(ConnectionProfile profile) {
        return mongoClients.computeIfAbsent(profile.getId(), id -> new MongoDbClient(profile));
    }

    public static boolean isConnected(ConnectionProfile profile) {
        return jdbcClients.containsKey(profile.getId()) || mongoClients.containsKey(profile.getId());
    }

    public static void disconnect(ConnectionProfile profile) {
        JdbcClient jdbc = jdbcClients.remove(profile.getId());
        if (jdbc != null) jdbc.close();
        MongoDbClient mongo = mongoClients.remove(profile.getId());
        if (mongo != null) mongo.close();
    }

    public static void closeAll() {
        jdbcClients.values().forEach(JdbcClient::close);
        jdbcClients.clear();
        mongoClients.values().forEach(MongoDbClient::close);
        mongoClients.clear();
    }

    /** Opens a client and verifies the server is reachable. Throws on failure. */
    public static void connectAndVerify(ConnectionProfile profile) throws Exception {
        if (profile.getType() == ConnectionProfile.DatabaseType.MONGODB) {
            mongo(profile).ping();
        } else {
            try (var conn = jdbc(profile).getConnection()) {
                conn.isValid(5);
            }
        }
    }
}
