/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package sh.pcx.unified.library;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Objects;

/**
 * URLClassLoader subclass for loading shared library classes.
 *
 * <p>LibraryClassLoader extends URLClassLoader to provide specialized class
 * loading behavior for shared libraries. It supports both parent-first (shared)
 * and child-first (isolated) loading modes.
 *
 * <h2>Loading Modes</h2>
 * <p><b>Parent-first (default):</b> Standard delegation model where the parent
 * classloader is consulted first. This allows libraries to share common
 * dependencies and prevents duplicate class loading.
 *
 * <p><b>Child-first (isolated):</b> Reverses the delegation model, loading
 * classes from this classloader's URLs first. Use this when a plugin needs
 * a different version of a library than the shared one.
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Create a shared library classloader
 * ClassLoaderConfig config = ClassLoaderConfig.forSharedLibrary(
 *     "hikaricp",
 *     Path.of("lib/HikariCP-7.0.2.jar"),
 *     getClass().getClassLoader()
 * );
 * LibraryClassLoader loader = new LibraryClassLoader(config);
 *
 * // Load a class from the library
 * Class<?> hikariClass = loader.loadClass("com.zaxxer.hikari.HikariDataSource");
 *
 * // Get library info
 * System.out.println("Library: " + loader.getLibraryName());
 * System.out.println("Isolated: " + loader.isIsolated());
 *
 * // Proper cleanup
 * try {
 *     loader.close();
 * } catch (IOException e) {
 *     logger.warn("Failed to close classloader", e);
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Class loading is thread-safe. The classloader uses synchronization
 * to prevent race conditions during class definition.
 *
 * <h2>Resource Loading</h2>
 * <p>Resources are also loaded using the same delegation model as classes.
 * Use {@link #getResource(String)} and {@link #getResourceAsStream(String)}
 * to load resources from library JARs.
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ClassLoaderConfig
 * @see IsolatedClassLoader
 */
public class LibraryClassLoader extends URLClassLoader {

    /**
     * The configuration for this classloader.
     */
    private final ClassLoaderConfig config;

    /**
     * Whether this classloader has been closed.
     */
    private volatile boolean closed = false;

    /**
     * Creates a new LibraryClassLoader with the given configuration.
     *
     * @param config The classloader configuration
     * @throws NullPointerException     if config is null
     * @throws IllegalArgumentException if JAR URLs cannot be created
     */
    public LibraryClassLoader(ClassLoaderConfig config) {
        super(toUrls(config.jarPaths()), config.parent());
        this.config = Objects.requireNonNull(config, "Config cannot be null");
    }

    /**
     * Converts paths to URLs.
     */
    private static URL[] toUrls(Iterable<Path> paths) {
        java.util.List<URL> urls = new java.util.ArrayList<>();
        for (Path path : paths) {
            try {
                urls.add(path.toUri().toURL());
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Invalid JAR path: " + path, e);
            }
        }
        return urls.toArray(new URL[0]);
    }

    /**
     * Loads a class, respecting the isolation mode.
     *
     * <p>If isolated mode is enabled, classes are loaded from this classloader's
     * URLs first, falling back to the parent only if not found or excluded.
     *
     * @param name    The binary name of the class
     * @param resolve If true, resolve the class
     * @return The loaded class
     * @throws ClassNotFoundException if the class is not found
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (closed) {
            throw new ClassNotFoundException("ClassLoader is closed: " + name);
        }

        synchronized (getClassLoadingLock(name)) {
            // Check if already loaded
            Class<?> loadedClass = findLoadedClass(name);
            if (loadedClass != null) {
                return loadedClass;
            }

            // Excluded packages always delegate to parent
            if (config.isExcluded(name)) {
                return super.loadClass(name, resolve);
            }

            // Child-first loading for isolated mode
            if (config.isolated()) {
                try {
                    loadedClass = findClass(name);
                } catch (ClassNotFoundException e) {
                    // Fall back to parent
                    loadedClass = super.loadClass(name, resolve);
                }
            } else {
                // Parent-first (default)
                loadedClass = super.loadClass(name, resolve);
            }

            if (resolve) {
                resolveClass(loadedClass);
            }

            return loadedClass;
        }
    }

    /**
     * Gets a resource, respecting the isolation mode.
     *
     * @param name The resource name
     * @return The URL of the resource, or null if not found
     */
    @Override
    public URL getResource(String name) {
        if (closed) {
            return null;
        }

        if (config.isolated()) {
            // Child-first for isolated mode
            URL url = findResource(name);
            if (url != null) {
                return url;
            }
            return super.getResource(name);
        } else {
            // Parent-first (default)
            return super.getResource(name);
        }
    }

    /**
     * Gets resources, respecting the isolation mode.
     *
     * @param name The resource name
     * @return An enumeration of URLs
     * @throws IOException if an I/O error occurs
     */
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (closed) {
            return java.util.Collections.emptyEnumeration();
        }

        if (config.isolated()) {
            // Combine child and parent resources, child first
            Enumeration<URL> childResources = findResources(name);
            Enumeration<URL> parentResources = getParent() != null ?
                    getParent().getResources(name) : java.util.Collections.emptyEnumeration();
            return new CombinedEnumeration(childResources, parentResources);
        } else {
            return super.getResources(name);
        }
    }

    /**
     * Gets a resource as a stream.
     *
     * @param name The resource name
     * @return An input stream, or null if not found
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        URL url = getResource(name);
        if (url == null) {
            return null;
        }
        try {
            return url.openStream();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Gets the library name for this classloader.
     *
     * @return The library name
     */
    public String getLibraryName() {
        return config.libraryName();
    }

    /**
     * Checks if this classloader uses isolated (child-first) loading.
     *
     * @return true if isolated mode is enabled
     */
    public boolean isIsolated() {
        return config.isolated();
    }

    /**
     * Gets the configuration for this classloader.
     *
     * @return The classloader configuration
     */
    public ClassLoaderConfig getConfig() {
        return config;
    }

    /**
     * Checks if this classloader has been closed.
     *
     * @return true if closed
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Closes this classloader and releases resources.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        closed = true;
        super.close();
    }

    /**
     * Returns a string representation of this classloader.
     *
     * @return A descriptive string
     */
    @Override
    public String toString() {
        return "LibraryClassLoader{" +
                "library='" + config.libraryName() + '\'' +
                ", isolated=" + config.isolated() +
                ", closed=" + closed +
                '}';
    }

    /**
     * Combines two enumerations.
     */
    private static class CombinedEnumeration implements Enumeration<URL> {
        private final Enumeration<URL> first;
        private final Enumeration<URL> second;

        CombinedEnumeration(Enumeration<URL> first, Enumeration<URL> second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean hasMoreElements() {
            return first.hasMoreElements() || second.hasMoreElements();
        }

        @Override
        public URL nextElement() {
            if (first.hasMoreElements()) {
                return first.nextElement();
            }
            return second.nextElement();
        }
    }
}
