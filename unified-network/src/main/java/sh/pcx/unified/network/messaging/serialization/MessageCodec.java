/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.serialization;

import sh.pcx.unified.network.messaging.MessageWrapper;
import org.jetbrains.annotations.NotNull;

/**
 * Codec for encoding and decoding network messages.
 *
 * <p>The MessageCodec handles serialization of {@link MessageWrapper} objects
 * to byte arrays for transmission, and deserialization back to objects.
 *
 * <h2>Implementations</h2>
 * <ul>
 *   <li>{@link GsonMessageCodec} - JSON-based serialization using Gson</li>
 *   <li>{@link BinaryMessageCodec} - Compact binary format</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public interface MessageCodec {

    /**
     * Encodes a message wrapper to bytes.
     *
     * @param <T>     the payload type
     * @param wrapper the message wrapper
     * @return the encoded bytes
     * @throws CodecException if encoding fails
     * @since 1.0.0
     */
    <T> byte @NotNull [] encode(@NotNull MessageWrapper<T> wrapper) throws CodecException;

    /**
     * Decodes bytes to a message wrapper.
     *
     * @param <T>         the payload type
     * @param data        the encoded bytes
     * @param payloadType the expected payload class
     * @return the decoded message wrapper
     * @throws CodecException if decoding fails
     * @since 1.0.0
     */
    @NotNull
    <T> MessageWrapper<T> decode(byte @NotNull [] data, @NotNull Class<T> payloadType) throws CodecException;

    /**
     * Decodes bytes without knowing the payload type.
     *
     * <p>The payload type is determined from the encoded data.
     *
     * @param data the encoded bytes
     * @return the decoded message wrapper
     * @throws CodecException if decoding fails
     * @since 1.0.0
     */
    @NotNull
    MessageWrapper<?> decode(byte @NotNull [] data) throws CodecException;

    /**
     * Registers a type adapter for custom serialization.
     *
     * @param <T>     the type
     * @param type    the class
     * @param adapter the type adapter
     * @since 1.0.0
     */
    <T> void registerAdapter(@NotNull Class<T> type, @NotNull TypeAdapter<T> adapter);

    /**
     * Exception thrown when encoding or decoding fails.
     *
     * @since 1.0.0
     */
    class CodecException extends RuntimeException {

        /**
         * Creates a new codec exception.
         *
         * @param message the error message
         */
        public CodecException(String message) {
            super(message);
        }

        /**
         * Creates a new codec exception with a cause.
         *
         * @param message the error message
         * @param cause   the underlying cause
         */
        public CodecException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Adapter for custom type serialization.
     *
     * @param <T> the type
     * @since 1.0.0
     */
    interface TypeAdapter<T> {

        /**
         * Serializes a value.
         *
         * @param value the value
         * @return serialized representation
         * @since 1.0.0
         */
        @NotNull
        Object serialize(@NotNull T value);

        /**
         * Deserializes a value.
         *
         * @param data the serialized data
         * @return the deserialized value
         * @since 1.0.0
         */
        @NotNull
        T deserialize(@NotNull Object data);
    }
}
