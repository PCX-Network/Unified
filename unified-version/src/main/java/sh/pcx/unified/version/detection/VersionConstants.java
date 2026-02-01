/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.version.detection;

import sh.pcx.unified.server.MinecraftVersion;
import org.jetbrains.annotations.NotNull;

/**
 * Version constant definitions for the NMS compatibility layer.
 *
 * <p>This class provides convenient constants for Minecraft versions and their
 * corresponding NMS package versions. Use these constants for version comparisons
 * and feature detection.
 *
 * <h2>NMS Version Mapping</h2>
 * <table>
 *   <tr><th>NMS Version</th><th>Minecraft Versions</th><th>Notes</th></tr>
 *   <tr><td>v1_20_R4</td><td>1.20.5, 1.20.6</td><td>Data Components introduced</td></tr>
 *   <tr><td>v1_21_R1</td><td>1.21, 1.21.1</td><td>Tricky Trials update</td></tr>
 *   <tr><td>v1_21_R2</td><td>1.21.2, 1.21.3</td><td>Bundles update</td></tr>
 *   <tr><td>v1_21_R3</td><td>1.21.4 - 1.21.10</td><td>Pale Garden update</td></tr>
 *   <tr><td>v1_21_R4</td><td>1.21.11+</td><td>Registry gamerules, Mojang mappings</td></tr>
 * </table>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Check NMS version string
 * String nmsVersion = VersionConstants.getNmsVersion(MinecraftVersion.V1_21_4);
 * // Returns: "v1_21_R3"
 *
 * // Get version range for NMS version
 * MinecraftVersion min = VersionConstants.getMinVersion(NmsVersion.V1_21_R1);
 * MinecraftVersion max = VersionConstants.getMaxVersion(NmsVersion.V1_21_R1);
 * // Returns: 1.21.0, 1.21.1
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class VersionConstants {

    private VersionConstants() {
        // Utility class - prevent instantiation
    }

    // ===== NMS Version Strings =====

    /** NMS version for 1.20.5-1.20.6. */
    public static final String NMS_V1_20_R4 = "v1_20_R4";

    /** NMS version for 1.21-1.21.1. */
    public static final String NMS_V1_21_R1 = "v1_21_R1";

    /** NMS version for 1.21.2-1.21.3. */
    public static final String NMS_V1_21_R2 = "v1_21_R2";

    /** NMS version for 1.21.4-1.21.10. */
    public static final String NMS_V1_21_R3 = "v1_21_R3";

    /** NMS version for 1.21.11+. */
    public static final String NMS_V1_21_R4 = "v1_21_R4";

    // ===== Minecraft Version Constants =====

    /** Minecraft 1.20.5 - Introduction of Data Components. */
    public static final MinecraftVersion V1_20_5 = MinecraftVersion.V1_20_5;

    /** Minecraft 1.20.6 - Last 1.20.x version. */
    public static final MinecraftVersion V1_20_6 = MinecraftVersion.V1_20_6;

    /** Minecraft 1.21 - Tricky Trials update. */
    public static final MinecraftVersion V1_21 = MinecraftVersion.V1_21;

    /** Minecraft 1.21.1 - Bug fixes. */
    public static final MinecraftVersion V1_21_1 = MinecraftVersion.V1_21_1;

    /** Minecraft 1.21.2 - Bundles update. */
    public static final MinecraftVersion V1_21_2 = MinecraftVersion.V1_21_2;

    /** Minecraft 1.21.3 - Bug fixes. */
    public static final MinecraftVersion V1_21_3 = MinecraftVersion.V1_21_3;

    /** Minecraft 1.21.4 - Pale Garden update. */
    public static final MinecraftVersion V1_21_4 = MinecraftVersion.V1_21_4;

    /** Minecraft 1.21.5 - Bug fixes. */
    public static final MinecraftVersion V1_21_5 = MinecraftVersion.V1_21_5;

    /** Minecraft 1.21.10 - Last pre-registry gamerules version. */
    public static final MinecraftVersion V1_21_10 = MinecraftVersion.V1_21_10;

    /** Minecraft 1.21.11 - Registry-based gamerules, Mojang mappings in Paper. */
    public static final MinecraftVersion V1_21_11 = MinecraftVersion.V1_21_11;

    // ===== Supported Version Range =====

    /** Minimum supported Minecraft version. */
    public static final MinecraftVersion MINIMUM_SUPPORTED = V1_20_5;

    /** Maximum supported Minecraft version. */
    public static final MinecraftVersion MAXIMUM_SUPPORTED = V1_21_11;

    // ===== Protocol Versions =====

    /** Protocol version for 1.20.5-1.20.6. */
    public static final int PROTOCOL_V1_20_5 = 766;

    /** Protocol version for 1.21-1.21.1. */
    public static final int PROTOCOL_V1_21 = 767;

    /** Protocol version for 1.21.2-1.21.3. */
    public static final int PROTOCOL_V1_21_2 = 768;

    /** Protocol version for 1.21.4. */
    public static final int PROTOCOL_V1_21_4 = 769;

    /** Protocol version for 1.21.11. */
    public static final int PROTOCOL_V1_21_11 = 770;

    // ===== Utility Methods =====

    /**
     * Gets the NMS version string for a Minecraft version.
     *
     * @param version the Minecraft version
     * @return the NMS version string
     * @since 1.0.0
     */
    @NotNull
    public static String getNmsVersion(@NotNull MinecraftVersion version) {
        return version.getNmsVersion();
    }

    /**
     * Gets the minimum Minecraft version for an NMS version.
     *
     * @param nmsVersion the NMS version string
     * @return the minimum Minecraft version
     * @throws IllegalArgumentException if the NMS version is unknown
     * @since 1.0.0
     */
    @NotNull
    public static MinecraftVersion getMinVersion(@NotNull String nmsVersion) {
        return switch (nmsVersion) {
            case NMS_V1_20_R4 -> V1_20_5;
            case NMS_V1_21_R1 -> V1_21;
            case NMS_V1_21_R2 -> V1_21_2;
            case NMS_V1_21_R3 -> V1_21_4;
            case NMS_V1_21_R4 -> V1_21_11;
            default -> throw new IllegalArgumentException("Unknown NMS version: " + nmsVersion);
        };
    }

    /**
     * Gets the maximum Minecraft version for an NMS version.
     *
     * @param nmsVersion the NMS version string
     * @return the maximum Minecraft version
     * @throws IllegalArgumentException if the NMS version is unknown
     * @since 1.0.0
     */
    @NotNull
    public static MinecraftVersion getMaxVersion(@NotNull String nmsVersion) {
        return switch (nmsVersion) {
            case NMS_V1_20_R4 -> V1_20_6;
            case NMS_V1_21_R1 -> V1_21_1;
            case NMS_V1_21_R2 -> V1_21_3;
            case NMS_V1_21_R3 -> V1_21_10;
            case NMS_V1_21_R4 -> V1_21_11; // Current max, will increase
            default -> throw new IllegalArgumentException("Unknown NMS version: " + nmsVersion);
        };
    }

    /**
     * Gets all supported NMS version strings.
     *
     * @return array of NMS version strings
     * @since 1.0.0
     */
    @NotNull
    public static String[] getSupportedNmsVersions() {
        return new String[]{
                NMS_V1_20_R4,
                NMS_V1_21_R1,
                NMS_V1_21_R2,
                NMS_V1_21_R3,
                NMS_V1_21_R4
        };
    }

    /**
     * Checks if a Minecraft version is supported.
     *
     * @param version the version to check
     * @return true if supported
     * @since 1.0.0
     */
    public static boolean isSupported(@NotNull MinecraftVersion version) {
        return version.isBetween(MINIMUM_SUPPORTED, MAXIMUM_SUPPORTED);
    }

    /**
     * Checks if an NMS version string is valid.
     *
     * @param nmsVersion the NMS version to check
     * @return true if valid
     * @since 1.0.0
     */
    public static boolean isValidNmsVersion(@NotNull String nmsVersion) {
        return switch (nmsVersion) {
            case NMS_V1_20_R4, NMS_V1_21_R1, NMS_V1_21_R2, NMS_V1_21_R3, NMS_V1_21_R4 -> true;
            default -> false;
        };
    }

    /**
     * NMS version enumeration for type-safe version handling.
     *
     * @since 1.0.0
     */
    public enum NmsVersion {
        /** 1.20.5-1.20.6. */
        V1_20_R4(NMS_V1_20_R4, V1_20_5, V1_20_6),

        /** 1.21-1.21.1. */
        V1_21_R1(NMS_V1_21_R1, V1_21, V1_21_1),

        /** 1.21.2-1.21.3. */
        V1_21_R2(NMS_V1_21_R2, V1_21_2, V1_21_3),

        /** 1.21.4-1.21.10. */
        V1_21_R3(NMS_V1_21_R3, V1_21_4, V1_21_10),

        /** 1.21.11+. */
        V1_21_R4(NMS_V1_21_R4, V1_21_11, V1_21_11);

        private final String versionString;
        private final MinecraftVersion minVersion;
        private final MinecraftVersion maxVersion;

        NmsVersion(String versionString, MinecraftVersion minVersion, MinecraftVersion maxVersion) {
            this.versionString = versionString;
            this.minVersion = minVersion;
            this.maxVersion = maxVersion;
        }

        /**
         * Gets the NMS version string.
         *
         * @return the version string
         */
        @NotNull
        public String getVersionString() {
            return versionString;
        }

        /**
         * Gets the minimum Minecraft version.
         *
         * @return the min version
         */
        @NotNull
        public MinecraftVersion getMinVersion() {
            return minVersion;
        }

        /**
         * Gets the maximum Minecraft version.
         *
         * @return the max version
         */
        @NotNull
        public MinecraftVersion getMaxVersion() {
            return maxVersion;
        }

        /**
         * Checks if a Minecraft version matches this NMS version.
         *
         * @param version the version to check
         * @return true if matches
         */
        public boolean matches(@NotNull MinecraftVersion version) {
            return version.isBetween(minVersion, maxVersion);
        }

        /**
         * Gets the NMS version for a Minecraft version.
         *
         * @param version the Minecraft version
         * @return the NMS version
         * @throws IllegalArgumentException if no matching NMS version
         */
        @NotNull
        public static NmsVersion fromMinecraftVersion(@NotNull MinecraftVersion version) {
            for (NmsVersion nms : values()) {
                if (nms.matches(version)) {
                    return nms;
                }
            }
            throw new IllegalArgumentException("No NMS version for: " + version);
        }

        /**
         * Gets the NMS version from a version string.
         *
         * @param versionString the version string
         * @return the NMS version
         * @throws IllegalArgumentException if no matching NMS version
         */
        @NotNull
        public static NmsVersion fromString(@NotNull String versionString) {
            for (NmsVersion nms : values()) {
                if (nms.versionString.equals(versionString)) {
                    return nms;
                }
            }
            throw new IllegalArgumentException("Unknown NMS version: " + versionString);
        }
    }
}
