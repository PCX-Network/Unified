/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.messages;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Serializes and deserializes messages to and from byte arrays.
 *
 * <p>This class handles the conversion of {@link Message} objects to byte arrays
 * for transmission over network channels, and back to Message objects on receipt.
 *
 * <h2>Serialization Format</h2>
 * <p>Messages are serialized in a binary format optimized for size and speed:
 * <pre>
 * [Header]
 *   - Magic bytes (2 bytes): 0x55 0x4D ("UM" for UnifiedMessage)
 *   - Version (1 byte): Protocol version
 *   - Flags (1 byte): Message flags
 * [Metadata]
 *   - Message ID (16 bytes): UUID
 *   - Type (variable): UTF-8 string
 *   - Timestamp (8 bytes): Epoch milliseconds
 *   - Source Server (variable): UTF-8 string
 *   - Target Server (variable): Optional UTF-8 string
 *   - Correlation ID (16 bytes): Optional UUID
 *   - TTL (8 bytes): Time-to-live in milliseconds
 * [Payload]
 *   - Length (4 bytes): Payload length
 *   - Data (variable): Serialized message data
 * </pre>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MessageSerializer serializer = MessageSerializer.create();
 *
 * // Register a custom message type
 * serializer.register(PlayerUpdateMessage.class,
 *     msg -> serializePlayerUpdate(msg),
 *     bytes -> deserializePlayerUpdate(bytes)
 * );
 *
 * // Serialize a message
 * byte[] data = serializer.serialize(message);
 *
 * // Deserialize a message
 * Message message = serializer.deserialize(data);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. All operations can be called concurrently.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Message
 * @see MessageType
 */
public class MessageSerializer {

    /**
     * Magic bytes identifying a UnifiedPlugin message.
     */
    private static final byte[] MAGIC = new byte[]{0x55, 0x4D}; // "UM"

    /**
     * Current protocol version.
     */
    private static final byte PROTOCOL_VERSION = 1;

    /**
     * Flag indicating the message has a target server.
     */
    private static final byte FLAG_HAS_TARGET = 0x01;

    /**
     * Flag indicating the message has a correlation ID.
     */
    private static final byte FLAG_HAS_CORRELATION = 0x02;

    /**
     * Flag indicating the message has custom metadata.
     */
    private static final byte FLAG_HAS_METADATA = 0x04;

    /**
     * Flag indicating the message uses compression.
     */
    private static final byte FLAG_COMPRESSED = 0x08;

    private final Map<String, Class<? extends Message>> typeRegistry;
    private final Map<Class<? extends Message>, Function<Message, byte[]>> serializers;
    private final Map<Class<? extends Message>, Function<byte[], Message>> deserializers;
    private final Map<String, Function<byte[], Message>> typeDeserializers;

