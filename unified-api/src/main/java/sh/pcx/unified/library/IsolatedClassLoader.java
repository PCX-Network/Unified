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
import java.security.CodeSource;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Isolated classloader for loading conflicting dependencies separately.
 *
 * <p>IsolatedClassLoader provides complete isolation from the parent classloader,
 * allowing plugins to use different versions of libraries than what the framework
 * provides. This is useful when a plugin requires a specific library version that
 * conflicts with the shared version.
 *
 * <h2>When to Use Isolation</h2>
 * <ul>
 *   <li>Plugin requires a newer version of a library than provided</li>
 *   <li>Plugin uses a library with incompatible changes in newer versions</li>
 *   <li>Testing with different library versions</li>
 *   <li>Avoiding classpath conflicts with other plugins</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Create an isolated classloader for a specific library version
 * IsolatedClassLoader loader = IsolatedClassLoader.create(
 *     "my-plugin-gson",
 *     Set.of(Path.of("plugins/MyPlugin/lib/gson-2.14.jar")),
 *     getClass().getClassLoader()
 * );
 *
 * // Load classes from the isolated version
 * Class<?> gsonClass = loader.loadClass("com.google.gson.Gson");
 * Object gson = gsonClass.getConstructor().newInstance();
 *
 * // Create with custom excluded packages
 * IsolatedClassLoader customLoader = IsolatedClassLoader.builder()
 *     .name("custom-isolation")
 *     .jarPaths(Set.of(jarPath))
 *     .parent(getClass().getClassLoader())
 *     .excludePackage("org.bukkit.")
 *     .excludePackage("sh.pcx.")
 *     .build();
 *
 * // Get information about loaded classes
 * Map<String, CodeSource> sources = loader.getLoadedClassSources();
 * sources.forEach((name, source) -> {
 *     System.out.println(name + " from " + source.getLocation());
 * });
 *
 * // Cleanup when done
 * loader.close();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Class loading is synchronized per class name to prevent race conditions.
 * The loader tracks all loaded classes for debugging and resource management.
 *
 * <h2>Memory Considerations</h2>
 * <p>Isolated classloaders maintain references to all loaded classes.
 * Always call {@link #close()} when the loader is no longer needed to
 * allow garbage collection of loaded classes.
 *
 * @author Supatuck
 * @since 1.0.0
 * @see LibraryClassLoader
 * @see ClassLoaderConfig
 */
public class IsolatedClassLoader extends URLClassLoader {

    /**
     * Default packages that are always loaded from parent.
     */
    private static final Set<String> SYSTEM_PACKAGES = Set.of(
            "java.",
            "javax.",
            "sun.",
            "com.sun.",
            "jdk."
    );

    /**
     * Name of this isolated classloader for debugging.
     */
    private final String name;

    /**
     * Packages to always delegate to parent.
     */
    private final Set<String> excludedPackages;

    /**
     * Track loaded classes for debugging.
     */
    private final Map<String, CodeSource> loadedClasses = new ConcurrentHashMap<>();

    /**
     * Whether this classloader has been closed.
     */
    private volatile boolean closed = false;

    /**
     * Creates a new IsolatedClassLoader.
     *
     * @param name             A descriptive name for this loader
     * @param urls             URLs to load classes from
     * @param parent           The parent classloader
     * @param excludedPackages Packages to delegate to parent
     */
    private IsolatedClassLoader(
            String name,
            URL[] urls,
            ClassLoader parent,
            Set<String> excludedPackages
    ) {
        super(urls, parent);
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.excludedPackages = Objects.requireNonNull(excludedPackages, "Excluded packages cannot be null");
    }

    /**
     * Creates an isolated classloader for the given JAR paths.
     *
     * @param name     A descriptive name for debugging
     * @param jarPaths Paths to JAR files to load
     * @param parent   The parent classloader
     * @return A new IsolatedClassLoader
     * @throws IllegalArgumentException if any path cannot be converted to URL
     */
    public static IsolatedClassLoader create(
            String name,
            Collection<Path> jarPaths,
            ClassLoader parent
    ) {
        return builder()
                .name(name)
                .jarPaths(jarPaths)
                .parent(parent)
                .build();
    }

    /**
     * Creates a new builder for IsolatedClassLoader.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Loads a class with child-first delegation.
     *
     * <p>Classes are loaded in this order:
     * <ol>
     *   <li>Check if already loaded</li>
     *   <li>Check if in system or excluded packages (delegate to parent)</li>
     *   <li>Try to load from this classloader's URLs</li>
     *   <li>Fall back to parent classloader</li>
     * </ol>
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

            // System packages always delegate
            if (isSystemPackage(name)) {
                return getParent().loadClass(name);
            }

            // Excluded packages delegate to parent
            if (isExcluded(name)) {
                return getParent().loadClass(name);
            }

            // Child-first: try to load from our URLs first
            try {
                loadedClass = findClass(name);
                if (loadedClass != null) {
                    // Track the loaded class
                    CodeSource source = loadedClass.getProtectionDomain().getCodeSource();
                    loadedClasses.put(name, source);

                    if (resolve) {
                        resolveClass(loadedClass);
                    }
                    return loadedClass;
                }
            } catch (ClassNotFoundException e) {
                // Fall through to parent
            }

            // Fall back to parent
            return getParent().loadClass(name);
        }
    }

    /**
     * Checks if a class is in a system package.
     */
    private boolean isSystemPackage(String className) {
        for (String pkg : SYSTEM_PACKAGES) {
            if (className.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a class is in an excluded package.
     */
    private boolean isExcluded(String className) {
        for (String pkg : excludedPackages) {
            if (className.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a resource with child-first lookup.
     *
     * @param name The resource name
     * @return The URL of the resource, or null if not found
     */
    @Override
    public URL getResource(String name) {
        if (closed) {
            return null;
        }

        // Try child first
        URL url = findResource(name);
        if (url != null) {
            return url;
        }

        // Fall back to parent
        return getParent().getResource(name);
    }

    /**
     * Gets all resources with child resources first.
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

        Enumeration<URL> childResources = findResources(name);
        Enumeration<URL> parentResources = getParent().getResources(name);

        return new CombinedEnumeration(childResources, parentResources);
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
     * Gets the name of this isolated classloader.
     *
     * @return The descriptive name
     */
    public String getLoaderName() {
        return name;
    }

    /**
     * Gets the packages excluded from isolation.
     *
     * @return An unmodifiable set of excluded package prefixes
     */
    public Set<String> getExcludedPackages() {
        return excludedPackages;
    }

    /**
     * Gets a map of all classes loaded by this classloader.
     *
     * @return An unmodifiable map of class name to code source
     */
    public Map<String, CodeSource> getLoadedClassSources() {
        return java.util.Collections.unmodifiableMap(loadedClasses);
    }

    /**
     * Gets the number of classes loaded by this classloader.
     *
     * @return The count of loaded classes
     */
    public int getLoadedClassCount() {
        return loadedClasses.size();
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
        loadedClasses.clear();
        super.close();
    }

    /**
     * Returns a string representation of this classloader.
     *
     * @return A descriptive string
     */
    @Override
    public String toString() {
        return "IsolatedClassLoader{" +
                "name='" + name + '\'' +
                ", loadedClasses=" + loadedClasses.size() +
                ", closed=" + closed +
                '}';
    }

    /**
     * Builder for IsolatedClassLoader.
     */
    public static final class Builder {
        private String name;
        private Collection<Path> jarPaths;
        private ClassLoader parent;
        private final Set<String> excludedPackages = new java.util.HashSet<>(Set.of(
                "org.bukkit.",
                "org.spigotmc.",
                "io.papermc.",
                "net.minecraft.",
                "sh.pcx.unified."
        ));

        private Builder() {
        }

        /**
         * Sets the name of the classloader.
         *
         * @param name A descriptive name
         * @return This builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the JAR paths to load.
         *
         * @param jarPaths Paths to JAR files
         * @return This builder
         */
        public Builder jarPaths(Collection<Path> jarPaths) {
            this.jarPaths = jarPaths;
            return this;
        }

        /**
         * Sets a single JAR path.
         *
         * @param jarPath Path to the JAR file
         * @return This builder
         */
        public Builder jarPath(Path jarPath) {
            this.jarPaths = Set.of(jarPath);
            return this;
        }

        /**
         * Sets the parent classloader.
         *
         * @param parent The parent classloader
         * @return This builder
         */
        public Builder parent(ClassLoader parent) {
            this.parent = parent;
            return this;
        }

        /**
         * Adds a package to exclude from isolation.
         *
         * @param packagePrefix The package prefix (e.g., "org.bukkit.")
         * @return This builder
         */
        public Builder excludePackage(String packagePrefix) {
            excludedPackages.add(packagePrefix);
            return this;
        }

        /**
         * Adds multiple packages to exclude.
         *
         * @param packages Package prefixes to exclude
         * @return This builder
         */
        public Builder excludePackages(Collection<String> packages) {
            excludedPackages.addAll(packages);
            return this;
        }

        /**
         * Clears the default excluded packages.
         *
         * @return This builder
         */
        public Builder clearExcludedPackages() {
            excludedPackages.clear();
            return this;
        }

        /**
         * Builds the IsolatedClassLoader.
         *
         * @return A new IsolatedClassLoader
         * @throws NullPointerException     if required fields are not set
         * @throws IllegalArgumentException if JAR paths cannot be converted to URLs
         */
        public IsolatedClassLoader build() {
            Objects.requireNonNull(name, "Name is required");
            Objects.requireNonNull(jarPaths, "JAR paths are required");
            Objects.requireNonNull(parent, "Parent classloader is required");

            if (jarPaths.isEmpty()) {
                throw new IllegalArgumentException("At least one JAR path is required");
            }

            URL[] urls = jarPaths.stream()
                    .map(path -> {
                        try {
                            return path.toUri().toURL();
                        } catch (MalformedURLException e) {
                            throw new IllegalArgumentException("Invalid JAR path: " + path, e);
                        }
                    })
                    .toArray(URL[]::new);

            return new IsolatedClassLoader(name, urls, parent, Set.copyOf(excludedPackages));
        }
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
