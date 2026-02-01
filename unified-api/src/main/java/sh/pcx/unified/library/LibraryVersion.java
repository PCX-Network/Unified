/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package sh.pcx.unified.library;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a semantic version for a library following SemVer 2.0.0 specification.
 *
 * <p>Semantic versioning uses the format: MAJOR.MINOR.PATCH[-PRERELEASE]
 *
 * <ul>
 *   <li><b>MAJOR</b> - Incremented for incompatible API changes</li>
 *   <li><b>MINOR</b> - Incremented for backwards-compatible functionality additions</li>
 *   <li><b>PATCH</b> - Incremented for backwards-compatible bug fixes</li>
 *   <li><b>PRERELEASE</b> - Optional prerelease identifier (e.g., "alpha", "beta.1", "RC1")</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Parse version strings
 * LibraryVersion v1 = LibraryVersion.parse("7.0.2");
 * LibraryVersion v2 = LibraryVersion.parse("4.26.1-SNAPSHOT");
 *
 * // Create versions directly
 * LibraryVersion v3 = new LibraryVersion(2, 0, 17, null);
 * LibraryVersion v4 = new LibraryVersion(1, 0, 0, "beta.1");
 *
 * // Compare versions
 * if (v1.compareTo(v2) > 0) {
 *     System.out.println(v1 + " is newer than " + v2);
 * }
 *
 * // Check compatibility
 * if (v1.isCompatibleWith(v2)) {
 *     // Same major version, can use together
 * }
 * }</pre>
 *
 * <h2>Version Comparison Rules</h2>
 * <ol>
 *   <li>Major versions are compared first</li>
 *   <li>If major versions are equal, minor versions are compared</li>
 *   <li>If minor versions are equal, patch versions are compared</li>
 *   <li>Prerelease versions have lower precedence than release versions</li>
 *   <li>Prerelease versions are compared lexicographically</li>
 * </ol>
 *
 * @param major      The major version number (must be non-negative)
 * @param minor      The minor version number (must be non-negative)
 * @param patch      The patch version number (must be non-negative)
 * @param prerelease The optional prerelease identifier (null for release versions)
 * @author Supatuck
 * @since 1.0.0
 * @see VersionRange
 * @see <a href="https://semver.org/">Semantic Versioning 2.0.0</a>
 */
