/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package sh.pcx.unified.library;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a range of acceptable library versions for dependency resolution.
 *
 * <p>Version ranges allow plugins to specify flexible version requirements,
 * ensuring compatibility while allowing updates within acceptable bounds.
 *
 * <h2>Range Notation</h2>
 * <table border="1">
 *   <tr><th>Notation</th><th>Description</th><th>Example</th></tr>
 *   <tr><td>[min,max]</td><td>Inclusive range</td><td>[1.0.0,2.0.0] matches 1.0.0 to 2.0.0</td></tr>
 *   <tr><td>(min,max)</td><td>Exclusive range</td><td>(1.0.0,2.0.0) excludes 1.0.0 and 2.0.0</td></tr>
 *   <tr><td>[min,max)</td><td>Min inclusive, max exclusive</td><td>[1.0.0,2.0.0) includes 1.0.0, excludes 2.0.0</td></tr>
 *   <tr><td>[min,)</td><td>Minimum only</td><td>[1.0.0,) matches 1.0.0 or higher</td></tr>
 *   <tr><td>(,max]</td><td>Maximum only</td><td>(,2.0.0] matches up to 2.0.0</td></tr>
 * </table>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Create range requiring version 7.x
 * VersionRange range = VersionRange.parse("[7.0.0,8.0.0)");
 *
 * // Check if a version satisfies the range
 * LibraryVersion version = LibraryVersion.parse("7.0.2");
 * if (range.contains(version)) {
 *     System.out.println("Version " + version + " is compatible");
 * }
 *
 * // Create ranges programmatically
 * VersionRange minOnly = VersionRange.atLeast(LibraryVersion.of(4, 0, 0));
 * VersionRange exact = VersionRange.exactly(LibraryVersion.parse("2.0.17"));
 * VersionRange compatible = VersionRange.compatibleWith(LibraryVersion.of(5, 0, 0));
 *
 * // Find intersection of ranges
 * VersionRange intersection = range.intersect(anotherRange);
 * }</pre>
 *
 * @param min          The minimum version (null for unbounded)
 * @param max          The maximum version (null for unbounded)
 * @param minInclusive Whether the minimum is inclusive
 * @param maxInclusive Whether the maximum is inclusive
 * @author Supatuck
 * @since 1.0.0
 * @see LibraryVersion
 * @see LibraryDependency
 */
