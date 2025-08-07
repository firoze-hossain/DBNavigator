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

    public DatabaseObject(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public DatabaseObject(String name, Type type, String schema) {
        this(name, type);
        this.schema = schema;
    }

    public enum Type {
        DATABASE("Database"),
        SCHEMA("Schema"),
        TABLE("Table"),
        VIEW("View"),
        PROCEDURE("Procedure"),
        FUNCTION("Function"),
        FOLDER("Folder");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    public String toString() {
        return name;
    }

    public String getFullName() {
        return schema != null ? schema + "." + name : name;
    }
}