package com.roze.dbnavigator.update;

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
 * RozeHub application-update client. It only consumes published releases from
 * the public RozeHub update API; it never writes into the installed app while
 * DBNavigator is running.
 */
public final class AppUpdateService {
    public static final String DEFAULT_ENDPOINT = "http://127.0.0.1:8000/api/v1/updates/dbnavigator";
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
            return "2.0.0";
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

    public static CompletableFuture<AppUpdate> checkForUpdate() {
        return checkForUpdate(AppSettingsStore.load().getUpdateEndpoint(), AppSettingsStore.load().getUpdateChannel());
    }

    public static CompletableFuture<AppUpdate> checkForUpdate(String endpoint, String channel) {
        String base = endpoint == null || endpoint.isBlank() ? DEFAULT_ENDPOINT : endpoint.trim();
        String separator = base.contains("?") ? "&" : "?";
        String uri = base + separator
                + "version=" + encode(currentVersion())
                + "&platform=" + encode(platform())
                + "&architecture=" + encode(architecture())
                + "&channel=" + encode(channel == null || channel.isBlank() ? "Stable" : channel);

        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("User-Agent", "DBNavigator-Pro/" + currentVersion())
                .GET()
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() / 100 != 2) {
                        return CompletableFuture.failedFuture(
                                new IOException("RozeHub returned HTTP " + response.statusCode()));
                    }
                    try {
                        return CompletableFuture.completedFuture(MAPPER.readValue(response.body(), AppUpdate.class));
                    } catch (Exception e) {
                        return CompletableFuture.failedFuture(new IOException("Invalid RozeHub update response", e));
                    }
                });
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
            return CompletableFuture.failedFuture(new IOException("RozeHub did not provide an update artifact download URL."));
        }
        if (fileName == null || fileName.isBlank()) {
            return CompletableFuture.failedFuture(new IOException("RozeHub did not provide an update artifact file name."));
        }
        if (expectedSha256 == null || expectedSha256.isBlank()) {
            return CompletableFuture.failedFuture(new IOException("RozeHub did not provide a SHA-256 checksum for the update artifact."));
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
                    throw new IOException("RozeHub download failed with HTTP " + response.statusCode());
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
                    throw new IOException("Downloaded file size does not match RozeHub metadata.");
                }

                String actualHash = sha256(partial);
                if (!actualHash.equalsIgnoreCase(expectedSha256.trim())) {
                    throw new IOException("SHA-256 verification failed. The downloaded package was not installed.");
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
     * Starts the verified update package. The package is selected from
     * RozeHub's updateArtifact, so macOS can use .pkg while public downloads
     * can continue using .dmg.
     */
    public static void installAndRestart(Path packageFile) throws IOException {
        if (packageFile == null || !Files.isRegularFile(packageFile)) {
            throw new IOException("Verified update package was not found.");
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
                throw new IOException("Unsupported Windows update package: " + packageFile.getFileName());
            }
        } else if (platform.equals("macOS")) {
            if (name.endsWith(".pkg")) {
                // macOS Installer.app handles permissions and displays the normal
                // signed package installation UI. This is the preferred update artifact.
                builder = new ProcessBuilder("open", "-W", packageFile.toAbsolutePath().toString());
            } else if (name.endsWith(".dmg")) {
                // Legacy fallback only. DMG is intended for new installations;
                // RozeHub should normally return a .pkg as updateArtifact.
                builder = new ProcessBuilder("open", packageFile.toAbsolutePath().toString());
            } else {
                throw new IOException("Unsupported macOS update package: " + packageFile.getFileName());
            }
        } else {
            if (name.endsWith(".deb")) {
                builder = new ProcessBuilder("pkexec", "apt-get", "install", "-y", packageFile.toAbsolutePath().toString());
            } else if (name.endsWith(".appimage")) {
                packageFile.toFile().setExecutable(true, false);
                builder = new ProcessBuilder(packageFile.toAbsolutePath().toString());
            } else {
                throw new IOException("Unsupported Linux update package: " + packageFile.getFileName());
            }
        }

        builder.inheritIO().start();
    }

    private static String sanitizeFileName(String value) {
        String name = value == null || value.isBlank() ? "DBNavigator-update.pkg" : Path.of(value).getFileName().toString();
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
