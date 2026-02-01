/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.folia;

import sh.pcx.unified.platform.Platform;
import sh.pcx.unified.platform.PlatformType;
import sh.pcx.unified.server.MinecraftVersion;
import sh.pcx.unified.server.ServerType;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Folia platform implementation.
 *
 * <p>This class provides platform detection and information for servers
 * running Folia, a Paper fork that implements region-based multithreading.
 * Folia fundamentally changes the server's threading model, requiring
 * special handling for tasks, entity operations, and world modifications.
 *
 * <h2>Threading Model</h2>
 * <p>Unlike standard Bukkit/Paper servers which use a single main thread,
 * Folia divides the world into regions that are each processed by their
 * own thread. This provides significant performance improvements for
 * servers with many players spread across the world, but requires careful
 * attention to thread safety.
 *
 * <h2>Key Differences from Paper</h2>
 * <ul>
 *   <li>No single "main thread" - each region has its own tick thread</li>
 *   <li>Entities can only be modified by their region's thread</li>
 *   <li>Cross-region operations require scheduling</li>
 *   <li>Traditional Bukkit schedulers are not safe to use</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * FoliaPlatform platform = new FoliaPlatform();
 *
 * // Check Folia-specific features
 * if (platform.isFolia()) {
 *     // Use region-aware scheduling
 *     getLogger().info("Running on Folia " + platform.getServerVersion());
 * }
 *
 * // Platform capabilities
 * boolean asyncChunks = platform.supportsAsyncChunks(); // true
 * boolean adventure = platform.supportsAdventure(); // true
 * boolean regionScheduling = platform.requiresRegionScheduling(); // true
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Platform
 * @see FoliaDetector
 */
public final class FoliaPlatform implements Platform {

    private static final Logger LOGGER = Logger.getLogger(FoliaPlatform.class.getName());

    /**
     * Pattern to extract version from Bukkit version string.
     */
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "\\(MC: (\\d+\\.\\d+(?:\\.\\d+)?)\\)"
    );

    /**
     * Pattern to extract NMS version.
     */
    private static final Pattern NMS_PATTERN = Pattern.compile(
            "v(\\d+_\\d+_R\\d+)"
    );

    /**
     * Cached server name.
     */
    private final String serverName;

    /**
     * Cached server version.
     */
    private final String serverVersion;

    /**
     * Cached Minecraft version.
     */
    private final MinecraftVersion minecraftVersion;

    /**
     * Cached NMS version string.
     */
    private final String nmsVersion;

    /**
     * Constructs a new FoliaPlatform instance.
     *
     * <p>This constructor performs version detection using reflection
     * to access Bukkit/Paper API methods.
     *
     * @throws IllegalStateException if not running on Folia
     * @since 1.0.0
     */
    public FoliaPlatform() {
        if (!FoliaDetector.isFolia()) {
            throw new IllegalStateException(
                    "FoliaPlatform can only be instantiated on Folia servers"
            );
        }

        // Detect versions using reflection (Bukkit may not be on compile classpath)
        this.serverName = detectServerName();
        this.serverVersion = detectServerVersion();
        this.minecraftVersion = detectMinecraftVersion();
        this.nmsVersion = detectNmsVersion();

        LOGGER.info("Folia platform initialized: " + serverVersion);
    }

    /**
     * Detects the server name.
     */
    private String detectServerName() {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object server = bukkitClass.getMethod("getServer").invoke(null);
            return (String) server.getClass().getMethod("getName").invoke(server);
        } catch (Exception e) {
            return "Folia";
        }
    }

    /**
     * Detects the full server version string.
     */
    private String detectServerVersion() {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            return (String) bukkitClass.getMethod("getVersion").invoke(null);
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * Detects the Minecraft version.
     */
    private MinecraftVersion detectMinecraftVersion() {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            String version = (String) bukkitClass.getMethod("getVersion").invoke(null);

            Matcher matcher = VERSION_PATTERN.matcher(version);
            if (matcher.find()) {
                String mcVersion = matcher.group(1);
                return MinecraftVersion.parse(mcVersion);
            }

            // Fallback: try getBukkitVersion
            String bukkitVersion = (String) bukkitClass.getMethod("getBukkitVersion").invoke(null);
            String[] parts = bukkitVersion.split("-")[0].split("\\.");
            if (parts.length >= 2) {
                return MinecraftVersion.parse(parts[0] + "." + parts[1]);
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to detect Minecraft version: " + e.getMessage());
        }
        return MinecraftVersion.V1_20_5; // Safe default
    }

    /**
     * Detects the NMS version string.
     */
    private String detectNmsVersion() {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object server = bukkitClass.getMethod("getServer").invoke(null);
            String className = server.getClass().getPackage().getName();

            Matcher matcher = NMS_PATTERN.matcher(className);
            if (matcher.find()) {
                return matcher.group(1);
            }

            // Modern Paper uses Mojang mappings, no NMS version
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    @NotNull
    public PlatformType getType() {
        return PlatformType.BUKKIT;
    }

    @Override
    @NotNull
    public ServerType getServerType() {
        return ServerType.FOLIA;
    }

    @Override
    @NotNull
    public MinecraftVersion getMinecraftVersion() {
        return minecraftVersion;
    }

    @Override
    @NotNull
    public String getServerName() {
        return serverName;
    }

    @Override
    @NotNull
    public String getServerVersion() {
        return serverVersion;
    }

    @Override
    public boolean isPaper() {
        return true; // Folia is Paper-based
    }

    @Override
    public boolean isFolia() {
        return true;
    }

    @Override
    public boolean isSponge() {
        return false;
    }

    @Override
    public boolean supportsAsyncChunks() {
        return true; // Folia inherits Paper's async chunk loading
    }

    @Override
    public boolean supportsAdventure() {
        return true; // Folia inherits Paper's native Adventure support
    }

    @Override
    public boolean requiresRegionScheduling() {
        return true; // This is the key difference from Paper
    }

    @Override
    public boolean supportsPluginMessaging() {
        return true;
    }

    @Override
    @NotNull
    public String getNmsVersion() {
        return nmsVersion;
    }

    /**
     * Checks if the current thread is the global region tick thread.
     *
     * <p>The global region handles server-wide operations that don't
     * belong to any specific world location.
     *
     * @return true if on the global region thread
     * @since 1.0.0
     */
    public boolean isGlobalTickThread() {
        return FoliaDetector.isGlobalTickThread();
    }

    /**
     * Checks if the current thread is any region tick thread.
     *
     * @return true if on a region tick thread
     * @since 1.0.0
     */
    public boolean isTickThread() {
        return FoliaDetector.isTickThread();
    }

    /**
     * Checks if the current thread owns the region at the specified location.
     *
     * @param world the world
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return true if the current thread owns this region
     * @since 1.0.0
     */
    public boolean isOwnedByCurrentRegion(@NotNull Object world, int chunkX, int chunkZ) {
        return FoliaDetector.isOwnedByCurrentRegion(world, chunkX, chunkZ);
    }

    @Override
    public String toString() {
        return String.format("FoliaPlatform[name=%s, version=%s, mc=%s]",
                serverName, serverVersion, minecraftVersion);
    }
}
