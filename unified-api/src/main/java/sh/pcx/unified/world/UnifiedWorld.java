/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world;

import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Platform-agnostic interface representing a Minecraft world.
 *
 * <p>This interface provides access to world properties, blocks, chunks, and entities.
 * It abstracts the differences between Bukkit's World and Sponge's ServerWorld.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get a world from the server
 * Optional<UnifiedWorld> world = server.getWorld("world");
 *
 * world.ifPresent(w -> {
 *     // Get world properties
 *     String name = w.getName();
 *     UnifiedLocation spawn = w.getSpawnLocation();
 *
 *     // Get a block at a location
 *     UnifiedBlock block = w.getBlockAt(100, 64, 200);
 *
 *     // Get players in the world
 *     Collection<UnifiedPlayer> players = w.getPlayers();
 *
 *     // Set world time
 *     w.setTime(6000); // Noon
 *
 *     // Weather control
 *     w.setStorm(false);
 *     w.setThundering(false);
 * });
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Read operations are thread-safe. Write operations (setters, block modifications)
 * should be performed on the appropriate thread depending on the platform.
 * For Folia, use region-aware scheduling.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedLocation
 * @see UnifiedBlock
 * @see UnifiedChunk
 */
public interface UnifiedWorld {

    /**
     * Returns the name of this world.
     *
     * @return the world name
     * @since 1.0.0
     */
    @NotNull
    String getName();

    /**
     * Returns the unique identifier of this world.
     *
     * @return the world's UUID
     * @since 1.0.0
     */
    @NotNull
    UUID getUniqueId();

    /**
     * Returns the spawn location of this world.
     *
     * @return the spawn location
     * @since 1.0.0
     */
    @NotNull
    UnifiedLocation getSpawnLocation();

    /**
     * Sets the spawn location of this world.
     *
     * @param location the new spawn location
     * @return true if the spawn was set successfully
     * @since 1.0.0
     */
    boolean setSpawnLocation(@NotNull UnifiedLocation location);

    /**
     * Sets the spawn location of this world.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return true if the spawn was set successfully
     * @since 1.0.0
     */
    boolean setSpawnLocation(int x, int y, int z);

    /**
     * Returns the block at the specified location.
     *
     * @param location the location
     * @return the block at the location
     * @since 1.0.0
     */
    @NotNull
    UnifiedBlock getBlockAt(@NotNull UnifiedLocation location);

    /**
     * Returns the block at the specified coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return the block at the coordinates
     * @since 1.0.0
     */
    @NotNull
    UnifiedBlock getBlockAt(int x, int y, int z);

    /**
     * Returns the chunk at the specified coordinates.
     *
     * @param x the chunk x coordinate
     * @param z the chunk z coordinate
     * @return the chunk at the coordinates
     * @since 1.0.0
     */
    @NotNull
    UnifiedChunk getChunkAt(int x, int z);

    /**
     * Returns the chunk containing the specified location.
     *
     * @param location the location
     * @return the chunk containing the location
     * @since 1.0.0
     */
    @NotNull
    UnifiedChunk getChunkAt(@NotNull UnifiedLocation location);

    /**
     * Loads the chunk at the specified coordinates asynchronously.
     *
     * @param x the chunk x coordinate
     * @param z the chunk z coordinate
     * @return a future that resolves to the loaded chunk
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<UnifiedChunk> getChunkAtAsync(int x, int z);

    /**
     * Checks if the chunk at the specified coordinates is loaded.
     *
     * @param x the chunk x coordinate
     * @param z the chunk z coordinate
     * @return true if the chunk is loaded
     * @since 1.0.0
     */
    boolean isChunkLoaded(int x, int z);

    /**
     * Loads the chunk at the specified coordinates.
     *
     * @param x        the chunk x coordinate
     * @param z        the chunk z coordinate
     * @param generate whether to generate the chunk if it doesn't exist
     * @return true if the chunk was loaded successfully
     * @since 1.0.0
     */
    boolean loadChunk(int x, int z, boolean generate);

    /**
     * Unloads the chunk at the specified coordinates.
     *
     * @param x    the chunk x coordinate
     * @param z    the chunk z coordinate
     * @param save whether to save the chunk before unloading
     * @return true if the chunk was unloaded successfully
     * @since 1.0.0
     */
    boolean unloadChunk(int x, int z, boolean save);

    /**
     * Returns all players currently in this world.
     *
     * @return a collection of players in this world
     * @since 1.0.0
     */
    @NotNull
    Collection<UnifiedPlayer> getPlayers();

    /**
     * Returns the current time in this world.
     *
     * <p>Time is measured in ticks, where 0 is sunrise, 6000 is noon,
     * 12000 is sunset, and 18000 is midnight.
     *
     * @return the world time in ticks (0-24000)
     * @since 1.0.0
     */
    long getTime();

    /**
     * Sets the world time.
     *
     * @param time the time in ticks (0-24000)
     * @since 1.0.0
     */
    void setTime(long time);

    /**
     * Returns the full world time (including days passed).
     *
     * @return the full time in ticks
     * @since 1.0.0
     */
    long getFullTime();

    /**
     * Sets the full world time.
     *
     * @param time the full time in ticks
     * @since 1.0.0
     */
    void setFullTime(long time);

