/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.version.api;

import sh.pcx.unified.server.MinecraftVersion;
import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;

/**
 * Service interface for Minecraft version detection and comparison.
 *
 * <p>This service provides methods to query the current server version and check
 * for feature availability. It abstracts away the complexity of version detection
 * across different server platforms (Paper, Spigot, Folia).
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private VersionProvider version;
 *
 * public void onEnable() {
 *     // Get current version
 *     MinecraftVersion current = version.current();
 *     logger.info("Running on Minecraft " + current);
 *
 *     // Version comparisons
 *     if (version.isAtLeast(MinecraftVersion.V1_21)) {
 *         enableModernFeatures();
 *     }
 *
 *     if (version.isOlderThan(MinecraftVersion.V1_21_11)) {
 *         // Use legacy gamerule naming
 *         useCamelCaseGameRules();
 *     }
 *
 *     // Feature detection
 *     if (version.supports(Feature.COMPONENT_ITEMS)) {
 *         useComponentBasedItems();
 *     }
 *
 *     if (version.supports(Feature.MOJANG_MAPPINGS)) {
 *         useMojangMappedNMS();
 *     }
 *
 *     // Get NMS version string
 *     String nmsVersion = version.getNmsVersion();
 *     // Returns: "v1_21_R1", "v1_21_R2", etc.
 * }
 * }</pre>
 *
 * <h2>Version Support</h2>
 * <p>The UnifiedPlugin API supports Minecraft versions 1.20.5 through 1.21.11+.
 * Attempting to run on unsupported versions will result in a warning at startup,
 * and some features may not work correctly.
 *
 * <h2>Thread Safety</h2>
 * <p>All methods in this interface are thread-safe. The version information
 * is determined at server startup and does not change during runtime.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MinecraftVersion
 * @see Feature
 */
public interface VersionProvider extends Service {

    /**
     * Returns the current Minecraft version of the server.
     *
     * @return the current Minecraft version
     * @since 1.0.0
     */
    @NotNull
    MinecraftVersion current();

    /**
     * Checks if the current version is at least the specified version.
     *
     * @param version the minimum version required
     * @return true if current version is equal to or newer than the specified version
     * @since 1.0.0
     */
    default boolean isAtLeast(@NotNull MinecraftVersion version) {
        return current().isAtLeast(version);
    }

    /**
     * Checks if the current version is older than the specified version.
     *
     * @param version the version to compare against
     * @return true if current version is older
     * @since 1.0.0
     */
    default boolean isOlderThan(@NotNull MinecraftVersion version) {
        return current().isOlderThan(version);
    }

    /**
     * Checks if the current version is newer than the specified version.
     *
     * @param version the version to compare against
     * @return true if current version is newer
     * @since 1.0.0
     */
    default boolean isNewerThan(@NotNull MinecraftVersion version) {
        return current().isNewerThan(version);
    }

    /**
     * Checks if the current version is exactly the specified version.
     *
     * @param version the version to check
     * @return true if versions match exactly
     * @since 1.0.0
     */
    default boolean isExactly(@NotNull MinecraftVersion version) {
        return current().equals(version);
    }

    /**
     * Checks if the current version is within the specified range (inclusive).
     *
     * @param min the minimum version
     * @param max the maximum version
     * @return true if current version is within the range
     * @since 1.0.0
     */
    default boolean isBetween(@NotNull MinecraftVersion min, @NotNull MinecraftVersion max) {
        return current().isBetween(min, max);
    }

    /**
     * Checks if the current version supports the specified feature.
     *
     * @param feature the feature to check for
     * @return true if the feature is available on this version
     * @since 1.0.0
     */
    default boolean supports(@NotNull Feature feature) {
        return feature.isAvailableOn(current());
    }

    /**
     * Checks if the current version is within the supported range.
     *
     * @return true if version is officially supported
     * @since 1.0.0
     */
    default boolean isSupported() {
        return current().isSupported();
    }

    /**
     * Returns the NMS version string for the current version.
     *
     * <p>This corresponds to the CraftBukkit package version:
     * <ul>
     *   <li>1.20.5-1.20.6: v1_20_R4</li>
     *   <li>1.21-1.21.1: v1_21_R1</li>
     *   <li>1.21.2-1.21.3: v1_21_R2</li>
     *   <li>1.21.4-1.21.10: v1_21_R3</li>
     *   <li>1.21.11+: v1_21_R4</li>
     * </ul>
     *
     * @return the NMS version string
     * @since 1.0.0
     */
    @NotNull
    default String getNmsVersion() {
        return current().getNmsVersion();
    }

    /**
     * Returns the protocol version number for the current version.
     *
     * <p>The protocol version is used for network communication
     * and client compatibility checks.
     *
     * @return the protocol version number
     * @since 1.0.0
     */
    default int getProtocolVersion() {
        return current().getProtocolVersion();
    }

    /**
     * Checks if the server uses Mojang mappings (Paper 1.21.11+).
     *
     * <p>Paper 1.21.11+ uses Mojang-mapped internals instead of Spigot mappings.
     * This affects how NMS classes and methods are accessed.
     *
     * @return true if using Mojang mappings
     * @since 1.0.0
     */
    boolean usesMojangMappings();

    /**
     * Returns the server platform type.
     *
     * @return the server platform (PAPER, SPIGOT, FOLIA, etc.)
     * @since 1.0.0
     */
    @NotNull
    String getPlatform();

    /**
     * Returns the full server version string.
     *
     * <p>This includes the platform name and build information,
     * e.g., "Paper 1.21.4-121" or "Spigot 1.21.1-R0.1-SNAPSHOT".
     *
     * @return the full server version string
     * @since 1.0.0
     */
    @NotNull
    String getServerVersion();

    @Override
    default String getServiceName() {
        return "VersionProvider";
    }
}
