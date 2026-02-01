/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package sh.pcx.unified.library;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Event record for library load notifications.
 *
 * <p>LibraryLoadEvent is fired when a library loading operation completes,
 * whether successfully or with an error. It provides detailed information
 * about the loading process including timing, status, and any errors.
 *
 * <h2>Event Types</h2>
 * <ul>
 *   <li><b>Success</b>: Library loaded successfully ({@link #isSuccess()})</li>
 *   <li><b>Failure</b>: Library failed to load ({@link #isFailure()})</li>
 *   <li><b>Skipped</b>: Library loading was skipped ({@link #isSkipped()})</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Listen for library load events
 * provider.addLoadListener(event -> {
 *     if (event.isSuccess()) {
 *         logger.info("Loaded {} in {}ms",
 *             event.library().name(),
 *             event.loadDuration().toMillis());
 *     } else if (event.isFailure()) {
 *         logger.error("Failed to load {}: {}",
 *             event.libraryName(),
 *             event.errorMessage().orElse("Unknown error"),
 *             event.error().orElse(null));
 *     }
 * });
 *
 * // Create events
 * LibraryLoadEvent success = LibraryLoadEvent.success(
 *     library,
 *     Duration.ofMillis(45)
 * );
 *
 * LibraryLoadEvent failure = LibraryLoadEvent.failure(
 *     "hikaricp",
 *     new IOException("JAR not found"),
 *     Duration.ofMillis(10)
 * );
 *
 * // Check event details
 * System.out.println("Library: " + event.libraryName());
 * System.out.println("Status: " + event.status());
 * System.out.println("Timestamp: " + event.timestamp());
 * }</pre>
 *
 * @param libraryName  The name of the library
 * @param library      The loaded library (null if failed)
 * @param status       The resulting status
 * @param error        Any error that occurred (null if successful)
 * @param loadDuration The time taken to load
 * @param timestamp    When the event occurred
 * @author Supatuck
 * @since 1.0.0
 * @see LibraryProvider
 * @see LibraryStatus
 */
public record LibraryLoadEvent(
        String libraryName,
        Library library,
        LibraryStatus status,
        Throwable error,
        Duration loadDuration,
        Instant timestamp
) {

    /**
     * Creates a new LibraryLoadEvent with validation.
     *
     * @param libraryName  The name of the library
     * @param library      The loaded library (null if failed)
     * @param status       The resulting status
     * @param error        Any error that occurred (null if successful)
     * @param loadDuration The time taken to load
     * @param timestamp    When the event occurred
     * @throws NullPointerException     if libraryName, status, or timestamp is null
     * @throws IllegalArgumentException if libraryName is empty
     */
    public LibraryLoadEvent {
        Objects.requireNonNull(libraryName, "Library name cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");

        if (libraryName.isEmpty()) {
            throw new IllegalArgumentException("Library name cannot be empty");
        }

        // Default load duration to zero if null
        if (loadDuration == null) {
            loadDuration = Duration.ZERO;
        }
    }

    /**
     * Creates a success event for a loaded library.
     *
     * @param library      The loaded library
     * @param loadDuration The time taken to load
     * @return A new success event
     * @throws NullPointerException if library is null
     */
    public static LibraryLoadEvent success(Library library, Duration loadDuration) {
        Objects.requireNonNull(library, "Library cannot be null");
        return new LibraryLoadEvent(
                library.name(),
                library,
                LibraryStatus.AVAILABLE,
                null,
                loadDuration,
                Instant.now()
        );
    }

    /**
     * Creates a success event with the current timestamp.
     *
     * @param library The loaded library
     * @return A new success event
     * @throws NullPointerException if library is null
     */
    public static LibraryLoadEvent success(Library library) {
        return success(library, Duration.ZERO);
    }

    /**
     * Creates a failure event.
     *
     * @param libraryName  The name of the library that failed
     * @param error        The error that occurred
     * @param loadDuration The time until failure
     * @return A new failure event
     * @throws NullPointerException if libraryName or error is null
     */
    public static LibraryLoadEvent failure(String libraryName, Throwable error, Duration loadDuration) {
        Objects.requireNonNull(libraryName, "Library name cannot be null");
        Objects.requireNonNull(error, "Error cannot be null");
        return new LibraryLoadEvent(
                libraryName,
                null,
                LibraryStatus.FAILED,
                error,
                loadDuration,
                Instant.now()
        );
    }

    /**
     * Creates a failure event with the current timestamp.
     *
     * @param libraryName The name of the library that failed
     * @param error       The error that occurred
     * @return A new failure event
     */
    public static LibraryLoadEvent failure(String libraryName, Throwable error) {
        return failure(libraryName, error, Duration.ZERO);
    }

    /**
     * Creates an event for a library that was not found.
     *
     * @param libraryName The name of the library
     * @return A new not-found event
     */
    public static LibraryLoadEvent notFound(String libraryName) {
        return new LibraryLoadEvent(
                libraryName,
                null,
                LibraryStatus.NOT_FOUND,
                null,
                Duration.ZERO,
                Instant.now()
        );
    }

    /**
     * Creates an event for a skipped library (already loaded).
     *
     * @param library The already-loaded library
     * @return A new skipped event
     */
    public static LibraryLoadEvent skipped(Library library) {
        return new LibraryLoadEvent(
                library.name(),
                library,
                LibraryStatus.AVAILABLE,
                null,
                Duration.ZERO,
                Instant.now()
        );
    }

    /**
     * Creates a loading-started event.
     *
     * @param libraryName The name of the library
     * @return A new loading event
     */
    public static LibraryLoadEvent loading(String libraryName) {
        return new LibraryLoadEvent(
                libraryName,
                null,
                LibraryStatus.LOADING,
                null,
                Duration.ZERO,
                Instant.now()
        );
    }

    /**
     * Checks if this event represents a successful load.
     *
     * @return true if the library was loaded successfully
     */
    public boolean isSuccess() {
        return status == LibraryStatus.AVAILABLE && library != null;
    }

    /**
     * Checks if this event represents a failed load.
     *
     * @return true if loading failed
     */
    public boolean isFailure() {
        return status == LibraryStatus.FAILED;
    }

    /**
     * Checks if this event represents a skipped load.
     *
     * @return true if loading was skipped (already loaded)
     */
    public boolean isSkipped() {
        return status == LibraryStatus.AVAILABLE && loadDuration.isZero();
    }

    /**
     * Checks if loading is in progress.
     *
     * @return true if in loading state
     */
    public boolean isLoading() {
        return status == LibraryStatus.LOADING;
    }

    /**
     * Gets the library if loaded successfully.
     *
     * @return An Optional containing the library
     */
    public Optional<Library> getLibrary() {
        return Optional.ofNullable(library);
    }

    /**
     * Gets the error if loading failed.
     *
     * @return An Optional containing the error
     */
    public Optional<Throwable> getError() {
        return Optional.ofNullable(error);
    }

    /**
     * Gets the error message if loading failed.
     *
     * @return An Optional containing the error message
     */
    public Optional<String> errorMessage() {
        return error != null ? Optional.ofNullable(error.getMessage()) : Optional.empty();
    }

    /**
     * Gets the library version if loaded successfully.
     *
     * @return An Optional containing the version
     */
    public Optional<LibraryVersion> version() {
        return library != null ? Optional.of(library.version()) : Optional.empty();
    }

    /**
     * Gets the load duration in milliseconds.
     *
     * @return The load duration in milliseconds
     */
    public long loadDurationMillis() {
        return loadDuration.toMillis();
    }

    /**
     * Returns a string representation of this event.
     *
     * @return A descriptive string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LibraryLoadEvent{");
        sb.append("library='").append(libraryName).append('\'');
        sb.append(", status=").append(status);

        if (library != null) {
            sb.append(", version=").append(library.version());
        }

        if (error != null) {
            sb.append(", error=").append(error.getClass().getSimpleName());
            if (error.getMessage() != null) {
                sb.append(": ").append(error.getMessage());
            }
        }

        if (!loadDuration.isZero()) {
            sb.append(", duration=").append(loadDuration.toMillis()).append("ms");
        }

        sb.append('}');
        return sb.toString();
    }
}
