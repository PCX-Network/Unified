/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package sh.pcx.unified.library;

import java.util.Objects;

/**
 * Represents a shared library available through the UnifiedPlugin API framework.
 *
 * <p>Libraries are external dependencies provided by the framework that plugins
 * can use without shading. Each library has a unique name, version, and main
 * entry class.
 *
 * <h2>Provided Libraries</h2>
 * The UnifiedPlugin API provides these shared libraries:
 * <ul>
 *   <li><b>guice</b> - Google Guice 7.0.0 for dependency injection</li>
 *   <li><b>configurate</b> - Configurate 4.2.0 for configuration management</li>
 *   <li><b>adventure</b> - Adventure 4.26.1 for text components</li>
 *   <li><b>hikaricp</b> - HikariCP 7.0.2 for database connection pooling</li>
 *   <li><b>caffeine</b> - Caffeine 3.2.3 for high-performance caching</li>
 *   <li><b>jedis</b> - Jedis 7.2.0 for synchronous Redis operations</li>
 *   <li><b>lettuce</b> - Lettuce 7.2.0.RELEASE for async Redis operations</li>
 *   <li><b>mongodb</b> - MongoDB Driver 5.5.0 for MongoDB client</li>
 *   <li><b>gson</b> - Gson 2.13.2 for JSON serialization</li>
 *   <li><b>slf4j</b> - SLF4J 2.0.17 for logging abstraction</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Get library information
 * Library hikari = Libraries.getLibrary("hikaricp");
 * System.out.println("HikariCP version: " + hikari.version());
 * System.out.println("Main class: " + hikari.mainClass());
 *
 * // Create library definition programmatically
 * Library custom = new Library(
 *     "custom-lib",
 *     LibraryVersion.parse("1.0.0"),
 *     "com.example.CustomLibrary"
 * );
 *
 * // Check version compatibility
 * Library guice = Libraries.getLibrary("guice");
 * VersionRange required = VersionRange.parse("[7.0.0,8.0.0)");
 * if (required.contains(guice.version())) {
 *     System.out.println("Guice version is compatible");
 * }
 * }</pre>
 *
 * <h2>Library Names</h2>
 * <p>Library names are case-insensitive and normalized to lowercase.
 * The following are all equivalent:
 * <ul>
 *   <li>{@code "HikariCP"}</li>
 *   <li>{@code "hikaricp"}</li>
 *   <li>{@code "HIKARICP"}</li>
 * </ul>
 *
 * @param name      The unique name of the library (case-insensitive)
 * @param version   The semantic version of the library
 * @param mainClass The fully qualified name of the library's main class
 * @author Supatuck
 * @since 1.0.0
 * @see Libraries
 * @see LibraryProvider
 * @see LibraryVersion
 */
public record Library(String name, LibraryVersion version, String mainClass) {

    /**
     * Creates a new Library with validation.
     *
     * @param name      The unique name of the library
     * @param version   The semantic version of the library
     * @param mainClass The fully qualified name of the library's main class
     * @throws NullPointerException     if any parameter is null
     * @throws IllegalArgumentException if name or mainClass is empty
     */
    public Library {
        Objects.requireNonNull(name, "Library name cannot be null");
        Objects.requireNonNull(version, "Library version cannot be null");
        Objects.requireNonNull(mainClass, "Main class cannot be null");

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Library name cannot be empty");
        }
        if (mainClass.isEmpty()) {
            throw new IllegalArgumentException("Main class cannot be empty");
        }

        // Normalize name to lowercase
        name = name.toLowerCase();
    }

    /**
     * Creates a new Library by parsing a version string.
     *
     * @param name      The unique name of the library
     * @param version   The version string to parse
     * @param mainClass The fully qualified name of the library's main class
     * @return A new Library instance
     * @throws IllegalArgumentException if the version string is invalid
     */
    public static Library of(String name, String version, String mainClass) {
        return new Library(name, LibraryVersion.parse(version), mainClass);
    }

    /**
     * Checks if this library satisfies the given version range.
     *
     * @param range The version range to check against
     * @return true if this library's version is within the range
     * @throws NullPointerException if range is null
     */
    public boolean satisfies(VersionRange range) {
        Objects.requireNonNull(range, "Version range cannot be null");
        return range.contains(version);
    }

    /**
     * Checks if this library is compatible with another version.
     *
     * <p>Libraries are considered compatible if they share the same major version.
     *
     * @param other The version to check compatibility with
     * @return true if the versions are compatible
     */
    public boolean isCompatibleWith(LibraryVersion other) {
        return version.isCompatibleWith(other);
    }

    /**
     * Gets the major version number.
     *
     * @return The major version component
     */
    public int majorVersion() {
        return version.major();
    }

    /**
     * Gets the minor version number.
     *
     * @return The minor version component
     */
    public int minorVersion() {
        return version.minor();
    }

    /**
     * Gets the patch version number.
     *
     * @return The patch version component
     */
    public int patchVersion() {
        return version.patch();
    }

    /**
     * Gets the version as a string.
     *
     * @return The version string (e.g., "7.0.2")
     */
    public String versionString() {
        return version.toString();
    }

    /**
     * Checks if this library matches the given name (case-insensitive).
     *
     * @param libraryName The name to match
     * @return true if the names match
     */
    public boolean matches(String libraryName) {
        return name.equalsIgnoreCase(libraryName);
    }

    /**
     * Returns a string representation of this library.
     *
     * @return A string in the format "name:version"
     */
    @Override
    public String toString() {
        return name + ":" + version;
    }

    /**
     * Creates a LibraryDependency for this library with the given version range.
     *
     * @param range    The acceptable version range
     * @param required Whether the dependency is required
     * @return A new LibraryDependency
     */
    public LibraryDependency asDependency(VersionRange range, boolean required) {
        return new LibraryDependency(name, range, required);
    }

    /**
     * Creates a required LibraryDependency for this exact library version.
     *
     * @return A new required LibraryDependency for this exact version
     */
    public LibraryDependency asRequiredDependency() {
        return new LibraryDependency(name, VersionRange.exactly(version), true);
    }

    /**
     * Creates an optional LibraryDependency compatible with this library's major version.
     *
     * @return A new optional LibraryDependency
     */
    public LibraryDependency asOptionalDependency() {
        return new LibraryDependency(name, VersionRange.compatibleWith(version), false);
    }
}
