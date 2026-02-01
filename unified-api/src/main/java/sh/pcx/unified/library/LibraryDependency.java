/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package sh.pcx.unified.library;

import java.util.Objects;

/**
 * Record representing a plugin's dependency on a shared library.
 *
 * <p>LibraryDependency captures the relationship between a plugin and a
 * library it needs, including version requirements and whether the
 * dependency is mandatory or optional.
 *
 * <h2>Dependency Types</h2>
 * <ul>
 *   <li><b>Required</b>: Plugin cannot function without this library.
 *       If not available, plugin loading fails.</li>
 *   <li><b>Optional</b>: Plugin can function without this library.
 *       Features using the library are gracefully disabled.</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Required dependency on HikariCP 7.x
 * LibraryDependency hikari = new LibraryDependency(
 *     "hikaricp",
 *     VersionRange.parse("[7.0.0,8.0.0)"),
 *     true
 * );
 *
 * // Optional dependency on Redis
 * LibraryDependency redis = LibraryDependency.optional(
 *     "jedis",
 *     VersionRange.atLeast(LibraryVersion.of(7, 0, 0))
 * );
 *
 * // Any version of Gson
 * LibraryDependency gson = LibraryDependency.required("gson");
 *
 * // Check if dependency is satisfied
 * if (hikari.isSatisfiedBy(Libraries.getLibrary("hikaricp"))) {
 *     // Use HikariCP
 * }
 *
 * // Use in plugin configuration
 * public class MyPlugin implements PluginLibraries {
 *     public Collection<LibraryDependency> getRequiredLibraries() {
 *         return List.of(
 *             LibraryDependency.required("guice", "[7.0.0,8.0.0)"),
 *             LibraryDependency.required("hikaricp", "[7.0.0,8.0.0)")
 *         );
 *     }
 * }
 * }</pre>
 *
 * <h2>Version Range Syntax</h2>
 * <ul>
 *   <li>{@code "[1.0.0,2.0.0]"} - Inclusive range</li>
 *   <li>{@code "[1.0.0,2.0.0)"} - Include min, exclude max</li>
 *   <li>{@code "[1.0.0,)"} - Minimum version only</li>
 *   <li>{@code "(,2.0.0]"} - Maximum version only</li>
 * </ul>
 *
 * @param name         The library name (case-insensitive)
 * @param versionRange The acceptable version range
 * @param required     Whether the dependency is required
 * @author Supatuck
 * @since 1.0.0
 * @see PluginLibraries
 * @see DependencyResolver
 * @see VersionRange
 */
