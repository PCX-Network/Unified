/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package sh.pcx.unified.library;

import java.util.Collection;
import java.util.Collections;

/**
 * Interface for per-plugin library configuration.
 *
 * <p>Plugins implement this interface to declare their library dependencies.
 * The framework uses this information to validate that all required libraries
 * are available before enabling the plugin.
 *
 * <h2>Implementation Example</h2>
 * <pre>{@code
 * public class MyPlugin extends UnifiedPlugin implements PluginLibraries {
 *
 *     @Override
 *     public Collection<LibraryDependency> getRequiredLibraries() {
 *         return List.of(
 *             LibraryDependency.required("guice", "[7.0.0,8.0.0)"),
 *             LibraryDependency.required("hikaricp", "[7.0.0,8.0.0)")
 *         );
 *     }
 *
 *     @Override
 *     public Collection<LibraryDependency> getOptionalLibraries() {
 *         return List.of(
 *             LibraryDependency.optional("jedis", "[7.0.0,8.0.0)"),
 *             LibraryDependency.optional("mongodb", "[5.0.0,6.0.0)")
 *         );
 *     }
 *
 *     @Override
 *     public void onPluginEnable() {
 *         // All required libraries are guaranteed available here
 *         Injector injector = Guice.createInjector(new MyModule());
 *
 *         // Check optional libraries
 *         if (isLibraryLoaded("jedis")) {
 *             setupRedisCache();
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Library Loading Behavior</h2>
 * <ul>
 *   <li><b>Required libraries</b>: Must be available. Plugin loading fails
 *       if any required library is missing or has incompatible version.</li>
 *   <li><b>Optional libraries</b>: May be unavailable. Plugin loads but
 *       features using the library should be disabled.</li>
 *   <li><b>Isolated libraries</b>: Loaded in separate classloader to avoid
 *       conflicts with other plugins.</li>
 * </ul>
 *
 * <h2>Version Validation</h2>
 * <p>The framework validates that loaded library versions satisfy the
 * declared version ranges. If a version conflict is detected, the plugin
 * is not enabled and a detailed error message is logged.
 *
 * @author Supatuck
 * @since 1.0.0
 * @see LibraryDependency
 * @see DependencyResolver
 * @see Libraries
 */
public interface PluginLibraries {

    /**
     * Gets the required library dependencies for this plugin.
     *
     * <p>Required libraries must be available for the plugin to enable.
     * If any required library is missing or has an incompatible version,
     * plugin loading will fail with an error.
     *
     * <pre>{@code
     * @Override
     * public Collection<LibraryDependency> getRequiredLibraries() {
     *     return List.of(
     *         LibraryDependency.required("guice", "[7.0.0,8.0.0)"),
     *         LibraryDependency.required("hikaricp")
     *     );
     * }
     * }</pre>
     *
     * @return A collection of required library dependencies
     */
    default Collection<LibraryDependency> getRequiredLibraries() {
        return Collections.emptyList();
    }

    /**
     * Gets the optional library dependencies for this plugin.
     *
     * <p>Optional libraries enhance the plugin but are not required for
     * basic functionality. The plugin should check availability before
     * using optional library features.
     *
     * <pre>{@code
     * @Override
     * public Collection<LibraryDependency> getOptionalLibraries() {
     *     return List.of(
     *         LibraryDependency.optional("jedis", "[7.0.0,8.0.0)"),
     *         LibraryDependency.optional("mongodb")
     *     );
     * }
     * }</pre>
     *
     * @return A collection of optional library dependencies
     */
    default Collection<LibraryDependency> getOptionalLibraries() {
        return Collections.emptyList();
    }

    /**
     * Gets libraries that should be loaded in isolated classloaders.
     *
     * <p>Isolated libraries are loaded separately to avoid classpath conflicts.
     * Use this when the plugin needs a different version of a library than
     * what the framework provides.
     *
     * <pre>{@code
     * @Override
     * public Collection<LibraryDependency> getIsolatedLibraries() {
     *     return List.of(
     *         // Need a specific Gson version
     *         LibraryDependency.required("custom-gson", "[2.14.0,2.15.0)")
     *     );
     * }
     * }</pre>
     *
     * @return A collection of isolated library dependencies
     */
    default Collection<LibraryDependency> getIsolatedLibraries() {
        return Collections.emptyList();
    }

