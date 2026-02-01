/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.server;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable record representing a Minecraft version with comparison support.
 *
 * <p>This record supports both the traditional versioning scheme (1.20.5, 1.21.11)
 * and Mojang's new 2026 versioning scheme (26.1, 26.2).
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Parse version strings
 * MinecraftVersion v1 = MinecraftVersion.parse("1.21.4");
 * MinecraftVersion v2 = MinecraftVersion.parse("1.20.5");
 *
 * // Compare versions
 * if (v1.compareTo(v2) > 0) {
 *     // v1 is newer than v2
 * }
 *
 * // Check version requirements
 * MinecraftVersion current = server.getMinecraftVersion();
 * if (current.isAtLeast(MinecraftVersion.V1_21)) {
 *     // Use 1.21+ features
 * }
 *
 * // Create versions programmatically
 * MinecraftVersion custom = new MinecraftVersion(1, 21, 11);
 *
 * // Use predefined constants
 * if (current.isOlderThan(MinecraftVersion.V1_20_5)) {
 *     throw new UnsupportedOperationException("Requires 1.20.5 or newer");
 * }
 * }</pre>
 *
 * @param major the major version component (e.g., 1 for 1.21.4, 26 for 26.1)
 * @param minor the minor version component (e.g., 21 for 1.21.4, 1 for 26.1)
 * @param patch the patch version component (e.g., 4 for 1.21.4, 0 for 26.1)
 *
 * @since 1.0.0
 * @author Supatuck
 */
public record MinecraftVersion(int major, int minor, int patch) implements Comparable<MinecraftVersion> {

