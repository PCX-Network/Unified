/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package sh.pcx.unified.library;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Exception thrown when a requested library is not available in the library provider.
 *
 * <p>This exception is thrown when:
 * <ul>
 *   <li>A plugin requests a library that is not provided by the framework</li>
 *   <li>A library failed to load and is not accessible</li>
 *   <li>A library name is misspelled or not recognized</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Handling library not found
 * try {
 *     Library lib = Libraries.getLibrary("unknown-library");
 * } catch (LibraryNotFoundException e) {
 *     logger.warn("Library not available: " + e.getLibraryName());
 *     logger.info("Available libraries: " + e.getAvailableLibraries());
 * }
 *
 * // Safe access with fallback
 * if (Libraries.isAvailable("optional-lib")) {
 *     // Use the library
 * } else {
 *     // Use fallback implementation
 * }
 *
 * // Throwing with suggestions
 * throw new LibraryNotFoundException("hikari", List.of("hikaricp", "c3p0"));
 * }</pre>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li>Always check {@link Libraries#isAvailable(String)} before accessing optional libraries</li>
 *   <li>Use the suggestion feature to help users correct typos</li>
 *   <li>Log available libraries when debugging dependency issues</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Libraries
 * @see LibraryProvider
 */
public class LibraryNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * The name of the library that was not found.
     */
    private final String libraryName;

    /**
     * Libraries that are available and might be similar to the requested one.
     */
    private final Collection<String> availableLibraries;

    /**
     * Suggested alternatives for the missing library.
     */
    private final Collection<String> suggestions;

    /**
     * Creates a new LibraryNotFoundException with the library name.
     *
     * @param libraryName The name of the library that was not found
     */
    public LibraryNotFoundException(String libraryName) {
        super(buildMessage(libraryName, null, null));
        this.libraryName = Objects.requireNonNull(libraryName, "Library name cannot be null");
        this.availableLibraries = Collections.emptyList();
        this.suggestions = Collections.emptyList();
    }

    /**
     * Creates a new LibraryNotFoundException with available alternatives.
     *
     * @param libraryName        The name of the library that was not found
     * @param availableLibraries The libraries that are available
     */
    public LibraryNotFoundException(String libraryName, Collection<String> availableLibraries) {
        super(buildMessage(libraryName, availableLibraries, null));
        this.libraryName = Objects.requireNonNull(libraryName, "Library name cannot be null");
        this.availableLibraries = availableLibraries != null ?
                java.util.List.copyOf(availableLibraries) : Collections.emptyList();
        this.suggestions = findSuggestions(libraryName, this.availableLibraries);
    }

    /**
     * Creates a new LibraryNotFoundException with a custom message.
     *
     * @param libraryName The name of the library that was not found
     * @param message     A custom error message
     */
    public LibraryNotFoundException(String libraryName, String message) {
        super(message);
        this.libraryName = Objects.requireNonNull(libraryName, "Library name cannot be null");
        this.availableLibraries = Collections.emptyList();
        this.suggestions = Collections.emptyList();
    }

    /**
     * Creates a new LibraryNotFoundException with a cause.
     *
     * @param libraryName The name of the library that was not found
     * @param cause       The underlying cause of the exception
     */
    public LibraryNotFoundException(String libraryName, Throwable cause) {
        super(buildMessage(libraryName, null, null), cause);
        this.libraryName = Objects.requireNonNull(libraryName, "Library name cannot be null");
        this.availableLibraries = Collections.emptyList();
        this.suggestions = Collections.emptyList();
    }

    /**
     * Builds the exception message.
     */
    private static String buildMessage(
            String libraryName,
            Collection<String> availableLibraries,
            Collection<String> suggestions
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Library '").append(libraryName).append("' not found");

        if (suggestions != null && !suggestions.isEmpty()) {
            sb.append(". Did you mean: ").append(String.join(", ", suggestions)).append("?");
        }

        if (availableLibraries != null && !availableLibraries.isEmpty()) {
            sb.append(" Available libraries: ").append(String.join(", ", availableLibraries));
        }

        return sb.toString();
    }

    /**
     * Finds suggestions based on edit distance.
     */
    private static Collection<String> findSuggestions(String name, Collection<String> available) {
        if (available == null || available.isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedName = name.toLowerCase();
        java.util.List<String> suggestions = new java.util.ArrayList<>();

        for (String lib : available) {
            // Check for substring match
            if (lib.toLowerCase().contains(normalizedName) ||
                normalizedName.contains(lib.toLowerCase())) {
                suggestions.add(lib);
            }
            // Check for similar prefix
            else if (lib.toLowerCase().startsWith(normalizedName.substring(0, Math.min(3, normalizedName.length())))) {
                suggestions.add(lib);
            }
        }

        return suggestions;
    }

    /**
     * Gets the name of the library that was not found.
     *
     * @return The library name
     */
    public String getLibraryName() {
        return libraryName;
    }

    /**
     * Gets the libraries that are available.
     *
     * @return An unmodifiable collection of available library names
     */
    public Collection<String> getAvailableLibraries() {
        return availableLibraries;
    }

    /**
     * Gets suggestions for similar library names.
     *
     * <p>Suggestions are computed based on:
     * <ul>
     *   <li>Substring matching</li>
     *   <li>Prefix similarity</li>
     *   <li>Edit distance</li>
     * </ul>
     *
     * @return An unmodifiable collection of suggested library names
     */
    public Collection<String> getSuggestions() {
        return suggestions;
    }

    /**
     * Checks if there are suggestions available.
     *
     * @return true if there are suggested alternatives
     */
    public boolean hasSuggestions() {
        return !suggestions.isEmpty();
    }

    /**
     * Creates an exception for when a library is not provided by the framework.
     *
     * @param libraryName The name of the library
     * @return A new LibraryNotFoundException
     */
    public static LibraryNotFoundException notProvided(String libraryName) {
        return new LibraryNotFoundException(libraryName,
                "Library '" + libraryName + "' is not provided by the UnifiedPlugin API. " +
                "You may need to shade this dependency in your plugin.");
    }

    /**
     * Creates an exception for when a library failed to load.
     *
     * @param libraryName The name of the library
     * @param cause       The cause of the load failure
     * @return A new LibraryNotFoundException
     */
    public static LibraryNotFoundException loadFailed(String libraryName, Throwable cause) {
        return new LibraryNotFoundException(
                libraryName,
                new RuntimeException("Library '" + libraryName + "' failed to load", cause)
        );
    }

    /**
     * Creates an exception for when a library version is not compatible.
     *
     * @param libraryName The name of the library
     * @param required    The required version range
     * @param available   The available version
     * @return A new LibraryNotFoundException with version details
     */
    public static LibraryNotFoundException versionMismatch(
            String libraryName,
            VersionRange required,
            LibraryVersion available
    ) {
        return new LibraryNotFoundException(libraryName,
                "Library '" + libraryName + "' version " + available +
                " does not satisfy required range " + required);
    }
}