public record VersionRange(
        LibraryVersion min,
        LibraryVersion max,
        boolean minInclusive,
        boolean maxInclusive
) {

    /**
     * Pattern for parsing version range strings.
     */
    private static final Pattern RANGE_PATTERN = Pattern.compile(
            "^([\\[\\(])\\s*([^,]*)\\s*,\\s*([^\\]\\)]*)\\s*([\\]\\)])$"
    );

    /**
     * A range that accepts any version.
     */
    public static final VersionRange ANY = new VersionRange(null, null, true, true);

    /**
     * Creates a new VersionRange with validation.
     *
     * @param min          The minimum version (null for unbounded)
     * @param max          The maximum version (null for unbounded)
     * @param minInclusive Whether the minimum is inclusive
     * @param maxInclusive Whether the maximum is inclusive
     * @throws IllegalArgumentException if min is greater than max
     */
    public VersionRange {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                    "Minimum version " + min + " cannot be greater than maximum version " + max);
        }
    }

    /**
     * Parses a version range string into a VersionRange.
     *
     * <p>Supported formats:
     * <ul>
     *   <li>{@code "[1.0.0,2.0.0]"} - Inclusive range</li>
     *   <li>{@code "(1.0.0,2.0.0)"} - Exclusive range</li>
     *   <li>{@code "[1.0.0,2.0.0)"} - Min inclusive, max exclusive</li>
     *   <li>{@code "[1.0.0,)"} - Minimum version only</li>
     *   <li>{@code "(,2.0.0]"} - Maximum version only</li>
     *   <li>{@code "1.0.0"} - Exact version (shorthand for [1.0.0,1.0.0])</li>
     * </ul>
     *
     * @param range The range string to parse
     * @return The parsed VersionRange
     * @throws IllegalArgumentException if the range string is invalid
     */
    public static VersionRange parse(String range) {
        if (range == null || range.isEmpty()) {
            throw new IllegalArgumentException("Range string cannot be null or empty");
        }

        range = range.trim();

        // Check for exact version shorthand
        if (!range.startsWith("[") && !range.startsWith("(")) {
            LibraryVersion exact = LibraryVersion.parse(range);
            return new VersionRange(exact, exact, true, true);
        }

        Matcher matcher = RANGE_PATTERN.matcher(range);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid version range format: " + range);
        }

        boolean minInclusive = "[".equals(matcher.group(1));
        boolean maxInclusive = "]".equals(matcher.group(4));
        String minStr = matcher.group(2).trim();
        String maxStr = matcher.group(3).trim();

        LibraryVersion min = minStr.isEmpty() ? null : LibraryVersion.parse(minStr);
        LibraryVersion max = maxStr.isEmpty() ? null : LibraryVersion.parse(maxStr);

        return new VersionRange(min, max, minInclusive, maxInclusive);
    }

    /**
     * Creates a range that matches exactly one version.
     *
     * @param version The exact version to match
     * @return A range that only matches the specified version
     * @throws NullPointerException if version is null
     */
    public static VersionRange exactly(LibraryVersion version) {
        Objects.requireNonNull(version, "Version cannot be null");
        return new VersionRange(version, version, true, true);
    }

    /**
     * Creates a range that matches the specified version or higher.
     *
     * @param version The minimum version
     * @return A range with no upper bound
     * @throws NullPointerException if version is null
     */
    public static VersionRange atLeast(LibraryVersion version) {
        Objects.requireNonNull(version, "Version cannot be null");
        return new VersionRange(version, null, true, true);
    }

    /**
     * Creates a range that matches versions up to and including the specified version.
     *
     * @param version The maximum version
     * @return A range with no lower bound
     * @throws NullPointerException if version is null
     */
    public static VersionRange atMost(LibraryVersion version) {
        Objects.requireNonNull(version, "Version cannot be null");
        return new VersionRange(null, version, true, true);
    }

    /**
     * Creates a range that matches any version compatible with the specified version.
     *
     * <p>Compatible versions share the same major version. For example,
     * {@code compatibleWith(7.0.0)} matches versions [7.0.0, 8.0.0).
     *
     * @param version The version to be compatible with
     * @return A range matching compatible versions
     * @throws NullPointerException if version is null
     */
    public static VersionRange compatibleWith(LibraryVersion version) {
        Objects.requireNonNull(version, "Version cannot be null");
        return new VersionRange(version, version.nextMajor(), true, false);
    }

    /**
     * Creates a range between two versions (both inclusive).
     *
     * @param min The minimum version
     * @param max The maximum version
     * @return A range from min to max (inclusive)
     * @throws NullPointerException if either version is null
     */
    public static VersionRange between(LibraryVersion min, LibraryVersion max) {
        Objects.requireNonNull(min, "Minimum version cannot be null");
        Objects.requireNonNull(max, "Maximum version cannot be null");
        return new VersionRange(min, max, true, true);
    }

    /**
     * Checks if a version is within this range.
     *
     * @param version The version to check
     * @return true if the version satisfies this range
     * @throws NullPointerException if version is null
     */
    public boolean contains(LibraryVersion version) {
        Objects.requireNonNull(version, "Version cannot be null");

        // Check minimum bound
        if (min != null) {
            int cmp = version.compareTo(min);
            if (minInclusive ? cmp < 0 : cmp <= 0) {
                return false;
            }
        }

        // Check maximum bound
        if (max != null) {
            int cmp = version.compareTo(max);
            if (maxInclusive ? cmp > 0 : cmp >= 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if this range overlaps with another range.
     *
     * @param other The other range to check
     * @return true if the ranges have any versions in common
     * @throws NullPointerException if other is null
     */
    public boolean overlaps(VersionRange other) {
        Objects.requireNonNull(other, "Range cannot be null");

        // If either range is unbounded on both ends, they overlap
        if ((this.min == null && this.max == null) || (other.min == null && other.max == null)) {
            return true;
        }

        // Check if this.max < other.min (no overlap)
        if (this.max != null && other.min != null) {
            int cmp = this.max.compareTo(other.min);
            if (cmp < 0 || (cmp == 0 && (!this.maxInclusive || !other.minInclusive))) {
                return false;
            }
        }

        // Check if other.max < this.min (no overlap)
        if (other.max != null && this.min != null) {
            int cmp = other.max.compareTo(this.min);
            if (cmp < 0 || (cmp == 0 && (!other.maxInclusive || !this.minInclusive))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Computes the intersection of this range with another range.
     *
     * @param other The other range
     * @return The intersection range, or null if ranges do not overlap
     * @throws NullPointerException if other is null
     */
    public VersionRange intersect(VersionRange other) {
        Objects.requireNonNull(other, "Range cannot be null");

        if (!overlaps(other)) {
            return null;
        }

        // Determine new minimum
        LibraryVersion newMin;
        boolean newMinInclusive;
        if (this.min == null) {
            newMin = other.min;
            newMinInclusive = other.minInclusive;
        } else if (other.min == null) {
            newMin = this.min;
            newMinInclusive = this.minInclusive;
        } else {
            int cmp = this.min.compareTo(other.min);
            if (cmp > 0) {
                newMin = this.min;
                newMinInclusive = this.minInclusive;
            } else if (cmp < 0) {
                newMin = other.min;
                newMinInclusive = other.minInclusive;
            } else {
                newMin = this.min;
                newMinInclusive = this.minInclusive && other.minInclusive;
            }
        }

        // Determine new maximum
        LibraryVersion newMax;
        boolean newMaxInclusive;
        if (this.max == null) {
            newMax = other.max;
            newMaxInclusive = other.maxInclusive;
        } else if (other.max == null) {
            newMax = this.max;
            newMaxInclusive = this.maxInclusive;
        } else {
            int cmp = this.max.compareTo(other.max);
            if (cmp < 0) {
                newMax = this.max;
                newMaxInclusive = this.maxInclusive;
            } else if (cmp > 0) {
                newMax = other.max;
                newMaxInclusive = other.maxInclusive;
            } else {
                newMax = this.max;
                newMaxInclusive = this.maxInclusive && other.maxInclusive;
            }
        }

        return new VersionRange(newMin, newMax, newMinInclusive, newMaxInclusive);
    }

    /**
     * Checks if this range is unbounded (accepts any version).
     *
     * @return true if both min and max are null
     */
    public boolean isUnbounded() {
        return min == null && max == null;
    }

    /**
     * Checks if this range matches exactly one version.
     *
     * @return true if min equals max and both bounds are inclusive
     */
    public boolean isExact() {
        return min != null && max != null && minInclusive && maxInclusive && min.equals(max);
    }

    /**
     * Returns the string representation of this range.
     *
     * <p>The format follows the standard range notation:
     * {@code [min,max]} for inclusive, {@code (min,max)} for exclusive.
     *
     * @return The range string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(minInclusive ? '[' : '(');
        sb.append(min != null ? min.toString() : "");
        sb.append(',');
        sb.append(max != null ? max.toString() : "");
        sb.append(maxInclusive ? ']' : ')');
        return sb.toString();
    }
}
