/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown when serialization or deserialization fails.
 *
 * <p>This exception wraps underlying errors that occur during the serialization
 * process, providing context about what operation failed and why.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * try {
 *     ItemStack item = serializer.deserialize(data, context);
 * } catch (SerializationException e) {
 *     logger.error("Failed to deserialize item: " + e.getMessage());
 *     if (e.getCause() != null) {
 *         logger.debug("Underlying cause: " + e.getCause());
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Serializer
 */
public class SerializationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String serializedData;
    private final Class<?> targetType;

    /**
     * Creates a new SerializationException with a message.
     *
     * @param message the error message
     */
    public SerializationException(@NotNull String message) {
        super(message);
        this.serializedData = null;
        this.targetType = null;
    }

    /**
     * Creates a new SerializationException with a message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public SerializationException(@NotNull String message, @Nullable Throwable cause) {
        super(message, cause);
        this.serializedData = null;
        this.targetType = null;
    }

    /**
     * Creates a new SerializationException with full context.
     *
     * @param message        the error message
     * @param cause          the underlying cause
     * @param serializedData the data that failed to deserialize (may be truncated)
     * @param targetType     the target type that was being deserialized to
     */
    public SerializationException(@NotNull String message, @Nullable Throwable cause,
                                  @Nullable String serializedData, @Nullable Class<?> targetType) {
        super(message, cause);
        this.serializedData = truncate(serializedData, 500);
        this.targetType = targetType;
    }

    /**
     * Creates an exception for a serialization failure.
     *
     * @param type  the type being serialized
     * @param cause the underlying cause
     * @return a new SerializationException
     * @since 1.0.0
     */
    @NotNull
    public static SerializationException serializationFailed(@NotNull Class<?> type, @NotNull Throwable cause) {
        return new SerializationException(
                "Failed to serialize " + type.getSimpleName() + ": " + cause.getMessage(),
                cause, null, type);
    }

    /**
     * Creates an exception for a deserialization failure.
     *
     * @param type  the target type
     * @param data  the data that failed to deserialize
     * @param cause the underlying cause
     * @return a new SerializationException
     * @since 1.0.0
     */
    @NotNull
    public static SerializationException deserializationFailed(@NotNull Class<?> type,
                                                               @NotNull String data,
                                                               @NotNull Throwable cause) {
        return new SerializationException(
                "Failed to deserialize to " + type.getSimpleName() + ": " + cause.getMessage(),
                cause, data, type);
    }

    /**
     * Creates an exception for an unsupported format.
     *
     * @param type   the type being serialized/deserialized
     * @param format the unsupported format
     * @return a new SerializationException
     * @since 1.0.0
     */
    @NotNull
    public static SerializationException unsupportedFormat(@NotNull Class<?> type,
                                                           @NotNull SerializationContext.Format format) {
        return new SerializationException(
                "Serializer for " + type.getSimpleName() + " does not support format: " + format,
                null, null, type);
    }

    /**
     * Creates an exception for invalid data format.
     *
     * @param type    the target type
     * @param data    the invalid data
     * @param message the error message
     * @return a new SerializationException
     * @since 1.0.0
     */
    @NotNull
    public static SerializationException invalidFormat(@NotNull Class<?> type,
                                                       @NotNull String data,
                                                       @NotNull String message) {
        return new SerializationException(
                "Invalid format for " + type.getSimpleName() + ": " + message,
                null, data, type);
    }

    /**
     * Creates an exception for version incompatibility.
     *
     * @param type            the type being deserialized
     * @param dataVersion     the version in the data
     * @param supportedVersion the maximum supported version
     * @return a new SerializationException
     * @since 1.0.0
     */
    @NotNull
    public static SerializationException versionIncompatible(@NotNull Class<?> type,
                                                             @NotNull SchemaVersion dataVersion,
                                                             @NotNull SchemaVersion supportedVersion) {
        return new SerializationException(
                "Data version " + dataVersion + " is not compatible with serializer version " +
                supportedVersion + " for type " + type.getSimpleName(),
                null, null, type);
    }

    /**
     * Returns the serialized data that caused the error (may be truncated).
     *
     * @return the serialized data, or null if not available
     * @since 1.0.0
     */
    @Nullable
    public String getSerializedData() {
        return serializedData;
    }

    /**
     * Returns the target type that was being serialized/deserialized.
     *
     * @return the target type, or null if not available
     * @since 1.0.0
     */
    @Nullable
    public Class<?> getTargetType() {
        return targetType;
    }

    private static String truncate(String data, int maxLength) {
        if (data == null || data.length() <= maxLength) {
            return data;
        }
        return data.substring(0, maxLength) + "... (truncated)";
    }
}
