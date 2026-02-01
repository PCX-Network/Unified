/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Provides information about a world during generation.
 *
 * <p>WorldInfo contains metadata about the world being generated, including
 * its name, seed, environment, and height limits. This information is
 * available to chunk generators during all generation phases.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @Override
 * public void generateNoise(WorldInfo worldInfo, Random random,
 *                           int chunkX, int chunkZ, ChunkData chunkData) {
 *     // Get world properties
 *     String name = worldInfo.getName();
 *     long seed = worldInfo.getSeed();
 *
 *     // Use height information
 *     int minY = worldInfo.getMinHeight();
 *     int maxY = worldInfo.getMaxHeight();
 *     int seaLevel = worldInfo.getSeaLevel();
 *
 *     // Check environment
 *     if (worldInfo.getEnvironment() == Environment.NETHER) {
 *         // Nether-specific generation
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see ChunkGenerator
 */
public interface WorldInfo {

    /**
     * Gets the name of the world.
     *
     * @return the world name
     * @since 1.0.0
     */
    @NotNull
    String getName();

    /**
     * Gets the unique identifier of the world.
     *
     * @return the world UUID
     * @since 1.0.0
     */
    @NotNull
    UUID getUniqueId();

    /**
     * Gets the world seed used for generation.
     *
     * @return the world seed
     * @since 1.0.0
     */
    long getSeed();

    /**
     * Gets the world environment (dimension type).
     *
     * @return the environment
     * @since 1.0.0
     */
    @NotNull
    UnifiedWorld.Environment getEnvironment();

    /**
     * Gets the minimum Y coordinate (height) for this world.
     *
     * @return the minimum height
     * @since 1.0.0
     */
    int getMinHeight();

    /**
     * Gets the maximum Y coordinate (height) for this world.
     *
     * @return the maximum height
     * @since 1.0.0
     */
    int getMaxHeight();

    /**
     * Gets the sea level for this world.
     *
     * @return the sea level Y coordinate
     * @since 1.0.0
     */
    int getSeaLevel();

    /**
     * Gets the logical height for this world.
     *
     * <p>The logical height is the maximum Y coordinate where blocks
     * can be placed by players (may differ from maxHeight).
     *
     * @return the logical height
     * @since 1.0.0
     */
    default int getLogicalHeight() {
        return getMaxHeight();
    }

    /**
     * Gets the total vertical size of this world.
     *
     * @return the height range (maxHeight - minHeight)
     * @since 1.0.0
     */
    default int getHeight() {
        return getMaxHeight() - getMinHeight();
    }

    /**
     * Checks if the given Y coordinate is valid for this world.
     *
     * @param y the Y coordinate to check
     * @return true if the coordinate is within bounds
     * @since 1.0.0
     */
    default boolean isValidHeight(int y) {
        return y >= getMinHeight() && y < getMaxHeight();
    }

    /**
     * Checks if this world has a ceiling (like the Nether).
     *
     * @return true if the world has a ceiling
     * @since 1.0.0
     */
    default boolean hasCeiling() {
        return getEnvironment() == UnifiedWorld.Environment.NETHER;
    }

    /**
     * Checks if this world has skylight.
     *
     * @return true if the world has skylight
     * @since 1.0.0
     */
    default boolean hasSkyLight() {
        return getEnvironment() != UnifiedWorld.Environment.NETHER;
    }

    /**
     * Gets the spawn X coordinate.
     *
     * @return the spawn X
     * @since 1.0.0
     */
    int getSpawnX();

    /**
     * Gets the spawn Y coordinate.
     *
     * @return the spawn Y
     * @since 1.0.0
     */
    int getSpawnY();

    /**
     * Gets the spawn Z coordinate.
     *
     * @return the spawn Z
     * @since 1.0.0
     */
    int getSpawnZ();

    /**
     * Creates a WorldInfo instance with the specified values.
     *
     * @param name        the world name
     * @param uuid        the world UUID
     * @param seed        the world seed
     * @param environment the environment
     * @param minHeight   the minimum height
     * @param maxHeight   the maximum height
     * @param seaLevel    the sea level
     * @return a new WorldInfo instance
     * @since 1.0.0
     */
    @NotNull
    static WorldInfo of(@NotNull String name, @NotNull UUID uuid, long seed,
                        @NotNull UnifiedWorld.Environment environment,
                        int minHeight, int maxHeight, int seaLevel) {
        return new WorldInfoImpl(name, uuid, seed, environment, minHeight, maxHeight, seaLevel, 0, 64, 0);
    }

    /**
     * Creates a WorldInfo for a normal world with default settings.
     *
     * @param name the world name
     * @param seed the world seed
     * @return a new WorldInfo instance
     * @since 1.0.0
     */
    @NotNull
    static WorldInfo normal(@NotNull String name, long seed) {
        return new WorldInfoImpl(name, UUID.randomUUID(), seed,
                UnifiedWorld.Environment.NORMAL, -64, 320, 63, 0, 64, 0);
    }

    /**
     * Creates a WorldInfo for a nether world with default settings.
     *
     * @param name the world name
     * @param seed the world seed
     * @return a new WorldInfo instance
     * @since 1.0.0
     */
    @NotNull
    static WorldInfo nether(@NotNull String name, long seed) {
        return new WorldInfoImpl(name, UUID.randomUUID(), seed,
                UnifiedWorld.Environment.NETHER, 0, 256, 31, 0, 64, 0);
    }

    /**
     * Creates a WorldInfo for an end world with default settings.
     *
     * @param name the world name
     * @param seed the world seed
     * @return a new WorldInfo instance
     * @since 1.0.0
     */
    @NotNull
    static WorldInfo theEnd(@NotNull String name, long seed) {
        return new WorldInfoImpl(name, UUID.randomUUID(), seed,
                UnifiedWorld.Environment.THE_END, 0, 256, 0, 0, 64, 0);
    }
}

/**
 * Default implementation of WorldInfo.
 */
record WorldInfoImpl(
        String name,
        UUID uuid,
        long seed,
        UnifiedWorld.Environment environment,
        int minHeight,
        int maxHeight,
        int seaLevel,
        int spawnX,
        int spawnY,
        int spawnZ
) implements WorldInfo {

    @Override
    @NotNull
    public String getName() {
        return name;
    }

    @Override
    @NotNull
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public long getSeed() {
        return seed;
    }

    @Override
    @NotNull
    public UnifiedWorld.Environment getEnvironment() {
        return environment;
    }

    @Override
    public int getMinHeight() {
        return minHeight;
    }

    @Override
    public int getMaxHeight() {
        return maxHeight;
    }

    @Override
    public int getSeaLevel() {
        return seaLevel;
    }

    @Override
    public int getSpawnX() {
        return spawnX;
    }

    @Override
    public int getSpawnY() {
        return spawnY;
    }

    @Override
    public int getSpawnZ() {
        return spawnZ;
    }
}
