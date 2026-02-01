/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package sh.pcx.unified.library;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for library classloader initialization.
 *
 * <p>This record provides all the configuration options needed to create
 * a classloader for loading shared libraries. It supports both shared
 * and isolated loading modes.
 *
 * <h2>Loading Modes</h2>
 * <ul>
 *   <li><b>Shared (parent-first)</b>: Classes are loaded from parent classloader first.
 *       This is the default mode, allowing libraries to share classes.</li>
 *   <li><b>Isolated (child-first)</b>: Classes are loaded from the library JAR first.
 *       Use this when a plugin needs a different version than the shared one.</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Simple shared library configuration
 * ClassLoaderConfig shared = ClassLoaderConfig.forSharedLibrary(
 *     "hikaricp",
 *     Path.of("lib/hikaricp-7.0.2.jar"),
 *     getClass().getClassLoader()
 * );
 *
 * // Isolated library with exclusions
 * ClassLoaderConfig isolated = ClassLoaderConfig.builder()
 *     .libraryName("special-lib")
 *     .jarPaths(Set.of(Path.of("lib/special.jar")))
 *     .parent(getClass().getClassLoader())
 *     .isolated(true)
 *     .excludedPackages(Set.of("org.bukkit", "net.minecraft"))
 *     .build();
 *
 * // Create the classloader
 * LibraryClassLoader loader = new LibraryClassLoader(config);
 * }</pre>
 *
 * <h2>Package Exclusions</h2>
 * <p>Excluded packages are always loaded from the parent classloader,
 * regardless of the isolation mode. This prevents conflicts with
 * server-provided classes like Bukkit API.
 *
 * @param libraryName      The name of the library being loaded
 * @param jarPaths         Paths to JAR files to load
 * @param parent           The parent classloader
 * @param isolated         Whether to use child-first loading
 * @param excludedPackages Packages to always load from parent
 * @author Supatuck
 * @since 1.0.0
 * @see LibraryClassLoader
 * @see IsolatedClassLoader
 */
