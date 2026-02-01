/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.mongo;

import sh.pcx.unified.world.UnifiedLocation;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.jetbrains.annotations.NotNull;

/**
 * MongoDB codec for serializing and deserializing {@link UnifiedLocation}.
 *
 * <p>This codec stores locations as embedded documents with the following structure:
 * <pre>
 * {
 *   "world": "world_name",
 *   "x": 100.5,
 *   "y": 64.0,
 *   "z": -200.25,
 *   "yaw": 90.0,
 *   "pitch": 0.0
 * }
 * </pre>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Register the codec
 * CodecRegistry registry = CodecRegistryBuilder.create()
 *     .addCodec(new LocationCodec())
 *     .build();
 *
 * // Store a location
 * Document player = new Document("uuid", uuid.toString())
 *     .append("home", new UnifiedLocation(world, 100, 64, -200));
 *
 * // Query by location
 * mongoService.find("players", Filters.and(
 *     Filters.gte("home.x", 0),
 *     Filters.lt("home.x", 100)
 * ));
 * }</pre>
 *
 * <h2>World Reference</h2>
 * <p>The world is stored by name only. When deserializing, the world reference
 * will be null and must be resolved by the application using the world name.
 * Use {@link UnifiedLocation#withWorld(sh.pcx.unified.world.UnifiedWorld)}
 * to set the world reference after loading.
 *
 * <h2>Thread Safety</h2>
 * <p>This codec is stateless and thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedLocation
 * @see CodecRegistryBuilder
 */
public class LocationCodec implements Codec<UnifiedLocation> {

    /**
     * Field name for the world.
     */
    public static final String FIELD_WORLD = "world";

    /**
     * Field name for the X coordinate.
     */
    public static final String FIELD_X = "x";

    /**
     * Field name for the Y coordinate.
     */
    public static final String FIELD_Y = "y";

    /**
     * Field name for the Z coordinate.
     */
    public static final String FIELD_Z = "z";

    /**
     * Field name for the yaw rotation.
     */
    public static final String FIELD_YAW = "yaw";

    /**
     * Field name for the pitch rotation.
     */
    public static final String FIELD_PITCH = "pitch";

    /**
     * Creates a new location codec.
     *
     * @since 1.0.0
     */
    public LocationCodec() {
        // Default constructor
    }

    /**
     * Returns the encoder class.
     *
     * @return UnifiedLocation.class
     * @since 1.0.0
     */
    @Override
    @NotNull
    public Class<UnifiedLocation> getEncoderClass() {
        return UnifiedLocation.class;
    }

    /**
     * Encodes a UnifiedLocation to BSON.
     *
     * @param writer         the BSON writer
     * @param value          the location value
     * @param encoderContext the encoder context
     * @since 1.0.0
     */
    @Override
    public void encode(
            @NotNull BsonWriter writer,
            @NotNull UnifiedLocation value,
            @NotNull EncoderContext encoderContext
    ) {
        writer.writeStartDocument();

        // Write world name (if present)
        if (value.world() != null) {
            writer.writeString(FIELD_WORLD, value.world().getName());
        } else {
            writer.writeNull(FIELD_WORLD);
        }

        // Write coordinates
        writer.writeDouble(FIELD_X, value.x());
        writer.writeDouble(FIELD_Y, value.y());
        writer.writeDouble(FIELD_Z, value.z());

        // Write rotation
        writer.writeDouble(FIELD_YAW, value.yaw());
        writer.writeDouble(FIELD_PITCH, value.pitch());

        writer.writeEndDocument();
    }

    /**
     * Decodes a UnifiedLocation from BSON.
     *
     * <p>Note: The world reference will be null after decoding. Use the
     * world name to resolve the actual world reference.
     *
     * @param reader         the BSON reader
     * @param decoderContext the decoder context
     * @return the decoded location (with null world reference)
     * @since 1.0.0
     */
    @Override
    @NotNull
    public UnifiedLocation decode(
            @NotNull BsonReader reader,
            @NotNull DecoderContext decoderContext
    ) {
        reader.readStartDocument();

        String worldName = null;
        double x = 0, y = 0, z = 0;
        float yaw = 0, pitch = 0;

        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String fieldName = reader.readName();

            switch (fieldName) {
                case FIELD_WORLD -> {
                    if (reader.getCurrentBsonType() == BsonType.NULL) {
                        reader.readNull();
                    } else {
                        worldName = reader.readString();
                    }
                }
                case FIELD_X -> x = readNumber(reader);
                case FIELD_Y -> y = readNumber(reader);
                case FIELD_Z -> z = readNumber(reader);
                case FIELD_YAW -> yaw = (float) readNumber(reader);
                case FIELD_PITCH -> pitch = (float) readNumber(reader);
                default -> reader.skipValue(); // Ignore unknown fields
            }
        }

        reader.readEndDocument();

        // Create location with null world (to be resolved later)
        // The world name is stored but we can't resolve the actual world here
        return new UnifiedLocation(null, x, y, z, yaw, pitch);
    }

    /**
     * Reads a number from the BSON reader, handling both int32 and double.
     *
     * @param reader the BSON reader
     * @return the number as a double
     */
    private double readNumber(@NotNull BsonReader reader) {
        return switch (reader.getCurrentBsonType()) {
            case INT32 -> reader.readInt32();
            case INT64 -> reader.readInt64();
            case DOUBLE -> reader.readDouble();
            default -> throw new IllegalArgumentException(
                    "Expected number but got: " + reader.getCurrentBsonType()
            );
        };
    }

    /**
     * Gets the world name from a serialized location document.
     *
     * <p>This is a utility method for extracting the world name from
     * a location that was serialized with this codec, useful for
     * resolving the world reference after loading.
     *
     * @param document the location document
     * @return the world name, or null if not present
     * @since 1.0.0
     */
    public static String getWorldName(@NotNull org.bson.Document document) {
        return document.getString(FIELD_WORLD);
    }
}
