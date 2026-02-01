/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.core;

import sh.pcx.unified.platform.paper.BukkitUnifiedPlugin;

/**
 * Main plugin class for the UnifiedPlugin API on Bukkit-based servers.
 *
 * <p>This plugin provides the core Unified API framework for Paper, Spigot,
 * and Folia servers. Other plugins can depend on this to access unified
 * abstractions for commands, configuration, GUIs, data persistence, and more.
 *
 * <p>This plugin bootstraps the platform provider automatically on first load.
 * Subsequent plugins extending {@link PaperUnifiedPlugin} will use the already
 * initialized platform.
 *
 * <p>For Sponge servers, see {@code SpongeUnifiedPlugin} which serves as
 * the entry point on that platform.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class UnifiedPluginAPI extends BukkitUnifiedPlugin {

    private static UnifiedPluginAPI instance;

    /**
     * Gets the singleton instance of the UnifiedPlugin API.
     *
     * @return the plugin instance, or null if not yet enabled
     */
    public static UnifiedPluginAPI getInstance() {
        return instance;
    }

    @Override
    protected void onPluginLoad() {
        instance = this;
        getLogger().info("UnifiedPlugin API loading...");
    }

    @Override
    protected void onPluginEnable() {
        getLogger().info("UnifiedPlugin API v" + getUnifiedMeta().version() + " enabled!");
        getLogger().info("Providing unified abstractions for dependent plugins.");
    }

    @Override
    protected void onPluginDisable() {
        getLogger().info("UnifiedPlugin API disabled.");
        instance = null;
    }

    @Override
    protected void onPluginReload() {
        getLogger().info("UnifiedPlugin API configuration reloaded.");
    }
}