    // Version parsing patterns
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:[-.].*)?");

    // Common version constants
    /** Minecraft 1.20.5 - Component-based items. */
    public static final MinecraftVersion V1_20_5 = new MinecraftVersion(1, 20, 5);

    /** Minecraft 1.20.6 - Last 1.20.x release. */
    public static final MinecraftVersion V1_20_6 = new MinecraftVersion(1, 20, 6);

    /** Minecraft 1.21 - Tricky Trials update. */
    public static final MinecraftVersion V1_21 = new MinecraftVersion(1, 21, 0);

    /** Minecraft 1.21.1 - Bug fixes. */
    public static final MinecraftVersion V1_21_1 = new MinecraftVersion(1, 21, 1);

    /** Minecraft 1.21.2 - Bundles update. */
    public static final MinecraftVersion V1_21_2 = new MinecraftVersion(1, 21, 2);

    /** Minecraft 1.21.3 - Bug fixes. */
    public static final MinecraftVersion V1_21_3 = new MinecraftVersion(1, 21, 3);

    /** Minecraft 1.21.4 - Pale Garden update. */
    public static final MinecraftVersion V1_21_4 = new MinecraftVersion(1, 21, 4);

    /** Minecraft 1.21.5 - Bug fixes. */
    public static final MinecraftVersion V1_21_5 = new MinecraftVersion(1, 21, 5);

    /** Minecraft 1.21.10 - Pre-registry gamerules. */
    public static final MinecraftVersion V1_21_10 = new MinecraftVersion(1, 21, 10);

    /** Minecraft 1.21.11 - Registry-based gamerules. */
    public static final MinecraftVersion V1_21_11 = new MinecraftVersion(1, 21, 11);

    /** Minimum supported version for UnifiedPlugin API. */
    public static final MinecraftVersion MINIMUM_SUPPORTED = V1_20_5;

    /** Maximum supported version for UnifiedPlugin API. */
    public static final MinecraftVersion MAXIMUM_SUPPORTED = V1_21_11;

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if any component is negative
     */
    public MinecraftVersion {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException(
                    "Version components must be non-negative: " + major + "." + minor + "." + patch);
        }
    }

    /**
     * Creates a MinecraftVersion without a patch number.
     *
     * @param major the major version
     * @param minor the minor version
     */
    public MinecraftVersion(int major, int minor) {
        this(major, minor, 0);
    }

    /**
     * Parses a version string into a MinecraftVersion.
     *
     * <p>Supports formats like:
     * <ul>
     *   <li>1.21.4</li>
     *   <li>1.21</li>
     *   <li>1.21.4-pre1</li>
     *   <li>26.1</li>
     * </ul>
     *
     * @param version the version string to parse
     * @return the parsed MinecraftVersion
     * @throws IllegalArgumentException if the version string is invalid
     * @since 1.0.0
     */
    @NotNull
    public static MinecraftVersion parse(@NotNull String version) {
        Objects.requireNonNull(version, "Version string cannot be null");

        Matcher matcher = VERSION_PATTERN.matcher(version.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid version format: " + version);
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;

        return new MinecraftVersion(major, minor, patch);
    }

    /**
     * Attempts to parse a version string, returning null on failure.
     *
     * @param version the version string to parse
     * @return the parsed MinecraftVersion, or null if parsing fails
     * @since 1.0.0
     */
    @Nullable
    public static MinecraftVersion tryParse(@Nullable String version) {
        if (version == null || version.isBlank()) {
            return null;
        }
        try {
            return parse(version);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Compares this version with another version.
     *
     * @param other the version to compare to
     * @return negative if this is older, positive if newer, 0 if equal
     * @since 1.0.0
     */
    @Override
    public int compareTo(@NotNull MinecraftVersion other) {
        int majorCompare = Integer.compare(this.major, other.major);
        if (majorCompare != 0) return majorCompare;

        int minorCompare = Integer.compare(this.minor, other.minor);
        if (minorCompare != 0) return minorCompare;

        return Integer.compare(this.patch, other.patch);
    }

    /**
     * Checks if this version is at least the specified version.
     *
     * @param other the minimum version required
     * @return true if this version is equal to or newer than the specified version
     * @since 1.0.0
     */
    public boolean isAtLeast(@NotNull MinecraftVersion other) {
        return compareTo(other) >= 0;
    }

    /**
     * Checks if this version is older than the specified version.
     *
     * @param other the version to compare against
     * @return true if this version is older
     * @since 1.0.0
     */
    public boolean isOlderThan(@NotNull MinecraftVersion other) {
        return compareTo(other) < 0;
    }

    /**
     * Checks if this version is newer than the specified version.
     *
     * @param other the version to compare against
     * @return true if this version is newer
     * @since 1.0.0
     */
    public boolean isNewerThan(@NotNull MinecraftVersion other) {
        return compareTo(other) > 0;
    }

    /**
     * Checks if this version is within the specified range (inclusive).
     *
     * @param min the minimum version
     * @param max the maximum version
     * @return true if this version is within the range
     * @since 1.0.0
     */
    public boolean isBetween(@NotNull MinecraftVersion min, @NotNull MinecraftVersion max) {
        return isAtLeast(min) && !isNewerThan(max);
    }

    /**
     * Checks if this version is supported by the UnifiedPlugin API.
     *
     * @return true if this version is within the supported range
     * @since 1.0.0
     */
    public boolean isSupported() {
        return isBetween(MINIMUM_SUPPORTED, MAXIMUM_SUPPORTED);
    }

    /**
     * Checks if this version uses the new 2026 versioning scheme.
     *
     * @return true if major version is 26 or higher
     * @since 1.0.0
     */
    public boolean isNewVersioningScheme() {
        return major >= 26;
    }

    /**
     * Returns the NMS version string for this Minecraft version.
     *
     * <p>This is used for version-specific NMS access on Bukkit-based servers.
     *
     * @return the NMS version string (e.g., "v1_21_R1")
     * @since 1.0.0
     */
    @NotNull
    public String getNmsVersion() {
        if (major == 1 && minor == 20 && patch >= 5) {
            return "v1_20_R4";
        } else if (major == 1 && minor == 21) {
            if (patch <= 1) {
                return "v1_21_R1";
            } else if (patch <= 3) {
                return "v1_21_R2";
            } else if (patch <= 10) {
                return "v1_21_R3";
            } else {
                return "v1_21_R4";
            }
        }
        return "v" + major + "_" + minor + "_R1";
    }

    /**
     * Returns the protocol version number for this Minecraft version.
     *
     * @return the protocol version number
     * @since 1.0.0
     */
    public int getProtocolVersion() {
        // Common protocol versions
        if (equals(V1_21_11)) return 770;
        if (equals(V1_21_4)) return 769;
        if (equals(V1_21_3)) return 768;
        if (equals(V1_21_2)) return 768;
        if (equals(V1_21_1)) return 767;
        if (equals(V1_21)) return 767;
        if (equals(V1_20_6)) return 766;
        if (equals(V1_20_5)) return 766;
        return -1; // Unknown
    }

    /**
     * Returns a string representation of this version.
     *
     * @return the version string (e.g., "1.21.4")
     * @since 1.0.0
     */
    @Override
    @NotNull
    public String toString() {
        if (patch == 0) {
            return major + "." + minor;
        }
        return major + "." + minor + "." + patch;
    }

    /**
     * Returns a full string representation including "Minecraft" prefix.
     *
     * @return the full version string (e.g., "Minecraft 1.21.4")
     * @since 1.0.0
     */
    @NotNull
    public String toFullString() {
        return "Minecraft " + this;
    }
}
