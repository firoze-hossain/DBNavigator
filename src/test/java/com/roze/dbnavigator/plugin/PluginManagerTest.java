package com.roze.dbnavigator.plugin;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class PluginManagerTest {

    @Test
    void testFetchMarketplacePlugins() {
        PluginManager.MarketplaceResult res = PluginManager.getInstance().fetchMarketplacePlugins("", "All").join();
        assertNotNull(res);
        assertNotNull(res.plugins);
        assertTrue(res.plugins.size() >= 10, "Should have at least 10 plugins returned");

        Plugin p = res.plugins.get(0);
        assertNotNull(p.getName());
        assertNotNull(p.getId());
    }

    @Test
    void testFilterMarketplacePlugins() {
        PluginManager.MarketplaceResult res = PluginManager.getInstance().fetchMarketplacePlugins("Vim", "All").join();
        assertNotNull(res);
        assertTrue(res.plugins.stream().anyMatch(p -> p.getName().toLowerCase().contains("vim")));
    }

    @Test
    void testInstalledState() {
        List<Plugin> installed = PluginManager.getInstance().getInstalledPlugins();
        assertNotNull(installed);
        assertFalse(installed.isEmpty(), "Bundled/installed plugins should not be empty");
    }
}
