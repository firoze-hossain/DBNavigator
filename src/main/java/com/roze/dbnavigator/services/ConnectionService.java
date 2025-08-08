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
            createConnectionPool(profile);
        }

        return connectionPools.get(poolKey).getConnection();
    }

    public static Connection testConnection(ConnectionProfile profile) throws SQLException {
        // Create a temporary connection without pooling for testing
        HikariConfig config = createConfig(profile);
        config.setMaximumPoolSize(1); // Minimal pool for testing
        config.setConnectionTimeout(5000); // Shorter timeout for tests

        try (HikariDataSource tempDataSource = new HikariDataSource(config)) {
            return tempDataSource.getConnection();
        }
    }

    public static void saveConnection(ConnectionProfile profile) {
        // Create a copy of the profile for saving
        ConnectionProfile profileToSave = new ConnectionProfile();
        // Copy all fields except password if not saving
        profileToSave.setName(profile.getName());
        profileToSave.setType(profile.getType());
        profileToSave.setHost(profile.getHost());
        profileToSave.setPort(profile.getPort());
        profileToSave.setDatabase(profile.getDatabase());
        profileToSave.setUsername(profile.getUsername());
        profileToSave.setSavePassword(profile.isSavePassword());
        profileToSave.setColor(profile.getColor());
        profileToSave.setAutoConnect(profile.isAutoConnect());

        if (profile.isSavePassword()) {
            profileToSave.setPassword(profile.getPassword());
        }

        savedConnections.removeIf(c -> c.getId().equals(profile.getId()));
        savedConnections.add(profileToSave);
        persistConnections();
    }

    private static void createConnectionPool(ConnectionProfile profile) {
        HikariConfig config = createConfig(profile);
        connectionPools.put(profile.getId(), new HikariDataSource(config));
    }

    private static HikariConfig createConfig(ConnectionProfile profile) {
        // Initial debug output
        System.out.println("\n=== DEBUG: Creating HikariConfig ===");
        System.out.println("Connection Profile Details:");
        System.out.println("  Name: " + profile.getName());
        System.out.println("  Type: " + profile.getType());
        System.out.println("  Host: " + profile.getHost());
        System.out.println("  Port: " + profile.getPort());
        System.out.println("  Database: " + profile.getDatabase());
        System.out.println("  Username: " + profile.getUsername());

        // Password debug information
        String passwordStatus;
        if (profile.getPassword() == null) {
            passwordStatus = "NULL";
        } else if (profile.getPassword().isEmpty()) {
            passwordStatus = "EMPTY STRING";
        } else {
            passwordStatus = "PRESENT (length: " + profile.getPassword().length() + ")";
        }
        System.out.println("  Password: " + passwordStatus);

        // Create config
        HikariConfig config = new HikariConfig();
        System.out.println("\nSetting basic configuration:");
        config.setJdbcUrl(profile.getJdbcUrl());
        System.out.println("  JDBC URL: " + profile.getJdbcUrl());
        config.setUsername(profile.getUsername());
        System.out.println("  Username: " + profile.getUsername());

        // Password handling with debug
        System.out.println("\nProcessing password:");
        if (profile.getPassword() != null && !profile.getPassword().isEmpty()) {
            config.setPassword(profile.getPassword());
            System.out.println("  Setting password (length: " + profile.getPassword().length() + ")");
        } else {
            System.out.println("  No password or empty password detected");
            if (profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL) {
                System.out.println("  PostgreSQL detected - adding empty password property");
                config.addDataSourceProperty("password", "");
                config.addDataSourceProperty("allowEmptyPassword", "true");
            }
        }

        // Pool configuration
        System.out.println("\nSetting pool configuration:");
        config.setMaximumPoolSize(5);
        System.out.println("  Max pool size: 5");
        config.setConnectionTimeout(30000);
        System.out.println("  Connection timeout: 30000ms");
        config.setPoolName("DBNavigator-" + profile.getName());
        System.out.println("  Pool name: DBNavigator-" + profile.getName());

        // Database-specific configuration
        System.out.println("\nSetting database-specific configuration:");
        if (profile.getType() == ConnectionProfile.DatabaseType.POSTGRESQL) {
            System.out.println("  PostgreSQL-specific settings:");
            config.addDataSourceProperty("preparedStatementCacheQueries", "250");
            System.out.println("    preparedStatementCacheQueries: 250");
            config.addDataSourceProperty("ssl", "false");
            System.out.println("    SSL: false");
            config.addDataSourceProperty("allowEmptyPassword", "true");
            System.out.println("    allowEmptyPassword: true");
        } else if (profile.getType() == ConnectionProfile.DatabaseType.MYSQL) {
            System.out.println("  MySQL-specific settings:");
            config.addDataSourceProperty("cachePrepStmts", "true");
            System.out.println("    cachePrepStmts: true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            System.out.println("    prepStmtCacheSize: 250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            System.out.println("    prepStmtCacheSqlLimit: 2048");
        }

        System.out.println("=== DEBUG: Configuration complete ===\n");
        return config;
    }

    public static Connection testPostgreSQLConnection(ConnectionProfile profile) throws SQLException {
        HikariConfig config = createConfig(profile);
        config.setMaximumPoolSize(1);
        config.setConnectionTimeout(5000);

        // Additional PostgreSQL-specific settings for testing
        config.addDataSourceProperty("loginTimeout", "5");
        config.addDataSourceProperty("socketTimeout", "5");

        try (HikariDataSource tempDataSource = new HikariDataSource(config)) {
            return tempDataSource.getConnection();
        }
    }

    private static void persistConnections() {
        // Save to JSON file
    }

    public static List<ConnectionProfile> getSavedConnections() {
        return Collections.unmodifiableList(savedConnections);
    }
}