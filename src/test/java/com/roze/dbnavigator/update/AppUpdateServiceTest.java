package com.roze.dbnavigator.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AppUpdateServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testVersionComparison() {
        assertTrue(AppUpdateService.compareVersions("1.0.1", "1.0.0") > 0);
        assertTrue(AppUpdateService.compareVersions("1.1.0", "1.0.9") > 0);
        assertTrue(AppUpdateService.compareVersions("2.0.0", "1.9.9") > 0);
        assertTrue(AppUpdateService.compareVersions("v1.0.1", "1.0.0") > 0);
        assertTrue(AppUpdateService.compareVersions("2026.1.4", "1.0.0") > 0);

        assertEquals(0, AppUpdateService.compareVersions("1.0.0", "1.0.0"));
        assertEquals(0, AppUpdateService.compareVersions("v1.0.0", "1.0.0"));
        assertEquals(0, AppUpdateService.compareVersions("1.0.0", "v1.0.0"));

        assertTrue(AppUpdateService.compareVersions("1.0.0", "1.0.1") < 0);
        assertTrue(AppUpdateService.compareVersions("0.9.9", "1.0.0") < 0);

        assertTrue(AppUpdateService.isNewerVersion("1.0.1", "1.0.0"));
        assertFalse(AppUpdateService.isNewerVersion("1.0.0", "1.0.0"));
        assertFalse(AppUpdateService.isNewerVersion("0.9.9", "1.0.0"));
    }

    @Test
    void testParseGitHubReleaseWithMultiPlatformAssets() throws Exception {
        String json = """
        {
          "tag_name": "v1.0.1",
          "name": "DBNavigator Pro 1.0.1",
          "body": "### Bug Fixes\\n- Fixed search everywhere connection backoff\\n- Added DataGrip update notification",
          "published_at": "2026-10-19T18:00:00Z",
          "html_url": "https://github.com/firoze-hossain/DBNavigator/releases/tag/v1.0.1",
          "assets": [
            {
              "name": "DBNavigatorPro-1.0.1-amd64.deb",
              "size": 84123456,
              "browser_download_url": "https://github.com/firoze-hossain/DBNavigator/releases/download/v1.0.1/DBNavigatorPro-1.0.1-amd64.deb"
            },
            {
              "name": "DBNavigatorPro-1.0.1-x64.msi",
              "size": 89123456,
              "browser_download_url": "https://github.com/firoze-hossain/DBNavigator/releases/download/v1.0.1/DBNavigatorPro-1.0.1-x64.msi"
            },
            {
              "name": "DBNavigatorPro-1.0.1.pkg",
              "size": 91123456,
              "browser_download_url": "https://github.com/firoze-hossain/DBNavigator/releases/download/v1.0.1/DBNavigatorPro-1.0.1.pkg"
            }
          ]
        }
        """;

        JsonNode releaseNode = mapper.readTree(json);
        AppUpdate update = AppUpdateService.parseGitHubRelease(releaseNode, "1.0.0", "Stable");

        assertNotNull(update);
        assertTrue(update.available, "Update should be available because 1.0.1 > 1.0.0");
        assertEquals("1.0.1", update.latestVersion);
        assertEquals("github", update.source);
        assertNotNull(update.release);
        assertEquals("1.0.1", update.release.version);
        assertTrue(update.release.notes.contains("DataGrip update notification"));

        // Verify the asset selected matches the current test runner's OS
        String platform = AppUpdateService.platform();
        if ("Linux".equals(platform)) {
            assertEquals("DBNavigatorPro-1.0.1-amd64.deb", update.release.fileName);
            assertEquals("https://github.com/firoze-hossain/DBNavigator/releases/download/v1.0.1/DBNavigatorPro-1.0.1-amd64.deb", update.release.downloadUrl);
            assertEquals(84123456L, update.release.fileSize);
        } else if ("Windows".equals(platform)) {
            assertEquals("DBNavigatorPro-1.0.1-x64.msi", update.release.fileName);
        } else if ("macOS".equals(platform)) {
            assertEquals("DBNavigatorPro-1.0.1.pkg", update.release.fileName);
        }
    }

    @Test
    void testParseGitHubReleaseWhenAlreadyUpToDate() throws Exception {
        String json = """
        {
          "tag_name": "v1.0.0",
          "name": "DBNavigator Pro 1.0.0",
          "body": "Initial release",
          "published_at": "2026-10-18T10:00:00Z",
          "html_url": "https://github.com/firoze-hossain/DBNavigator/releases/tag/v1.0.0",
          "assets": []
        }
        """;

        JsonNode releaseNode = mapper.readTree(json);
        AppUpdate update = AppUpdateService.parseGitHubRelease(releaseNode, "1.0.0", "Stable");

        assertNotNull(update);
        assertFalse(update.available, "Update should NOT be available because current version 1.0.0 == latest 1.0.0");
        assertEquals("1.0.0", update.latestVersion);
    }
}
