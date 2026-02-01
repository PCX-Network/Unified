/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root.
 */
package sh.pcx.unified.library;

/**
 * Enumeration of possible library loading states.
 *
 * <p>LibraryStatus represents the current state of a library in the
 * loading lifecycle. Libraries transition through these states as they
 * are discovered, loaded, and made available for use.
 *
 * <h2>State Transitions</h2>
 * <pre>
 *                    +-------------+
 *                    |  NOT_FOUND  |
 *                    +-------------+
 *                          |
 *           (library registered in registry)
 *                          v
 *                    +-------------+
 *                    |  REGISTERED |
 *                    +-------------+
 *                          |
 *              (plugin requests library)
 *                          v
 *                    +-------------+
 *                    |   LOADING   |
 *                    +-------------+
 *                        /   \
 *            (success)  /     \  (failure)
 *                      v       v
 *              +-----------+ +--------+
 *              | AVAILABLE | | FAILED |
 *              +-----------+ +--------+
 * </pre>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * LibraryStatus status = Libraries.getStatus("hikaricp");
 *
 * switch (status) {
 *     case AVAILABLE -> {
 *         // Library is ready to use
 *         Library lib = Libraries.getLibrary("hikaricp");
 *         useHikari();
 *     }
 *     case LOADING -> {
 *         // Wait for loading to complete
 *         waitForLibrary("hikaricp");
 *     }
 *     case FAILED -> {
 *         // Handle load failure
 *         logger.error("HikariCP failed to load");
 *         useFallbackConnection();
 *     }
 *     case REGISTERED -> {
 *         // Library exists but not yet loaded
 *         Libraries.preload("hikaricp");
 *     }
 *     case NOT_FOUND -> {
 *         // Library not available
 *         logger.warn("HikariCP not provided");
 *     }
 * }
 *
 * // Check if usable
 * if (status.isUsable()) {
 *     // Safe to use the library
 * }
 *
 * // Check if terminal state
 * if (status.isTerminal()) {
 *     // No further transitions expected
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Libraries
 * @see LibraryProvider
 */
public enum LibraryStatus {

    /**
     * Library is loaded and ready for use.
     *
     * <p>This is the normal operating state. All library classes are
     * accessible and can be instantiated.
     */
    AVAILABLE("Library is loaded and ready for use", true, true),

    /**
     * Library is currently being loaded.
     *
     * <p>This is a transient state during library initialization.
     * The library will transition to either AVAILABLE or FAILED.
     */
    LOADING("Library is currently being loaded", false, false),

    /**
     * Library failed to load.
     *
     * <p>An error occurred during library loading. Check the server
     * logs for details. The library cannot be used.
     */
    FAILED("Library failed to load", false, true),

    /**
     * Library is registered but not yet loaded.
     *
     * <p>The library is known to the registry but has not been
     * loaded yet. Use {@link Libraries#preload(String)} to trigger
     * loading.
     */
    REGISTERED("Library is registered but not loaded", false, false),

    /**
     * Library is not found in the registry.
     *
     * <p>The requested library is not provided by the framework.
     * You may need to shade this dependency in your plugin or
     * check for typos in the library name.
     */
    NOT_FOUND("Library is not available", false, true);

    /**
     * Human-readable description of this status.
     */
    private final String description;

    /**
     * Whether the library can be used in this state.
     */
    private final boolean usable;

    /**
     * Whether this is a terminal state (no further transitions).
     */
    private final boolean terminal;

    /**
     * Creates a new LibraryStatus.
     *
     * @param description Human-readable description
     * @param usable      Whether the library is usable
     * @param terminal    Whether this is a terminal state
     */
    LibraryStatus(String description, boolean usable, boolean terminal) {
        this.description = description;
        this.usable = usable;
        this.terminal = terminal;
    }

    /**
     * Gets the human-readable description of this status.
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if the library is usable in this state.
     *
     * <p>Only libraries in the AVAILABLE state are usable.
     *
     * @return true if the library can be used
     */
    public boolean isUsable() {
        return usable;
    }

    /**
     * Checks if this is a terminal state.
     *
     * <p>Terminal states (AVAILABLE, FAILED, NOT_FOUND) indicate that
     * no further state transitions are expected without manual
     * intervention.
     *
     * @return true if no further transitions are expected
     */
    public boolean isTerminal() {
        return terminal;
    }

    /**
     * Checks if loading is in progress.
     *
     * @return true if in LOADING state
     */
    public boolean isLoading() {
        return this == LOADING;
    }

    /**
     * Checks if the library is available.
     *
     * @return true if in AVAILABLE state
     */
    public boolean isAvailable() {
        return this == AVAILABLE;
    }

    /**
     * Checks if loading failed.
     *
     * @return true if in FAILED state
     */
    public boolean isFailed() {
        return this == FAILED;
    }

    /**
     * Checks if the library was not found.
     *
     * @return true if in NOT_FOUND state
     */
    public boolean isNotFound() {
        return this == NOT_FOUND;
    }

    /**
     * Checks if the library is registered.
     *
     * @return true if in REGISTERED or AVAILABLE state
     */
    public boolean isRegistered() {
        return this == REGISTERED || this == AVAILABLE;
    }

    /**
     * Checks if loading can be retried.
     *
     * <p>Loading can be retried from FAILED or REGISTERED states.
     *
     * @return true if loading can be attempted
     */
    public boolean canRetry() {
        return this == FAILED || this == REGISTERED;
    }

    /**
     * Returns a string representation of this status.
     *
     * @return The status name and description
     */
    @Override
    public String toString() {
        return name() + ": " + description;
    }
}
