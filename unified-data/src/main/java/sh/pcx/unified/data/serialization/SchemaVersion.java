/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a schema version for serialized data.
 *
 * <p>Schema versions use semantic versioning (major.minor.patch) to track
 * compatibility between different versions of serialized data. This enables
 * forward and backward compatibility through migration.
 *
 * <h2>Version Compatibility Rules</h2>
 * <ul>
 *   <li><b>Patch changes:</b> Fully compatible, no migration needed</li>
 *   <li><b>Minor changes:</b> Backward compatible, may add optional fields</li>
 *   <li><b>Major changes:</b> Breaking changes, requires migration</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create versions
 * SchemaVersion v1 = SchemaVersion.of(1, 0, 0);
 * SchemaVersion v2 = SchemaVersion.of(2, 0, 0);
 *
 * // Compare versions
 * if (v1.isOlderThan(v2)) {
 *     // Need to migrate from v1 to v2
 * }
 *
 * // Check compatibility
 * if (v1.isCompatibleWith(v2)) {
 *     // Can read without migration
 * }
 *
 * // Parse from string
 * SchemaVersion parsed = SchemaVersion.parse("1.2.3");
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>SchemaVersion instances are immutable and therefore thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SchemaMigration
 * @see VersionedData
 */
public final class SchemaVersion implements Comparable<SchemaVersion> {

    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:-(.+))?$");

    private static final SchemaVersion CURRENT = new SchemaVersion(1, 0, 0, null);
    private static final SchemaVersion ZERO = new SchemaVersion(0, 0, 0, null);

    private final int major;
    private final int minor;
    private final int patch;
    private final String preRelease;

    private SchemaVersion(int major, int minor, int patch, @Nullable String preRelease) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version components cannot be negative");
        }
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = preRelease;
    }

    /**
     * Returns the current schema version for the serialization system.
     *
     * @return the current schema version
     * @since 1.0.0
     */
    @NotNull
    public static SchemaVersion current() {
        return CURRENT;
    }

    /**
     * Returns version 0.0.0 (for unversioned data).
     *
     * @return the zero version
     * @since 1.0.0
     */
    @NotNull
    public static SchemaVersion zero() {
        return ZERO;
    }

    /**
     * Creates a schema version with major.minor.patch.
     *
     * @param major the major version
     * @param minor the minor version
     * @param patch the patch version
     * @return a new SchemaVersion
     * @since 1.0.0
     */
    @NotNull
    public static SchemaVersion of(int major, int minor, int patch) {
        return new SchemaVersion(major, minor, patch, null);
    }

    /**
     * Creates a schema version with major.minor (patch defaults to 0).
     *
     * @param major the major version
     * @param minor the minor version
     * @return a new SchemaVersion
     * @since 1.0.0
     */
    @NotNull
    public static SchemaVersion of(int major, int minor) {
        return new SchemaVersion(major, minor, 0, null);
    }

    /**
     * Creates a schema version with only major (minor and patch default to 0).
     *
     * @param major the major version
     * @return a new SchemaVersion
     * @since 1.0.0
     */
    @NotNull
    public static SchemaVersion of(int major) {
        return new SchemaVersion(major, 0, 0, null);
    }

    /**
     * Creates a pre-release version.
     *
     * @param major      the major version
     * @param minor      the minor version
     * @param patch      the patch version
     * @param preRelease the pre-release identifier (e.g., "alpha.1", "beta.2")
     * @return a new SchemaVersion
     * @since 1.0.0
     */
    @NotNull
    public static SchemaVersion preRelease(int major, int minor, int patch, @NotNull String preRelease) {
        Objects.requireNonNull(preRelease, "preRelease cannot be null");
        return new SchemaVersion(major, minor, patch, preRelease);
    }

    /**
     * Parses a version string.
     *
     * <p>Accepted formats:
     * <ul>
     *   <li>"1" - major only</li>
     *   <li>"1.2" - major.minor</li>
     *   <li>"1.2.3" - major.minor.patch</li>
     *   <li>"1.2.3-alpha.1" - with pre-release</li>
     * </ul>
     *
     * @param version the version string
     * @return the parsed SchemaVersion
     * @throws IllegalArgumentException if the version string is invalid
     * @since 1.0.0
     */
    @NotNull
    public static SchemaVersion parse(@NotNull String version) {
        Objects.requireNonNull(version, "version cannot be null");
        Matcher matcher = VERSION_PATTERN.matcher(version.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid version format: " + version);
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
        int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
        String preRelease = matcher.group(4);

        return new SchemaVersion(major, minor, patch, preRelease);
    }

    /**
     * Attempts to parse a version string, returning null on failure.
     *
     * @param version the version string
     * @return the parsed SchemaVersion, or null if invalid
     * @since 1.0.0
     */
    @Nullable
    public static SchemaVersion tryParse(@Nullable String version) {
        if (version == null || version.isEmpty()) {
            return null;
        }
        try {
            return parse(version);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Returns the major version number.
     *
     * @return the major version
     * @since 1.0.0
     */
    public int getMajor() {
        return major;
    }

    /**
     * Returns the minor version number.
     *
     * @return the minor version
     * @since 1.0.0
     */
    public int getMinor() {
        return minor;
    }

    /**
     * Returns the patch version number.
     *
     * @return the patch version
     * @since 1.0.0
     */
    public int getPatch() {
        return patch;
    }

    /**
     * Returns the pre-release identifier.
     *
     * @return the pre-release identifier, or null if not a pre-release
     * @since 1.0.0
     */
    @Nullable
    public String getPreRelease() {
        return preRelease;
    }

    /**
     * Returns whether this is a pre-release version.
     *
     * @return true if this is a pre-release version
     * @since 1.0.0
     */
    public boolean isPreRelease() {
        return preRelease != null;
    }

    /**
     * Returns whether this version is older than another.
     *
     * @param other the version to compare with
     * @return true if this version is older
     * @since 1.0.0
     */
    public boolean isOlderThan(@NotNull SchemaVersion other) {
        return compareTo(other) < 0;
    }

    /**
     * Returns whether this version is newer than another.
     *
     * @param other the version to compare with
     * @return true if this version is newer
     * @since 1.0.0
     */
    public boolean isNewerThan(@NotNull SchemaVersion other) {
        return compareTo(other) > 0;
    }

    /**
     * Returns whether this version is older than or equal to another.
     *
     * @param other the version to compare with
     * @return true if this version is older or equal
     * @since 1.0.0
     */
    public boolean isOlderThanOrEqual(@NotNull SchemaVersion other) {
        return compareTo(other) <= 0;
    }

    /**
     * Returns whether this version is newer than or equal to another.
     *
     * @param other the version to compare with
     * @return true if this version is newer or equal
     * @since 1.0.0
     */
    public boolean isNewerThanOrEqual(@NotNull SchemaVersion other) {
        return compareTo(other) >= 0;
    }

    /**
     * Returns whether this version is compatible with another.
     *
     * <p>Versions are compatible if they have the same major version,
     * and the data version is not newer than this version.
     *
     * @param dataVersion the version of the data
     * @return true if the versions are compatible
     * @since 1.0.0
     */
    public boolean isCompatibleWith(@NotNull SchemaVersion dataVersion) {
        if (this.major != dataVersion.major) {
            return false;
        }
        // Same major, can read older minor/patch versions
        return this.isNewerThanOrEqual(dataVersion);
    }

    /**
     * Returns whether this version requires migration from another.
     *
     * <p>Migration is required if the major version differs.
     *
     * @param fromVersion the version to migrate from
     * @return true if migration is required
     * @since 1.0.0
     */
    public boolean requiresMigrationFrom(@NotNull SchemaVersion fromVersion) {
        return this.major != fromVersion.major;
    }

    /**
     * Returns the next major version.
     *
     * @return a new SchemaVersion with incremented major and reset minor/patch
     * @since 1.0.0
     */
    @NotNull
    public SchemaVersion nextMajor() {
        return new SchemaVersion(major + 1, 0, 0, null);
    }

    /**
     * Returns the next minor version.
     *
     * @return a new SchemaVersion with incremented minor and reset patch
     * @since 1.0.0
     */
    @NotNull
    public SchemaVersion nextMinor() {
        return new SchemaVersion(major, minor + 1, 0, null);
    }

    /**
     * Returns the next patch version.
     *
     * @return a new SchemaVersion with incremented patch
     * @since 1.0.0
     */
    @NotNull
    public SchemaVersion nextPatch() {
        return new SchemaVersion(major, minor, patch + 1, null);
    }

    /**
     * Returns a version with the pre-release stripped.
     *
     * @return a release version
     * @since 1.0.0
     */
    @NotNull
    public SchemaVersion toRelease() {
        return preRelease == null ? this : new SchemaVersion(major, minor, patch, null);
    }

    @Override
    public int compareTo(@NotNull SchemaVersion other) {
        int result = Integer.compare(this.major, other.major);
        if (result != 0) return result;

        result = Integer.compare(this.minor, other.minor);
        if (result != 0) return result;

        result = Integer.compare(this.patch, other.patch);
        if (result != 0) return result;

        // Pre-release versions are always older than release versions
        if (this.preRelease == null && other.preRelease != null) {
            return 1;
        }
        if (this.preRelease != null && other.preRelease == null) {
            return -1;
        }
        if (this.preRelease != null) {
            return this.preRelease.compareTo(other.preRelease);
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchemaVersion that = (SchemaVersion) o;
        return major == that.major &&
                minor == that.minor &&
                patch == that.patch &&
                Objects.equals(preRelease, that.preRelease);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, preRelease);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(major).append('.').append(minor).append('.').append(patch);
        if (preRelease != null) {
            sb.append('-').append(preRelease);
        }
        return sb.toString();
    }

    /**
     * Returns a short version string (major.minor).
     *
     * @return the short version string
     * @since 1.0.0
     */
    @NotNull
    public String toShortString() {
        return major + "." + minor;
    }
}
