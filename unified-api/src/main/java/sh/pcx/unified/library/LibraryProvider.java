/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package sh.pcx.unified.library;

import java.util.Collection;
import java.util.Optional;

/**
 * Main interface for accessing shared libraries provided by the UnifiedPlugin API.
 *
 * <p>The LibraryProvider manages the lifecycle and access to shared libraries,
 * eliminating the need for plugins to shade common dependencies. Libraries are
 * loaded once by the framework and shared across all dependent plugins.
 *
 * <h2>Architecture</h2>
 * <pre>
 * +------------------+
 * | UnifiedPluginAPI | &lt;-- Loads and provides libraries
 * +------------------+
 *         |
 *         v
 * +------------------+     +------------------+
 * | LibraryProvider  |----&gt;| LibraryRegistry  |
 * +------------------+     +------------------+
 *         |
 *    +----+----+----+
 *    |    |    |    |
 *    v    v    v    v
 *  Guice  HikariCP  Adventure  ...
 * </pre>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Get the library provider from UnifiedAPI
 * LibraryProvider provider = UnifiedAPI.getLibraryProvider();
 *
 * // Check library availability
 * if (provider.isAvailable("hikaricp")) {
 *     System.out.println("HikariCP is available!");
 * }
 *
 * // Get library information
 * Optional<Library> library = provider.getLibrary("guice");
 * library.ifPresent(lib -> {
 *     System.out.println("Guice version: " + lib.version());
 * });
 *
 * // Get library version directly
 * String version = provider.getVersion("adventure"); // "4.26.1"
 *
 * // List all available libraries
 * provider.getAllLibraries().forEach(lib -> {
 *     System.out.println(lib.name() + " v" + lib.version());
 * });
 *
 * // Check library status
 * LibraryStatus status = provider.getStatus("mongodb");
 * switch (status) {
 *     case AVAILABLE -> System.out.println("Ready to use");
 *     case LOADING -> System.out.println("Still loading...");
 *     case FAILED -> System.out.println("Load failed");
 *     case NOT_FOUND -> System.out.println("Not provided");
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All implementations of this interface must be thread-safe. Library loading
 * may occur concurrently, and multiple plugins may query the provider simultaneously.
 *
 * <h2>Lazy Loading</h2>
 * <p>Libraries are loaded on-demand when first requested. Use {@link #preload(String)}
 * to eagerly load libraries during plugin startup if needed.
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Library
 * @see LibraryRegistry
 * @see Libraries
 */
public interface LibraryProvider {

    /**
     * Checks if a library is available and ready to use.
     *
     * <p>A library is considered available if it has been loaded successfully
     * and its classes can be accessed by plugins.
     *
     * @param name The library name (case-insensitive)
     * @return true if the library is available
     * @throws NullPointerException if name is null
     */
    boolean isAvailable(String name);

    /**
     * Gets the version string of a library.
     *
     * @param name The library name (case-insensitive)
     * @return The version string (e.g., "7.0.2"), or null if not available
     * @throws NullPointerException if name is null
     */
    String getVersion(String name);

    /**
     * Gets the version of a library as a LibraryVersion object.
     *
     * @param name The library name (case-insensitive)
     * @return An Optional containing the version, or empty if not available
     * @throws NullPointerException if name is null
     */
    Optional<LibraryVersion> getLibraryVersion(String name);

    /**
     * Gets information about a library.
     *
     * @param name The library name (case-insensitive)
     * @return An Optional containing the library, or empty if not available
     * @throws NullPointerException if name is null
     */
    Optional<Library> getLibrary(String name);

    /**
     * Gets information about a library, throwing if not found.
     *
     * @param name The library name (case-insensitive)
     * @return The library information
     * @throws LibraryNotFoundException if the library is not available
     * @throws NullPointerException     if name is null
     */
    Library requireLibrary(String name) throws LibraryNotFoundException;

    /**
     * Gets all available libraries.
     *
     * @return An unmodifiable collection of all available libraries
     */
    Collection<Library> getAllLibraries();

    /**
     * Gets the names of all available libraries.
     *
     * @return An unmodifiable collection of library names
     */
    Collection<String> getAvailableLibraryNames();

    /**
     * Gets the current status of a library.
     *
     * @param name The library name (case-insensitive)
     * @return The library status
     * @throws NullPointerException if name is null
     */
    LibraryStatus getStatus(String name);

    /**
     * Preloads a library, ensuring it is available for immediate use.
     *
     * <p>This is useful during plugin startup to fail-fast if a required
     * library is not available, rather than failing later during use.
     *
     * @param name The library name (case-insensitive)
     * @return true if the library was loaded successfully
     * @throws NullPointerException if name is null
     */
    boolean preload(String name);

    /**
     * Preloads all specified libraries.
     *
     * @param names The library names to preload
     * @return true if all libraries were loaded successfully
     * @throws NullPointerException if names is null or contains null elements
     */
    boolean preloadAll(Collection<String> names);

    /**
     * Gets the classloader used for loading library classes.
     *
     * <p>Plugins typically do not need to access the classloader directly,
     * as library classes are automatically available through the plugin's
     * classloader hierarchy.
     *
     * @param name The library name (case-insensitive)
     * @return An Optional containing the classloader, or empty if not available
     * @throws NullPointerException if name is null
     */
    Optional<ClassLoader> getClassLoader(String name);

    /**
     * Gets the library registry.
     *
     * <p>The registry contains metadata about all known libraries,
     * including those not yet loaded.
     *
     * @return The library registry
     */
    LibraryRegistry getRegistry();

    /**
     * Checks if a library version satisfies the given range.
     *
     * @param name  The library name (case-insensitive)
     * @param range The version range to check
     * @return true if the library is available and its version is in range
     * @throws NullPointerException if name or range is null
     */
    boolean satisfiesVersion(String name, VersionRange range);

    /**
     * Adds a listener to be notified of library load events.
     *
     * @param listener The listener to add
     * @throws NullPointerException if listener is null
     */
    void addLoadListener(LibraryLoadListener listener);

    /**
     * Removes a previously added load listener.
     *
     * @param listener The listener to remove
     * @throws NullPointerException if listener is null
     */
    void removeLoadListener(LibraryLoadListener listener);

    /**
     * Functional interface for library load event listeners.
     */
    @FunctionalInterface
    interface LibraryLoadListener {
        /**
         * Called when a library load event occurs.
         *
         * @param event The load event
         */
        void onLibraryLoad(LibraryLoadEvent event);
    }
}
