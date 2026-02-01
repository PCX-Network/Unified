/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package sh.pcx.unified.library;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Static utility class for accessing shared libraries provided by the UnifiedPlugin API.
 *
 * <p>The Libraries class provides a convenient static API for checking library
 * availability and accessing library information. It serves as the primary
 * entry point for plugins to interact with the shared library system.
 *
 * <h2>Provided Libraries</h2>
 * <p>The UnifiedPlugin API provides the following shared libraries:
 *
 * <table border="1">
 *   <caption>Shared Libraries</caption>
 *   <tr><th>Library</th><th>Version</th><th>Main Class</th><th>Purpose</th></tr>
 *   <tr><td>guice</td><td>7.0.0</td><td>com.google.inject.Guice</td><td>Dependency injection framework</td></tr>
 *   <tr><td>configurate</td><td>4.2.0</td><td>org.spongepowered.configurate.ConfigurationNode</td><td>Configuration management</td></tr>
 *   <tr><td>adventure</td><td>4.26.1</td><td>net.kyori.adventure.text.Component</td><td>Text component API</td></tr>
 *   <tr><td>hikaricp</td><td>7.0.2</td><td>com.zaxxer.hikari.HikariDataSource</td><td>High-performance JDBC connection pooling</td></tr>
 *   <tr><td>caffeine</td><td>3.2.3</td><td>com.github.benmanes.caffeine.cache.Caffeine</td><td>High-performance caching library</td></tr>
 *   <tr><td>jedis</td><td>7.2.0</td><td>redis.clients.jedis.Jedis</td><td>Synchronous Redis client</td></tr>
 *   <tr><td>lettuce</td><td>7.2.0.RELEASE</td><td>io.lettuce.core.RedisClient</td><td>Asynchronous Redis client</td></tr>
 *   <tr><td>mongodb</td><td>5.5.0</td><td>com.mongodb.client.MongoClients</td><td>MongoDB Java driver</td></tr>
 *   <tr><td>gson</td><td>2.13.2</td><td>com.google.gson.Gson</td><td>JSON serialization/deserialization</td></tr>
 *   <tr><td>slf4j</td><td>2.0.17</td><td>org.slf4j.LoggerFactory</td><td>Simple Logging Facade for Java</td></tr>
 * </table>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Check if a library is available
 * if (Libraries.isAvailable("hikaricp")) {
 *     // Safe to use HikariCP classes
 *     HikariDataSource ds = new HikariDataSource();
 * }
 *
 * // Get library version
 * String version = Libraries.getVersion("adventure"); // "4.26.1"
 *
 * // Get full library information
 * Library guice = Libraries.getLibrary("guice");
 * System.out.println("Guice " + guice.version() + " loaded");
 *
 * // List all available libraries
 * for (Library lib : Libraries.getAllLibraries()) {
 *     System.out.println(lib.name() + " v" + lib.version());
 * }
 *
 * // Check library status
 * LibraryStatus status = Libraries.getStatus("mongodb");
 * if (status.isAvailable()) {
 *     MongoClients.create("mongodb://localhost");
 * }
 *
 * // Require a library (throws if not available)
 * Library hikari = Libraries.require("hikaricp");
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods in this class are thread-safe. Library information is cached
 * and protected by read-write locks for optimal concurrent access.
 *
 * <h2>Initialization</h2>
 * <p>The Libraries class is initialized automatically when the UnifiedPlugin API
 * loads. Plugins depending on UnifiedPluginAPI can safely use this class
 * immediately in their onEnable methods.
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Library
 * @see LibraryProvider
 * @see LibraryStatus
 */
public final class Libraries {

    /**
     * Lock for thread-safe provider access.
     */
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * The library provider instance.
     */
    private static volatile LibraryProvider provider;

    /**
     * Cache of library information for fast access.
     */
    private static final Map<String, Library> libraryCache = new ConcurrentHashMap<>();

    /**
     * Cache of library statuses.
     */
    private static final Map<String, LibraryStatus> statusCache = new ConcurrentHashMap<>();

    /**
     * Whether the library system has been initialized.
     */
    private static volatile boolean initialized = false;

