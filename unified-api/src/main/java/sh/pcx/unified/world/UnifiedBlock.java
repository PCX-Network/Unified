/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world;

import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

/**
 * Platform-agnostic interface representing a block in a Minecraft world.
 *
 * <p>This interface provides access to block properties, state, and data.
 * It abstracts the differences between Bukkit's Block and Sponge's BlockState.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get a block from the world
 * UnifiedBlock block = world.getBlockAt(100, 64, 200);
 *
 * // Check block type
 * String type = block.getType();
 * if (type.equals("minecraft:diamond_ore")) {
 *     // Handle diamond ore
 * }
 *
 * // Change block type
 * block.setType("minecraft:stone");
 *
 * // Get block data
 * Optional<String> facing = block.getProperty("facing");
 *
 * // Get relative blocks
 * UnifiedBlock above = block.getRelative(BlockFace.UP);
 * UnifiedBlock north = block.getRelative(BlockFace.NORTH);
 *
 * // Check block properties
 * if (block.isSolid() && !block.isLiquid()) {
 *     // Solid non-liquid block
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Block operations should be performed on the appropriate thread.
 * Read operations are generally safe, but modifications require main thread
 * or region thread (Folia).
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedWorld
 * @see UnifiedLocation
 */
public interface UnifiedBlock {

    /**
     * Returns the location of this block.
     *
     * @return the block's location
     * @since 1.0.0
     */
    @NotNull
    UnifiedLocation getLocation();

    /**
     * Returns the world this block is in.
     *
     * @return the block's world
     * @since 1.0.0
     */
    @NotNull
    UnifiedWorld getWorld();

    /**
     * Returns the X coordinate of this block.
     *
     * @return the x coordinate
     * @since 1.0.0
     */
    int getX();

    /**
     * Returns the Y coordinate of this block.
     *
     * @return the y coordinate
     * @since 1.0.0
     */
    int getY();

    /**
     * Returns the Z coordinate of this block.
     *
     * @return the z coordinate
     * @since 1.0.0
     */
    int getZ();

    /**
     * Returns the type of this block as a namespaced ID.
     *
     * <p>Examples: "minecraft:stone", "minecraft:oak_log", "minecraft:air"
     *
     * @return the block type ID
     * @since 1.0.0
     */
    @NotNull
    String getType();

    /**
     * Sets the type of this block.
     *
     * @param type the block type ID (e.g., "minecraft:stone")
     * @since 1.0.0
     */
    void setType(@NotNull String type);

    /**
     * Sets the type of this block with optional physics update.
     *
     * @param type   the block type ID
     * @param physics whether to apply physics updates to neighboring blocks
     * @since 1.0.0
     */
    void setType(@NotNull String type, boolean physics);

    /**
     * Checks if this block is air.
     *
     * @return true if the block is air
     * @since 1.0.0
     */
    boolean isEmpty();

    /**
     * Checks if this block is a liquid (water or lava).
     *
     * @return true if the block is a liquid
     * @since 1.0.0
     */
    boolean isLiquid();

    /**
     * Checks if this block is solid (can be walked on).
     *
     * @return true if the block is solid
     * @since 1.0.0
     */
    boolean isSolid();

    /**
     * Checks if this block is transparent (light can pass through).
     *
     * @return true if the block is transparent
     * @since 1.0.0
     */
    boolean isTransparent();

    /**
     * Checks if this block can be passed through.
     *
     * @return true if entities can pass through this block
     * @since 1.0.0
     */
    boolean isPassable();

    /**
     * Checks if this block is burnable (can catch fire).
     *
     * @return true if the block is burnable
     * @since 1.0.0
     */
    boolean isBurnable();

    /**
     * Checks if this block is replaceable (like grass or flowers).
     *
     * @return true if the block can be replaced when placing another block
     * @since 1.0.0
     */
    boolean isReplaceable();

    /**
     * Returns the light level at this block from all sources.
     *
     * @return the light level (0-15)
     * @since 1.0.0
     */
    int getLightLevel();

    /**
     * Returns the light level from the sky at this block.
     *
     * @return the sky light level (0-15)
     * @since 1.0.0
     */
    int getLightFromSky();

    /**
     * Returns the light level from block sources at this block.
     *
     * @return the block light level (0-15)
     * @since 1.0.0
     */
    int getLightFromBlocks();

    /**
     * Returns the block relative to this one in the specified direction.
     *
     * @param face the direction
     * @return the relative block
     * @since 1.0.0
     */
    @NotNull
    UnifiedBlock getRelative(@NotNull BlockFace face);

    /**
     * Returns the block relative to this one with the specified offset.
     *
     * @param dx the x offset
     * @param dy the y offset
     * @param dz the z offset
     * @return the relative block
     * @since 1.0.0
     */
    @NotNull
    UnifiedBlock getRelative(int dx, int dy, int dz);