public record LibraryVersion(int major, int minor, int patch, String prerelease)
        implements Comparable<LibraryVersion> {

    /**
     * Regular expression pattern for parsing semantic version strings.
     *
     * <p>Matches versions in the format: MAJOR.MINOR.PATCH[-PRERELEASE]
     * where MAJOR, MINOR, and PATCH are non-negative integers.
     */
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "^(\\d+)\\.(\\d+)\\.(\\d+)(?:-([A-Za-z0-9.\\-]+))?(?:\\+.*)?$"
    );

    /**
     * Represents version 0.0.0, typically used as a minimum bound.
     */
    public static final LibraryVersion ZERO = new LibraryVersion(0, 0, 0, null);

    /**
     * Creates a new LibraryVersion with validation.
     *
     * @param major      The major version number
     * @param minor      The minor version number
     * @param patch      The patch version number
     * @param prerelease The optional prerelease identifier
     * @throws IllegalArgumentException if any version number is negative
     */
    public LibraryVersion {
        if (major < 0) {
            throw new IllegalArgumentException("Major version must be non-negative: " + major);
        }
        if (minor < 0) {
            throw new IllegalArgumentException("Minor version must be non-negative: " + minor);
        }
        if (patch < 0) {
            throw new IllegalArgumentException("Patch version must be non-negative: " + patch);
        }
        // Normalize empty prerelease to null
        if (prerelease != null && prerelease.isEmpty()) {
            prerelease = null;
        }
    }

    /**
     * Parses a version string into a LibraryVersion.
     *
     * <p>Supported formats:
     * <ul>
     *   <li>{@code "1.0.0"} - Standard release version</li>
     *   <li>{@code "2.0.0-alpha"} - Prerelease version</li>
     *   <li>{@code "3.0.0-beta.1"} - Prerelease with numeric identifier</li>
     *   <li>{@code "4.0.0+build.123"} - Version with build metadata (metadata is ignored)</li>
     *   <li>{@code "7.2.0.RELEASE"} - Spring-style version (converted to 7.2.0-RELEASE)</li>
     * </ul>
     *
     * @param version The version string to parse
     * @return The parsed LibraryVersion
     * @throws IllegalArgumentException if the version string is null, empty, or invalid
     */
    public static LibraryVersion parse(String version) {
        if (version == null || version.isEmpty()) {
            throw new IllegalArgumentException("Version string cannot be null or empty");
        }

        // Handle Spring-style versions like "7.2.0.RELEASE"
        String normalizedVersion = normalizeSpringVersion(version);

        Matcher matcher = VERSION_PATTERN.matcher(normalizedVersion);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid version format: " + version
                    + ". Expected format: MAJOR.MINOR.PATCH[-PRERELEASE]");
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));
        String prerelease = matcher.group(4);

        return new LibraryVersion(major, minor, patch, prerelease);
    }

    /**
     * Normalizes Spring-style version strings to SemVer format.
     *
     * <p>Converts versions like "7.2.0.RELEASE" to "7.2.0-RELEASE".
     *
     * @param version The version string to normalize
     * @return The normalized version string
     */
    private static String normalizeSpringVersion(String version) {
        // Pattern for Spring versions: X.Y.Z.QUALIFIER
        Pattern springPattern = Pattern.compile("^(\\d+\\.\\d+\\.\\d+)\\.([A-Za-z][A-Za-z0-9.\\-]*)$");
        Matcher matcher = springPattern.matcher(version);
        if (matcher.matches()) {
            return matcher.group(1) + "-" + matcher.group(2);
        }
        return version;
    }

    /**
     * Creates a release version (without prerelease identifier).
     *
     * @param major The major version number
     * @param minor The minor version number
     * @param patch The patch version number
     * @return A new LibraryVersion without prerelease identifier
     */
    public static LibraryVersion of(int major, int minor, int patch) {
        return new LibraryVersion(major, minor, patch, null);
    }

    /**
     * Checks if this is a prerelease version.
     *
     * <p>Prerelease versions have lower precedence than their associated
     * release version and are intended for testing and development.
     *
     * @return true if this version has a prerelease identifier
     */
    public boolean isPrerelease() {
        return prerelease != null;
    }

    /**
     * Checks if this version is compatible with another version.
     *
     * <p>Two versions are considered compatible if they share the same
     * major version number. According to SemVer, minor and patch updates
     * should be backwards compatible within the same major version.
     *
     * @param other The version to check compatibility with
     * @return true if the versions are compatible (same major version)
     * @throws NullPointerException if other is null
     */
    public boolean isCompatibleWith(LibraryVersion other) {
        Objects.requireNonNull(other, "Version to compare cannot be null");
        return this.major == other.major;
    }

    /**
     * Checks if this version is newer than another version.
     *
     * @param other The version to compare with
     * @return true if this version is greater than the other version
     * @throws NullPointerException if other is null
     */
    public boolean isNewerThan(LibraryVersion other) {
        return compareTo(other) > 0;
    }

    /**
     * Checks if this version is older than another version.
     *
     * @param other The version to compare with
     * @return true if this version is less than the other version
     * @throws NullPointerException if other is null
     */
    public boolean isOlderThan(LibraryVersion other) {
        return compareTo(other) < 0;
    }

    /**
     * Returns the next major version (X+1.0.0).
     *
     * @return A new LibraryVersion with incremented major version
     */
    public LibraryVersion nextMajor() {
        return new LibraryVersion(major + 1, 0, 0, null);
    }

    /**
     * Returns the next minor version (X.Y+1.0).
     *
     * @return A new LibraryVersion with incremented minor version
     */
    public LibraryVersion nextMinor() {
        return new LibraryVersion(major, minor + 1, 0, null);
    }

    /**
     * Returns the next patch version (X.Y.Z+1).
     *
     * @return A new LibraryVersion with incremented patch version
     */
    public LibraryVersion nextPatch() {
        return new LibraryVersion(major, minor, patch + 1, null);
    }

    /**
     * Compares this version with another version.
     *
     * <p>Comparison follows semantic versioning precedence rules:
     * <ol>
     *   <li>Compare major versions numerically</li>
     *   <li>Compare minor versions numerically</li>
     *   <li>Compare patch versions numerically</li>
     *   <li>A prerelease version has lower precedence than a release version</li>
     *   <li>Prerelease identifiers are compared lexicographically</li>
     * </ol>
     *
     * @param other The version to compare to
     * @return negative if this &lt; other, zero if equal, positive if this &gt; other
     * @throws NullPointerException if other is null
     */
    @Override
    public int compareTo(LibraryVersion other) {
        Objects.requireNonNull(other, "Version to compare cannot be null");

        // Compare major versions
        int result = Integer.compare(this.major, other.major);
        if (result != 0) {
            return result;
        }

        // Compare minor versions
        result = Integer.compare(this.minor, other.minor);
        if (result != 0) {
            return result;
        }

        // Compare patch versions
        result = Integer.compare(this.patch, other.patch);
        if (result != 0) {
            return result;
        }

        // Handle prerelease comparison
        if (this.prerelease == null && other.prerelease == null) {
            return 0; // Both are release versions
        }
        if (this.prerelease == null) {
            return 1; // Release > prerelease
        }
        if (other.prerelease == null) {
            return -1; // Prerelease < release
        }

        // Compare prerelease identifiers
        return comparePrerelease(this.prerelease, other.prerelease);
    }

    /**
     * Compares two prerelease strings according to SemVer rules.
     *
     * @param pr1 First prerelease identifier
     * @param pr2 Second prerelease identifier
     * @return comparison result
     */
    private static int comparePrerelease(String pr1, String pr2) {
        String[] parts1 = pr1.split("\\.");
        String[] parts2 = pr2.split("\\.");

        int length = Math.min(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            String p1 = parts1[i];
            String p2 = parts2[i];

            // Try to compare as numbers
            boolean p1Numeric = isNumeric(p1);
            boolean p2Numeric = isNumeric(p2);

            if (p1Numeric && p2Numeric) {
                int result = Integer.compare(Integer.parseInt(p1), Integer.parseInt(p2));
                if (result != 0) {
                    return result;
                }
            } else if (p1Numeric) {
                return -1; // Numeric < non-numeric
            } else if (p2Numeric) {
                return 1; // Non-numeric > numeric
            } else {
                int result = p1.compareTo(p2);
                if (result != 0) {
                    return result;
                }
            }
        }

        // Longer prerelease has higher precedence
        return Integer.compare(parts1.length, parts2.length);
    }

    /**
     * Checks if a string represents a numeric value.
     *
     * @param str The string to check
     * @return true if the string is all digits
     */
    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the string representation of this version.
     *
     * <p>The format is: MAJOR.MINOR.PATCH[-PRERELEASE]
     *
     * @return The version string
     */
    @Override
    public String toString() {
        if (prerelease != null) {
            return major + "." + minor + "." + patch + "-" + prerelease;
        }
        return major + "." + minor + "." + patch;
    }
}
