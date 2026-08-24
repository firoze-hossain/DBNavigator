package com.roze.dbnavigator.update;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Locale;
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

        // Legacy release-level package fields. Kept for backward compatibility.
        public String fileName;
        public long fileSize;
        public String sha256;
        public String downloadUrl;

        public Artifact installerArtifact;
        public Artifact updateArtifact;
        public String publishedAt;

        /**
         * RozeHub's UPDATER artifact is the package intended for an already
         * installed application. Older RozeHub releases may not have artifacts;
         * in that case we fall back to the legacy release package.
         */
        public Artifact effectiveUpdateArtifact() {
            if (updateArtifact != null && updateArtifact.isUsable()) return updateArtifact;
            if (installerArtifact != null && installerArtifact.isUsable()) return installerArtifact;
            if (fileName != null && !fileName.isBlank()) {
                Artifact legacy = new Artifact();
                legacy.purpose = "UPDATER";
                legacy.packageType = extensionOf(fileName);
                legacy.fileName = fileName;
                legacy.fileSize = fileSize;
                legacy.sha256 = sha256;
                legacy.downloadUrl = downloadUrl;
                return legacy;
            }
            return null;
        }

        public String effectiveFileName() {
            Artifact artifact = effectiveUpdateArtifact();
            return artifact == null ? fileName : artifact.fileName;
        }

        public long effectiveFileSize() {
            Artifact artifact = effectiveUpdateArtifact();
            return artifact == null ? fileSize : artifact.fileSize;
        }

        public String effectiveSha256() {
            Artifact artifact = effectiveUpdateArtifact();
            return artifact == null ? sha256 : artifact.sha256;
        }

        public String effectiveDownloadUrl() {
            Artifact artifact = effectiveUpdateArtifact();
            return artifact == null ? downloadUrl : artifact.downloadUrl;
        }

        public String effectivePackageType() {
            Artifact artifact = effectiveUpdateArtifact();
            if (artifact != null && artifact.packageType != null && !artifact.packageType.isBlank()) {
                return artifact.packageType.toLowerCase(Locale.ROOT);
            }
            return extensionOf(effectiveFileName());
        }

        public String displayNotes() {
            return notes == null || notes.isBlank() ? "No release notes were provided." : notes;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Artifact {
        public long id;
        public String purpose;
        public String packageType;
        public String fileName;
        public long fileSize;
        public String sha256;
        public String downloadUrl;

        public boolean isUsable() {
            return fileName != null && !fileName.isBlank()
                    && downloadUrl != null && !downloadUrl.isBlank();
        }
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) return "";
        String name = fileName.trim();
        int dot = name.lastIndexOf('.');
        return dot >= 0 && dot + 1 < name.length() ? name.substring(dot + 1) : "";
    }
}