    /**
     * Gets a block state property value.
     *
     * <p>Examples: "facing", "half", "waterlogged", "powered"
     *
     * @param property the property name
     * @return the property value, or empty if not present
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getProperty(@NotNull String property);

    /**
     * Sets a block state property value.
     *
     * @param property the property name
     * @param value    the property value
     * @return true if the property was set successfully
     * @since 1.0.0
     */
    boolean setProperty(@NotNull String property, @NotNull String value);

    /**
     * Returns all property names for this block type.
     *
     * @return a collection of property names
     * @since 1.0.0
     */
    @NotNull
    Collection<String> getPropertyNames();

    /**
     * Checks if this block has the specified property.
     *
     * @param property the property name
     * @return true if the block has this property
     * @since 1.0.0
     */
    boolean hasProperty(@NotNull String property);

    /**
     * Returns the chunk containing this block.
     *
     * @return the containing chunk
     * @since 1.0.0
     */
    @NotNull
    UnifiedChunk getChunk();

    /**
     * Breaks this block naturally (as if broken by a player).
     *
     * @return true if the block was broken
     * @since 1.0.0
     */
    boolean breakNaturally();

    /**
     * Breaks this block with the specified tool.
     *
     * @param tool the tool used to break the block, or null for hand
     * @return true if the block was broken
     * @since 1.0.0
     */
    boolean breakNaturally(@Nullable UnifiedItemStack tool);

    /**
     * Returns the drops that would result from breaking this block.
     *
     * @return a collection of item drops
     * @since 1.0.0
     */
    @NotNull
    Collection<UnifiedItemStack> getDrops();

    /**
     * Returns the drops that would result from breaking this block with a tool.
     *
     * @param tool the tool used to break the block
     * @return a collection of item drops
     * @since 1.0.0
     */
    @NotNull
    Collection<UnifiedItemStack> getDrops(@NotNull UnifiedItemStack tool);

    /**
     * Checks if this block is powered by redstone.
     *
     * @return true if the block is powered
     * @since 1.0.0
     */
    boolean isPowered();

    /**
     * Checks if this block is indirectly powered by redstone.
     *
     * @return true if the block is indirectly powered
     * @since 1.0.0
     */
    boolean isIndirectlyPowered();

    /**
     * Returns the redstone power level of this block.
     *
     * @return the power level (0-15)
     * @since 1.0.0
     */
    int getBlockPower();

    /**
     * Returns the underlying platform-specific block object.
     *
     * @param <T> the expected platform block type
     * @return the platform-specific block object
     * @since 1.0.0
     */
    @NotNull
    <T> T getHandle();

    /**
     * Block face directions.
     *
     * @since 1.0.0
     */
    enum BlockFace {
        /** Negative Y direction (down). */
        DOWN(0, -1, 0),
        /** Positive Y direction (up). */
        UP(0, 1, 0),
        /** Negative Z direction (north). */
        NORTH(0, 0, -1),
        /** Positive Z direction (south). */
        SOUTH(0, 0, 1),
        /** Negative X direction (west). */
        WEST(-1, 0, 0),
        /** Positive X direction (east). */
        EAST(1, 0, 0),
        /** North-east diagonal. */
        NORTH_EAST(1, 0, -1),
        /** North-west diagonal. */
        NORTH_WEST(-1, 0, -1),
        /** South-east diagonal. */
        SOUTH_EAST(1, 0, 1),
        /** South-west diagonal. */
        SOUTH_WEST(-1, 0, 1),
        /** No direction (self). */
        SELF(0, 0, 0);

        private final int modX;
        private final int modY;
        private final int modZ;

        BlockFace(int modX, int modY, int modZ) {
            this.modX = modX;
            this.modY = modY;
            this.modZ = modZ;
        }

        /**
         * Returns the X modifier for this face.
         *
         * @return the x offset
         * @since 1.0.0
         */
        public int getModX() {
            return modX;
        }

        /**
         * Returns the Y modifier for this face.
         *
         * @return the y offset
         * @since 1.0.0
         */
        public int getModY() {
            return modY;
        }

        /**
         * Returns the Z modifier for this face.
         *
         * @return the z offset
         * @since 1.0.0
         */
        public int getModZ() {
            return modZ;
        }

        /**
         * Returns the opposite face.
         *
         * @return the opposite block face
         * @since 1.0.0
         */
        @NotNull
        public BlockFace getOppositeFace() {
            return switch (this) {
                case DOWN -> UP;
                case UP -> DOWN;
                case NORTH -> SOUTH;
                case SOUTH -> NORTH;
                case WEST -> EAST;
                case EAST -> WEST;
                case NORTH_EAST -> SOUTH_WEST;
                case NORTH_WEST -> SOUTH_EAST;
                case SOUTH_EAST -> NORTH_WEST;
                case SOUTH_WEST -> NORTH_EAST;
                case SELF -> SELF;
            };
        }
    }
}