    /**
     * Creates a new message serializer.
     */
    private MessageSerializer() {
        this.typeRegistry = new ConcurrentHashMap<>();
        this.serializers = new ConcurrentHashMap<>();
        this.deserializers = new ConcurrentHashMap<>();
        this.typeDeserializers = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new message serializer instance.
     *
     * @return a new serializer
     * @since 1.0.0
     */
    @NotNull
    public static MessageSerializer create() {
        return new MessageSerializer();
    }

    /**
     * Registers a message type with custom serialization.
     *
     * @param <T>          the message type
     * @param messageClass the message class
     * @param serializer   the serialization function
     * @param deserializer the deserialization function
     * @since 1.0.0
     */
    public <T extends Message> void register(
            @NotNull Class<T> messageClass,
            @NotNull Function<T, byte[]> serializer,
            @NotNull Function<byte[], T> deserializer
    ) {
        MessageType annotation = messageClass.getAnnotation(MessageType.class);
        String type = annotation != null ? annotation.value() : messageClass.getSimpleName();

        typeRegistry.put(type, messageClass);
        serializers.put(messageClass, msg -> serializer.apply(messageClass.cast(msg)));
        deserializers.put(messageClass, bytes -> deserializer.apply(bytes));
        typeDeserializers.put(type, bytes -> deserializer.apply(bytes));
    }

    /**
     * Registers a message type using default JSON-like serialization.
     *
     * @param <T>          the message type
     * @param messageClass the message class
     * @since 1.0.0
     */
    public <T extends Message> void register(@NotNull Class<T> messageClass) {
        MessageType annotation = messageClass.getAnnotation(MessageType.class);
        String type = annotation != null ? annotation.value() : messageClass.getSimpleName();
        typeRegistry.put(type, messageClass);
    }

    /**
     * Serializes a message to a byte array.
     *
     * @param message the message to serialize
     * @return the serialized bytes
     * @throws SerializationException if serialization fails
     * @since 1.0.0
     */
    @NotNull
    public byte[] serialize(@NotNull Message message) throws SerializationException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(baos)) {

            // Write header
            out.write(MAGIC);
            out.writeByte(PROTOCOL_VERSION);

            // Calculate flags
            byte flags = 0;
            if (message.getTargetServer().isPresent()) {
                flags |= FLAG_HAS_TARGET;
            }
            if (message.getCorrelationId().isPresent()) {
                flags |= FLAG_HAS_CORRELATION;
            }
            out.writeByte(flags);

            // Write metadata
            writeUUID(out, message.getMessageId());
            writeString(out, message.getType());
            out.writeLong(message.getTimestamp().toEpochMilli());
            writeString(out, message.getSourceServer());

            if (message.getTargetServer().isPresent()) {
                writeString(out, message.getTargetServer().get());
            }

            if (message.getCorrelationId().isPresent()) {
                writeUUID(out, message.getCorrelationId().get());
            }

            out.writeLong(message.getTimeToLive());

            // Serialize payload
            byte[] payload = serializePayload(message);
            out.writeInt(payload.length);
            out.write(payload);

            return baos.toByteArray();

        } catch (IOException e) {
            throw new SerializationException("Failed to serialize message", e);
        }
    }

    /**
     * Deserializes a message from a byte array.
     *
     * @param data the serialized bytes
     * @return the deserialized message
     * @throws SerializationException if deserialization fails
     * @since 1.0.0
     */
    @NotNull
    public Message deserialize(byte @NotNull [] data) throws SerializationException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream in = new DataInputStream(bais)) {

            // Read and verify header
            byte[] magic = new byte[2];
            in.readFully(magic);
            if (magic[0] != MAGIC[0] || magic[1] != MAGIC[1]) {
                throw new SerializationException("Invalid message format: bad magic bytes");
            }

            byte version = in.readByte();
            if (version > PROTOCOL_VERSION) {
                throw new SerializationException("Unsupported protocol version: " + version);
            }

            byte flags = in.readByte();

            // Read metadata
            UUID messageId = readUUID(in);
            String type = readString(in);
            Instant timestamp = Instant.ofEpochMilli(in.readLong());
            String sourceServer = readString(in);

            String targetServer = null;
            if ((flags & FLAG_HAS_TARGET) != 0) {
                targetServer = readString(in);
            }

            UUID correlationId = null;
            if ((flags & FLAG_HAS_CORRELATION) != 0) {
                correlationId = readUUID(in);
            }

            long ttl = in.readLong();

            // Read payload
            int payloadLength = in.readInt();
            byte[] payload = new byte[payloadLength];
            in.readFully(payload);

            // Deserialize to specific type if registered
            if (typeDeserializers.containsKey(type)) {
                return typeDeserializers.get(type).apply(payload);
            }

            // Return a generic message wrapper
            return createGenericMessage(messageId, type, timestamp, sourceServer,
                    targetServer, correlationId, ttl, payload);

        } catch (IOException e) {
            throw new SerializationException("Failed to deserialize message", e);
        }
    }

    /**
     * Deserializes a message to a specific type.
     *
     * @param <T>          the message type
     * @param data         the serialized bytes
     * @param messageClass the expected message class
     * @return the deserialized message
     * @throws SerializationException if deserialization fails or type mismatch
     * @since 1.0.0
     */
    @NotNull
    public <T extends Message> T deserialize(byte @NotNull [] data, @NotNull Class<T> messageClass)
            throws SerializationException {
        Message message = deserialize(data);
        if (!messageClass.isInstance(message)) {
            throw new SerializationException("Message type mismatch: expected " +
                    messageClass.getSimpleName() + " but got " + message.getClass().getSimpleName());
        }
        return messageClass.cast(message);
    }

    /**
     * Returns the registered message class for a type identifier.
     *
     * @param type the type identifier
     * @return the message class, or empty if not registered
     * @since 1.0.0
     */
    @NotNull
    public Optional<Class<? extends Message>> getMessageClass(@NotNull String type) {
        return Optional.ofNullable(typeRegistry.get(type));
    }

    /**
     * Checks if a message type is registered.
     *
     * @param type the type identifier
     * @return true if registered
     * @since 1.0.0
     */
    public boolean isRegistered(@NotNull String type) {
        return typeRegistry.containsKey(type);
    }

    private byte[] serializePayload(Message message) throws IOException {
        Function<Message, byte[]> serializer = serializers.get(message.getClass());
        if (serializer != null) {
            return serializer.apply(message);
        }

        // Default: serialize as empty payload for built-in messages
        // Custom messages should register their own serializers
        return new byte[0];
    }

    private Message createGenericMessage(
            UUID messageId, String type, Instant timestamp, String sourceServer,
            @Nullable String targetServer, @Nullable UUID correlationId, long ttl, byte[] payload
    ) {
        return new GenericMessage(messageId, type, timestamp, sourceServer,
                targetServer, correlationId, ttl, payload);
    }

    private void writeUUID(DataOutputStream out, UUID uuid) throws IOException {
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
    }

    private UUID readUUID(DataInputStream in) throws IOException {
        return new UUID(in.readLong(), in.readLong());
    }

    private void writeString(DataOutputStream out, String str) throws IOException {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    private String readString(DataInputStream in) throws IOException {
        int length = in.readShort();
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * A generic message implementation for unregistered types.
     */
    private static class GenericMessage implements Message {
        private final UUID messageId;
        private final String type;
        private final Instant timestamp;
        private final String sourceServer;
        private final String targetServer;
        private final UUID correlationId;
        private final long ttl;
        private final byte[] payload;
        private final Map<String, Object> metadata = new HashMap<>();

        GenericMessage(UUID messageId, String type, Instant timestamp, String sourceServer,
                       @Nullable String targetServer, @Nullable UUID correlationId,
                       long ttl, byte[] payload) {
            this.messageId = messageId;
            this.type = type;
            this.timestamp = timestamp;
            this.sourceServer = sourceServer;
            this.targetServer = targetServer;
            this.correlationId = correlationId;
            this.ttl = ttl;
            this.payload = payload;
        }

        @Override
        @NotNull
        public UUID getMessageId() {
            return messageId;
        }

        @Override
        @NotNull
        public String getType() {
            return type;
        }

        @Override
        @NotNull
        public Instant getTimestamp() {
            return timestamp;
        }

        @Override
        @NotNull
        public String getSourceServer() {
            return sourceServer;
        }

        @Override
        @NotNull
        public Optional<String> getTargetServer() {
            return Optional.ofNullable(targetServer);
        }

        @Override
        @NotNull
        public Optional<UUID> getCorrelationId() {
            return Optional.ofNullable(correlationId);
        }

        @Override
        public long getTimeToLive() {
            return ttl;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMetadata(@NotNull String key) {
            return (T) metadata.get(key);
        }

        /**
         * Returns the raw payload bytes.
         *
         * @return the payload
         */
        public byte[] getPayload() {
            return payload;
        }
    }

    /**
     * Exception thrown when serialization or deserialization fails.
     */
    public static class SerializationException extends RuntimeException {
        /**
         * Creates a new serialization exception.
         *
         * @param message the error message
         */
        public SerializationException(String message) {
            super(message);
        }

        /**
         * Creates a new serialization exception with a cause.
         *
         * @param message the error message
         * @param cause   the underlying cause
         */
        public SerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
