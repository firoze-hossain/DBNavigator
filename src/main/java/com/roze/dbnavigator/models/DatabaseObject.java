package com.roze.dbnavigator.models;

import javafx.scene.image.Image;
import lombok.Data;

@Data
public class DatabaseObject {
    private String name;
    private Type type;
    private String schema;
    private String additionalInfo;
    private transient Image icon;

    // Add these new properties
    private boolean loaded = false;
    private int childCount = 0;


    public DatabaseObject(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public DatabaseObject(String name, Type type, String schema) {
        this(name, type);
        this.schema = schema;
    }

    public enum Type {
        // Server level items
        SERVER("Server"),
        DATABASE("Database"),

        // Schema level items
        SCHEMA("Schema"),
        SCHEMAS_FOLDER("Schemas"),

        // Table related
        TABLES_FOLDER("Tables"),
        TABLE("Table"),
        COLUMNS_FOLDER("Columns"),
        COLUMN("Column"),
        INDEXES_FOLDER("Indexes"),
        INDEX("Index"),

        // Other objects
        VIEWS_FOLDER("Views"),
        VIEW("View"),
        PROCEDURES_FOLDER("Procedures"),
        PROCEDURE("Procedure"),
        FUNCTIONS_FOLDER("Functions"),
        FUNCTION("Function"),
        SEQUENCES_FOLDER("Sequences"),
        SEQUENCE("Sequence"),

        // System folders
        SYSTEM_FOLDER("System"),
        SECURITY_FOLDER("Security"),
        USER("User"),
        ROLE("Role");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isFolder() {
            return name().endsWith("_FOLDER");
        }
    }

    @Override
    public String toString() {
        if (type.isFolder() && childCount > 0) {
            return String.format("%s (%d)", name, childCount);
        }
        return name;
    }

    public String getSchema() {
        if (type == Type.SCHEMA) {
            return name;
        }
        return schema;
    }
    public String getFullName() {
        return schema != null ? schema + "." + name : name;
    }
}