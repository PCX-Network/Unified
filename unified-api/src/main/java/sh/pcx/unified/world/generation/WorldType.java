/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import org.jetbrains.annotations.NotNull;

/**
 * Represents the different types of vanilla world generation.
 *
 * <p>World types determine the terrain generation style used when creating
 * a new world. Custom generators can override these settings entirely.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Create a normal world
 * worlds.create("survival")
 *     .type(WorldType.NORMAL)
 *     .create();
 *
 * // Create a flat world
 * worlds.create("flatworld")
 *     .type(WorldType.FLAT)
 *     .create();
 *
 * // Create an amplified world
 * worlds.create("epic")
 *     .type(WorldType.AMPLIFIED)
 *     .create();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
public enum WorldType {

    /**
     * Standard Minecraft world generation with realistic terrain.
     *
     * @since 1.0.0
     */
    NORMAL("minecraft:normal"),

    /**
     * Flat world with configurable layers.
     *
     * @since 1.0.0
     */
    FLAT("minecraft:flat"),

    /**
     * Large biomes world with bigger biome sizes.
     *
     * @since 1.0.0
     */
    LARGE_BIOMES("minecraft:large_biomes"),

    /**
     * Amplified world with extreme terrain heights.
     *
     * <p>Warning: This type is resource-intensive and may cause
     * performance issues on lower-end hardware.
     *
     * @since 1.0.0
     */
    AMPLIFIED("minecraft:amplified"),

    /**
     * Single biome world (requires biome specification).
     *
     * @since 1.0.0
     */
    SINGLE_BIOME_SURFACE("minecraft:single_biome_surface"),

    /**
     * Debug mode world showing all block states.
     *
     * <p>This is a special world type primarily for development
     * and testing purposes.
     *
     * @since 1.0.0
     */
    DEBUG_ALL_BLOCK_STATES("minecraft:debug_all_block_states"),

    /**
     * Custom world type using a custom generator.
     *
     * @since 1.0.0
     */
    CUSTOM("custom");

    private final String key;

    WorldType(@NotNull String key) {
        this.key = key;
    }

    /**
     * Gets the namespaced key for this world type.
     *
     * @return the namespaced key
     * @since 1.0.0
     */
    @NotNull
    public String getKey() {
        return key;
    }

    /**
     * Gets a world type by its key.
     *
     * @param key the namespaced key
     * @return the world type, or NORMAL if not found
     * @since 1.0.0
     */
    @NotNull
    public static WorldType fromKey(@NotNull String key) {
        for (WorldType type : values()) {
            if (type.key.equalsIgnoreCase(key)) {
                return type;
            }
        }
        return NORMAL;
    }

    /**
     * Checks if this world type supports custom configuration.
     *
     * @return true if configuration is supported
     * @since 1.0.0
     */
    public boolean supportsConfiguration() {
        return switch (this) {
            case FLAT, SINGLE_BIOME_SURFACE, CUSTOM -> true;
            default -> false;
        };
    }

    /**
     * Checks if this world type is performance-intensive.
     *
     * @return true if generation is resource-intensive
     * @since 1.0.0
     */
    public boolean isResourceIntensive() {
        return this == AMPLIFIED;
    }
}
