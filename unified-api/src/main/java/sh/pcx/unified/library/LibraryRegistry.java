/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package sh.pcx.unified.library;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

/**
 * Registry of all known libraries, including those not yet loaded.
 *
 * <p>The LibraryRegistry maintains metadata about available libraries and
 * provides discovery capabilities. Unlike {@link LibraryProvider}, which
 * only exposes loaded libraries, the registry knows about all libraries
 * that can potentially be loaded.
 *
 * <h2>Registry vs Provider</h2>
 * <table border="1">
 *   <tr><th>LibraryRegistry</th><th>LibraryProvider</th></tr>
 *   <tr><td>Knows about all possible libraries</td><td>Only loaded libraries</td></tr>
 *   <tr><td>Provides metadata and paths</td><td>Provides runtime access</td></tr>
 *   <tr><td>Can register new libraries</td><td>Read-only access</td></tr>
 *   <tr><td>Used during initialization</td><td>Used during runtime</td></tr>
 * </table>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * LibraryRegistry registry = provider.getRegistry();
 *
 * // Check if a library is registered
 * if (registry.isRegistered("hikaricp")) {
 *     System.out.println("HikariCP is available");
 * }
 *
 * // Get library metadata
 * Optional<LibraryMetadata> metadata = registry.getMetadata("guice");
 * metadata.ifPresent(m -> {
 *     System.out.println("Main class: " + m.mainClass());
 *     System.out.println("JAR path: " + m.jarPath());
 * });
 *
 * // List all registered libraries
 * registry.getAllRegistered().forEach(lib -> {
 *     System.out.println(lib.name() + " registered");
 * });
 *
 * // Register a custom library (for extensions)
 * registry.register(new Library("custom", version, "com.example.Main"));
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations must be thread-safe as registration may occur from
 * multiple threads during server startup.
 *
 * @author Supatuck
 * @since 1.0.0
 * @see LibraryProvider
 * @see Library
 */
public interface LibraryRegistry {

    /**
     * Checks if a library is registered in this registry.
     *
     * <p>A library being registered does not mean it is loaded or available.
     * Use {@link LibraryProvider#isAvailable(String)} to check if a library
     * can be used.
     *
     * @param name The library name (case-insensitive)
     * @return true if the library is registered
     * @throws NullPointerException if name is null
     */
    boolean isRegistered(String name);

    /**
     * Gets a registered library by name.
     *
     * @param name The library name (case-insensitive)
     * @return An Optional containing the library, or empty if not registered
     * @throws NullPointerException if name is null
     */
    Optional<Library> get(String name);

    /**
     * Gets all registered libraries.
     *
     * @return An unmodifiable collection of all registered libraries
     */
    Collection<Library> getAllRegistered();

    /**
     * Gets the names of all registered libraries.
     *
     * @return An unmodifiable collection of library names
     */
    Collection<String> getRegisteredNames();

    /**
     * Gets the JAR file path for a library.
     *
     * @param name The library name (case-insensitive)
     * @return An Optional containing the path, or empty if not found
     * @throws NullPointerException if name is null
     */
    Optional<Path> getJarPath(String name);

    /**
     * Gets all JAR files for a library including dependencies.
     *
     * <p>Some libraries require multiple JAR files. This method returns
     * all JAR files that need to be loaded for the library to function.
     *
     * @param name The library name (case-insensitive)
     * @return A collection of JAR paths (may be empty if not found)
     * @throws NullPointerException if name is null
     */
    Collection<Path> getAllJarPaths(String name);

    /**
     * Registers a new library.
     *
     * <p>This is typically used during framework initialization or when
     * adding custom libraries at runtime.
     *
     * @param library The library to register
     * @throws NullPointerException     if library is null
     * @throws IllegalStateException    if a library with the same name is already registered
     * @throws IllegalArgumentException if the library is invalid
     */
    void register(Library library);

    /**
     * Registers a library with its JAR path.
     *
     * @param library The library to register
     * @param jarPath The path to the library JAR file
     * @throws NullPointerException     if library or jarPath is null
     * @throws IllegalStateException    if a library with the same name is already registered
     * @throws IllegalArgumentException if the JAR path does not exist
     */
    void register(Library library, Path jarPath);

    /**
     * Unregisters a library.
     *
     * <p>This removes the library from the registry. If the library is
     * currently loaded, it will remain accessible until the server restarts.
     *
     * @param name The library name (case-insensitive)
     * @return true if the library was unregistered, false if not found
     * @throws NullPointerException if name is null
     */
    boolean unregister(String name);

    /**
     * Gets the number of registered libraries.
     *
     * @return The count of registered libraries
     */
    int size();

    /**
     * Checks if the registry is empty.
     *
     * @return true if no libraries are registered
     */
    boolean isEmpty();

    /**
     * Clears all registered libraries.
     *
     * <p><b>Warning:</b> This is a destructive operation typically only
     * used during testing or complete framework reinitialization.
     */
    void clear();

    /**
     * Gets libraries matching a version range.
     *
     * @param name  The library name (case-insensitive)
     * @param range The version range to match
     * @return An Optional containing the library if it matches the range
     * @throws NullPointerException if name or range is null
     */
    Optional<Library> getMatching(String name, VersionRange range);

    /**
     * Finds libraries by a name pattern.
     *
     * <p>The pattern supports wildcards:
     * <ul>
     *   <li>{@code *} matches any sequence of characters</li>
     *   <li>{@code ?} matches any single character</li>
     * </ul>
     *
     * @param pattern The name pattern to match
     * @return A collection of matching libraries
     * @throws NullPointerException if pattern is null
     */
    Collection<Library> findByPattern(String pattern);

    /**
     * Gets the library directory path.
     *
     * @return The path to the directory containing library JAR files
     */
    Path getLibraryDirectory();

    /**
     * Refreshes the registry by scanning the library directory.
     *
     * <p>This rescans the library directory for new or updated JAR files
     * and updates the registry accordingly.
     */
    void refresh();
}