    /**
     * Returns the current weather duration in ticks.
     *
     * @return the weather duration
     * @since 1.0.0
     */
    int getWeatherDuration();

    /**
     * Sets the weather duration.
     *
     * @param duration the duration in ticks
     * @since 1.0.0
     */
    void setWeatherDuration(int duration);

    /**
     * Checks if it is currently storming in this world.
     *
     * @return true if there is a storm
     * @since 1.0.0
     */
    boolean hasStorm();

    /**
     * Sets whether the world is storming.
     *
     * @param storm true to start a storm
     * @since 1.0.0
     */
    void setStorm(boolean storm);

    /**
     * Checks if it is currently thundering in this world.
     *
     * @return true if there is thunder
     * @since 1.0.0
     */
    boolean isThundering();

    /**
     * Sets whether the world is thundering.
     *
     * @param thundering true to start thunder
     * @since 1.0.0
     */
    void setThundering(boolean thundering);

    /**
     * Sets the world to clear weather.
     *
     * @param duration the duration of clear weather in ticks
     * @since 1.0.0
     */
    void setClearWeatherDuration(int duration);

    /**
     * Returns the difficulty of this world.
     *
     * @return the world difficulty
     * @since 1.0.0
     */
    @NotNull
    Difficulty getDifficulty();

    /**
     * Sets the difficulty of this world.
     *
     * @param difficulty the new difficulty
     * @since 1.0.0
     */
    void setDifficulty(@NotNull Difficulty difficulty);

    /**
     * Returns the environment/dimension type of this world.
     *
     * @return the world environment
     * @since 1.0.0
     */
    @NotNull
    Environment getEnvironment();

    /**
     * Returns the seed of this world.
     *
     * @return the world seed
     * @since 1.0.0
     */
    long getSeed();

    /**
     * Checks if PvP is enabled in this world.
     *
     * @return true if PvP is enabled
     * @since 1.0.0
     */
    boolean isPvPEnabled();

    /**
     * Sets whether PvP is enabled in this world.
     *
     * @param pvp true to enable PvP
     * @since 1.0.0
     */
    void setPvPEnabled(boolean pvp);

    /**
     * Returns the minimum Y coordinate for this world.
     *
     * @return the minimum Y level
     * @since 1.0.0
     */
    int getMinHeight();

    /**
     * Returns the maximum Y coordinate for this world.
     *
     * @return the maximum Y level
     * @since 1.0.0
     */
    int getMaxHeight();

    /**
     * Returns the sea level for this world.
     *
     * @return the sea level Y coordinate
     * @since 1.0.0
     */
    int getSeaLevel();

    /**
     * Gets a game rule value.
     *
     * @param rule the game rule name
     * @return the game rule value, or empty if not set
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getGameRuleValue(@NotNull String rule);

    /**
     * Sets a game rule value.
     *
     * @param rule  the game rule name
     * @param value the value to set
     * @return true if the game rule was set successfully
     * @since 1.0.0
     */
    boolean setGameRule(@NotNull String rule, @NotNull String value);

    /**
     * Gets a boolean game rule value.
     *
     * @param rule         the game rule name
     * @param defaultValue the default value if not set
     * @return the game rule value
     * @since 1.0.0
     */
    boolean getGameRuleBoolean(@NotNull String rule, boolean defaultValue);

    /**
     * Gets an integer game rule value.
     *
     * @param rule         the game rule name
     * @param defaultValue the default value if not set
     * @return the game rule value
     * @since 1.0.0
     */
    int getGameRuleInt(@NotNull String rule, int defaultValue);

    /**
     * Creates an explosion at the specified location.
     *
     * @param location   the explosion center
     * @param power      the explosion power
     * @param setFire    whether to set blocks on fire
     * @param breakBlocks whether to break blocks
     * @return true if the explosion was created
     * @since 1.0.0
     */
    boolean createExplosion(@NotNull UnifiedLocation location, float power, boolean setFire, boolean breakBlocks);

    /**
     * Strikes lightning at the specified location.
     *
     * @param location the location to strike
     * @param effect   whether this is just a visual effect (no damage)
     * @since 1.0.0
     */
    void strikeLightning(@NotNull UnifiedLocation location, boolean effect);

    /**
     * Saves this world to disk.
     *
     * @since 1.0.0
     */
    void save();

    /**
     * Returns the underlying platform-specific world object.
     *
     * @param <T> the expected platform world type
     * @return the platform-specific world object
     * @since 1.0.0
     */
    @NotNull
    <T> T getHandle();

    /**
     * World difficulty levels.
     *
     * @since 1.0.0
     */
    enum Difficulty {
        /** No hostile mobs spawn and no hunger. */
        PEACEFUL,
        /** Hostile mobs deal reduced damage. */
        EASY,
        /** Default difficulty. */
        NORMAL,
        /** Hostile mobs deal more damage, starvation can kill. */
        HARD
    }

    /**
     * World environment/dimension types.
     *
     * @since 1.0.0
     */
    enum Environment {
        /** The normal overworld. */
        NORMAL,
        /** The nether dimension. */
        NETHER,
        /** The end dimension. */
        THE_END,
        /** A custom dimension. */
        CUSTOM
    }
}
