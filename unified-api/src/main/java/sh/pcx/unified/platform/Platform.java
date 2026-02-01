/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform;

import sh.pcx.unified.server.MinecraftVersion;
import sh.pcx.unified.server.ServerType;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for platform detection and information.
 *
 * <p>This interface provides access to platform-specific information including
 * the platform type, server type, and Minecraft version. It is the primary
 * entry point for platform-aware code that needs to adapt to different
 * server implementations.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get the current platform
 * Platform platform = Platform.current();
 *
 * // Check platform type
 * if (platform.getType() == PlatformType.BUKKIT) {
 *     // Bukkit-specific code
 * }
 *
 * // Check for specific features
 * if (platform.isFolia()) {
 *     // Use region-aware scheduling
 * }
 *
 * // Version checks
 * if (platform.isAtLeast(MinecraftVersion.V1_21)) {
 *     // Use 1.21+ features
 * }
 *
 * // Platform capabilities
 * if (platform.supportsAsyncChunks()) {
 *     // Use async chunk loading
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>The Platform instance is immutable and thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlatformType
 * @see PlatformProvider
 */
public interface Platform {

    /**
     * Returns the current platform instance.
     *
     * @return the current platform
     * @throws IllegalStateException if the platform has not been initialized
     * @since 1.0.0
     */
    @NotNull
    static Platform current() {
        Platform platform = PlatformHolder.INSTANCE;
        if (platform == null) {
            throw new IllegalStateException(
                    "Platform has not been initialized. " +
                    "Ensure UnifiedPluginAPI is loaded before accessing the platform."
            );
        }
        return platform;
    }

    /**
     * Checks if the platform has been initialized.
     *
     * @return true if the platform is available
     * @since 1.0.0
     */
    static boolean isInitialized() {
        return PlatformHolder.INSTANCE != null;
    }

    /**
     * Returns the platform type.
     *
     * @return the platform type (BUKKIT, SPONGE, etc.)
     * @since 1.0.0
     */
    @NotNull
    PlatformType getType();

    /**
     * Returns the server type.
     *
     * @return the server type (PAPER, FOLIA, SPIGOT, SPONGE, etc.)
     * @since 1.0.0
     */
    @NotNull
    ServerType getServerType();

    /**
     * Returns the Minecraft version.
     *
     * @return the Minecraft version
     * @since 1.0.0
     */
    @NotNull
    MinecraftVersion getMinecraftVersion();

    /**
     * Returns the server software name.
     *
     * @return the server name (e.g., "Paper", "Sponge")
     * @since 1.0.0
     */
    @NotNull
    String getServerName();

    /**
     * Returns the server software version.
     *
     * @return the full server version string
     * @since 1.0.0
     */
    @NotNull
    String getServerVersion();

    /**
     * Checks if the server is running Paper or a Paper fork.
     *
     * @return true if Paper API is available
     * @since 1.0.0
     */
    boolean isPaper();

    /**
     * Checks if the server is running Folia.
     *
     * @return true if Folia's region threading is active
     * @since 1.0.0
     */
    boolean isFolia();

    /**
     * Checks if the server is running Sponge.
     *
     * @return true if Sponge API is available
     * @since 1.0.0
     */
    boolean isSponge();

    /**
     * Checks if the Minecraft version is at least the specified version.
     *
     * @param version the minimum version required
     * @return true if the current version meets or exceeds the requirement
     * @since 1.0.0
     */
    default boolean isAtLeast(@NotNull MinecraftVersion version) {
        return getMinecraftVersion().isAtLeast(version);
    }

    /**
     * Checks if the Minecraft version is older than the specified version.
     *
     * @param version the version to compare against
     * @return true if the current version is older
     * @since 1.0.0
     */
    default boolean isOlderThan(@NotNull MinecraftVersion version) {
        return getMinecraftVersion().isOlderThan(version);
    }

    /**
     * Checks if the current version is supported by the API.
     *
     * @return true if the version is within the supported range
     * @since 1.0.0
     */
    default boolean isVersionSupported() {
        return getMinecraftVersion().isSupported();
    }

    /**
     * Checks if the platform supports async chunk loading.
     *
     * @return true if async chunk loading is available
     * @since 1.0.0
     */
    boolean supportsAsyncChunks();

    /**
     * Checks if the platform supports Adventure natively.
     *
     * @return true if Adventure is natively supported
     * @since 1.0.0
     */
    boolean supportsAdventure();

    /**
     * Checks if the platform supports component-based items (1.20.5+).
     *
     * @return true if component items are supported
     * @since 1.0.0
     */
    default boolean supportsComponentItems() {
        return isAtLeast(MinecraftVersion.V1_20_5);
    }

    /**
     * Checks if the platform requires region-aware scheduling.
     *
     * @return true if region scheduling is required (Folia)
     * @since 1.0.0
     */
    default boolean requiresRegionScheduling() {
        return isFolia();
    }

    /**
     * Checks if the platform supports plugin messaging channels.
     *
     * @return true if plugin messaging is available
     * @since 1.0.0
     */
    boolean supportsPluginMessaging();

    /**
     * Returns the NMS version string for this platform.
     *
     * <p>This is only relevant for Bukkit-based platforms.
     *
     * @return the NMS version string, or empty for non-Bukkit platforms
     * @since 1.0.0
     */
    @NotNull
    String getNmsVersion();
}