public record LibraryDependency(
        String name,
        VersionRange versionRange,
        boolean required
) {

    /**
     * Creates a new LibraryDependency with validation.
     *
     * @param name         The library name
     * @param versionRange The acceptable version range
     * @param required     Whether the dependency is required
     * @throws NullPointerException     if name or versionRange is null
     * @throws IllegalArgumentException if name is empty
     */
    public LibraryDependency {
        Objects.requireNonNull(name, "Library name cannot be null");
        Objects.requireNonNull(versionRange, "Version range cannot be null");

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Library name cannot be empty");
        }

        // Normalize name to lowercase
        name = name.toLowerCase();
    }

    /**
     * Creates a required dependency with any version.
     *
     * @param name The library name
     * @return A required dependency accepting any version
     */
    public static LibraryDependency required(String name) {
        return new LibraryDependency(name, VersionRange.ANY, true);
    }

    /**
     * Creates a required dependency with a version range.
     *
     * @param name  The library name
     * @param range The version range string
     * @return A required dependency
     * @throws IllegalArgumentException if range is invalid
     */
    public static LibraryDependency required(String name, String range) {
        return new LibraryDependency(name, VersionRange.parse(range), true);
    }

    /**
     * Creates a required dependency with a version range.
     *
     * @param name  The library name
     * @param range The version range
     * @return A required dependency
     */
    public static LibraryDependency required(String name, VersionRange range) {
        return new LibraryDependency(name, range, true);
    }

    /**
     * Creates an optional dependency with any version.
     *
     * @param name The library name
     * @return An optional dependency accepting any version
     */
    public static LibraryDependency optional(String name) {
        return new LibraryDependency(name, VersionRange.ANY, false);
    }

    /**
     * Creates an optional dependency with a version range.
     *
     * @param name  The library name
     * @param range The version range string
     * @return An optional dependency
     * @throws IllegalArgumentException if range is invalid
     */
    public static LibraryDependency optional(String name, String range) {
        return new LibraryDependency(name, VersionRange.parse(range), false);
    }

    /**
     * Creates an optional dependency with a version range.
     *
     * @param name  The library name
     * @param range The version range
     * @return An optional dependency
     */
    public static LibraryDependency optional(String name, VersionRange range) {
        return new LibraryDependency(name, range, false);
    }

    /**
     * Creates a dependency requiring an exact version.
     *
     * @param name     The library name
     * @param version  The exact version required
     * @param required Whether the dependency is required
     * @return A dependency for the exact version
     */
    public static LibraryDependency exact(String name, LibraryVersion version, boolean required) {
        return new LibraryDependency(name, VersionRange.exactly(version), required);
    }

    /**
     * Creates a dependency compatible with a version (same major).
     *
     * @param name     The library name
     * @param version  The version to be compatible with
     * @param required Whether the dependency is required
     * @return A dependency for compatible versions
     */
    public static LibraryDependency compatible(String name, LibraryVersion version, boolean required) {
        return new LibraryDependency(name, VersionRange.compatibleWith(version), required);
    }

    /**
     * Checks if a library satisfies this dependency.
     *
     * @param library The library to check
     * @return true if the library satisfies this dependency
     * @throws NullPointerException if library is null
     */
    public boolean isSatisfiedBy(Library library) {
        Objects.requireNonNull(library, "Library cannot be null");
        return library.name().equalsIgnoreCase(name) && versionRange.contains(library.version());
    }

    /**
     * Checks if a version satisfies this dependency.
     *
     * @param version The version to check
     * @return true if the version is within range
     * @throws NullPointerException if version is null
     */
    public boolean isSatisfiedBy(LibraryVersion version) {
        return versionRange.contains(version);
    }

    /**
     * Checks if this is an optional dependency.
     *
     * @return true if not required
     */
    public boolean isOptional() {
        return !required;
    }

    /**
     * Checks if this dependency accepts any version.
     *
     * @return true if version range is unbounded
     */
    public boolean acceptsAnyVersion() {
        return versionRange.isUnbounded();
    }

    /**
     * Checks if this dependency requires an exact version.
     *
     * @return true if version range is exact
     */
    public boolean requiresExactVersion() {
        return versionRange.isExact();
    }

    /**
     * Creates a copy with a different required flag.
     *
     * @param required Whether the dependency should be required
     * @return A new LibraryDependency with updated required flag
     */
    public LibraryDependency withRequired(boolean required) {
        return new LibraryDependency(name, versionRange, required);
    }

    /**
     * Creates a copy with a different version range.
     *
     * @param range The new version range
     * @return A new LibraryDependency with updated range
     */
    public LibraryDependency withVersionRange(VersionRange range) {
        return new LibraryDependency(name, range, required);
    }

    /**
     * Checks if this dependency can be satisfied by the currently loaded libraries.
     *
     * @return true if the library is available and version matches
     */
    public boolean isSatisfied() {
        return Libraries.findLibrary(name)
                .map(this::isSatisfiedBy)
                .orElse(false);
    }

    /**
     * Gets the minimum required version, if any.
     *
     * @return The minimum version, or null if unbounded
     */
    public LibraryVersion minimumVersion() {
        return versionRange.min();
    }

    /**
     * Gets the maximum allowed version, if any.
     *
     * @return The maximum version, or null if unbounded
     */
    public LibraryVersion maximumVersion() {
        return versionRange.max();
    }

    /**
     * Returns a string representation of this dependency.
     *
     * @return A string in the format "name:versionRange (required/optional)"
     */
    @Override
    public String toString() {
        return name + ":" + versionRange + (required ? " (required)" : " (optional)");
    }
}
