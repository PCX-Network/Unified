/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package sh.pcx.unified.library;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for resolving library dependencies.
 *
 * <p>DependencyResolver analyzes and resolves library dependencies for plugins,
 * handling version conflicts and computing optimal loading orders. It supports
 * both simple linear resolution and complex conflict resolution scenarios.
 *
 * <h2>Resolution Process</h2>
 * <ol>
 *   <li>Collect all declared dependencies from plugins</li>
 *   <li>Identify version conflicts between plugins</li>
 *   <li>Attempt to find compatible versions for all requirements</li>
 *   <li>Compute dependency loading order</li>
 *   <li>Return resolved dependencies or report conflicts</li>
 * </ol>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * DependencyResolver resolver = // obtain resolver
 *
 * // Resolve dependencies for a plugin
 * List<LibraryDependency> deps = plugin.getRequiredLibraries();
 * ResolutionResult result = resolver.resolve(deps);
 *
 * if (result.isSuccess()) {
 *     for (Library lib : result.getResolvedLibraries()) {
 *         System.out.println("Loading: " + lib);
 *     }
 * } else {
 *     for (String conflict : result.getConflicts()) {
 *         System.err.println("Conflict: " + conflict);
 *     }
 * }
 *
 * // Check if a specific dependency can be satisfied
 * LibraryDependency hikari = LibraryDependency.required("hikaricp", "[7.0.0,8.0.0)");
 * if (resolver.canResolve(hikari)) {
 *     System.out.println("HikariCP is available");
 * }
 *
 * // Get the best matching version
 * Optional<Library> best = resolver.findBestMatch("guice",
 *     List.of(
 *         VersionRange.parse("[7.0.0,7.1.0)"),
 *         VersionRange.parse("[7.0.0,8.0.0)")
 *     )
 * );
 * }</pre>
 *
 * <h2>Conflict Resolution Strategies</h2>
 * <ul>
 *   <li><b>Highest Version</b>: Use the highest version that satisfies all constraints</li>
 *   <li><b>First Declared</b>: Use the version from the first plugin to declare it</li>
 *   <li><b>Explicit Override</b>: Use a version explicitly configured by server admin</li>
 *   <li><b>Fail</b>: Report an error if no compatible version exists</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see LibraryDependency
 * @see PluginLibraries
 * @see VersionRange
 */
public interface DependencyResolver {

    /**
     * Resolves a collection of library dependencies.
     *
     * <p>This method analyzes all dependencies, identifies potential conflicts,
     * and returns a resolution result containing either the resolved libraries
     * or information about unresolvable conflicts.
     *
     * @param dependencies The dependencies to resolve
     * @return The resolution result
     * @throws NullPointerException if dependencies is null
     */
    ResolutionResult resolve(Collection<LibraryDependency> dependencies);

    /**
     * Resolves dependencies from multiple sources (plugins).
     *
     * <p>This method handles dependencies from multiple plugins, detecting
     * cross-plugin conflicts and attempting to find versions that satisfy all.
     *
     * @param dependenciesBySource Dependencies grouped by source (plugin name)
     * @return The resolution result
     * @throws NullPointerException if dependenciesBySource is null
     */
    ResolutionResult resolveAll(Map<String, Collection<LibraryDependency>> dependenciesBySource);

    /**
     * Checks if a dependency can be resolved.
     *
     * @param dependency The dependency to check
     * @return true if the dependency can be satisfied
     * @throws NullPointerException if dependency is null
     */
    boolean canResolve(LibraryDependency dependency);

    /**
     * Finds the best matching library for given version constraints.
     *
     * <p>When multiple versions could satisfy the constraints, this returns
     * the "best" match according to the resolution strategy (typically the
     * highest compatible version).
     *
     * @param libraryName The library name
     * @param ranges      Version ranges to satisfy
     * @return An Optional containing the best match, or empty if none found
     * @throws NullPointerException if libraryName or ranges is null
     */
    Optional<Library> findBestMatch(String libraryName, Collection<VersionRange> ranges);

    /**
     * Computes the intersection of multiple version ranges.
     *
     * <p>Returns a range that satisfies all input ranges, or empty if the
     * ranges are mutually exclusive.
     *
     * @param ranges The version ranges to intersect
     * @return An Optional containing the intersection, or empty if none
     * @throws NullPointerException if ranges is null
     */
    Optional<VersionRange> intersectRanges(Collection<VersionRange> ranges);

    /**
     * Gets the loading order for resolved dependencies.
     *
     * <p>Libraries may have dependencies on each other. This returns an
     * ordered list where each library comes after all its dependencies.
     *
     * @param libraries The libraries to order
     * @return An ordered list of libraries
     * @throws NullPointerException if libraries is null
     */
    List<Library> computeLoadOrder(Collection<Library> libraries);

    /**
     * Validates that all dependencies can be satisfied.
     *
     * <p>This is a quick check that throws if any dependency cannot be
     * resolved, without performing full resolution.
     *
     * @param dependencies The dependencies to validate
     * @throws LibraryNotFoundException if a required library is not available
     * @throws VersionConflictException if version constraints cannot be satisfied
     */
    void validate(Collection<LibraryDependency> dependencies);

    /**
     * Gets the resolution strategy used by this resolver.
     *
     * @return The current resolution strategy
     */
    ResolutionStrategy getStrategy();

    /**
     * Sets the resolution strategy.
     *
     * @param strategy The strategy to use
     * @throws NullPointerException if strategy is null
     */
    void setStrategy(ResolutionStrategy strategy);

    /**
     * Result of dependency resolution.
     */
    interface ResolutionResult {

        /**
         * Checks if resolution was successful.
         *
         * @return true if all dependencies were resolved
         */
        boolean isSuccess();

        /**
         * Gets the resolved libraries.
         *
         * @return An unmodifiable list of resolved libraries
         * @throws IllegalStateException if resolution failed
         */
        List<Library> getResolvedLibraries();

        /**
         * Gets conflict descriptions if resolution failed.
         *
         * @return An unmodifiable list of conflict descriptions
         */
        List<String> getConflicts();

        /**
         * Gets the dependencies that could not be resolved.
         *
         * @return An unmodifiable collection of unresolved dependencies
         */
        Collection<LibraryDependency> getUnresolvedDependencies();

        /**
         * Gets version conflict details.
         *
         * @return A map of library name to conflict information
         */
        Map<String, VersionConflict> getVersionConflicts();

        /**
         * Throws an exception if resolution failed.
         *
         * @throws LibraryNotFoundException if libraries are missing
         * @throws VersionConflictException if versions conflict
         */
        void throwIfFailed();
    }

    /**
     * Information about a version conflict.
     */
    interface VersionConflict {

        /**
         * Gets the library name.
         *
         * @return The library name
         */
        String getLibraryName();

        /**
         * Gets the conflicting version ranges.
         *
         * @return A map of source to required range
         */
        Map<String, VersionRange> getConflictingRanges();

        /**
         * Gets the available version, if any.
         *
         * @return An Optional containing the available version
         */
        Optional<LibraryVersion> getAvailableVersion();

        /**
         * Gets a human-readable description of the conflict.
         *
         * @return The conflict description
         */
        String getDescription();
    }

    /**
     * Strategy for resolving version conflicts.
     */
    enum ResolutionStrategy {

        /**
         * Use the highest version that satisfies all constraints.
         */
        HIGHEST_VERSION,

        /**
         * Use the first declared version.
         */
        FIRST_DECLARED,

        /**
         * Fail if any conflict exists.
         */
        STRICT,

        /**
         * Use the version provided by the framework.
         */
        FRAMEWORK_PROVIDED
    }
}
