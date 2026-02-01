/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown when schematic loading or saving fails.
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see StructureService
 * @see Schematic
 */
public class SchematicException extends RuntimeException {

    /**
     * Creates a new schematic exception.
     *
     * @param message the error message
     * @since 1.0.0
     */
    public SchematicException(@NotNull String message) {
        super(message);
    }

    /**
     * Creates a new schematic exception with a cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     * @since 1.0.0
     */
    public SchematicException(@NotNull String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception for an invalid schematic format.
     *
     * @param format the expected format
     * @return the exception
     * @since 1.0.0
     */
    @NotNull
    public static SchematicException invalidFormat(@NotNull StructureService.SchematicFormat format) {
        return new SchematicException("Invalid schematic format: expected " + format);
    }

    /**
     * Creates an exception for a corrupted schematic.
     *
     * @param reason the corruption reason
     * @return the exception
     * @since 1.0.0
     */
    @NotNull
    public static SchematicException corrupted(@NotNull String reason) {
        return new SchematicException("Corrupted schematic: " + reason);
    }

    /**
     * Creates an exception for a schematic that is too large.
     *
     * @param width  the width
     * @param height the height
     * @param depth  the depth
     * @param maxSize the maximum allowed size
     * @return the exception
     * @since 1.0.0
     */
    @NotNull
    public static SchematicException tooLarge(int width, int height, int depth, int maxSize) {
        return new SchematicException(String.format(
                "Schematic too large: %dx%dx%d exceeds maximum %d",
                width, height, depth, maxSize));
    }
}
