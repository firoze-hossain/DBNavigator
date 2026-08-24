package com.roze.dbnavigator.update;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class AppUpdate {
    public String project;
    public String currentVersion;
    public boolean available;
    public boolean mandatory;
    public String channel;
    public String platform;
    public String architecture;
    public String latestVersion;
    public String minimumSupportedVersion;
    public Release release;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Release {
        public long id;
        public String version;
        public String channel;
        public String notes;
        public String minimumVersion;
        public boolean mandatory;
        public String fileName;
        public long fileSize;
        public String sha256;
        public String publishedAt;
        public String downloadUrl;

        public String displayNotes() {
            return notes == null || notes.isBlank() ? "No release notes were provided." : notes;
        }
    }
}
