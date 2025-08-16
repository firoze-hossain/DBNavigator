package com.roze.dbnavigator.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.UUID;

@Data
public class ConnectionProfile {
    private String id = UUID.randomUUID().toString();
    private String name;
    private DatabaseType type;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private boolean savePassword;
    private String color;
    private boolean autoConnect;
    // Make sure to include id in your copy constructor
    public ConnectionProfile copy() {
        ConnectionProfile copy = new ConnectionProfile();
        copy.setId(this.id);
        // ... copy other fields ...
        return copy;
    }
    @JsonIgnore
    public String getJdbcUrl() {
        return switch (type) {
            case MYSQL -> String.format("jdbc:mysql://%s:%d/%s?useSSL=false", host, port, database);
            case POSTGRESQL -> {
                String baseUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
                // Add SSL and other PostgreSQL-specific parameters
                yield baseUrl + "?ssl=false&sslmode=disable";
            }
            case SQLITE -> String.format("jdbc:sqlite:%s", database);
            default -> throw new IllegalArgumentException("Unsupported database type");
        };
    }

    public enum DatabaseType {
        MYSQL("MySQL", "mysql.png"),
        POSTGRESQL("PostgreSQL", "postgresql.png"),
        SQLITE("SQLite", "sqlite.png");

        private final String displayName;
        private final String icon;

        DatabaseType(String displayName, String icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIcon() {
            return icon;
        }
    }
}