    /**
     * Private constructor to prevent instantiation.
     */
    private Libraries() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    /**
     * Initializes the library system with a provider.
     *
     * <p>This method is called by the UnifiedPlugin API during startup.
     * Plugins should not call this method directly.
     *
     * @param libraryProvider The library provider to use
     * @throws NullPointerException  if libraryProvider is null
     * @throws IllegalStateException if already initialized
     */
    public static void initialize(LibraryProvider libraryProvider) {
        Objects.requireNonNull(libraryProvider, "Library provider cannot be null");

        lock.writeLock().lock();
        try {
            if (initialized) {
                throw new IllegalStateException("Libraries already initialized");
            }
            provider = libraryProvider;
            initialized = true;

            // Pre-populate caches
            for (Library lib : provider.getAllLibraries()) {
                libraryCache.put(lib.name().toLowerCase(), lib);
                statusCache.put(lib.name().toLowerCase(), LibraryStatus.AVAILABLE);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Checks if the library system has been initialized.
     *
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Checks if a library is available and ready to use.
     *
     * <p>This is the recommended way to check library availability before
     * using library classes.
     *
     * <pre>{@code
     * if (Libraries.isAvailable("hikaricp")) {
     *     // Safe to use HikariCP
     *     HikariDataSource ds = new HikariDataSource();
     * }
     * }</pre>
     *
     * @param name The library name (case-insensitive)
     * @return true if the library is available
     * @throws NullPointerException if name is null
     */
    public static boolean isAvailable(String name) {
        Objects.requireNonNull(name, "Library name cannot be null");

        String normalizedName = name.toLowerCase();

        // Check cache first
        LibraryStatus cached = statusCache.get(normalizedName);
        if (cached != null) {
            return cached.isAvailable();
        }

        // Query provider
        lock.readLock().lock();
        try {
            if (provider == null) {
                return false;
            }
            boolean available = provider.isAvailable(name);
            statusCache.put(normalizedName, available ? LibraryStatus.AVAILABLE : LibraryStatus.NOT_FOUND);
            return available;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the version string of a library.
     *
     * <pre>{@code
     * String version = Libraries.getVersion("guice"); // "7.0.0"
     * }</pre>
     *
     * @param name The library name (case-insensitive)
     * @return The version string, or null if the library is not available
     * @throws NullPointerException if name is null
     */
    public static String getVersion(String name) {
        Objects.requireNonNull(name, "Library name cannot be null");

        String normalizedName = name.toLowerCase();

        // Check cache first
        Library cached = libraryCache.get(normalizedName);
        if (cached != null) {
            return cached.version().toString();
        }

        lock.readLock().lock();
        try {
            if (provider == null) {
                return null;
            }
            return provider.getVersion(name);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets full information about a library.
     *
     * <pre>{@code
     * Library hikari = Libraries.getLibrary("hikaricp");
     * System.out.println("Version: " + hikari.version());
     * System.out.println("Main class: " + hikari.mainClass());
     * }</pre>
     *
     * @param name The library name (case-insensitive)
     * @return The library information
     * @throws LibraryNotFoundException if the library is not available
     * @throws NullPointerException     if name is null
     */
    public static Library getLibrary(String name) {
        Objects.requireNonNull(name, "Library name cannot be null");

        String normalizedName = name.toLowerCase();

        // Check cache first
        Library cached = libraryCache.get(normalizedName);
        if (cached != null) {
            return cached;
        }

        lock.readLock().lock();
        try {
            if (provider == null) {
                throw new LibraryNotFoundException(name, "Library system not initialized");
            }
            return provider.requireLibrary(name);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets library information as an Optional.
     *
     * <pre>{@code
     * Libraries.findLibrary("hikaricp").ifPresent(lib -> {
     *     System.out.println("Found: " + lib);
     * });
     * }</pre>
     *
     * @param name The library name (case-insensitive)
     * @return An Optional containing the library, or empty if not available
     * @throws NullPointerException if name is null
     */
    public static Optional<Library> findLibrary(String name) {
        Objects.requireNonNull(name, "Library name cannot be null");

        String normalizedName = name.toLowerCase();

        // Check cache first
        Library cached = libraryCache.get(normalizedName);
        if (cached != null) {
            return Optional.of(cached);
        }

        lock.readLock().lock();
        try {
            if (provider == null) {
                return Optional.empty();
            }
            return provider.getLibrary(name);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets all available libraries.
     *
     * <pre>{@code
     * for (Library lib : Libraries.getAllLibraries()) {
     *     System.out.println(lib.name() + " v" + lib.version());
     * }
     * }</pre>
     *
     * @return An unmodifiable collection of all available libraries
     */
    public static Collection<Library> getAllLibraries() {
        lock.readLock().lock();
        try {
            if (provider == null) {
                return Collections.emptyList();
            }
            return provider.getAllLibraries();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the names of all available libraries.
     *
     * @return An unmodifiable collection of library names
     */
    public static Collection<String> getAvailableNames() {
        lock.readLock().lock();
        try {
            if (provider == null) {
                return Collections.emptyList();
            }
            return provider.getAvailableLibraryNames();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the current status of a library.
     *
     * <pre>{@code
     * LibraryStatus status = Libraries.getStatus("mongodb");
     * switch (status) {
     *     case AVAILABLE -> useMongoDb();
     *     case FAILED -> logger.error("MongoDB failed to load");
     *     case NOT_FOUND -> logger.warn("MongoDB not available");
     * }
     * }</pre>
     *
     * @param name The library name (case-insensitive)
     * @return The library status
     * @throws NullPointerException if name is null
     */
    public static LibraryStatus getStatus(String name) {
        Objects.requireNonNull(name, "Library name cannot be null");

        String normalizedName = name.toLowerCase();

        // Check cache first
        LibraryStatus cached = statusCache.get(normalizedName);
        if (cached != null) {
            return cached;
        }

        lock.readLock().lock();
        try {
            if (provider == null) {
                return LibraryStatus.NOT_FOUND;
            }
            LibraryStatus status = provider.getStatus(name);
            statusCache.put(normalizedName, status);
            return status;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Requires a library to be available, throwing if not found.
     *
     * <p>Use this method when a library is required for your plugin to function.
     * If the library is not available, a descriptive exception is thrown.
     *
     * <pre>{@code
     * // In plugin onEnable
     * Library hikari = Libraries.require("hikaricp");
     * Library guice = Libraries.require("guice");
     * // If we get here, both libraries are available
     * }</pre>
     *
     * @param name The library name (case-insensitive)
     * @return The library information
     * @throws LibraryNotFoundException if the library is not available
     * @throws NullPointerException     if name is null
     */
    public static Library require(String name) {
        return getLibrary(name);
    }

    /**
     * Preloads a library, ensuring it is immediately available.
     *
     * <p>This triggers eager loading of the library if it has not already
     * been loaded. Use this during plugin startup to fail-fast if a
     * required library is missing.
     *
     * @param name The library name (case-insensitive)
     * @return true if the library is available after preloading
     * @throws NullPointerException if name is null
     */
    public static boolean preload(String name) {
        Objects.requireNonNull(name, "Library name cannot be null");

        lock.readLock().lock();
        try {
            if (provider == null) {
                return false;
            }
            return provider.preload(name);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Checks if a library version satisfies a version range.
     *
     * <pre>{@code
     * VersionRange range = VersionRange.parse("[7.0.0,8.0.0)");
     * if (Libraries.satisfies("guice", range)) {
     *     // Guice version is compatible
     * }
     * }</pre>
     *
     * @param name  The library name (case-insensitive)
     * @param range The version range to check
     * @return true if the library is available and its version satisfies the range
     * @throws NullPointerException if name or range is null
     */
    public static boolean satisfies(String name, VersionRange range) {
        Objects.requireNonNull(name, "Library name cannot be null");
        Objects.requireNonNull(range, "Version range cannot be null");

        lock.readLock().lock();
        try {
            if (provider == null) {
                return false;
            }
            return provider.satisfiesVersion(name, range);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the library provider.
     *
     * <p>Most plugins should use the static methods on this class instead
     * of accessing the provider directly.
     *
     * @return The library provider
     * @throws IllegalStateException if the library system is not initialized
     */
    public static LibraryProvider getProvider() {
        lock.readLock().lock();
        try {
            if (provider == null) {
                throw new IllegalStateException("Library system not initialized");
            }
            return provider;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Clears the library cache.
     *
     * <p>This is primarily used for testing. In normal operation, the cache
     * is managed automatically.
     */
    public static void clearCache() {
        libraryCache.clear();
        statusCache.clear();
    }

    /**
     * Resets the library system.
     *
     * <p><b>Warning:</b> This is a destructive operation primarily for testing.
     * It should not be called during normal server operation.
     */
    public static void reset() {
        lock.writeLock().lock();
        try {
            provider = null;
            initialized = false;
            libraryCache.clear();
            statusCache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
