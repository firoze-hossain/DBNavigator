package com.roze.dbnavigator.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.File;
import java.util.Objects;

/**
 * Represents a user project in DBNavigator Pro, matching JetBrains DataGrip's project concept.
 * Projects have a name, directory path, and last opened timestamp.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Project {

    private String name = "default";
    private String path = "";
    private long createdTime = System.currentTimeMillis();
    private long lastOpened = System.currentTimeMillis();

    public Project() {}

    public Project(String name, String path) {
        this.name = name != null && !name.isBlank() ? name.trim() : "default";
        this.path = path != null ? path.trim() : "";
        this.createdTime = System.currentTimeMillis();
        this.lastOpened = System.currentTimeMillis();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null && !name.isBlank() ? name.trim() : "default";
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path != null ? path.trim() : "";
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public long getLastOpened() {
        return lastOpened;
    }

    public void setLastOpened(long lastOpened) {
        this.lastOpened = lastOpened;
    }

    @JsonIgnore
    public String getInitialLetter() {
        if (name == null || name.isBlank()) return "D";
        return name.trim().substring(0, 1).toUpperCase();
    }

    @JsonIgnore
    public String getDisplayPath() {
        if (path == null || path.isBlank()) return "";
        String userHome = System.getProperty("user.home");
        if (userHome != null && path.startsWith(userHome)) {
            return "~" + path.substring(userHome.length());
        }
        return path;
    }

    @JsonIgnore
    public File toFile() {
        return path != null && !path.isBlank() ? new File(path) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Project other)) return false;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name + " (" + getDisplayPath() + ")";
    }
}
