package com.roze.dbnavigator.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Descriptor and metadata for a DBNavigator plugin (Marketplace or Installed).
 */
public class Plugin {

    private String id;
    private String name;
    private String vendor = "JetBrains s.r.o.";
    private String version = "1.0.0";
    private String category = "Other";
    private List<String> tags = new ArrayList<>();
    private String summary = "";
    private String description = "";
    private long downloadsCount = 0;
    private double rating = 5.0;
    private int reviewsCount = 0;
    private String website = "";
    private boolean official = true;
    private boolean bundled = false;
    private boolean enabled = true;
    private boolean installed = false;
    private String installedVersion;
    private String downloadUrl = "";
    private String fileName = "";
    private String sha256 = "";
    private long fileSize = 0;
    private String releaseNotes = "";
    private String iconColor = "#4a88c7";
    private String iconType = "default"; // air, ideavim, ignore, bigdata, wakatime, cloud, mcp, db, xml, code, lang, ml

    public Plugin() {}

    public Plugin(String id, String name, String vendor, String version, String category,
                  String summary, String description, boolean official, boolean bundled,
                  boolean installed, boolean enabled, String iconType, String iconColor) {
        this.id = id;
        this.name = name;
        this.vendor = vendor;
        this.version = version;
        this.category = category;
        this.summary = summary;
        this.description = description;
        this.official = official;
        this.bundled = bundled;
        this.installed = installed;
        this.enabled = enabled;
        this.iconType = iconType;
        this.iconColor = iconColor;
    }

    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name; }

    public String getVendor() { return vendor != null ? vendor : "JetBrains s.r.o."; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public String getVersion() { return version != null ? version : "1.0.0"; }
    public void setVersion(String version) { this.version = version; }

    public String getCategory() { return category != null ? category : "Other"; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getTags() {
        if (tags == null) tags = new ArrayList<>();
        return tags;
    }
    public void setTags(List<String> tags) { this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>(); }

    public String getSummary() { return summary != null ? summary : ""; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getDescription() { return description != null ? description : ""; }
    public void setDescription(String description) { this.description = description; }

    public long getDownloadsCount() { return downloadsCount; }
    public void setDownloadsCount(long downloadsCount) { this.downloadsCount = downloadsCount; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviewsCount() { return reviewsCount; }
    public void setReviewsCount(int reviewsCount) { this.reviewsCount = reviewsCount; }

    public String getWebsite() { return website != null ? website : ""; }
    public void setWebsite(String website) { this.website = website; }

    public boolean isOfficial() { return official; }
    public void setOfficial(boolean official) { this.official = official; }

    public boolean isBundled() { return bundled; }
    public void setBundled(boolean bundled) { this.bundled = bundled; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isInstalled() { return installed; }
    public void setInstalled(boolean installed) { this.installed = installed; }

    public String getInstalledVersion() { return installedVersion != null ? installedVersion : version; }
    public void setInstalledVersion(String installedVersion) { this.installedVersion = installedVersion; }

    public String getDownloadUrl() { return downloadUrl != null ? downloadUrl : ""; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public String getFileName() { return fileName != null ? fileName : ""; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getSha256() { return sha256 != null ? sha256 : ""; }
    public void setSha256(String sha256) { this.sha256 = sha256; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getReleaseNotes() { return releaseNotes != null ? releaseNotes : ""; }
    public void setReleaseNotes(String releaseNotes) { this.releaseNotes = releaseNotes; }

    public String getIconColor() { return iconColor != null ? iconColor : "#4a88c7"; }
    public void setIconColor(String iconColor) { this.iconColor = iconColor; }

    public String getIconType() { return iconType != null ? iconType : "default"; }
    public void setIconType(String iconType) { this.iconType = iconType; }

    public String formattedDownloads() {
        if (downloadsCount >= 1_000_000) {
            double m = downloadsCount / 1_000_000.0;
            return String.format(java.util.Locale.US, "%.1fM", m);
        } else if (downloadsCount >= 1_000) {
            double k = downloadsCount / 1_000.0;
            return String.format(java.util.Locale.US, "%.1fK", k);
        } else {
            return String.valueOf(downloadsCount);
        }
    }

    public String formattedRating() {
        return String.format(java.util.Locale.US, "%.2f", rating);
    }
}
