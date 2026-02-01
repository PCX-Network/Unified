/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Represents a schematic (saved structure) that can be placed in the world.
 *
 * <p>Schematics store block data, entities, and metadata for structures.
 * They support various formats including WorldEdit's .schem and Minecraft's
 * native .nbt format.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Load a schematic
 * Schematic tower = structures.loadSchematic(Path.of("plugins/MyPlugin/tower.schem"));
 *
 * // Get schematic info
 * int width = tower.getWidth();
 * int height = tower.getHeight();
 * int depth = tower.getDepth();
 *
 * // Get blocks
 * BlockType cornerBlock = tower.getBlock(0, 0, 0);
 *
 * // Place the schematic
 * tower.paste(location, StructureService.Rotation.NONE, false);
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see StructureService
 * @see Structure
 */
public interface Schematic {

    /**
     * Gets the width (X size) of this schematic.
     *
     * @return the width
     * @since 1.0.0
     */
    int getWidth();

    /**
     * Gets the height (Y size) of this schematic.
     *
     * @return the height
     * @since 1.0.0
     */
    int getHeight();

    /**
     * Gets the depth (Z size) of this schematic.
     *
     * @return the depth
     * @since 1.0.0
     */
    int getDepth();

    /**
     * Gets the total number of blocks in this schematic.
     *
     * @return the block count
     * @since 1.0.0
     */
    default int getBlockCount() {
        return getWidth() * getHeight() * getDepth();
    }

    /**
     * Gets the offset from the origin.
     *
     * @return the offset as [x, y, z]
     * @since 1.0.0
     */
    @NotNull
    int[] getOffset();

    /**
     * Gets the block type at the specified relative coordinates.
     *
     * @param x the X coordinate (0 to width-1)
     * @param y the Y coordinate (0 to height-1)
     * @param z the Z coordinate (0 to depth-1)
     * @return the block type
     * @throws IndexOutOfBoundsException if coordinates are out of bounds
     * @since 1.0.0
     */
    @NotNull
    BlockType getBlock(int x, int y, int z);

    /**
     * Gets the block data at the specified relative coordinates.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the block data, or null if none
     * @since 1.0.0
     */
    @Nullable
    BlockData getBlockData(int x, int y, int z);

    /**
     * Gets all entity data stored in this schematic.
     *
     * @return a list of entity data
     * @since 1.0.0
     */
    @NotNull
    List<EntityData> getEntities();

    /**
     * Gets the schematic metadata.
     *
     * @return the metadata map
     * @since 1.0.0
     */
    @NotNull
    Map<String, Object> getMetadata();

    /**
     * Gets a specific metadata value.
     *
     * @param key the metadata key
     * @param <T> the value type
     * @return the value, or null if not present
     * @since 1.0.0
     */
    @Nullable
    <T> T getMetadata(@NotNull String key);

    /**
     * Pastes this schematic at the specified location.
     *
     * @param location   the paste location
     * @param rotation   the rotation to apply
     * @param includeAir whether to paste air blocks
     * @since 1.0.0
     */
    void paste(@NotNull UnifiedLocation location, @NotNull StructureService.Rotation rotation,
               boolean includeAir);

    /**
     * Pastes this schematic with full options.
     *
     * @param location the paste location
     * @param options  the placement options
     * @since 1.0.0
     */
    void paste(@NotNull UnifiedLocation location, @NotNull StructureService.PlacementOptions options);

    /**
     * Creates a copy of this schematic rotated by the specified amount.
     *
     * @param rotation the rotation
     * @return a rotated copy
     * @since 1.0.0
     */
    @NotNull
    Schematic rotate(@NotNull StructureService.Rotation rotation);

    /**
     * Creates a copy of this schematic mirrored.
     *
     * @param mirror the mirror mode
     * @return a mirrored copy
     * @since 1.0.0
     */
    @NotNull
    Schematic mirror(@NotNull StructureService.Mirror mirror);

    /**
     * Gets the format this schematic was loaded from.
     *
     * @return the schematic format
     * @since 1.0.0
     */
    @NotNull
    StructureService.SchematicFormat getFormat();

    /**
     * Iterates over all non-air blocks in this schematic.
     *
     * @param consumer the block consumer
     * @since 1.0.0
     */
    void forEach(@NotNull BlockConsumer consumer);

    /**
     * Consumer for iterating over schematic blocks.
     *
     * @since 1.0.0
     */
    @FunctionalInterface
    interface BlockConsumer {
        /**
         * Accepts a block from the schematic.
         *
         * @param x         the relative X coordinate
         * @param y         the relative Y coordinate
         * @param z         the relative Z coordinate
         * @param blockType the block type
         * @param blockData the block data (may be null)
         * @since 1.0.0
         */
        void accept(int x, int y, int z, @NotNull BlockType blockType, @Nullable BlockData blockData);
    }

    /**
     * Entity data stored in a schematic.
     *
     * @param entityType the entity type key
     * @param x          the relative X position
     * @param y          the relative Y position
     * @param z          the relative Z position
     * @param nbt        additional NBT data
     * @since 1.0.0
     */
    record EntityData(
            @NotNull String entityType,
            double x, double y, double z,
            @NotNull Map<String, Object> nbt
    ) {}
}