public record ClassLoaderConfig(
        String libraryName,
        Set<Path> jarPaths,
        ClassLoader parent,
        boolean isolated,
        Set<String> excludedPackages
) {

    /**
     * Default packages that are always excluded from isolation.
     */
    public static final Set<String> DEFAULT_EXCLUDED_PACKAGES = Set.of(
            "java.",
            "javax.",
            "sun.",
            "com.sun.",
            "org.bukkit.",
            "org.spigotmc.",
            "io.papermc.",
            "net.minecraft.",
            "org.slf4j.",
            "sh.pcx.unified."
    );

    /**
     * Creates a new ClassLoaderConfig with validation.
     *
     * @param libraryName      The name of the library being loaded
     * @param jarPaths         Paths to JAR files to load
     * @param parent           The parent classloader
     * @param isolated         Whether to use child-first loading
     * @param excludedPackages Packages to always load from parent
     * @throws NullPointerException     if required parameters are null
     * @throws IllegalArgumentException if libraryName is empty or jarPaths is empty
     */
    public ClassLoaderConfig {
        Objects.requireNonNull(libraryName, "Library name cannot be null");
        Objects.requireNonNull(jarPaths, "JAR paths cannot be null");
        Objects.requireNonNull(parent, "Parent classloader cannot be null");

        if (libraryName.isEmpty()) {
            throw new IllegalArgumentException("Library name cannot be empty");
        }
        if (jarPaths.isEmpty()) {
            throw new IllegalArgumentException("At least one JAR path is required");
        }

        // Make immutable copies
        jarPaths = Set.copyOf(jarPaths);
        excludedPackages = excludedPackages != null ?
                Set.copyOf(excludedPackages) : DEFAULT_EXCLUDED_PACKAGES;
    }

    /**
     * Creates a configuration for a shared (parent-first) library.
     *
     * @param libraryName The library name
     * @param jarPath     The path to the library JAR
     * @param parent      The parent classloader
     * @return A new ClassLoaderConfig for shared loading
     */
    public static ClassLoaderConfig forSharedLibrary(
            String libraryName,
            Path jarPath,
            ClassLoader parent
    ) {
        return new ClassLoaderConfig(
                libraryName,
                Set.of(jarPath),
                parent,
                false,
                DEFAULT_EXCLUDED_PACKAGES
        );
    }

    /**
     * Creates a configuration for an isolated (child-first) library.
     *
     * @param libraryName The library name
     * @param jarPath     The path to the library JAR
     * @param parent      The parent classloader
     * @return A new ClassLoaderConfig for isolated loading
     */
    public static ClassLoaderConfig forIsolatedLibrary(
            String libraryName,
            Path jarPath,
            ClassLoader parent
    ) {
        return new ClassLoaderConfig(
                libraryName,
                Set.of(jarPath),
                parent,
                true,
                DEFAULT_EXCLUDED_PACKAGES
        );
    }

    /**
     * Creates a new builder for ClassLoaderConfig.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a copy of this configuration with a different isolation setting.
     *
     * @param isolated The new isolation setting
     * @return A new ClassLoaderConfig with the updated setting
     */
    public ClassLoaderConfig withIsolation(boolean isolated) {
        return new ClassLoaderConfig(libraryName, jarPaths, parent, isolated, excludedPackages);
    }

    /**
     * Creates a copy with additional JAR paths.
     *
     * @param additionalPaths Additional JAR paths to include
     * @return A new ClassLoaderConfig with the additional paths
     */
    public ClassLoaderConfig withAdditionalJars(Collection<Path> additionalPaths) {
        Set<Path> combined = new java.util.HashSet<>(jarPaths);
        combined.addAll(additionalPaths);
        return new ClassLoaderConfig(libraryName, combined, parent, isolated, excludedPackages);
    }

    /**
     * Creates a copy with additional excluded packages.
     *
     * @param packages Additional packages to exclude
     * @return A new ClassLoaderConfig with the additional exclusions
     */
    public ClassLoaderConfig withExcludedPackages(Collection<String> packages) {
        Set<String> combined = new java.util.HashSet<>(excludedPackages);
        combined.addAll(packages);
        return new ClassLoaderConfig(libraryName, jarPaths, parent, isolated, combined);
    }

    /**
     * Checks if a class name is in an excluded package.
     *
     * @param className The fully qualified class name
     * @return true if the class should be loaded from parent
     */
    public boolean isExcluded(String className) {
        for (String excluded : excludedPackages) {
            if (className.startsWith(excluded)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the primary JAR path (first in the set).
     *
     * @return The primary JAR path
     */
    public Path primaryJarPath() {
        return jarPaths.iterator().next();
    }

    /**
     * Builder for ClassLoaderConfig.
     */
    public static final class Builder {
        private String libraryName;
        private Set<Path> jarPaths = Collections.emptySet();
        private ClassLoader parent;
        private boolean isolated = false;
        private Set<String> excludedPackages = DEFAULT_EXCLUDED_PACKAGES;

        private Builder() {
        }

        /**
         * Sets the library name.
         *
         * @param libraryName The library name
         * @return This builder
         */
        public Builder libraryName(String libraryName) {
            this.libraryName = libraryName;
            return this;
        }

        /**
         * Sets the JAR paths.
         *
         * @param jarPaths The JAR paths
         * @return This builder
         */
        public Builder jarPaths(Set<Path> jarPaths) {
            this.jarPaths = jarPaths;
            return this;
        }

        /**
         * Sets a single JAR path.
         *
         * @param jarPath The JAR path
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
         * Sets whether to use isolated (child-first) loading.
         *
         * @param isolated true for child-first, false for parent-first
         * @return This builder
         */
        public Builder isolated(boolean isolated) {
            this.isolated = isolated;
            return this;
        }

        /**
         * Sets the excluded packages.
         *
         * @param excludedPackages Packages to load from parent
         * @return This builder
         */
        public Builder excludedPackages(Set<String> excludedPackages) {
            this.excludedPackages = excludedPackages;
            return this;
        }

        /**
         * Builds the ClassLoaderConfig.
         *
         * @return A new ClassLoaderConfig
         * @throws NullPointerException     if required fields are not set
         * @throws IllegalArgumentException if validation fails
         */
        public ClassLoaderConfig build() {
            return new ClassLoaderConfig(libraryName, jarPaths, parent, isolated, excludedPackages);
        }
    }
}
