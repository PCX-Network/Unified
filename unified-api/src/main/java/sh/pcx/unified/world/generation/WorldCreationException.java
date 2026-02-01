/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown when world creation, loading, or import fails.
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see WorldService
 * @see WorldCreator
 */
public class WorldCreationException extends RuntimeException {

    private final String worldName;
    private final Reason reason;

    /**
     * Creates a new world creation exception.
     *
     * @param worldName the world name
     * @param message   the error message
     * @since 1.0.0
     */
    public WorldCreationException(@NotNull String worldName, @NotNull String message) {
        super(message);
        this.worldName = worldName;
        this.reason = Reason.UNKNOWN;
    }

    /**
     * Creates a new world creation exception with a reason.
     *
     * @param worldName the world name
     * @param reason    the failure reason
     * @param message   the error message
     * @since 1.0.0
     */
    public WorldCreationException(@NotNull String worldName, @NotNull Reason reason, @NotNull String message) {
        super(message);
        this.worldName = worldName;
        this.reason = reason;
    }

    /**
     * Creates a new world creation exception with a cause.
     *
     * @param worldName the world name
     * @param message   the error message
     * @param cause     the underlying cause
     * @since 1.0.0
     */
    public WorldCreationException(@NotNull String worldName, @NotNull String message, @Nullable Throwable cause) {
        super(message, cause);
        this.worldName = worldName;
        this.reason = Reason.UNKNOWN;
    }

    /**
     * Creates a new world creation exception with a reason and cause.
     *
     * @param worldName the world name
     * @param reason    the failure reason
     * @param message   the error message
     * @param cause     the underlying cause
     * @since 1.0.0
     */
    public WorldCreationException(@NotNull String worldName, @NotNull Reason reason,
                                  @NotNull String message, @Nullable Throwable cause) {
        super(message, cause);
        this.worldName = worldName;
        this.reason = reason;
    }

    /**
     * Gets the world name that failed to be created.
     *
     * @return the world name
     * @since 1.0.0
     */
    @NotNull
    public String getWorldName() {
        return worldName;
    }

    /**
     * Gets the failure reason.
     *
     * @return the reason
     * @since 1.0.0
     */
    @NotNull
    public Reason getReason() {
        return reason;
    }

    // ==================== Factory Methods ====================

    /**
     * Creates an exception for a world that already exists.
     *
     * @param worldName the world name
     * @return the exception
     * @since 1.0.0
     */
    @NotNull
    public static WorldCreationException alreadyExists(@NotNull String worldName) {
        return new WorldCreationException(worldName, Reason.ALREADY_EXISTS,
                "World '" + worldName + "' already exists");
    }

    /**
     * Creates an exception for an invalid world name.
     *
     * @param worldName the world name
     * @return the exception
     * @since 1.0.0
     */
    @NotNull
    public static WorldCreationException invalidName(@NotNull String worldName) {
        return new WorldCreationException(worldName, Reason.INVALID_NAME,
                "Invalid world name: '" + worldName + "'");
    }

    /**
     * Creates an exception for a generator not found.
     *
     * @param worldName   the world name
     * @param generatorId the generator ID
     * @return the exception
     * @since 1.0.0
     */
    @NotNull
    public static WorldCreationException generatorNotFound(@NotNull String worldName, @NotNull String generatorId) {
        return new WorldCreationException(worldName, Reason.GENERATOR_NOT_FOUND,
                "Generator not found: '" + generatorId + "'");
    }

    /**
     * Creates an exception for IO errors.
     *
     * @param worldName the world name
     * @param cause     the IO exception
     * @return the exception
     * @since 1.0.0
     */
    @NotNull
    public static WorldCreationException ioError(@NotNull String worldName, @NotNull Throwable cause) {
        return new WorldCreationException(worldName, Reason.IO_ERROR,
                "IO error creating world '" + worldName + "': " + cause.getMessage(), cause);
    }

    /**
     * Creates an exception for world not found.
     *
     * @param worldName the world name
     * @return the exception
     * @since 1.0.0
     */
    @NotNull
    public static WorldCreationException notFound(@NotNull String worldName) {
        return new WorldCreationException(worldName, Reason.NOT_FOUND,
                "World not found: '" + worldName + "'");
    }

    /**
     * Creates an exception for corrupted world data.
     *
     * @param worldName the world name
     * @return the exception
     * @since 1.0.0
     */
    @NotNull
    public static WorldCreationException corrupted(@NotNull String worldName) {
        return new WorldCreationException(worldName, Reason.CORRUPTED,
                "World data is corrupted: '" + worldName + "'");
    }

    /**
     * Reasons for world creation failure.
     *
     * @since 1.0.0
     */
    public enum Reason {
        /** World with the same name already exists. */
        ALREADY_EXISTS,
        /** Invalid world name (illegal characters, reserved names). */
        INVALID_NAME,
        /** Specified generator was not found. */
        GENERATOR_NOT_FOUND,
        /** IO error during creation. */
        IO_ERROR,
        /** World files not found. */
        NOT_FOUND,
        /** World data is corrupted. */
        CORRUPTED,
        /** Operation timed out. */
        TIMEOUT,
        /** Platform does not support this operation. */
        UNSUPPORTED,
        /** Unknown error. */
        UNKNOWN
    }
}
