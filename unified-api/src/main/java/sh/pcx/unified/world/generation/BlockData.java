/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Represents additional block state data beyond the basic block type.
 *
 * <p>BlockData contains properties like facing direction, waterlogged state,
 * age, and other block-specific attributes. It provides a platform-agnostic
 * way to work with block states during world generation.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Create block data with properties
 * BlockData stairsData = BlockData.builder(BlockType.OAK_STAIRS)
 *     .property("facing", "north")
 *     .property("half", "bottom")
 *     .property("shape", "straight")
 *     .property("waterlogged", false)
 *     .build();
 *
 * chunkData.setBlock(x, y, z, BlockType.OAK_STAIRS, stairsData);
 *
 * // Create directional block data
 * BlockData logData = BlockData.builder(BlockType.OAK_LOG)
 *     .property("axis", "y")
 *     .build();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see ChunkData
 * @see BlockType
 */
public interface BlockData {

    /**
     * Gets the block type for this data.
     *
     * @return the block type
     * @since 1.0.0
     */
    @NotNull
    BlockType getBlockType();

    /**
     * Gets all properties of this block data.
     *
     * @return an unmodifiable map of property names to values
     * @since 1.0.0
     */
    @NotNull
    Map<String, Object> getProperties();

    /**
     * Gets a property value.
     *
     * @param property the property name
     * @return the property value, or empty if not set
     * @since 1.0.0
     */
    @NotNull
    Optional<Object> getProperty(@NotNull String property);

    /**
     * Gets a string property value.
     *
     * @param property     the property name
     * @param defaultValue the default value
     * @return the property value, or the default
     * @since 1.0.0
     */
    @NotNull
    String getString(@NotNull String property, @NotNull String defaultValue);

    /**
     * Gets an integer property value.
     *
     * @param property     the property name
     * @param defaultValue the default value
     * @return the property value, or the default
     * @since 1.0.0
     */
    int getInt(@NotNull String property, int defaultValue);

    /**
     * Gets a boolean property value.
     *
     * @param property     the property name
     * @param defaultValue the default value
     * @return the property value, or the default
     * @since 1.0.0
     */
    boolean getBoolean(@NotNull String property, boolean defaultValue);

    /**
     * Creates a copy of this block data with a modified property.
     *
     * @param property the property name
     * @param value    the new value
     * @return a new BlockData with the modified property
     * @since 1.0.0
     */
    @NotNull
    BlockData withProperty(@NotNull String property, @NotNull Object value);

    /**
     * Converts this block data to a string representation.
     *
     * <p>The format is: {@code namespace:path[property=value,property=value]}
     *
     * @return the string representation
     * @since 1.0.0
     */
    @NotNull
    String asString();

    /**
     * Gets the underlying platform-specific block data object.
     *
     * @param <T> the expected platform type
     * @return the platform-specific block data
     * @since 1.0.0
     */
    @NotNull
    <T> T getHandle();

    /**
     * Creates a new builder for block data.
     *
     * @param blockType the block type
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    static Builder builder(@NotNull BlockType blockType) {
        return new BlockDataBuilder(blockType);
    }

    /**
     * Parses block data from a string representation.
     *
     * @param data the string representation
     * @return the parsed block data, or null if invalid
     * @since 1.0.0
     */
    @Nullable
    static BlockData parse(@NotNull String data) {
        return BlockDataBuilder.parse(data);
    }

    /**
     * Builder for creating BlockData instances.
     *
     * @since 1.0.0
     */
    interface Builder {

        /**
         * Sets a string property.
         *
         * @param property the property name
         * @param value    the value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder property(@NotNull String property, @NotNull String value);

        /**
         * Sets an integer property.
         *
         * @param property the property name
         * @param value    the value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder property(@NotNull String property, int value);

        /**
         * Sets a boolean property.
         *
         * @param property the property name
         * @param value    the value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder property(@NotNull String property, boolean value);

        /**
         * Sets the facing direction.
         *
         * @param direction the direction (north, south, east, west, up, down)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder facing(@NotNull String direction);

        /**
         * Sets the axis for logs and pillars.
         *
         * @param axis the axis (x, y, z)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder axis(@NotNull String axis);

        /**
         * Sets the waterlogged state.
         *
         * @param waterlogged true if waterlogged
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder waterlogged(boolean waterlogged);

        /**
         * Sets the half for slabs and stairs.
         *
         * @param half the half (top, bottom)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder half(@NotNull String half);

        /**
         * Sets the age property for crops and plants.
         *
         * @param age the age value
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder age(int age);

        /**
         * Sets the power level for redstone.
         *
         * @param power the power level (0-15)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder power(int power);

        /**
         * Builds the block data.
         *
         * @return the built block data
         * @since 1.0.0
         */
        @NotNull
        BlockData build();
    }
}
