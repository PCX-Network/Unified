/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.messaging.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import sh.pcx.unified.messaging.Message;
import sh.pcx.unified.network.messaging.MessageWrapper;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * JSON-based message codec using Gson.
 *
 * <p>This codec serializes messages to JSON format. It supports compression
 * for large payloads and handles common types like UUID and Instant.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class GsonMessageCodec implements MessageCodec {

    private static final int COMPRESSION_THRESHOLD = 1024; // bytes
    private static final byte FLAG_COMPRESSED = 0x01;

    private final Gson gson;
    private final Map<Class<?>, MessageCodec.TypeAdapter<?>> customAdapters;
    private final Map<String, Class<?>> typeRegistry;

    /**
     * Creates a new Gson codec with default settings.
     */
    public GsonMessageCodec() {
        this.customAdapters = new ConcurrentHashMap<>();
        this.typeRegistry = new ConcurrentHashMap<>();
        this.gson = createGson();
    }

    /**
     * Creates a codec with a custom Gson instance.
     *
     * @param gson the Gson instance
     */
    public GsonMessageCodec(@NotNull Gson gson) {
        this.customAdapters = new ConcurrentHashMap<>();
        this.typeRegistry = new ConcurrentHashMap<>();
        this.gson = gson;
    }

    @Override
    public <T> byte @NotNull [] encode(@NotNull MessageWrapper<T> wrapper) throws CodecException {
        try {
            // Register the payload type if not already registered
            registerType(wrapper.payloadClass());

            // Create envelope with type information
            JsonObject envelope = new JsonObject();
            envelope.addProperty("messageId", wrapper.messageId().toString());
            envelope.addProperty("channel", wrapper.channel());
            envelope.addProperty("type", wrapper.type());
            envelope.addProperty("timestamp", wrapper.timestamp().toEpochMilli());
            envelope.addProperty("sourceServer", wrapper.sourceServer());
            wrapper.targetServer().ifPresent(t -> envelope.addProperty("targetServer", t));
            wrapper.correlationId().ifPresent(c -> envelope.addProperty("correlationId", c.toString()));
            envelope.addProperty("ttl", wrapper.ttl());
            envelope.addProperty("payloadClass", wrapper.payloadClass().getName());
            envelope.add("payload", gson.toJsonTree(wrapper.payload()));

            String json = gson.toJson(envelope);
            byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

            // Compress if large
            if (jsonBytes.length > COMPRESSION_THRESHOLD) {
                byte[] compressed = compress(jsonBytes);
                byte[] result = new byte[compressed.length + 1];
                result[0] = FLAG_COMPRESSED;
                System.arraycopy(compressed, 0, result, 1, compressed.length);
                return result;
            }

            byte[] result = new byte[jsonBytes.length + 1];
            result[0] = 0; // Not compressed
            System.arraycopy(jsonBytes, 0, result, 1, jsonBytes.length);
            return result;

        } catch (Exception e) {
            throw new CodecException("Failed to encode message", e);
        }
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> MessageWrapper<T> decode(byte @NotNull [] data, @NotNull Class<T> payloadType) throws CodecException {
        try {
            byte flags = data[0];
            byte[] jsonBytes = new byte[data.length - 1];
            System.arraycopy(data, 1, jsonBytes, 0, jsonBytes.length);

            if ((flags & FLAG_COMPRESSED) != 0) {
                jsonBytes = decompress(jsonBytes);
            }

            String json = new String(jsonBytes, StandardCharsets.UTF_8);
            JsonObject envelope = gson.fromJson(json, JsonObject.class);

            UUID messageId = UUID.fromString(envelope.get("messageId").getAsString());
            String channel = envelope.get("channel").getAsString();
            String type = envelope.get("type").getAsString();
            Instant timestamp = Instant.ofEpochMilli(envelope.get("timestamp").getAsLong());
            String sourceServer = envelope.get("sourceServer").getAsString();
            String targetServer = envelope.has("targetServer") ?
                    envelope.get("targetServer").getAsString() : null;
            UUID correlationId = envelope.has("correlationId") ?
                    UUID.fromString(envelope.get("correlationId").getAsString()) : null;
            long ttl = envelope.get("ttl").getAsLong();

            T payload = gson.fromJson(envelope.get("payload"), payloadType);

            return createWrapper(
                    messageId, channel, type, timestamp, sourceServer,
                    targetServer, correlationId, ttl, payload, payloadType
            );

        } catch (Exception e) {
            throw new CodecException("Failed to decode message", e);
        }
    }

    @Override
    @NotNull
    public MessageWrapper<?> decode(byte @NotNull [] data) throws CodecException {
        try {
            byte flags = data[0];
            byte[] jsonBytes = new byte[data.length - 1];
            System.arraycopy(data, 1, jsonBytes, 0, jsonBytes.length);

            if ((flags & FLAG_COMPRESSED) != 0) {
                jsonBytes = decompress(jsonBytes);
            }

            String json = new String(jsonBytes, StandardCharsets.UTF_8);
            JsonObject envelope = gson.fromJson(json, JsonObject.class);

            String payloadClassName = envelope.get("payloadClass").getAsString();
            Class<?> payloadClass = Class.forName(payloadClassName);

            return decode(data, payloadClass);

        } catch (ClassNotFoundException e) {
            throw new CodecException("Unknown payload class", e);
        } catch (Exception e) {
            throw new CodecException("Failed to decode message", e);
        }
    }

    @Override
    public <T> void registerAdapter(@NotNull Class<T> type, @NotNull MessageCodec.TypeAdapter<T> adapter) {
        customAdapters.put(type, adapter);
    }

    /**
     * Registers a message type for decoding.
     *
     * @param type the message class
     */
    public void registerType(@NotNull Class<?> type) {
        Message annotation = type.getAnnotation(Message.class);
        if (annotation != null) {
            String typeName = annotation.value().isEmpty() ? type.getSimpleName() : annotation.value();
            typeRegistry.put(typeName, type);
        }
        typeRegistry.put(type.getName(), type);
    }

    private Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(UUID.class, new UuidAdapter())
                .registerTypeAdapter(Instant.class, new InstantAdapter())
                .registerTypeAdapterFactory(new RecordTypeAdapterFactory())
                .serializeNulls()
                .create();
    }

    @SuppressWarnings("unchecked")
    private <T> MessageWrapper<T> createWrapper(
            UUID messageId, String channel, String type, Instant timestamp,
            String sourceServer, String targetServer, UUID correlationId,
            long ttl, Object payload, Class<?> payloadClass
    ) {
        // Use reflection to create a wrapper with all fields
        // This is a simplified version - real implementation would use a builder
        return (MessageWrapper<T>) MessageWrapper.wrap(payload, sourceServer)
                .withTarget(targetServer)
                .withCorrelation(correlationId)
                .withTtl(ttl);
    }

    private byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(data);
        }
        return baos.toByteArray();
    }

    private byte[] decompress(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPInputStream gzip = new GZIPInputStream(bais)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzip.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
        }
        return baos.toByteArray();
    }

    /**
     * Gson adapter for UUID.
     */
    private static class UuidAdapter implements JsonSerializer<UUID>, JsonDeserializer<UUID> {
        @Override
        public JsonElement serialize(UUID src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src.toString());
        }

        @Override
        public UUID deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return UUID.fromString(json.getAsString());
        }
    }

    /**
     * Gson adapter for Instant.
     */
    private static class InstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src.toEpochMilli());
        }

        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return Instant.ofEpochMilli(json.getAsLong());
        }
    }

    /**
     * Type adapter factory for Java records.
     */
    private static class RecordTypeAdapterFactory implements TypeAdapterFactory {
        @Override
        public <T> com.google.gson.TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            @SuppressWarnings("unchecked")
            Class<T> rawType = (Class<T>) type.getRawType();
            if (!rawType.isRecord()) {
                return null;
            }
            return new RecordTypeAdapter<>(gson, rawType);
        }
    }

    /**
     * Type adapter for Java records.
     */
    private static class RecordTypeAdapter<T> extends com.google.gson.TypeAdapter<T> {
        private final Gson gson;
        private final Class<T> recordClass;

        RecordTypeAdapter(Gson gson, Class<T> recordClass) {
            this.gson = gson;
            this.recordClass = recordClass;
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.beginObject();
            for (var component : recordClass.getRecordComponents()) {
                out.name(component.getName());
                try {
                    Object fieldValue = component.getAccessor().invoke(value);
                    gson.toJson(fieldValue, component.getGenericType(), out);
                } catch (Exception e) {
                    throw new IOException("Failed to serialize record component", e);
                }
            }
            out.endObject();
        }

        @Override
        @SuppressWarnings("unchecked")
        public T read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            var components = recordClass.getRecordComponents();
            Object[] args = new Object[components.length];
            Class<?>[] types = new Class[components.length];

            for (int i = 0; i < components.length; i++) {
                types[i] = components[i].getType();
            }

            in.beginObject();
            while (in.hasNext()) {
                String name = in.nextName();
                for (int i = 0; i < components.length; i++) {
                    if (components[i].getName().equals(name)) {
                        args[i] = gson.fromJson(in, components[i].getGenericType());
                        break;
                    }
                }
            }
            in.endObject();

            try {
                var constructor = recordClass.getDeclaredConstructor(types);
                return constructor.newInstance(args);
            } catch (Exception e) {
                throw new IOException("Failed to create record instance", e);
            }
        }
    }
}
