/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.fake.block;

import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Service for sending client-side fake blocks.
 *
 * <p>Fake blocks appear only on the client and don't affect the actual world.
 * They are useful for previewing builds, highlighting areas, or creating
 * visual effects.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private FakeBlockService fakeBlocks;
 *
 * // Send a single fake block
 * fakeBlocks.send(player, location, Material.DIAMOND_BLOCK);
 *
 * // Send multiple fake blocks
 * fakeBlocks.sendMultiple(player, Map.of(
 *     loc1, Material.GOLD_BLOCK,
 *     loc2, Material.EMERALD_BLOCK,
 *     loc3, Material.IRON_BLOCK
 * ));
 *
 * // Clear fake block (restore real block)
 * fakeBlocks.clear(player, location);
 *
 * // Send block with data
 * BlockData stairs = Material.OAK_STAIRS.createBlockData();
 * ((Stairs) stairs).setFacing(BlockFace.NORTH);
 * ((Stairs) stairs).setHalf(Bisected.Half.TOP);
 * fakeBlocks.send(player, location, stairs);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This service is thread-safe. All operations can be called from any thread.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public interface FakeBlockService extends Service {

    /**
     * Sends a fake block to a player.
     *
     * @param player   the player
     * @param location the block location
     * @param material the block material
     * @since 1.0.0
     */
    void send(@NotNull Object player, @NotNull Object location, @NotNull Object material);

    /**
     * Sends a fake block with block data to a player.
     *
     * @param player    the player
     * @param location  the block location
     * @param blockData the block data
     * @since 1.0.0
     */
    void sendData(@NotNull Object player, @NotNull Object location, @NotNull Object blockData);

    /**
     * Sends multiple fake blocks to a player.
     *
     * @param player the player
     * @param blocks map of locations to materials
     * @since 1.0.0
     */
    void sendMultiple(@NotNull Object player, @NotNull Map<?, ?> blocks);

    /**
     * Sends multiple fake blocks with block data to a player.
     *
     * @param player the player
     * @param blocks map of locations to block data
     * @since 1.0.0
     */
    void sendMultipleData(@NotNull Object player, @NotNull Map<?, ?> blocks);

    /**
     * Sends fake blocks to multiple players.
     *
     * @param players  the players
     * @param location the block location
     * @param material the block material
     * @since 1.0.0
     */
    void broadcast(@NotNull Collection<?> players, @NotNull Object location, @NotNull Object material);

    /**
     * Clears a fake block, restoring the real block.
     *
     * @param player   the player
     * @param location the block location
     * @since 1.0.0
     */
    void clear(@NotNull Object player, @NotNull Object location);

    /**
     * Clears multiple fake blocks.
     *
     * @param player    the player
     * @param locations the block locations
     * @since 1.0.0
     */
    void clearMultiple(@NotNull Object player, @NotNull Collection<?> locations);

    /**
     * Clears all fake blocks for a player.
     *
     * @param player the player
     * @since 1.0.0
     */
    void clearAll(@NotNull Object player);

    /**
     * Clears all fake blocks for a player by UUID.
     *
     * @param playerId the player's UUID
     * @since 1.0.0
     */
    void clearAll(@NotNull UUID playerId);

    /**
     * Gets all fake block locations for a player.
     *
     * @param playerId the player's UUID
     * @return the fake block locations
     * @since 1.0.0
     */
    @NotNull
    Collection<?> getFakeBlockLocations(@NotNull UUID playerId);

    /**
     * Checks if a location has a fake block for a player.
     *
     * @param playerId the player's UUID
     * @param location the block location
     * @return true if there is a fake block
     * @since 1.0.0
     */
    boolean hasFakeBlock(@NotNull UUID playerId, @NotNull Object location);

    /**
     * Gets the fake block material at a location for a player.
     *
     * @param <T>      the material type
     * @param playerId the player's UUID
     * @param location the block location
     * @return the fake material, or null if no fake block
     * @since 1.0.0
     */
    @Nullable
    <T> T getFakeBlock(@NotNull UUID playerId, @NotNull Object location);

    /**
     * Creates a region of fake blocks.
     *
     * @param player   the player
     * @param corner1  first corner
     * @param corner2  second corner
     * @param material the fill material
     * @since 1.0.0
     */
    void fillRegion(@NotNull Object player, @NotNull Object corner1, @NotNull Object corner2, @NotNull Object material);

    /**
     * Creates a hollow box of fake blocks.
     *
     * @param player   the player
     * @param corner1  first corner
     * @param corner2  second corner
     * @param material the box material
     * @since 1.0.0
     */
    void createBox(@NotNull Object player, @NotNull Object corner1, @NotNull Object corner2, @NotNull Object material);

    /**
     * Creates a sphere of fake blocks.
     *
     * @param player   the player
     * @param center   the center location
     * @param radius   the radius
     * @param material the sphere material
     * @param hollow   whether the sphere is hollow
     * @since 1.0.0
     */
    void createSphere(@NotNull Object player, @NotNull Object center, double radius, @NotNull Object material, boolean hollow);

    /**
     * Creates a line of fake blocks.
     *
     * @param player   the player
     * @param start    the start location
     * @param end      the end location
     * @param material the line material
     * @since 1.0.0
     */
    void createLine(@NotNull Object player, @NotNull Object start, @NotNull Object end, @NotNull Object material);

    /**
     * Schedules a fake block to be cleared after a delay.
     *
     * @param player     the player
     * @param location   the block location
     * @param delayTicks the delay in ticks
     * @since 1.0.0
     */
    void scheduleRestore(@NotNull Object player, @NotNull Object location, long delayTicks);

    /**
     * Sends a block break animation.
     *
     * @param player   the player
     * @param location the block location
     * @param stage    the break stage (0-9)
     * @since 1.0.0
     */
    void sendBreakAnimation(@NotNull Object player, @NotNull Object location, int stage);

    /**
     * Clears the block break animation.
     *
     * @param player   the player
     * @param location the block location
     * @since 1.0.0
     */
    void clearBreakAnimation(@NotNull Object player, @NotNull Object location);

    /**
     * Sends a block action packet (doors, chests, etc).
     *
     * @param player   the player
     * @param location the block location
     * @param action   the action ID
     * @param param    the action parameter
     * @since 1.0.0
     */
    void sendBlockAction(@NotNull Object player, @NotNull Object location, int action, int param);
}
