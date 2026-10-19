package com.roze.dbnavigator.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roze.dbnavigator.db.AppSettingsStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Dynamic dual-source application-update client.
 * Priority 1: Checks website / RozeHub API endpoint.
 * Priority 2: Falls back smoothly to GitHub Releases (firoze-hossain/DBNavigator)
 * if the website is offline, unlaunched, or has no new release.
 */
public final class AppUpdateService {
    public static final String DEFAULT_ENDPOINT = AppSettingsStore.DEFAULT_ROZEHUB_API_BASE_URL + "/updates/dbnavigator";
    private static final Path UPDATE_DIR = Path.of(System.getProperty("user.home"), ".dbnavigator", "updates");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private AppUpdateService() {}

    public static String currentVersion() {
        String version = AppUpdateService.class.getPackage().getImplementationVersion();
        if (version == null || version.isBlank()) {
            return "1.0.0";
        }
        return version.startsWith("v") || version.startsWith("V") ? version.substring(1) : version;
    }

    public static String platform() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return "Windows";
        if (os.contains("mac") || os.contains("darwin")) return "macOS";
        return "Linux";
    }

    public static String architecture() {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return arch.contains("aarch64") || arch.contains("arm64") ? "ARM64" : "x64";
    }

    /**
     * Compares two semantic version strings (e.g., "1.0.1" vs "1.0.0", "v2026.1.4" vs "1.0.0").
     * Returns > 0 if v1 > v2, < 0 if v1 < v2, and 0 if equal.
     */
    public static int compareVersions(String v1, String v2) {
        if (v1 == null && v2 == null) return 0;
        if (v1 == null) return -1;
        if (v2 == null) return 1;

        String clean1 = v1.trim().replaceFirst("^[vV]", "").split("[-_+]")[0];
        String clean2 = v2.trim().replaceFirst("^[vV]", "").split("[-_+]")[0];

        String[] parts1 = clean1.split("\\.");
        String[] parts2 = clean2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            long p1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            long p2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            if (p1 != p2) {
                return Long.compare(p1, p2);
            }
        }
        return 0;
    }

    private static long parseVersionPart(String part) {
        if (part == null || part.isBlank()) return 0;
        try {
            String digits = part.replaceAll("[^0-9].*$", "");
            return digits.isEmpty() ? 0 : Long.parseLong(digits);
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean isNewerVersion(String candidate, String current) {
        return compareVersions(candidate, current) > 0;
    }

    public static CompletableFuture<AppUpdate> checkForUpdate() {
        var settings = AppSettingsStore.load();
        return checkForUpdate(settings.getUpdateEndpoint(), settings.getUpdateChannel(), settings.getGitHubRepo());
    }

    public static CompletableFuture<AppUpdate> checkForUpdate(String endpoint, String channel) {
        return checkForUpdate(endpoint, channel, AppSettingsStore.load().getGitHubRepo());
    }

    /**
     * Checks website/RozeHub API first. If offline, unreachable, or no newer version,
     * seamlessly falls back to GitHub Releases for the configured repository.
     */
    public static CompletableFuture<AppUpdate> checkForUpdate(String endpoint, String channel, String githubRepo) {
        return checkForUpdateFromWebsite(endpoint, channel)
                .thenCompose(websiteUpdate -> {
                    if (websiteUpdate != null && websiteUpdate.available && websiteUpdate.release != null
                            && isNewerVersion(websiteUpdate.release.version, currentVersion())) {
                        websiteUpdate.source = "website";
                        return CompletableFuture.completedFuture(websiteUpdate);
                    }
                    // If website returned no newer update, check GitHub Releases fallback
                    return checkForUpdateFromGitHub(githubRepo, channel);
                })
                .exceptionallyCompose(error -> {
                    // Website was down, connection refused, or returned error -> fall back to GitHub
                    return checkForUpdateFromGitHub(githubRepo, channel);
                });
    }

    /**
     * Priority 1: Check website/RozeHub update API.
     */
    public static CompletableFuture<AppUpdate> checkForUpdateFromWebsite(String endpoint, String channel) {
        String base;
        if (endpoint == null || endpoint.isBlank()) {
            base = AppSettingsStore.getRozeHubUpdatesApiUrl();
        } else {
            String trimmed = endpoint.trim().replaceAll("/+$", "");
            if (trimmed.endsWith("/api/v1/updates/dbnavigator")) {
                base = trimmed;
            } else if (trimmed.endsWith("/api/v1")) {
                base = trimmed + "/updates/dbnavigator";
            } else if (trimmed.contains("/api/v1")) {
                int idx = trimmed.indexOf("/api/v1");
                base = trimmed.substring(0, idx + "/api/v1".length()) + "/updates/dbnavigator";
            } else {
                base = trimmed + "/api/v1/updates/dbnavigator";
            }
        }
        String separator = base.contains("?") ? "&" : "?";
        String uri = base + separator
                + "version=" + encode(currentVersion())
                + "&platform=" + encode(platform())
                + "&architecture=" + encode(architecture())
                + "&arch=" + encode(architecture())
                + "&channel=" + encode(channel == null || channel.isBlank() ? "Stable" : channel);

        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                .timeout(Duration.ofSeconds(4)) // 4s timeout for fast fallback
                .header("Accept", "application/json")
                .header("User-Agent", "DBNavigator-Pro/" + currentVersion())
                .GET()
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() / 100 != 2) {
                        return CompletableFuture.failedFuture(
                                new IOException("Website update server returned HTTP " + response.statusCode()));
                    }
                    try {
                        AppUpdate update = MAPPER.readValue(response.body(), AppUpdate.class);
                        if (update != null) update.source = "website";
                        return CompletableFuture.completedFuture(update);
                    } catch (Exception e) {
                        return CompletableFuture.failedFuture(new IOException("Invalid website update response", e));
                    }
                });
    }

    /**
     * Priority 2: Check GitHub Releases API for the repository.
     */
    public static CompletableFuture<AppUpdate> checkForUpdateFromGitHub(String repo, String channel) {
        String targetRepo = (repo == null || repo.isBlank()) ? "firoze-hossain/DBNavigator" : repo.trim();
        String latestUrl = "https://api.github.com/repos/" + targetRepo + "/releases/latest";

        HttpRequest request = HttpRequest.newBuilder(URI.create(latestUrl))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "DBNavigator-Pro/" + currentVersion())
                .GET()
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonNode rootNode = MAPPER.readTree(response.body());
                            return CompletableFuture.completedFuture(parseGitHubRelease(rootNode, currentVersion(), channel));
                        } catch (Exception e) {
                            return CompletableFuture.failedFuture(e);
                        }
                    } else if (response.statusCode() == 404) {
                        // /releases/latest might be 404 if no release is marked "latest", so try /releases
                        return queryGitHubReleasesList(targetRepo, channel);
                    } else {
                        return CompletableFuture.failedFuture(
                                new IOException("GitHub API returned HTTP " + response.statusCode()));
                    }
                })
                .exceptionallyCompose(err -> queryGitHubTagsFallback(targetRepo, channel));
    }

    private static CompletableFuture<AppUpdate> queryGitHubReleasesList(String targetRepo, String channel) {
        String releasesUrl = "https://api.github.com/repos/" + targetRepo + "/releases";
        HttpRequest request = HttpRequest.newBuilder(URI.create(releasesUrl))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "DBNavigator-Pro/" + currentVersion())
                .GET()
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonNode array = MAPPER.readTree(response.body());
                            if (array.isArray() && array.size() > 0) {
                                for (JsonNode item : array) {
                                    if (!item.path("draft").asBoolean(false)) {
                                        return CompletableFuture.completedFuture(parseGitHubRelease(item, currentVersion(), channel));
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    return queryGitHubTagsFallback(targetRepo, channel);
                });
    }

    private static CompletableFuture<AppUpdate> queryGitHubTagsFallback(String targetRepo, String channel) {
        String tagsUrl = "https://api.github.com/repos/" + targetRepo + "/tags";
        HttpRequest request = HttpRequest.newBuilder(URI.create(tagsUrl))
                .timeout(Duration.ofSeconds(6))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "DBNavigator-Pro/" + currentVersion())
                .GET()
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonNode array = MAPPER.readTree(response.body());
                            if (array.isArray() && array.size() > 0) {
                                String tagName = array.get(0).path("name").asText("");
                                String cleanVer = tagName.replaceFirst("^[vV]", "");
                                boolean newer = isNewerVersion(cleanVer, currentVersion());
                                AppUpdate update = new AppUpdate();
                                update.source = "github";
                                update.project = "DBNavigator";
                                update.currentVersion = currentVersion();
                                update.latestVersion = cleanVer;
                                update.available = newer;
                                update.channel = channel != null ? channel : "Stable";
                                update.platform = platform();
                                update.architecture = architecture();

                                if (newer) {
                                    AppUpdate.Release rel = new AppUpdate.Release();
                                    rel.version = cleanVer;
                                    rel.channel = channel;
                                    rel.notes = "New release tag " + tagName + " published on GitHub (" + targetRepo + ").";
                                    rel.htmlUrl = "https://github.com/" + targetRepo + "/releases/tag/" + tagName;
                                    rel.fileName = "DBNavigator-" + cleanVer + ".zip";
                                    rel.downloadUrl = rel.htmlUrl;
                                    update.release = rel;
                                }
                                return update;
                            }
                        } catch (Exception ignored) {}
                    }
                    AppUpdate noUpdate = new AppUpdate();
                    noUpdate.source = "github";
                    noUpdate.currentVersion = currentVersion();
                    noUpdate.latestVersion = currentVersion();
                    noUpdate.available = false;
                    return noUpdate;
                });
    }

    public static AppUpdate parseGitHubRelease(JsonNode releaseNode, String currentVersion, String channel) {
        String tagName = releaseNode.path("tag_name").asText("");
        if (tagName.isBlank()) {
            tagName = releaseNode.path("name").asText("");
        }
        String latestVersion = tagName.replaceFirst("^[vV]", "").trim();
        boolean newer = isNewerVersion(latestVersion, currentVersion);

        AppUpdate update = new AppUpdate();
        update.project = "DBNavigator";
        update.currentVersion = currentVersion;
        update.latestVersion = latestVersion;
        update.available = newer;
        update.source = "github";
        update.channel = channel != null ? channel : "Stable";
        update.platform = platform();
        update.architecture = architecture();

        AppUpdate.Release release = new AppUpdate.Release();
        release.id = releaseNode.path("id").asLong(0);
        release.version = latestVersion;
        release.channel = channel;
        release.notes = releaseNode.path("body").asText("No release notes were provided.");
        release.publishedAt = releaseNode.path("published_at").asText("");
        release.htmlUrl = releaseNode.path("html_url").asText("");

        // Select best matching asset for OS & Architecture
        String os = platform();
        String arch = architecture();
        JsonNode assets = releaseNode.path("assets");
        JsonNode bestAsset = null;
        int bestScore = -1;

        if (assets.isArray()) {
            for (JsonNode asset : assets) {
                String name = asset.path("name").asText("").toLowerCase(Locale.ROOT);
                int score = scoreAsset(name, os, arch);
                if (score > bestScore) {
                    bestScore = score;
                    bestAsset = asset;
                }
            }
        }

        if (bestAsset != null) {
            release.fileName = bestAsset.path("name").asText("");
            release.fileSize = bestAsset.path("size").asLong(0);
            release.downloadUrl = bestAsset.path("browser_download_url").asText("");
        } else {
            // Fallback to source archive or release page
            release.fileName = "DBNavigator-" + latestVersion + ".zip";
            release.downloadUrl = releaseNode.path("zipball_url").asText(release.htmlUrl);
        }

        update.release = release;
        return update;
    }

    private static int scoreAsset(String name, String os, String arch) {
        int score = 0;
        if ("Linux".equals(os)) {
            if (name.endsWith(".deb")) score += 1000;
            else if (name.endsWith(".appimage")) score += 800;
            else if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) score += 600;
            else if (name.endsWith(".rpm")) score += 300;
            else if (name.endsWith(".zip")) score += 200;

            if (score > 0) {
                if ("ARM64".equals(arch) && (name.contains("arm64") || name.contains("aarch64"))) score += 300;
                if ("x64".equals(arch) && (name.contains("x86_64") || name.contains("amd64") || name.contains("x64"))) score += 300;
            }
        } else if ("Windows".equals(os)) {
            if (name.endsWith(".msi")) score += 1000;
            else if (name.endsWith(".exe")) score += 900;
            else if (name.endsWith(".zip")) score += 400;

            if (score > 0) {
                if ("ARM64".equals(arch) && name.contains("arm64")) score += 300;
                if ("x64".equals(arch) && (name.contains("x64") || name.contains("x86_64") || name.contains("win64"))) score += 300;
            }
        } else if ("macOS".equals(os)) {
            if (name.endsWith(".pkg")) score += 1000;
            else if (name.endsWith(".dmg")) score += 900;
            else if (name.endsWith(".zip")) score += 400;

            if (score > 0) {
                if ("ARM64".equals(arch) && (name.contains("arm64") || name.contains("aarch64"))) score += 300;
                if ("x64".equals(arch) && (name.contains("x86_64") || name.contains("x64"))) score += 300;
            }
        }
        return score;
    }

    public static CompletableFuture<Path> download(AppUpdate.Release release, Consumer<Double> progress) {
        if (release == null) {
            return CompletableFuture.failedFuture(new IOException("No update release was provided."));
        }
        String downloadUrl = release.effectiveDownloadUrl();
        String fileName = release.effectiveFileName();
        long fileSize = release.effectiveFileSize();
        String expectedSha256 = release.effectiveSha256();
        if (downloadUrl == null || downloadUrl.isBlank()) {
            return CompletableFuture.failedFuture(new IOException("No download URL was provided for update."));
        }
        if (fileName == null || fileName.isBlank()) {
            return CompletableFuture.failedFuture(new IOException("No file name was provided for update."));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.createDirectories(UPDATE_DIR);
                String safeName = sanitizeFileName(fileName);
                Path partial = UPDATE_DIR.resolve(safeName + ".part");
                Path target = UPDATE_DIR.resolve(safeName);
                Files.deleteIfExists(partial);

                HttpRequest request = HttpRequest.newBuilder(URI.create(downloadUrl))
                        .timeout(Duration.ofMinutes(30))
                        .header("Accept", "application/octet-stream")
                        .header("User-Agent", "DBNavigator-Pro/" + currentVersion())
                        .GET()
                        .build();
                HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() / 100 != 2) {
                    throw new IOException("Download failed with HTTP " + response.statusCode());
                }

                long expectedSize = fileSize;
                long downloaded = 0;
                try (InputStream in = response.body();
                     var out = Files.newOutputStream(partial)) {
                    byte[] buffer = new byte[1024 * 1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        downloaded += read;
                        if (progress != null && expectedSize > 0) {
                            progress.accept(Math.min(1d, downloaded / (double) expectedSize));
                        }
                    }
                }

                if (expectedSize > 0 && downloaded != expectedSize) {
                    throw new IOException("Downloaded file size (" + downloaded + ") does not match metadata (" + expectedSize + ").");
                }

                // If SHA-256 is provided (e.g. from RozeHub API), verify it.
                // Standard GitHub direct assets do not require it.
                if (expectedSha256 != null && !expectedSha256.isBlank()) {
                    String actualHash = sha256(partial);
                    if (!actualHash.equalsIgnoreCase(expectedSha256.trim())) {
                        throw new IOException("SHA-256 verification failed. The downloaded package was not installed.");
                    }
                }

                Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                if (progress != null) progress.accept(1d);
                return target;
            } catch (Exception e) {
                try { Files.deleteIfExists(UPDATE_DIR.resolve(sanitizeFileName(fileName) + ".part")); }
                catch (Exception ignored) {}
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    /**
     * Starts the verified update package for the current operating system,
     * terminates DBNavigator cleanly, and allows the installer to update seamlessly.
     */
    public static void installAndRestart(Path packageFile) throws IOException {
        if (packageFile == null || !Files.isRegularFile(packageFile)) {
            throw new IOException("Update package was not found: " + packageFile);
        }

        String name = packageFile.getFileName().toString().toLowerCase(Locale.ROOT);
        String platform = platform();
        ProcessBuilder builder;

        if (platform.equals("Windows")) {
            if (name.endsWith(".msi")) {
                builder = new ProcessBuilder("msiexec.exe", "/i", packageFile.toAbsolutePath().toString());
            } else if (name.endsWith(".exe")) {
                builder = new ProcessBuilder(packageFile.toAbsolutePath().toString());
            } else {
                builder = new ProcessBuilder("explorer.exe", "/select,", packageFile.toAbsolutePath().toString());
            }
        } else if (platform.equals("macOS")) {
            if (name.endsWith(".pkg")) {
                builder = new ProcessBuilder("open", "-W", packageFile.toAbsolutePath().toString());
            } else if (name.endsWith(".dmg")) {
                builder = new ProcessBuilder("open", packageFile.toAbsolutePath().toString());
            } else {
                builder = new ProcessBuilder("open", "-R", packageFile.toAbsolutePath().toString());
            }
        } else {
            if (name.endsWith(".deb")) {
                try {
                    // Try pkexec dpkg for root installer prompt
                    builder = new ProcessBuilder("pkexec", "dpkg", "-i", packageFile.toAbsolutePath().toString());
                } catch (Exception e) {
                    builder = new ProcessBuilder("xdg-open", packageFile.toAbsolutePath().toString());
                }
            } else if (name.endsWith(".appimage")) {
                packageFile.toFile().setExecutable(true, false);
                builder = new ProcessBuilder(packageFile.toAbsolutePath().toString());
            } else {
                builder = new ProcessBuilder("xdg-open", packageFile.getParent().toAbsolutePath().toString());
            }
        }

        builder.inheritIO().start();
        javafx.application.Platform.exit();
        System.exit(0);
    }

    private static String sanitizeFileName(String value) {
        String name = value == null || value.isBlank() ? "DBNavigator-update" : Path.of(value).getFileName().toString();
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        StringBuilder result = new StringBuilder(64);
        for (byte b : digest.digest()) result.append(String.format("%02x", b));
        return result.toString();
    }
}