    /**
     * Gets all library dependencies (required, optional, and isolated).
     *
     * @return A collection of all declared dependencies
     */
    default Collection<LibraryDependency> getAllLibraries() {
        java.util.List<LibraryDependency> all = new java.util.ArrayList<>();
        all.addAll(getRequiredLibraries());
        all.addAll(getOptionalLibraries());
        all.addAll(getIsolatedLibraries());
        return Collections.unmodifiableList(all);
    }

    /**
     * Checks if a library is loaded and available for this plugin.
     *
     * <p>This checks both the library availability and version compatibility
     * with declared dependencies.
     *
     * <pre>{@code
     * if (isLibraryLoaded("jedis")) {
     *     JedisPool pool = new JedisPool("localhost");
     * }
     * }</pre>
     *
     * @param name The library name (case-insensitive)
     * @return true if the library is loaded and compatible
     */
    default boolean isLibraryLoaded(String name) {
        if (!Libraries.isAvailable(name)) {
            return false;
        }

        // Check if version matches any declared dependency
        for (LibraryDependency dep : getAllLibraries()) {
            if (dep.name().equalsIgnoreCase(name)) {
                return dep.isSatisfied();
            }
        }

        // Library available but not declared - still usable
        return true;
    }

    /**
     * Validates that all required libraries are available with compatible versions.
     *
     * @throws LibraryNotFoundException if a required library is not available
     * @throws VersionConflictException if a library version is incompatible
     */
    default void validateLibraries() {
        for (LibraryDependency dep : getRequiredLibraries()) {
            if (!Libraries.isAvailable(dep.name())) {
                throw new LibraryNotFoundException(dep.name(),
                        Libraries.getAvailableNames());
            }

            Library library = Libraries.getLibrary(dep.name());
            if (!dep.isSatisfiedBy(library)) {
                throw VersionConflictException.incompatible(
                        dep.name(),
                        library.version(),
                        dep.versionRange(),
                        getPluginName()
                );
            }
        }
    }

    /**
     * Gets the name of the plugin implementing this interface.
     *
     * <p>This is used in error messages when library validation fails.
     *
     * @return The plugin name
     */
    default String getPluginName() {
        return getClass().getSimpleName();
    }

    /**
     * Called after all libraries have been validated and loaded.
     *
     * <p>Override this method to perform library-dependent initialization
     * that should happen before the main plugin enable.
     */
    default void onLibrariesLoaded() {
        // Default implementation does nothing
    }

    /**
     * Gets a report of library status for debugging.
     *
     * @return A formatted string showing library status
     */
    default String getLibraryReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Library Dependencies for ").append(getPluginName()).append(":\n");

        sb.append("\nRequired:\n");
        for (LibraryDependency dep : getRequiredLibraries()) {
            appendDependencyStatus(sb, dep);
        }

        sb.append("\nOptional:\n");
        for (LibraryDependency dep : getOptionalLibraries()) {
            appendDependencyStatus(sb, dep);
        }

        if (!getIsolatedLibraries().isEmpty()) {
            sb.append("\nIsolated:\n");
            for (LibraryDependency dep : getIsolatedLibraries()) {
                appendDependencyStatus(sb, dep);
            }
        }

        return sb.toString();
    }

    /**
     * Appends dependency status to a StringBuilder.
     */
    private static void appendDependencyStatus(StringBuilder sb, LibraryDependency dep) {
        sb.append("  - ").append(dep.name());
        sb.append(" ").append(dep.versionRange());

        if (Libraries.isAvailable(dep.name())) {
            Library lib = Libraries.getLibrary(dep.name());
            boolean satisfied = dep.isSatisfiedBy(lib);
            sb.append(" -> v").append(lib.version());
            sb.append(satisfied ? " [OK]" : " [INCOMPATIBLE]");
        } else {
            sb.append(" -> [NOT FOUND]");
        }
        sb.append("\n");
    }
}
