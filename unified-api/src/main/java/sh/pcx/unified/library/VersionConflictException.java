/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package sh.pcx.unified.library;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Exception thrown when there is a version conflict between library dependencies.
 *
 * <p>Version conflicts occur when multiple plugins require incompatible versions
 * of the same library, and no single version can satisfy all requirements.
 *
 * <h2>Common Conflict Scenarios</h2>
 * <ul>
 *   <li>Plugin A requires Guice [7.0.0,7.1.0) and Plugin B requires Guice [7.2.0,8.0.0)</li>
 *   <li>A plugin requires a version newer than what the framework provides</li>
 *   <li>Multiple conflicting transitive dependencies</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Catching version conflicts during resolution
 * try {
 *     resolver.resolveAll();
 * } catch (VersionConflictException e) {
 *     logger.error("Cannot resolve dependencies: " + e.getMessage());
 *     logger.error("Library: " + e.getLibraryName());
 *     logger.error("Available: " + e.getAvailableVersion());
 *     logger.error("Required: " + e.getRequiredRange());
 *     for (String requester : e.getRequesters()) {
 *         logger.error("  - Requested by: " + requester);
 *     }
 * }
 *
 * // Throwing with detailed information
 * throw new VersionConflictException(
 *     "guice",
 *     LibraryVersion.parse("7.0.0"),
 *     VersionRange.parse("[7.2.0,8.0.0)"),
 *     List.of("PluginA", "PluginB")
 * );
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see DependencyResolver
 * @see VersionRange
 */
public class VersionConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * The name of the library with the version conflict.
     */
    private final String libraryName;

    /**
     * The available version of the library (may be null if library is not loaded).
     */
    private final LibraryVersion availableVersion;

    /**
     * The version range that could not be satisfied.
     */
    private final VersionRange requiredRange;

    /**
     * The plugins or modules requesting the conflicting versions.
     */
    private final Collection<String> requesters;

    /**
     * Creates a new VersionConflictException with a simple message.
     *
     * @param message The error message describing the conflict
     */
    public VersionConflictException(String message) {
        super(message);
        this.libraryName = null;
        this.availableVersion = null;
        this.requiredRange = null;
        this.requesters = Collections.emptyList();
    }

    /**
     * Creates a new VersionConflictException with full conflict details.
     *
     * @param libraryName      The name of the library with the conflict
     * @param availableVersion The currently available version (may be null)
     * @param requiredRange    The version range that could not be satisfied
     * @param requesters       The plugins requesting the incompatible versions
     */
    public VersionConflictException(
            String libraryName,
            LibraryVersion availableVersion,
            VersionRange requiredRange,
            Collection<String> requesters
    ) {
        super(buildMessage(libraryName, availableVersion, requiredRange, requesters));
        this.libraryName = Objects.requireNonNull(libraryName, "Library name cannot be null");
        this.availableVersion = availableVersion;
        this.requiredRange = requiredRange;
        this.requesters = requesters != null ? List.copyOf(requesters) : Collections.emptyList();
    }

    /**
     * Creates a new VersionConflictException when a required library is not available.
     *
     * @param libraryName   The name of the missing library
     * @param requiredRange The version range that was requested
     * @param requester     The plugin requesting the library
     * @return A new VersionConflictException
     */
    public static VersionConflictException notAvailable(
            String libraryName,
            VersionRange requiredRange,
            String requester
    ) {
        return new VersionConflictException(
                libraryName,
                null,
                requiredRange,
                Collections.singletonList(requester)
        );
    }

    /**
     * Creates a new VersionConflictException when the available version is incompatible.
     *
     * @param libraryName      The name of the library
     * @param availableVersion The available version
     * @param requiredRange    The required version range
     * @param requester        The plugin with the incompatible requirement
     * @return A new VersionConflictException
     */
    public static VersionConflictException incompatible(
            String libraryName,
            LibraryVersion availableVersion,
            VersionRange requiredRange,
            String requester
    ) {
        return new VersionConflictException(
                libraryName,
                availableVersion,
                requiredRange,
                Collections.singletonList(requester)
        );
    }

    /**
     * Builds the exception message from conflict details.
     */
    private static String buildMessage(
            String libraryName,
            LibraryVersion availableVersion,
            VersionRange requiredRange,
            Collection<String> requesters
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Version conflict for library '").append(libraryName).append("': ");

        if (availableVersion == null) {
            sb.append("No version available");
        } else {
            sb.append("version ").append(availableVersion).append(" is available");
        }

        if (requiredRange != null) {
            sb.append(", but version ").append(requiredRange).append(" is required");
        }

        if (requesters != null && !requesters.isEmpty()) {
            sb.append(" by ");
            if (requesters.size() == 1) {
                sb.append(requesters.iterator().next());
            } else {
                sb.append(String.join(", ", requesters));
            }
        }

        return sb.toString();
    }

    /**
     * Gets the name of the library with the version conflict.
     *
     * @return The library name, or null if not specified
     */
    public String getLibraryName() {
        return libraryName;
    }

    /**
     * Gets the currently available version of the library.
     *
     * @return The available version, or null if the library is not loaded
     */
    public LibraryVersion getAvailableVersion() {
        return availableVersion;
    }

    /**
     * Gets the version range that could not be satisfied.
     *
     * @return The required version range, or null if not specified
     */
    public VersionRange getRequiredRange() {
        return requiredRange;
    }

    /**
     * Gets the plugins or modules requesting conflicting versions.
     *
     * @return An unmodifiable collection of requester names
     */
    public Collection<String> getRequesters() {
        return requesters;
    }

    /**
     * Checks if this conflict is resolvable by upgrading the available version.
     *
     * <p>A conflict is resolvable if there is an available version and a
     * required range, and the required range allows versions higher than
     * the current version.
     *
     * @return true if upgrading might resolve the conflict
     */
    public boolean isPotentiallyResolvable() {
        if (availableVersion == null || requiredRange == null) {
            return false;
        }
        // Check if the required range has a minimum that is higher than available
        return requiredRange.min() != null &&
               requiredRange.min().compareTo(availableVersion) > 0 &&
               requiredRange.max() == null ||
               (requiredRange.max() != null && requiredRange.max().compareTo(availableVersion) > 0);
    }

    // Import for List.copyOf
    private static final class List {
        static <T> java.util.List<T> copyOf(Collection<T> collection) {
            return java.util.List.copyOf(collection);
        }
    }
}
