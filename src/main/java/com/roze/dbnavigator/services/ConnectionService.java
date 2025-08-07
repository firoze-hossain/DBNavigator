package com.roze.dbnavigator.services;


import com.roze.dbnavigator.models.ConnectionProfile;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class ConnectionService {
    private static final Map<String, HikariDataSource> connectionPools = new HashMap<>();
    private static final List<ConnectionProfile> savedConnections = new ArrayList<>();

    public static synchronized Connection getConnection(ConnectionProfile profile) throws SQLException {
        String poolKey = profile.getId();

        if (!connectionPools.containsKey(poolKey)) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(profile.getJdbcUrl());
            config.setUsername(profile.getUsername());
            config.setPassword(profile.getPassword());
            config.setMaximumPoolSize(5);
            config.setConnectionTimeout(30000);
            config.setPoolName("DBNavigator-" + profile.getName());

            // Database-specific optimizations
            switch (profile.getType()) {
                case MYSQL:
                    config.addDataSourceProperty("cachePrepStmts", "true");
                    config.addDataSourceProperty("prepStmtCacheSize", "250");
                    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                    break;
                case POSTGRESQL:
                    config.addDataSourceProperty("preparedStatementCacheQueries", "250");
                    break;
            }

            connectionPools.put(poolKey, new HikariDataSource(config));
        }

        return connectionPools.get(poolKey).getConnection();
    }

    public static void saveConnection(ConnectionProfile profile) {
        // Encrypt password if saving
        if (!profile.isSavePassword()) {
            profile.setPassword(null);
        }
        savedConnections.removeIf(c -> c.getId().equals(profile.getId()));
        savedConnections.add(profile);
        persistConnections();
    }

    private static void persistConnections() {
        // Save to JSON file
    }

    public static List<ConnectionProfile> getSavedConnections() {
        return Collections.unmodifiableList(savedConnections);
    }
}